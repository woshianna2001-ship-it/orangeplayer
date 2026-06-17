@echo off
chcp 65001 >nul
echo ========================================
echo Android 最低版本检查工具
echo ========================================
echo.

echo [当前配置]
echo.
echo 播放器库 (palyerlibrary):
findstr /C:"minSdk" palyerlibrary\build.gradle
echo.
echo 应用 (app):
findstr /C:"minSdk" app\build.gradle
echo.

echo ========================================
echo [依赖库最低版本要求]
echo ========================================
echo.

echo AndroidX AppCompat: API 14+ (Android 4.0+) ✅
echo Material Design: API 14+ (Android 4.0+) ✅
echo.

echo --- 播放器内核 ---
echo GSYVideoPlayer: API 16+ (Android 4.1+) ❌
echo IJK 播放器: API 16+ (Android 4.1+) ❌
echo ExoPlayer: API 21+ (Android 5.0+) ❌
echo 系统播放器: API 1+ (所有版本) ✅
echo.

echo --- 可选功能 ---
echo 弹幕 (DanmakuFlameMaster): API 14+ (Android 4.0+) ✅
echo OCR (Tesseract): API 21+ (Android 5.0+) ❌
echo 语音识别 (Vosk): API 21+ (Android 5.0+) ❌
echo ML Kit 翻译: API 19+ (Android 4.4+) ❌
echo Glide: API 14+ (Android 4.0+) ✅
echo.

echo --- 特殊功能 ---
echo SurfaceControl (画中画): API 29+ (Android 10+) ❌
echo 语音捕获服务: API 29+ (Android 10+) ❌
echo.

echo ========================================
echo [Android 版本市场占有率 (2024)]
echo ========================================
echo.
echo Android 4.0 (API 14-15): ^< 0.1%%
echo Android 4.1-4.3 (API 16-18): ^< 0.5%%
echo Android 4.4 (API 19): ~0.5%%
echo Android 5.0-5.1 (API 21-22): ~2%%
echo Android 6.0+ (API 23+): ~97%%
echo.

echo ========================================
echo [结论]
echo ========================================
echo.
echo 要支持 Android 4.0 (API 14):
echo   ❌ 无法使用任何现代播放器内核
echo   ❌ 无法使用 AI 功能 (OCR/语音识别)
echo   ❌ 功能严重受限
echo   ✅ 仅可使用系统播放器 + 弹幕
echo.
echo 推荐最低版本:
echo   • minSdk 21 (Android 5.0) - 推荐
echo   • 覆盖 99%%+ 活跃设备
echo   • 支持所有功能
echo.
echo 详细分析请查看: docs\ANDROID_4_COMPATIBILITY.md
echo.

pause
