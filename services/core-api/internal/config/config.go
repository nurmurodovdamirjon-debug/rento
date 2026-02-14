package config

import (
	"os"
	"strconv"
	"strings"
)

// Config — loyiha konfiguratsiyasi
type Config struct {
	AppEnv           string
	AppPort          string
	AppVersion       string
	DatabaseURL      string
	RedisURL         string
	ElasticURL       string
	JWTAccessSecret  string
	JWTRefreshSecret string
	JWTAccessExpiry  string
	JWTRefreshExpiry string
	MinioEndpoint    string
	MinioAccessKey   string
	MinioSecretKey   string
	MinioUseSSL      bool
	CORSOrigins      []string
	RateLimitMax     int
}

// Load — .env dan konfiguratsiya yuklash
func Load() *Config {
	return &Config{
		AppEnv:           getEnv("APP_ENV", "development"),
		AppPort:          getEnv("APP_PORT", "3002"),
		AppVersion:       getEnv("APP_VERSION", "0.1.0"),
		DatabaseURL:      buildDSN(),
		RedisURL:         getEnv("REDIS_HOST", "localhost") + ":" + getEnv("REDIS_PORT", "6379"),
		ElasticURL:       getEnv("ES_URL", "http://localhost:9200"),
		JWTAccessSecret:  getEnv("JWT_ACCESS_SECRET", ""),
		JWTRefreshSecret: getEnv("JWT_REFRESH_SECRET", ""),
		JWTAccessExpiry:  getEnv("JWT_ACCESS_EXPIRY", "15m"),
		JWTRefreshExpiry: getEnv("JWT_REFRESH_EXPIRY", "7d"),
		MinioEndpoint:    getEnv("MINIO_ENDPOINT", "localhost:9000"),
		MinioAccessKey:   getEnv("MINIO_ACCESS_KEY", "minioadmin"),
		MinioSecretKey:   getEnv("MINIO_SECRET_KEY", "minioadmin"),
		MinioUseSSL:      getEnvBool("MINIO_USE_SSL", false),
		CORSOrigins:      strings.Split(getEnv("CORS_ALLOWED_ORIGINS", ""), ","),
		RateLimitMax:     getEnvInt("RATE_LIMIT_MAX_REQUESTS", 60),
	}
}

func buildDSN() string {
	return "host=" + getEnv("DB_HOST", "localhost") +
		" port=" + getEnv("DB_PORT", "5432") +
		" user=" + getEnv("DB_USER", "rento_user") +
		" password=" + getEnv("DB_PASSWORD", "rento_secret_password") +
		" dbname=" + getEnv("DB_NAME", "rento") +
		" sslmode=" + getEnv("DB_SSL_MODE", "disable")
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}

func getEnvInt(key string, fallback int) int {
	if v := os.Getenv(key); v != "" {
		if i, err := strconv.Atoi(v); err == nil {
			return i
		}
	}
	return fallback
}

func getEnvBool(key string, fallback bool) bool {
	if v := os.Getenv(key); v != "" {
		if b, err := strconv.ParseBool(v); err == nil {
			return b
		}
	}
	return fallback
}
