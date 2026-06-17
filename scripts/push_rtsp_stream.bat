@echo off
chcp 65001 >nul
echo ========================================
echo RTSP 推流脚本
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
echo 开始推流...
echo 按 Ctrl+C 停止推流
echo ========================================
echo.

REM 推流命令（循环播放，降低分辨率以减少带宽）
REM 优化版：使用 veryfast preset 提高编码效率
"%FFMPEG_PATH%" -re -stream_loop -1 -i "%VIDEO_FILE%" ^
    -c:v libx264 -preset ultrafast -tune zerolatency ^
    -vf "scale=854:480" ^
    -b:v 1M -maxrate 1M -bufsize 2M ^
    -c:a aac -b:a 128k ^
    -f rtsp "%RTSP_URL%"

pause
