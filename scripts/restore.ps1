param([Parameter(Mandatory = $true)][string]$BackupFile)
$ErrorActionPreference = "Stop"
if (-not (Test-Path $BackupFile -PathType Leaf)) { throw "Backup file not found: $BackupFile" }
$ResolvedBackup = (Resolve-Path $BackupFile).Path
Set-Location (Split-Path -Parent $PSScriptRoot)

Write-Host "This replaces the current minshuku database with: $ResolvedBackup"
if ((Read-Host "Type RESTORE to continue") -ne "RESTORE") { Write-Host "Cancelled."; exit 1 }

docker compose stop app
cmd /c "docker compose exec -T db pg_restore -U minshuku -d minshuku --clean --if-exists --no-owner --no-acl < `"$ResolvedBackup`""
if ($LASTEXITCODE -ne 0) { throw "Restore failed; the app remains stopped for safety." }
docker compose start app
Write-Host "Restore completed."
