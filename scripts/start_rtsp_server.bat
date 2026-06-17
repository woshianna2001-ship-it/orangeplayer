@echo off
echo ========================================
echo 启动 RTSP 服务器
echo ========================================
echo.
echo RTSP 服务器地址: rtsp://localhost:8554/live
echo 或使用局域网地址: rtsp://你的IP:8554/live
echo.
echo 按 Ctrl+C 停止服务器
echo ========================================
echo.

cd /d D:\rtsp_server
mediamtx.exe

pause
