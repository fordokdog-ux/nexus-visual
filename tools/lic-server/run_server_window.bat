@echo off
setlocal
cd /d "%~dp0"

REM Starts the server in a NEW window so it won't stop when you run other commands.
start "Nexus Visual lic-server" cmd /k "call run_server.bat"
