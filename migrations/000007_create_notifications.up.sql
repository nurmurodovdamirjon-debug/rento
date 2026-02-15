-- 000007_create_notifications.up.sql
-- Sprint 6: Bildirishnomalar jadvali

CREATE TABLE IF NOT EXISTS notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Turi va havolalar
    type            VARCHAR(50) NOT NULL,        -- 'new_message','listing_approved','listing_rejected','new_favorite','price_drop','system'
    title           VARCHAR(200) NOT NULL,
    body            VARCHAR(500) NOT NULL,

    -- Havola — polimorfik
    ref_type        VARCHAR(50),                 -- 'listing','chat_room','user'
    ref_id          UUID,

    -- Holat
    is_read         BOOLEAN DEFAULT FALSE,
    read_at         TIMESTAMPTZ,

    -- Push
    push_sent       BOOLEAN DEFAULT FALSE,
    push_sent_at    TIMESTAMPTZ,

    -- Meta
    metadata        JSONB,

    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read) WHERE is_read = FALSE;
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_created ON notifications(created_at DESC);

-- FCM token jadvali — mobil qurilmalar uchun
CREATE TABLE IF NOT EXISTS fcm_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token           VARCHAR(500) NOT NULL,
    device_type     VARCHAR(20) DEFAULT 'android',  -- 'android','ios','web'
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),

    UNIQUE (user_id, token)
);

CREATE INDEX idx_fcm_tokens_user ON fcm_tokens(user_id);
CREATE INDEX idx_fcm_tokens_active ON fcm_tokens(user_id, is_active) WHERE is_active = TRUE;
