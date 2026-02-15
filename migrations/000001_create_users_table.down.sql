-- 000001_create_users_table.down.sql
-- Rollback: users jadvali va trigger o'chirish

DROP TRIGGER IF EXISTS trigger_users_updated_at ON users;
DROP FUNCTION IF EXISTS update_updated_at_column();
DROP TABLE IF EXISTS users;
