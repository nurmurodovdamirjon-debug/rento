package user

import "time"

// ===== REQUEST DTOs =====

// UpdateProfileRequest — profil yangilash so'rovi
type UpdateProfileRequest struct {
	FullName *string `json:"full_name" binding:"omitempty,min=2,max=100"`
	Email    *string `json:"email" binding:"omitempty,email"`
	Language *string `json:"language" binding:"omitempty,oneof=uz ru en"`
}

// ===== RESPONSE DTOs =====

// UserResponse — to'liq profil (o'z profili uchun)
type UserResponse struct {
	ID            string     `json:"id"`
	Phone         string     `json:"phone"`
	PhoneVerified bool       `json:"phone_verified"`
	FullName      *string    `json:"full_name"`
	Email         *string    `json:"email"`
	AvatarURL     *string    `json:"avatar_url"`
	Role          string     `json:"role"`
	IDVerified    bool       `json:"id_verified"`
	IDVerifiedAt  *time.Time `json:"id_verified_at,omitempty"`
	RatingAvg     float64    `json:"rating_avg"`
	RatingCount   int        `json:"rating_count"`
	Subscription  string     `json:"subscription"`
	SubExpiresAt  *time.Time `json:"sub_expires_at,omitempty"`
	Language      string     `json:"language"`
	LastSeenAt    *time.Time `json:"last_seen_at,omitempty"`
	CreatedAt     time.Time  `json:"created_at"`
	IsActive      bool       `json:"is_active"`
}

// PublicUserResponse — ochiq profil (boshqa foydalanuvchi uchun)
// Telefon, email va shaxsiy ma'lumotlar ko'rsatilmaydi
type PublicUserResponse struct {
	ID          string     `json:"id"`
	FullName    *string    `json:"full_name"`
	AvatarURL   *string    `json:"avatar_url"`
	Role        string     `json:"role"`
	IDVerified  bool       `json:"id_verified"`
	RatingAvg   float64    `json:"rating_avg"`
	RatingCount int        `json:"rating_count"`
	CreatedAt   time.Time  `json:"created_at"`
	LastSeenAt  *time.Time `json:"last_seen_at,omitempty"`
}
