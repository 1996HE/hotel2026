#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE_CONF_FILE="${ROOT_DIR}/config/local-httpd-jukai.conf"
RUNTIME_CONF_FILE="/tmp/hotel-management-jukai-httpd.conf"

if ! grep -qE '^[[:space:]]*127\.0\.0\.1[[:space:]]+jukai\.internal([[:space:]]|$)' /etc/hosts; then
  echo "Add this line to /etc/hosts first:"
  echo "127.0.0.1 jukai.internal"
  echo
  echo "Example:"
  echo "sudo sh -c 'echo 127.0.0.1 jukai.internal >> /etc/hosts'"
  exit 1
fi

cp "${SOURCE_CONF_FILE}" "${RUNTIME_CONF_FILE}"
exec httpd -f "${RUNTIME_CONF_FILE}" -DFOREGROUND
