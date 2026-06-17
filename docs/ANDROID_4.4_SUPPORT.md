# Android 4.4 (KitKat) 支持指南

## 概述

OrangePlayer 可以支持 Android 4.4 (API 19)，但需要移除部分功能和调整依赖。

## 兼容性状态

### ✅ 完全兼容的功能

| 功能 | 最低 API | 状态 |
|------|---------|------|
| AndroidX AppCompat | 14 | ✅ 兼容 |
| Material Design | 14 | ✅ 兼容 |
| Glide 图片加载 | 14 | ✅ 兼容 |
| 弹幕 (DanmakuFlameMaster) | 14 | ✅ 兼容 |
| 系统播放器 | 1 | ✅ 兼容 |
| 基础播放控制 | 1 | ✅ 兼容 |
| 字幕显示 | 14 | ✅ 兼容 |

### ⚠️ 需要调整的功能

| 功能 | 最低 API | 调整方案 |
|------|---------|---------|
| IJK 播放器 | 16 | 使用旧版本或特殊编译 |
| GSYVideoPlayer | 16 | 使用旧版本 |
| ExoPlayer | 21 | 移除或使用旧版本 |

### ❌ 不兼容的功能

| 功能 | 最低 API | 说明 |
|------|---------|------|
| OCR 字幕识别 | 21 | 需要 Android 5.0+ |
| 语音识别 (Vosk) | 21 | 需要 Android 5.0+ |
| SurfaceControl | 29 | 需要 Android 10+ |
| 阿里云播放器 | 21 | 需要 Android 5.0+ |

## 实施步骤

### 1. 修改 Gradle 配置

#### palyerlibrary/build.gradle

```gradle
android {
    namespace 'com.orange.playerlibrary'
    compileSdk 36

    defaultConfig {
        minSdk 19  // 降低到 Android 4.4
        targetSdk 36
    }
    
    // 其他配置保持不变
}

dependencies {
    // 基础依赖（兼容 API 19）
    compileOnly 'androidx.appcompat:appcompat:1.7.1'
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    
    // GSYVideoPlayer - 使用兼容版本
    compileOnly 'io.github.706412584:gsyVideoPlayer-java:1.1.0'
    
    // IJK 播放器 - 兼容 API 16+
    compileOnly 'io.github.706412584:gsyVideoPlayer-java:1.1.0'
    
    // ExoPlayer - 移除或使用旧版本
    // compileOnly 'io.github.706412584:gsyVideoPlayer-exo_player2:1.1.0'  // 需要 API 21+
    
    // 弹幕（兼容 API 14）
    compileOnly 'com.github.bilibili:DanmakuFlameMaster:0.9.25'
    
    // 移除不兼容的依赖
    // compileOnly 'cz.adaptech.tesseract4android:tesseract4android:4.7.0'  // 需要 API 21+
    // compileOnly 'com.alphacephei:vosk-android:0.3.47'  // 需要 API 21+
    // compileOnly 'com.google.mlkit:translate:17.0.2'  // 需要 API 19+，但功能有限
}
```

#### app/build.gradle

```gradle
android {
    namespace 'com.orange.player'
    compileSdk 36

    defaultConfig {
        applicationId "com.orange.player"
        minSdk 19  // 降低到 Android 4.4
        targetSdk 36
        versionCode 1
        versionName "1.0"
        
        ndk {
            abiFilters 'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64'
        }
    }
    
    // 其他配置保持不变
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.7.1'
    implementation 'com.google.android.material:material:1.13.0'
    implementation 'androidx.activity:activity:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.2.1'
    
    implementation project(':palyerlibrary')
    
    // 播放器内核 - 仅使用兼容 API 19 的
    implementation 'io.github.706412584:gsyVideoPlayer-java:1.1.0'      // IJK 播放器
    // implementation 'io.github.706412584:gsyVideoPlayer-exo_player2:1.1.0'  // 移除 ExoPlayer
    
    // 移除阿里云播放器（需要 API 21+）
    // implementation('io.github.706412584:gsyVideoPlayer-aliplay:1.1.0')
    
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    implementation 'com.github.bilibili:DanmakuFlameMaster:0.9.25'
    
    // 移除 AI 功能（需要 API 21+）
    // implementation 'cz.adaptech.tesseract4android:tesseract4android:4.7.0'
    // implementation 'com.google.mlkit:translate:17.0.2'
    // implementation 'com.alphacephei:vosk-android:0.3.47'
    
    // 移除 FFmpeg 解码器（需要 API 21+）
    // implementation 'org.jellyfin.media3:media3-ffmpeg-decoder:1.8.0+1'
    
    // DLNA 投屏（兼容 API 19）
    implementation 'com.github.uaoan:UaoanDLNA:1.0.1'
    implementation 'com.squareup.okhttp3:okhttp:4.9.0'
    implementation 'com.squareup.okio:okio:2.10.0'
    implementation 'org.jetbrains.kotlin:kotlin-stdlib:1.5.30'
}
```

