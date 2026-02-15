package middleware

import (
	"context"
	"fmt"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/redis/go-redis/v9"
	"github.com/rento/core-api/pkg/response"
)

// RateLimitMiddleware — Redis asosida IP rate limiter
// maxRequests: daqiqasiga maksimal so'rovlar (masalan 60)
func RateLimitMiddleware(redisClient *redis.Client, maxRequests int) gin.HandlerFunc {
	return func(c *gin.Context) {
		ctx := context.Background()
		ip := c.ClientIP()
		key := fmt.Sprintf("rate:api:%s", ip)

		current, err := redisClient.Incr(ctx, key).Result()
		if err != nil {
			// Redis xato bo'lsa — o'tkazib yuborish
			c.Next()
			return
		}

		if current == 1 {
			redisClient.Expire(ctx, key, time.Minute)
		}

		if current > int64(maxRequests) {
			response.TooManyRequests(c, "So'rovlar limiti oshib ketdi")
			c.Abort()
			return
		}

		c.Next()
	}
}
