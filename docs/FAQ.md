# 常见问题

OrangePlayer 常见问题解答。

## 目录

- [依赖问题](#依赖问题)
- [播放问题](#播放问题)
- [功能问题](#功能问题)
- [性能问题](#性能问题)
- [其他问题](#其他问题)

---

## 依赖问题

### Q1: NoClassDefFoundError: BasePlayerManager

**错误信息：**
```
java.lang.NoClassDefFoundError: Failed resolution of: Lcom/shuyu/gsyvideoplayer/player/BasePlayerManager
```

**原因：** 缺少 GSYVideoPlayer 基础依赖或子依赖

**解决方案：**

**方案一：仅使用系统播放器（最小依赖）**

```gradle
dependencies {
    // OrangePlayer 核心库
    implementation 'com.github.706412584:orangeplayer:v1.0.3'
    
    // GSY 最小依赖（系统播放器必需）
    implementation 'io.github.706412584:gsyVideoPlayer-java:1.1.0'
    implementation 'io.github.706412584:gsyVideoPlayer-base:1.1.0'
    implementation 'io.github.706412584:gsyVideoPlayer-proxy_cache:1.1.0'
    implementation 'io.github.706412584:gsyijkjava:1.0.0'
}
```

**方案二：使用 ExoPlayer（推荐）**

```gradle
dependencies {
    // OrangePlayer 核心库
    implementation 'com.github.706412584:orangeplayer:v1.0.3'
    
    // GSY 基础依赖（必需！）
    implementation 'io.github.706412584:gsyVideoPlayer-java:1.1.0'
    
    // GSY 子依赖（如果构建工具不自动解析传递依赖）
    implementation 'io.github.706412584:gsyVideoPlayer-proxy_cache:1.1.0'
    implementation 'io.github.706412584:gsyVideoPlayer-base:1.1.0'
    implementation 'io.github.706412584:gsyijkjava:1.0.0'
    
    // ExoPlayer 播放内核
    implementation 'io.github.706412584:gsyVideoPlayer-exo_player2:1.1.0'
    
    // ExoPlayer 依赖（Media3）
    implementation 'androidx.media3:media3-exoplayer:1.8.0'
    implementation 'androidx.media3:media3-ui:1.8.0'
}
```

详见 [安装指南](INSTALLATION.md)。

### Q2: 投屏功能不可用

**原因：** 缺少 DLNA 投屏库

**解决方案：**

```gradle
dependencies {
    implementation 'com.github.AnyListen:UaoanDLNA:1.0.1'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.okio:okio:3.6.0'
}
```

### Q3: OCR/语音识别按钮显示"查看安装说明"

**原因：** 缺少对应的 SDK 依赖

**解决方案：**

**OCR 功能：**
```gradle
dependencies {
    implementation 'cz.adaptech.tesseract4android:tesseract4android:4.7.0'
    implementation 'com.google.mlkit:translate:17.0.2'
}
```

详见 [OCR 功能指南](OCR_GUIDE.md)。

**语音识别：**
```gradle
dependencies {
    implementation 'com.alphacephei:vosk-android:0.3.47'
}
```

详见 [语音识别指南](SPEECH_RECOGNITION.md)。

---

## 播放问题

### Q4: 播放黑屏或无声音

**可能原因：**
1. 视频编码格式不支持
2. 播放内核不兼容
3. 视频文件损坏
4. 网络连接问题

**解决方案：**

**1. 尝试切换播放内核**

```java
// 切换到 ExoPlayer
videoView.selectPlayerFactory(PlayerConstants.ENGINE_EXO);
videoView.startPlayLogic();

// 切换到 IJK
videoView.selectPlayerFactory(PlayerConstants.ENGINE_IJK);
videoView.startPlayLogic();

// 切换到系统播放器
videoView.selectPlayerFactory(PlayerConstants.ENGINE_DEFAULT);
videoView.startPlayLogic();
```

**2. 添加扩展编码支持（MPEG 编码）**

如果是 MPEG 编码视频，添加扩展 so 库：

```gradle
dependencies {
    implementation 'io.github.706412584:gsyVideoPlayer-ex_so:1.1.0'
}
```

**3. 检查视频格式**

支持的格式：
- 容器：MP4、MKV、AVI、FLV、TS、M3U8
- 视频编码：H.264、H.265、VP8、VP9
- 音频编码：AAC、MP3、Opus

**4. 检查网络连接**

```java
// 添加网络权限
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

// 允许 HTTP 明文流量
<application android:usesCleartextTraffic="true" ... >
```

### Q5: 阿里云播放器黑屏/水印

**原因：** 阿里云播放器 5.4.0+ 需要 License

**解决方案：**

**方案一：申请 License（推荐）**

1. 登录 [阿里云视频点播控制台](https://vod.console.aliyun.com/)
2. 创建应用获取 License
3. 在 Application 中初始化：

```java
import com.aliyun.player.AliPlayerFactory;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化阿里云播放器 License
        AliPlayerFactory.setLicenseKey("your_license_key");
    }
}
```

**方案二：使用旧版本（5.3.0 免授权）**

```gradle
// 排除默认的阿里云 SDK
implementation ('com.github.706412584:orangeplayer:v1.0.3') {
    exclude group: 'com.aliyun.sdk.android', module: 'AliyunPlayer'
}

// 使用 5.3.0 免授权版本
implementation 'com.aliyun.sdk.android:AliyunPlayer:5.3.0-full'
```

详见 [阿里云播放器配置](ALIYUN_PLAYER.md)。

### Q6: 视频卡顿或缓冲慢

**可能原因：**
1. 网络速度慢
2. 视频码率过高
3. 设备性能不足
4. 缓存设置不当

**解决方案：**

**1. 降低视频分辨率**

选择较低分辨率的视频源。

**2. 启用硬件解码**

```java
PlayerSettingsManager settings = PlayerSettingsManager.getInstance(context);
settings.setHardwareDecode(true);
```

**3. 调整缓冲设置**

```java
// 增加缓冲大小
GSYVideoManager.instance().setBufferSize(512 * 1024);  // 512KB
```

**4. 使用 ExoPlayer**

ExoPlayer 的缓冲策略更优：

```java
videoView.selectPlayerFactory(PlayerConstants.ENGINE_EXO);
```

### Q7: 阿里云播放器无法播放 RTSP 直播流

**错误信息：**
```
E AliFrameWork: [ffmpegDataSource] :open error
E AliFrameWork: [avFormatDemuxer] :avformat_open_input error -1330794744,Protocol not found
E AliFrameWork: ErrorCallback(537198593,Unsupported protocol)
```

**原因：** 阿里云播放器不支持 RTSP 协议

**支持的协议：**
- ✅ HLS (m3u8)
- ✅ RTMP / RTMPS
- ✅ FLV
- ✅ HTTP / HTTPS
- ❌ RTSP（不支持）

**解决方案：**

**方案一：切换到 ExoPlayer 内核（推荐）**

```java
// ExoPlayer 完整支持 RTSP 协议
videoView.selectPlayerFactory(PlayerConstants.ENGINE_EXO);
videoView.setUp("rtsp://192.168.1.6:8554/live", true, "RTSP 直播");
videoView.startPlayLogic();
```

**方案二：切换到 IJK 内核**

```java
// IJK 也支持 RTSP，但性能略低于 ExoPlayer
videoView.selectPlayerFactory(PlayerConstants.ENGINE_IJK);
videoView.setUp("rtsp://192.168.1.6:8554/live", true, "RTSP 直播");
videoView.startPlayLogic();
```

**方案三：使用阿里云支持的协议**

如果必须使用阿里云播放器，请将直播流转换为 HLS 或 RTMP 格式：

```java
// 使用 HLS 格式
videoView.selectPlayerFactory(PlayerConstants.ENGINE_ALI);
videoView.setUp("https://example.com/live.m3u8", true, "HLS 直播");
videoView.startPlayLogic();

// 或使用 RTMP 格式
videoView.setUp("rtmp://example.com/live/stream", true, "RTMP 直播");
videoView.startPlayLogic();
```

**播放器内核协议支持对比：**

| 内核 | RTSP | HLS | RTMP | FLV | HTTP |
|------|------|-----|------|-----|------|
| ExoPlayer | ✅ | ✅ | ❌ | ✅ | ✅ |
| IJK | ✅ | ✅ | ✅ | ✅ | ✅ |
| 阿里云 | ❌ | ✅ | ✅ | ✅ | ✅ |
| 系统播放器 | 取决于设备 | ✅ | ❌ | ❌ | ✅ |

**推荐方案：**
- **RTSP 直播**：使用 ExoPlayer 或 IJK
- **HLS 直播**：阿里云、ExoPlayer、IJK 均可
- **RTMP 直播**：阿里云或 IJK

### Q8: IJK 播放器播放 RTSP/RTMP 直播流问题

**问题描述：**
- RTSP 直播流：播放失败，无画面无声音
- RTMP 直播流：提示错误但有声音，画面停留在错误界面

**可能原因：**
1. **IJK so 库未正确加载**：IJK 播放器需要 native 库支持
2. **RTSP/RTMP 协议支持问题**：IJK 的 RTSP/RTMP 实现可能存在兼容性问题
3. **网络配置问题**：防火墙或网络策略阻止了 RTSP/RTMP 连接
4. **视频编码格式不支持**：某些编码格式 IJK 可能无法解码

**解决方案：**

**方案一：切换到 ExoPlayer 内核（强烈推荐）**

ExoPlayer 对 RTSP 协议的支持更完善，性能也更好：

```java
// 切换到 ExoPlayer
videoView.selectPlayerFactory(PlayerConstants.ENGINE_EXO);
videoView.setUp("rtsp://192.168.1.6:8554/live", true, "RTSP 直播");
videoView.startPlayLogic();
```

**方案二：使用智能内核选择器（自动选择最佳内核）**

启用自动内核选择，播放器会根据协议自动选择最合适的内核：

```java
// 在 Application 或 Activity 中启用
PlayerSettingsManager.getInstance(context).setAutoSelectEngine(true);

// 之后直接播放，会自动选择 ExoPlayer（RTSP）或阿里云（RTMP）
videoView.setUp("rtsp://192.168.1.6:8554/live", true, "RTSP 直播");
videoView.startPlayLogic();
```

**方案三：检查 IJK so 库是否正确加载**

如果必须使用 IJK，请确保 so 库已正确导入：

```gradle
dependencies {
    // IJK 播放器
    implementation 'io.github.706412584:gsyVideoPlayer-java:1.1.0'
    
    // 如果需要支持更多编码格式，添加扩展 so 库
    implementation 'io.github.706412584:gsyVideoPlayer-ex_so:1.1.0'
}
```

检查 so 库是否加载成功：

```java
try {
    System.loadLibrary("ijkffmpeg");
    System.loadLibrary("ijksdl");
    System.loadLibrary("ijkplayer");
    Log.d(TAG, "IJK so 库加载成功");
} catch (UnsatisfiedLinkError e) {
    Log.e(TAG, "IJK so 库加载失败", e);
}
```

**方案四：使用阿里云播放 RTMP（延迟最低）**

如果是 RTMP 直播流，强烈推荐使用阿里云播放器（延迟仅 1-3 秒）：

```java
// 阿里云播放 RTMP 延迟极低
videoView.selectPlayerFactory(PlayerConstants.ENGINE_ALI);
videoView.setUp("rtmp://example.com/live/stream", true, "RTMP 直播");
videoView.startPlayLogic();
```

**播放器内核直播流支持对比：**

| 协议 | ExoPlayer | IJK | 阿里云 | 推荐 |
|------|-----------|-----|--------|------|
| **RTSP** | ✅ 完整支持 | ⚠️ 部分支持 | ❌ 不支持 | **ExoPlayer** |
| **RTMP** | ❌ 不支持 | ✅ 支持 | ✅ **延迟 1-3秒** | **阿里云** ⭐ |
| **HLS** | ✅ 支持 | ✅ 支持 | ✅ 商业级优化 | 阿里云/ExoPlayer |
| **FLV** | ✅ 支持 | ✅ 支持 | ✅ 支持 | 阿里云/IJK |

**延迟对比：**
- **阿里云 RTMP**：1-3 秒（商业级优化）⭐
- **IJK RTMP**：3-5 秒
- **HLS**：10-30 秒
- **ExoPlayer RTSP**：1-5 秒

**推荐方案总结：**
- **RTSP 直播**：使用 ExoPlayer（完整支持，性能好）
- **RTMP 直播**：使用阿里云（延迟极低，商业级优化）
- **HLS 直播**：阿里云、ExoPlayer、IJK 均可
- **启用智能选择器**：自动根据协议选择最佳内核

**调试方法：**

查看详细日志：

```bash
adb logcat -s "IjkMediaPlayer:V" "IJKMEDIA:V" "*:E"
```

常见错误信息：
- `Protocol not found`：协议不支持
- `Connection refused`：网络连接被拒绝
- `Invalid data found`：数据格式错误

### Q9: ExoPlayer 播放 RTMP 切换全屏报错

**问题描述：**

使用 ExoPlayer 播放 RTMP 直播流时，切换全屏会报错：

```
E System  : java.lang.NullPointerException: Null reference used for synchronization (monitor-enter)
E System  :      at android.view.Surface.release(Surface.java:254)
E System  :      at android.view.Surface.finalize(Surface.java:242)
```

但播放 RTSP 流时不会出现此问题。

**原因分析：**

RTMP 直播流在全屏切换时，Surface 管理不当导致：
1. 旧 Surface 在新 Surface 设置前被过早释放
2. Surface.finalize() 被 GC 调用时，Surface 已经被释放，导致 NullPointerException
3. RTMP 流对 Surface 切换更敏感，RTSP 流相对稳定

**解决方案：**

**v1.0.4+ 版本已修复此问题**，修复内容：

1. **Surface 引用保持**：直播流在切换时保持旧 Surface 引用，避免过早释放
2. **延迟释放**：只有在新 Surface 成功设置后，才释放旧 Surface
3. **异常保护**：所有 Surface 操作都添加了 try-catch 保护

**如果仍然遇到问题，可以尝试：**

**方案一：使用阿里云播放器（推荐）**

阿里云播放器对 RTMP 支持最好，延迟极低（1-3秒）：

```java
PlayerSettingsManager.getInstance(context).setPlayerEngine(PlayerConstants.ENGINE_ALI);
```

**方案二：启用智能内核选择器**

自动为 RTMP 流选择最佳播放器：

```java
videoView.enableAutoEngineSelection(true);
```

**方案三：使用 IJK 播放器**

IJK 对 RTMP 支持较好（虽然可能有其他问题）：

```java
PlayerSettingsManager.getInstance(context).setPlayerEngine(PlayerConstants.ENGINE_IJK);
```

**播放器内核 RTMP 支持对比：**

| 播放器 | RTMP 支持 | 延迟 | 稳定性 | 推荐度 |
|--------|----------|------|--------|--------|
| 阿里云 | ✅ 完整 | 1-3秒 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| IJK | ✅ 支持 | 3-5秒 | ⭐⭐⭐ | ⭐⭐⭐ |
| ExoPlayer | ⚠️ 有限 | 2-4秒 | ⭐⭐ | ⭐⭐ |
| 系统播放器 | ❌ 不支持 | - | - | ❌ |

### Q10: 横竖屏切换后播放异常

**可能原因：**
1. Activity 配置不正确
2. 生命周期处理不当

**解决方案：**

**1. 配置 AndroidManifest.xml**

```xml
<activity
    android:name=".YourActivity"
    android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation|keyboardHidden"
    android:supportsPictureInPicture="true"
    android:resizeableActivity="true">
</activity>
```

**2. 处理配置变化**

```java
@Override
public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    videoView.onConfigurationChanged(this, newConfig, 
        mOrientationUtils, true, true);
}
```

**3. 处理返回键**

```java
@Override
public void onBackPressed() {
    if (videoView.isFullScreen()) {
        videoView.exitFullScreen();
        return;
    }
    super.onBackPressed();
}
```

### Q11: 本地 HTTP 代理播放失败（AndroidVideoCache）

**问题描述：**
使用本地 HTTP 代理（如 AndroidVideoCache）播放视频时失败，所有播放器引擎（IJK、ExoPlayer、系统播放器）都无法播放。日志显示：
```
curl: (8) Invalid Content-Length: value
E IJKMEDIA: Failed to open file 'http://127.0.0.1:xxxx/...' or configure filtergraph
```

**原因：** 本地 HTTP 代理服务器返回的 Content-Length 头无效或缺失

**解决方案：**

**方案一：升级视频缓存库（推荐）**

```gradle
dependencies {
    // 使用最新版本
    implementation 'com.danikula:videocache:2.7.1'
    
    // 或使用 GSYVideoPlayer 内置缓存
    implementation 'io.github.706412584:gsyVideoPlayer-proxy_cache:1.1.0'
}
```

**方案二：直接播放原始 URL**

```java
// 不使用本地代理
String originalUrl = "https://example.com/video.m3u8";
videoView.setUp(originalUrl, true, title);
```

**方案三：使用本地文件路径**

如果文件已完全下载：
```java
String localPath = "/storage/emulated/0/path/to/video.mp4";
videoView.setUp(localPath, true, title);
```

**诊断方法：**

```bash
# 测试代理服务器响应
adb shell "curl -I http://127.0.0.1:xxxx/path/to/file"

# 查看是否返回有效的 Content-Length
```

**技术细节：**
- 详见：[docs/fixes/local_http_proxy_content_length_fix.md](fixes/local_http_proxy_content_length_fix.md)
- 问题影响所有播放器引擎，不是播放器本身的问题
- 根本原因是 HTTP 代理服务器配置问题

### Q12: IJK 播放器无法播放本地文件（file:// 协议）

**问题描述：**
使用 IJK 播放器播放 `file://` 协议的本地文件时失败，日志显示：
```
E IJKMEDIA: Protocol 'file' not on whitelist
```

**原因：** IJK 播放器默认的协议白名单不包含 `file` 协议

**解决方案：**

OrangePlayer v1.0.9+ 已修复此问题，使用自定义的 `OrangeIjkPlayerManager` 自动添加 `file` 协议到白名单。

**如果使用旧版本，请升级到最新版：**

```gradle
dependencies {
    implementation 'com.github.706412584:orangeplayer:v1.0.9+'
}
```

**临时解决方案（旧版本）：**
切换到 ExoPlayer 或系统播放器播放本地文件：

```java
// 切换到 ExoPlayer
videoView.selectPlayerFactory(PlayerConstants.ENGINE_EXO);

// 或切换到系统播放器
videoView.selectPlayerFactory(PlayerConstants.ENGINE_DEFAULT);
```

**注意：** 如果你的 URL 是 `http://127.0.0.1:xxxx/...`，这不是 file:// 协议问题，请参考 Q11。

**技术细节：**
- 修复文件：`OrangeIjkPlayerManager.java`
- 添加的协议：`file,http,https,tls,rtp,tcp,udp,crypto,httpproxy,concat,subfile`
- 详见：[docs/fixes/ijk_local_file_fix.md](fixes/ijk_local_file_fix.md)

### Q13: M3U8 播放内容不完整或跳过部分内容

**问题描述：**
播放 M3U8 视频时，发现视频内容不完整，某些片段被跳过，或者播放时长比预期短。

**可能原因：**
M3U8 去广告功能误将正常内容识别为广告并移除。

**解决方案：**

**方案一：关闭 M3U8 去广告功能**

```java
// 在播放前关闭去广告功能
videoView.setM3U8AdRemovalEnabled(false);
videoView.setUp(m3u8Url, true, title);
videoView.startPlayLogic();
```

**方案二：检查日志确认广告检测**

```bash
# 查看广告检测日志
adb logcat -s M3U8AdRemover:D
```

日志示例：
```
D M3U8AdRemover: Ad detected by prefix length at position 120: e57566bf8e40715435.ts
D M3U8AdRemover: Total segments parsed: 442
D M3U8AdRemover: Ad segments removed: 7
```

如果发现正常片段被误识别为广告，请关闭去广告功能。

**方案三：清除缓存重新播放**

```java
// 清除 M3U8 缓存
M3U8AdManager.getInstance(context).clearCache(m3u8Url);
```

**M3U8 去广告检测机制：**

OrangePlayer 使用多种方式检测广告片段：
1. **路径模式检测**：识别不同来源的片段
2. **前缀长度检测**：识别文件名前缀长度异常的片段
3. **数字序列突变检测**：识别文件名数字跳跃过大的片段
4. **DISCONTINUITY 标记**：检测流切换点之间的短片段组
5. **短片段组检测**：检测异常短的连续片段

**何时关闭去广告功能：**
- 视频内容不完整
- 播放时长明显短于预期
- 日志显示大量片段被移除
- 视频包含多个不同来源的正常内容（如合集）

**API 说明：**

```java
// 启用/禁用 M3U8 去广告（默认启用）
videoView.setM3U8AdRemovalEnabled(true);   // 启用
videoView.setM3U8AdRemovalEnabled(false);  // 禁用

// 检查当前状态
boolean isEnabled = videoView.isM3U8AdRemovalEnabled();
```

---

## 功能问题

### Q14: 字幕不显示

**可能原因：**
1. 字幕文件格式不支持
2. 字幕编码问题
3. 字幕加载失败

**解决方案：**

**1. 检查字幕格式**

支持的格式：SRT、ASS/SSA、VTT

**2. 检查字幕编码**

字幕文件应使用 UTF-8 编码。

**3. 检查加载结果**

```java
SubtitleManager manager = videoView.getVideoController().getSubtitleManager();
manager.loadSubtitle(url, new SubtitleManager.OnSubtitleLoadListener() {
    @Override
    public void onLoadSuccess(int count) {
        Log.d(TAG, "字幕加载成功，共 " + count + " 条");
        manager.start();
    }
    
    @Override
    public void onLoadFailed(String error) {
        Log.e(TAG, "字幕加载失败：" + error);
    }
});
```

**4. 调整字幕大小**

```java
manager.setTextSize(18f);  // 18sp
```

### Q15: 弹幕不显示

**可能原因：**
1. 弹幕被隐藏
2. 弹幕透明度设置为 0
3. 弹幕速度过快

**解决方案：**

**1. 显示弹幕**

```java
IDanmakuController danmaku = videoView.getVideoController().getDanmakuController();
danmaku.show();
```

**2. 调整弹幕设置**

```java
PlayerSettingsManager settings = PlayerSettingsManager.getInstance(context);
settings.setDanmakuTextSize(16f);      // 字体大小
settings.setDanmakuSpeed(1.2f);        // 滚动速度
settings.setDanmakuAlpha(0.8f);        // 透明度（0.0-1.0）
```

**3. 发送测试弹幕**

```java
danmaku.sendDanmaku("测试弹幕", 0xFFFFFFFF);
```

### Q16: 语音识别无法启动

**可能原因：**
1. Android 版本低于 10
2. 缺少 Vosk SDK
3. 模型文件未正确放置
4. 权限未授予

**解决方案：**

**1. 检查 Android 版本**

```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    // Android 10+，可以使用语音识别
} else {
    Toast.makeText(this, "语音识别需要 Android 10 或更高版本", 
        Toast.LENGTH_SHORT).show();
}
```

**2. 添加依赖**

```gradle
dependencies {
    implementation 'com.alphacephei:vosk-android:0.3.47'
}
```

**3. 下载并放置模型文件**

下载模型：https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip

放置位置：
```
app/src/main/assets/
└── vosk-model-small-cn/
    ├── am/
    ├── conf/
    ├── graph/
    └── ...
```

**4. 授予权限**

首次使用时会弹出系统权限对话框，选择"此应用"并允许。

详见 [语音识别指南](SPEECH_RECOGNITION.md)。

### Q17: OCR 识别不准确

**可能原因：**
1. 字幕太小或太模糊
2. 识别区域设置不正确
3. 选择的语言不正确
4. 背景干扰

**解决方案：**

**1. 调整识别区域**

只包含字幕部分，排除背景干扰。

**2. 选择正确的源语言**

确保选择的语言与字幕语言一致。

**3. 使用高清视频源**

清晰的字幕识别率更高。

**4. 暂停视频进行识别**

暂停后识别准确率更高。

详见 [OCR 功能指南](OCR_GUIDE.md)。

---

## 性能问题

### Q18: 应用内存占用过高

**可能原因：**
1. 视频分辨率过高
2. 缓存设置过大
3. 语言包/模型占用内存
4. 内存泄漏

**解决方案：**

**1. 降低视频分辨率**

选择较低分辨率的视频源。

**2. 及时释放资源**

```java
@Override
protected void onDestroy() {
    super.onDestroy();
    videoView.release();
}
```

**3. 清理缓存**

```java
// 清理视频缓存
GSYVideoManager.instance().clearAllDefaultCache(context);
```

**4. 卸载不用的语言包**

```java
LanguagePackManager manager = new LanguagePackManager(context);
manager.deleteLanguage("unused_lang");
```

### Q19: 应用启动慢

**可能原因：**
1. 语言包/模型加载慢
2. 初始化操作过多
3. 主线程阻塞

**解决方案：**

**1. 延迟加载语言包**

不要在启动时加载所有语言包，按需加载。

**2. 异步初始化**

```java
new Thread(() -> {
    // 初始化操作
    initPlayer();
}).start();
```

**3. 使用懒加载**

只在需要时才初始化功能模块。

### Q20: 播放时 CPU 占用高

**可能原因：**
1. 使用软件解码
2. 视频码率过高
3. OCR/语音识别占用 CPU
4. 弹幕渲染占用 CPU

**解决方案：**

**1. 启用硬件解码**

```java
PlayerSettingsManager settings = PlayerSettingsManager.getInstance(context);
settings.setHardwareDecode(true);
```

**2. 关闭不需要的功能**

```java
// 关闭 OCR
eventManager.stopOcrTranslate();

// 关闭语音识别
eventManager.stopSpeechTranslate();

// 隐藏弹幕
danmaku.hide();
```

**3. 降低视频分辨率**

选择较低分辨率的视频源。

---

## 其他问题

### Q21: 如何自定义 UI

**方案一：修改主题颜色**

在 `styles.xml` 中定义主题颜色：

```xml
<style name="AppTheme" parent="Theme.AppCompat.Light.DarkActionBar">
    <item name="colorPrimary">@color/colorPrimary</item>
    <item name="colorPrimaryDark">@color/colorPrimaryDark</item>
    <item name="colorAccent">@color/colorAccent</item>
</style>
```

**方案二：自定义控制器**

```java
OrangeVideoController controller = new OrangeVideoController(context);
// 自定义控制器...
videoView.setVideoController(controller);
```

**方案三：自定义组件**

继承现有组件并重写方法：

```java
public class MyVodControlView extends VodControlView {
    public MyVodControlView(Context context) {
        super(context);
    }
    
    @Override
    protected void initView() {
        super.initView();
        // 自定义 UI
    }
}
```

### Q22: 如何保存播放进度

OrangePlayer 自动保存播放进度，无需手动处理。

**查看播放历史：**

```java
import com.orange.playerlibrary.history.PlayHistoryManager;

PlayHistoryManager manager = PlayHistoryManager.getInstance(context);
long position = manager.getHistory(videoUrl);
if (position > 0) {
    videoView.seekTo(position);
}
```

**清除播放历史：**

```java
manager.deleteHistory(videoUrl);
```

### Q23: 如何实现倍速播放

**设置倍速：**

```java
videoView.setSpeed(1.5f);  // 1.5 倍速
```

**倍速限制：**
- IJK 内核：最高 2.0x
- 其他内核：最高 5.0x

**长按倍速：**

```java
PlayerSettingsManager settings = PlayerSettingsManager.getInstance(context);
settings.setLongPressSpeed(2.0f);  // 长按 2 倍速
```

### Q24: 如何实现画中画

**进入画中画：**

```java
import android.app.PictureInPictureParams;
import android.util.Rational;

if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    Rational aspectRatio = new Rational(16, 9);
    PictureInPictureParams params = new PictureInPictureParams.Builder()
        .setAspectRatio(aspectRatio)
        .build();
    enterPictureInPictureMode(params);
}
```

**处理画中画状态变化：**

```java
@Override
public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, 
                                          Configuration newConfig) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
    
    if (isInPictureInPictureMode) {
        videoView.getVideoController().hide();
    } else {
        videoView.getVideoController().show();
    }
}
```

**配置 AndroidManifest.xml：**

```xml
<activity
    android:name=".YourActivity"
    android:supportsPictureInPicture="true"
    android:resizeableActivity="true">
</activity>
```

---

## 获取帮助

如果以上方案无法解决你的问题，可以通过以下方式获取帮助：

1. **查看文档**
   - [安装指南](INSTALLATION.md)
   - [API 文档](API.md)
   - [OCR 功能指南](OCR_GUIDE.md)
   - [语音识别指南](SPEECH_RECOGNITION.md)

2. **提交 Issue**
   - GitHub Issues: https://github.com/706412584/orangeplayer/issues
   - 请提供详细的错误信息和复现步骤

3. **联系作者**
   - QQ: 706412584

4. **查看示例代码**
   - Demo 应用：https://github.com/706412584/orangeplayer/tree/main/app


### Q25: 支持的最低 Android 版本是多少？

**当前配置：**
- 播放器库 (palyerlibrary): minSdk 21 (Android 5.0)
- Demo 应用 (app): minSdk 23 (Android 6.0)

**推荐配置：**
```gradle
android {
    defaultConfig {
        minSdk 21  // Android 5.0 Lollipop
        targetSdk 36
    }
}
```

**为什么不支持 Android 4.0？**

1. **播放器内核限制**
   - GSYVideoPlayer: 需要 API 16+
   - IJK 播放器: 需要 API 16+
   - ExoPlayer: 需要 API 21+
   - 系统播放器: 支持 API 1+（功能有限）

2. **AI 功能限制**
   - OCR (Tesseract): 需要 API 21+
   - 语音识别 (Vosk): 需要 API 21+
   - ML Kit 翻译: 需要 API 19+

3. **市场占有率**
   - Android 4.0: < 0.1%
   - Android 5.0+: 99%+

4. **Google Play 要求**
   - 2021 年起要求 minSdk 21+

**如果必须支持 Android 4.0：**

只能使用系统播放器，功能严重受限：
- ❌ 无法使用 IJK/ExoPlayer
- ❌ 无 OCR 字幕识别
- ❌ 无语音识别
- ❌ 无画中画
- ✅ 可以使用系统播放器
- ✅ 可以使用弹幕

**详细分析：**
- 查看 [Android 4.0 兼容性分析](ANDROID_4_COMPATIBILITY.md)
- 运行检查脚本：`scripts\check_min_sdk.bat`

**版本覆盖率 (2024)：**

| Android 版本 | API Level | 市场占有率 |
|-------------|-----------|-----------|
| 4.0-4.0.4 | 14-15 | < 0.1% |
| 4.1-4.3 | 16-18 | < 0.5% |
| 4.4 | 19 | ~0.5% |
| 5.0-5.1 | 21-22 | ~2% |
| 6.0+ | 23+ | ~97% |

**结论：** 保持 minSdk 21，覆盖 99%+ 设备，支持所有现代功能。

### Q26: 可以降低到 Android 4.4 (API 19) 吗？

**可以，但需要调整：**

```gradle
android {
    defaultConfig {
        minSdk 19  // Android 4.4 KitKat
    }
}
```

**需要移除的功能：**
- ❌ ExoPlayer 播放器（需要 API 21+）
- ❌ 阿里云播放器（需要 API 21+）
- ❌ OCR 字幕识别（需要 API 21+）
- ❌ 语音识别（需要 API 29+）
- ❌ FFmpeg 解码器（需要 API 21+）
- ❌ SurfaceControl 无缝切换（需要 API 29+）

**可以保留的功能：**
- ✅ IJK 播放器（支持 API 16+）
- ✅ 系统播放器（支持所有版本）
- ✅ 弹幕（支持 API 14+）
- ✅ 基础播放控制
- ✅ 字幕显示
- ✅ 倍速播放
- ✅ DLNA 投屏

**快速配置：**

1. 运行配置脚本：
```bash
scripts\configure_android_44.bat
```

2. 手动移除不兼容的依赖（脚本会提示）

3. 在代码中添加版本检查：
```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
    // 使用 ExoPlayer
} else {
    // 降级到 IJK 或系统播放器
}
```

**详细指南：**
- 查看 [Android 4.4 支持指南](ANDROID_4.4_SUPPORT.md)
- 包含完整的配置步骤和代码示例

**覆盖率：** 99.5%+ 设备（相比 minSdk 21 的 99%）

**权衡考虑：**
- 仅增加 0.5% 覆盖率
- 功能受限
- 维护成本增加
- 需要充分测试

**推荐：** 
- 一般应用：保持 minSdk 21
- 企业/教育应用：可以考虑 minSdk 19
- 特殊需求：可以发布两个版本（标准版和兼容版）
