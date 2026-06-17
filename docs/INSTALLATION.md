# 安装指南

完整的 OrangePlayer 安装和依赖配置指南。

## 目录

- [添加仓库](#添加仓库)
- [依赖配置](#依赖配置)
  - [最小依赖（系统播放器）](#最小依赖系统播放器)
  - [推荐配置（ExoPlayer）](#推荐配置exoplayer)
  - [其他播放内核](#其他播放内核)
  - [可选功能依赖](#可选功能依赖)
- [AndroidManifest 配置](#androidmanifest-配置)
- [混淆配置](#混淆配置)

---

## 添加仓库

在项目的 `settings.gradle` 中添加仓库：

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
        // 阿里云播放器仓库（如需使用阿里云内核）
        maven { url 'https://maven.aliyun.com/repository/releases' }
    }
}
```

---

## 依赖配置

### 最小依赖（系统播放器）

⚠️ **重要**：OrangePlayer 基于 GSYVideoPlayer，必须同时添加 GSY 的基础依赖，否则会报 `NoClassDefFoundError`！

#### 方案一：支持传递依赖的构建工具（推荐）

如果你的构建工具支持自动解析传递依赖（如 Gradle、Maven），只需添加：

```gradle
// app/build.gradle
dependencies {
    // OrangePlayer 核心库
    implementation 'com.github.706412584:orangeplayer:v1.0.3'
    
    // GSY 基础依赖（会自动引入子依赖）
    implementation 'io.github.706412584:gsyVideoPlayer-java:1.1.0'
}
```

#### 方案二：不支持传递依赖的构建工具

如果你的构建工具不自动解析传递依赖，需要手动添加所有子依赖：

```gradle
// app/build.gradle
dependencies {
    // OrangePlayer 核心库
    implementation 'com.github.706412584:orangeplayer:v1.0.3'
    
    // GSY 基础依赖
    implementation 'io.github.706412584:gsyVideoPlayer-java:1.1.0'
    
    // GSY 子依赖（手动添加）
    implementation 'io.github.706412584:gsyVideoPlayer-base:1.1.0'
    implementation 'io.github.706412584:gsyVideoPlayer-proxy_cache:1.1.0'
    implementation 'io.github.706412584:gsyijkjava:1.0.0'
}
```

> **依赖说明：**
> - `gsyvideoplayer-java` - GSY 主模块
> - `gsyvideoplayer-base` - 包含 `BasePlayerManager` 等核心类
> - `gsyVideoPlayer-proxy_cache` - 视频缓存功能
> - `gsyijkjava` - IJK 接口层（约 50KB，不含 so 库，所有播放器都需要）

---

### 推荐配置（ExoPlayer）

如果使用 ExoPlayer（推荐，格式支持更全）：

```gradle
// app/build.gradle
dependencies {
    // OrangePlayer 核心库
    implementation 'com.github.706412584:orangeplayer:v1.0.3'
    
    // GSY 基础依赖
    implementation 'io.github.706412584:gsyVideoPlayer-java:1.1.0'
    
    // ExoPlayer 播放内核
    implementation 'io.github.706412584:gsyVideoPlayer-exo_player2:1.1.0'
    
    // 如果构建工具不支持传递依赖，还需要手动添加：
    // implementation 'io.github.706412584:gsyVideoPlayer-base:1.1.0'
    // implementation 'io.github.706412584:gsyVideoPlayer-proxy_cache:1.1.0'
    // implementation 'io.github.706412584:gsyijkjava:1.0.0'
    // implementation 'androidx.media3:media3-exoplayer:1.8.0'
    // implementation 'androidx.media3:media3-ui:1.8.0'
}
```

---

### 其他播放内核

#### IJK 播放器

IJK 内核需要额外添加 so 库依赖：

```gradle
dependencies {
    // IJK 播放器 so 库（按需添加对应 CPU 架构）
    implementation 'io.github.706412584:gsyVideoPlayer-arm64:1.1.0'   // arm64-v8a（推荐）
    implementation 'io.github.706412584:gsyVideoPlayer-armv7a:1.1.0'  // armeabi-v7a
    implementation 'io.github.706412584:gsyVideoPlayer-armv5:1.1.0'   // armeabi（旧设备）
    implementation 'io.github.706412584:gsyVideoPlayer-x86:1.1.0'     // x86 模拟器
    implementation 'io.github.706412584:gsyVideoPlayer-x64:1.1.0'     // x86_64 模拟器
}
```

> **注意**：`gsyijkjava` 是 IJK 的 Java 接口层（约 50KB），所有播放器都需要。上面的 so 库才是真正的 IJK 播放器（每个约 10-15MB），只有使用 IJK 内核时才需要添加。

#### 扩展编码支持

如需支持 MPEG 编码、RTSP、concat、crypto 协议等，添加扩展 so 库：

```gradle
dependencies {
    // 扩展编码支持（支持 mpeg 编码和更多协议，支持 16k Page Size）
    // 注意：会增加包体积
    implementation 'io.github.706412584:gsyVideoPlayer-ex_so:1.1.0'
}
```

> **说明**：普通版本支持 H.263/H.264/H.265 等常见编码，对于 MPEG 编码可能出现有声音无画面的情况。`ex_so` 扩展库补充了 MPEG 编码和更多协议支持。

#### 阿里云播放器

```gradle
dependencies {
    // 阿里云播放器模式（需要 License）
    implementation 'io.github.706412584:gsyVideoPlayer-aliplay:1.1.0'
}
```

⚠️ **注意**：阿里云播放器从 5.4.0 版本开始需要 License 授权。详见 [阿里云播放器配置](ALIYUN_PLAYER.md)。

---

### 可选功能依赖

#### DLNA 投屏

```gradle
dependencies {
    // DLNA 投屏库
    implementation 'com.github.AnyListen:UaoanDLNA:1.0.1'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.okio:okio:3.6.0'
}
```

#### OCR 字幕识别与翻译

```gradle
dependencies {
    // OCR 文字识别
    implementation 'cz.adaptech.tesseract4android:tesseract4android:4.7.0'
    
    // 文字翻译
    implementation 'com.google.mlkit:translate:17.0.2'
}
```

详细配置请查看 [OCR 功能指南](OCR_GUIDE.md)。

#### 语音识别（需要 Android 10+）

```gradle
dependencies {
    // Vosk 离线语音识别
    implementation 'com.alphacephei:vosk-android:0.3.47'
}
```

详细配置请查看 [语音识别指南](SPEECH_RECOGNITION.md)。

---

## AndroidManifest 配置

在 `AndroidManifest.xml` 中添加必要的权限和配置：

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          xmlns:tools="http://schemas.android.com/tools">
    
    <!-- 允许使用 minSdk 24 的投屏库 -->
    <uses-sdk tools:overrideLibrary="com.uaoanlao.tv" />
    
    <!-- 网络权限（必需）-->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <!-- 投屏需要的 WiFi 权限（可选，投屏功能需要）-->
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
    
    <application
        android:usesCleartextTraffic="true"
        ... >
        
        <!-- Activity 配置（支持横竖屏切换和画中画）-->
        <activity
            android:name=".YourActivity"
            android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation|keyboardHidden"
            android:supportsPictureInPicture="true"
            android:resizeableActivity="true">
        </activity>
    </application>
</manifest>
```

### 关键配置说明

| 配置项 | 说明 |
|--------|------|
| `usesCleartextTraffic="true"` | 允许 HTTP 明文流量（播放 HTTP 视频源需要）|
| `configChanges` | 防止横竖屏切换时 Activity 重建 |
| `supportsPictureInPicture` | 启用画中画模式 |
| `resizeableActivity` | 允许调整窗口大小 |

---

## 混淆配置

在 `proguard-rules.pro` 中添加混淆规则：

```proguard
# GSYVideoPlayer
-keep class com.shuyu.gsyvideoplayer.** { *; }
-keep class tv.danmaku.ijk.** { *; }

# OrangePlayer
-keep class com.orange.playerlibrary.** { *; }

# Tesseract OCR
-keep class com.googlecode.tesseract.android.** { *; }

# ML Kit Translation
-keep class com.google.mlkit.** { *; }

# Vosk 语音识别
-keep class org.vosk.** { *; }

# 阿里云播放器
-keep class com.aliyun.player.** { *; }
-keep class com.cicada.player.** { *; }

# DLNA 投屏
-keep class com.uaoanlao.tv.** { *; }
```

---

## 完整依赖配置示例

### 标准配置（推荐）

适用于支持传递依赖解析的构建工具（Gradle、Maven）：

```gradle
// app/build.gradle
dependencies {
    // OrangePlayer 核心库
    implementation 'com.github.706412584:orangeplayer:v1.0.3'
    
    // === GSY 基础依赖（必需）===
    implementation 'io.github.706412584:gsyVideoPlayer-java:1.1.0'
    
    // === 播放内核（按需选择）===
    
    // ExoPlayer 模式（推荐）
    implementation 'io.github.706412584:gsyVideoPlayer-exo_player2:1.1.0'
    
    // 阿里云播放器模式（需要 License）
    implementation 'io.github.706412584:gsyVideoPlayer-aliplay:1.1.0'
    
    // IJK 播放器 so 库（根据目标设备选择）
    implementation 'io.github.706412584:gsyVideoPlayer-arm64:1.1.0'
    implementation 'io.github.706412584:gsyVideoPlayer-armv7a:1.1.0'
    
    // === 可选功能 ===
    
    // DLNA 投屏
    implementation 'com.github.AnyListen:UaoanDLNA:1.0.1'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    
    // OCR 字幕识别与翻译
    implementation 'cz.adaptech.tesseract4android:tesseract4android:4.7.0'
    implementation 'com.google.mlkit:translate:17.0.2'
    
    // 语音识别（需要 Android 10+）
    implementation 'com.alphacephei:vosk-android:0.3.47'
}
```

### 完整配置（手动传递依赖）

适用于不支持传递依赖解析的构建工具：

```gradle
// app/build.gradle
dependencies {
    // OrangePlayer 核心库
    implementation 'com.github.706412584:orangeplayer:v1.0.3'
    
    // === GSY 基础依赖（必需）===
    implementation 'io.github.706412584:gsyVideoPlayer-java:1.1.0'
    
    // GSY 子依赖（手动添加）
    implementation 'io.github.706412584:gsyVideoPlayer-proxy_cache:1.1.0'
    implementation 'io.github.706412584:gsyVideoPlayer-base:1.1.0'
    implementation 'io.github.706412584:gsyijkjava:1.0.0'
    
    // === 播放内核（按需选择）===
    
    // ExoPlayer 模式（推荐）
    implementation 'io.github.706412584:gsyVideoPlayer-exo_player2:1.1.0'
    implementation 'androidx.media3:media3-exoplayer:1.8.0'
    implementation 'androidx.media3:media3-ui:1.8.0'
    implementation 'androidx.media3:media3-common:1.8.0'
    
    // 阿里云播放器模式（需要 License）
    implementation 'io.github.706412584:gsyVideoPlayer-aliplay:1.1.0'
    
    // IJK 播放器 so 库（根据目标设备选择）
    implementation 'io.github.706412584:gsyVideoPlayer-arm64:1.1.0'
    implementation 'io.github.706412584:gsyVideoPlayer-armv7a:1.1.0'
    
    // === AndroidX 基础库（通常已包含）===
    implementation 'androidx.appcompat:appcompat:1.7.1'
    implementation 'androidx.annotation:annotation:1.6.0'
    implementation 'androidx.core:core:1.13.0'
    
    // === 可选功能 ===
    
    // DLNA 投屏
    implementation 'com.github.AnyListen:UaoanDLNA:1.0.1'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.okio:okio:3.6.0'
    
    // OCR 字幕识别与翻译
    implementation 'cz.adaptech.tesseract4android:tesseract4android:4.7.0'
    implementation 'com.google.mlkit:translate:17.0.2'
    
    // 语音识别（需要 Android 10+）
    implementation 'com.alphacephei:vosk-android:0.3.47'
}
```

---

## 下一步

- [基本使用](../README.md#基本使用)
- [播放内核切换](PLAYER_ENGINES.md)
- [OCR 功能配置](OCR_GUIDE.md)
- [语音识别配置](SPEECH_RECOGNITION.md)
- [常见问题](FAQ.md)
