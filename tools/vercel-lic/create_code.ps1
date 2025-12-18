param(
  [Parameter(Mandatory=$true)][string]$BaseUrl,
  [Parameter(Mandatory=$true)][string]$AdminToken
)

$ErrorActionPreference = 'Stop'

$uri = $BaseUrl.TrimEnd('/') + '/admin_create_code'
$headers = @{ Authorization = "Bearer $AdminToken" }

$res = Invoke-RestMethod -Method Post -Uri $uri -Headers $headers -ContentType 'application/json' -Body '{}'
$res.code
