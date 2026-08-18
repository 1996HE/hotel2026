$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectDir = Split-Path -Parent $ScriptDir
Set-Location $ProjectDir

# 以 UTF-8 读取中日文演示内容，并在任一 SQL 错误发生时立即停止。
Get-Content -Raw -Encoding UTF8 "$ScriptDir/seed-demo-data.sql" |
    docker compose exec -T db psql -U minshuku -d minshuku -v ON_ERROR_STOP=1

if ($LASTEXITCODE -ne 0) {
    throw "演示数据写入失败，事务已回滚。"
}

Write-Host "演示数据已准备完成：每个业务表均包含 30 条可识别的演示记录。"
