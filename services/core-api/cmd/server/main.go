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
	"github.com/rento/core-api/internal/middleware"
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

	// Auth middleware
	authMW := middleware.AuthMiddleware(cfg.JWTAccessSecret)

	// API v1 routes
	v1 := router.Group("/api/v1")
	{
		// User routes (GET /users/me, PUT /users/me, GET /users/:id)
		userHandler.RegisterRoutes(v1, authMW)
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
