@echo off
chcp 65001 >nul
echo ========================================
echo Android 4.4 兼容配置工具
echo ========================================
echo.

echo 此脚本将帮助你配置项目以支持 Android 4.4 (API 19)
echo.
echo 警告: 这将移除以下功能:
echo   - ExoPlayer 播放器
echo   - 阿里云播放器
echo   - OCR 字幕识别
echo   - 语音识别
echo   - ML Kit 翻译
echo   - FFmpeg 解码器
echo.

set /p CONFIRM="确定要继续吗? (y/n): "
if /i not "%CONFIRM%"=="y" (
    echo 操作已取消
    pause
    exit /b 0
)

echo.
echo ========================================
echo 开始配置...
echo ========================================
echo.

echo [1/5] 备份当前配置...
if not exist "backup" mkdir backup
copy /y "palyerlibrary\build.gradle" "backup\palyerlibrary_build.gradle.bak" >nul
copy /y "app\build.gradle" "backup\app_build.gradle.bak" >nul
echo 备份完成: backup\*.bak
echo.

echo [2/5] 修改 palyerlibrary minSdk...
powershell -Command "(Get-Content 'palyerlibrary\build.gradle') -replace 'minSdk 21', 'minSdk 19' | Set-Content 'palyerlibrary\build.gradle'"
echo palyerlibrary minSdk 已设置为 19
echo.

echo [3/5] 修改 app minSdk...
powershell -Command "(Get-Content 'app\build.gradle') -replace 'minSdk 23', 'minSdk 19' | Set-Content 'app\build.gradle'"
echo app minSdk 已设置为 19
echo.

echo [4/5] 显示需要手动移除的依赖...
echo.
echo 请手动编辑 app\build.gradle，注释或移除以下依赖:
echo.
echo   // ExoPlayer (需要 API 21+)
echo   // implementation 'io.github.carguo:gsyvideoplayer-exo2:11.3.0'
echo.
echo   // 阿里云播放器 (需要 API 21+)
echo   // implementation('io.github.carguo:gsyvideoplayer-aliplay:11.3.0')
echo   // implementation 'com.aliyun.sdk.android:AliyunPlayer:5.4.7.1-full'
echo.
echo   // OCR (需要 API 21+)
echo   // implementation 'cz.adaptech.tesseract4android:tesseract4android:4.7.0'
echo.
echo   // 语音识别 (需要 API 21+)
echo   // implementation 'com.alphacephei:vosk-android:0.3.47'
echo.
echo   // ML Kit (需要 API 19+，但功能有限)
echo   // implementation 'com.google.mlkit:translate:17.0.2'
echo.
echo   // FFmpeg 解码器 (需要 API 21+)
echo   // implementation 'org.jellyfin.media3:media3-ffmpeg-decoder:1.8.0+1'
echo.

echo [5/5] 配置完成!
echo.
echo ========================================
echo 后续步骤
echo ========================================
echo.
echo 1. 手动编辑 app\build.gradle 移除不兼容的依赖
echo 2. 在代码中添加版本检查 (参考 docs\ANDROID_4.4_SUPPORT.md)
echo 3. 同步 Gradle 项目
echo 4. 在 Android 4.4 设备/模拟器上测试
echo.
echo 详细指南: docs\ANDROID_4.4_SUPPORT.md
echo.

echo 如需恢复原配置，运行: scripts\restore_config.bat
echo.

pause
