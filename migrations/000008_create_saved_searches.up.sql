-- 000008_create_saved_searches.up.sql
-- Sprint 6: Saqlangan qidiruvlar jadvali

CREATE TABLE IF NOT EXISTS saved_searches (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Qidiruv parametrlari
    name            VARCHAR(100) NOT NULL,       -- "Toshkent 2-xonali"
    city            VARCHAR(100),
    district        VARCHAR(100),
    type            VARCHAR(20),                 -- 'apartment','house','office'
    min_price       DECIMAL(12,2),
    max_price       DECIMAL(12,2),
    rooms           SMALLINT,

    -- Bildirishnoma
    notify_enabled  BOOLEAN DEFAULT TRUE,        -- Yangi e'lon bo'lsa xabar berish
    last_notified   TIMESTAMPTZ,

    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_saved_searches_user ON saved_searches(user_id);
CREATE INDEX idx_saved_searches_notify ON saved_searches(notify_enabled) WHERE notify_enabled = TRUE;
