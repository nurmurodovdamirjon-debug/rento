package notification

import (
	"context"
	"database/sql"
	"fmt"

	"github.com/google/uuid"
	"github.com/jmoiron/sqlx"
)

// Repository — notifications DB operatsiyalari
type Repository struct {
	db *sqlx.DB
}

// NewRepository — yangi Repository yaratish
func NewRepository(db *sqlx.DB) *Repository {
	return &Repository{db: db}
}

// Create — yangi bildirishnoma yaratish
func (r *Repository) Create(ctx context.Context, input *CreateNotificationInput) (*Notification, error) {
	query := `
		INSERT INTO notifications (user_id, type, title, body, ref_type, ref_id)
		VALUES ($1, $2, $3, $4, NULLIF($5, ''), NULLIF($6, '')::uuid)
		RETURNING id, user_id, type, title, body, ref_type, ref_id,
		          is_read, read_at, push_sent, push_sent_at, created_at`

	var n Notification
	err := r.db.QueryRowxContext(ctx, query,
		input.UserID, input.Type, input.Title, input.Body,
		input.RefType, input.RefID,
	).StructScan(&n)
	if err != nil {
		return nil, fmt.Errorf("create notification: %w", err)
	}
	return &n, nil
}

// GetByUser — foydalanuvchi bildirishnomalarini olish
func (r *Repository) GetByUser(ctx context.Context, userID string, page, perPage int) ([]*NotificationListItem, int, error) {
	offset := (page - 1) * perPage

	var total int
	countQuery := `SELECT COUNT(*) FROM notifications WHERE user_id = $1`
	if err := r.db.QueryRowContext(ctx, countQuery, userID).Scan(&total); err != nil {
		return nil, 0, fmt.Errorf("count notifications: %w", err)
	}

	query := `
		SELECT id, type, title, body, ref_type, ref_id, is_read, created_at
		FROM notifications
		WHERE user_id = $1
		ORDER BY created_at DESC
		LIMIT $2 OFFSET $3`

	rows, err := r.db.QueryxContext(ctx, query, userID, perPage, offset)
	if err != nil {
		return nil, 0, fmt.Errorf("query notifications: %w", err)
	}
	defer rows.Close()

	var items []*NotificationListItem
	for rows.Next() {
		var item NotificationListItem
		var refType sql.NullString
		var refID sql.NullString

		if err := rows.Scan(
			&item.ID, &item.Type, &item.Title, &item.Body,
			&refType, &refID, &item.IsRead, &item.CreatedAt,
		); err != nil {
			return nil, 0, fmt.Errorf("scan notification: %w", err)
		}

		if refType.Valid {
			item.RefType = &refType.String
		}
		if refID.Valid {
			parsed, err := uuid.Parse(refID.String)
			if err == nil {
				item.RefID = &parsed
			}
		}

		items = append(items, &item)
	}

	return items, total, nil
}

// MarkAsRead — bitta bildirishnomani o'qildi deb belgilash
func (r *Repository) MarkAsRead(ctx context.Context, id, userID string) error {
	query := `UPDATE notifications SET is_read = TRUE, read_at = NOW() WHERE id = $1 AND user_id = $2`
	_, err := r.db.ExecContext(ctx, query, id, userID)
	if err != nil {
		return fmt.Errorf("mark as read: %w", err)
	}
	return nil
}

// MarkAllAsRead — barcha bildirishnomalarni o'qildi deb belgilash
func (r *Repository) MarkAllAsRead(ctx context.Context, userID string) (int, error) {
	query := `UPDATE notifications SET is_read = TRUE, read_at = NOW() WHERE user_id = $1 AND is_read = FALSE`
	result, err := r.db.ExecContext(ctx, query, userID)
	if err != nil {
		return 0, fmt.Errorf("mark all read: %w", err)
	}
	rowsAffected, _ := result.RowsAffected()
	return int(rowsAffected), nil
}

// GetUnreadCount — o'qilmagan bildirishnomalar soni
func (r *Repository) GetUnreadCount(ctx context.Context, userID string) (int, error) {
	var count int
	query := `SELECT COUNT(*) FROM notifications WHERE user_id = $1 AND is_read = FALSE`
	if err := r.db.QueryRowContext(ctx, query, userID).Scan(&count); err != nil {
		return 0, fmt.Errorf("unread count: %w", err)
	}
	return count, nil
}

// MarkPushSent — push yuborildi deb belgilash
func (r *Repository) MarkPushSent(ctx context.Context, id string) error {
	query := `UPDATE notifications SET push_sent = TRUE, push_sent_at = NOW() WHERE id = $1`
	_, err := r.db.ExecContext(ctx, query, id)
	return err
}

// ===== FCM Token =====

// SaveFCMToken — FCM tokenni saqlash/yangilash
func (r *Repository) SaveFCMToken(ctx context.Context, userID, token, deviceType string) error {
	query := `
		INSERT INTO fcm_tokens (user_id, token, device_type)
		VALUES ($1, $2, $3)
		ON CONFLICT (user_id, token) DO UPDATE SET
			is_active = TRUE,
			device_type = EXCLUDED.device_type,
			updated_at = NOW()`
	_, err := r.db.ExecContext(ctx, query, userID, token, deviceType)
	if err != nil {
		return fmt.Errorf("save fcm token: %w", err)
	}
	return nil
}

// DeleteFCMToken — FCM tokenni o'chirish (logout yoki token invalidation)
func (r *Repository) DeleteFCMToken(ctx context.Context, userID, token string) error {
	query := `DELETE FROM fcm_tokens WHERE user_id = $1 AND token = $2`
	_, err := r.db.ExecContext(ctx, query, userID, token)
	return err
}

// GetUserFCMTokens — foydalanuvchining barcha faol FCM tokenlari
func (r *Repository) GetUserFCMTokens(ctx context.Context, userID string) ([]string, error) {
	var tokens []string
	query := `SELECT token FROM fcm_tokens WHERE user_id = $1 AND is_active = TRUE`
	if err := r.db.SelectContext(ctx, &tokens, query, userID); err != nil {
		return nil, fmt.Errorf("get fcm tokens: %w", err)
	}
	return tokens, nil
}
