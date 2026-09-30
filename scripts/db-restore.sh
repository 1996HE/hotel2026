#!/usr/bin/env sh
set -eu

dump_file="${1:?usage: db-restore.sh DUMP_FILE}"
test -f "$dump_file"

PGPASSWORD="${DB_PASSWORD:?DB_PASSWORD is required}" pg_restore \
  --host="${DB_HOST:-localhost}" \
  --port="${DB_PORT:-55432}" \
  --username="${DB_USERNAME:-minshuku}" \
  --dbname="${DB_NAME:-minshuku}" \
  --clean \
  --if-exists \
  --no-owner \
  "$dump_file"

echo "Restore completed: $dump_file"
