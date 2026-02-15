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
