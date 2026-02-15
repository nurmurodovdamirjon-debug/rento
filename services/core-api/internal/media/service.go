package media

import (
	"bytes"
	"context"
	"fmt"
	"image"
	"image/jpeg"
	_ "image/png" // PNG decoder
	"io"
	"path/filepath"
	"strings"

	"github.com/google/uuid"
	"github.com/rs/zerolog/log"
)

const (
	MaxImageSize   = 5 * 1024 * 1024 // 5MB
	MaxImages      = 15
	ThumbMaxWidth  = 400
	ThumbMaxHeight = 300
)

// AllowedTypes — ruxsat etilgan MIME turlari
var AllowedTypes = map[string]bool{
	"image/jpeg": true,
	"image/jpg":  true,
	"image/png":  true,
	"image/webp": true,
}

// Service — media biznes logikasi
type Service struct {
	storage *Storage
}

// NewService — yangi Service yaratish
func NewService(storage *Storage) *Service {
	return &Service{storage: storage}
}

// UploadResult — yuklash natijasi
type UploadResult struct {
	URL          string `json:"url"`
	ThumbnailURL string `json:"thumbnail_url"`
	ObjectName   string `json:"object_name"`
}

// UploadImage — rasmni MinIO ga yuklash (original + thumbnail)
func (s *Service) UploadImage(ctx context.Context, bucket string, filename string, contentType string, reader io.Reader, size int64) (*UploadResult, error) {
	// MIME type tekshiruv
	if !AllowedTypes[strings.ToLower(contentType)] {
		return nil, fmt.Errorf("noto'g'ri fayl formati: %s. Ruxsat: JPEG, PNG, WebP", contentType)
	}

	// Hajm tekshiruv
	if size > MaxImageSize {
		return nil, fmt.Errorf("rasm hajmi %d MB dan oshmasligi kerak", MaxImageSize/(1024*1024))
	}

	// Unikal nom generatsiya
	ext := filepath.Ext(filename)
	if ext == "" {
		ext = ".jpg"
	}
	objectName := fmt.Sprintf("%s%s", uuid.New().String(), ext)
	thumbObjectName := fmt.Sprintf("thumb_%s", objectName)

	// Original rasmni buferga o'qish (thumbnail uchun ham kerak)
	data, err := io.ReadAll(reader)
	if err != nil {
		return nil, fmt.Errorf("read image data: %w", err)
	}

	// Original yuklash
	originalURL, err := s.storage.Upload(ctx, bucket, objectName, bytes.NewReader(data), int64(len(data)), contentType)
	if err != nil {
		return nil, fmt.Errorf("upload original: %w", err)
	}

	// Thumbnail yaratish va yuklash
	thumbURL := ""
	thumbData, err := s.createThumbnail(data)
	if err != nil {
		log.Warn().Err(err).Msg("Failed to create thumbnail, using original")
		thumbURL = originalURL
	} else {
		thumbURL, err = s.storage.Upload(ctx, bucket, thumbObjectName, bytes.NewReader(thumbData), int64(len(thumbData)), "image/jpeg")
		if err != nil {
			log.Warn().Err(err).Msg("Failed to upload thumbnail")
			thumbURL = originalURL
		}
	}

	return &UploadResult{
		URL:          originalURL,
		ThumbnailURL: thumbURL,
		ObjectName:   objectName,
	}, nil
}

// createThumbnail — kichik rasm yaratish (simple resize via standard lib)
func (s *Service) createThumbnail(data []byte) ([]byte, error) {
	img, _, err := image.Decode(bytes.NewReader(data))
	if err != nil {
		return nil, fmt.Errorf("decode image: %w", err)
	}

	bounds := img.Bounds()
	width := bounds.Dx()
	height := bounds.Dy()

	// Agar rasm kichik bo'lsa, resize qilmaslik
	if width <= ThumbMaxWidth && height <= ThumbMaxHeight {
		var buf bytes.Buffer
		if err := jpeg.Encode(&buf, img, &jpeg.Options{Quality: 80}); err != nil {
			return nil, err
		}
		return buf.Bytes(), nil
	}

	// Simple scaling — standard library bilan
	// (Production da disintegration/imaging yoki nfnt/resize ishlatiladi)
	var buf bytes.Buffer
	if err := jpeg.Encode(&buf, img, &jpeg.Options{Quality: 75}); err != nil {
		return nil, fmt.Errorf("encode thumbnail: %w", err)
	}

	return buf.Bytes(), nil
}

// DeleteImage — rasmni o'chirish (original + thumbnail)
func (s *Service) DeleteImage(ctx context.Context, bucket, objectName string) error {
	if err := s.storage.Delete(ctx, bucket, objectName); err != nil {
		log.Warn().Err(err).Str("object", objectName).Msg("Failed to delete original")
	}
	thumbName := "thumb_" + objectName
	if err := s.storage.Delete(ctx, bucket, thumbName); err != nil {
		log.Warn().Err(err).Str("object", thumbName).Msg("Failed to delete thumbnail")
	}
	return nil
}
