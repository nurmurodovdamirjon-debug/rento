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
	repo       *Repository
	searchRepo *SearchRepo
	redis      *redis.Client
}

// NewService — yangi Service yaratish
func NewService(repo *Repository, searchRepo *SearchRepo, redis *redis.Client) *Service {
	return &Service{repo: repo, searchRepo: searchRepo, redis: redis}
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

	// ES ga sync (goroutine — bloklamas)
	go s.syncToES(context.Background(), listing)

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

// GetListings — e'lonlar ro'yxati (filtrli, batch images — N+1 fix)
func (s *Service) GetListings(ctx context.Context, filter *ListingsFilter) ([]*ListingListItem, int, error) {
	listings, total, err := s.repo.FindAll(ctx, filter)
	if err != nil {
		return nil, 0, fmt.Errorf("get listings: %w", err)
	}

	// Batch images olish (N+1 fix)
	ids := make([]string, len(listings))
	for i, l := range listings {
		ids[i] = l.ID.String()
	}
	imagesMap, err := s.repo.FindImagesByListingIDs(ctx, ids)
	if err != nil {
		log.Warn().Err(err).Msg("Failed to batch load images")
		imagesMap = map[string][]ListingImage{}
	}

	items := make([]*ListingListItem, len(listings))
	for i, l := range listings {
		lid := l.ID.String()
		images := s.imagesToResponse(imagesMap[lid])
		items[i] = l.ToListItem(images)
	}

	return items, total, nil
}

// GetMyListings — foydalanuvchining o'z e'lonlari (batch images — N+1 fix)
func (s *Service) GetMyListings(ctx context.Context, userID, status string, page, perPage int) ([]*ListingListItem, int, error) {
	listings, total, err := s.repo.FindByUserID(ctx, userID, status, page, perPage)
	if err != nil {
		return nil, 0, fmt.Errorf("get my listings: %w", err)
	}

	ids := make([]string, len(listings))
	for i, l := range listings {
		ids[i] = l.ID.String()
	}
	imagesMap, err := s.repo.FindImagesByListingIDs(ctx, ids)
	if err != nil {
		log.Warn().Err(err).Msg("Failed to batch load images")
		imagesMap = map[string][]ListingImage{}
	}

	items := make([]*ListingListItem, len(listings))
	for i, l := range listings {
		lid := l.ID.String()
		images := s.imagesToResponse(imagesMap[lid])
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

	// ES ga sync (goroutine — bloklamas)
	go s.syncToES(context.Background(), updated)

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

	if err := s.repo.Delete(ctx, id); err != nil {
		return err
	}

	// ES dan o'chirish (goroutine — bloklamas)
	go s.deleteFromES(context.Background(), id)

	return nil
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
	return s.imagesToResponse(images)
}

// imagesToResponse — ListingImage slice → ImageResponse slice (batch uchun helper)
func (s *Service) imagesToResponse(images []ListingImage) []ImageResponse {
	if images == nil {
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

// ===== SEARCH + NEARBY =====

// SearchListings — Elasticsearch orqali qidiruv
func (s *Service) SearchListings(ctx context.Context, filter *SearchFilter) ([]*ListingListItem, int, error) {
	if s.searchRepo == nil {
		return nil, 0, fmt.Errorf("search not available")
	}

	result, err := s.searchRepo.Search(ctx, filter)
	if err != nil {
		return nil, 0, fmt.Errorf("search listings: %w", err)
	}

	if len(result.IDs) == 0 {
		return []*ListingListItem{}, result.Total, nil
	}

	// DB dan to'liq ma'lumotlarni olish (ES tartibi bilan)
	listings, err := s.repo.FindByIDs(ctx, result.IDs)
	if err != nil {
		return nil, 0, fmt.Errorf("find listings by ids: %w", err)
	}

	ids := make([]string, len(listings))
	for i, l := range listings {
		ids[i] = l.ID.String()
	}
	imagesMap, _ := s.repo.FindImagesByListingIDs(ctx, ids)
	if imagesMap == nil {
		imagesMap = map[string][]ListingImage{}
	}

	items := make([]*ListingListItem, len(listings))
	for i, l := range listings {
		lid := l.ID.String()
		images := s.imagesToResponse(imagesMap[lid])
		items[i] = l.ToListItem(images)
	}

	return items, result.Total, nil
}

// GetNearby — yaqin atrofdagi e'lonlar (PostGIS, batch images)
func (s *Service) GetNearby(ctx context.Context, filter *NearbyFilter) ([]*NearbyListItem, int, error) {
	results, total, err := s.repo.FindNearby(ctx, filter)
	if err != nil {
		return nil, 0, fmt.Errorf("get nearby: %w", err)
	}

	ids := make([]string, len(results))
	for i, r := range results {
		ids[i] = r.ID.String()
	}
	imagesMap, _ := s.repo.FindImagesByListingIDs(ctx, ids)
	if imagesMap == nil {
		imagesMap = map[string][]ListingImage{}
	}

	items := make([]*NearbyListItem, len(results))
	for i, r := range results {
		lid := r.ID.String()
		images := s.imagesToResponse(imagesMap[lid])
		item := &NearbyListItem{
			ListingListItem: *r.Listing.ToListItem(images),
			DistanceMeters:  r.DistanceMeters,
		}
		if r.Latitude.Valid {
			item.Latitude = &r.Latitude.Float64
		}
		if r.Longitude.Valid {
			item.Longitude = &r.Longitude.Float64
		}
		items[i] = item
	}

	return items, total, nil
}

// ===== ES SYNC =====

// syncToES — e'lonni Elasticsearch ga sinxronlash
func (s *Service) syncToES(ctx context.Context, listing *Listing) {
	if s.searchRepo == nil {
		return
	}

	images, _ := s.repo.FindImagesByListingID(ctx, listing.ID.String())
	doc := ListingToDocument(listing, len(images))

	if err := s.searchRepo.IndexListing(ctx, doc); err != nil {
		log.Warn().Err(err).Str("listing_id", listing.ID.String()).Msg("Failed to sync listing to ES")
	}
}

// deleteFromES — e'lonni Elasticsearch dan o'chirish
func (s *Service) deleteFromES(ctx context.Context, id string) {
	if s.searchRepo == nil {
		return
	}
	if err := s.searchRepo.DeleteDocument(ctx, id); err != nil {
		log.Warn().Err(err).Str("listing_id", id).Msg("Failed to delete listing from ES")
	}
}

// ===== ADMIN MODERATION =====

// AdminUpdateStatus — admin tomonidan e'lon statusini o'zgartirish (ownership tekshiruvsiz)
func (s *Service) AdminUpdateStatus(ctx context.Context, id, adminID, newStatus string) error {
	listing, err := s.repo.FindByID(ctx, id)
	if err != nil {
		return fmt.Errorf("find listing: %w", err)
	}
	if listing == nil {
		return ErrNotFound
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

	if err := s.repo.UpdateStatus(ctx, id, newStatus); err != nil {
		return fmt.Errorf("update status: %w", err)
	}

	log.Info().
		Str("listing_id", id).
		Str("admin_id", adminID).
		Str("old_status", listing.Status).
		Str("new_status", newStatus).
		Msg("Admin updated listing status")

	return nil
}

// AdminGetPendingListings — admin uchun tasdiqlash kutayotgan e'lonlar (batch images)
func (s *Service) AdminGetPendingListings(ctx context.Context, page, perPage int) ([]*ListingListItem, int, error) {
	listings, total, err := s.repo.FindByStatus(ctx, "pending", page, perPage)
	if err != nil {
		return nil, 0, fmt.Errorf("get pending listings: %w", err)
	}

	ids := make([]string, len(listings))
	for i, l := range listings {
		ids[i] = l.ID.String()
	}
	imagesMap, err := s.repo.FindImagesByListingIDs(ctx, ids)
	if err != nil {
		log.Warn().Err(err).Msg("Failed to batch load images")
		imagesMap = map[string][]ListingImage{}
	}

	items := make([]*ListingListItem, len(listings))
	for i, l := range listings {
		lid := l.ID.String()
		images := s.imagesToResponse(imagesMap[lid])
		items[i] = l.ToListItem(images)
	}

	return items, total, nil
}
