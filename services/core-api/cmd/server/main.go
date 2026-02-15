package main

import (
	"context"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"

	"github.com/rento/core-api/internal/config"
	"github.com/rento/core-api/internal/favorite"
	"github.com/rento/core-api/internal/listing"
	"github.com/rento/core-api/internal/media"
	"github.com/rento/core-api/internal/middleware"
	"github.com/rento/core-api/internal/notification"
	"github.com/rento/core-api/internal/report"
	"github.com/rento/core-api/internal/user"
	"github.com/rento/core-api/pkg/database"
	"github.com/rento/core-api/pkg/response"
)

func main() {
	cfg := config.Load()

	// Zerolog sozlash
	if cfg.AppEnv == "development" {
		log.Logger = log.Output(zerolog.ConsoleWriter{Out: os.Stderr, TimeFormat: time.RFC3339})
		zerolog.SetGlobalLevel(zerolog.DebugLevel)
	} else {
		zerolog.SetGlobalLevel(zerolog.InfoLevel)
		gin.SetMode(gin.ReleaseMode)
	}

	// PostgreSQL ulanish
	db, err := database.NewPostgresDB(cfg.DatabaseURL)
	if err != nil {
		log.Fatal().Err(err).Msg("Failed to connect PostgreSQL")
	}
	defer db.Close()

	// Redis ulanish
	redisClient, err := database.NewRedisClient(cfg.RedisURL, "")
	if err != nil {
		log.Fatal().Err(err).Msg("Failed to connect Redis")
	}
	defer redisClient.Close()

	// Elasticsearch ulanish
	esClient, err := database.NewElasticClient(cfg.ElasticURL)
	if err != nil {
		log.Warn().Err(err).Msg("Elasticsearch not available — search disabled")
		esClient = nil
	}

	router := gin.New()

	// Middleware
	router.Use(middleware.LoggerMiddleware())
	router.Use(gin.Recovery())
	router.Use(middleware.CORSMiddleware(cfg.CORSOrigins))
	router.Use(middleware.RateLimitMiddleware(redisClient, cfg.RateLimitMax))

	// Health check endpoint
	router.GET("/health", func(c *gin.Context) {
		response.OK(c, gin.H{
			"status":  "ok",
			"service": "core-api",
			"version": cfg.AppVersion,
		})
	})

	// User module — repository → service → handler
	userRepo := user.NewRepository(db)
	userService := user.NewService(userRepo)
	userHandler := user.NewHandler(userService)

	// Listing module — repository → search → service → handler
	listingRepo := listing.NewRepository(db, redisClient)
	var searchRepo *listing.SearchRepo
	if esClient != nil {
		searchRepo = listing.NewSearchRepo(esClient, redisClient)
		if err := searchRepo.EnsureIndex(context.Background()); err != nil {
			log.Warn().Err(err).Msg("Failed to ensure ES index")
		}
	}
	listingService := listing.NewService(listingRepo, searchRepo, redisClient)
	listingHandler := listing.NewHandler(listingService)

	// Media module — storage → service → handler
	minioStorage, err := media.NewStorage(cfg.MinioEndpoint, cfg.MinioAccessKey, cfg.MinioSecretKey, cfg.MinioUseSSL)
	if err != nil {
		log.Fatal().Err(err).Msg("Failed to connect MinIO")
	}
	if err := minioStorage.EnsureBuckets(context.Background()); err != nil {
		log.Fatal().Err(err).Msg("Failed to ensure MinIO buckets")
	}
	mediaService := media.NewService(minioStorage)
	mediaHandler := media.NewHandler(mediaService)

	// Favorite module — repository → service → handler
	favoriteRepo := favorite.NewRepository(db)
	favoriteService := favorite.NewService(favoriteRepo)
	favoriteHandler := favorite.NewHandler(favoriteService)

	// Notification module — repository → service → handler
	notificationRepo := notification.NewRepository(db)
	notificationService := notification.NewService(notificationRepo)
	notificationHandler := notification.NewHandler(notificationService)

	// Report module — repository → service → handler
	reportRepo := report.NewRepository(db)
	reportService := report.NewService(reportRepo)
	reportHandler := report.NewHandler(reportService)

	// notificationService ni keyinchalik chat/listing modullarida ishlatish mumkin
	_ = notificationService

	// Auth middleware
	authMW := middleware.AuthMiddleware(cfg.JWTAccessSecret)

	// Admin middleware
	adminMW := middleware.RequireAdmin()

	// API v1 routes
	v1 := router.Group("/api/v1")
	{
		// User routes (GET /users/me, PUT /users/me, GET /users/:id)
		userHandler.RegisterRoutes(v1, authMW)

		// Listing routes (CRUD + status + stats)
		listingHandler.RegisterRoutes(v1, authMW)

		// Media routes (POST /media/upload)
		mediaHandler.RegisterRoutes(v1, authMW)

		// Favorite routes (POST/GET /favorites)
		favoriteHandler.RegisterRoutes(v1, authMW)

		// Notification routes (GET/PUT /notifications)
		notificationHandler.RegisterRoutes(v1, authMW)

		// Report routes (POST /reports, GET/PUT /reports/admin/*)
		reportHandler.RegisterRoutes(v1, authMW, adminMW)

		// Admin routes
		listingHandler.RegisterAdminRoutes(v1, authMW, adminMW)
		userHandler.RegisterAdminRoutes(v1, authMW, adminMW)
	}

	srv := &http.Server{
		Addr:         ":" + cfg.AppPort,
		Handler:      router,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 15 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	// Graceful shutdown
	go func() {
		log.Info().
			Str("port", cfg.AppPort).
			Str("env", cfg.AppEnv).
			Msg("Core API starting")
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatal().Err(err).Msg("Server failed")
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	log.Info().Msg("Shutting down server...")
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	if err := srv.Shutdown(ctx); err != nil {
		log.Fatal().Err(err).Msg("Server forced shutdown")
	}
	log.Info().Msg("Server exited")
}
