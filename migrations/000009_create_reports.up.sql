-- 000009: Reports (shikoyatlar) jadval yaratish
-- E'lonlar yoki foydalanuvchilarga shikoyat yuborish

CREATE TABLE IF NOT EXISTS reports (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type     VARCHAR(20) NOT NULL CHECK (target_type IN ('listing', 'user', 'message')),
    target_id       UUID NOT NULL,
    reason          VARCHAR(50) NOT NULL CHECK (reason IN (
        'spam', 'fraud', 'inappropriate', 'duplicate',
        'wrong_info', 'offensive', 'illegal', 'other'
    )),
    description     TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN (
        'pending', 'reviewing', 'resolved', 'dismissed'
    )),
    admin_note      TEXT,
    resolved_by     UUID REFERENCES users(id),
    resolved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Bir foydalanuvchi bir target ga faqat bitta shikoyat
CREATE UNIQUE INDEX idx_reports_unique_reporter
    ON reports(reporter_id, target_type, target_id);

-- Admin uchun pending shikoyatlar
CREATE INDEX idx_reports_status ON reports(status) WHERE status IN ('pending', 'reviewing');

-- Target bo'yicha shikoyatlar
CREATE INDEX idx_reports_target ON reports(target_type, target_id);

-- Vaqt bo'yicha
CREATE INDEX idx_reports_created ON reports(created_at DESC);
