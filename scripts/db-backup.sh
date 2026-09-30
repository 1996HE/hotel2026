#!/usr/bin/env sh
set -eu

backup_dir="${1:-./backups}"
timestamp="$(date +%Y%m%d-%H%M%S)"
mkdir -p "$backup_dir"

PGPASSWORD="${DB_PASSWORD:?DB_PASSWORD is required}" pg_dump \
  --host="${DB_HOST:-localhost}" \
  --port="${DB_PORT:-55432}" \
  --username="${DB_USERNAME:-minshuku}" \
  --dbname="${DB_NAME:-minshuku}" \
  --format=custom \
  --file="$backup_dir/minshuku-$timestamp.dump"

echo "Backup created: $backup_dir/minshuku-$timestamp.dump"
