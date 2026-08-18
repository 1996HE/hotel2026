$ErrorActionPreference = "Stop"
$ProjectDir = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectDir

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker Desktop is required. Install and start Docker, then run this script again."
}
if (-not (Test-Path ".env")) {
    Copy-Item ".env.example" ".env"
    Write-Host "Created .env. Change DB_PASSWORD before using the system outside this computer."
}

New-Item -ItemType Directory -Force -Path "backups" | Out-Null
docker compose up -d --build
Start-Process "http://localhost:8000/jukai-internal/"
Write-Host "The system is starting: http://localhost:8000/jukai-internal/"
