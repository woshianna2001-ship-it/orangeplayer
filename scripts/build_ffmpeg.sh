#!/bin/bash
set -euo pipefail

cleanup_proxy() {
    unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY ALL_PROXY all_proxy no_proxy NO_PROXY
}

enable_wsl_proxy_if_available() {
    local proxy_port="${CLASH_PROXY_PORT:-7887}"
    local host_ip="127.0.0.1"

    if [ -f /etc/resolv.conf ]; then
        host_ip=$(awk '/nameserver/ {print $2; exit}' /etc/resolv.conf 2>/dev/null || true)
    fi

    if [ -z "$host_ip" ]; then
        echo "[proxy] 未找到 WSL 网关地址，使用直连"
        cleanup_proxy
        return
    fi

    if timeout 2 bash -c "</dev/tcp/${host_ip}/${proxy_port}" >/dev/null 2>&1; then
        local proxy_url="http://${host_ip}:${proxy_port}"
        export http_proxy="$proxy_url"
        export https_proxy="$proxy_url"
        export HTTP_PROXY="$proxy_url"
        export HTTPS_PROXY="$proxy_url"
        export no_proxy="127.0.0.1,localhost"
        export NO_PROXY="$no_proxy"
        echo "[proxy] 检测到 Clash 代理可用，临时启用: $proxy_url"
    else
        echo "[proxy] Clash 代理不可用，使用直连"
        cleanup_proxy
    fi
}

trap cleanup_proxy EXIT
enable_wsl_proxy_if_available

echo "========================================"
echo "  FFmpeg 精简版多架构编译脚本"
echo "========================================"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if [ -d /home/xcwl ]; then
    BASE_HOME=/home/xcwl
else
    BASE_HOME=$HOME
fi

WORK_DIR="${BASE_HOME}/ffmpeg-build"
NDK_ROOT="${WORK_DIR}/android-ndk-r21e"
OPENSSL_SRC_DIR="${WORK_DIR}/openssl-1.1.1w"
FFMPEG_SRC_DIR="${WORK_DIR}/ffmpeg-src"
THIRD_PARTY_ROOT="${PROJECT_ROOT}/orange-ffmpeg/src/main/cpp/third_party/ffmpeg"

DEFAULT_ABIS="armeabi-v7a,arm64-v8a,x86,x86_64"
REQUESTED_ABIS="${ORANGE_FFMPEG_ABIS:-$DEFAULT_ABIS}"
IFS=',' read -r -a ABI_LIST <<< "$REQUESTED_ABIS"

mkdir -p "$WORK_DIR"
cd "$WORK_DIR"

echo "[1/6] 安装编译工具..."
sudo apt update
sudo apt install -y build-essential git yasm nasm pkg-config wget unzip

echo "[2/6] 检查 NDK..."
if [ ! -d "$NDK_ROOT" ]; then
    wget -O android-ndk-r21e-linux-x86_64.zip https://dl.google.com/android/repository/android-ndk-r21e-linux-x86_64.zip
    unzip -q android-ndk-r21e-linux-x86_64.zip
    rm -f android-ndk-r21e-linux-x86_64.zip
fi

export ANDROID_NDK_HOME="$NDK_ROOT"
export ANDROID_NDK_ROOT="$NDK_ROOT"
export PATH="$NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin:$PATH"

echo "[3/6] 检查 OpenSSL 源码..."
if [ ! -d "$OPENSSL_SRC_DIR" ]; then
    wget -O openssl-1.1.1w.tar.gz https://www.openssl.org/source/openssl-1.1.1w.tar.gz
    tar -xzf openssl-1.1.1w.tar.gz
    rm -f openssl-1.1.1w.tar.gz
fi

echo "[4/6] 检查 FFmpeg 源码..."
if [ ! -d "$FFMPEG_SRC_DIR" ]; then
    wget -O ffmpeg-5.1.tar.xz https://ffmpeg.org/releases/ffmpeg-5.1.tar.xz
    tar -xf ffmpeg-5.1.tar.xz
    mv ffmpeg-5.1 ffmpeg-src
    rm -f ffmpeg-5.1.tar.xz
fi

