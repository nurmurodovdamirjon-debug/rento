package media

import (
	"fmt"
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/rento/core-api/pkg/response"
)

// Handler — media HTTP handlerlari
type Handler struct {
	service *Service
}

// NewHandler — yangi Handler yaratish
func NewHandler(service *Service) *Handler {
	return &Handler{service: service}
}

// RegisterRoutes — media routelarini ro'yxatdan o'tkazish
func (h *Handler) RegisterRoutes(r *gin.RouterGroup, authMW gin.HandlerFunc) {
	media := r.Group("/media")
	media.Use(authMW)
	{
		media.POST("/upload", h.Upload)
	}
}

// Upload — POST /media/upload
// Multipart form-data bilan rasm yuklash
func (h *Handler) Upload(c *gin.Context) {
	userID := c.GetString("userId")
	if userID == "" {
		response.Unauthorized(c, "AUTH_REQUIRED", "Avtorizatsiya talab qilinadi")
		return
	}

	// Bucket parametri (default: listings)
	bucket := c.DefaultPostForm("bucket", BucketListings)
	if bucket != BucketListings && bucket != BucketAvatars {
		response.BadRequest(c, "INVALID_BUCKET", "Bucket faqat 'listings' yoki 'avatars' bo'lishi mumkin")
		return
	}

	file, header, err := c.Request.FormFile("file")
	if err != nil {
		response.BadRequest(c, "FILE_REQUIRED", "Fayl yuborilmadi")
		return
	}
	defer file.Close()

	// Hajm tekshiruv
	if header.Size > MaxImageSize {
		response.BadRequest(c, "MEDIA_TOO_LARGE",
			fmt.Sprintf("Rasm hajmi %d MB dan oshmasligi kerak", MaxImageSize/(1024*1024)))
		return
	}

	// MIME type aniqlash
	buf := make([]byte, 512)
	n, _ := file.Read(buf)
	contentType := http.DetectContentType(buf[:n])
	file.Seek(0, 0) // position qaytarish

	if !AllowedTypes[contentType] {
		response.BadRequest(c, "MEDIA_INVALID_TYPE",
			"Noto'g'ri fayl formati. Ruxsat: JPEG, PNG, WebP")
		return
	}

	result, err := h.service.UploadImage(
		c.Request.Context(),
		bucket,
		header.Filename,
		contentType,
		file,
		header.Size,
	)
	if err != nil {
		response.InternalError(c)
		return
	}

	response.Created(c, result)
}
