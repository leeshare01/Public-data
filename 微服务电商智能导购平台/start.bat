@echo off
chcp 65001 >nul
title EShop Launcher

REM ============================================================
REM  E-Shop Microservice Launcher (Windows)
REM
REM  Optional env vars:
REM    JAVA_HOME         JDK directory. Falls back to "java" on PATH.
REM    DEEPSEEK_API_KEY  DeepSeek API key. Required only for AI guide chat.
REM ============================================================

REM Project root = the directory containing this script (trailing backslash).
REM Works no matter where the repo is cloned - no manual edit needed.
set "PROJECT_DIR=%~dp0"

if defined JAVA_HOME (
    set "JAVA=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA=java"
)

echo ============================================
echo   E-Shop Microservice Launcher
echo   Project: %PROJECT_DIR%
echo ============================================
echo.

if not defined DEEPSEEK_API_KEY (
    echo   [!!] DEEPSEEK_API_KEY is not set - AI guide chat will be unavailable.
)

REM ===== 1. Start Nacos =====
echo [1/4] Checking Nacos...
curl -s -o nul --max-time 2 http://localhost:8848/nacos/ 2>nul
if not errorlevel 1 (
    echo   [OK] Nacos already running
    goto :nacos_ready
)
if exist "%PROJECT_DIR%work\nacos\bin\startup.cmd" (
    echo   [..] Starting Nacos...
    start "nacos" "%PROJECT_DIR%work\nacos\bin\startup.cmd" -m standalone
    timeout /t 15 /nobreak >nul
    echo   [OK] Nacos started
    goto :nacos_ready
)
echo   [!!] Nacos not found at work\nacos\
echo        Nacos is not bundled with this repo. Download and extract it to work\nacos\:
echo        https://github.com/alibaba/nacos/releases   (2.3.x recommended)
echo        Or start Nacos manually, then re-run this script.
pause
exit /b 1

:nacos_ready

REM ===== 2. Check MySQL =====
echo [2/4] Checking MySQL...
tasklist /FI "IMAGENAME eq mysqld.exe" 2>nul | find /i "mysqld.exe" >nul
if errorlevel 1 (
    echo   [!!] MySQL not running - please start MySQL manually
)

REM ===== 3. Start Microservices =====
echo [3/4] Starting backend services...
if not exist "%PROJECT_DIR%logs\backend" mkdir "%PROJECT_DIR%logs\backend"
cd /d "%PROJECT_DIR%springcloud-eshop"

echo   [..] gateway (:8086)...
start "gateway" "%JAVA%" -Dfile.encoding=UTF-8 -Xmx256m -Xms128m -jar eshop-gateway\target\eshop-gateway-app.jar --server.port=8086
timeout /t 8 /nobreak >nul

echo   [..] user-service (:8081)...
start "user" "%JAVA%" -Dfile.encoding=UTF-8 -Xmx256m -Xms128m -jar eshop-user-service\target\eshop-user-service-1.0.0.jar --server.port=8081
timeout /t 6 /nobreak >nul

echo   [..] product-service (:8082)...
start "product" "%JAVA%" -Dfile.encoding=UTF-8 -Xmx256m -Xms128m -jar eshop-product-service\target\eshop-product-service-1.0.0.jar --server.port=8082
timeout /t 6 /nobreak >nul

echo   [..] cart-service (:8083)...
start "cart" "%JAVA%" -Dfile.encoding=UTF-8 -Xmx256m -Xms128m -jar eshop-cart-service\target\eshop-cart-service-1.0.0.jar --server.port=8083
timeout /t 6 /nobreak >nul

echo   [..] order-service (:8084)...
start "order" "%JAVA%" -Dfile.encoding=UTF-8 -Xmx256m -Xms128m -jar eshop-order-service\target\eshop-order-service-1.0.0.jar --server.port=8084
timeout /t 6 /nobreak >nul

echo   [..] ai-guide-service (:8085)...
start "ai" "%JAVA%" -Dfile.encoding=UTF-8 -Xmx256m -Xms128m -jar eshop-ai-guide-service\target\eshop-ai-guide-service-1.0.0.jar --server.port=8085
timeout /t 10 /nobreak >nul

echo   [OK] All backend services started

REM ===== 4. Start Frontend =====
echo [4/4] Starting frontend...
cd /d "%PROJECT_DIR%frontend"
if not exist node_modules (
    echo   [..] Installing frontend dependencies ^(first run^)...
    call npm install
)
set NODE_OPTIONS=--max-old-space-size=4096
start "frontend" cmd /c "npm run dev -- --port 3010"

echo   [OK] Frontend =^> http://localhost:3010
echo.
echo ============================================
echo   All services started!
echo   Frontend:  http://localhost:3010
echo   Gateway:   http://localhost:8086
echo   Nacos:     http://localhost:8848/nacos
echo ============================================
pause
