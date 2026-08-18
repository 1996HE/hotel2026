#!/usr/bin/env sh
set -eu

# Resolve the repository from this script so it also works when double-clicked on macOS.
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
cd "$PROJECT_DIR"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker Desktop is required. Install and start Docker, then run this script again."
  exit 1
fi

if [ ! -f .env ]; then
  cp .env.example .env
  echo "Created .env. Change DB_PASSWORD before using the system outside this computer."
fi

mkdir -p backups
docker compose up -d --build
echo "The system is starting: http://localhost:8000/jukai-internal/"

if command -v open >/dev/null 2>&1; then
  open "http://localhost:8000/jukai-internal/"
elif command -v xdg-open >/dev/null 2>&1; then
  xdg-open "http://localhost:8000/jukai-internal/" >/dev/null 2>&1 || true
fi
