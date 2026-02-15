package user

import (
	"database/sql"
	"testing"
	"time"

	"github.com/google/uuid"
)

// ===== Model Tests =====

func TestUserToResponse(t *testing.T) {
	now := time.Now()
	userID := uuid.New()

	u := &User{
		ID:            userID,
		Phone:         "+998901234567",
		PhoneVerified: true,
		FullName:      sql.NullString{String: "Alisher Karimov", Valid: true},
		Email:         sql.NullString{String: "alisher@gmail.com", Valid: true},
		AvatarURL:     sql.NullString{String: "https://media.rento.uz/avatars/test.webp", Valid: true},
		Role:          "tenant",
		IDVerified:    false,
		RatingAvg:     4.5,
		RatingCount:   10,
		Subscription:  "free",
		Language:      "uz",
		LastSeenAt:    sql.NullTime{Time: now, Valid: true},
		CreatedAt:     now,
		UpdatedAt:     now,
		IsActive:      true,
		IsBlocked:     false,
	}

	resp := u.ToResponse()

	if resp.ID != userID.String() {
		t.Errorf("expected ID %s, got %s", userID.String(), resp.ID)
	}
	if resp.Phone != "+998901234567" {
		t.Errorf("expected phone +998901234567, got %s", resp.Phone)
	}
	if !resp.PhoneVerified {
		t.Error("expected phone_verified true")
	}
	if resp.FullName == nil || *resp.FullName != "Alisher Karimov" {
		t.Error("expected full_name to be Alisher Karimov")
	}
	if resp.Email == nil || *resp.Email != "alisher@gmail.com" {
		t.Error("expected email to be alisher@gmail.com")
	}
	if resp.AvatarURL == nil || *resp.AvatarURL != "https://media.rento.uz/avatars/test.webp" {
		t.Error("expected avatar_url")
	}
	if resp.Role != "tenant" {
		t.Errorf("expected role tenant, got %s", resp.Role)
	}
	if resp.RatingAvg != 4.5 {
		t.Errorf("expected rating_avg 4.5, got %f", resp.RatingAvg)
	}
	if resp.RatingCount != 10 {
		t.Errorf("expected rating_count 10, got %d", resp.RatingCount)
	}
	if resp.Subscription != "free" {
		t.Errorf("expected subscription free, got %s", resp.Subscription)
	}
	if resp.Language != "uz" {
		t.Errorf("expected language uz, got %s", resp.Language)
	}
	if resp.LastSeenAt == nil {
		t.Error("expected last_seen_at to be set")
	}
	if !resp.IsActive {
		t.Error("expected is_active true")
	}
}

func TestUserToResponseNullFields(t *testing.T) {
	now := time.Now()
	u := &User{
		ID:          uuid.New(),
		Phone:       "+998901234567",
		Role:        "tenant",
		Language:    "uz",
		CreatedAt:   now,
		UpdatedAt:   now,
		IsActive:    true,
		// FullName, Email, AvatarURL — null
	}

	resp := u.ToResponse()

	if resp.FullName != nil {
		t.Error("expected full_name to be nil")
	}
	if resp.Email != nil {
		t.Error("expected email to be nil")
	}
	if resp.AvatarURL != nil {
		t.Error("expected avatar_url to be nil")
	}
	if resp.IDVerifiedAt != nil {
		t.Error("expected id_verified_at to be nil")
	}
	if resp.SubExpiresAt != nil {
		t.Error("expected sub_expires_at to be nil")
	}
	if resp.LastSeenAt != nil {
		t.Error("expected last_seen_at to be nil")
	}
}

func TestUserToPublicResponse(t *testing.T) {
	now := time.Now()
	userID := uuid.New()

	u := &User{
		ID:            userID,
		Phone:         "+998901234567",
		PhoneVerified: true,
		FullName:      sql.NullString{String: "Bekzod", Valid: true},
		Email:         sql.NullString{String: "bekzod@test.com", Valid: true},
		AvatarURL:     sql.NullString{String: "https://media.rento.uz/avatars/bekzod.webp", Valid: true},
		Role:          "landlord",
		IDVerified:    true,
		RatingAvg:     4.75,
		RatingCount:   8,
		Subscription:  "pro",
		Language:      "ru",
		LastSeenAt:    sql.NullTime{Time: now, Valid: true},
		CreatedAt:     now,
		UpdatedAt:     now,
		IsActive:      true,
	}

	resp := u.ToPublicResponse()

	// Ochiq ma'lumotlar bor
	if resp.ID != userID.String() {
		t.Errorf("expected ID %s, got %s", userID.String(), resp.ID)
	}
	if resp.FullName == nil || *resp.FullName != "Bekzod" {
		t.Error("expected full_name Bekzod")
	}
	if resp.AvatarURL == nil {
		t.Error("expected avatar_url to be set")
	}
	if resp.Role != "landlord" {
		t.Errorf("expected role landlord, got %s", resp.Role)
	}
	if resp.IDVerified != true {
		t.Error("expected id_verified true")
	}
	if resp.RatingAvg != 4.75 {
		t.Errorf("expected rating_avg 4.75, got %f", resp.RatingAvg)
	}
}

// ===== DTO Tests =====

func TestUpdateProfileRequestValidation(t *testing.T) {
	// Test: Nil fields — no updates
	req := &UpdateProfileRequest{}
	if req.FullName != nil || req.Email != nil || req.Language != nil {
		t.Error("expected all fields nil by default")
	}

	// Test: set values
	name := "Alisher"
	email := "a@b.com"
	lang := "uz"
	req = &UpdateProfileRequest{
		FullName: &name,
		Email:    &email,
		Language: &lang,
	}
	if *req.FullName != "Alisher" {
		t.Error("expected FullName Alisher")
	}
	if *req.Email != "a@b.com" {
		t.Error("expected Email a@b.com")
	}
	if *req.Language != "uz" {
		t.Error("expected Language uz")
	}
}

func TestPublicResponseHidesPrivateData(t *testing.T) {
	u := &User{
		ID:        uuid.New(),
		Phone:     "+998901234567",
		Email:     sql.NullString{String: "secret@test.com", Valid: true},
		FullName:  sql.NullString{String: "Public Name", Valid: true},
		Role:      "tenant",
		Language:  "uz",
		CreatedAt: time.Now(),
		UpdatedAt: time.Now(),
		IsActive:  true,
	}

	pub := u.ToPublicResponse()

	// PublicUserResponse da phone va email yo'q — struct da bunday field mavjud emas
	// Bu test shunchaki compilatsiya orqali tekshiriladi
	if pub.ID == "" {
		t.Error("expected ID to be set")
	}
	if pub.FullName == nil || *pub.FullName != "Public Name" {
		t.Error("expected full_name Public Name")
	}
}
