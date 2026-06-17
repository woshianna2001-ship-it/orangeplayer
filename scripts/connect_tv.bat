@echo off
chcp 65001 >nul
echo ========================================
echo   ADB 无线连接电视工具
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

echo 请确保：
echo 1. 电视和电脑在同一个 WiFi 网络
echo 2. 电视已开启 ADB 调试（通常在开发者选项中）
echo 3. 已知电视的 IP 地址
echo.

set /p TV_IP=请输入电视的 IP 地址（例如：192.168.1.100）: 

if "%TV_IP%"=="" (
    echo [错误] IP 地址不能为空
    pause
    exit /b 1
)

echo.
echo [1/3] 连接到电视 %TV_IP%:5555 ...
adb connect %TV_IP%:5555

echo.
echo [2/3] 检查连接状态...
adb devices

echo.
echo [3/3] 测试连接...
adb shell echo "连接成功！"

echo.
echo ========================================
echo 连接完成！
echo.
echo 常用命令：
echo   查看日志: adb logcat
echo   安装应用: adb install app.apk
echo   卸载应用: adb uninstall com.orange.player.legacy
echo   断开连接: adb disconnect %TV_IP%:5555
echo ========================================
echo.

pause
