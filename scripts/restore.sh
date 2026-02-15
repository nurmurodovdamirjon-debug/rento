#!/bin/bash
# ====================================================
# RENTO — PostgreSQL Restore Script
# ====================================================
# Backupdan ma'lumotlar bazasini tiklash
# Ishlatish: ./restore.sh <backup_file>
# ====================================================

set -euo pipefail

if [ $# -eq 0 ]; then
    echo "Ishlatish: $0 <backup_fayl>"
    echo ""
    echo "Mavjud backuplar:"
    ls -lh /backups/rento_*.sql.gz 2>/dev/null || echo "  Hech qanday backup topilmadi"
    exit 1
fi

BACKUP_FILE="$1"

if [ ! -f "$BACKUP_FILE" ]; then
    echo "❌ Fayl topilmadi: $BACKUP_FILE"
    exit 1
fi

echo "⚠️  OGOHLANTIRISH: Bu amal mavjud ma'lumotlarni o'chiradi!"
echo "Tiklanadigan fayl: $BACKUP_FILE"
echo ""
read -p "Davom etasizmi? (yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo "Bekor qilindi."
    exit 0
fi

echo "📦 Backup tiklash boshlandi..."

# Decompress if needed
if [[ "$BACKUP_FILE" == *.gz ]]; then
    TEMP_FILE="${BACKUP_FILE%.gz}"
    gunzip -k "$BACKUP_FILE"
    BACKUP_FILE="$TEMP_FILE"
    CLEANUP_TEMP=true
else
    CLEANUP_TEMP=false
fi

# Restore
pg_restore \
    -h "${PGHOST:-localhost}" \
    -p "${PGPORT:-5432}" \
    -U "${PGUSER:-rento_user}" \
    -d "${PGDATABASE:-rento}" \
    --no-password \
    --clean \
    --if-exists \
    --verbose \
    "$BACKUP_FILE" 2>&1 | tail -20

# Cleanup temp file
if [ "$CLEANUP_TEMP" = true ]; then
    rm -f "$TEMP_FILE"
fi

echo ""
echo "✅ Ma'lumotlar bazasi muvaffaqiyatli tiklandi!"
echo "Tiklanish vaqti: $(date)"
