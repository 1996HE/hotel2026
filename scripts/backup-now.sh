#!/usr/bin/env sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
cd "$PROJECT_DIR"

BACKUP_DIR=${BACKUP_DIRECTORY:-./backups}
mkdir -p "$BACKUP_DIR"
BACKUP_FILE="$BACKUP_DIR/minshuku-manual-$(date '+%Y%m%d-%H%M%S').dump"

# The archive is streamed directly to the host-selected backup folder.
docker compose exec -T db pg_dump -U minshuku -d minshuku --format=custom --no-owner --no-acl > "$BACKUP_FILE"
echo "Backup created: $BACKUP_FILE"
