#!/usr/bin/env bash
# Create a NEW application account; no DROP USER and no permission reset.
set -euo pipefail
: "${DB_NAME:?Set an existing migrated database}"
: "${FRESH_APP_PASSWORD:?Set a randomly generated 20+ character application password}"
: "${MYSQL_ROOT_PASSWORD:?Set the DBA password}"
APP_DB_USER="${APP_DB_USER:-cfd_app}"
APP_DB_HOST="${APP_DB_HOST:-localhost}"
[[ "$DB_NAME" =~ ^[A-Za-z][A-Za-z0-9_]{0,63}$ ]] || exit 2
[[ "$APP_DB_USER" =~ ^[A-Za-z][A-Za-z0-9_]{0,31}$ ]] || exit 2
[[ "$APP_DB_HOST" =~ ^[A-Za-z0-9_.-]+$ ]] || exit 2
[[ "$FRESH_APP_PASSWORD" =~ ^[A-Za-z0-9_-]{20,128}$ ]] || { echo "Use a 20-128 character random alphanumeric/underscore/hyphen password." >&2; exit 2; }
export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"
mysql --no-defaults --protocol=TCP --host="${DB_HOST:-127.0.0.1}" --port="${DB_PORT:-3306}" --user="${MYSQL_ROOT_USER:-root}" <<SQL
CREATE USER '$APP_DB_USER'@'$APP_DB_HOST' IDENTIFIED BY '$FRESH_APP_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE ON `$DB_NAME`.* TO '$APP_DB_USER'@'$APP_DB_HOST';
SHOW GRANTS FOR '$APP_DB_USER'@'$APP_DB_HOST';
SQL
