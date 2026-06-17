# 自编译精简 FFmpeg 指南

## 目标

编译一个精简版 FFmpeg，只包含：
- m3u8/HLS 下载
- AES-128 解密
- 合并视频
- 转换为 mp4

预期体积：3-5 MB（单架构）

---

## 模块归属与依赖（新增）

- **产物归属**：FFmpeg 编译产物统一归属 `orange-ffmpeg` 模块，路径为 `orange-ffmpeg/src/main/jniLibs/arm64-v8a`。
- **依赖方式**：`orange-downloader` 对 `orange-ffmpeg` 为可选依赖，需要 ts 合并时在业务层添加 `implementation project(':orange-ffmpeg')`。
- **职责边界**：`palyerlibrary` 不再承载 FFmpeg 产物与 JNI 封装，避免与下载库职责混淆。

---

## 一、WSL 环境准备


```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装编译工具
sudo apt install -y build-essential git yasm nasm pkg-config \
    wget unzip autoconf automake libtool texinfo \
    python3 python3-pip

# 创建工作目录
mkdir -p ~/ffmpeg-build
cd ~/ffmpeg-build
```

---

## 二、下载 NDK

```bash
# 下载 NDK r21e（推荐版本，稳定）
wget https://dl.google.com/android/repository/android-ndk-r21e-linux-x86_64.zip
unzip android-ndk-r21e-linux-x86_64.zip
rm android-ndk-r21e-linux-x86_64.zip

# 设置环境变量
export NDK_ROOT=~/ffmpeg-build/android-ndk-r21e
export PATH=$NDK_ROOT:$PATH

# 验证
$NDK_ROOT/ndk-build --version
```

---

## 三、下载 OpenSSL（支持 HTTPS/AES）

```bash
cd ~/ffmpeg-build

# 下载 OpenSSL
wget https://www.openssl.org/source/openssl-1.1.1w.tar.gz
tar -xzf openssl-1.1.1w.tar.gz
rm openssl-1.1.1w.tar.gz
```

---

## 四、编译 OpenSSL

创建编译脚本：

```bash
cd ~/ffmpeg-build
cat > build_openssl.sh << 'EOF'
#!/bin/bash

NDK_ROOT=~/ffmpeg-build/android-ndk-r21e
OPENSSL_SRC=~/ffmpeg-build/openssl-1.1.1w
OUTPUT_DIR=~/ffmpeg-build/openssl-android

# 目标架构
ARCHS=("arm64" "arm")
TRIPLES=("aarch64-linux-android" "arm-linux-androideabi")

for i in ${!ARCHS[@]}; do
    ARCH=${ARCHS[$i]}
    TRIPLE=${TRIPLES[$i]}
    
    echo "Building OpenSSL for $ARCH..."
    
    cd $OPENSSL_SRC
    make clean 2>/dev/null
    
    # 设置编译器
    export ANDROID_NDK_HOME=$NDK_ROOT
    export PATH=$NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin:$PATH
    
    # 配置
    if [ "$ARCH" = "arm64" ]; then
        ./Configure android-arm64 \
            --prefix=$OUTPUT_DIR/$ARCH \
            --openssldir=$OUTPUT_DIR/$ARCH \
            no-shared no-tests
    else
        ./Configure android-arm \
            --prefix=$OUTPUT_DIR/$ARCH \
            --openssldir=$OUTPUT_DIR/$ARCH \
            no-shared no-tests
    fi
    
    # 编译安装
    make -j$(nproc)
    make install
    
    echo "OpenSSL for $ARCH done!"
done
EOF

chmod +x build_openssl.sh
./build_openssl.sh
```

---

## 五、下载并配置 FFmpeg

```bash
cd ~/ffmpeg-build

# 下载 FFmpeg 5.1（稳定版）
wget https://ffmpeg.org/releases/ffmpeg-5.1.tar.xz
tar -xf ffmpeg-5.1.tar.xz
rm ffmpeg-5.1.tar.xz
mv ffmpeg-5.1 ffmpeg-src
```

---

## 六、创建精简配置

