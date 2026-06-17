@echo off
chcp 65001 >nul

echo ========================================
echo 本地 Maven 发布脚本（测试用）
echo ========================================
echo.

REM 返回项目根目录
cd ..

echo [1/2] 清理旧构建...
call gradlew clean

echo.
echo [2/2] 发布到本地 Maven 仓库...
call gradlew :palyerlibrary:publishToMavenLocal

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ 发布失败！
    pause
    exit /b 1
)

echo.
echo ========================================
echo ✅ 本地发布成功！
echo ========================================
echo.
echo 📦 发布信息:
echo    Group ID: io.github.706412584
echo    Artifact ID: orangeplayer
echo    Version: 1.0.8
echo.
echo 📂 本地路径:
echo    %USERPROFILE%\.m2\repository\io\github\706412584\orangeplayer\1.0.8\
echo.
echo 💡 在其他项目中使用:
echo    dependencies {
echo        implementation 'io.github.706412584:orangeplayer:1.0.8'
echo    }
echo.

pause
