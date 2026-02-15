package listing

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/redis/go-redis/v9"
	"github.com/rs/zerolog/log"
)

// Xatolar
var (
	ErrNotFound           = errors.New("e'lon topilmadi")
	ErrForbidden          = errors.New("sizda ruxsat yo'q")
	ErrDailyLimitExceeded = errors.New("kunlik limit: max 10 ta e'lon")
	ErrInvalidTransition  = errors.New("noto'g'ri status o'tishi")
)

// Ruxsat etilgan status o'tishlar
var allowedTransitions = map[string][]string{
	"pending":  {"active", "rejected"},
	"active":   {"rented", "archived", "expired"},
	"rejected": {"pending"},
	"rented":   {"active"},
}

const maxDailyListings = 10

// Service — e'lon biznes logikasi
type Service struct {
	repo  *Repository
	redis *redis.Client
}

// NewService — yangi Service yaratish
func NewService(repo *Repository, redis *redis.Client) *Service {
	return &Service{repo: repo, redis: redis}
}

// CreateListing — yangi e'lon yaratish (kunlik limit tekshiruvli)
func (s *Service) CreateListing(ctx context.Context, userID string, req *CreateListingRequest) (*ListingResponse, error) {
	// Kunlik limit tekshiruv
	if err := s.checkDailyLimit(ctx, userID); err != nil {
		return nil, err
	}

	listing, err := s.repo.Create(ctx, userID, req)
	if err != nil {
		log.Error().Err(err).Str("user_id", userID).Msg("Failed to create listing")
		return nil, fmt.Errorf("create listing: %w", err)
	}

	return listing.ToResponse([]ImageResponse{}), nil
}

// GetListing — bitta e'lonni olish (views oshirish bilan)
func (s *Service) GetListing(ctx context.Context, id string) (*ListingResponse, error) {
	listing, err := s.repo.FindByID(ctx, id)
	if err != nil {
		return nil, fmt.Errorf("get listing: %w", err)
	}
	if listing == nil {
		return nil, ErrNotFound
	}

	// Views count oshirish (goroutine — bloklamas)
	go s.repo.IncrementViews(context.Background(), id)

	images := s.getImagesResponse(ctx, id)
	return listing.ToResponse(images), nil
}

// GetListings — e'lonlar ro'yxati (filtrli)
func (s *Service) GetListings(ctx context.Context, filter *ListingsFilter) ([]*ListingListItem, int, error) {
	listings, total, err := s.repo.FindAll(ctx, filter)
	if err != nil {
		return nil, 0, fmt.Errorf("get listings: %w", err)
	}

	items := make([]*ListingListItem, len(listings))
	for i, l := range listings {
		images := s.getImagesResponse(ctx, l.ID.String())
		items[i] = l.ToListItem(images)
	}

	return items, total, nil
}

// GetMyListings — foydalanuvchining o'z e'lonlari
func (s *Service) GetMyListings(ctx context.Context, userID, status string, page, perPage int) ([]*ListingListItem, int, error) {
	listings, total, err := s.repo.FindByUserID(ctx, userID, status, page, perPage)
	if err != nil {
		return nil, 0, fmt.Errorf("get my listings: %w", err)
	}

	items := make([]*ListingListItem, len(listings))
	for i, l := range listings {
		images := s.getImagesResponse(ctx, l.ID.String())
		items[i] = l.ToListItem(images)
	}

	return items, total, nil
}

// UpdateListing — e'lonni tahrirlash (faqat owner)
func (s *Service) UpdateListing(ctx context.Context, id, userID string, req *UpdateListingRequest) (*ListingResponse, error) {
	listing, err := s.repo.FindByID(ctx, id)
	if err != nil {
		return nil, fmt.Errorf("find listing: %w", err)
	}
	if listing == nil {
		return nil, ErrNotFound
	}

	// Ownership tekshiruv
	if listing.UserID.String() != userID {
		return nil, ErrForbidden
	}

	updated, err := s.repo.Update(ctx, id, req)
	if err != nil {
		return nil, fmt.Errorf("update listing: %w", err)
	}
	if updated == nil {
		return nil, ErrNotFound
	}

	images := s.getImagesResponse(ctx, id)
	return updated.ToResponse(images), nil
}