```bash
cd ~/ffmpeg-build
cat > ffmpeg_config.sh << 'EOF'
#!/bin/bash

# 精简配置：只保留 m3u8下载、AES解密、合并、mp4输出

COMMON_OPTIONS="
--disable-everything
--enable-small
--enable-optimizations
--disable-debug
--disable-doc
--disable-ffmpeg
--disable-ffplay
--disable-ffprobe
--disable-avdevice
--disable-postproc
--disable-network
--disable-hwaccels
--disable-parsers
--disable-bsfs
--disable-zlib
--disable-bzlib
--disable-iconv
"

# 启用的协议
PROTOCOLS="
--enable-protocol=file,http,https,hls,concat,data,crypto,tls
"

# 启用的解封装器
DEMUXERS="
--enable-demuxer=hls,mpegts,mpegtsraw,mov,mp4,concat
"

# 启用的封装器
MUXERS="
--enable-muxer=mp4,mov,mpegts
"

# 启用的解码器
DECODERS="
--enable-decoder=h264,hevc,aac,mp3,mpegts
"

# 启用的过滤器
FILTERS="
--enable-filter=aresample,aformat,scale
"

# OpenSSL 支持
SSL_OPTIONS="
--enable-openssl
--enable-nonfree
"

echo "FFmpeg 精简配置已创建"
EOF

chmod +x ffmpeg_config.sh
```

---

## 七、编译 FFmpeg（arm64-v8a）

```bash
cd ~/ffmpeg-build
cat > build_ffmpeg_arm64.sh << 'EOF'
#!/bin/bash

NDK_ROOT=~/ffmpeg-build/android-ndk-r21e
FFMPEG_SRC=~/ffmpeg-build/ffmpeg-src
OPENSSL_DIR=~/ffmpeg-build/openssl-android/arm64
OUTPUT_DIR=~/ffmpeg-build/ffmpeg-android/arm64

export ANDROID_NDK_HOME=$NDK_ROOT
export TOOLCHAIN=$NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64
export SYSROOT=$TOOLCHAIN/sysroot
export PATH=$TOOLCHAIN/bin:$PATH

ARCH=arm64
CPU=armv8-a
TRIPLE=aarch64-linux-android
CC=$TRIPLE21-clang
CXX=$TRIPLE21-clang++
CROSS_PREFIX=$TRIPLE-

cd $FFMPEG_SRC
make clean 2>/dev/null

./configure \
    --prefix=$OUTPUT_DIR \
    --arch=$ARCH \
    --cpu=$CPU \
    --cross-prefix=$CROSS_PREFIX \
    --cc=$CC \
    --cxx=$CXX \
    --sysroot=$SYSROOT \
    --target-os=android \
    --enable-cross-compile \
    --enable-pic \
    --enable-jni \
    --enable-mediacodec \
    --pkg-config=pkg-config \
    --pkg-config-flags="--static" \
    --extra-cflags="-I$OPENSSL_DIR/include -fPIC -DANDROID -D__ANDROID_API__=21" \
    --extra-ldflags="-L$OPENSSL_DIR/lib -static" \
    --extra-libs="-lssl -lcrypto -lc -ldl" \
    \
    --disable-everything \
    --enable-small \
    --enable-optimizations \
    --disable-debug \
    --disable-doc \
    --disable-ffmpeg \
    --disable-ffplay \
    --disable-ffprobe \
    --disable-avdevice \
    --disable-postproc \
    \
    --enable-protocol=file,http,https,hls,concat,data \
    --enable-demuxer=hls,mpegts,mpegtsraw,mov,mp4,concat \
    --enable-muxer=mp4,mov,mpegts \
    --enable-decoder=h264,hevc,aac,mp3,mpegts \
    --enable-parser=h264,hevc,aac,aac_latm,mpegaudio,mpegts \
    --enable-bsf=h264_mp4toannexb,hevc_mp4toannexb \
    --enable-filter=aresample,aformat,scale \
    \
    --enable-openssl \
    --enable-nonfree

make -j$(nproc)
make install

echo "FFmpeg arm64 编译完成!"
ls -lh $OUTPUT_DIR/lib/
EOF

chmod +x build_ffmpeg_arm64.sh
./build_ffmpeg_arm64.sh
```

---

## 八、创建 JNI 封装

