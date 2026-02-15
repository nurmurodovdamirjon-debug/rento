-- 000006_create_favorites.up.sql
-- Sprint 6: Sevimlilar jadvali

CREATE TABLE IF NOT EXISTS favorites (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    listing_id      UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),

    -- Har bir foydalanuvchi uchun har bir e'lon faqat bir marta
    UNIQUE (user_id, listing_id)
);

-- Indexes
CREATE INDEX idx_favorites_user ON favorites(user_id);
CREATE INDEX idx_favorites_listing ON favorites(listing_id);
CREATE INDEX idx_favorites_user_created ON favorites(user_id, created_at DESC);
