#!/bin/bash
set -euo pipefail

# ========================================
# IJKPlayer 官方编译脚本（带 16K 支持）
# 基于：
# - https://github.com/CarGuo/GSYVideoPlayer/blob/master/doc/BUILD_SO.md
# - https://juejin.cn/post/7396306532671094793
# ========================================

cleanup_proxy() {
    unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY ALL_PROXY all_proxy no_proxy NO_PROXY
}

enable_wsl_proxy_if_available() {
    local host_ip
    
    # 从 /etc/resolv.conf 获取 Windows 主机 IP
    if [ -f /etc/resolv.conf ]; then
        host_ip=$(awk '/nameserver/ {print $2; exit}' /etc/resolv.conf 2>/dev/null || true)
    fi
    
    if [ -z "$host_ip" ]; then
        host_ip=$(ip route | grep default | awk '{print $3}' 2>/dev/null || true)
    fi

    if [ -z "$host_ip" ]; then
        echo "[proxy] 未找到 WSL 网关地址，使用直连"
        cleanup_proxy
        return
    fi

    echo "[proxy] 检测到 Windows 主机 IP: $host_ip"
    
    # 尝试常见的代理端口
    for port in 7887 7891 7892 10809 10808 7897; do
        echo "[proxy] 尝试端口 $port..."
        if timeout 2 bash -c "echo > /dev/tcp/${host_ip}/${port}" >/dev/null 2>&1; then
            local proxy_url="http://${host_ip}:${port}"
            export http_proxy="$proxy_url"
            export https_proxy="$proxy_url"
            export HTTP_PROXY="$proxy_url"
            export HTTPS_PROXY="$proxy_url"
            export no_proxy="127.0.0.1,localhost"
            export NO_PROXY="$no_proxy"
            echo "[proxy] ✓ 检测到代理可用: $proxy_url"
            
            # 测试 GitHub 连接
            if timeout 5 curl -s -o /dev/null -w "%{http_code}" --proxy "$proxy_url" https://github.com | grep -q "200\|301\|302"; then
                echo "[proxy] ✓ GitHub 连接测试成功"
                return 0
            else
                echo "[proxy] ✗ GitHub 连接测试失败，尝试下一个端口..."
                cleanup_proxy
            fi
        fi
    done
    
    echo "[proxy] ✗ 所有代理端口都不可用，使用直连"
    cleanup_proxy
}

trap cleanup_proxy EXIT
enable_wsl_proxy_if_available

# ========================================
# 初始化路径变量（必须在使用前定义）
# ========================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# 工作目录
if [ -d /home/xcwl ]; then
    BASE_HOME=/home/xcwl
else
    BASE_HOME=$HOME
fi

WORK_DIR="${BASE_HOME}/ijkplayer-build-official"
IJK_DIR="${WORK_DIR}/ijkplayer"

echo "========================================"
echo "  编译 IJKPlayer（16K 支持）"
echo "========================================"
echo ""
echo "选择 FFmpeg 源："
echo "  1) FFmpeg 官方最新版本 (master 分支) - 需要 NDK r21+"
echo "  2) FFmpeg 官方 n7.1 稳定版 - 需要 NDK r21+"
echo "  3) FFmpeg 官方 n6.1 LTS 版 - 需要 NDK r21+"
echo "  4) CarGuo/FFmpeg ijk-n4.3-20260301-007 - 使用 NDK r10e"
echo "  5) Bilibili/FFmpeg ff4.0--ijk0.8.8 (IJK 默认) - 使用 NDK r10e"
echo "  6) 706412584/FFmpeg ijk-n4.3-20260301-007 (原始版本) - 使用 NDK r10e"
echo "  7) 706412584/FFmpeg hls-discontinuity-fix-v2 (修复版本 - Part 1 + Part 2) - 使用 NDK r10e ⭐"
echo ""
read -p "请选择 [1-7，默认 6]: " ffmpeg_choice

