package listing

import (
	"database/sql"
	"time"

	"github.com/google/uuid"
)

// Listing — e'lon domain modeli (listings jadvali)
type Listing struct {
	ID               uuid.UUID       `json:"id" db:"id"`
	UserID           uuid.UUID       `json:"user_id" db:"user_id"`
	Type             string          `json:"type" db:"type"`
	DealType         string          `json:"deal_type" db:"deal_type"`
	City             string          `json:"city" db:"city"`
	District         sql.NullString  `json:"district" db:"district"`
	Address          sql.NullString  `json:"address" db:"address"`
	Landmark         sql.NullString  `json:"landmark" db:"landmark"`
	Latitude         sql.NullFloat64 `json:"latitude" db:"latitude"`
	Longitude        sql.NullFloat64 `json:"longitude" db:"longitude"`
	Rooms            sql.NullInt16   `json:"rooms" db:"rooms"`
	Floor            sql.NullInt16   `json:"floor" db:"floor"`
	TotalFloors      sql.NullInt16   `json:"total_floors" db:"total_floors"`
	AreaSqm          sql.NullFloat64 `json:"area_sqm" db:"area_sqm"`
	Price            float64         `json:"price" db:"price"`
	Currency         string          `json:"currency" db:"currency"`
	PriceNegotiable  bool            `json:"price_negotiable" db:"price_negotiable"`
	HasFurniture     bool            `json:"has_furniture" db:"has_furniture"`
	HasAppliances    bool            `json:"has_appliances" db:"has_appliances"`
	HasInternet      bool            `json:"has_internet" db:"has_internet"`
	HasParking       bool            `json:"has_parking" db:"has_parking"`
	HasConditioner   bool            `json:"has_conditioner" db:"has_conditioner"`
	AllowsPets       bool            `json:"allows_pets" db:"allows_pets"`
	AllowsChildren   bool            `json:"allows_children" db:"allows_children"`
	UtilitiesIncl    bool            `json:"utilities_included" db:"utilities_included"`
	DepositAmount    sql.NullFloat64 `json:"deposit_amount" db:"deposit_amount"`
	Status           string          `json:"status" db:"status"`
	RejectionReason  sql.NullString  `json:"rejection_reason" db:"rejection_reason"`
	IsPremium        bool            `json:"is_premium" db:"is_premium"`
	PremiumUntil     sql.NullTime    `json:"premium_until" db:"premium_until"`
	ViewsCount       int             `json:"views_count" db:"views_count"`
	FavoritesCount   int             `json:"favorites_count" db:"favorites_count"`
	ContactsCount    int             `json:"contacts_count" db:"contacts_count"`
	Title            string          `json:"title" db:"title"`
	Description      sql.NullString  `json:"description" db:"description"`
	PublishedAt      sql.NullTime    `json:"published_at" db:"published_at"`
	ExpiresAt        sql.NullTime    `json:"expires_at" db:"expires_at"`
	CreatedAt        time.Time       `json:"created_at" db:"created_at"`
	UpdatedAt        time.Time       `json:"updated_at" db:"updated_at"`
}

// ListingImage — e'lon rasmi (listing_images jadvali)
type ListingImage struct {
	ID           uuid.UUID    `json:"id" db:"id"`
	ListingID    uuid.UUID    `json:"listing_id" db:"listing_id"`
	URL          string       `json:"url" db:"url"`
	ThumbnailURL sql.NullString `json:"thumbnail_url" db:"thumbnail_url"`
	SortOrder    int          `json:"sort_order" db:"sort_order"`
	IsMain       bool         `json:"is_main" db:"is_main"`
	CreatedAt    time.Time    `json:"created_at" db:"created_at"`
}

// ListingStats — e'lon statistikasi (Redis + DB)
type ListingStats struct {
	Views     int `json:"views"`
	Favorites int `json:"favorites"`
	Contacts  int `json:"contacts"`
}

// NearbyListing — yaqin atrofdagi e'lon (masofa bilan, PostGIS natijasi)
type NearbyListing struct {
	Listing
	DistanceMeters float64 `json:"distance_meters" db:"distance_meters"`
}

