@echo off
chcp 65001 >nul
setlocal

if "%1"=="" (
    echo Usage: release.bat vX.X.X
    echo Example: release.bat v1.0.2
    exit /b 1
)

set VERSION=%1

echo ========================================
echo   OrangePlayer Release Script
echo   Version: %VERSION%
echo ========================================

echo.
echo Pushing to main...
git push origin main

echo.
echo Checking existing tag...
git tag -d %VERSION% 2>nul
git push origin :refs/tags/%VERSION% 2>nul

echo.
echo Creating tag: %VERSION%
git tag %VERSION%

echo.
echo Pushing tag...
git push origin %VERSION%

echo.
echo ========================================
echo   Release Complete!
echo ========================================
echo.
echo GitHub Actions: https://github.com/706412584/orangeplayer/actions
echo JitPack: https://jitpack.io/#706412584/orangeplayer
echo.
echo Dependency:
echo   implementation 'com.github.706412584:orangeplayer:%VERSION%'

endlocal
