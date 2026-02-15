-- 000003_create_listing_images.up.sql
-- Sprint 3: E'lon rasmlari jadvali

CREATE TABLE IF NOT EXISTS listing_images (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id      UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    url             VARCHAR(500) NOT NULL,
    thumbnail_url   VARCHAR(500),
    sort_order      SMALLINT DEFAULT 0,
    is_main         BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_listing_images_listing ON listing_images(listing_id);
CREATE INDEX idx_listing_images_sort ON listing_images(listing_id, sort_order);
