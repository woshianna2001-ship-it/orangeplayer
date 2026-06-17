# Android 4.0 兼容性分析

## 当前状态

### 最低 SDK 版本

| 模块 | 当前 minSdk | Android 版本 |
|------|-------------|--------------|
| app | 23 | Android 6.0 (Marshmallow) |
| palyerlibrary | 21 | Android 5.0 (Lollipop) |

### Android 4.0 信息

- **API Level**: 14 (Android 4.0) / 15 (Android 4.0.3)
- **发布时间**: 2011 年
- **市场占有率**: < 0.1% (2024 年数据)

## 兼容性障碍

### 1. 核心依赖库限制

#### AndroidX 库
```gradle
androidx.appcompat:appcompat:1.7.1
```
- **最低要求**: API 14
- **实际可用**: API 14+
- ✅ **兼容 Android 4.0**

#### Material Design
```gradle
com.google.android.material:material:1.13.0
```
- **最低要求**: API 14
- **实际可用**: API 14+
- ✅ **兼容 Android 4.0**

### 2. 播放器内核限制

#### GSYVideoPlayer
```gradle
io.github.706412584:gsyVideoPlayer-java:1.1.0
```
- **最低要求**: API 16 (Android 4.1)
- ❌ **不兼容 Android 4.0**
- **原因**: 依赖 MediaCodec API (API 16+)

#### IJK 播放器
```gradle
io.github.706412584:gsyVideoPlayer-java:1.1.0
```
- **最低要求**: API 16
- ❌ **不兼容 Android 4.0**
- **原因**: 
  - FFmpeg 编译需要 API 16+ 的 NDK 特性
  - MediaCodec 硬件解码需要 API 16+

#### ExoPlayer
```gradle
io.github.706412584:gsyVideoPlayer-exo_player2:1.1.0
```
- **最低要求**: API 21 (Android 5.0)
- ❌ **不兼容 Android 4.0**
- **原因**: 使用了 Lollipop+ 的 API

#### 系统播放器 (MediaPlayer)
- **最低要求**: API 1
- ✅ **兼容 Android 4.0**
- **限制**: 功能有限，不支持现代流媒体格式

### 3. 可选功能限制

#### 弹幕 (DanmakuFlameMaster)
```gradle
com.github.bilibili:DanmakuFlameMaster:0.9.25
```
- **最低要求**: API 14
- ✅ **兼容 Android 4.0**

#### OCR (Tesseract)
```gradle
cz.adaptech.tesseract4android:4.7.0
```
- **最低要求**: API 21
- ❌ **不兼容 Android 4.0**

#### 语音识别 (Vosk)
```gradle
com.alphacephei:vosk-android:0.3.47
```
- **最低要求**: API 21
- ❌ **不兼容 Android 4.0**

#### ML Kit 翻译
```gradle
com.google.mlkit:translate:17.0.2
```
- **最低要求**: API 19
- ❌ **不兼容 Android 4.0**

### 4. 代码层面限制

#### SurfaceControl (画中画、无缝切换)
```java
@RequiresApi(api = Build.VERSION_CODES.Q)  // API 29
```
- 需要 Android 10+
- Android 4.0 不支持

#### 语音识别服务
```java
@RequiresApi(api = Build.VERSION_CODES.Q)  // API 29
```
- 需要 Android 10+
- Android 4.0 不支持

## 支持 Android 4.0 的方案

### 方案一：最小化功能版本（不推荐）

#### 修改内容

1. **降低 minSdk**
```gradle
// palyerlibrary/build.gradle
defaultConfig {
    minSdk 14  // Android 4.0
}
```

2. **移除不兼容的依赖**
```gradle
dependencies {
    // 仅保留基础依赖
    implementation 'androidx.appcompat:appcompat:1.7.1'
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    
    // 移除所有播放器内核
    // 移除 OCR、语音识别、ML Kit
    // 移除 ExoPlayer、IJK
}
```

3. **仅使用系统播放器**
```java
// 强制使用 MediaPlayer
PlayerFactory.setPlayManager(SystemPlayerManager.class);
```

#### 功能限制

- ❌ 无法使用 IJK 播放器
- ❌ 无法使用 ExoPlayer
- ❌ 无法使用阿里云播放器
- ❌ 无 OCR 字幕识别
- ❌ 无语音识别
- ❌ 无 ML Kit 翻译
- ❌ 无画中画
- ❌ 无 SurfaceControl 无缝切换
- ✅ 可以使用系统播放器（功能有限）
- ✅ 可以使用弹幕
- ✅ 基础播放控制

