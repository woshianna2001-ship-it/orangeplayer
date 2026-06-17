@echo off
chcp 65001 >nul
echo ========================================
echo RTSP 推流脚本（高码率版 - 局域网专用）
echo ========================================
echo.

REM 设置 FFmpeg 路径
set FFMPEG_PATH=D:\ffmpeg-5.1.2-essentials_build\bin\ffmpeg.exe

REM 设置 RTSP 服务器地址
set RTSP_URL=rtsp://localhost:8554/live

REM 如果没有传入参数，提示用户输入视频文件路径
if "%~1"=="" (
    echo 请将视频文件拖拽到此窗口，然后按回车：
    set /p VIDEO_FILE=
) else (
    set VIDEO_FILE=%~1
)

REM 去除引号
set VIDEO_FILE=%VIDEO_FILE:"=%

echo.
echo 视频文件: %VIDEO_FILE%
echo RTSP 地址: %RTSP_URL%
echo.

REM 检查视频文件是否存在
if not exist "%VIDEO_FILE%" (
    echo [错误] 视频文件不存在: %VIDEO_FILE%
    echo.
    echo 提示：你可以直接将视频文件拖拽到此批处理文件上
    echo.
    pause
    exit /b 1
)

REM 检查 FFmpeg 是否存在
if not exist "%FFMPEG_PATH%" (
    echo [错误] FFmpeg 不存在: %FFMPEG_PATH%
    echo.
    pause
    exit /b 1
)

echo [提示] 请确保 RTSP 服务器已启动（运行 start_rtsp_server.bat）
echo.
echo 配置说明：
echo - 分辨率: 1920x1080 (1080p)
echo - 码率: 8 Mbps (约 1 MB/s)
echo - 适用场景: 局域网高质量直播
echo - 网络要求: 千兆网络或 5GHz WiFi
echo.
echo 开始推流...
echo 按 Ctrl+C 停止推流
echo ========================================
echo.

REM 推流命令（高码率版本）
REM 参数说明：
REM - scale=1920:1080 : 1080p 分辨率
REM - b:v 8M : 视频码率 8Mbps
REM - maxrate 8M : 最大码率 8Mbps
REM - bufsize 16M : 缓冲区 16MB
REM - preset ultrafast : 快速编码
REM - tune zerolatency : 低延迟优化
"%FFMPEG_PATH%" -re -stream_loop -1 -i "%VIDEO_FILE%" ^
    -c:v libx264 -preset ultrafast -tune zerolatency ^
    -vf "scale=1920:1080" ^
    -b:v 8M -maxrate 8M -bufsize 16M ^
    -c:a aac -b:a 192k ^
    -f rtsp "%RTSP_URL%"

pause
