$ErrorActionPreference = "Stop"
Set-Location (Split-Path -Parent $PSScriptRoot)
# Keep the database volume and backup files; only stop containers.
docker compose down
