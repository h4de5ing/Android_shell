@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

:: ===== 配置 =====
set XAPK_FILE=%~dp0ScreenStream_4.1.13_APKPure.xapk
set TEMP_DIR=%TEMP%\xapk_install_%RANDOM%
:: ================

echo [1] 检查 adb...
adb version >nul 2>&1
if errorlevel 1 (
    echo 错误: 未找到 adb，请确保 adb 已加入 PATH
    pause & exit /b 1
)

echo [2] 检查设备连接...
adb get-state >nul 2>&1
if errorlevel 1 (
    echo 错误: 未检测到已连接的设备，请检查 USB 连接和调试模式
    pause & exit /b 1
)

echo [3] 解压 xapk 到临时目录: %TEMP_DIR%
mkdir "%TEMP_DIR%"
D:\soft\7-Zip\7z.exe x "%XAPK_FILE%" -o"%TEMP_DIR%" -y >nul
if errorlevel 1 (
    echo 错误: 解压失败
    rd /s /q "%TEMP_DIR%"
    pause & exit /b 1
)

echo [4] 收集 APK 文件...
set APK_LIST=
for /r "%TEMP_DIR%" %%f in (*.apk) do (
    set APK_LIST=!APK_LIST! "%%f"
)

if "!APK_LIST!"=="" (
    echo 错误: xapk 中未找到任何 .apk 文件
    rd /s /q "%TEMP_DIR%"
    pause & exit /b 1
)

echo 找到以下 APK:
for /r "%TEMP_DIR%" %%f in (*.apk) do echo   %%~nxf

echo [5] 执行 adb install-multiple ...
adb install-multiple -r !APK_LIST!
if errorlevel 1 (
    echo 安装失败！
) else (
    echo 安装成功！
)

echo [6] 清理临时文件...
rd /s /q "%TEMP_DIR%"

pause
