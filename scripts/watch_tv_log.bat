@echo off
chcp 65001 >nul
echo ========================================
echo   实时查看电视应用日志
echo ========================================
echo.

REM 检查 ADB 是否可用
where adb >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 ADB 工具
    pause
    exit /b 1
)

echo 检查设备连接...
adb devices
echo.

echo 清除旧日志...
adb logcat -c
echo.

echo ========================================
echo 开始监控日志（按 Ctrl+C 停止）
echo ========================================
echo.

REM 只显示错误和应用相关日志
adb logcat *:E OrangeApplication:D OrangevideoView:D GSYVideoPlayer:D AndroidRuntime:E

pause
