@echo off
setlocal
cd /d "%~dp0"
if not exist "node_modules\electron\package.json" (
    call npm.cmd ci
    if errorlevel 1 goto failed
)
call npm.cmd start
if errorlevel 1 goto failed
exit /b 0

:failed
echo OrbitVisuals Launcher could not start.
pause
exit /b 1