// DeleteListing — e'lonni o'chirish (faqat owner)
func (s *Service) DeleteListing(ctx context.Context, id, userID string) error {
	listing, err := s.repo.FindByID(ctx, id)
	if err != nil {
		return fmt.Errorf("find listing: %w", err)
	}
	if listing == nil {
		return ErrNotFound
	}
	if listing.UserID.String() != userID {
		return ErrForbidden
	}

	return s.repo.Delete(ctx, id)
}

// UpdateStatus — e'lon statusini o'zgartirish (faqat owner, transition tekshiruvli)
func (s *Service) UpdateStatus(ctx context.Context, id, userID, newStatus string) error {
	listing, err := s.repo.FindByID(ctx, id)
	if err != nil {
		return fmt.Errorf("find listing: %w", err)
	}
	if listing == nil {
		return ErrNotFound
	}
	if listing.UserID.String() != userID {
		return ErrForbidden
	}

	// Status transition tekshiruv
	allowed, ok := allowedTransitions[listing.Status]
	if !ok {
		return fmt.Errorf("%w: '%s' dan o'tish mumkin emas", ErrInvalidTransition, listing.Status)
	}

	valid := false
	for _, s := range allowed {
		if s == newStatus {
			valid = true
			break
		}
	}
	if !valid {
		return fmt.Errorf("%w: '%s' → '%s' taqiqlangan", ErrInvalidTransition, listing.Status, newStatus)
	}

	return s.repo.UpdateStatus(ctx, id, newStatus)
}

// GetStats — e'lon statistikasi
func (s *Service) GetStats(ctx context.Context, id, userID string) (*StatsResponse, error) {
	listing, err := s.repo.FindByID(ctx, id)
	if err != nil {
		return nil, fmt.Errorf("find listing: %w", err)
	}
	if listing == nil {
		return nil, ErrNotFound
	}
	if listing.UserID.String() != userID {
		return nil, ErrForbidden
	}

	stats, err := s.repo.GetStats(ctx, id)
	if err != nil {
		return nil, fmt.Errorf("get stats: %w", err)
	}

	return &StatsResponse{
		Views:     stats.Views,
		Favorites: stats.Favorites,
		Contacts:  stats.Contacts,
	}, nil
}

// checkDailyLimit — kunlik e'lon limiti tekshiruvi (Redis INCR)
func (s *Service) checkDailyLimit(ctx context.Context, userID string) error {
	key := fmt.Sprintf("rate:listing_create:%s", userID)

	count, err := s.redis.Get(ctx, key).Int()
	if err != nil && err != redis.Nil {
		return fmt.Errorf("check daily limit: %w", err)
	}

	if count >= maxDailyListings {
		return ErrDailyLimitExceeded
	}

	pipe := s.redis.Pipeline()
	pipe.Incr(ctx, key)
	pipe.Expire(ctx, key, 24*time.Hour)
	_, err = pipe.Exec(ctx)
	return err
}

// getImagesResponse — e'lon rasmlarini ImageResponse ga aylantirish
func (s *Service) getImagesResponse(ctx context.Context, listingID string) []ImageResponse {
	images, err := s.repo.FindImagesByListingID(ctx, listingID)
	if err != nil {
		log.Warn().Err(err).Str("listing_id", listingID).Msg("Failed to load images")
		return []ImageResponse{}
	}

	resp := make([]ImageResponse, len(images))
	for i, img := range images {
		resp[i] = ImageResponse{
			ID:        img.ID.String(),
			URL:       img.URL,
			SortOrder: img.SortOrder,
			IsMain:    img.IsMain,
		}
		if img.ThumbnailURL.Valid {
			resp[i].ThumbnailURL = &img.ThumbnailURL.String
		}
	}
	return resp
}