build_one_abi() {
    local abi="$1"
    local ff_arch=""
    local ff_cpu=""
    local ff_cross_prefix=""
    local ff_cc=""
    local ff_cxx=""
    local openssl_target=""
    local ff_extra_config=""

    case "$abi" in
        arm64-v8a)
            ff_arch="arm64"
            ff_cpu="armv8-a"
            ff_cross_prefix="aarch64-linux-android-"
            ff_cc="aarch64-linux-android21-clang"
            ff_cxx="aarch64-linux-android21-clang++"
            openssl_target="android-arm64"
            # 避免 arm64 静态库链接到 Android .so 时出现非 PIC 的 AArch64 重定位错误
            ff_extra_config="--disable-neon --disable-asm"
            ;;
        armeabi-v7a)
            ff_arch="arm"
            ff_cpu="armv7-a"
            ff_cross_prefix="arm-linux-androideabi-"
            ff_cc="armv7a-linux-androideabi21-clang"
            ff_cxx="armv7a-linux-androideabi21-clang++"
            openssl_target="android-arm"
            ;;
        x86)
            ff_arch="x86"
            ff_cpu="i686"
            ff_cross_prefix="i686-linux-android-"
            ff_cc="i686-linux-android21-clang"
            ff_cxx="i686-linux-android21-clang++"
            openssl_target="android-x86"
            # Android x86 下 libavcodec/x86 某些 CABAC inline asm 会触发寄存器不足错误
            # 仅对 x86 关闭 asm，保证构建稳定
            ff_extra_config="--disable-asm --disable-inline-asm --disable-x86asm"
            ;;
        x86_64)
            ff_arch="x86_64"
            ff_cpu="x86_64"
            ff_cross_prefix="x86_64-linux-android-"
            ff_cc="x86_64-linux-android21-clang"
            ff_cxx="x86_64-linux-android21-clang++"
            openssl_target="android-x86_64"
            # x86_64 下也可能出现非 PIC 的 x86 汇编目标重定位错误
            ff_extra_config="--disable-asm --disable-inline-asm --disable-x86asm"
            ;;
        *)
            echo "不支持的 ABI: $abi"
            exit 1
            ;;
    esac

    local openssl_out="${WORK_DIR}/openssl-android/${abi}"
    local ffmpeg_out="${WORK_DIR}/ffmpeg-android/${abi}"
    local project_base="${THIRD_PARTY_ROOT}/${abi}"
    local project_lib_dir="${project_base}/lib"
    local project_include_dir="${project_base}/include"
    local openssl_marker="${openssl_out}/.android_no_stdio_built"

    echo "----------------------------------------"
    echo "开始构建 ABI: ${abi}"
    echo "----------------------------------------"

    echo "[${abi}] 构建 OpenSSL..."
    if [ ! -f "$openssl_marker" ]; then
        rm -rf "$openssl_out"
        cd "$OPENSSL_SRC_DIR"
        make clean 2>/dev/null || true
        ./Configure "$openssl_target" \
            --prefix="$openssl_out" \
            --openssldir="$openssl_out" \
            no-shared no-tests no-stdio no-ui-console no-engine no-async
        make -j"$(nproc)"
        make install_sw
        mkdir -p "$openssl_out"
        touch "$openssl_marker"
    fi

    mkdir -p "$openssl_out/lib/pkgconfig"
    cat > "$openssl_out/lib/pkgconfig/openssl.pc" << EOF
prefix=$openssl_out
exec_prefix=
libdir=$openssl_out/lib
includedir=$openssl_out/include

Name: OpenSSL
Description: Secure Sockets Layer and cryptography libraries
Version: 1.1.1w
Requires: libssl libcrypto
EOF

    cat > "$openssl_out/lib/pkgconfig/libssl.pc" << EOF
prefix=$openssl_out
exec_prefix=
libdir=$openssl_out/lib
includedir=$openssl_out/include

Name: libssl
Description: OpenSSL SSL/TLS library
Version: 1.1.1w
Requires.private: libcrypto
Libs: -L$openssl_out/lib -lssl
Libs.private: -ldl -lm
Cflags: -I$openssl_out/include
EOF

    cat > "$openssl_out/lib/pkgconfig/libcrypto.pc" << EOF
