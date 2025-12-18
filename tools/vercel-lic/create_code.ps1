param(
  [Parameter(Mandatory=$true)][string]$BaseUrl,
  [Parameter(Mandatory=$true)][string]$AdminToken
)

$ErrorActionPreference = 'Stop'

$uri = $BaseUrl.TrimEnd('/') + '/admin_create_code'
$headers = @{ Authorization = "Bearer $AdminToken" }

try {
  $res = Invoke-RestMethod -Method Post -Uri $uri -Headers $headers -ContentType 'application/json' -Body '{}'
  $res.code
} catch {
  $status = $null
  $body = $null

  try { $status = $_.Exception.Response.StatusCode.value__ } catch { }
  try {
    $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    $body = $sr.ReadToEnd()
    $sr.Close()
  } catch { }

  Write-Host "Request failed." -ForegroundColor Red
  if ($status -ne $null) { Write-Host "HTTP $status" -ForegroundColor Yellow }
  if ($body) { Write-Host $body }

  if ($status -eq 401) {
    Write-Host "Hint: Vercel /api/admin_create_code compares your Bearer token with env var NV_ADMIN_TOKEN." -ForegroundColor Yellow
    Write-Host "- Ensure NV_ADMIN_TOKEN is set in Vercel (Production)" -ForegroundColor Yellow
    Write-Host "- Redeploy after changing env vars" -ForegroundColor Yellow
    Write-Host "- Make sure token matches exactly (no spaces/newlines)" -ForegroundColor Yellow
  }

  throw
}
