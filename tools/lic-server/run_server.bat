@echo off
setlocal
cd /d "%~dp0"

REM Create venv if missing
if not exist ".venv\Scripts\python.exe" (
  py -m venv .venv
)

REM Install deps (safe to run every time)
".venv\Scripts\python.exe" -m pip install -r requirements.txt

REM Run server
echo Starting lic-server at http://127.0.0.1:8787
echo (Do not close this window while activating)
".venv\Scripts\python.exe" -m uvicorn lic_server:APP --host 127.0.0.1 --port 8787

if errorlevel 1 (
  echo.
  echo lic-server exited with an error.
  echo Tip: check if port 8787 is already in use.
  echo.
  pause
)
