#!/bin/bash

# 验证 IJK 编译使用的 FFmpeg 源码版本
# 用于确认是否使用了 CarGuo 的 FFmpeg 修复版本

set -e

echo "========================================"
echo "验证 IJK FFmpeg 源码版本"
echo "========================================"

BUILD_DIR=~/ijkplayer-build-official/ijkplayer

if [ ! -d "$BUILD_DIR" ]; then
    echo "❌ 编译目录不存在: $BUILD_DIR"
    echo "请先运行 build_ijk_official.sh 编译 IJK"
    exit 1
fi

cd "$BUILD_DIR"

echo ""
echo "1. 检查 init-android.sh 配置..."
echo "----------------------------------------"
if [ -f "init-android.sh" ]; then
    echo "FFmpeg 配置:"
    grep "IJK_FFMPEG" init-android.sh | grep -v "^#" | head -5
else
    echo "❌ init-android.sh 不存在"
fi

echo ""
echo "2. 检查 FFmpeg 源码目录..."
echo "----------------------------------------"
if [ -d "extra/ffmpeg" ]; then
    cd extra/ffmpeg
    
    echo "Git 远程仓库:"
    git remote -v
    
    echo ""
    echo "当前分支/提交:"
    git log --oneline -1
    
    echo ""
    echo "最近 10 个提交:"
    git log --oneline -10
    
    echo ""
    echo "检查是否有 HLS 相关修复:"
    git log --all --grep="hls\|HLS\|discontinuity\|DISCONTINUITY" --oneline | head -10
    
else
    echo "❌ FFmpeg 源码目录不存在: extra/ffmpeg"
    echo "可能已被清理，无法验证源码版本"
fi

echo ""
echo "========================================"
echo "验证完成"
echo "========================================"
