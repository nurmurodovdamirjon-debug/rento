package listing

import (
	"errors"
	"strconv"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/rento/core-api/pkg/response"
)

// Handler — e'lon HTTP handlerlari
type Handler struct {
	service *Service
}

// NewHandler — yangi Handler yaratish
func NewHandler(service *Service) *Handler {
	return &Handler{service: service}
}

// RegisterRoutes — listing routelarini ro'yxatdan o'tkazish
func (h *Handler) RegisterRoutes(r *gin.RouterGroup, authMW gin.HandlerFunc) {
	listings := r.Group("/listings")
	{
		// Public routes — auth ixtiyoriy
		listings.GET("", h.GetListings)
		listings.GET("/search", h.SearchListings)
		listings.GET("/nearby", h.GetNearby)
		listings.GET("/:id", h.GetListing)

		// Protected routes — auth talab qilinadi
		protected := listings.Group("")
		protected.Use(authMW)
		{
			protected.POST("", h.CreateListing)
			protected.PUT("/:id", h.UpdateListing)
			protected.DELETE("/:id", h.DeleteListing)
			protected.PUT("/:id/status", h.UpdateStatus)
			protected.GET("/:id/stats", h.GetStats)
			protected.GET("/my", h.GetMyListings)
		}
	}
}

// RegisterAdminRoutes — admin moderation routelari
func (h *Handler) RegisterAdminRoutes(r *gin.RouterGroup, authMW, adminMW gin.HandlerFunc) {
	admin := r.Group("/admin/listings")
	admin.Use(authMW, adminMW)
	{
		admin.GET("/pending", h.AdminGetPending)
		admin.PUT("/:id/approve", h.AdminApprove)
		admin.PUT("/:id/reject", h.AdminReject)
	}
}

// CreateListing — POST /listings
func (h *Handler) CreateListing(c *gin.Context) {
	userID := c.GetString("userId")
	if userID == "" {
		response.Unauthorized(c, "AUTH_REQUIRED", "Avtorizatsiya talab qilinadi")
		return
	}

	var req CreateListingRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.ValidationError(c, err.Error(), nil)
		return
	}

	listing, err := h.service.CreateListing(c.Request.Context(), userID, &req)
	if err != nil {
		if errors.Is(err, ErrDailyLimitExceeded) {
			response.TooManyRequests(c, "Kunlik limit: max 10 ta e'lon. Ertaga qaytadan urinib ko'ring.")
			return
		}
		response.InternalError(c)
		return
	}

	response.Created(c, listing)
}

// GetListing — GET /listings/:id
func (h *Handler) GetListing(c *gin.Context) {
	id := c.Param("id")

	if id == "my" {
		h.GetMyListings(c)
		return
	}

	if _, err := uuid.Parse(id); err != nil {
		response.BadRequest(c, "INVALID_ID", "Noto'g'ri ID formati")
		return
	}

	listing, err := h.service.GetListing(c.Request.Context(), id)
	if err != nil {
		if errors.Is(err, ErrNotFound) {
			response.NotFound(c, "LISTING_NOT_FOUND", "E'lon topilmadi")
			return
		}
		response.InternalError(c)
		return
	}

	response.OK(c, listing)
}

// GetListings — GET /listings?city=...&type=...
func (h *Handler) GetListings(c *gin.Context) {
	var filter ListingsFilter
	if err := c.ShouldBindQuery(&filter); err != nil {
		response.ValidationError(c, err.Error(), nil)
		return
	}

	items, total, err := h.service.GetListings(c.Request.Context(), &filter)
	if err != nil {
		response.InternalError(c)
		return
	}

	response.Paginated(c, items, filter.Page, filter.PerPage, total)
}

// GetMyListings — GET /listings/my?status=...
func (h *Handler) GetMyListings(c *gin.Context) {
	userID := c.GetString("userId")
	if userID == "" {
		response.Unauthorized(c, "AUTH_REQUIRED", "Avtorizatsiya talab qilinadi")
		return
	}

	status := c.Query("status")
	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	perPage, _ := strconv.Atoi(c.DefaultQuery("per_page", "20"))

	items, total, err := h.service.GetMyListings(c.Request.Context(), userID, status, page, perPage)
	if err != nil {
		response.InternalError(c)
		return
	}

	response.Paginated(c, items, page, perPage, total)
}

