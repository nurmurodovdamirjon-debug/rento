package user

import (
	"database/sql"
	"time"

	"github.com/google/uuid"
)

// User — foydalanuvchi domain modeli (users jadvali)
type User struct {
	ID             uuid.UUID      `json:"id" db:"id"`
	Phone          string         `json:"phone" db:"phone"`
	PhoneVerified  bool           `json:"phone_verified" db:"phone_verified"`
	FullName       sql.NullString `json:"full_name" db:"full_name"`
	Email          sql.NullString `json:"email" db:"email"`
	AvatarURL      sql.NullString `json:"avatar_url" db:"avatar_url"`
	Role           string         `json:"role" db:"role"`
	IDVerified     bool           `json:"id_verified" db:"id_verified"`
	IDDocumentURL  sql.NullString `json:"id_document_url" db:"id_document_url"`
	IDVerifiedAt   sql.NullTime   `json:"id_verified_at" db:"id_verified_at"`
	RatingAvg      float64        `json:"rating_avg" db:"rating_avg"`
	RatingCount    int            `json:"rating_count" db:"rating_count"`
	Subscription   string         `json:"subscription" db:"subscription"`
	SubExpiresAt   sql.NullTime   `json:"sub_expires_at" db:"sub_expires_at"`
	Language       string         `json:"language" db:"language"`
	LastSeenAt     sql.NullTime   `json:"last_seen_at" db:"last_seen_at"`
	CreatedAt      time.Time      `json:"created_at" db:"created_at"`
	UpdatedAt      time.Time      `json:"updated_at" db:"updated_at"`
	IsActive       bool           `json:"is_active" db:"is_active"`
	IsBlocked      bool           `json:"is_blocked" db:"is_blocked"`
}

// ToResponse — domain model → API response (o'z profili)
func (u *User) ToResponse() *UserResponse {
	resp := &UserResponse{
		ID:            u.ID.String(),
		Phone:         u.Phone,
		PhoneVerified: u.PhoneVerified,
		Role:          u.Role,
		IDVerified:    u.IDVerified,
		RatingAvg:     u.RatingAvg,
		RatingCount:   u.RatingCount,
		Subscription:  u.Subscription,
		Language:      u.Language,
		CreatedAt:     u.CreatedAt,
		IsActive:      u.IsActive,
	}

	if u.FullName.Valid {
		resp.FullName = &u.FullName.String
	}
	if u.Email.Valid {
		resp.Email = &u.Email.String
	}
	if u.AvatarURL.Valid {
		resp.AvatarURL = &u.AvatarURL.String
	}
	if u.IDVerifiedAt.Valid {
		resp.IDVerifiedAt = &u.IDVerifiedAt.Time
	}
	if u.SubExpiresAt.Valid {
		resp.SubExpiresAt = &u.SubExpiresAt.Time
	}
	if u.LastSeenAt.Valid {
		resp.LastSeenAt = &u.LastSeenAt.Time
	}

	return resp
}

// ToPublicResponse — domain model → API response (boshqa foydalanuvchi profili)
// Telefon, email va shaxsiy ma'lumotlar ko'rsatilmaydi
func (u *User) ToPublicResponse() *PublicUserResponse {
	resp := &PublicUserResponse{
		ID:          u.ID.String(),
		Role:        u.Role,
		IDVerified:  u.IDVerified,
		RatingAvg:   u.RatingAvg,
		RatingCount: u.RatingCount,
		CreatedAt:   u.CreatedAt,
	}

	if u.FullName.Valid {
		resp.FullName = &u.FullName.String
	}
	if u.AvatarURL.Valid {
		resp.AvatarURL = &u.AvatarURL.String
	}
	if u.LastSeenAt.Valid {
		resp.LastSeenAt = &u.LastSeenAt.Time
	}

	return resp
}
