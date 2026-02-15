package report

import (
	"context"
	"fmt"

	"github.com/google/uuid"
	"github.com/jmoiron/sqlx"
)

// Repository — report database operatsiyalari
type Repository struct {
	db *sqlx.DB
}

// NewRepository — yangi Repository yaratish
func NewRepository(db *sqlx.DB) *Repository {
	return &Repository{db: db}
}

// Create — yangi shikoyat yaratish
func (r *Repository) Create(ctx context.Context, reporterID string, req *CreateReportRequest) (*Report, error) {
	report := &Report{}

	query := `
		INSERT INTO reports (reporter_id, target_type, target_id, reason, description)
		VALUES ($1, $2, $3, $4, NULLIF($5, ''))
		RETURNING id, reporter_id, target_type, target_id, reason, description, 
		          status, admin_note, resolved_by, resolved_at, created_at, updated_at`

	err := r.db.QueryRowxContext(ctx, query,
		reporterID, req.TargetType, req.TargetID, req.Reason, req.Description,
	).StructScan(report)

	if err != nil {
		return nil, fmt.Errorf("create report: %w", err)
	}
	return report, nil
}

// FindByID — ID bo'yicha topish
func (r *Repository) FindByID(ctx context.Context, id string) (*Report, error) {
	report := &Report{}
	query := `SELECT * FROM reports WHERE id = $1`
	err := r.db.GetContext(ctx, report, query, id)
	if err != nil {
		return nil, fmt.Errorf("find report %s: %w", id, err)
	}
	return report, nil
}

// GetAll — barcha shikoyatlar (admin, paginated, filtr bilan)
func (r *Repository) GetAll(ctx context.Context, status string, page, perPage int) ([]ReportListItem, int, error) {
	offset := (page - 1) * perPage

	// Count
	countQuery := `SELECT COUNT(*) FROM reports`
	countArgs := []interface{}{}
	if status != "" {
		countQuery += ` WHERE status = $1`
		countArgs = append(countArgs, status)
	}

	var total int
	if err := r.db.GetContext(ctx, &total, countQuery, countArgs...); err != nil {
		return nil, 0, fmt.Errorf("count reports: %w", err)
	}

	// Data
	dataQuery := `
		SELECT r.id, r.reporter_id, u.phone AS reporter_phone,
		       r.target_type, r.target_id, r.reason, r.description,
		       r.status, r.admin_note, r.created_at
		FROM reports r
		JOIN users u ON u.id = r.reporter_id`

	dataArgs := []interface{}{}
	if status != "" {
		dataQuery += ` WHERE r.status = $1 ORDER BY r.created_at DESC LIMIT $2 OFFSET $3`
		dataArgs = append(dataArgs, status, perPage, offset)
	} else {
		dataQuery += ` ORDER BY r.created_at DESC LIMIT $1 OFFSET $2`
		dataArgs = append(dataArgs, perPage, offset)
	}

	var items []ReportListItem
	if err := r.db.SelectContext(ctx, &items, dataQuery, dataArgs...); err != nil {
		return nil, 0, fmt.Errorf("get reports: %w", err)
	}

	return items, total, nil
}

// GetByTarget — target bo'yicha shikoyatlar
func (r *Repository) GetByTarget(ctx context.Context, targetType, targetID string) ([]Report, error) {
	var reports []Report
	query := `SELECT * FROM reports WHERE target_type = $1 AND target_id = $2 ORDER BY created_at DESC`
	if err := r.db.SelectContext(ctx, &reports, query, targetType, targetID); err != nil {
		return nil, fmt.Errorf("get reports by target: %w", err)
	}
	return reports, nil
}

// Resolve — shikoyatni hal qilish (admin)
func (r *Repository) Resolve(ctx context.Context, id, adminID, status, adminNote string) error {
	query := `
		UPDATE reports 
		SET status = $2, admin_note = NULLIF($3, ''), resolved_by = $4, 
		    resolved_at = NOW(), updated_at = NOW()
		WHERE id = $1`

	result, err := r.db.ExecContext(ctx, query, id, status, adminNote, adminID)
	if err != nil {
		return fmt.Errorf("resolve report %s: %w", id, err)
	}

	rows, err := result.RowsAffected()
	if err != nil {
		return fmt.Errorf("resolve report rows: %w", err)
	}
	if rows == 0 {
		return fmt.Errorf("report not found: %s", id)
	}
	return nil
}

// ExistsByReporter — foydalanuvchi allaqachon shikoyat yuborganmi
func (r *Repository) ExistsByReporter(ctx context.Context, reporterID, targetType, targetID string) (bool, error) {
	var exists bool
	query := `SELECT EXISTS(SELECT 1 FROM reports WHERE reporter_id = $1 AND target_type = $2 AND target_id = $3)`
	if err := r.db.GetContext(ctx, &exists, query, reporterID, targetType, targetID); err != nil {
		return false, fmt.Errorf("check report exists: %w", err)
	}
	return exists, nil
}

// CountByTarget — target bo'yicha shikoyat soni
func (r *Repository) CountByTarget(ctx context.Context, targetType, targetID string) (int, error) {
	var count int
	query := `SELECT COUNT(*) FROM reports WHERE target_type = $1 AND target_id = $2`
	if err := r.db.GetContext(ctx, &count, query, targetType, targetID); err != nil {
		return 0, fmt.Errorf("count reports by target: %w", err)
	}
	return count, nil
}

// parseUUID — UUID ni tekshirish
func parseUUID(s string) (uuid.UUID, error) {
	return uuid.Parse(s)
}
