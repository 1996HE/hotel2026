#!/usr/bin/env sh
set -eu
if [ "$#" -ne 1 ] || [ ! -f "$1" ]; then
  echo "Usage: $0 /absolute/path/to/minshuku-backup.dump"
  exit 1
fi

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
RESTORE_FILE=$1
cd "$PROJECT_DIR"

echo "This replaces the current minshuku database with: $RESTORE_FILE"
printf "Type RESTORE to continue: "
read -r CONFIRMATION
[ "$CONFIRMATION" = "RESTORE" ] || { echo "Cancelled."; exit 1; }

docker compose stop app
docker compose exec -T db pg_restore -U minshuku -d minshuku --clean --if-exists --no-owner --no-acl < "$RESTORE_FILE"
docker compose start app
echo "Restore completed."