case "${ffmpeg_choice:-6}" in
    1)
        FFMPEG_REPO="https://github.com/FFmpeg/FFmpeg.git"
        FFMPEG_BRANCH="master"
        FFMPEG_DESC="FFmpeg 官方最新版本 (master)"
        USE_NEW_NDK=true
        ;;
    2)
        FFMPEG_REPO="https://github.com/FFmpeg/FFmpeg.git"
        FFMPEG_BRANCH="n7.1"
        FFMPEG_DESC="FFmpeg 官方 n7.1 稳定版"
        USE_NEW_NDK=true
        ;;
    3)
        FFMPEG_REPO="https://github.com/FFmpeg/FFmpeg.git"
        FFMPEG_BRANCH="n6.1"
        FFMPEG_DESC="FFmpeg 官方 n6.1 LTS 版"
        USE_NEW_NDK=true
        ;;
    4)
        FFMPEG_REPO="https://github.com/CarGuo/FFmpeg.git"
        FFMPEG_BRANCH="ijk-n4.3-20260301-007"
        FFMPEG_DESC="CarGuo/FFmpeg ijk-n4.3-20260301-007"
        USE_NEW_NDK=false
        ;;
    5)
        FFMPEG_REPO="https://github.com/Bilibili/FFmpeg.git"
        FFMPEG_BRANCH="ff4.0--ijk0.8.8"
        FFMPEG_DESC="Bilibili/FFmpeg ff4.0--ijk0.8.8 (IJK 默认)"
        USE_NEW_NDK=false
        ;;
    6)
        FFMPEG_REPO="https://github.com/706412584/FFmpeg.git"
        FFMPEG_BRANCH="ijk-n4.3-20260301-007"
        FFMPEG_DESC="706412584/FFmpeg ijk-n4.3-20260301-007 (原始版本)"
        USE_NEW_NDK=false
        ;;
    7)
        FFMPEG_REPO="https://github.com/706412584/FFmpeg.git"
        FFMPEG_BRANCH="hls-discontinuity-fix-v2"
        FFMPEG_DESC="706412584/FFmpeg hls-discontinuity-fix-v2 (修复版本 - Part 1 + Part 2)"
        USE_NEW_NDK=false
        ;;
    *)
        FFMPEG_REPO="https://github.com/706412584/FFmpeg.git"
        FFMPEG_BRANCH="ijk-n4.3-20260301-007"
        FFMPEG_DESC="706412584/FFmpeg ijk-n4.3-20260301-007 (原始版本)"
        USE_NEW_NDK=false
        ;;
esac

if [ "$USE_NEW_NDK" = true ]; then
    NDK_VERSION="r21e"
    NDK_ZIP="android-ndk-r21e-linux-x86_64.zip"
    NDK_URL="https://dl.google.com/android/repository/${NDK_ZIP}"
    NDK_DIR="${WORK_DIR}/android-ndk-r21e"
else
    NDK_VERSION="r10e"
    NDK_ZIP="android-ndk-r10e-linux-x86_64.zip"
    NDK_URL="https://dl.google.com/android/repository/${NDK_ZIP}"
    NDK_DIR="${WORK_DIR}/android-ndk-r10e"
fi

echo ""
echo "编译配置："
echo "  - FFmpeg: $FFMPEG_DESC"
echo "  - NDK: $NDK_VERSION (使用 65536 对齐支持 16K Page Size)"
echo "  - 16K Page Size: 使用 65536 (64K) 对齐"
echo ""

mkdir -p "$WORK_DIR"
cd "$WORK_DIR"

# ========================================
# [1/9] 检查依赖
# ========================================
echo "[1/9] 检查依赖..."
missing_deps=()
for dep in git yasm nasm gcc make wget unzip curl; do
    if ! command -v "$dep" &> /dev/null; then
        missing_deps+=("$dep")
    fi
done

