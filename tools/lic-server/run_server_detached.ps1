$ErrorActionPreference = 'Stop'

Set-Location -LiteralPath $PSScriptRoot

if (!(Test-Path -LiteralPath '.\.venv\Scripts\python.exe')) {
  py -m venv .venv
}

& .\.venv\Scripts\python.exe -m pip install -r requirements.txt | Out-Host

$proc = Start-Process -FilePath (Resolve-Path '.\.venv\Scripts\python.exe') `
  -WorkingDirectory $PSScriptRoot `
  -ArgumentList @('-m','uvicorn','lic_server:APP','--host','127.0.0.1','--port','8787') `
  -PassThru

Write-Host "lic-server started. PID=$($proc.Id) URL=http://127.0.0.1:8787"
