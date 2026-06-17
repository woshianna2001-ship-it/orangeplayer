# build_ffmpeg.sh 使用说明

## 作用

`scripts/build_ffmpeg.sh` 用于在 WSL/Linux 环境下：

- 自动准备 Android NDK
- 自动准备 OpenSSL 源码与编译产物
- 自动准备 FFmpeg 源码
- 按指定 ABI 构建精简版 FFmpeg 静态库
- 将产物复制到 `orange-ffmpeg/src/main/cpp/third_party/ffmpeg/<abi>/`

当前脚本已包含：

- OpenSSL 支持
- `crypto` / `tls` protocol 支持
- 构建前自动探测 Clash 代理
- 构建结束自动清理临时代理环境变量

## 默认 ABI

不传环境变量时，默认构建以下全部架构：

- `armeabi-v7a`
- `arm64-v8a`
- `x86`
- `x86_64`

如需只构建单 ABI，可设置：

```bash
ORANGE_FFMPEG_ABIS=arm64-v8a
```

## 运行环境

建议在 WSL Ubuntu 中执行。

脚本默认使用以下目录：

- 工作目录：`/home/xcwl/ffmpeg-build`
- 若不存在：`$HOME/ffmpeg-build`

项目目录示例：

```bash
/mnt/d/android/projecet_iade/orangeplayer
```

## 直接运行

全架构构建：

```bash
cd /mnt/d/android/projecet_iade/orangeplayer
chmod +x scripts/build_ffmpeg.sh
bash scripts/build_ffmpeg.sh
```

仅构建 arm64：

```bash
cd /mnt/d/android/projecet_iade/orangeplayer
chmod +x scripts/build_ffmpeg.sh
ORANGE_FFMPEG_ABIS=arm64-v8a bash scripts/build_ffmpeg.sh
```

## 代理逻辑

脚本启动时会自动执行以下逻辑：

- 读取 WSL 网关地址
- 使用 `CLASH_PROXY_PORT` 指定的端口检查 Clash 是否可连
- 可连则临时设置 `http_proxy/https_proxy`
- 不可连则走直连
- 脚本退出时自动 `unset` 代理变量

默认端口：

```bash
7887
```

如果你的 Clash 端口不同，可这样运行：

```bash
CLASH_PROXY_PORT=7890 bash scripts/build_ffmpeg.sh
```

## 当前启用的关键能力

脚本中的 FFmpeg configure 已启用：

- `--enable-openssl`
- `--enable-protocol=file,http,https,hls,concat,data,crypto,tls,rtmp,rtsp,udp,tcp`
- `--enable-demuxer=hls,mpegts,mpegtsraw,mov,mp4,concat,flv`
- `--enable-muxer=mp4,mov,mpegts`

## 产物位置

构建完成后，每个 ABI 的产物会复制到：

```bash
orange-ffmpeg/src/main/cpp/third_party/ffmpeg/<abi>/
```

例如：

```bash
orange-ffmpeg/src/main/cpp/third_party/ffmpeg/arm64-v8a/lib/libavformat.a
orange-ffmpeg/src/main/cpp/third_party/ffmpeg/arm64-v8a/lib/libcrypto.a
orange-ffmpeg/src/main/cpp/third_party/ffmpeg/arm64-v8a/lib/libssl.a
```

## 如何确认加密支持已开启

构建完成后，在 WSL 中执行：

```bash
BUILD_ROOT=/home/xcwl/ffmpeg-build
[ -d "$BUILD_ROOT" ] || BUILD_ROOT="$HOME/ffmpeg-build"
grep -R -n CONFIG_CRYPTO_PROTOCOL "$BUILD_ROOT/ffmpeg-src"
grep -R -n CONFIG_TLS_PROTOCOL "$BUILD_ROOT/ffmpeg-src"
grep -R -n CONFIG_OPENSSL "$BUILD_ROOT/ffmpeg-src"
```

预期至少看到：

```bash
ffbuild/config.mak:CONFIG_CRYPTO_PROTOCOL=yes
config_components.h:#define CONFIG_CRYPTO_PROTOCOL 1
ffbuild/config.mak:CONFIG_TLS_PROTOCOL=yes
config_components.h:#define CONFIG_TLS_PROTOCOL 1
config.h:#define CONFIG_OPENSSL 1
```

说明：

- `CONFIG_OPENSSL` 常见于 `config.h`
- `CONFIG_CRYPTO_PROTOCOL` / `CONFIG_TLS_PROTOCOL` 常见于 `config_components.h` 和 `ffbuild/config.mak`

## 构建完成后建议

回到项目根目录重新安装调试包：

```powershell
.\gradlew :app:installDebug
```

## 常见问题

### 1. WSL 提示 localhost 代理未镜像

这是 NAT 模式 WSL 的常见提示。脚本当前不会强依赖 `127.0.0.1`，而是优先读取 WSL 网关地址并检查 Clash 端口。

### 2. 只看到 `CONFIG_OPENSSL 1`

不要只查 `config.h`。

请同时检查：

```bash
grep -R -n CONFIG_CRYPTO_PROTOCOL "$BUILD_ROOT/ffmpeg-src"
grep -R -n CONFIG_TLS_PROTOCOL "$BUILD_ROOT/ffmpeg-src"
```

### 3. 构建日志里出现语法错误

先检查脚本语法：

```bash
bash -n scripts/build_ffmpeg.sh
```

### 4. 产物已经复制，但仍然合并失败

这说明：

- 构建链路可能已正常
- 但运行时还可能存在流格式、加密流解复用、码流兼容性或 JNI 封装限制问题

建议进一步结合 Android 设备日志分析：

- `video_downloader`
- `orangeffmpegkit`
- `FFmpegKit`

