-- Chat xonalari (tenant ↔ landlord, e'lon kontekstida)
CREATE TABLE IF NOT EXISTS chat_rooms (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id      UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    tenant_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    landlord_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    last_message_at TIMESTAMPTZ,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),

    -- Bitta e'lon uchun tenant-landlord juftligi yagona bo'lishi kerak
    CONSTRAINT uq_chat_room_listing_users UNIQUE (listing_id, tenant_id, landlord_id),
    -- O'ziga o'zi yozish mumkin emas
    CONSTRAINT chk_different_users CHECK (tenant_id <> landlord_id)
);

CREATE INDEX idx_chat_rooms_tenant ON chat_rooms(tenant_id, last_message_at DESC);
CREATE INDEX idx_chat_rooms_landlord ON chat_rooms(landlord_id, last_message_at DESC);
CREATE INDEX idx_chat_rooms_listing ON chat_rooms(listing_id);
CREATE INDEX idx_chat_rooms_active ON chat_rooms(is_active) WHERE is_active = TRUE;
