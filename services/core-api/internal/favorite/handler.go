package favorite

import (
	"strconv"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/rento/core-api/pkg/response"
)

// Handler — favorites HTTP handlerlari
type Handler struct {
	service *Service
}

// NewHandler — yangi Handler yaratish
func NewHandler(service *Service) *Handler {
	return &Handler{service: service}
}

// RegisterRoutes — favorite routelarini ro'yxatdan o'tkazish
func (h *Handler) RegisterRoutes(r *gin.RouterGroup, authMW gin.HandlerFunc) {
	favorites := r.Group("/favorites")
	favorites.Use(authMW)
	{
		// POST /favorites/:listing_id — toggle favorite
		favorites.POST("/:listing_id", h.Toggle)

		// GET /favorites — sevimlilar ro'yxati
		favorites.GET("", h.GetFavorites)

		// GET /favorites/check/:listing_id — tekshirish
		favorites.GET("/check/:listing_id", h.Check)

		// GET /favorites/ids — barcha sevimli e'lon ID lari (batch)
		favorites.GET("/ids", h.GetFavoriteIDs)
	}
}

// Toggle — POST /favorites/:listing_id — qo'shish/olib tashlash
func (h *Handler) Toggle(c *gin.Context) {
	userID := c.GetString("userId")
	if userID == "" {
		response.Unauthorized(c, "AUTH_REQUIRED", "Avtorizatsiya talab qilinadi")
		return
	}

	listingID := c.Param("listing_id")
	if _, err := uuid.Parse(listingID); err != nil {
		response.BadRequest(c, "INVALID_ID", "Noto'g'ri listing_id formati")
		return
	}

	result, err := h.service.Toggle(c.Request.Context(), userID, listingID)
	if err != nil {
		response.InternalError(c)
		return
	}

	response.OK(c, result)
}

// GetFavorites — GET /favorites — sevimlilar ro'yxati
func (h *Handler) GetFavorites(c *gin.Context) {
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

	items, total, err := h.service.GetUserFavorites(c.Request.Context(), userID, page, perPage)
	if err != nil {
		response.InternalError(c)
		return
	}

	response.Paginated(c, items, page, perPage, total)
}

// Check — GET /favorites/check/:listing_id — sevimlilardami tekshirish
func (h *Handler) Check(c *gin.Context) {
	userID := c.GetString("userId")
	if userID == "" {
		response.Unauthorized(c, "AUTH_REQUIRED", "Avtorizatsiya talab qilinadi")
		return
	}

	listingID := c.Param("listing_id")
	if _, err := uuid.Parse(listingID); err != nil {
		response.BadRequest(c, "INVALID_ID", "Noto'g'ri listing_id formati")
		return
	}

	isFavorite, err := h.service.IsFavorite(c.Request.Context(), userID, listingID)
	if err != nil {
		response.InternalError(c)
		return
	}

	response.OK(c, gin.H{"is_favorite": isFavorite})
}

// GetFavoriteIDs — GET /favorites/ids — barcha sevimli IDlar
func (h *Handler) GetFavoriteIDs(c *gin.Context) {
	userID := c.GetString("userId")
	if userID == "" {
		response.Unauthorized(c, "AUTH_REQUIRED", "Avtorizatsiya talab qilinadi")
		return
	}

	ids, err := h.service.GetUserFavoriteIDs(c.Request.Context(), userID)
	if err != nil {
		response.InternalError(c)
		return
	}

	response.OK(c, gin.H{"listing_ids": ids})
}
