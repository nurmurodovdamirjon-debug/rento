package listing

import (
	"context"
	"database/sql"
	"fmt"
	"strings"

	"github.com/jmoiron/sqlx"
	"github.com/redis/go-redis/v9"
)

// Repository — e'lon DB operatsiyalari
type Repository struct {
	db    *sqlx.DB
	redis *redis.Client
}

// NewRepository — yangi Repository yaratish
func NewRepository(db *sqlx.DB, redis *redis.Client) *Repository {
	return &Repository{db: db, redis: redis}
}

// listingColumns — SELECT uchun to'liq ustunlar
const listingColumns = `
	id, user_id, type, deal_type,
	city, district, address, landmark, latitude, longitude,
	rooms, floor, total_floors, area_sqm,
	price, currency, price_negotiable,
	has_furniture, has_appliances, has_internet, has_parking,
	has_conditioner, allows_pets, allows_children,
	utilities_included, deposit_amount,
	status, rejection_reason,
	is_premium, premium_until,
	views_count, favorites_count, contacts_count,
	title, description,
	published_at, expires_at, created_at, updated_at`

// Create — yangi e'lon yaratish
func (r *Repository) Create(ctx context.Context, userID string, req *CreateListingRequest) (*Listing, error) {
	query := `
		INSERT INTO listings (
			user_id, type, deal_type, city, district, address, landmark,
			latitude, longitude, rooms, floor, total_floors, area_sqm,
			price, currency, price_negotiable,
			has_furniture, has_appliances, has_internet, has_parking,
			has_conditioner, allows_pets, allows_children,
			utilities_included, deposit_amount,
			title, description, status
		) VALUES (
			$1, $2, COALESCE($3, 'rent'), $4, $5, $6, $7,
			$8, $9, $10, $11, $12, $13,
			$14, $15, COALESCE($16, false),
			COALESCE($17, false), COALESCE($18, false), COALESCE($19, false), COALESCE($20, false),
			COALESCE($21, false), COALESCE($22, false), COALESCE($23, true),
			COALESCE($24, false), $25,
			$26, $27, 'pending'
		)
		RETURNING ` + listingColumns

	dealType := "rent"
	if req.DealType != "" {
		dealType = req.DealType
	}

	var listing Listing
	err := r.db.QueryRowxContext(ctx, query,
		userID, req.Type, dealType, req.City, req.District, req.Address, req.Landmark,
		req.Latitude, req.Longitude, req.Rooms, req.Floor, req.TotalFloors, req.AreaSqm,
		req.Price, req.Currency, req.PriceNegotiable,
		req.HasFurniture, req.HasAppliances, req.HasInternet, req.HasParking,
		req.HasConditioner, req.AllowsPets, req.AllowsChildren,
		req.UtilitiesIncl, req.DepositAmount,
		req.Title, req.Description,
	).StructScan(&listing)
	if err != nil {
		return nil, fmt.Errorf("create listing: %w", err)
	}
	return &listing, nil
}

// FindByID — e'lonni ID bo'yicha topish
func (r *Repository) FindByID(ctx context.Context, id string) (*Listing, error) {
	var listing Listing
	query := `SELECT ` + listingColumns + ` FROM listings WHERE id = $1`

	if err := r.db.GetContext(ctx, &listing, query, id); err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, fmt.Errorf("find listing %s: %w", id, err)
	}
	return &listing, nil
}

