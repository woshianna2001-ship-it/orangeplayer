@echo off
REM 批量发布所有模块到本地 Maven 仓库

echo ========================================
echo 发布 OrangePlayer 所有模块
echo ========================================
echo.

echo [1/10] 发布 palyerlibrary...
call gradlew :palyerlibrary:publishToMavenLocal
if %ERRORLEVEL% NEQ 0 (
    echo 错误: palyerlibrary 发布失败
    exit /b 1
)
echo.

echo [2/10] 发布 gsyVideoPlayer-base...
call gradlew :gsyVideoPlayer-base:publishToMavenLocal
if %ERRORLEVEL% NEQ 0 (
    echo 错误: gsyVideoPlayer-base 发布失败
    exit /b 1
)
echo.

echo [3/10] 发布 gsyVideoPlayer-proxy_cache...
call gradlew :gsyVideoPlayer-proxy_cache:publishToMavenLocal
if %ERRORLEVEL% NEQ 0 (
    echo 错误: gsyVideoPlayer-proxy_cache 发布失败
    exit /b 1
)
echo.

echo [4/10] 发布 gsyVideoPlayer-java...
call gradlew :gsyVideoPlayer-java:publishToMavenLocal
if %ERRORLEVEL% NEQ 0 (
    echo 错误: gsyVideoPlayer-java 发布失败
    exit /b 1
)
echo.

echo [5/10] 发布 gsyVideoPlayer-exo_player2...
call gradlew :gsyVideoPlayer-exo_player2:publishToMavenLocal
if %ERRORLEVEL% NEQ 0 (
    echo 错误: gsyVideoPlayer-exo_player2 发布失败
    exit /b 1
)
echo.

echo [6/10] 发布 gsyVideoPlayer-aliplay...
call gradlew :gsyVideoPlayer-aliplay:publishToMavenLocal
if %ERRORLEVEL% NEQ 0 (
    echo 错误: gsyVideoPlayer-aliplay 发布失败
    exit /b 1
)
echo.

echo [7/10] 发布 gsyVideoPlayer-armv7a...
call gradlew :gsyVideoPlayer-armv7a:publishToMavenLocal
if %ERRORLEVEL% NEQ 0 (
    echo 错误: gsyVideoPlayer-armv7a 发布失败
    exit /b 1
)
echo.

echo [8/10] 发布 gsyVideoPlayer-armv64...
call gradlew :gsyVideoPlayer-armv64:publishToMavenLocal
if %ERRORLEVEL% NEQ 0 (
    echo 错误: gsyVideoPlayer-armv64 发布失败
    exit /b 1
)
echo.

echo [9/10] 发布 gsyVideoPlayer-x86...
call gradlew :gsyVideoPlayer-x86:publishToMavenLocal
if %ERRORLEVEL% NEQ 0 (
    echo 错误: gsyVideoPlayer-x86 发布失败
    exit /b 1
)
echo.

echo [10/10] 发布 gsyVideoPlayer-x86_64...
call gradlew :gsyVideoPlayer-x86_64:publishToMavenLocal
if %ERRORLEVEL% NEQ 0 (
    echo 错误: gsyVideoPlayer-x86_64 发布失败
    exit /b 1
)
echo.

echo [11/11] 发布 gsyVideoPlayer-ex_so...
call gradlew :gsyVideoPlayer-ex_so:publishToMavenLocal
if %ERRORLEVEL% NEQ 0 (
    echo 错误: gsyVideoPlayer-ex_so 发布失败
    exit /b 1
)
echo.

echo ========================================
echo 所有模块发布成功！
echo ========================================
echo.
echo 发布位置: %USERPROFILE%\.m2\repository\io\github\706412584\
echo.
echo 模块列表:
echo   - orangeplayer (核心库)
echo   - gsyVideoPlayer-base
echo   - gsyVideoPlayer-proxy_cache
echo   - gsyVideoPlayer-java
echo   - gsyVideoPlayer-exo_player2 (ExoPlayer 内核)
echo   - gsyVideoPlayer-aliplay (阿里云播放器内核)
echo   - gsyVideoPlayer-armv7a (ARM 32位 so)
echo   - gsyVideoPlayer-armv64 (ARM 64位 so)
echo   - gsyVideoPlayer-x86 (x86 32位 so)
echo   - gsyVideoPlayer-x86_64 (x86 64位 so)
echo   - gsyVideoPlayer-ex_so (IJK 加密支持 so，全架构)
echo.

pause
