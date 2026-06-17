@echo off
chcp 65001 >nul
echo ========================================
echo 恢复原始配置
echo ========================================
echo.

if not exist "backup\palyerlibrary_build.gradle.bak" (
    echo 错误: 未找到备份文件
    echo 请确保之前运行过 configure_android_44.bat
    pause
    exit /b 1
)

echo 正在恢复配置...
echo.

copy /y "backup\palyerlibrary_build.gradle.bak" "palyerlibrary\build.gradle" >nul
copy /y "backup\app_build.gradle.bak" "app\build.gradle" >nul

echo 配置已恢复到原始状态
echo.
echo 请同步 Gradle 项目以应用更改
echo.

pause