// FindAll — e'lonlar ro'yxati (filtr + pagination)
func (r *Repository) FindAll(ctx context.Context, f *ListingsFilter) ([]*Listing, int, error) {
	where := []string{"status = 'active'"}
	args := []interface{}{}
	argIdx := 1

	if f.City != "" {
		where = append(where, fmt.Sprintf("city = $%d", argIdx))
		args = append(args, f.City)
		argIdx++
	}
	if f.District != "" {
		where = append(where, fmt.Sprintf("district = $%d", argIdx))
		args = append(args, f.District)
		argIdx++
	}
	if f.Type != "" {
		where = append(where, fmt.Sprintf("type = $%d", argIdx))
		args = append(args, f.Type)
		argIdx++
	}
	if f.DealType != "" {
		where = append(where, fmt.Sprintf("deal_type = $%d", argIdx))
		args = append(args, f.DealType)
		argIdx++
	}
	if f.RoomsMin != nil {
		where = append(where, fmt.Sprintf("rooms >= $%d", argIdx))
		args = append(args, *f.RoomsMin)
		argIdx++
	}
	if f.RoomsMax != nil {
		where = append(where, fmt.Sprintf("rooms <= $%d", argIdx))
		args = append(args, *f.RoomsMax)
		argIdx++
	}
	if f.PriceMin != nil {
		where = append(where, fmt.Sprintf("price >= $%d", argIdx))
		args = append(args, *f.PriceMin)
		argIdx++
	}
	if f.PriceMax != nil {
		where = append(where, fmt.Sprintf("price <= $%d", argIdx))
		args = append(args, *f.PriceMax)
		argIdx++
	}
	if f.Currency != "" {
		where = append(where, fmt.Sprintf("currency = $%d", argIdx))
		args = append(args, f.Currency)
		argIdx++
	}
	if f.HasFurniture != nil {
		where = append(where, fmt.Sprintf("has_furniture = $%d", argIdx))
		args = append(args, *f.HasFurniture)
		argIdx++
	}
	if f.HasParking != nil {
		where = append(where, fmt.Sprintf("has_parking = $%d", argIdx))
		args = append(args, *f.HasParking)
		argIdx++
	}
	if f.AllowsPets != nil {
		where = append(where, fmt.Sprintf("allows_pets = $%d", argIdx))
		args = append(args, *f.AllowsPets)
		argIdx++
	}

	whereClause := strings.Join(where, " AND ")

	// Total count
	var total int
	countQuery := fmt.Sprintf("SELECT COUNT(*) FROM listings WHERE %s", whereClause)
	if err := r.db.GetContext(ctx, &total, countQuery, args...); err != nil {
		return nil, 0, fmt.Errorf("count listings: %w", err)
	}

	// Sorting
	orderBy := "is_premium DESC, published_at DESC NULLS LAST, created_at DESC"
	switch f.Sort {
	case "price_asc":
		orderBy = "is_premium DESC, price ASC"
	case "price_desc":
		orderBy = "is_premium DESC, price DESC"
	case "date_desc":
		orderBy = "is_premium DESC, created_at DESC"
	}

	// Pagination
	if f.Page < 1 {
		f.Page = 1
	}
	if f.PerPage < 1 || f.PerPage > 50 {
		f.PerPage = 20
	}
	offset := (f.Page - 1) * f.PerPage

	dataQuery := fmt.Sprintf(
		"SELECT %s FROM listings WHERE %s ORDER BY %s LIMIT %d OFFSET %d",
		listingColumns, whereClause, orderBy, f.PerPage, offset,
	)

	var listings []*Listing
	if err := r.db.SelectContext(ctx, &listings, dataQuery, args...); err != nil {
		return nil, 0, fmt.Errorf("find listings: %w", err)
	}

	return listings, total, nil
}

// FindByUserID — foydalanuvchining e'lonlari
func (r *Repository) FindByUserID(ctx context.Context, userID string, status string, page, perPage int) ([]*Listing, int, error) {
	where := "user_id = $1"
	args := []interface{}{userID}
	argIdx := 2

	if status != "" {
		where += fmt.Sprintf(" AND status = $%d", argIdx)
		args = append(args, status)
	}

	var total int
	countQuery := fmt.Sprintf("SELECT COUNT(*) FROM listings WHERE %s", where)
	if err := r.db.GetContext(ctx, &total, countQuery, args...); err != nil {
		return nil, 0, fmt.Errorf("count user listings: %w", err)
	}

	if page < 1 {
		page = 1
	}
	if perPage < 1 || perPage > 50 {
		perPage = 20
	}
	offset := (page - 1) * perPage

	dataQuery := fmt.Sprintf(
		"SELECT %s FROM listings WHERE %s ORDER BY created_at DESC LIMIT %d OFFSET %d",
		listingColumns, where, perPage, offset,
	)

	var listings []*Listing
	if err := r.db.SelectContext(ctx, &listings, dataQuery, args...); err != nil {
		return nil, 0, fmt.Errorf("find user listings: %w", err)
	}

	return listings, total, nil
}