// UpdateListing — PUT /listings/:id
func (h *Handler) UpdateListing(c *gin.Context) {
	userID := c.GetString("userId")
	if userID == "" {
		response.Unauthorized(c, "AUTH_REQUIRED", "Avtorizatsiya talab qilinadi")
		return
	}

	id := c.Param("id")
	if _, err := uuid.Parse(id); err != nil {
		response.BadRequest(c, "INVALID_ID", "Noto'g'ri ID formati")
		return
	}

	var req UpdateListingRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.ValidationError(c, err.Error(), nil)
		return
	}

	listing, err := h.service.UpdateListing(c.Request.Context(), id, userID, &req)
	if err != nil {
		if errors.Is(err, ErrNotFound) {
			response.NotFound(c, "LISTING_NOT_FOUND", "E'lon topilmadi")
			return
		}
		if errors.Is(err, ErrForbidden) {
			response.Forbidden(c, "LISTING_NOT_OWNER", "E'lon egasi emassiz")
			return
		}
		response.InternalError(c)
		return
	}

	response.OK(c, listing)
}

// DeleteListing — DELETE /listings/:id
func (h *Handler) DeleteListing(c *gin.Context) {
	userID := c.GetString("userId")
	if userID == "" {
		response.Unauthorized(c, "AUTH_REQUIRED", "Avtorizatsiya talab qilinadi")
		return
	}

	id := c.Param("id")
	if _, err := uuid.Parse(id); err != nil {
		response.BadRequest(c, "INVALID_ID", "Noto'g'ri ID formati")
		return
	}

	err := h.service.DeleteListing(c.Request.Context(), id, userID)
	if err != nil {
		if errors.Is(err, ErrNotFound) {
			response.NotFound(c, "LISTING_NOT_FOUND", "E'lon topilmadi")
			return
		}
		if errors.Is(err, ErrForbidden) {
			response.Forbidden(c, "LISTING_NOT_OWNER", "E'lon egasi emassiz")
			return
		}
		response.InternalError(c)
		return
	}

	response.NoContent(c)
}

// UpdateStatus — PUT /listings/:id/status
func (h *Handler) UpdateStatus(c *gin.Context) {
	userID := c.GetString("userId")
	if userID == "" {
		response.Unauthorized(c, "AUTH_REQUIRED", "Avtorizatsiya talab qilinadi")
		return
	}

	id := c.Param("id")
	if _, err := uuid.Parse(id); err != nil {
		response.BadRequest(c, "INVALID_ID", "Noto'g'ri ID formati")
		return
	}

	var req UpdateStatusRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		response.ValidationError(c, err.Error(), nil)
		return
	}

	err := h.service.UpdateStatus(c.Request.Context(), id, userID, req.Status)
	if err != nil {
		if errors.Is(err, ErrNotFound) {
			response.NotFound(c, "LISTING_NOT_FOUND", "E'lon topilmadi")
			return
		}
		if errors.Is(err, ErrForbidden) {
			response.Forbidden(c, "LISTING_NOT_OWNER", "E'lon egasi emassiz")
			return
		}
		if errors.Is(err, ErrInvalidTransition) {
			response.BadRequest(c, "INVALID_STATUS_TRANSITION", err.Error())
			return
		}
		response.InternalError(c)
		return
	}

	response.OK(c, gin.H{"status": req.Status, "message": "Status yangilandi"})
}