if [ ${#missing_deps[@]} -eq 0 ]; then
    echo "  ✓ 所有依赖已安装"
else
    echo "  缺少依赖: ${missing_deps[*]}"
    echo "  安装依赖..."
    sudo apt update
    sudo apt install -y git yasm nasm build-essential wget unzip curl
fi

# ========================================
# [2/9] 下载 NDK
# ========================================
echo "[2/9] 下载 NDK $NDK_VERSION..."

# 检查是否已有 NDK（在其他位置）
EXISTING_NDK=""
if [ "$USE_NEW_NDK" = true ]; then
    # 检查常见的 NDK r21e 位置
    for ndk_path in \
        "${BASE_HOME}/ffmpeg-build/android-ndk-r21e" \
        "${BASE_HOME}/android-ndk-r21e" \
        "${WORK_DIR}/android-ndk-r21e"; do
        if [ -d "$ndk_path" ] && [ -f "$ndk_path/ndk-build" ]; then
            EXISTING_NDK="$ndk_path"
            break
        fi
    done
else
    # 检查常见的 NDK r10e 位置
    for ndk_path in \
        "${BASE_HOME}/ijkplayer-build-official/android-ndk-r10e" \
        "${BASE_HOME}/ffmpeg-build/android-ndk-r10e" \
        "${BASE_HOME}/android-ndk-r10e" \
        "${WORK_DIR}/android-ndk-r10e"; do
        if [ -d "$ndk_path" ] && [ -f "$ndk_path/ndk-build" ]; then
            EXISTING_NDK="$ndk_path"
            break
        fi
    done
fi

if [ -n "$EXISTING_NDK" ]; then
    echo "  ✓ 检测到已有的 NDK $NDK_VERSION: $EXISTING_NDK"
    NDK_DIR="$EXISTING_NDK"
elif [ -d "$NDK_DIR" ] && [ -f "$NDK_DIR/ndk-build" ]; then
    echo "  ✓ NDK $NDK_VERSION 已存在: $NDK_DIR"
else
    echo "  下载 NDK $NDK_VERSION（约 400-800MB，这可能需要 5-10 分钟）..."
    
    if [ ! -f "$NDK_ZIP" ]; then
        wget -O "$NDK_ZIP" "$NDK_URL" || {
            echo "  ✗ 下载失败，请手动下载并解压到: $NDK_DIR"
            echo "  下载地址: $NDK_URL"
            exit 1
        }
    fi
    
    echo "  解压 NDK..."
    unzip -q "$NDK_ZIP"
    rm -f "$NDK_ZIP"
    
    echo "  ✓ NDK $NDK_VERSION 安装完成: $NDK_DIR"
fi

export ANDROID_NDK="$NDK_DIR"
echo "  设置 ANDROID_NDK=$ANDROID_NDK"

# ========================================
# [3/9] 克隆 IJKPlayer
# ========================================
echo "[3/9] 克隆 IJKPlayer..."
if [ -d "$IJK_DIR" ]; then
    echo "  检测到已存在的 IJKPlayer 目录"
    cd "$IJK_DIR"
    
    # 检查是否有未提交的修改
    if ! git diff --quiet 2>/dev/null || ! git diff --cached --quiet 2>/dev/null; then
        echo "  发现未提交的修改，重置到干净状态..."
        git reset --hard HEAD
        git clean -fdx
    fi
    
    echo "  获取远程更新..."
    git fetch origin
    git checkout master
    git reset --hard origin/master
else
    echo "  克隆 IJKPlayer 仓库..."
    git clone https://github.com/bilibili/ijkplayer.git
    cd "$IJK_DIR"
fi

# ========================================
# [4/9] 修改 init-android.sh 使用指定的 FFmpeg
# ========================================
echo "[4/9] 配置 FFmpeg 源（$FFMPEG_DESC）..."
if [ ! -f "init-android.sh.bak" ]; then
    cp init-android.sh init-android.sh.bak
fi

# 替换为指定的 FFmpeg 源
sed -i "s|IJK_FFMPEG_UPSTREAM=.*|IJK_FFMPEG_UPSTREAM=$FFMPEG_REPO|g" init-android.sh
sed -i "s|IJK_FFMPEG_FORK=.*|IJK_FFMPEG_FORK=$FFMPEG_REPO|g" init-android.sh
sed -i "s|IJK_FFMPEG_COMMIT=.*|IJK_FFMPEG_COMMIT=$FFMPEG_BRANCH|g" init-android.sh

echo "  ✓ FFmpeg 源已配置为 $FFMPEG_DESC"
echo ""
echo "  验证配置..."
grep "IJK_FFMPEG" init-android.sh | grep -v "^#" | head -3

# ========================================
# [5/9] 初始化 OpenSSL 和 FFmpeg
# ========================================
echo "[5/9] 初始化 OpenSSL 和 FFmpeg..."

# 清理旧的 FFmpeg 目录（避免分支/标签冲突）
if [ -d "extra/ffmpeg" ] || [ -d "android/contrib/ffmpeg-armv5" ]; then
    echo "  清理旧的 FFmpeg 目录..."
    rm -rf extra/ffmpeg
    rm -rf android/contrib/ffmpeg-*
fi

echo "  初始化 OpenSSL（这可能需要几分钟）..."
./init-android-openssl.sh

echo "  初始化 FFmpeg（这可能需要 10-20 分钟，取决于网络速度）..."
./init-android.sh

# ========================================
# [6/9] 应用 16K Page Size 补丁
# ========================================
echo "[6/9] 应用 16K Page Size 补丁..."
echo "  16K Page Size 说明："
echo "  - Android 15+ 部分设备使用 16KB 页大小"
echo "  - 需要 SO 文件使用 65536 (64K) 对齐"
echo "  - 使用 readelf -l xxx.so | grep LOAD 可以验证 Align = 0x10000"
echo ""

# 修改 Android.mk 文件添加 16K 对齐标志
echo "  修改 ijkj4a/Android.mk..."
if ! grep -q "LOCAL_LDFLAGS.*-Wl,-z,max-page-size=65536" ijkmedia/ijkj4a/Android.mk; then
    sed -i '/LOCAL_LDLIBS/a LOCAL_LDFLAGS += -Wl,-z,max-page-size=65536' ijkmedia/ijkj4a/Android.mk
    echo "  ✓ 已添加 16K 对齐标志到 ijkj4a"
fi

echo "  修改 ijkplayer/Android.mk..."
if ! grep -q "LOCAL_LDFLAGS.*-Wl,-z,max-page-size=65536" ijkmedia/ijkplayer/Android.mk; then
    sed -i '/LOCAL_LDLIBS/a LOCAL_LDFLAGS += -Wl,-z,max-page-size=65536' ijkmedia/ijkplayer/Android.mk
    echo "  ✓ 已添加 16K 对齐标志到 ijkplayer"
fi

echo "  修改 ijksdl/Android.mk..."
if ! grep -q "LOCAL_LDFLAGS.*-Wl,-z,max-page-size=65536" ijkmedia/ijksdl/Android.mk; then
    sed -i '/LOCAL_LDLIBS/a LOCAL_LDFLAGS += -Wl,-z,max-page-size=65536' ijkmedia/ijksdl/Android.mk
    echo "  ✓ 已添加 16K 对齐标志到 ijksdl"
fi

echo "  修改 FFmpeg 编译脚本..."
if ! grep -q "max-page-size=65536" android/contrib/tools/do-compile-ffmpeg.sh; then
    sed -i 's/--extra-ldflags="\$COMMON_FF_CFG_LDFLAGS"/--extra-ldflags="\$COMMON_FF_CFG_LDFLAGS -Wl,-z,max-page-size=65536"/g' android/contrib/tools/do-compile-ffmpeg.sh
    echo "  ✓ 已添加 16K 对齐标志到 FFmpeg"
fi

echo "  修改 OpenSSL 编译脚本..."
if ! grep -q "max-page-size=65536" android/contrib/tools/do-compile-openssl.sh; then
    sed -i 's/-fPIC"/-fPIC -Wl,-z,max-page-size=65536"/g' android/contrib/tools/do-compile-openssl.sh
    echo "  ✓ 已添加 16K 对齐标志到 OpenSSL"
fi

# ========================================
# [7/9] 配置编译选项
# ========================================
echo "[7/9] 配置编译选项..."
cd config

if [ -f module.sh ]; then
    rm -f module.sh
fi

# 复制项目的配置文件
echo "  使用项目配置: module-lite-more-fixed.sh"
cp "$PROJECT_ROOT/GSYVideoPlayer-source/module-lite-more-fixed.sh" ./module-lite-more-fixed.sh
ln -s module-lite-more-fixed.sh module.sh

cd ..

# ========================================
# [8/9] 选择编译架构
# ========================================
echo "[8/9] 选择编译架构..."
echo ""
echo "可用架构："
echo "  1) armv7a  - 32位 ARM (armeabi-v7a)"
echo "  2) arm64   - 64位 ARM (arm64-v8a) [推荐]"
echo "  3) x86     - 32位 x86"
echo "  4) x86_64  - 64位 x86_64"
echo "  5) all     - 编译所有架构（耗时最长）"
echo ""
echo "提示："
echo "  - 生产环境推荐只编译 arm64（覆盖 95%+ 设备）"
echo "  - armv7a 用于兼容老设备"
echo "  - x86/x86_64 用于模拟器测试"
echo ""
read -p "请选择 [1-5，默认 2]: " arch_choice

case "${arch_choice:-2}" in
    1) ARCHS="armv7a" ;;
    2) ARCHS="arm64" ;;
    3) ARCHS="x86" ;;
    4) ARCHS="x86_64" ;;
    5) ARCHS="armv7a arm64 x86 x86_64" ;;
    *) ARCHS="arm64" ;;
