#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
cd "$PROJECT_DIR"

# 通过运行中的 PostgreSQL 容器执行；ON_ERROR_STOP 确保任一错误都会整体回滚。
docker compose exec -T db psql \
  -U minshuku \
  -d minshuku \
  -v ON_ERROR_STOP=1 \
  < "$SCRIPT_DIR/seed-demo-data.sql"

echo "Demo data is ready: each business table contains its 30 identifiable demo rows."
