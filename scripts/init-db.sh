#!/bin/bash
# Rento — Database initialization script
# Bu script PostgreSQL container ishga tushganda avtomatik ishlaydi

set -e

echo "=== Rento DB Init ==="

# PostGIS extension
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- PostGIS extension (geo queries uchun)
    CREATE EXTENSION IF NOT EXISTS postgis;

    -- pgcrypto extension (UUID generation uchun)
    CREATE EXTENSION IF NOT EXISTS pgcrypto;

    -- pg_trgm extension (fuzzy search uchun)
    CREATE EXTENSION IF NOT EXISTS pg_trgm;

    -- Confirmation
    SELECT 'Rento DB initialized successfully' AS status;
    SELECT postgis_version() AS postgis_version;
EOSQL

echo "=== Rento DB Init Complete ==="
