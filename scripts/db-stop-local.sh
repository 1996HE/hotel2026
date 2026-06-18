#!/usr/bin/env bash
# 使用 bash 执行本地数据库停止脚本。
set -euo pipefail # 遇到错误、未定义变量或管道失败时立即退出。
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)" # 计算项目根目录绝对路径。
DATA_DIR="${ROOT_DIR}/.local/postgres" # 定义 PostgreSQL 数据目录。
if [ -d "${DATA_DIR}" ]; then pg_ctl -D "${DATA_DIR}" stop; fi # 数据目录存在时停止 PostgreSQL。
