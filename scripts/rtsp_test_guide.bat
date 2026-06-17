@echo off
chcp 65001 >nul
color 0A
echo.
echo ╔════════════════════════════════════════════════════════════╗
echo ║          RTSP 直播推流测试指南                              ║
echo ╚════════════════════════════════════════════════════════════╝
echo.
echo 【步骤 1】启动 RTSP 服务器
echo    双击运行: start_rtsp_server.bat
echo    等待看到 "rtsp server is ready" 提示
echo.
echo 【步骤 2】开始推流
echo    双击运行: push_rtsp_stream.bat
echo    等待看到推流成功的日志
echo.
echo 【步骤 3】在 Android 设备上播放
echo    RTSP 地址: rtsp://你的电脑IP:8554/live
echo.
echo ════════════════════════════════════════════════════════════
echo.
echo 【如何获取电脑 IP 地址】
echo.
ipconfig | findstr /i "IPv4"
echo.
echo ════════════════════════════════════════════════════════════
echo.
echo 【Android 播放代码】
echo.
echo String rtspUrl = "rtsp://你的IP:8554/live";
echo mVideoView.setUp(rtspUrl, true, "RTSP 直播测试");
echo mVideoView.startPlayLogic();
echo.
echo ════════════════════════════════════════════════════════════
echo.
echo 按任意键退出...
pause >nul
