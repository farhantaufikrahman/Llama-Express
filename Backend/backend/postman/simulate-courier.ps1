param(
  [Parameter(Mandatory)] [string]$Email,
  [Parameter(Mandatory)] [string]$Password,
  [double]$FromLat = -6.900, [double]$FromLng = 107.600,
  [double]$ToLat   = -6.910, [double]$ToLng   = 107.600,
  [int]$Steps = 20,
  [string]$Api = "http://localhost:8080/api/v1"
)
$ErrorActionPreference = "Stop"
$login = Invoke-RestMethod -Method Post -Uri "$Api/auth/login" -ContentType "application/json" `
  -Body (@{ email = $Email; password = $Password } | ConvertTo-Json)
$headers = @{ Authorization = "Bearer $($login.accessToken)" }

for ($i = 0; $i -le $Steps; $i++) {
  $t = $i / $Steps
  $body = @{ lat = $FromLat + ($ToLat - $FromLat) * $t; lng = $FromLng + ($ToLng - $FromLng) * $t } | ConvertTo-Json
  Invoke-RestMethod -Method Post -Uri "$Api/courier/me/location" -Headers $headers -ContentType "application/json" -Body $body | Out-Null
  Write-Host ("Titik {0}/{1}: {2:N6}, {3:N6}" -f $i, $Steps, ($FromLat + ($ToLat - $FromLat) * $t), ($FromLng + ($ToLng - $FromLng) * $t))
  Start-Sleep -Seconds 3
}