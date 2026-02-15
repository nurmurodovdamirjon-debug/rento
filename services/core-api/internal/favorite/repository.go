package favorite

import (
	"context"
	"database/sql"
	"fmt"

	"github.com/jmoiron/sqlx"
)

// Repository — favorites DB operatsiyalari
type Repository struct {
	db *sqlx.DB
}

// NewRepository — yangi Repository yaratish
func NewRepository(db *sqlx.DB) *Repository {
	return &Repository{db: db}
}

// Exists — sevimlilar ichida bor-yo'qligini tekshirish
func (r *Repository) Exists(ctx context.Context, userID, listingID string) (bool, error) {
	var exists bool
	query := `SELECT EXISTS(SELECT 1 FROM favorites WHERE user_id = $1 AND listing_id = $2)`
	if err := r.db.QueryRowContext(ctx, query, userID, listingID).Scan(&exists); err != nil {
		return false, fmt.Errorf("check favorite exists: %w", err)
	}
	return exists, nil
}

// Add — sevimlilarga qo'shish
func (r *Repository) Add(ctx context.Context, userID, listingID string) error {
	tx, err := r.db.BeginTxx(ctx, nil)
	if err != nil {
		return fmt.Errorf("begin tx: %w", err)
	}
	defer tx.Rollback()

	// Sevimlilar jadvaliga qo'shish
	_, err = tx.ExecContext(ctx,
		`INSERT INTO favorites (user_id, listing_id) VALUES ($1, $2) ON CONFLICT DO NOTHING`,
		userID, listingID,
	)
	if err != nil {
		return fmt.Errorf("add favorite: %w", err)
	}

	// E'lonning favorites_count ni oshirish
	_, err = tx.ExecContext(ctx,
		`UPDATE listings SET favorites_count = favorites_count + 1 WHERE id = $1`,
		listingID,
	)
	if err != nil {
		return fmt.Errorf("increment favorites count: %w", err)
	}

	return tx.Commit()
}

// Remove — sevimlilardan olib tashlash
func (r *Repository) Remove(ctx context.Context, userID, listingID string) error {
	tx, err := r.db.BeginTxx(ctx, nil)
	if err != nil {
		return fmt.Errorf("begin tx: %w", err)
	}
	defer tx.Rollback()

	result, err := tx.ExecContext(ctx,
		`DELETE FROM favorites WHERE user_id = $1 AND listing_id = $2`,
		userID, listingID,
	)
	if err != nil {
		return fmt.Errorf("remove favorite: %w", err)
	}

	rowsAffected, _ := result.RowsAffected()
	if rowsAffected > 0 {
		_, err = tx.ExecContext(ctx,
			`UPDATE listings SET favorites_count = GREATEST(favorites_count - 1, 0) WHERE id = $1`,
			listingID,
		)
		if err != nil {
			return fmt.Errorf("decrement favorites count: %w", err)
		}
	}

	return tx.Commit()
}

// GetUserFavorites — foydalanuvchining sevimlilarini olish (e'lon ma'lumotlari bilan)
func (r *Repository) GetUserFavorites(ctx context.Context, userID string, page, perPage int) ([]*FavoriteListItem, int, error) {
	offset := (page - 1) * perPage

	// Umumiy soni
	var total int
	countQuery := `SELECT COUNT(*) FROM favorites WHERE user_id = $1`
	if err := r.db.QueryRowContext(ctx, countQuery, userID).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count favorites: %w", err)
	}

	// Ro'yxat
	query := `
		SELECT
			f.id AS favorite_id,
			l.id AS listing_id,
			l.title,
			l.city,
			l.price,
			l.currency,
			l.rooms,
			l.area_sqm,
			(SELECT li.url FROM listing_images li 
			 WHERE li.listing_id = l.id 
			 ORDER BY li.sort_order LIMIT 1) AS image_url,
			l.status,
			l.is_premium,
			f.created_at AS favorited_at
		FROM favorites f
		JOIN listings l ON l.id = f.listing_id
		WHERE f.user_id = $1
		ORDER BY f.created_at DESC
		LIMIT $2 OFFSET $3`

	rows, err := r.db.QueryxContext(ctx, query, userID, perPage, offset)
	if err != nil {
		return nil, 0, fmt.Errorf("query favorites: %w", err)
	}
	defer rows.Close()

	var items []*FavoriteListItem
	for rows.Next() {
		var item FavoriteListItem
		var rooms sql.NullInt16
		var areaSqm sql.NullFloat64
		var imageURL sql.NullString

		if err := rows.Scan(
			&item.FavoriteID, &item.ListingID, &item.Title,
			&item.City, &item.Price, &item.Currency,
			&rooms, &areaSqm, &imageURL,
			&item.Status, &item.IsPremium, &item.FavoritedAt,
		); err != nil {
			return nil, 0, fmt.Errorf("scan favorite: %w", err)
		}

		if rooms.Valid {
			v := rooms.Int16
			item.Rooms = &v
		}
		if areaSqm.Valid {
			v := areaSqm.Float64
			item.AreaSqm = &v
		}
		if imageURL.Valid {
			item.ImageURL = &imageURL.String
		}

		items = append(items, &item)
	}

	return items, total, nil
}

// GetFavoritesCount — e'lonning umumiy favorites_count
func (r *Repository) GetFavoritesCount(ctx context.Context, listingID string) (int, error) {
	var count int
	query := `SELECT favorites_count FROM listings WHERE id = $1`
	if err := r.db.QueryRowContext(ctx, query, listingID).Scan(&count); err != nil {
		if err == sql.ErrNoRows {
			return 0, nil
		}
		return 0, fmt.Errorf("get favorites count: %w", err)
	}
	return count, nil
}

// GetUserFavoriteIDs — foydalanuvchining sevimli e'lon ID lari (batch check uchun)
func (r *Repository) GetUserFavoriteIDs(ctx context.Context, userID string) ([]string, error) {
	var ids []string
	query := `SELECT listing_id::text FROM favorites WHERE user_id = $1`
	if err := r.db.SelectContext(ctx, &ids, query, userID); err != nil {
		return nil, fmt.Errorf("get favorite ids: %w", err)
	}
	return ids, nil
}