// ToResponse — domain model → API response (to'liq)
func (l *Listing) ToResponse(images []ImageResponse) *ListingResponse {
	resp := &ListingResponse{
		ID:              l.ID.String(),
		UserID:          l.UserID.String(),
		Type:            l.Type,
		DealType:        l.DealType,
		City:            l.City,
		Price:           l.Price,
		Currency:        l.Currency,
		PriceNegotiable: l.PriceNegotiable,
		HasFurniture:    l.HasFurniture,
		HasAppliances:   l.HasAppliances,
		HasInternet:     l.HasInternet,
		HasParking:      l.HasParking,
		HasConditioner:  l.HasConditioner,
		AllowsPets:      l.AllowsPets,
		AllowsChildren:  l.AllowsChildren,
		UtilitiesIncl:   l.UtilitiesIncl,
		Status:          l.Status,
		IsPremium:       l.IsPremium,
		ViewsCount:      l.ViewsCount,
		FavoritesCount:  l.FavoritesCount,
		ContactsCount:   l.ContactsCount,
		Title:           l.Title,
		Images:          images,
		CreatedAt:       l.CreatedAt,
		UpdatedAt:       l.UpdatedAt,
	}

	if l.District.Valid {
		resp.District = &l.District.String
	}
	if l.Address.Valid {
		resp.Address = &l.Address.String
	}
	if l.Landmark.Valid {
		resp.Landmark = &l.Landmark.String
	}
	if l.Latitude.Valid {
		resp.Latitude = &l.Latitude.Float64
	}
	if l.Longitude.Valid {
		resp.Longitude = &l.Longitude.Float64
	}
	if l.Rooms.Valid {
		v := int(l.Rooms.Int16)
		resp.Rooms = &v
	}
	if l.Floor.Valid {
		v := int(l.Floor.Int16)
		resp.Floor = &v
	}
	if l.TotalFloors.Valid {
		v := int(l.TotalFloors.Int16)
		resp.TotalFloors = &v
	}
	if l.AreaSqm.Valid {
		resp.AreaSqm = &l.AreaSqm.Float64
	}
	if l.DepositAmount.Valid {
		resp.DepositAmount = &l.DepositAmount.Float64
	}
	if l.Description.Valid {
		resp.Description = &l.Description.String
	}
	if l.RejectionReason.Valid {
		resp.RejectionReason = &l.RejectionReason.String
	}
	if l.PublishedAt.Valid {
		resp.PublishedAt = &l.PublishedAt.Time
	}
	if l.ExpiresAt.Valid {
		resp.ExpiresAt = &l.ExpiresAt.Time
	}
	if l.PremiumUntil.Valid {
		resp.PremiumUntil = &l.PremiumUntil.Time
	}

	return resp
}

// ToListItem — qisqartirilgan list item (ro'yxat uchun)
func (l *Listing) ToListItem(images []ImageResponse) *ListingListItem {
	item := &ListingListItem{
		ID:              l.ID.String(),
		Type:            l.Type,
		DealType:        l.DealType,
		City:            l.City,
		Price:           l.Price,
		Currency:        l.Currency,
		PriceNegotiable: l.PriceNegotiable,
		HasFurniture:    l.HasFurniture,
		HasInternet:     l.HasInternet,
		IsPremium:       l.IsPremium,
		ViewsCount:      l.ViewsCount,
		FavoritesCount:  l.FavoritesCount,
		Title:           l.Title,
		Images:          images,
		CreatedAt:       l.CreatedAt,
	}

	if l.District.Valid {
		item.District = &l.District.String
	}
	if l.Address.Valid {
		item.Address = &l.Address.String
	}
	if l.Rooms.Valid {
		v := int(l.Rooms.Int16)
		item.Rooms = &v
	}
	if l.Floor.Valid {
		v := int(l.Floor.Int16)
		item.Floor = &v
	}
	if l.TotalFloors.Valid {
		v := int(l.TotalFloors.Int16)
		item.TotalFloors = &v
	}
	if l.AreaSqm.Valid {
		item.AreaSqm = &l.AreaSqm.Float64
	}
	if l.PublishedAt.Valid {
		item.PublishedAt = &l.PublishedAt.Time
	}

	return item
}
