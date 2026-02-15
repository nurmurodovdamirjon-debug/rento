package report

import (
	"context"
	"errors"
	"fmt"

	"github.com/rs/zerolog/log"
)

// Xatolar
var (
	ErrNotFound        = errors.New("shikoyat topilmadi")
	ErrAlreadyReported = errors.New("siz bu target ga allaqachon shikoyat yuborgansiz")
	ErrSelfReport      = errors.New("o'zingizga shikoyat yubora olmaysiz")
)

// Auto-action threshold: 5 ta shikoyatdan keyin avtomatik harakatlar
const autoActionThreshold = 5

// Service — report biznes logikasi
type Service struct {
	repo *Repository
}

// NewService — yangi Service yaratish
func NewService(repo *Repository) *Service {
	return &Service{repo: repo}
}

// CreateReport — yangi shikoyat yaratish
func (s *Service) CreateReport(ctx context.Context, reporterID string, req *CreateReportRequest) (*Report, error) {
	// O'ziga shikoyat tekshiruv (agar user target bo'lsa)
	if req.TargetType == TargetTypeUser && req.TargetID == reporterID {
		return nil, ErrSelfReport
	}

	// Takroriy shikoyat tekshiruv
	exists, err := s.repo.ExistsByReporter(ctx, reporterID, req.TargetType, req.TargetID)
	if err != nil {
		return nil, fmt.Errorf("check existing: %w", err)
	}
	if exists {
		return nil, ErrAlreadyReported
	}

	report, err := s.repo.Create(ctx, reporterID, req)
	if err != nil {
		return nil, fmt.Errorf("create: %w", err)
	}

	// Shikoyat soni tekshirish — auto-action
	count, err := s.repo.CountByTarget(ctx, req.TargetType, req.TargetID)
	if err != nil {
		log.Error().Err(err).Str("target", req.TargetID).Msg("Count reports by target failed")
	} else if count >= autoActionThreshold {
		log.Warn().
			Str("target_type", req.TargetType).
			Str("target_id", req.TargetID).
			Int("count", count).
			Msg("Target reached auto-action threshold — review required")
	}

	log.Info().
		Str("reporter", reporterID).
		Str("target", req.TargetType+"/"+req.TargetID).
		Str("reason", req.Reason).
		Msg("Report created")

	return report, nil
}

// GetReports — barcha shikoyatlar (admin)
func (s *Service) GetReports(ctx context.Context, status string, page, perPage int) ([]ReportListItem, int, error) {
	if page < 1 {
		page = 1
	}
	if perPage < 1 || perPage > 100 {
		perPage = 20
	}
	return s.repo.GetAll(ctx, status, page, perPage)
}

// GetReport — bitta shikoyat (admin)
func (s *Service) GetReport(ctx context.Context, id string) (*Report, error) {
	return s.repo.FindByID(ctx, id)
}

// ResolveReport — shikoyatni hal qilish (admin)
func (s *Service) ResolveReport(ctx context.Context, id, adminID string, req *ResolveReportRequest) error {
	report, err := s.repo.FindByID(ctx, id)
	if err != nil {
		return ErrNotFound
	}

	if report.Status != StatusPending && report.Status != StatusReviewing {
		return fmt.Errorf("faqat pending/reviewing shikoyatlarni hal qilish mumkin")
	}

	if err := s.repo.Resolve(ctx, id, adminID, req.Status, req.AdminNote); err != nil {
		return fmt.Errorf("resolve: %w", err)
	}

	log.Info().
		Str("report_id", id).
		Str("admin", adminID).
		Str("status", req.Status).
		Msg("Report resolved")

	return nil
}
