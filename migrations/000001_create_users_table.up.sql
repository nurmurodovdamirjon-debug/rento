-- 000001_create_users_table.up.sql
-- Sprint 1: Foydalanuvchilar jadvali

CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone           VARCHAR(20) UNIQUE NOT NULL,
    phone_verified  BOOLEAN DEFAULT FALSE,
    full_name       VARCHAR(100),
    email           VARCHAR(255),
    avatar_url      VARCHAR(500),
    role            VARCHAR(20) DEFAULT 'tenant',       -- 'tenant','landlord','both','admin'
    id_verified     BOOLEAN DEFAULT FALSE,
    id_document_url VARCHAR(500),
    id_verified_at  TIMESTAMPTZ,
    rating_avg      DECIMAL(3,2) DEFAULT 0.00,
    rating_count    INTEGER DEFAULT 0,
    subscription    VARCHAR(20) DEFAULT 'free',          -- 'free','pro'
    sub_expires_at  TIMESTAMPTZ,
    language        VARCHAR(5) DEFAULT 'uz',             -- 'uz','ru','en'
    last_seen_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    is_active       BOOLEAN DEFAULT TRUE,
    is_blocked      BOOLEAN DEFAULT FALSE
);

-- Indexes
CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_created_at ON users(created_at);

-- updated_at avtomatik trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
