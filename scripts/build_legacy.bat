@echo off
chcp 65001 >nul
echo ========================================
echo 构建 OrangePlayer Legacy (Android 4.4)
echo ========================================
echo.

echo [1/4] 清理旧的构建文件...
call gradlew :app-legacy:clean
if %errorlevel% neq 0 (
    echo 错误: 清理失败
    pause
    exit /b 1
)
echo.

echo [2/4] 构建 Debug 版本...
call gradlew :app-legacy:assembleDebug
if %errorlevel% neq 0 (
    echo 错误: Debug 构建失败
    pause
    exit /b 1
)
echo.

echo [3/4] 构建 Release 版本...
call gradlew :app-legacy:assembleRelease
if %errorlevel% neq 0 (
    echo 错误: Release 构建失败
    pause
    exit /b 1
)
echo.

echo [4/4] 构建完成!
echo.
echo 输出文件:
echo   Debug:   app-legacy\build\outputs\apk\debug\app-legacy-debug.apk
echo   Release: app-legacy\build\outputs\apk\release\app-legacy-release.apk
echo.

set /p INSTALL="是否安装 Debug 版本到设备? (y/n): "
if /i "%INSTALL%"=="y" (
    echo.
    echo 正在安装...
    call gradlew :app-legacy:installDebug
    if %errorlevel% neq 0 (
        echo 错误: 安装失败
    ) else (
        echo 安装成功!
    )
)

echo.
pause
