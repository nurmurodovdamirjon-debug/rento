package favorite

import (
	"context"
	"errors"
	"fmt"

	"github.com/rs/zerolog/log"
)

// Xatolar
var (
	ErrListingNotFound = errors.New("e'lon topilmadi")
	ErrAlreadyFavorite = errors.New("allaqachon sevimlilarda")
)

// Service — favorites biznes logikasi
type Service struct {
	repo *Repository
}

// NewService — yangi Service yaratish
func NewService(repo *Repository) *Service {
	return &Service{repo: repo}
}

// Toggle — sevimlilarga qo'shish/olib tashlash (toggle)
func (s *Service) Toggle(ctx context.Context, userID, listingID string) (*ToggleResponse, error) {
	exists, err := s.repo.Exists(ctx, userID, listingID)
	if err != nil {
		log.Error().Err(err).Str("user_id", userID).Str("listing_id", listingID).Msg("Failed to check favorite")
		return nil, fmt.Errorf("toggle favorite: %w", err)
	}

	if exists {
		// Olib tashlash
		if err := s.repo.Remove(ctx, userID, listingID); err != nil {
			return nil, fmt.Errorf("remove favorite: %w", err)
		}
	} else {
		// Qo'shish
		if err := s.repo.Add(ctx, userID, listingID); err != nil {
			return nil, fmt.Errorf("add favorite: %w", err)
		}
	}

	count, err := s.repo.GetFavoritesCount(ctx, listingID)
	if err != nil {
		count = 0
	}

	log.Info().
		Str("user_id", userID).
		Str("listing_id", listingID).
		Bool("is_favorite", !exists).
		Msg("Favorite toggled")

	return &ToggleResponse{
		IsFavorite: !exists,
		Count:      count,
	}, nil
}

// GetUserFavorites — foydalanuvchining sevimlilar ro'yxati
func (s *Service) GetUserFavorites(ctx context.Context, userID string, page, perPage int) ([]*FavoriteListItem, int, error) {
	if page < 1 {
		page = 1
	}
	if perPage < 1 || perPage > 50 {
		perPage = 20
	}

	return s.repo.GetUserFavorites(ctx, userID, page, perPage)
}

// IsFavorite — e'lon sevimlilardami tekshirish
func (s *Service) IsFavorite(ctx context.Context, userID, listingID string) (bool, error) {
	return s.repo.Exists(ctx, userID, listingID)
}

// GetUserFavoriteIDs — foydalanuvchining sevimli e'lon IDlari (batch)
func (s *Service) GetUserFavoriteIDs(ctx context.Context, userID string) ([]string, error) {
	return s.repo.GetUserFavoriteIDs(ctx, userID)
}
