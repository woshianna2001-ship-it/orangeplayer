@echo off
chcp 65001 >nul
echo ========================================
echo   OrangePlayer Legacy 电视崩溃诊断工具
echo ========================================
echo.

REM 检查 ADB 是否可用
where adb >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 ADB 工具
    echo 请确保 Android SDK Platform-Tools 已安装并添加到 PATH
    pause
    exit /b 1
)

echo [1/6] 检查设备连接...
adb devices
echo.

echo [2/6] 检查应用是否已安装...
adb shell pm list packages | findstr "com.orange.player.legacy"
if %errorlevel% neq 0 (
    echo [警告] 应用未安装或设备未连接
)
echo.

echo [3/6] 查看最近的崩溃日志...
echo ----------------------------------------
adb logcat -d | findstr /C:"FATAL EXCEPTION" /C:"AndroidRuntime" /C:"com.orange.player.legacy"
echo ----------------------------------------
echo.

echo [4/6] 导出完整日志到文件...
set LOGFILE=tv_crash_log_%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%.txt
set LOGFILE=%LOGFILE: =0%
adb logcat -d > "%LOGFILE%"
echo 日志已保存到: %LOGFILE%
echo.

echo [5/6] 检查应用崩溃日志文件...
adb shell "ls -la /sdcard/Android/data/com.orange.player.legacy/files/crash_logs/"
echo.

echo [6/6] 拉取应用崩溃日志...
if not exist "crash_logs" mkdir crash_logs
adb pull /sdcard/Android/data/com.orange.player.legacy/files/crash_logs/ crash_logs/
echo.

echo ========================================
echo 诊断完成！
echo.
echo 请检查以下文件：
echo 1. %LOGFILE% - 完整系统日志
echo 2. crash_logs\ - 应用崩溃日志
echo ========================================
echo.

pause