### 2. 修改代码 - 添加版本检查

#### OrangevideoView.java

```java
private void initPlayerFactory() {
    if (mPlayerFactoryInitialized) {
        return;
    }
    
    String engine = mSettingsManager.getPlayerEngine();
    boolean fallbackToSystem = false;
    
    switch (engine) {
        case PlayerConstants.ENGINE_IJK:
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {  // API 16
                if (isIjkPlayerAvailable()) {
                    try {
                        PlayerFactory.setPlayManager(OrangeIjkPlayerManager.class);
                        android.util.Log.d(TAG, "使用 IJK 播放器");
                    } catch (Exception e) {
                        android.util.Log.w(TAG, "IJK 播放器初始化失败", e);
                        fallbackToSystem = true;
                    }
                } else {
                    fallbackToSystem = true;
                }
            } else {
                android.util.Log.w(TAG, "Android 版本过低，不支持 IJK 播放器");
                fallbackToSystem = true;
            }
            break;
            
        case PlayerConstants.ENGINE_EXO:
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {  // API 21
                // ExoPlayer 代码
            } else {
                android.util.Log.w(TAG, "Android 版本过低，不支持 ExoPlayer");
                fallbackToSystem = true;
            }
            break;
            
        case PlayerConstants.ENGINE_ALI:
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {  // API 21
                // 阿里云播放器代码
            } else {
                android.util.Log.w(TAG, "Android 版本过低，不支持阿里云播放器");
                fallbackToSystem = true;
            }
            break;
            
        default:
            fallbackToSystem = true;
            break;
    }
    
    if (fallbackToSystem) {
        PlayerFactory.setPlayManager(OrangeSystemPlayerManager.class);
        android.util.Log.d(TAG, "使用系统播放器");
    }
    
    mPlayerFactoryInitialized = true;
}
```

#### 移除或条件化 API 21+ 的功能

```java
// OCR 功能
public void enableOCR() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        // OCR 代码
    } else {
        Toast.makeText(getContext(), "OCR 需要 Android 5.0+", Toast.LENGTH_SHORT).show();
    }
}

// 语音识别
public void enableSpeechRecognition() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {  // API 29
        // 语音识别代码
    } else {
        Toast.makeText(getContext(), "语音识别需要 Android 10+", Toast.LENGTH_SHORT).show();
    }
}

// SurfaceControl
@RequiresApi(api = Build.VERSION_CODES.Q)
private void reparent(@Nullable SurfaceView surfaceView) {
    // 保持 @RequiresApi 注解，确保只在 API 29+ 调用
}
```

### 3. 更新 AndroidManifest.xml

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- 最低版本声明 -->
    <uses-sdk
        android:minSdkVersion="19"
        android:targetSdkVersion="36" />
    
    <!-- 权限声明 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="28" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    
    <!-- Android 4.4 需要的权限 -->
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    
    <application>
        <!-- 应用配置 -->
    </application>
