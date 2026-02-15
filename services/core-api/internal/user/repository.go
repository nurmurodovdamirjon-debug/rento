package user

import (
	"context"
	"database/sql"
	"fmt"

	"github.com/jmoiron/sqlx"
)

// Repository — foydalanuvchi DB operatsiyalari
type Repository struct {
	db *sqlx.DB
}

// NewRepository — yangi Repository yaratish
func NewRepository(db *sqlx.DB) *Repository {
	return &Repository{db: db}
}

// FindByID — foydalanuvchini ID bo'yicha topish
func (r *Repository) FindByID(ctx context.Context, id string) (*User, error) {
	var user User
	query := `
		SELECT id, phone, phone_verified, full_name, email, avatar_url,
		       role, id_verified, id_document_url, id_verified_at,
		       rating_avg, rating_count, subscription, sub_expires_at,
		       language, last_seen_at, created_at, updated_at,
		       is_active, is_blocked
		FROM users
		WHERE id = $1 AND is_active = true`

	if err := r.db.GetContext(ctx, &user, query, id); err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, fmt.Errorf("find user %s: %w", id, err)
	}
	return &user, nil
}

// FindByPhone — foydalanuvchini telefon raqam bo'yicha topish
func (r *Repository) FindByPhone(ctx context.Context, phone string) (*User, error) {
	var user User
	query := `
		SELECT id, phone, phone_verified, full_name, email, avatar_url,
		       role, id_verified, id_document_url, id_verified_at,
		       rating_avg, rating_count, subscription, sub_expires_at,
		       language, last_seen_at, created_at, updated_at,
		       is_active, is_blocked
		FROM users
		WHERE phone = $1 AND is_active = true`

	if err := r.db.GetContext(ctx, &user, query, phone); err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, fmt.Errorf("find user by phone %s: %w", phone, err)
	}
	return &user, nil
}

// Update — foydalanuvchi profilini yangilash (COALESCE — faqat yuborilgan maydonlar)
func (r *Repository) Update(ctx context.Context, id string, req *UpdateProfileRequest) (*User, error) {
	query := `
		UPDATE users
		SET full_name = COALESCE($2, full_name),
		    email = COALESCE($3, email),
		    language = COALESCE($4, language)
		WHERE id = $1 AND is_active = true
		RETURNING id, phone, phone_verified, full_name, email, avatar_url,
		          role, id_verified, id_document_url, id_verified_at,
		          rating_avg, rating_count, subscription, sub_expires_at,
		          language, last_seen_at, created_at, updated_at,
		          is_active, is_blocked`

	var user User
	err := r.db.QueryRowxContext(ctx, query, id, req.FullName, req.Email, req.Language).StructScan(&user)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, fmt.Errorf("update user %s: %w", id, err)
	}
	return &user, nil
}

// UpdateAvatar — avatar URL ni yangilash
func (r *Repository) UpdateAvatar(ctx context.Context, id string, avatarURL string) error {
	query := `UPDATE users SET avatar_url = $2 WHERE id = $1 AND is_active = true`
	result, err := r.db.ExecContext(ctx, query, id, avatarURL)
	if err != nil {
		return fmt.Errorf("update avatar %s: %w", id, err)
	}

	rows, err := result.RowsAffected()
	if err != nil {
		return fmt.Errorf("update avatar rows affected: %w", err)
	}
	if rows == 0 {
		return fmt.Errorf("user not found: %s", id)
	}
	return nil
}

// UpdateLastSeen — oxirgi faollik vaqtini yangilash
func (r *Repository) UpdateLastSeen(ctx context.Context, id string) error {
	query := `UPDATE users SET last_seen_at = NOW() WHERE id = $1`
	_, err := r.db.ExecContext(ctx, query, id)
	if err != nil {
		return fmt.Errorf("update last seen %s: %w", id, err)
	}
	return nil
}
