#!/bin/bash
# 检查编译好的 SO 文件

WORK_DIR="${HOME}/ijkplayer-build-official"
IJK_DIR="${WORK_DIR}/ijkplayer"

echo "检查编译好的 SO 文件..."
echo ""

check_arch() {
    local arch=$1
    local abi=$2
    local so_dir="${IJK_DIR}/android/ijkplayer/ijkplayer-${arch}/src/main/libs/${abi}"
    
    echo "[$arch - $abi]"
    if [ -d "$so_dir" ]; then
        ls -lh "$so_dir"/*.so 2>/dev/null || echo "  没有找到 SO 文件"
    else
        echo "  目录不存在: $so_dir"
    fi
    echo ""
}

check_arch "armv7a" "armeabi-v7a"
check_arch "arm64" "arm64-v8a"
check_arch "x86" "x86"
check_arch "x86_64" "x86_64"