// GetStats — GET /listings/:id/stats
func (h *Handler) GetStats(c *gin.Context) {
	userID := c.GetString("userId")
	if userID == "" {
		response.Unauthorized(c, "AUTH_REQUIRED", "Avtorizatsiya talab qilinadi")
		return
	}

	id := c.Param("id")
	if _, err := uuid.Parse(id); err != nil {
		response.BadRequest(c, "INVALID_ID", "Noto'g'ri ID formati")
		return
	}

	stats, err := h.service.GetStats(c.Request.Context(), id, userID)
	if err != nil {
		if errors.Is(err, ErrNotFound) {
			response.NotFound(c, "LISTING_NOT_FOUND", "E'lon topilmadi")
			return
		}
		if errors.Is(err, ErrForbidden) {
			response.Forbidden(c, "LISTING_NOT_OWNER", "E'lon egasi emassiz")
			return
		}
		response.InternalError(c)
		return
	}

	response.OK(c, stats)
}

// SearchListings — GET /listings/search?q=...&city=...&sort=relevance
func (h *Handler) SearchListings(c *gin.Context) {
	var filter SearchFilter
	if err := c.ShouldBindQuery(&filter); err != nil {
		response.ValidationError(c, err.Error(), nil)
		return
	}

	items, total, err := h.service.SearchListings(c.Request.Context(), &filter)
	if err != nil {
		response.InternalError(c)
		return
	}

	response.Paginated(c, items, filter.Page, filter.PerPage, total)
}

// GetNearby — GET /listings/nearby?lat=...&lng=...&radius_km=...
func (h *Handler) GetNearby(c *gin.Context) {
	var filter NearbyFilter
	if err := c.ShouldBindQuery(&filter); err != nil {
		response.ValidationError(c, err.Error(), nil)
		return
	}

	items, total, err := h.service.GetNearby(c.Request.Context(), &filter)
	if err != nil {
		response.InternalError(c)
		return
	}

	response.Paginated(c, items, filter.Page, filter.PerPage, total)
}

// ===== ADMIN MODERATION HANDLERS =====

// AdminGetPending — GET /admin/listings/pending?page=1&per_page=20
func (h *Handler) AdminGetPending(c *gin.Context) {
	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	perPage, _ := strconv.Atoi(c.DefaultQuery("per_page", "20"))

	items, total, err := h.service.AdminGetPendingListings(c.Request.Context(), page, perPage)
	if err != nil {
		response.InternalError(c)
		return
	}

	response.Paginated(c, items, page, perPage, total)
}

// AdminApprove — PUT /admin/listings/:id/approve
func (h *Handler) AdminApprove(c *gin.Context) {
	adminID := c.GetString("userId")
	id := c.Param("id")
	if _, err := uuid.Parse(id); err != nil {
		response.BadRequest(c, "INVALID_ID", "Noto'g'ri ID formati")
		return
	}

	err := h.service.AdminUpdateStatus(c.Request.Context(), id, adminID, "active")
	if err != nil {
		if errors.Is(err, ErrNotFound) {
			response.NotFound(c, "LISTING_NOT_FOUND", "E'lon topilmadi")
			return
		}
		if errors.Is(err, ErrInvalidTransition) {
			response.BadRequest(c, "INVALID_STATUS_TRANSITION", err.Error())
			return
		}
		response.InternalError(c)
		return
	}

	response.OK(c, gin.H{"status": "active", "message": "E'lon tasdiqlandi"})
}

// AdminReject — PUT /admin/listings/:id/reject
func (h *Handler) AdminReject(c *gin.Context) {
	adminID := c.GetString("userId")
	id := c.Param("id")
	if _, err := uuid.Parse(id); err != nil {
		response.BadRequest(c, "INVALID_ID", "Noto'g'ri ID formati")
		return
	}

	err := h.service.AdminUpdateStatus(c.Request.Context(), id, adminID, "rejected")
	if err != nil {
		if errors.Is(err, ErrNotFound) {
			response.NotFound(c, "LISTING_NOT_FOUND", "E'lon topilmadi")
			return
		}
		if errors.Is(err, ErrInvalidTransition) {
			response.BadRequest(c, "INVALID_STATUS_TRANSITION", err.Error())
			return
		}
		response.InternalError(c)
		return
	}

	response.OK(c, gin.H{"status": "rejected", "message": "E'lon rad etildi"})
}
