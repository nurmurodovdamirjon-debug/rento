package response

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

// Response — standart API javob
type Response struct {
	Success bool        `json:"success"`
	Data    interface{} `json:"data,omitempty"`
	Error   *ErrorInfo  `json:"error,omitempty"`
	Meta    *Meta       `json:"meta,omitempty"`
}

// ErrorInfo — xato ma'lumoti
type ErrorInfo struct {
	Code    string      `json:"code"`
	Message string      `json:"message"`
	Details interface{} `json:"details,omitempty"`
}

// Meta — pagination
type Meta struct {
	Page       int `json:"page"`
	PerPage    int `json:"per_page"`
	Total      int `json:"total"`
	TotalPages int `json:"total_pages"`
}

// ===== Muvaffaqiyatli javoblar =====

// OK — 200
func OK(c *gin.Context, data interface{}) {
	c.JSON(http.StatusOK, Response{
		Success: true,
		Data:    data,
	})
}

// Created — 201
func Created(c *gin.Context, data interface{}) {
	c.JSON(http.StatusCreated, Response{
		Success: true,
		Data:    data,
	})
}

// NoContent — 204
func NoContent(c *gin.Context) {
	c.Status(http.StatusNoContent)
}

// Paginated — 200 with meta
func Paginated(c *gin.Context, data interface{}, page, perPage, total int) {
	totalPages := total / perPage
	if total%perPage != 0 {
		totalPages++
	}

	c.JSON(http.StatusOK, Response{
		Success: true,
		Data:    data,
		Meta: &Meta{
			Page:       page,
			PerPage:    perPage,
			Total:      total,
			TotalPages: totalPages,
		},
	})
}

// ===== Xato javoblar =====

// BadRequest — 400
func BadRequest(c *gin.Context, code, message string) {
	c.JSON(http.StatusBadRequest, Response{
		Success: false,
		Error:   &ErrorInfo{Code: code, Message: message},
	})
}

// Unauthorized — 401
func Unauthorized(c *gin.Context, code, message string) {
	c.JSON(http.StatusUnauthorized, Response{
		Success: false,
		Error:   &ErrorInfo{Code: code, Message: message},
	})
}

// Forbidden — 403
func Forbidden(c *gin.Context, code, message string) {
	c.JSON(http.StatusForbidden, Response{
		Success: false,
		Error:   &ErrorInfo{Code: code, Message: message},
	})
}

// NotFound — 404
func NotFound(c *gin.Context, code, message string) {
	c.JSON(http.StatusNotFound, Response{
		Success: false,
		Error:   &ErrorInfo{Code: code, Message: message},
	})
}

// Conflict — 409
func Conflict(c *gin.Context, code, message string) {
	c.JSON(http.StatusConflict, Response{
		Success: false,
		Error:   &ErrorInfo{Code: code, Message: message},
	})
}

// ValidationError — 400 with details
func ValidationError(c *gin.Context, message string, details interface{}) {
	c.JSON(http.StatusBadRequest, Response{
		Success: false,
		Error:   &ErrorInfo{Code: "VALIDATION_ERROR", Message: message, Details: details},
	})
}

// TooManyRequests — 429
func TooManyRequests(c *gin.Context, message string) {
	c.JSON(http.StatusTooManyRequests, Response{
		Success: false,
		Error:   &ErrorInfo{Code: "RATE_LIMIT_EXCEEDED", Message: message},
	})
}

// InternalError — 500
func InternalError(c *gin.Context) {
	c.JSON(http.StatusInternalServerError, Response{
		Success: false,
		Error:   &ErrorInfo{Code: "INTERNAL_ERROR", Message: "Ichki server xatosi"},
	})
}
