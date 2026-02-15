package report

import (
	"errors"
	"fmt"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/rento/core-api/pkg/response"
)

// Handler — report HTTP handlerlari
type Handler struct {
	service *Service
}

// NewHandler — yangi Handler yaratish
func NewHandler(service *Service) *Handler {
	return &Handler{service: service}
}

// RegisterRoutes — report routelarini ro'yxatdan o'tkazish
func (h *Handler) RegisterRoutes(r *gin.RouterGroup, authMW, adminMW gin.HandlerFunc) {
	reports := r.Group("/reports")
	{
		// Foydalanuvchi — shikoyat yuborish
		reports.POST("", authMW, h.CreateReport)

		// Admin — shikoyatlar boshqaruvi
		admin := reports.Group("/admin")
		admin.Use(authMW, adminMW)
		{
			admin.GET("", h.GetReports)
			admin.GET("/:id", h.GetReport)
			admin.PUT("/:id/resolve", h.ResolveReport)
		}
	}
}

// CreateReport — POST /reports
func (h *Handler) CreateReport(c *gin.Context) {
	userID := c.GetString("userId")
	if userID == "" {
		response.Unauthorized(c, "AUTH_REQUIRED", "Avtorizatsiya talab qilinadi")
		return
	}

	var req CreateReportRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.ValidationError(c, err.Error(), nil)
		return
	}

	report, err := h.service.CreateReport(c.Request.Context(), userID, &req)
	if err != nil {
		if errors.Is(err, ErrAlreadyReported) {
			response.BadRequest(c, "ALREADY_REPORTED", err.Error())
			return
		}
		if errors.Is(err, ErrSelfReport) {
			response.BadRequest(c, "SELF_REPORT", err.Error())
			return
		}
		response.InternalError(c)
		return
	}

	response.Created(c, report.ToResponse())
}

// GetReports — GET /reports/admin?status=pending&page=1&per_page=20
func (h *Handler) GetReports(c *gin.Context) {
	status := c.Query("status")
	page := queryInt(c, "page", 1)
	perPage := queryInt(c, "per_page", 20)

	items, total, err := h.service.GetReports(c.Request.Context(), status, page, perPage)
	if err != nil {
		response.InternalError(c)
		return
	}

	response.Paginated(c, items, page, perPage, total)
}

// GetReport — GET /reports/admin/:id
func (h *Handler) GetReport(c *gin.Context) {
	id := c.Param("id")
	if _, err := uuid.Parse(id); err != nil {
		response.BadRequest(c, "INVALID_ID", "Noto'g'ri ID formati")
		return
	}

	report, err := h.service.GetReport(c.Request.Context(), id)
	if err != nil {
		response.NotFound(c, "REPORT_NOT_FOUND", "Shikoyat topilmadi")
		return
	}

	response.OK(c, report.ToResponse())
}

// ResolveReport — PUT /reports/admin/:id/resolve
func (h *Handler) ResolveReport(c *gin.Context) {
	adminID := c.GetString("userId")
	if adminID == "" {
		response.Unauthorized(c, "AUTH_REQUIRED", "Avtorizatsiya talab qilinadi")
		return
	}

	id := c.Param("id")
	if _, err := uuid.Parse(id); err != nil {
		response.BadRequest(c, "INVALID_ID", "Noto'g'ri ID formati")
		return
	}

	var req ResolveReportRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.ValidationError(c, err.Error(), nil)
		return
	}

	if err := h.service.ResolveReport(c.Request.Context(), id, adminID, &req); err != nil {
		if errors.Is(err, ErrNotFound) {
			response.NotFound(c, "REPORT_NOT_FOUND", "Shikoyat topilmadi")
			return
		}
		response.BadRequest(c, "RESOLVE_FAILED", err.Error())
		return
	}

	response.OK(c, gin.H{"message": "Shikoyat hal qilindi"})
}

// queryInt — query parametrdan int olish
func queryInt(c *gin.Context, key string, defaultVal int) int {
	val := c.Query(key)
	if val == "" {
		return defaultVal
	}
	n := defaultVal
	_, _ = fmt.Sscanf(val, "%d", &n)
	return n
}