#### 支持的格式

系统播放器在 Android 4.0 上支持：
- MP4 (H.264/AAC)
- 3GP
- WebM (VP8)
- 本地文件
- HTTP 流媒体（有限）

不支持：
- HLS (m3u8)
- DASH
- RTSP/RTMP 直播
- 现代编解码器 (HEVC, VP9, AV1)

### 方案二：创建独立的 Android 4.0 分支（推荐）

如果确实需要支持 Android 4.0，建议：

1. **创建独立分支**
```bash
git checkout -b android-4.0-legacy
```

2. **简化功能**
- 移除所有现代播放器内核
- 仅保留系统播放器
- 移除 AI 功能（OCR、语音识别）
- 简化 UI（移除 Material Design 3 特性）

3. **独立维护**
- 主分支继续支持 Android 5.0+
- Legacy 分支仅修复严重 bug

### 方案三：不支持 Android 4.0（强烈推荐）

#### 理由

1. **市场占有率极低**
   - Android 4.0 市场占有率 < 0.1%
   - 大部分设备已升级或淘汰

2. **开发成本高**
   - 需要维护两套代码
   - 测试成本翻倍
   - 功能严重受限

3. **用户体验差**
   - 无法使用现代播放器
   - 格式支持有限
   - 性能较差

4. **安全风险**
   - Android 4.0 不再接收安全更新
   - 存在已知安全漏洞

5. **行业标准**
   - Google Play 要求 minSdk 21+ (2021 年起)
   - 主流应用最低支持 Android 5.0

## 推荐配置

### 当前推荐 (minSdk 21)

```gradle
android {
    defaultConfig {
        minSdk 21  // Android 5.0 (Lollipop)
        targetSdk 36
    }
}
```

**覆盖率**: 99%+ 的活跃设备

**支持功能**:
- ✅ 所有播放器内核
- ✅ 所有 AI 功能
- ✅ 现代流媒体格式
- ✅ 硬件加速
- ✅ 完整的 UI 特性

### 如果需要更广泛兼容 (minSdk 19)

```gradle
android {
    defaultConfig {
        minSdk 19  // Android 4.4 (KitKat)
        targetSdk 36
    }
}
```

**覆盖率**: 99.5%+ 的活跃设备

**需要调整**:
- 移除 ExoPlayer（或使用旧版本）
- 移除 OCR、语音识别
- 保留 IJK 播放器（需要特殊编译）
- 条件性使用 API 21+ 特性

## 实施步骤（如果坚持支持 Android 4.0）

### 1. 修改 Gradle 配置

```gradle
// palyerlibrary/build.gradle
android {
    defaultConfig {
        minSdk 14
    }
}

// app/build.gradle
android {
    defaultConfig {
        minSdk 14
    }
}
```

### 2. 移除不兼容依赖

```gradle
dependencies {
    // 基础依赖
    implementation 'androidx.appcompat:appcompat:1.7.1'
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    
    // 弹幕（可选）
    implementation 'com.github.bilibili:DanmakuFlameMaster:0.9.25'
    
    // 移除所有播放器内核
    // 移除 AI 功能
}
```

### 3. 修改代码

```java
// 移除所有 @RequiresApi 注解的功能
// 或添加版本检查

if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    // 使用 SurfaceControl
} else {
    // 降级方案
}
```

### 4. 测试

- 在 Android 4.0 模拟器上测试
- 测试所有基础功能
- 确认降级方案正常工作

## 结论

**不建议支持 Android 4.0**，原因：

1. ❌ 市场占有率极低（< 0.1%）
2. ❌ 开发和维护成本高
3. ❌ 功能严重受限
4. ❌ 用户体验差
5. ❌ 存在安全风险
6. ❌ 违背行业标准

**推荐方案**:
- 保持 minSdk 21 (Android 5.0)
- 覆盖 99%+ 的活跃设备
- 支持所有现代功能
- 符合 Google Play 要求

如果确实有特殊需求（如企业内部应用、特定设备），建议创建独立的 Legacy 分支，而不是影响主版本的开发。

---

**最后更新**: 2026-01-31  
**当前 minSdk**: 21 (palyerlibrary) / 23 (app)  
**推荐 minSdk**: 21 (Android 5.0+)