prefix=$openssl_out
exec_prefix=
libdir=$openssl_out/lib
includedir=$openssl_out/include

Name: libcrypto
Description: OpenSSL cryptography library
Version: 1.1.1w
Libs: -L$openssl_out/lib -lcrypto
Libs.private: -ldl -lm
Cflags: -I$openssl_out/include
EOF

    echo "[${abi}] 构建 FFmpeg..."
    cd "$FFMPEG_SRC_DIR"
    make distclean 2>/dev/null || true

    env PKG_CONFIG_PATH="$openssl_out/lib/pkgconfig" PKG_CONFIG_LIBDIR="$openssl_out/lib/pkgconfig" ./configure \
        --prefix="$ffmpeg_out" \
        --arch="$ff_arch" \
        --cpu="$ff_cpu" \
        --cross-prefix="$ff_cross_prefix" \
        --cc="$ff_cc" \
        --cxx="$ff_cxx" \
        --target-os=android \
        --enable-cross-compile \
        --enable-pic \
        --enable-small \
        --disable-everything \
        --disable-debug \
        --disable-doc \
        --disable-ffmpeg \
        --disable-ffplay \
        --disable-ffprobe \
        --disable-avdevice \
        --disable-postproc \
        --enable-protocol=file,http,https,hls,concat,data,crypto,tls,rtmp,rtsp,udp,tcp \
        --enable-demuxer=hls,mpegts,mpegtsraw,mov,mp4,concat,flv,image2 \
        --enable-muxer=mp4,mov,mpegts,image2,mp3 \
        --enable-decoder=h264,hevc,aac,mp3,png,mjpeg \
        --enable-encoder=aac,png,mjpeg,mp3,mpeg4 \
        --enable-parser=h264,hevc,aac,aac_latm,mpegaudio \
        --enable-bsf=h264_mp4toannexb,hevc_mp4toannexb,aac_adtstoasc \
        --enable-filter=aresample,scale,transpose,trim,atrim,setpts,asetpts,overlay,concat \
        --enable-openssl \
        --enable-nonfree \
        --pkg-config=pkg-config \
        --extra-cflags="-I$openssl_out/include -fPIC -DANDROID -D__ANDROID_API__=21" \
        --extra-ldflags="-L$openssl_out/lib" \
        --extra-libs="-lssl -lcrypto -ldl -landroid -lm" \
        --pkg-config-flags="--static" \
        $ff_extra_config

    echo "[${abi}] 检查加密协议开关..."
    grep -E "CONFIG_(OPENSSL|CRYPTO_PROTOCOL|TLS_PROTOCOL)" config.h || true

    make -j"$(nproc)"
    make install

    echo "[${abi}] 复制产物到 orange-ffmpeg third_party..."
    mkdir -p "$project_lib_dir" "$project_include_dir"

    cp "$ffmpeg_out/lib/libavcodec.a" "$project_lib_dir/"
    cp "$ffmpeg_out/lib/libavformat.a" "$project_lib_dir/"
    cp "$ffmpeg_out/lib/libavutil.a" "$project_lib_dir/"
    cp "$ffmpeg_out/lib/libswresample.a" "$project_lib_dir/"
    cp "$ffmpeg_out/lib/libswscale.a" "$project_lib_dir/"

    if [ -f "$openssl_out/lib/libssl.a" ] && [ -f "$openssl_out/lib/libcrypto.a" ]; then
        cp "$openssl_out/lib/libssl.a" "$project_lib_dir/"
        cp "$openssl_out/lib/libcrypto.a" "$project_lib_dir/"
    fi

    rm -rf "$project_include_dir"/*
    cp -r "$ffmpeg_out/include/"* "$project_include_dir/"

    echo "[${abi}] 完成，输出: $project_base"
    ls -lh "$project_lib_dir"
}

echo "[5/6] 开始构建目标 ABI: ${REQUESTED_ABIS}"
for raw_abi in "${ABI_LIST[@]}"; do
    abi="$(echo "$raw_abi" | xargs)"
    if [ -n "$abi" ]; then
        build_one_abi "$abi"
    fi
done

echo "[6/6] 全部完成"
echo "third_party 输出目录: $THIRD_PARTY_ROOT"