```bash
cd ~/ffmpeg-build
mkdir -p jni-wrapper/src/main/java/com/orange/ffmpeg
mkdir -p jni-wrapper/src/main/jni

# 创建 JNI 源码目录
mkdir -p jni-wrapper/jni
```

### 8.1 创建 JNI 头文件

```bash
cat > jni-wrapper/jni/ffmpeg_wrapper.h << 'EOF'
#ifndef FFMPEG_WRAPPER_H
#define FFMPEG_WRAPPER_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

// 初始化 FFmpeg
JNIEXPORT jint JNICALL Java_com_orange_ffmpeg_FFmpegKit_nativeInit(JNIEnv *env, jclass clazz);

// 执行 FFmpeg 命令
JNIEXPORT jint JNICALL Java_com_orange_ffmpeg_FFmpegKit_nativeExecute(JNIEnv *env, jclass clazz, jobjectArray args);

// 异步执行
JNIEXPORT void JNICALL Java_com_orange_ffmpeg_FFmpegKit_nativeExecuteAsync(JNIEnv *env, jclass clazz, jobjectArray args, jobject callback);

// 取消执行
JNIEXPORT void JNICALL Java_com_orange_ffmpeg_FFmpegKit_nativeCancel(JNIEnv *env, jclass clazz);

// 获取版本
JNIEXPORT jstring JNICALL Java_com_orange_ffmpeg_FFmpegKit_nativeGetVersion(JNIEnv *env, jclass clazz);

#ifdef __cplusplus
}
#endif

#endif
EOF
```

### 8.2 创建 JNI 实现

```bash
cat > jni-wrapper/jni/ffmpeg_wrapper.c << 'EOF'
#include "ffmpeg_wrapper.h"
#include <android/log.h>
#include <pthread.h>
#include <stdlib.h>
#include <string.h>

#define LOG_TAG "FFmpegWrapper"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// FFmpeg 头文件
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libavutil/avutil.h>

// 全局取消标志
static volatile int g_cancel_flag = 0;
static pthread_t g_exec_thread = 0;

// 异步执行参数
typedef struct {
    JavaVM *vm;
    jobject callback;
    int argc;
    char **argv;
} AsyncExecParams;

// FFmpeg 主函数声明
extern int ffmpeg_main(int argc, char **argv);

// 初始化
JNIEXPORT jint JNICALL Java_com_orange_ffmpeg_FFmpegKit_nativeInit(JNIEnv *env, jclass clazz) {
    LOGI("FFmpeg initializing...");
    // FFmpeg 5.x 不需要 av_register_all
    LOGI("FFmpeg version: %s", av_version_info());
    return 0;
}

// 执行命令
JNIEXPORT jint JNICALL Java_com_orange_ffmpeg_FFmpegKit_nativeExecute(JNIEnv *env, jclass clazz, jobjectArray args) {
    if (args == NULL) {
        LOGE("args is NULL");
        return -1;
    }
    
    int argc = (*env)->GetArrayLength(env, args);
    char **argv = (char **)malloc(sizeof(char *) * (argc + 1));
    
    for (int i = 0; i < argc; i++) {
        jstring arg = (jstring)(*env)->GetObjectArrayElement(env, args, i);
        const char *arg_str = (*env)->GetStringUTFChars(env, arg, NULL);
        argv[i] = strdup(arg_str);
        (*env)->ReleaseStringUTFChars(env, arg, arg_str);
        (*env)->DeleteLocalRef(env, arg);
    }
    argv[argc] = NULL;
    
    LOGI("Executing FFmpeg with %d args", argc);
    
    g_cancel_flag = 0;
    int ret = ffmpeg_main(argc, argv);
    
    // 清理
    for (int i = 0; i < argc; i++) {
        free(argv[i]);
    }
    free(argv);
    
    LOGI("FFmpeg returned: %d", ret);
    return ret;
}

// 回调方法
static void call_callback(JavaVM *vm, jobject callback, int result) {
    JNIEnv *env;
    (*vm)->AttachCurrentThread(vm, &env, NULL);
    
    jclass callbackClass = (*env)->GetObjectClass(env, callback);
    jmethodID onComplete = (*env)->GetMethodID(env, callbackClass, "onComplete", "(I)V");
    
    if (onComplete) {
        (*env)->CallVoidMethod(env, callback, onComplete, result);
    }
    
    (*vm)->DetachCurrentThread(vm);
}

// 异步执行线程
static void *async_exec_thread(void *arg) {
    AsyncExecParams *params = (AsyncExecParams *)arg;
    
    JNIEnv *env;
    (*(params->vm))->AttachCurrentThread(params->vm, &env, NULL);
    
    int result = Java_com_orange_ffmpeg_FFmpegKit_nativeExecute(env, NULL, NULL);
    
    call_callback(params->vm, params->callback, result);
    
    (*(params->vm))->DetachCurrentThread(params->vm);
    
    free(params);
    return NULL;
}

// 异步执行
JNIEXPORT void JNICALL Java_com_orange_ffmpeg_FFmpegKit_nativeExecuteAsync(JNIEnv *env, jclass clazz, jobjectArray args, jobject callback) {
    JavaVM *vm;
    (*env)->GetJavaVM(env, &vm);
    
    // 转换参数
    int argc = (*env)->GetArrayLength(env, args);
    char **argv = (char **)malloc(sizeof(char *) * argc);
    
    for (int i = 0; i < argc; i++) {
        jstring arg = (jstring)(*env)->GetObjectArrayElement(env, args, i);
        const char *arg_str = (*env)->GetStringUTFChars(env, arg, NULL);
        argv[i] = strdup(arg_str);
        (*env)->ReleaseStringUTFChars(env, arg, arg_str);
    }
    
    // 创建线程执行
    AsyncExecParams *params = malloc(sizeof(AsyncExecParams));
    params->vm = vm;
    params->callback = (*env)->NewGlobalRef(env, callback);
    params->argc = argc;
    params->argv = argv;
    
    pthread_create(&g_exec_thread, NULL, async_exec_thread, params);
}

// 取消
JNIEXPORT void JNICALL Java_com_orange_ffmpeg_FFmpegKit_nativeCancel(JNIEnv *env, jclass clazz) {
    g_cancel_flag = 1;
}

// 获取版本
JNIEXPORT jstring JNICALL Java_com_orange_ffmpeg_FFmpegKit_nativeGetVersion(JNIEnv *env, jclass clazz) {
    return (*env)->NewStringUTF(env, av_version_info());
}
EOF
```

