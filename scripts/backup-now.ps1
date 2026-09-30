$ErrorActionPreference = "Stop"
Set-Location (Split-Path -Parent $PSScriptRoot)
$BackupDir = if ($env:BACKUP_DIRECTORY) { $env:BACKUP_DIRECTORY } else { ".\backups" }
New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null
$BackupFile = Join-Path $BackupDir ("minshuku-manual-{0}.dump" -f (Get-Date -Format "yyyyMMdd-HHmmss"))

# Use cmd redirection because PowerShell 5 can alter binary pipeline bytes.
cmd /c "docker compose exec -T db pg_dump -U minshuku -d minshuku --format=custom --no-owner --no-acl > `"$BackupFile`""
if ($LASTEXITCODE -ne 0) { throw "Backup failed." }
Write-Host "Backup created: $BackupFile"
