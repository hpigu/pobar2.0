@echo off
chcp 65001 >nul
title Pobar 酒吧系統 — 關閉中

echo ==========================================
echo   Pobar 酒吧管理系統  ^|  關閉中...
echo ==========================================
echo.

cd /d "%~dp0\.."

docker info >nul 2>&1
if errorlevel 1 (
    echo [i] Docker 尚未啟動，系統應已是關閉狀態。
    pause
    exit /b 0
)

echo 正在關閉所有服務...
docker compose down

echo.
echo ==========================================
echo   系統已關閉。
echo ==========================================
echo.
echo 按任意鍵關閉此視窗...
pause >nul
