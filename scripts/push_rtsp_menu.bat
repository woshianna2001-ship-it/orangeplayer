@echo off
chcp 65001 >nul
cls
echo ========================================
echo RTSP Streaming - Bitrate Selection
echo ========================================
echo.
echo Please select streaming quality:
echo.
echo 1. Low (1 Mbps / 125 KB/s)     - WiFi, lowest latency
echo 2. Medium (4 Mbps / 500 KB/s)  - 5GHz WiFi
echo 3. High (8 Mbps / 1 MB/s)      - Gigabit WiFi
echo 4. Ultra (20 Mbps / 2.5 MB/s)  - Gigabit Ethernet
echo 5. Original (no transcode)     - Highest quality
echo.
set /p CHOICE=Enter option (1-5): 

if "%CHOICE%"=="1" goto LOW
if "%CHOICE%"=="2" goto MEDIUM
if "%CHOICE%"=="3" goto HIGH
if "%CHOICE%"=="4" goto ULTRA
if "%CHOICE%"=="5" goto ORIGINAL
echo Invalid option
pause
exit /b 1

:LOW
echo.
echo Selected: Low bitrate (1 Mbps)
call push_rtsp_stream.bat
exit /b 0

:MEDIUM
echo.
echo Selected: Medium bitrate (4 Mbps)
set BITRATE=4M
set RESOLUTION=1280:720
goto PUSH

:HIGH
echo.
echo Selected: High bitrate (8 Mbps)
set BITRATE=8M
set RESOLUTION=1920:1080
goto PUSH

:ULTRA
echo.
echo Selected: Ultra bitrate (20 Mbps)
set BITRATE=20M
set RESOLUTION=1920:1080
goto PUSH

:ORIGINAL
echo.
echo Selected: Original bitrate (no transcode)
goto PUSH_ORIGINAL

:PUSH
REM FFmpeg path
set FFMPEG_PATH=D:\ffmpeg-5.1.2-essentials_build\bin\ffmpeg.exe
set RTSP_URL=rtsp://localhost:8554/live

echo.
echo Drag video file here and press Enter:
set /p VIDEO_FILE=
set VIDEO_FILE=%VIDEO_FILE:"=%

if not exist "%VIDEO_FILE%" (
    echo [ERROR] Video file not found
    pause
    exit /b 1
)

echo.
echo Configuration:
echo - Resolution: %RESOLUTION%
echo - Bitrate: %BITRATE%
echo - Network speed: ~%BITRATE% / 8
echo.
echo Starting stream...
echo ========================================
echo.

"%FFMPEG_PATH%" -re -stream_loop -1 -i "%VIDEO_FILE%" ^
    -c:v libx264 -preset ultrafast -tune zerolatency ^
    -vf "scale=%RESOLUTION%" ^
    -b:v %BITRATE% -maxrate %BITRATE% -bufsize %BITRATE%*2 ^
    -c:a aac -b:a 192k ^
    -f rtsp "%RTSP_URL%"

pause
exit /b 0

:PUSH_ORIGINAL
REM FFmpeg path
set FFMPEG_PATH=D:\ffmpeg-5.1.2-essentials_build\bin\ffmpeg.exe
set RTSP_URL=rtsp://localhost:8554/live

echo.
echo Drag video file here and press Enter:
set /p VIDEO_FILE=
set VIDEO_FILE=%VIDEO_FILE:"=%

if not exist "%VIDEO_FILE%" (
    echo [ERROR] Video file not found
    pause
    exit /b 1
)

echo.
echo WARNING: Original bitrate may be very high (10-50 Mbps)
echo Make sure your network can handle it
echo.
echo Starting stream...
echo ========================================
echo.

REM No transcode, copy streams directly
"%FFMPEG_PATH%" -re -stream_loop -1 -i "%VIDEO_FILE%" ^
    -c:v copy -c:a copy ^
    -f rtsp "%RTSP_URL%"

pause
exit /b 0
