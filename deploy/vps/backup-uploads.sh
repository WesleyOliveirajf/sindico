#!/usr/bin/env bash
# Backup dos anexos (volume Docker "sindico_uploads"). O banco fica no Supabase e tem backup proprio.
# Uso manual ou via cron (ver README.md).
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/sindico}"
KEEP_DAYS="${KEEP_DAYS:-14}"
VOLUME="${VOLUME:-sindico_uploads}"
STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="$BACKUP_DIR/uploads-$STAMP.tar.gz"

mkdir -p "$BACKUP_DIR"

docker run --rm -v "$VOLUME":/data:ro -v "$BACKUP_DIR":/out alpine \
  tar czf "/out/uploads-$STAMP.tar.gz" -C /data .

# Falha se o arquivo nao for um tar.gz valido.
tar tzf "$OUT" > /dev/null

find "$BACKUP_DIR" -type f -name 'uploads-*.tar.gz' -mtime +"$KEEP_DAYS" -delete

# Copia off-site (recomendado). Descomente apos configurar `rclone config`:
# rclone copy "$OUT" remoto:sindico-backups

echo "backup ok: $OUT ($(du -h "$OUT" | cut -f1))"
