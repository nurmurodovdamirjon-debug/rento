package middleware

import (
	"github.com/gin-gonic/gin"
	"github.com/rento/core-api/pkg/response"
)

// RequireAdmin — admin roli tekshirish middleware
// AuthMiddleware dan keyin ishlatiladi
func RequireAdmin() gin.HandlerFunc {
	return func(c *gin.Context) {
		role := c.GetString("userRole")
		if role != "admin" {
			response.Forbidden(c, "ADMIN_REQUIRED", "Bu endpoint faqat admin uchun")
			c.Abort()
			return
		}
		c.Next()
	}
}
