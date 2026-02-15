package notification

import (
	"database/sql"
	"time"

	"github.com/google/uuid"
)

// Notification — bildirishnoma domain modeli
type Notification struct {
	ID         uuid.UUID      `json:"id" db:"id"`
	UserID     uuid.UUID      `json:"user_id" db:"user_id"`
	Type       string         `json:"type" db:"type"`
	Title      string         `json:"title" db:"title"`
	Body       string         `json:"body" db:"body"`
	RefType    sql.NullString `json:"ref_type,omitempty" db:"ref_type"`
	RefID      *uuid.UUID     `json:"ref_id,omitempty" db:"ref_id"`
	IsRead     bool           `json:"is_read" db:"is_read"`
	ReadAt     sql.NullTime   `json:"read_at,omitempty" db:"read_at"`
	PushSent   bool           `json:"push_sent" db:"push_sent"`
	PushSentAt sql.NullTime   `json:"push_sent_at,omitempty" db:"push_sent_at"`
	CreatedAt  time.Time      `json:"created_at" db:"created_at"`
}

// NotificationListItem — ro'yxat uchun soddalashtirilgan bildirishnoma
type NotificationListItem struct {
	ID        uuid.UUID  `json:"id" db:"id"`
	Type      string     `json:"type" db:"type"`
	Title     string     `json:"title" db:"title"`
	Body      string     `json:"body" db:"body"`
	RefType   *string    `json:"ref_type,omitempty" db:"ref_type"`
	RefID     *uuid.UUID `json:"ref_id,omitempty" db:"ref_id"`
	IsRead    bool       `json:"is_read" db:"is_read"`
	CreatedAt time.Time  `json:"created_at" db:"created_at"`
}

// FCMToken — Firebase Cloud Messaging token
type FCMToken struct {
	ID         uuid.UUID `json:"id" db:"id"`
	UserID     uuid.UUID `json:"user_id" db:"user_id"`
	Token      string    `json:"token" db:"token"`
	DeviceType string    `json:"device_type" db:"device_type"`
	IsActive   bool      `json:"is_active" db:"is_active"`
	CreatedAt  time.Time `json:"created_at" db:"created_at"`
	UpdatedAt  time.Time `json:"updated_at" db:"updated_at"`
}

// CreateNotificationInput — bildirishnoma yaratish uchun input
type CreateNotificationInput struct {
	UserID  string
	Type    string
	Title   string
	Body    string
	RefType string
	RefID   string
}

// RegisterTokenRequest — FCM token ro'yxatdan o'tkazish
type RegisterTokenRequest struct {
	Token      string `json:"token" binding:"required"`
	DeviceType string `json:"device_type"` // android, ios, web
}

// Notification turlari konstantalari
const (
	TypeNewMessage      = "new_message"
	TypeListingApproved = "listing_approved"
	TypeListingRejected = "listing_rejected"
	TypeNewFavorite     = "new_favorite"
	TypePriceDrop       = "price_drop"
	TypeSystem          = "system"
)
