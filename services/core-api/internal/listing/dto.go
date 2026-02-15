package listing

import "time"

// ===== REQUEST DTOs =====

// CreateListingRequest — yangi e'lon yaratish
type CreateListingRequest struct {
	Type             string   `json:"type" binding:"required,oneof=apartment house office shop warehouse"`
	DealType         string   `json:"deal_type" binding:"omitempty,oneof=rent daily"`
	City             string   `json:"city" binding:"required,min=2,max=100"`
	District         *string  `json:"district,omitempty" binding:"omitempty,max=100"`
	Address          *string  `json:"address,omitempty" binding:"omitempty,max=300"`
	Landmark         *string  `json:"landmark,omitempty" binding:"omitempty,max=200"`
	Latitude         *float64 `json:"latitude,omitempty"`
	Longitude        *float64 `json:"longitude,omitempty"`
	Rooms            *int     `json:"rooms,omitempty" binding:"omitempty,min=1,max=20"`
	Floor            *int     `json:"floor,omitempty" binding:"omitempty,min=-2,max=100"`
	TotalFloors      *int     `json:"total_floors,omitempty" binding:"omitempty,min=1,max=100"`
	AreaSqm          *float64 `json:"area_sqm,omitempty" binding:"omitempty,min=1"`
	Price            float64  `json:"price" binding:"required,min=0"`
	Currency         string   `json:"currency" binding:"required,oneof=UZS USD"`
	PriceNegotiable  *bool    `json:"price_negotiable,omitempty"`
	HasFurniture     *bool    `json:"has_furniture,omitempty"`
	HasAppliances    *bool    `json:"has_appliances,omitempty"`
	HasInternet      *bool    `json:"has_internet,omitempty"`
	HasParking       *bool    `json:"has_parking,omitempty"`
	HasConditioner   *bool    `json:"has_conditioner,omitempty"`
	AllowsPets       *bool    `json:"allows_pets,omitempty"`
	AllowsChildren   *bool    `json:"allows_children,omitempty"`
	UtilitiesIncl    *bool    `json:"utilities_included,omitempty"`
	DepositAmount    *float64 `json:"deposit_amount,omitempty" binding:"omitempty,min=0"`
	Title            string   `json:"title" binding:"required,min=5,max=200"`
	Description      *string  `json:"description,omitempty" binding:"omitempty,min=20,max=5000"`
}

// UpdateListingRequest — e'lonni tahrirlash (partial, COALESCE)
type UpdateListingRequest struct {
	Type             *string  `json:"type,omitempty" binding:"omitempty,oneof=apartment house office shop warehouse"`
	DealType         *string  `json:"deal_type,omitempty" binding:"omitempty,oneof=rent daily"`
	City             *string  `json:"city,omitempty" binding:"omitempty,min=2,max=100"`
	District         *string  `json:"district,omitempty" binding:"omitempty,max=100"`
	Address          *string  `json:"address,omitempty" binding:"omitempty,max=300"`
	Landmark         *string  `json:"landmark,omitempty" binding:"omitempty,max=200"`
	Latitude         *float64 `json:"latitude,omitempty"`
	Longitude        *float64 `json:"longitude,omitempty"`
	Rooms            *int     `json:"rooms,omitempty" binding:"omitempty,min=1,max=20"`
	Floor            *int     `json:"floor,omitempty" binding:"omitempty,min=-2,max=100"`
	TotalFloors      *int     `json:"total_floors,omitempty" binding:"omitempty,min=1,max=100"`
	AreaSqm          *float64 `json:"area_sqm,omitempty" binding:"omitempty,min=1"`
	Price            *float64 `json:"price,omitempty" binding:"omitempty,min=0"`
	Currency         *string  `json:"currency,omitempty" binding:"omitempty,oneof=UZS USD"`
	PriceNegotiable  *bool    `json:"price_negotiable,omitempty"`
	HasFurniture     *bool    `json:"has_furniture,omitempty"`
	HasAppliances    *bool    `json:"has_appliances,omitempty"`
	HasInternet      *bool    `json:"has_internet,omitempty"`
	HasParking       *bool    `json:"has_parking,omitempty"`
	HasConditioner   *bool    `json:"has_conditioner,omitempty"`
	AllowsPets       *bool    `json:"allows_pets,omitempty"`
	AllowsChildren   *bool    `json:"allows_children,omitempty"`
	UtilitiesIncl    *bool    `json:"utilities_included,omitempty"`
	DepositAmount    *float64 `json:"deposit_amount,omitempty" binding:"omitempty,min=0"`
	Title            *string  `json:"title,omitempty" binding:"omitempty,min=5,max=200"`
	Description      *string  `json:"description,omitempty" binding:"omitempty,min=20,max=5000"`
}

// UpdateStatusRequest — e'lon statusini o'zgartirish
type UpdateStatusRequest struct {
	Status string `json:"status" binding:"required,oneof=active rented archived"`
}

