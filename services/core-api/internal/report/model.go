package report

import (
	"database/sql"
	"time"

	"github.com/google/uuid"
)

// Report — shikoyat domain modeli
type Report struct {
	ID          uuid.UUID      `json:"id" db:"id"`
	ReporterID  uuid.UUID      `json:"reporter_id" db:"reporter_id"`
	TargetType  string         `json:"target_type" db:"target_type"`
	TargetID    uuid.UUID      `json:"target_id" db:"target_id"`
	Reason      string         `json:"reason" db:"reason"`
	Description sql.NullString `json:"description" db:"description"`
	Status      string         `json:"status" db:"status"`
	AdminNote   sql.NullString `json:"admin_note" db:"admin_note"`
	ResolvedBy  *uuid.UUID     `json:"resolved_by" db:"resolved_by"`
	ResolvedAt  sql.NullTime   `json:"resolved_at" db:"resolved_at"`
	CreatedAt   time.Time      `json:"created_at" db:"created_at"`
	UpdatedAt   time.Time      `json:"updated_at" db:"updated_at"`
}

// ReportListItem — shikoyat ro'yxat elementi (admin uchun)
type ReportListItem struct {
	ID             uuid.UUID      `json:"id" db:"id"`
	ReporterID     uuid.UUID      `json:"reporter_id" db:"reporter_id"`
	ReporterPhone  string         `json:"reporter_phone" db:"reporter_phone"`
	TargetType     string         `json:"target_type" db:"target_type"`
	TargetID       uuid.UUID      `json:"target_id" db:"target_id"`
	Reason         string         `json:"reason" db:"reason"`
	Description    sql.NullString `json:"description" db:"description"`
	Status         string         `json:"status" db:"status"`
	AdminNote      sql.NullString `json:"admin_note" db:"admin_note"`
	CreatedAt      time.Time      `json:"created_at" db:"created_at"`
}

// CreateReportRequest — shikoyat yaratish
type CreateReportRequest struct {
	TargetType  string `json:"target_type" binding:"required,oneof=listing user message"`
	TargetID    string `json:"target_id" binding:"required,uuid"`
	Reason      string `json:"reason" binding:"required,oneof=spam fraud inappropriate duplicate wrong_info offensive illegal other"`
	Description string `json:"description" binding:"max=1000"`
}

// ResolveReportRequest — shikoyatni hal qilish (admin)
type ResolveReportRequest struct {
	Status    string `json:"status" binding:"required,oneof=resolved dismissed"`
	AdminNote string `json:"admin_note" binding:"max=1000"`
}

// ReportResponse — API javob
type ReportResponse struct {
	ID          string  `json:"id"`
	ReporterID  string  `json:"reporter_id"`
	TargetType  string  `json:"target_type"`
	TargetID    string  `json:"target_id"`
	Reason      string  `json:"reason"`
	Description *string `json:"description"`
	Status      string  `json:"status"`
	AdminNote   *string `json:"admin_note,omitempty"`
	CreatedAt   string  `json:"created_at"`
}

// ToResponse — model → API javob
func (r *Report) ToResponse() *ReportResponse {
	resp := &ReportResponse{
		ID:         r.ID.String(),
		ReporterID: r.ReporterID.String(),
		TargetType: r.TargetType,
		TargetID:   r.TargetID.String(),
		Reason:     r.Reason,
		Status:     r.Status,
		CreatedAt:  r.CreatedAt.Format(time.RFC3339),
	}
	if r.Description.Valid {
		resp.Description = &r.Description.String
	}
	if r.AdminNote.Valid {
		resp.AdminNote = &r.AdminNote.String
	}
	return resp
}

// Shikoyat sabablari
const (
	ReasonSpam          = "spam"
	ReasonFraud         = "fraud"
	ReasonInappropriate = "inappropriate"
	ReasonDuplicate     = "duplicate"
	ReasonWrongInfo     = "wrong_info"
	ReasonOffensive     = "offensive"
	ReasonIllegal       = "illegal"
	ReasonOther         = "other"
)

// Shikoyat statuslari
const (
	StatusPending   = "pending"
	StatusReviewing = "reviewing"
	StatusResolved  = "resolved"
	StatusDismissed = "dismissed"
)

// Target turlari
const (
	TargetTypeListing = "listing"
	TargetTypeUser    = "user"
	TargetTypeMessage = "message"
)
