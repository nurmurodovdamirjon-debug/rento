package favorite

import (
	"time"

	"github.com/google/uuid"
)

// Favorite — sevimli domain modeli (favorites jadvali)
type Favorite struct {
	ID        uuid.UUID `json:"id" db:"id"`
	UserID    uuid.UUID `json:"user_id" db:"user_id"`
	ListingID uuid.UUID `json:"listing_id" db:"listing_id"`
	CreatedAt time.Time `json:"created_at" db:"created_at"`
}

// FavoriteListItem — sevimlilar ro'yxatidagi e'lon (listing bilan)
type FavoriteListItem struct {
	FavoriteID uuid.UUID `json:"favorite_id" db:"favorite_id"`
	ListingID  uuid.UUID `json:"listing_id" db:"listing_id"`
	Title      string    `json:"title" db:"title"`
	City       string    `json:"city" db:"city"`
	Price      float64   `json:"price" db:"price"`
	Currency   string    `json:"currency" db:"currency"`
	Rooms      *int16    `json:"rooms,omitempty" db:"rooms"`
	AreaSqm    *float64  `json:"area_sqm,omitempty" db:"area_sqm"`
	ImageURL   *string   `json:"image_url,omitempty" db:"image_url"`
	Status     string    `json:"status" db:"status"`
	IsPremium  bool      `json:"is_premium" db:"is_premium"`
	FavoritedAt time.Time `json:"favorited_at" db:"favorited_at"`
}

// ToggleResponse — toggle natijasi
type ToggleResponse struct {
	IsFavorite bool `json:"is_favorite"`
	Count      int  `json:"favorites_count"`
}
