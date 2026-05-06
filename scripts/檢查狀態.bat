@echo off
chcp 65001 >nul
title Pobar 酒吧系統 — 狀態檢查

echo ==========================================
echo   Pobar 酒吧管理系統  ^|  系統狀態
echo ==========================================
echo.

cd /d "%~dp0\.."

docker info >nul 2>&1
if errorlevel 1 (
    echo [X] Docker 未執行 — 系統處於關閉狀態。
    echo.
    echo 按任意鍵關閉此視窗...
    pause >nul
    exit /b 0
)

echo 容器狀態：
echo.
docker compose ps
echo.

REM 確認 frontend 容器是否在跑
docker compose ps --services --filter status=running | findstr /i "frontend" >nul 2>&1
if errorlevel 1 (
    echo [X] 系統尚未啟動，請執行「啟動系統.bat」。
) else (
    echo [O] 系統運作正常！
    echo     前台點餐：http://localhost
    echo.
    start http://localhost
)

echo.
echo 按任意鍵關閉此視窗...
pause >nul