esac

echo "  将编译架构: $ARCHS"
echo ""

# ========================================
# [9/9] 开始编译
# ========================================
echo "[9/9] 开始编译..."
echo "  编译开始时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

cd android/contrib

for arch in $ARCHS; do
    echo "========================================"
    echo "  编译架构: $arch"
    echo "========================================"
    
    echo "  [1/3] 编译 OpenSSL ($arch)..."
    ./compile-openssl.sh clean
    ./compile-openssl.sh $arch
    
    echo "  [2/3] 编译 FFmpeg ($arch)..."
    ./compile-ffmpeg.sh clean
    ./compile-ffmpeg.sh $arch
    
    echo "  [3/3] 编译 IJK ($arch)..."
    cd ..
    ./compile-ijk.sh $arch
    cd contrib
    
    echo "  ✓ $arch 编译完成"
    echo ""
done

cd ../..

echo ""
echo "========================================"
echo "  编译完成！"
echo "========================================"
echo "  编译结束时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""
echo "SO 文件位置："
for arch in $ARCHS; do
    case $arch in
        armv7a)
            echo "  armv7a: $IJK_DIR/android/ijkplayer/ijkplayer-armv7a/src/main/obj/local/armeabi-v7a/"
            ;;
        arm64)
            echo "  arm64:  $IJK_DIR/android/ijkplayer/ijkplayer-arm64/src/main/obj/local/arm64-v8a/"
            ;;
        x86)
            echo "  x86:    $IJK_DIR/android/ijkplayer/ijkplayer-x86/src/main/obj/local/x86/"
            ;;
        x86_64)
            echo "  x86_64: $IJK_DIR/android/ijkplayer/ijkplayer-x86_64/src/main/obj/local/x86_64/"
            ;;
    esac
done
echo ""
echo "验证 16K 对齐："
echo "  运行以下命令验证 SO 文件是否正确对齐："
echo "  readelf -l <so文件路径> | grep LOAD"
echo "  应该看到 Align = 0x10000 (65536)"
echo ""
echo "下一步："
echo "  1. 验证 SO 文件对齐（可选）"
echo "  2. 运行 scripts/copy_ijk_so_simple.sh 复制 SO 文件到项目"
echo "  3. 在 Windows 中重新编译项目"
echo "  4. 测试视频播放"
echo ""
