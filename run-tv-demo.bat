@echo off
echo ========================================
echo   OrangePlayer TV Demo 启动脚本
echo ========================================
echo.

echo [1/3] 编译 TV 模块...
call gradlew.bat :app-tv:assembleDebug
if %errorlevel% neq 0 (
    echo.
    echo 编译失败！
    pause
    exit /b 1
)

echo.
echo [2/3] 检查设备连接...
adb devices
echo.

echo [3/3] 安装到设备...
adb install -r app-tv\build\outputs\apk\debug\app-tv-debug.apk
if %errorlevel% neq 0 (
    echo.
    echo 安装失败！请检查设备连接。
    pause
    exit /b 1
)

echo.
echo ========================================
echo   安装成功！
echo ========================================
echo.
echo 提示：
echo - 在 TV 启动器中找到 "OrangePlayer TV"
echo - 使用遥控器方向键导航
echo - 按确认键播放视频
echo.
pause
