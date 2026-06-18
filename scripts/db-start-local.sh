#!/usr/bin/env bash
# 使用 bash 执行本地数据库启动脚本。
set -euo pipefail # 遇到错误、未定义变量或管道失败时立即退出。
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)" # 计算项目根目录绝对路径。
DATA_DIR="${ROOT_DIR}/.local/postgres" # 定义 PostgreSQL 数据目录。
LOG_FILE="${ROOT_DIR}/.local/postgres.log" # 定义 PostgreSQL 日志文件路径。
mkdir -p "${ROOT_DIR}/.local" # 确保本地运行目录存在。
if [ ! -d "${DATA_DIR}" ]; then initdb -D "${DATA_DIR}"; fi # 数据目录不存在时初始化数据库集群。
pg_ctl -D "${DATA_DIR}" -l "${LOG_FILE}" -o "-p 55432" start || true # 启动 PostgreSQL，已启动时不让脚本失败。
createuser -h localhost -p 55432 minshuku 2>/dev/null || true # 创建应用数据库用户，已存在时忽略。
createdb -h localhost -p 55432 -O minshuku minshuku 2>/dev/null || true # 创建应用数据库，已存在时忽略。
echo "PostgreSQL is ready on localhost:55432." # 输出数据库就绪提示。