### 8.3 创建 Android.mk

```bash
cat > jni-wrapper/jni/Android.mk << 'EOF'
LOCAL_PATH := $(call my-dir)

# FFmpeg 库
include $(CLEAR_VARS)
LOCAL_MODULE := ffmpeg
LOCAL_SRC_FILES := libffmpeg.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)

# 封装库
include $(CLEAR_VARS)
LOCAL_MODULE := ffmpeg-wrapper
LOCAL_SRC_FILES := ffmpeg_wrapper.c
LOCAL_SHARED_LIBRARIES := ffmpeg
LOCAL_LDLIBS := -llog -landroid
LOCAL_CFLAGS := -DANDROID
include $(BUILD_SHARED_LIBRARY)
EOF
```

### 8.4 创建 Application.mk

```bash
cat > jni-wrapper/jni/Application.mk << 'EOF'
APP_ABI := arm64-v8a
APP_PLATFORM := android-21
APP_STL := c++_static
APP_CPPFLAGS := -std=c++11
EOF
```

---

## 九、编译 JNI 封装

```bash
cd ~/ffmpeg-build/jni-wrapper/jni

# 复制编译好的 FFmpeg 库
cp ~/ffmpeg-build/ffmpeg-android/arm64/lib/libavcodec.a libavcodec.a
cp ~/ffmpeg-build/ffmpeg-android/arm64/lib/libavformat.a libavformat.a
cp ~/ffmpeg-build/ffmpeg-android/arm64/lib/libavutil.a libavutil.a
cp ~/ffmpeg-build/ffmpeg-android/arm64/lib/libswresample.a libswresample.a
cp ~/ffmpeg-build/ffmpeg-android/arm64/lib/libswscale.a libswscale.a

# 复制头文件
cp -r ~/ffmpeg-build/ffmpeg-android/arm64/include .

# 使用 NDK 编译
$NDK_ROOT/ndk-build NDK_PROJECT_PATH=. APP_BUILD_SCRIPT=./Android.mk
```

