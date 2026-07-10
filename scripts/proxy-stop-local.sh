#!/usr/bin/env bash
set -euo pipefail

PID_FILE="/tmp/hotel-management-jukai-httpd.pid"

if [ ! -f "${PID_FILE}" ]; then
  echo "Local proxy is not running."
  exit 0
fi

httpd -f /tmp/hotel-management-jukai-httpd.conf -k stop
