@echo off
chcp 65001 >nul
echo ========================================
echo 播放问题诊断工具
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

echo [2] 查找正在运行的应用...
set /p PKG_NAME="请输入应用包名 (默认: com.jsg): "
if "%PKG_NAME%"=="" set PKG_NAME=com.jsg

adb shell ps -A | findstr %PKG_NAME%
if %errorlevel% neq 0 (
    echo 警告: 应用 %PKG_NAME% 未运行
)
echo.

echo [3] 检查本地 HTTP 代理服务器...
echo 查找监听端口...
adb shell netstat -an | findstr "LISTEN" | findstr "127.0.0.1"
echo.

echo [4] 清除日志并开始监控...
adb logcat -c
echo 日志已清除
echo.

echo ========================================
echo 请在应用中播放视频，然后按任意键继续...
echo ========================================
pause >nul

echo.
echo [5] 分析播放日志...
echo.

echo --- 检查 HTTP 代理错误 ---
adb logcat -d | findstr /i "Invalid Content-Length Failed to open file"
echo.

echo --- 检查协议白名单错误 ---
adb logcat -d | findstr /i "Protocol.*not on whitelist"
echo.

echo --- 检查播放错误 ---
adb logcat -d | findstr /i "OrangevideoView.*onPlayError"
echo.

echo --- 检查 URL ---
adb logcat -d | findstr /i "OrangevideoView.*URL:"
echo.

echo.
echo ========================================
echo 诊断建议
echo ========================================
echo.

echo 如果看到 "Invalid Content-Length":
echo   - 问题: 本地 HTTP 代理服务器配置错误
echo   - 解决: 升级 AndroidVideoCache 或直接播放原始 URL
echo   - 详见: docs/fixes/local_http_proxy_content_length_fix.md
echo.

echo 如果看到 "Protocol 'file' not on whitelist":
echo   - 问题: IJK 播放器不支持 file:// 协议
echo   - 解决: 升级到 OrangePlayer v1.0.9+ 或切换播放器
echo   - 详见: docs/fixes/ijk_local_file_fix.md
echo.

echo 如果看到 "Failed to open file":
echo   - 可能原因: 文件不存在、权限问题或网络错误
echo   - 检查: 文件路径和网络连接
echo.

echo.
echo 完整日志已保存，按任意键查看...
pause >nul

echo.
echo [6] 保存完整日志...
adb logcat -d > playback_diagnosis_%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%.log
echo 日志已保存到当前目录
echo.

pause
