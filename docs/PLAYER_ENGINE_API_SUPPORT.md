# 播放器内核 API 支持情况

本文档详细说明各播放器内核对不同 Android 版本的支持情况。

## 播放器内核对比

| 播放器内核 | 最低 API | Android 版本 | 协议支持 | 性能 | 推荐场景 |
|-----------|---------|-------------|---------|------|---------|
| **Media3 ExoPlayer** | API 21 | Android 5.0+ | RTSP, HLS, DASH, HTTP | 优秀 | 现代设备 |
| **IJK 播放器** | API 19 | Android 4.4+ | RTSP, RTMP, HLS, HTTP | 良好 | 兼容旧设备 |
| **系统播放器** | API 14 | Android 4.0+ | HTTP, HLS (有限) | 一般 | 最大兼容性 |
| **阿里云播放器** | API 21 | Android 5.0+ | 全协议 | 优秀 | 商业场景 |

## 详细支持情况

### Media3 ExoPlayer (androidx.media3)

**版本**: 1.9.1  
**最低要求**: API 21 (Android 5.0)

#### 功能支持

| 功能 | 最低 API | Android 版本 |
|-----|---------|-------------|
| 音频播放 | 21 | 5.0 |
| 视频播放 | 21 | 5.0 |
| HDR 视频 | 24 | 7.0 |
| DASH (无 DRM) | 21 | 5.0 |
| DASH (Widevine CENC) | 21 | 5.0 |
| HLS (无 DRM) | 21 | 5.0 |
| HLS (AES-128) | 21 | 5.0 |
| HLS (Widevine CENC) | 19 | 4.4 |
| SmoothStreaming | 19 | 4.4 |

**注意**: 虽然部分功能支持 API 19，但 Media3 库本身要求 API 21+。

