package errors

import (
	"errors"
	"net/http"
)

// AppError — ilovadagi standart xato
type AppError struct {
	StatusCode int    `json:"-"`
	Code       string `json:"code"`
	Message    string `json:"message"`
}

func (e *AppError) Error() string {
	return e.Message
}

// ===== Constructor functions =====

// NewBadRequest — 400
func NewBadRequest(code, message string) *AppError {
	return &AppError{StatusCode: http.StatusBadRequest, Code: code, Message: message}
}

// NewUnauthorized — 401
func NewUnauthorized(code, message string) *AppError {
	return &AppError{StatusCode: http.StatusUnauthorized, Code: code, Message: message}
}

// NewForbidden — 403
func NewForbidden(code, message string) *AppError {
	return &AppError{StatusCode: http.StatusForbidden, Code: code, Message: message}
}

// NewNotFound — 404
func NewNotFound(code, message string) *AppError {
	return &AppError{StatusCode: http.StatusNotFound, Code: code, Message: message}
}

// NewConflict — 409
func NewConflict(code, message string) *AppError {
	return &AppError{StatusCode: http.StatusConflict, Code: code, Message: message}
}

// NewTooManyRequests — 429
func NewTooManyRequests(code, message string) *AppError {
	return &AppError{StatusCode: http.StatusTooManyRequests, Code: code, Message: message}
}

// NewInternal — 500
func NewInternal(message string) *AppError {
	return &AppError{StatusCode: http.StatusInternalServerError, Code: "INTERNAL_ERROR", Message: message}
}

// ===== Sentinel errors =====

var (
	ErrInvalidToken = errors.New("noto'g'ri token")
	ErrTokenExpired = errors.New("token muddati tugagan")
	ErrUserBlocked  = errors.New("foydalanuvchi bloklangan")
	ErrUserNotFound = errors.New("foydalanuvchi topilmadi")
)
