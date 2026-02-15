-- 000002_create_listings.down.sql
-- Rollback: listings jadvali

DROP TRIGGER IF EXISTS trigger_listings_updated_at ON listings;
DROP TABLE IF EXISTS listings;
