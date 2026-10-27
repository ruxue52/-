@echo off
title ShuHaiGe System Launcher
set "ROOT=%~dp0"
set "ROOT=%ROOT:~0,-1%"

echo ==========================================
echo   ShuHaiGe - Bookstore ERP  Starting ...
echo ==========================================
echo.
echo [1/2] Starting backend (port 8080) ...
start "ShuHaiGe-Backend" /d "%ROOT%\03-backend" cmd /k java -jar target\psi-backend.jar
echo       waiting for backend to be ready ...
ping -n 13 127.0.0.1 >nul
echo.
echo [2/2] Starting web admin (port 5173) ...
start "ShuHaiGe-Web" /d "%ROOT%\04-frontend-web" cmd /k npm run dev
ping -n 6 127.0.0.1 >nul
echo.
echo   Backend : http://127.0.0.1:8080   (serves all clients)
echo   Web     : http://localhost:5173  (admin console)
echo   Account : admin / 123456
echo.
echo   Tip: close the "ShuHaiGe-Backend" / "ShuHaiGe-Web"
echo        windows to stop the services.
echo.
pause
