@echo off
chcp 65001 >nul
title Pobar 酒吧系統 — 啟動中

echo ==========================================
echo   Pobar 酒吧管理系統  ^|  啟動中...
echo ==========================================
echo.

cd /d "%~dp0\.."

REM 檢查 Docker 是否在執行
docker info >nul 2>&1
if errorlevel 1 (
    echo [!] Docker 尚未啟動，請先開啟 Docker Desktop，等待圖示變為綠色後再執行此程式。
    pause
    exit /b 1
)

echo [1/2] 啟動所有服務中（首次啟動需要幾分鐘）...
docker compose up -d --build

if errorlevel 1 (
    echo.
    echo [X] 啟動失敗！請截圖上方錯誤訊息並聯絡工程師。
    pause
    exit /b 1
)

echo.
echo [2/2] 等待系統就緒...
timeout /t 8 /nobreak >nul

echo.
echo ==========================================
echo   系統已啟動！
echo   請使用瀏覽器開啟：http://localhost
echo ==========================================
echo.

start http://localhost

echo 按任意鍵關閉此視窗...
pause >nul
