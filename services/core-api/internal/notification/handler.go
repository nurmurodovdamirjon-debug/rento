package notification

import (
	"strconv"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/rento/core-api/pkg/response"
)

// Handler — notifications HTTP handlerlari
type Handler struct {
	service *Service
}

// NewHandler — yangi Handler yaratish
func NewHandler(service *Service) *Handler {
	return &Handler{service: service}
}

// RegisterRoutes — notification routelarini ro'yxatdan o'tkazish
func (h *Handler) RegisterRoutes(r *gin.RouterGroup, authMW gin.HandlerFunc) {
	notifications := r.Group("/notifications")
	notifications.Use(authMW)
	{
		// GET /notifications — bildirishnomalar ro'yxati
		notifications.GET("", h.GetNotifications)

		// GET /notifications/unread-count — o'qilmaganlar soni
		notifications.GET("/unread-count", h.GetUnreadCount)

		// PUT /notifications/:id/read — o'qildi belgilash
		notifications.PUT("/:id/read", h.MarkAsRead)

		// PUT /notifications/read-all — barchasini o'qildi
		notifications.PUT("/read-all", h.MarkAllAsRead)

		// POST /notifications/fcm-token — FCM token saqlash
		notifications.POST("/fcm-token", h.RegisterFCMToken)

		// DELETE /notifications/fcm-token — FCM token o'chirish
		notifications.DELETE("/fcm-token", h.UnregisterFCMToken)
	}
}

// GetNotifications — GET /notifications
func (h *Handler) GetNotifications(c *gin.Context) {
	userID := c.GetString("userId")
	if userID == "" {
		response.Unauthorized(c, "AUTH_REQUIRED", "Avtorizatsiya talab qilinadi")
		return
	}

	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	perPage, _ := strconv.Atoi(c.DefaultQuery("per_page", "20"))
	if perPage > 50 {
		perPage = 50
	}

	items, total, err := h.service.GetNotifications(c.Request.Context(), userID, page, perPage)
	if err != nil {
		response.InternalError(c)
		return
	}

	response.Paginated(c, items, page, perPage, total)
}

// GetUnreadCount — GET /notifications/unread-count
func (h *Handler) GetUnreadCount(c *gin.Context) {
	userID := c.GetString("userId")
	if userID == "" {
		response.Unauthorized(c, "AUTH_REQUIRED", "Avtorizatsiya talab qilinadi")
		return
	}

	count, err := h.service.GetUnreadCount(c.Request.Context(), userID)
	if err != nil {
		response.InternalError(c)
		return
	}

	response.OK(c, gin.H{"unread_count": count})
}

// MarkAsRead — PUT /notifications/:id/read
func (h *Handler) MarkAsRead(c *gin.Context) {
	userID := c.GetString("userId")
	if userID == "" {
		response.Unauthorized(c, "AUTH_REQUIRED", "Avtorizatsiya talab qilinadi")
		return
	}

	id := c.Param("id")
	if _, err := uuid.Parse(id); err != nil {
		response.BadRequest(c, "INVALID_ID", "Noto'g'ri bildirishnoma ID")
		return
	}

	if err := h.service.MarkAsRead(c.Request.Context(), id, userID); err != nil {
		response.InternalError(c)
		return
	}

	response.NoContent(c)
}

// MarkAllAsRead — PUT /notifications/read-all
func (h *Handler) MarkAllAsRead(c *gin.Context) {
	userID := c.GetString("userId")
	if userID == "" {
		response.Unauthorized(c, "AUTH_REQUIRED", "Avtorizatsiya talab qilinadi")
		return
	}

	count, err := h.service.MarkAllAsRead(c.Request.Context(), userID)
	if err != nil {
		response.InternalError(c)
		return
	}

	response.OK(c, gin.H{"marked_count": count})
}

// RegisterFCMToken — POST /notifications/fcm-token
func (h *Handler) RegisterFCMToken(c *gin.Context) {
	userID := c.GetString("userId")
	if userID == "" {
		response.Unauthorized(c, "AUTH_REQUIRED", "Avtorizatsiya talab qilinadi")
		return
	}

	var req RegisterTokenRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.ValidationError(c, err.Error(), nil)
		return
	}

	if err := h.service.RegisterFCMToken(c.Request.Context(), userID, req.Token, req.DeviceType); err != nil {
		response.InternalError(c)
		return
	}

	response.OK(c, gin.H{"message": "FCM token saqlandi"})
}

// UnregisterFCMToken — DELETE /notifications/fcm-token
func (h *Handler) UnregisterFCMToken(c *gin.Context) {
	userID := c.GetString("userId")
	if userID == "" {
		response.Unauthorized(c, "AUTH_REQUIRED", "Avtorizatsiya talab qilinadi")
		return
	}

	var req struct {
		Token string `json:"token" binding:"required"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		response.ValidationError(c, err.Error(), nil)
		return
	}

	if err := h.service.UnregisterFCMToken(c.Request.Context(), userID, req.Token); err != nil {
		response.InternalError(c)
		return
	}

	response.NoContent(c)
}
