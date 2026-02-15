-- 000002_create_listings.up.sql
-- Sprint 3: E'lonlar jadvali

CREATE TABLE IF NOT EXISTS listings (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Turi
    type              VARCHAR(20) NOT NULL,       -- 'apartment','house','office','shop','warehouse'
    deal_type         VARCHAR(10) DEFAULT 'rent',  -- 'rent','daily'

    -- Manzil
    city              VARCHAR(100) NOT NULL,
    district          VARCHAR(100),
    address           VARCHAR(300),
    landmark          VARCHAR(200),
    latitude          DECIMAL(10,8),
    longitude         DECIMAL(11,8),

    -- Mulk parametrlari
    rooms             SMALLINT,
    floor             SMALLINT,
    total_floors      SMALLINT,
    area_sqm          DECIMAL(8,2),

    -- Narx
    price             DECIMAL(12,2) NOT NULL,
    currency          VARCHAR(3) DEFAULT 'UZS',
    price_negotiable  BOOLEAN DEFAULT FALSE,

    -- Qo'shimcha
    has_furniture      BOOLEAN DEFAULT FALSE,
    has_appliances     BOOLEAN DEFAULT FALSE,
    has_internet       BOOLEAN DEFAULT FALSE,
    has_parking        BOOLEAN DEFAULT FALSE,
    has_conditioner    BOOLEAN DEFAULT FALSE,
    allows_pets        BOOLEAN DEFAULT FALSE,
    allows_children    BOOLEAN DEFAULT TRUE,

    -- Kommunal
    utilities_included BOOLEAN DEFAULT FALSE,
    deposit_amount     DECIMAL(12,2),

    -- Holat
    status            VARCHAR(20) DEFAULT 'pending',
    rejection_reason  VARCHAR(500),

    -- Premium
    is_premium        BOOLEAN DEFAULT FALSE,
    premium_until     TIMESTAMPTZ,

    -- Statistika (Redis da ham saqlanadi)
    views_count       INTEGER DEFAULT 0,
    favorites_count   INTEGER DEFAULT 0,
    contacts_count    INTEGER DEFAULT 0,

    -- Tavsif
    title             VARCHAR(200) NOT NULL,
    description       TEXT,

    -- Meta
    published_at      TIMESTAMPTZ,
    expires_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ DEFAULT NOW(),
    updated_at        TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_listings_user ON listings(user_id);
CREATE INDEX idx_listings_status ON listings(status);
CREATE INDEX idx_listings_city ON listings(city);
CREATE INDEX idx_listings_type ON listings(type);
CREATE INDEX idx_listings_deal_type ON listings(deal_type);
CREATE INDEX idx_listings_price ON listings(price);
CREATE INDEX idx_listings_rooms ON listings(rooms);
CREATE INDEX idx_listings_premium ON listings(is_premium, published_at DESC);
CREATE INDEX idx_listings_created ON listings(created_at DESC);
CREATE INDEX idx_listings_city_status ON listings(city, status);

-- updated_at trigger (reuse from users migration)
CREATE TRIGGER trigger_listings_updated_at
    BEFORE UPDATE ON listings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