// ListingsFilter — ro'yxat filtri (query params)
type ListingsFilter struct {
	City         string  `form:"city"`
	District     string  `form:"district"`
	Type         string  `form:"type"`
	DealType     string  `form:"deal_type"`
	RoomsMin     *int    `form:"rooms_min"`
	RoomsMax     *int    `form:"rooms_max"`
	PriceMin     *float64 `form:"price_min"`
	PriceMax     *float64 `form:"price_max"`
	Currency     string  `form:"currency"`
	HasFurniture *bool   `form:"has_furniture"`
	HasParking   *bool   `form:"has_parking"`
	AllowsPets   *bool   `form:"allows_pets"`
	Sort         string  `form:"sort"`
	Page         int     `form:"page,default=1"`
	PerPage      int     `form:"per_page,default=20"`
}

// ===== RESPONSE DTOs =====

// ListingResponse — to'liq e'lon javobi
type ListingResponse struct {
	ID              string          `json:"id"`
	UserID          string          `json:"user_id"`
	Type            string          `json:"type"`
	DealType        string          `json:"deal_type"`
	City            string          `json:"city"`
	District        *string         `json:"district,omitempty"`
	Address         *string         `json:"address,omitempty"`
	Landmark        *string         `json:"landmark,omitempty"`
	Latitude        *float64        `json:"latitude,omitempty"`
	Longitude       *float64        `json:"longitude,omitempty"`
	Rooms           *int            `json:"rooms,omitempty"`
	Floor           *int            `json:"floor,omitempty"`
	TotalFloors     *int            `json:"total_floors,omitempty"`
	AreaSqm         *float64        `json:"area_sqm,omitempty"`
	Price           float64         `json:"price"`
	Currency        string          `json:"currency"`
	PriceNegotiable bool            `json:"price_negotiable"`
	HasFurniture    bool            `json:"has_furniture"`
	HasAppliances   bool            `json:"has_appliances"`
	HasInternet     bool            `json:"has_internet"`
	HasParking      bool            `json:"has_parking"`
	HasConditioner  bool            `json:"has_conditioner"`
	AllowsPets      bool            `json:"allows_pets"`
	AllowsChildren  bool            `json:"allows_children"`
	UtilitiesIncl   bool            `json:"utilities_included"`
	DepositAmount   *float64        `json:"deposit_amount,omitempty"`
	Status          string          `json:"status"`
	RejectionReason *string         `json:"rejection_reason,omitempty"`
	IsPremium       bool            `json:"is_premium"`
	PremiumUntil    *time.Time      `json:"premium_until,omitempty"`
	ViewsCount      int             `json:"views_count"`
	FavoritesCount  int             `json:"favorites_count"`
	ContactsCount   int             `json:"contacts_count"`
	Title           string          `json:"title"`
	Description     *string         `json:"description,omitempty"`
	Images          []ImageResponse `json:"images"`
	PublishedAt     *time.Time      `json:"published_at,omitempty"`
	ExpiresAt       *time.Time      `json:"expires_at,omitempty"`
	CreatedAt       time.Time       `json:"created_at"`
	UpdatedAt       time.Time       `json:"updated_at"`
}

// ListingListItem — ro'yxat uchun qisqa e'lon
type ListingListItem struct {
	ID              string          `json:"id"`
	Type            string          `json:"type"`
	DealType        string          `json:"deal_type"`
	City            string          `json:"city"`
	District        *string         `json:"district,omitempty"`
	Address         *string         `json:"address,omitempty"`
	Rooms           *int            `json:"rooms,omitempty"`
	Floor           *int            `json:"floor,omitempty"`
	TotalFloors     *int            `json:"total_floors,omitempty"`
	AreaSqm         *float64        `json:"area_sqm,omitempty"`
	Price           float64         `json:"price"`
	Currency        string          `json:"currency"`
	PriceNegotiable bool            `json:"price_negotiable"`
	HasFurniture    bool            `json:"has_furniture"`
	HasInternet     bool            `json:"has_internet"`
	IsPremium       bool            `json:"is_premium"`
	ViewsCount      int             `json:"views_count"`
	FavoritesCount  int             `json:"favorites_count"`
	Title           string          `json:"title"`
	Images          []ImageResponse `json:"images"`
	PublishedAt     *time.Time      `json:"published_at,omitempty"`
	CreatedAt       time.Time       `json:"created_at"`
}

// ImageResponse — rasm javobi
type ImageResponse struct {
	ID           string  `json:"id"`
	URL          string  `json:"url"`
	ThumbnailURL *string `json:"thumbnail_url,omitempty"`
	SortOrder    int     `json:"sort_order"`
	IsMain       bool    `json:"is_main"`
}

// NearbyFilter — yaqin atrofdagi e'lonlar filtri (PostGIS)
type NearbyFilter struct {
	Lat      float64 `form:"lat" binding:"required"`
	Lng      float64 `form:"lng" binding:"required"`
	RadiusKm int     `form:"radius_km,default=5"`
	Type     string  `form:"type"`
	DealType string  `form:"deal_type"`
	Page     int     `form:"page,default=1"`
	PerPage  int     `form:"per_page,default=20"`
}

// NearbyListItem — yaqinidagi e'lon (masofa bilan)
type NearbyListItem struct {
	ListingListItem
	Latitude       *float64 `json:"latitude,omitempty"`
	Longitude      *float64 `json:"longitude,omitempty"`
	DistanceMeters float64  `json:"distance_meters"`
}

// StatsResponse — statistika javobi
type StatsResponse struct {
	Views     int `json:"views"`
	Favorites int `json:"favorites"`
	Contacts  int `json:"contacts"`
}
