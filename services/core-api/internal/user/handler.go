package user

import (
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/rento/core-api/pkg/response"
)

// Handler — foydalanuvchi HTTP handlerlari
type Handler struct {
	service *Service
}

// NewHandler — yangi Handler yaratish
func NewHandler(service *Service) *Handler {
	return &Handler{service: service}
}

// RegisterRoutes — user routelarini ro'yxatdan o'tkazish
func (h *Handler) RegisterRoutes(r *gin.RouterGroup, authMW gin.HandlerFunc) {
	users := r.Group("/users")
	{
		// Protected routes — auth talab qilinadi
		users.Use(authMW)
		{
			users.GET("/me", h.GetMyProfile)
			users.PUT("/me", h.UpdateMyProfile)
			users.GET("/:id", h.GetPublicProfile)
		}
	}
}

// GetMyProfile — GET /users/me
// O'z profilini olish
func (h *Handler) GetMyProfile(c *gin.Context) {
	userID := c.GetString("userId")
	if userID == "" {
		response.Unauthorized(c, "AUTH_REQUIRED", "Avtorizatsiya talab qilinadi")
		return
	}

	profile, err := h.service.GetProfile(c.Request.Context(), userID)
	if err != nil {
		if strings.Contains(err.Error(), "not found") {
			response.NotFound(c, "USER_NOT_FOUND", "Foydalanuvchi topilmadi")
			return
		}
		if strings.Contains(err.Error(), "blocked") {
			response.Forbidden(c, "USER_BLOCKED", "Akkauntingiz bloklangan")
			return
		}
		response.InternalError(c)
		return
	}

	response.OK(c, profile)
}

// UpdateMyProfile — PUT /users/me
// O'z profilini yangilash
func (h *Handler) UpdateMyProfile(c *gin.Context) {
	userID := c.GetString("userId")
	if userID == "" {
		response.Unauthorized(c, "AUTH_REQUIRED", "Avtorizatsiya talab qilinadi")
		return
	}

	var req UpdateProfileRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.ValidationError(c, err.Error(), nil)
		return
	}

	// Hech narsa yuborilmagan bo'lsa
	if req.FullName == nil && req.Email == nil && req.Language == nil {
		response.BadRequest(c, "NO_FIELDS", "Kamida bitta maydon yuborilishi kerak")
		return
	}

	profile, err := h.service.UpdateProfile(c.Request.Context(), userID, &req)
	if err != nil {
		if strings.Contains(err.Error(), "not found") {
			response.NotFound(c, "USER_NOT_FOUND", "Foydalanuvchi topilmadi")
			return
		}
		if strings.Contains(err.Error(), "blocked") {
			response.Forbidden(c, "USER_BLOCKED", "Akkauntingiz bloklangan")
			return
		}
		response.InternalError(c)
		return
	}

	response.OK(c, profile)
}

// GetPublicProfile — GET /users/:id
// Boshqa foydalanuvchining ochiq profilini olish
func (h *Handler) GetPublicProfile(c *gin.Context) {
	id := c.Param("id")

	// "me" ga GET /users/me orqali kirish kerak, bu yerda emas
	if id == "me" {
		h.GetMyProfile(c)
		return
	}

	// UUID format tekshirish
	if _, err := uuid.Parse(id); err != nil {
		response.BadRequest(c, "INVALID_ID", "Noto'g'ri ID formati")
		return
	}

	profile, err := h.service.GetPublicProfile(c.Request.Context(), id)
	if err != nil {
		if strings.Contains(err.Error(), "not found") {
			response.NotFound(c, "USER_NOT_FOUND", "Foydalanuvchi topilmadi")
			return
		}
		response.InternalError(c)
		return
	}

	response.OK(c, profile)
}
