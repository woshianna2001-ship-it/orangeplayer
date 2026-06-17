#!/bin/bash
# 复制编译好的 IJK SO 文件到项目（简化版）

echo "========================================"
echo "  复制 IJK SO 文件到项目"
echo "========================================"
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# 源目录
if [ -d /home/xcwl ]; then
    BASE_HOME=/home/xcwl
else
    BASE_HOME=$HOME
fi

WORK_DIR="${BASE_HOME}/ijkplayer-build-official"
IJK_DIR="${WORK_DIR}/ijkplayer"

# 目标目录
TARGET_DIR="${PROJECT_ROOT}/GSYVideoPlayer-source/gsyVideoPlayer-ex_so/src/main/jniLibs"

echo "源目录: $IJK_DIR"
echo "目标目录: $TARGET_DIR"
echo ""

# 创建目标目录
mkdir -p "$TARGET_DIR"

# 复制 armv7a
echo "[1/4] 复制 armeabi-v7a..."
mkdir -p "$TARGET_DIR/armeabi-v7a"
if [ -f "$IJK_DIR/android/ijkplayer/ijkplayer-armv7a/src/main/obj/local/armeabi-v7a/libijkffmpeg.so" ]; then
    cp -v "$IJK_DIR/android/ijkplayer/ijkplayer-armv7a/src/main/obj/local/armeabi-v7a/"*.so "$TARGET_DIR/armeabi-v7a/"
    echo "✓ 复制成功"
else
    echo "✗ SO 文件不存在"
fi
echo ""

# 复制 arm64
echo "[2/4] 复制 arm64-v8a..."
mkdir -p "$TARGET_DIR/arm64-v8a"
if [ -f "$IJK_DIR/android/ijkplayer/ijkplayer-arm64/src/main/obj/local/arm64-v8a/libijkffmpeg.so" ]; then
    cp -v "$IJK_DIR/android/ijkplayer/ijkplayer-arm64/src/main/obj/local/arm64-v8a/"*.so "$TARGET_DIR/arm64-v8a/"
    echo "✓ 复制成功"
else
    echo "✗ SO 文件不存在"
fi
echo ""

# 复制 x86
echo "[3/4] 复制 x86..."
mkdir -p "$TARGET_DIR/x86"
if [ -f "$IJK_DIR/android/ijkplayer/ijkplayer-x86/src/main/obj/local/x86/libijkffmpeg.so" ]; then
    cp -v "$IJK_DIR/android/ijkplayer/ijkplayer-x86/src/main/obj/local/x86/"*.so "$TARGET_DIR/x86/"
    echo "✓ 复制成功"
else
    echo "✗ SO 文件不存在"
fi
echo ""

# 复制 x86_64
echo "[4/4] 复制 x86_64..."
mkdir -p "$TARGET_DIR/x86_64"
if [ -f "$IJK_DIR/android/ijkplayer/ijkplayer-x86_64/src/main/obj/local/x86_64/libijkffmpeg.so" ]; then
    cp -v "$IJK_DIR/android/ijkplayer/ijkplayer-x86_64/src/main/obj/local/x86_64/"*.so "$TARGET_DIR/x86_64/"
    echo "✓ 复制成功"
else
    echo "✗ SO 文件不存在"
fi
echo ""

echo "========================================"
echo "复制完成！"
echo "========================================"
echo ""

# 列出复制的文件
echo "已复制的文件："
ls -lh "$TARGET_DIR"/*/*.so 2>/dev/null | awk '{print $9, $5}'
echo ""

echo "下一步："
echo "  在 Windows 中运行: ./gradlew :app:clean :app:assembleDebug"
echo ""
