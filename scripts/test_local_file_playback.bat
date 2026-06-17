@echo off
chcp 65001 >nul
echo ========================================
echo IJK 播放器本地文件播放测试
echo ========================================
echo.

echo [1] 检查设备连接...
adb devices
if %errorlevel% neq 0 (
    echo 错误: 未检测到 ADB 设备
    pause
    exit /b 1
)
echo.

echo [2] 清除日志缓存...
adb logcat -c
echo 日志已清除
echo.

echo [3] 查找包含 com.jsg 的应用...
adb shell pm list packages | findstr com.jsg
if %errorlevel% neq 0 (
    echo 警告: 未找到 com.jsg 应用
)
echo.

echo [4] 开始监控播放器日志...
echo 提示: 请在应用中播放本地视频文件
echo 按 Ctrl+C 停止监控
echo.
echo ----------------------------------------
adb logcat | findstr /i "IJKMEDIA OrangevideoView Orange IJK protocol whitelist"
