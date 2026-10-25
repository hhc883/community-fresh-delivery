#!/usr/bin/env bash
# Explicit database, unique filename, no automatic deletion. Verify restoration separately.
set -euo pipefail
: "${DB_NAME:?Set DB_NAME}"
: "${DB_USER:?Set a backup account}"
: "${DB_PASSWORD:?Set DB_PASSWORD}"
: "${BACKUP_DIR:?Set an absolute backup directory}"
[[ "$DB_NAME" =~ ^[A-Za-z][A-Za-z0-9_]{0,63}$ ]] || exit 2
mkdir -p -- "$BACKUP_DIR"
export MYSQL_PWD="$DB_PASSWORD"
OUT_FILE="$BACKUP_DIR/${DB_NAME}_$(date +%Y%m%d_%H%M%S)_$$.sql.gz"
mysqldump --no-defaults --protocol=TCP --host="${DB_HOST:-127.0.0.1}" --port="${DB_PORT:-3306}" --user="$DB_USER" --single-transaction --no-tablespaces "$DB_NAME" | gzip > "$OUT_FILE"
test -s "$OUT_FILE"
gzip -t "$OUT_FILE"
echo "Backup created: $OUT_FILE"