**参考**: [Media3 Supported Devices](https://developer.android.com/media/media3/exoplayer/supported-devices)

---

### ExoPlayer 2.x (旧版本，已废弃)

**版本**: 2.0 - 2.19 (com.google.android.exoplayer2)  
**最低要求**: API 16 (Android 4.1) - 早期版本  
**最低要求**: API 19 (Android 4.4) - 后期版本  
**状态**: ⚠️ 已废弃，不再维护

#### 历史版本支持

ExoPlayer 2.x 在其生命周期中经历了多次 minSdkVersion 变更：

- **ExoPlayer 2.0 - 2.10**: 支持 API 16 (Android 4.1+)
- **ExoPlayer 2.11+**: 提高到 API 19 (Android 4.4+)
- **2023年后**: 官方建议迁移到 Media3 (需要 API 21+)

#### 为什么不推荐使用旧版 ExoPlayer？

1. **已废弃**: Google 已停止维护 `com.google.android.exoplayer2`
2. **安全问题**: 不再接收安全更新和 bug 修复
3. **兼容性**: 可能与新版 Android 系统不兼容
4. **依赖冲突**: 与现代 AndroidX 库可能冲突
5. **功能缺失**: 缺少新特性和性能优化

**官方迁移指南**: [ExoPlayer to Media3 Migration](https://developer.android.com/media/media3/exoplayer/migration-guide)

#### 如果必须支持 Android 4.1-4.4

如果你的应用必须支持 Android 4.1-4.4，**不要使用 ExoPlayer**，请使用：

1. **IJK 播放器** (推荐) - 支持 API 19+，功能完整
2. **系统播放器** - 支持 API 14+，功能有限

**重要**: 即使找到旧版 ExoPlayer 2.x 支持 API 16，也不建议使用，因为：
- 已停止维护，存在安全风险
- 可能与项目中的其他依赖冲突
- IJK 播放器是更好的选择

---

### IJK 播放器 (ijkplayer)

**版本**: GSYVideoPlayer 集成版本  
**最低要求**: API 19 (Android 4.4)

#### 协议支持
- ✅ RTSP 直播
- ✅ RTMP 直播
- ✅ HLS (m3u8)
- ✅ HTTP/HTTPS 点播
- ✅ 本地文件播放

#### 优势
- 支持 Android 4.4+
- 协议支持全面
- 性能稳定
- 开源免费

#### 劣势
- so 文件体积较大
- 部分新特性支持较慢

---

### 系统播放器 (MediaPlayer)

**最低要求**: API 14 (Android 4.0)

#### 协议支持
- ✅ HTTP/HTTPS 点播
- ⚠️ HLS (有限支持，兼容性差)
- ❌ RTSP (部分设备不支持)
- ❌ RTMP (不支持)

#### 优势
- 无需额外依赖
- 体积最小
- 支持所有 Android 版本

#### 劣势
- 兼容性差
- 功能有限
- 不推荐用于生产环境

---

### 阿里云播放器

**版本**: 5.4.7.1 (免授权版本)  
**最低要求**: API 21 (Android 5.0)

#### 优势
- 性能优秀
- 商业级稳定性
- 完整的 DRM 支持

#### 劣势
- 需要 API 21+
- 不支持 Android 4.4 及以下

---

## 版本选择建议

### app 模块 (现代设备)
```gradle
minSdk 23  // Android 6.0+

dependencies {
    // 推荐使用 ExoPlayer
    implementation project(':gsyVideoPlayer-exo_player2')
    
    // 或使用阿里云播放器
    implementation project(':gsyVideoPlayer-aliplay')
}
```

### app-legacy 模块 (兼容旧设备)
```gradle
minSdk 14  // Android 4.0+

dependencies {
    // 只使用 IJK 播放器 (API 19+)
    implementation project(':gsyVideoPlayer-java')
    implementation project(':gsyVideoPlayer-armv7a')
    implementation project(':gsyVideoPlayer-armv64')
    
    // 不要添加 ExoPlayer (需要 API 21+)
    // 不要添加阿里云播放器 (需要 API 21+)
}
```

---

## Android 4.x 兼容性总结

| Android 版本 | API Level | 可用播放器 | 推荐方案 |
|-------------|-----------|-----------|---------|
| Android 4.0-4.3 | 14-18 | 系统播放器 | ⚠️ 仅基础播放 |
| Android 4.4 | 19 | 系统播放器, IJK | ✅ 使用 IJK |
| Android 5.0+ | 21+ | 全部 | ✅ 使用 ExoPlayer |

### Android 4.1-4.3 (API 16-18) 限制
- ❌ 不支持 ExoPlayer
- ❌ 不支持 IJK 播放器
- ✅ 只能使用系统播放器
- ⚠️ 功能和兼容性有限

### Android 4.4 (API 19) 推荐配置
- ✅ 使用 IJK 播放器作为主力
- ✅ 系统播放器作为备选
- ❌ 不要使用 ExoPlayer (需要 API 21+)

---

## 模拟器支持

### Media3 ExoPlayer
- ✅ Android Studio 官方模拟器: API 23+ 支持
- ❌ Android Studio 官方模拟器: API 19-22 不支持
- ⚠️ 第三方模拟器: 支持情况不一

### IJK 播放器
- ⚠️ 模拟器音频可能不稳定
- ✅ 真实设备通常正常

**建议**: 尽量在真实设备上测试媒体播放功能。

---

## 常见问题

### Q: Android 4.1 可以使用 ExoPlayer 吗？
**A**: 不可以。
- **Media3 ExoPlayer**: 需要 API 21+ (Android 5.0+)
- **旧版 ExoPlayer 2.x**: 早期版本支持 API 16+，但已废弃且不安全
- **推荐方案**: 使用 IJK 播放器 (API 19+) 或系统播放器 (API 14+)

### Q: 可以使用旧版 ExoPlayer 2.x 支持 Android 4.1 吗？
**A**: 技术上可行但**强烈不推荐**：
- ❌ 已废弃，不再接收安全更新
- ❌ 可能与现代依赖库冲突
- ❌ 缺少新特性和性能优化
- ✅ 更好的选择：IJK 播放器（支持 API 19+，持续维护）

### Q: Android 4.4 推荐使用哪个播放器？
**A**: 推荐使用 IJK 播放器。它支持 API 19+，协议支持全面，性能稳定，且持续维护。

### Q: 为什么模拟器播放有问题？
**A**: 模拟器的媒体栈实现不完整，建议在真实设备上测试。特别是：
- ExoPlayer 需要 API 23+ 的模拟器
- API 19-22 的模拟器可能无法正常播放

### Q: 如何在运行时选择播放器？
**A**: 可以根据 Android 版本动态选择：
```java
if (Build.VERSION.SDK_INT >= 21) {
    // 使用 ExoPlayer (推荐)
    PlayerFactory.setPlayManager(Exo2PlayerManager.class);
} else if (Build.VERSION.SDK_INT >= 19) {
    // 使用 IJK 播放器
    PlayerFactory.setPlayManager(GSYVideoManager.class);
} else {
    // 使用系统播放器 (功能有限)
    PlayerFactory.setPlayManager(SystemPlayerManager.class);
}
```

### Q: GSYVideoPlayer 的 ExoPlayer 模块支持哪个版本？
**A**: GSYVideoPlayer 11.3.0 使用的是 **Media3 ExoPlayer 1.9.1**，需要 API 21+。
- 如果需要支持 Android 4.4 (API 19)，不要添加 ExoPlayer 依赖
- 只使用 IJK 播放器模块即可

---

## 更新日志

- **2026-02-07**: 创建文档，记录各播放器内核 API 支持情况
- 确认 Media3 ExoPlayer 需要 API 21+
- 确认 IJK 播放器支持 API 19+
- 移除 app-legacy 中的阿里云播放器依赖

---

## 参考资料

- [Media3 Supported Devices](https://developer.android.com/media/media3/exoplayer/supported-devices)
- [Media3 Release Notes](https://developer.android.com/jetpack/androidx/releases/media3)
- [GSYVideoPlayer Documentation](https://github.com/CarGuo/GSYVideoPlayer)
- [Android API Levels](https://apilevels.com/)
