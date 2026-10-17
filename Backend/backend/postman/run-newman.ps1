param([string]$BaseUrl = "http://localhost:8080")
$ErrorActionPreference = "Stop"

# Folder tempat script ini berada ($PSScriptRoot kadang kosong, jadi ada cadangan)
$here = $PSScriptRoot
if (-not $here -and $MyInvocation.MyCommand.Path) { $here = Split-Path -Parent $MyInvocation.MyCommand.Path }
if (-not $here) { $here = Join-Path (Get-Location) "postman" }
$backendDir = Split-Path -Parent $here

# Baca backend/.env -> nilai yang SAMA dengan yang dipakai Spring Boot
$dotenv = @{}
$envFile = Join-Path $backendDir ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | Where-Object { $_ -match '^\s*[A-Za-z_][A-Za-z0-9_]*\s*=' } | ForEach-Object {
        $k, $v = $_ -split '=', 2
        $dotenv[$k.Trim()] = $v.Trim().Trim('"')
    }
}

function Get-Setting([string]$name) {
    $fromShell = [Environment]::GetEnvironmentVariable($name)
    if ($fromShell) { return $fromShell }   # nilai dari terminal mengalahkan .env (berguna di Jenkins)
    return $dotenv[$name]
}

$adminEmail = Get-Setting "ADMIN_EMAIL"
$adminPassword = Get-Setting "ADMIN_PASSWORD"
$midtransKey = Get-Setting "MIDTRANS_SERVER_KEY"

if (-not $adminEmail) { throw "ADMIN_EMAIL tidak ditemukan di $envFile" }
if (-not $adminPassword) { throw "ADMIN_PASSWORD tidak ditemukan di $envFile" }
if (-not $midtransKey) { throw "MIDTRANS_SERVER_KEY tidak ditemukan di $envFile" }

$collection = Join-Path $here "llama-express.postman_collection.json"
if (-not (Test-Path $collection)) { throw "Collection tidak ditemukan di: $collection" }
if (-not (Test-Path (Join-Path $here "proof.jpg"))) { throw "proof.jpg tidak ditemukan di: $here" }

$out = Join-Path $backendDir "target\newman"
New-Item -ItemType Directory -Force -Path $out | Out-Null

Write-Host "Script     : run-newman.ps1 (v3, membaca .env)"
Write-Host "Collection : $collection"
Write-Host "Base URL   : $BaseUrl"
Write-Host "Admin      : $adminEmail"

$env:NODE_OPTIONS = "--no-deprecation"
$ErrorActionPreference = "Continue"

npx --yes newman@6 run $collection `
    --working-dir $here `
    --env-var "rootUrl=$BaseUrl" `
    --env-var "baseUrl=$BaseUrl/api/v1" `
    --env-var "adminEmail=$adminEmail" `
    --env-var "adminPassword=$adminPassword" `
    --env-var "midtransServerKey=$midtransKey" `
    --delay-request 200 `
    --timeout-request 20000 `
    --reporters cli, junit `
    --reporter-junit-export (Join-Path $out "results.xml")

exit $LASTEXITCODE