// Update — e'lonni tahrirlash (COALESCE)
func (r *Repository) Update(ctx context.Context, id string, req *UpdateListingRequest) (*Listing, error) {
	query := `
		UPDATE listings SET
			type = COALESCE($2, type),
			deal_type = COALESCE($3, deal_type),
			city = COALESCE($4, city),
			district = COALESCE($5, district),
			address = COALESCE($6, address),
			landmark = COALESCE($7, landmark),
			latitude = COALESCE($8, latitude),
			longitude = COALESCE($9, longitude),
			rooms = COALESCE($10, rooms),
			floor = COALESCE($11, floor),
			total_floors = COALESCE($12, total_floors),
			area_sqm = COALESCE($13, area_sqm),
			price = COALESCE($14, price),
			currency = COALESCE($15, currency),
			price_negotiable = COALESCE($16, price_negotiable),
			has_furniture = COALESCE($17, has_furniture),
			has_appliances = COALESCE($18, has_appliances),
			has_internet = COALESCE($19, has_internet),
			has_parking = COALESCE($20, has_parking),
			has_conditioner = COALESCE($21, has_conditioner),
			allows_pets = COALESCE($22, allows_pets),
			allows_children = COALESCE($23, allows_children),
			utilities_included = COALESCE($24, utilities_included),
			deposit_amount = COALESCE($25, deposit_amount),
			title = COALESCE($26, title),
			description = COALESCE($27, description),
			status = 'pending'
		WHERE id = $1
		RETURNING ` + listingColumns

	var listing Listing
	err := r.db.QueryRowxContext(ctx, query, id,
		req.Type, req.DealType, req.City, req.District, req.Address, req.Landmark,
		req.Latitude, req.Longitude, req.Rooms, req.Floor, req.TotalFloors, req.AreaSqm,
		req.Price, req.Currency, req.PriceNegotiable,
		req.HasFurniture, req.HasAppliances, req.HasInternet, req.HasParking,
		req.HasConditioner, req.AllowsPets, req.AllowsChildren,
		req.UtilitiesIncl, req.DepositAmount,
		req.Title, req.Description,
	).StructScan(&listing)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, fmt.Errorf("update listing %s: %w", id, err)
	}
	return &listing, nil
}

// UpdateStatus — statusni o'zgartirish
func (r *Repository) UpdateStatus(ctx context.Context, id, status string) error {
	query := `UPDATE listings SET status = $2 WHERE id = $1`
	result, err := r.db.ExecContext(ctx, query, id, status)
	if err != nil {
		return fmt.Errorf("update listing status %s: %w", id, err)
	}
	rows, _ := result.RowsAffected()
	if rows == 0 {
		return fmt.Errorf("listing not found: %s", id)
	}
	return nil
}

// Delete — e'lonni o'chirish (soft delete = archived)
func (r *Repository) Delete(ctx context.Context, id string) error {
	query := `UPDATE listings SET status = 'archived' WHERE id = $1`
	result, err := r.db.ExecContext(ctx, query, id)
	if err != nil {
		return fmt.Errorf("delete listing %s: %w", id, err)
	}
	rows, _ := result.RowsAffected()
	if rows == 0 {
		return fmt.Errorf("listing not found: %s", id)
	}
	return nil
}

// FindImagesByListingID — e'lon rasmlarini olish
func (r *Repository) FindImagesByListingID(ctx context.Context, listingID string) ([]ListingImage, error) {
	var images []ListingImage
	query := `
		SELECT id, listing_id, url, thumbnail_url, sort_order, is_main, created_at
		FROM listing_images
		WHERE listing_id = $1
		ORDER BY sort_order ASC`

	if err := r.db.SelectContext(ctx, &images, query, listingID); err != nil {
		return nil, fmt.Errorf("find images for listing %s: %w", listingID, err)
	}
	return images, nil
}

// CreateImage — rasm qo'shish
func (r *Repository) CreateImage(ctx context.Context, listingID, url, thumbnailURL string, sortOrder int, isMain bool) (*ListingImage, error) {
	var img ListingImage
	query := `
		INSERT INTO listing_images (listing_id, url, thumbnail_url, sort_order, is_main)
		VALUES ($1, $2, $3, $4, $5)
		RETURNING id, listing_id, url, thumbnail_url, sort_order, is_main, created_at`

	err := r.db.QueryRowxContext(ctx, query, listingID, url, thumbnailURL, sortOrder, isMain).StructScan(&img)
	if err != nil {
		return nil, fmt.Errorf("create image: %w", err)
	}
	return &img, nil
}

// DeleteImagesByListingID — e'lonning barcha rasmlarini o'chirish
func (r *Repository) DeleteImagesByListingID(ctx context.Context, listingID string) error {
	_, err := r.db.ExecContext(ctx, "DELETE FROM listing_images WHERE listing_id = $1", listingID)
	return err
}

