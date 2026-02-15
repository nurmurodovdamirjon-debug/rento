#!/bin/bash
# ====================================================
# RENTO — PostgreSQL Backup Script
# ====================================================
# Har 6 soatda ishga tushadi (docker-compose.prod.yml orqali)
# Backup formati: custom (pg_restore bilan tiklash mumkin)
# Saqlash muddati: 30 kun (auto-cleanup)
# ====================================================

set -euo pipefail

# ===== KONFIGURATSIYA =====
BACKUP_DIR="/backups"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/rento_${TIMESTAMP}.sql.gz"
LOG_FILE="${BACKUP_DIR}/backup.log"

# ===== RANG KODLARI =====
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log() {
    echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# ===== BACKUP JILDINI YARATISH =====
mkdir -p "$BACKUP_DIR"

log "${GREEN}===== BACKUP BOSHLANDI =====${NC}"
log "Database: ${PGDATABASE}"
log "Host: ${PGHOST}:${PGPORT}"
log "Backup fayl: ${BACKUP_FILE}"

# ===== PG_DUMP =====
if pg_dump \
    -h "$PGHOST" \
    -p "$PGPORT" \
    -U "$PGUSER" \
    -d "$PGDATABASE" \
    --no-password \
    --verbose \
    --format=custom \
    --compress=9 \
    --file="${BACKUP_FILE%.gz}" \
    2>> "$LOG_FILE"; then

    # Compress
    gzip "${BACKUP_FILE%.gz}" 2>> "$LOG_FILE" || true

    BACKUP_SIZE=$(du -h "$BACKUP_FILE" 2>/dev/null | cut -f1 || echo "N/A")
    log "${GREEN}✅ Backup muvaffaqiyatli: ${BACKUP_FILE} (${BACKUP_SIZE})${NC}"
else
    log "${RED}❌ Backup XATO! pg_dump muvaffaqiyatsiz${NC}"
    exit 1
fi

# ===== ESKI BACKUP LARNI TOZALASH =====
log "${YELLOW}Eski backuplarni tozalash (${RETENTION_DAYS} kundan eski)...${NC}"
DELETED_COUNT=0

find "$BACKUP_DIR" -name "rento_*.sql.gz" -mtime +${RETENTION_DAYS} -print0 | while IFS= read -r -d '' file; do
    rm -f "$file"
    log "O'chirildi: $file"
    DELETED_COUNT=$((DELETED_COUNT + 1))
done

# ===== STATISTIKA =====
TOTAL_BACKUPS=$(find "$BACKUP_DIR" -name "rento_*.sql.gz" | wc -l)
TOTAL_SIZE=$(du -sh "$BACKUP_DIR" 2>/dev/null | cut -f1 || echo "N/A")

log "Jami backuplar: ${TOTAL_BACKUPS}"
log "Jami hajm: ${TOTAL_SIZE}"
log "${GREEN}===== BACKUP YAKUNLANDI =====${NC}"
echo ""

# ===== SOGLIQNI TEKSHIRISH FAYLI =====
echo "$TIMESTAMP" > "${BACKUP_DIR}/last_backup_timestamp"
