# OrangePlayer Legacy - Android 4.4 兼容版本

这是 OrangePlayer 的 Android 4.4 (API 19) 兼容版本。

## 版本信息

- **应用 ID**: `com.orange.player.legacy`
- **最低版本**: Android 4.4 (API 19)
- **目标版本**: Android 14 (API 36)
- **版本名称**: 1.0-legacy

## 功能对比

### ✅ 支持的功能

| 功能 | 状态 |
|------|------|
| IJK 播放器 | ✅ 完全支持 |
| 系统播放器 | ✅ 完全支持 |
| 弹幕 | ✅ 完全支持 |
| 字幕 | ✅ 完全支持 |
| 倍速播放 | ✅ 完全支持 |
| 播放控制 | ✅ 完全支持 |
| 播放历史 | ✅ 完全支持 |
| DLNA 投屏 | ✅ 完全支持 |
| 手势控制 | ✅ 完全支持 |

### ❌ 不支持的功能

| 功能 | 原因 |
|------|------|
| ExoPlayer | 需要 API 21+ |
| 阿里云播放器 | 需要 API 21+ |
| OCR 字幕识别 | 需要 API 21+ |
| 语音识别 | 需要 API 29+ |
| ML Kit 翻译 | 需要 API 19+（功能有限）|
| 画中画 | 需要 API 26+ |
| SurfaceControl | 需要 API 29+ |
| FFmpeg 解码器 | 需要 API 21+ |

## 支持的格式

### IJK 播放器

- ✅ MP4 (H.264/AAC)
- ✅ HLS (m3u8)
- ✅ RTSP 直播
- ✅ RTMP 直播
- ✅ FLV
- ✅ HTTP 流媒体

### 系统播放器

- ✅ MP4 (H.264/AAC)
- ✅ 3GP
- ✅ WebM (VP8)
- ⚠️ HTTP 流媒体（有限支持）
- ❌ HLS (m3u8)
- ❌ RTSP/RTMP

## 构建说明

### 1. 同步项目

```bash
./gradlew :app-legacy:clean
```

### 2. 构建 Debug 版本

```bash
./gradlew :app-legacy:assembleDebug
```

### 3. 构建 Release 版本

```bash
./gradlew :app-legacy:assembleRelease
```

### 4. 安装到设备

```bash
./gradlew :app-legacy:installDebug
```

## 测试清单

在 Android 4.4 设备/模拟器上测试：

- [ ] 应用启动
- [ ] IJK 播放器播放 MP4
- [ ] IJK 播放器播放 HLS
- [ ] 系统播放器播放 MP4
- [ ] 播放控制（播放/暂停/快进/快退）
- [ ] 进度条拖动
- [ ] 音量/亮度调节
- [ ] 全屏切换
- [ ] 弹幕显示和发送
- [ ] 字幕显示
- [ ] 倍速播放
- [ ] 播放历史
- [ ] DLNA 投屏
- [ ] 内存占用
- [ ] 性能表现

## 性能优化

### 内存优化

```java
// 降低缓存大小
GSYVideoManager.instance().setBufferSize(512);

// 及时释放资源
@Override
protected void onDestroy() {
    super.onDestroy();
    videoView.release();
}
```

### 渲染优化

```java
// Android 4.4 使用 TextureView 更稳定
GSYVideoType.setRenderType(GSYVideoType.TEXTURE);
```

## 已知限制

1. **播放器选择**
   - 默认使用 IJK 播放器
   - 不支持 ExoPlayer
   - 系统播放器功能有限

2. **格式支持**
   - 系统播放器不支持 HLS
   - 不支持现代编解码器（HEVC, VP9, AV1）

3. **性能**
   - 旧设备性能较弱
   - 建议降低视频质量
   - 减少缓存大小

4. **UI 限制**
   - 部分 Material Design 3 特性不可用
   - 某些动画效果可能较差

## 与标准版本的区别

| 项目 | 标准版 (app) | Legacy 版 (app-legacy) |
|------|-------------|----------------------|
| minSdk | 23 (Android 6.0) | 19 (Android 4.4) |
| 应用 ID | com.orange.player | com.orange.player.legacy |
| ExoPlayer | ✅ | ❌ |
| 阿里云播放器 | ✅ | ❌ |
| OCR | ✅ | ❌ |
| 语音识别 | ✅ | ❌ |
| 覆盖率 | 99% | 99.5% |

## 发布说明

### APK 命名

- Debug: `app-legacy-debug.apk`
- Release: `app-legacy-release.apk`

### 版本号

- versionCode: 与标准版相同
- versionName: 添加 `-legacy` 后缀

### 发布渠道

建议作为独立版本发布：
- Google Play: 作为单独的应用
- 其他渠道: 标注为"兼容版"或"Legacy 版"

## 常见问题

### Q: 为什么创建 Legacy 版本？

A: 为了支持 Android 4.4 设备，覆盖额外的 0.5% 用户。

### Q: 应该使用哪个版本？

A: 
- Android 6.0+: 使用标准版 (app)
- Android 4.4-5.1: 使用 Legacy 版 (app-legacy)

### Q: Legacy 版本会持续维护吗？

A: 会进行基础维护和 bug 修复，但新功能优先添加到标准版。

### Q: 如何切换播放器？

A: Legacy 版本默认使用 IJK 播放器，可以在设置中切换到系统播放器。

## 相关文档

- [Android 4.4 支持指南](../docs/ANDROID_4.4_SUPPORT.md)
- [Android 4.0 兼容性分析](../docs/ANDROID_4_COMPATIBILITY.md)
- [FAQ](../docs/FAQ.md)

## 技术支持

如有问题，请提交 Issue 并注明使用的是 Legacy 版本。

---

**最后更新**: 2026-01-31  
**版本**: 1.0-legacy  
**基于**: OrangePlayer v1.0.8
