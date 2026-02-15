package notification

import (
	"context"
	"fmt"

	"github.com/rs/zerolog/log"
)

// Service — notifications biznes logikasi
type Service struct {
	repo *Repository
}

// NewService — yangi Service yaratish
func NewService(repo *Repository) *Service {
	return &Service{repo: repo}
}

// CreateNotification — yangi bildirishnoma yaratish (+ push agar token bor)
func (s *Service) CreateNotification(ctx context.Context, input *CreateNotificationInput) (*Notification, error) {
	n, err := s.repo.Create(ctx, input)
	if err != nil {
		log.Error().Err(err).Str("user_id", input.UserID).Str("type", input.Type).Msg("Failed to create notification")
		return nil, fmt.Errorf("create notification: %w", err)
	}

	// Push notification yuborish (async — bloklamas)
	go s.sendPush(context.Background(), n)

	return n, nil
}

// GetNotifications — foydalanuvchi bildirishnomalar ro'yxati
func (s *Service) GetNotifications(ctx context.Context, userID string, page, perPage int) ([]*NotificationListItem, int, error) {
	if page < 1 {
		page = 1
	}
	if perPage < 1 || perPage > 50 {
		perPage = 20
	}

	return s.repo.GetByUser(ctx, userID, page, perPage)
}

// MarkAsRead — bitta bildirishnomani o'qildi belgilash
func (s *Service) MarkAsRead(ctx context.Context, id, userID string) error {
	return s.repo.MarkAsRead(ctx, id, userID)
}

// MarkAllAsRead — barcha bildirishnomalarni o'qildi belgilash
func (s *Service) MarkAllAsRead(ctx context.Context, userID string) (int, error) {
	return s.repo.MarkAllAsRead(ctx, userID)
}

// GetUnreadCount — o'qilmagan soni
func (s *Service) GetUnreadCount(ctx context.Context, userID string) (int, error) {
	return s.repo.GetUnreadCount(ctx, userID)
}

// RegisterFCMToken — FCM tokenni saqlash
func (s *Service) RegisterFCMToken(ctx context.Context, userID, token, deviceType string) error {
	if deviceType == "" {
		deviceType = "android"
	}
	return s.repo.SaveFCMToken(ctx, userID, token, deviceType)
}

// UnregisterFCMToken — FCM tokenni o'chirish
func (s *Service) UnregisterFCMToken(ctx context.Context, userID, token string) error {
	return s.repo.DeleteFCMToken(ctx, userID, token)
}

// sendPush — Firebase Cloud Messaging orqali push yuborish
func (s *Service) sendPush(ctx context.Context, n *Notification) {
	tokens, err := s.repo.GetUserFCMTokens(ctx, n.UserID.String())
	if err != nil {
		log.Error().Err(err).Str("user_id", n.UserID.String()).Msg("Failed to get FCM tokens")
		return
	}

	if len(tokens) == 0 {
		return
	}

	// TODO: Firebase Admin SDK bilan push yuborish
	// Hozircha log qilamiz — Sprint 7 da to'liq integratsiya
	log.Info().
		Str("notification_id", n.ID.String()).
		Str("user_id", n.UserID.String()).
		Int("token_count", len(tokens)).
		Str("title", n.Title).
		Msg("Push notification queued (FCM stub)")

	// Push yuborildi deb belgilash
	if err := s.repo.MarkPushSent(ctx, n.ID.String()); err != nil {
		log.Error().Err(err).Str("notification_id", n.ID.String()).Msg("Failed to mark push sent")
	}
}

// NotifyNewMessage — yangi xabar bildirishnomasi (chat service dan chaqiriladi)
func (s *Service) NotifyNewMessage(ctx context.Context, userID, senderName, chatRoomID string) {
	_, err := s.CreateNotification(ctx, &CreateNotificationInput{
		UserID:  userID,
		Type:    TypeNewMessage,
		Title:   "Yangi xabar",
		Body:    fmt.Sprintf("%s sizga xabar yozdi", senderName),
		RefType: "chat_room",
		RefID:   chatRoomID,
	})
	if err != nil {
		log.Error().Err(err).Msg("Failed to notify new message")
	}
}

// NotifyListingApproved — e'lon tasdiqlandi bildirishnomasi
func (s *Service) NotifyListingApproved(ctx context.Context, userID, listingID, listingTitle string) {
	_, err := s.CreateNotification(ctx, &CreateNotificationInput{
		UserID:  userID,
		Type:    TypeListingApproved,
		Title:   "E'lon tasdiqlandi ✅",
		Body:    fmt.Sprintf("«%s» e'loningiz tasdiqlandi va endi barchaga ko'rinadi", listingTitle),
		RefType: "listing",
		RefID:   listingID,
	})
	if err != nil {
		log.Error().Err(err).Msg("Failed to notify listing approved")
	}
}

// NotifyNewFavorite — kimdir e'lonni sevimlilariga qo'shdi
func (s *Service) NotifyNewFavorite(ctx context.Context, listingOwnerID, listingID, listingTitle string) {
	_, err := s.CreateNotification(ctx, &CreateNotificationInput{
		UserID:  listingOwnerID,
		Type:    TypeNewFavorite,
		Title:   "Yangi sevimli ❤️",
		Body:    fmt.Sprintf("Kimdir «%s» e'loningizni sevimlilarga qo'shdi", listingTitle),
		RefType: "listing",
		RefID:   listingID,
	})
	if err != nil {
		log.Error().Err(err).Msg("Failed to notify new favorite")
	}
}