---

## 十、创建 Java 封装类

在 Android 项目中创建：

```java
// orange-ffmpeg/src/main/java/com/orange/ffmpeg/FFmpegKit.java
package com.orange.ffmpeg;


public class FFmpegKit {
    
    static {
        System.loadLibrary("ffmpeg");
        System.loadLibrary("ffmpeg-wrapper");
        nativeInit();
    }
    
    public interface ExecuteCallback {
        void onComplete(int result);
    }
    
    // 执行 FFmpeg 命令（同步）
    public static int execute(String[] args) {
        return nativeExecute(args);
    }
    
    // 执行 FFmpeg 命令（异步）
    public static void executeAsync(String[] args, ExecuteCallback callback) {
        nativeExecuteAsync(args, callback);
    }
    
    // 取消执行
    public static void cancel() {
        nativeCancel();
    }
    
    // 获取版本
    public static String getVersion() {
        return nativeGetVersion();
    }
    
    // ========== 便捷方法 ==========
    
    /**
     * 下载 m3u8 并转换为 mp4
     */
    public static void downloadM3U8(String m3u8Url, String outputPath, ExecuteCallback callback) {
        executeAsync(new String[]{
            "-i", m3u8Url,
            "-c", "copy",
            "-bsf:a", "aac_adtstoasc",
            outputPath
        }, callback);
    }
    
    /**
     * 合并视频文件
     */
    public static int concatVideos(String[] inputFiles, String outputPath) {
        // 创建文件列表
        StringBuilder fileList = new StringBuilder();
        for (String file : inputFiles) {
            fileList.append("file '").append(file).append("'\n");
        }
        
        return execute(new String[]{
            "-f", "concat",
            "-safe", "0",
            "-i", fileList.toString(),
            "-c", "copy",
            outputPath
        });
    }
    
    /**
     * 解密 AES 加密的 m3u8
     */
    public static void decryptM3U8(String m3u8Url, String key, String outputPath, ExecuteCallback callback) {
        executeAsync(new String[]{
            "-allowed_extensions", "ALL",
            "-protocol_whitelist", "file,http,https,tls,tcp",
            "-i", m3u8Url,
            "-c", "copy",
            outputPath
        }, callback);
    }
    
    // Native 方法
    private static native int nativeInit();
    private static native int nativeExecute(String[] args);
    private static native void nativeExecuteAsync(String[] args, ExecuteCallback callback);
    private static native void nativeCancel();
    private static native String nativeGetVersion();
}
```

---

## 十一、使用示例

```java
// 下载 m3u8
FFmpegKit.downloadM3U8(
    "https://example.com/video.m3u8",
    "/sdcard/Movies/video.mp4",
    result -> {
        if (result == 0) {
            Log.d("FFmpeg", "下载成功");
        } else {
            Log.e("FFmpeg", "下载失败: " + result);
        }
    }
);

// 合并视频
FFmpegKit.concatVideos(new String[]{
    "/sdcard/Movies/part1.ts",
    "/sdcard/Movies/part2.ts"
}, "/sdcard/Movies/merged.mp4");

// 执行自定义命令
FFmpegKit.execute(new String[]{
    "-i", "input.mp4",
    "-vn", "-acodec", "copy",
    "output.aac"
});
```

---

## 预期结果

| 文件 | 大小 |
|------|------|
| libffmpeg.so | 3-4 MB |
| libffmpeg-wrapper.so | 50-100 KB |
| **总计** | **~3-5 MB** |

---

## 常见问题

### 1. OpenSSL 编译失败
确保安装了 Perl 和 Make：
```bash
sudo apt install perl make
```

### 2. FFmpeg 配置失败
检查 NDK 版本和路径是否正确。

### 3. 运行时找不到库
确保在 Gradle 中配置了正确的 abiFilters：
```gradle
android {
    defaultConfig {
        ndk { abiFilters "arm64-v8a" }
    }
}
```

---

## 参考

- [FFmpeg 编译文档](https://trac.ffmpeg.org/wiki/CompilationGuide)
- [Android NDK](https://developer.android.com/ndk)
- [FFmpeg Wiki - HLS](https://trac.ffmpeg.org/wiki/HLS)
