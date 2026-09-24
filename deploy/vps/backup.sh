#!/usr/bin/env bash
# Backup diario do Postgres + uploads. Uso: cron (ver README.md).
set -euo pipefail

cd "$(dirname "$0")"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/sindico}"
KEEP_DAYS="${KEEP_DAYS:-14}"
STAMP="$(date +%Y%m%d-%H%M%S)"

mkdir -p "$BACKUP_DIR"

docker compose exec -T db pg_dump -U sindico -d sindico --no-owner --no-privileges \
  | gzip > "$BACKUP_DIR/db-$STAMP.sql.gz"

# Uploads (volume nomeado do Compose)
docker run --rm -v sindico_uploads:/data:ro -v "$BACKUP_DIR":/out alpine \
  tar czf "/out/uploads-$STAMP.tar.gz" -C /data .

find "$BACKUP_DIR" -type f -mtime +"$KEEP_DAYS" -delete

# Copia off-site (recomendado): descomente apos configurar `rclone config`.
# rclone copy "$BACKUP_DIR" remoto:sindico-backups --max-age 2d

echo "backup ok: $STAMP"
