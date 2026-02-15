package middleware

import (
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
	"github.com/rento/core-api/pkg/response"
)

// AuthMiddleware — JWT auth middleware
// Authorization: Bearer <token> headerdan token oladi
// auth-service bilan bir xil JWT secret ishlatadi
func AuthMiddleware(jwtSecret string) gin.HandlerFunc {
	return func(c *gin.Context) {
		authHeader := c.GetHeader("Authorization")

		if authHeader == "" || !strings.HasPrefix(authHeader, "Bearer ") {
			response.Unauthorized(c, "AUTH_TOKEN_INVALID", "Authorization header topilmadi")
			c.Abort()
			return
		}

		tokenString := strings.TrimPrefix(authHeader, "Bearer ")

		if tokenString == "" {
			response.Unauthorized(c, "AUTH_TOKEN_INVALID", "Token bo'sh")
			c.Abort()
			return
		}

		// Token parse va verify
		token, err := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
			// HMAC algoritmni tekshirish
			if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
				return nil, jwt.ErrSignatureInvalid
			}
			return []byte(jwtSecret), nil
		})

		if err != nil {
			if err == jwt.ErrTokenExpired {
				response.Unauthorized(c, "AUTH_TOKEN_EXPIRED", "Token muddati tugagan")
			} else {
				response.Unauthorized(c, "AUTH_TOKEN_INVALID", "Token noto'g'ri")
			}
			c.Abort()
			return
		}

		claims, ok := token.Claims.(jwt.MapClaims)
		if !ok || !token.Valid {
			response.Unauthorized(c, "AUTH_TOKEN_INVALID", "Token claims noto'g'ri")
			c.Abort()
			return
		}

		// Issuer tekshirish
		iss, _ := claims["iss"].(string)
		if iss != "rento.uz" {
			response.Unauthorized(c, "AUTH_TOKEN_INVALID", "Token issuer noto'g'ri")
			c.Abort()
			return
		}

		// Context ga user ma'lumotlarini yozish
		sub, _ := claims["sub"].(string)
		role, _ := claims["role"].(string)
		phone, _ := claims["phone"].(string)

		c.Set("userId", sub)
		c.Set("userRole", role)
		c.Set("userPhone", phone)

		c.Next()
	}
}
