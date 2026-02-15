package user

import (
	"context"
	"fmt"

	"github.com/rs/zerolog/log"
)

// Service — foydalanuvchi biznes logikasi
type Service struct {
	repo *Repository
}

// NewService — yangi Service yaratish
func NewService(repo *Repository) *Service {
	return &Service{repo: repo}
}

// GetProfile — o'z profilini olish
func (s *Service) GetProfile(ctx context.Context, userID string) (*UserResponse, error) {
	user, err := s.repo.FindByID(ctx, userID)
	if err != nil {
		return nil, fmt.Errorf("get profile: %w", err)
	}
	if user == nil {
		return nil, fmt.Errorf("user not found: %s", userID)
	}
	if user.IsBlocked {
		return nil, fmt.Errorf("user blocked: %s", userID)
	}

	// last_seen yangilash (background — xato bo'lsa ham davom etadi)
	go func() {
		if err := s.repo.UpdateLastSeen(context.Background(), userID); err != nil {
			log.Warn().Err(err).Str("userId", userID).Msg("Failed to update last_seen")
		}
	}()

	return user.ToResponse(), nil
}

// UpdateProfile — profilni yangilash
func (s *Service) UpdateProfile(ctx context.Context, userID string, req *UpdateProfileRequest) (*UserResponse, error) {
	// Avval foydalanuvchi borligini tekshirish
	existing, err := s.repo.FindByID(ctx, userID)
	if err != nil {
		return nil, fmt.Errorf("update profile find: %w", err)
	}
	if existing == nil {
		return nil, fmt.Errorf("user not found: %s", userID)
	}
	if existing.IsBlocked {
		return nil, fmt.Errorf("user blocked: %s", userID)
	}

	// Profilni yangilash
	updated, err := s.repo.Update(ctx, userID, req)
	if err != nil {
		return nil, fmt.Errorf("update profile: %w", err)
	}
	if updated == nil {
		return nil, fmt.Errorf("user not found after update: %s", userID)
	}

	log.Info().
		Str("userId", userID).
		Msg("Profile updated")

	return updated.ToResponse(), nil
}

// GetPublicProfile — boshqa foydalanuvchining ochiq profilini olish
func (s *Service) GetPublicProfile(ctx context.Context, targetUserID string) (*PublicUserResponse, error) {
	user, err := s.repo.FindByID(ctx, targetUserID)
	if err != nil {
		return nil, fmt.Errorf("get public profile: %w", err)
	}
	if user == nil {
		return nil, fmt.Errorf("user not found: %s", targetUserID)
	}

	return user.ToPublicResponse(), nil
}

// ===== ADMIN METHODS =====

// BlockUser — foydalanuvchini bloklash (admin)
func (s *Service) BlockUser(ctx context.Context, targetUserID, adminID string) error {
	user, err := s.repo.FindByID(ctx, targetUserID)
	if err != nil {
		return fmt.Errorf("find user: %w", err)
	}
	if user == nil {
		return fmt.Errorf("user not found: %s", targetUserID)
	}
	if user.IsBlocked {
		return fmt.Errorf("user already blocked: %s", targetUserID)
	}

	if err := s.repo.BlockUser(ctx, targetUserID); err != nil {
		return fmt.Errorf("block user: %w", err)
	}

	log.Info().
		Str("admin", adminID).
		Str("target_user", targetUserID).
		Msg("User blocked by admin")

	return nil
}

// UnblockUser — foydalanuvchini blokdan chiqarish (admin)
func (s *Service) UnblockUser(ctx context.Context, targetUserID, adminID string) error {
	user, err := s.repo.FindByID(ctx, targetUserID)
	if err != nil {
		return fmt.Errorf("find user: %w", err)
	}
	if user == nil {
		return fmt.Errorf("user not found: %s", targetUserID)
	}
	if !user.IsBlocked {
		return fmt.Errorf("user not blocked: %s", targetUserID)
	}

	if err := s.repo.UnblockUser(ctx, targetUserID); err != nil {
		return fmt.Errorf("unblock user: %w", err)
	}

	log.Info().
		Str("admin", adminID).
		Str("target_user", targetUserID).
		Msg("User unblocked by admin")

	return nil
}

// GetAllUsers — barcha foydalanuvchilar (admin)
func (s *Service) GetAllUsers(ctx context.Context, page, perPage int) ([]*UserResponse, int, error) {
	users, total, err := s.repo.GetAllUsers(ctx, page, perPage)
	if err != nil {
		return nil, 0, fmt.Errorf("get all users: %w", err)
	}

	responses := make([]*UserResponse, len(users))
	for i, u := range users {
		responses[i] = u.ToResponse()
	}

	return responses, total, nil
}
