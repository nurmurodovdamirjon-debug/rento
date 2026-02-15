-- Xabarlar jadvali
CREATE TABLE IF NOT EXISTS messages (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id      UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    sender_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content      TEXT,
    message_type VARCHAR(10) NOT NULL DEFAULT 'text',
    media_url    VARCHAR(500),
    metadata     JSONB,
    is_read      BOOLEAN DEFAULT FALSE,
    read_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT chk_message_type CHECK (message_type IN ('text', 'image', 'location', 'contact')),
    -- Text xabarlarda content bo'sh bo'lmasligi kerak
    CONSTRAINT chk_content_or_media CHECK (
        (message_type = 'text' AND content IS NOT NULL AND content <> '') OR
        (message_type = 'image' AND media_url IS NOT NULL) OR
        (message_type IN ('location', 'contact') AND metadata IS NOT NULL) OR
        content IS NOT NULL
    )
);

CREATE INDEX idx_messages_room_created ON messages(room_id, created_at DESC);
CREATE INDEX idx_messages_sender ON messages(sender_id);
CREATE INDEX idx_messages_unread ON messages(room_id, is_read) WHERE is_read = FALSE;