// FindByIDs — ID lar ro'yxati bo'yicha e'lonlarni olish (tartibni saqlaydi)
func (r *Repository) FindByIDs(ctx context.Context, ids []string) ([]*Listing, error) {
	if len(ids) == 0 {
		return []*Listing{}, nil
	}

	// IN clause qurilishi
	placeholders := make([]string, len(ids))
	args := make([]interface{}, len(ids))
	for i, id := range ids {
		placeholders[i] = fmt.Sprintf("$%d", i+1)
		args[i] = id
	}

	query := fmt.Sprintf(
		"SELECT %s FROM listings WHERE id IN (%s)",
		listingColumns,
		strings.Join(placeholders, ","),
	)

	var listings []*Listing
	if err := r.db.SelectContext(ctx, &listings, query, args...); err != nil {
		return nil, fmt.Errorf("find listings by ids: %w", err)
	}

	// ES tartibini saqlash — IDs tartibi bo'yicha qaytarish
	idOrder := make(map[string]int, len(ids))
	for i, id := range ids {
		idOrder[id] = i
	}
	ordered := make([]*Listing, 0, len(listings))
	byID := make(map[string]*Listing, len(listings))
	for _, l := range listings {
		byID[l.ID.String()] = l
	}
	for _, id := range ids {
		if l, ok := byID[id]; ok {
			ordered = append(ordered, l)
		}
	}

	return ordered, nil
}

// FindNearby — yaqin atrofdagi e'lonlar (PostGIS ST_DWithin)
func (r *Repository) FindNearby(ctx context.Context, filter *NearbyFilter) ([]*NearbyListing, int, error) {
	if filter.Page < 1 {
		filter.Page = 1
	}
	if filter.RadiusKm < 1 {
		filter.RadiusKm = 5
	}
	if filter.PerPage < 1 || filter.PerPage > 50 {
		filter.PerPage = 20
	}
	offset := (filter.Page - 1) * filter.PerPage
	radiusMeters := filter.RadiusKm * 1000

	// WHERE shartlari
	where := []string{
		"status = 'active'",
		"latitude IS NOT NULL",
		"longitude IS NOT NULL",
	}
	args := []interface{}{filter.Lng, filter.Lat, radiusMeters}
	argIdx := 4

	if filter.Type != "" {
		where = append(where, fmt.Sprintf("type = $%d", argIdx))
		args = append(args, filter.Type)
		argIdx++
	}
	if filter.DealType != "" {
		where = append(where, fmt.Sprintf("deal_type = $%d", argIdx))
		args = append(args, filter.DealType)
		argIdx++
	}

	whereClause := strings.Join(where, " AND ")

	// Total count
	countQuery := fmt.Sprintf(`
		SELECT COUNT(*) FROM listings
		WHERE %s
		AND ST_DWithin(
			ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography,
			ST_SetSRID(ST_MakePoint($1, $2), 4326)::geography,
			$3
		)`, whereClause)

	var total int
	if err := r.db.GetContext(ctx, &total, countQuery, args...); err != nil {
		return nil, 0, fmt.Errorf("count nearby listings: %w", err)
	}

	// Data query
	dataQuery := fmt.Sprintf(`
		SELECT %s,
			ST_Distance(
				ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography,
				ST_SetSRID(ST_MakePoint($1, $2), 4326)::geography
			) AS distance_meters
		FROM listings
		WHERE %s
		AND ST_DWithin(
			ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography,
			ST_SetSRID(ST_MakePoint($1, $2), 4326)::geography,
			$3
		)
		ORDER BY is_premium DESC, distance_meters ASC
		LIMIT %d OFFSET %d`,
		listingColumns, whereClause, filter.PerPage, offset)

	var results []*NearbyListing
	if err := r.db.SelectContext(ctx, &results, dataQuery, args...); err != nil {
		return nil, 0, fmt.Errorf("find nearby listings: %w", err)
	}

	return results, total, nil
}

// IncrementViews — ko'rishlar sonini Redis da oshirish
func (r *Repository) IncrementViews(ctx context.Context, listingID string) {
	key := fmt.Sprintf("stats:listing:%s:views", listingID)
	r.redis.Incr(ctx, key)
}

// GetStats — e'lon statistikasini olish (Redis + DB)
func (r *Repository) GetStats(ctx context.Context, listingID string) (*ListingStats, error) {
	viewsKey := fmt.Sprintf("stats:listing:%s:views", listingID)
	contactsKey := fmt.Sprintf("stats:listing:%s:contacts", listingID)

	views, _ := r.redis.Get(ctx, viewsKey).Int()
	contacts, _ := r.redis.Get(ctx, contactsKey).Int()

	var favCount int
	_ = r.db.GetContext(ctx, &favCount,
		"SELECT COUNT(*) FROM favorites WHERE listing_id = $1", listingID)

	return &ListingStats{Views: views, Favorites: favCount, Contacts: contacts}, nil
}