</manifest>
```

### 4. 测试清单

#### 必须测试的功能

- [ ] 系统播放器播放本地视频
- [ ] 系统播放器播放网络视频
- [ ] IJK 播放器（如果可用）
- [ ] 播放控制（播放/暂停/快进/快退）
- [ ] 进度条拖动
- [ ] 音量/亮度调节
- [ ] 全屏切换
- [ ] 弹幕显示和发送
- [ ] 字幕显示
- [ ] 倍速播放
- [ ] 播放列表

#### 已知限制

- ❌ ExoPlayer 不可用
- ❌ 阿里云播放器不可用
- ❌ OCR 字幕识别不可用
- ❌ 语音识别不可用
- ❌ 画中画不可用（需要 API 26+）
- ❌ SurfaceControl 无缝切换不可用
- ⚠️ IJK 播放器可能需要特殊编译

## 支持的格式（Android 4.4）

### 系统播放器支持

| 格式 | 支持情况 |
|------|---------|
| MP4 (H.264/AAC) | ✅ 完全支持 |
| 3GP | ✅ 完全支持 |
| WebM (VP8) | ✅ 完全支持 |
| HTTP 流媒体 | ⚠️ 有限支持 |
| HLS (m3u8) | ❌ 不支持 |
| DASH | ❌ 不支持 |
| RTSP | ⚠️ 有限支持 |
| RTMP | ❌ 不支持 |

### IJK 播放器支持（如果可用）

| 格式 | 支持情况 |
|------|---------|
| MP4 | ✅ 完全支持 |
| HLS (m3u8) | ✅ 完全支持 |
| RTSP | ✅ 完全支持 |
| RTMP | ✅ 完全支持 |
| FLV | ✅ 完全支持 |

## 性能优化建议

### 1. 减少内存占用

```java
// 降低缓存大小
GSYVideoManager.instance().setBufferSize(512);  // 默认 1024

// 及时释放资源
@Override
protected void onDestroy() {
    super.onDestroy();
    videoView.release();
}
```

### 2. 优化渲染

```java
// Android 4.4 使用 TextureView 更稳定
GSYVideoType.setRenderType(GSYVideoType.TEXTURE);
```

### 3. 禁用不必要的功能

```java
// 禁用 OCR
videoView.disableOCR();

// 禁用语音识别
videoView.disableSpeechRecognition();
```

## 市场数据

| Android 版本 | API Level | 市场占有率 (2024) |
|-------------|-----------|------------------|
| 4.4 (KitKat) | 19 | ~0.5% |
| 5.0+ | 21+ | ~99% |

**覆盖率提升**: 从 99% 提升到 99.5%

## 推荐配置

### 方案一：仅支持 Android 5.0+ (推荐)

```gradle
minSdk 21  // 覆盖 99%，支持所有功能
```

### 方案二：支持 Android 4.4

```gradle
minSdk 19  // 覆盖 99.5%，部分功能受限
```

### 方案三：双版本发布

- **标准版**: minSdk 21，完整功能
- **兼容版**: minSdk 19，基础功能

## 常见问题

### Q: IJK 播放器在 Android 4.4 上能用吗？

A: 理论上可以，但需要：
1. GSYVideoPlayer 支持 API 16+
2. IJK 的 so 库需要针对 API 16 编译
3. 可能需要禁用某些高级特性

### Q: 为什么不支持 ExoPlayer？

A: ExoPlayer 最低需要 API 21 (Android 5.0)，这是 Google 官方的限制。

### Q: 性能会受影响吗？

A: 是的，Android 4.4 设备通常：
- 硬件性能较弱
- 内存较小
- 不支持现代编解码器
- 建议降低视频质量和缓存大小

### Q: 值得支持 Android 4.4 吗？

A: 取决于目标用户：
- **一般应用**: 不值得（0.5% 用户，开发成本高）
- **企业应用**: 可能值得（特定设备要求）
- **教育应用**: 可能值得（旧设备较多）

## 总结

✅ **可以支持 Android 4.4**，但需要：
1. 移除 ExoPlayer、阿里云播放器
2. 移除 OCR、语音识别等 AI 功能
3. 添加版本检查代码
4. 充分测试兼容性

⚠️ **权衡考虑**：
- 覆盖率仅提升 0.5%
- 功能受限
- 维护成本增加
- 性能可能较差

💡 **建议**：
- 如果目标用户主要是现代设备，保持 minSdk 21
- 如果有特殊需求（企业、教育），可以降到 minSdk 19
- 考虑发布两个版本：标准版和兼容版

---

**最后更新**: 2026-01-31  
**测试状态**: 需要在 Android 4.4 设备/模拟器上测试  
**推荐配置**: minSdk 21 (除非有特殊需求)
