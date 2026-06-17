# RTSP 直播推流测试指南

## 测量直播延迟

### 性能说明 ⚠️

添加时间戳水印会增加 CPU 负担，可能导致推流速度变慢。有以下解决方案：

**方案 1：使用硬件加速（推荐）**
```bash
push_rtsp_with_timestamp_fast.bat  # 使用 GPU 加速，速度快 5-10 倍
```

**方案 2：使用优化版时间戳**
```bash
push_rtsp_with_timestamp.bat  # 已优化，降低分辨率和码率
```

**方案 3：不使用时间戳**
```bash
push_rtsp_stream.bat  # 无时间戳，速度最快
```

### 方法 1：使用时间戳水印（推荐）

#### 步骤 1：使用带时间戳的推流脚本

运行 `push_rtsp_with_timestamp.bat` 而不是普通的推流脚本：

```bash
# 拖拽视频文件到 push_rtsp_with_timestamp.bat
```

这个脚本会在视频左上角添加当前时间戳（精确到毫秒），格式如：`14:30:25.500`

#### 步骤 2：在 Android 上播放

在 MainActivity 中点击"延迟测试"按钮，或使用代码：

```java
String rtspUrl = "rtsp://192.168.1.10:8554/live";
mVideoView.selectPlayerFactory("exo");
mVideoView.setUp(rtspUrl, true, "RTSP 延迟测试");
mVideoView.startPlayLogic();
```

#### 步骤 3：计算延迟

对比视频中显示的时间戳与手机当前时间：

```
示例：
- 视频显示时间: 14:30:25.500
- 手机当前时间: 14:30:27.800
- 延迟 = 2800 - 500 = 2300 ms (2.3秒)
```

### 方法 2：使用秒表

1. 在电脑上准备一个秒表或计时器
2. 同时启动电脑秒表和手机播放
3. 对比两边的时间差

### 典型延迟值参考

- **局域网 RTSP**：500ms - 2000ms（0.5-2秒）
- **局域网 HLS**：2000ms - 6000ms（2-6秒）
- **公网 HLS**：5000ms - 15000ms（5-15秒）

### 影响延迟的因素

1. **推流参数**：
   - `-tune zerolatency`：降低延迟
   - `-preset ultrafast`：快速编码
   - 缓冲区大小：`-bufsize` 越小延迟越低

2. **网络质量**：
   - WiFi 信号强度
   - 网络带宽
   - 丢包率

3. **播放器缓冲**：
   - ExoPlayer 默认有 2-3 秒缓冲
   - 可以通过配置减少缓冲时间

### 优化延迟的方法

#### 方法 1：调整 FFmpeg 参数

编辑 `push_rtsp_with_timestamp.bat`：

```bash
# 降低延迟的配置
-preset ultrafast -tune zerolatency ^
-b:v 1M -maxrate 1M -bufsize 500K ^  # 减小缓冲区
-g 30 ^  # 减小 GOP 大小
```

#### 方法 2：使用 UDP 代替 TCP

RTSP 可以使用 UDP 传输，延迟更低：

```bash
# 在推流 URL 后添加参数
rtsp://192.168.1.10:8554/live?tcp=0
```

#### 方法 3：减少分辨率和码率

```bash
-vf "scale=640:360" ^  # 降低到 360p
-b:v 500k ^  # 降低码率
```

## 快速开始

### 码率选择指南

根据你的网络环境选择合适的推流脚本：

| 脚本 | 码率 | 网速 | 分辨率 | 适用场景 |
|------|------|------|--------|----------|
| `push_rtsp_stream.bat` | 1 Mbps | 125 KB/s | 480p | WiFi，低延迟 |
| `push_rtsp_high_quality.bat` | 8 Mbps | 1 MB/s | 1080p | 千兆 WiFi |
| `push_rtsp_ultra_quality.bat` | 20 Mbps | 2.5 MB/s | 1080p | 千兆有线 |
| `push_rtsp_menu.bat` | 自选 | 自选 | 自选 | 交互式选择 |

### 步骤 1：启动 RTSP 服务器

双击运行 `start_rtsp_server.bat`

等待看到类似以下提示：
```
2026/01/08 22:00:00 INF rtsp server is ready
```

### 步骤 2：开始推流

**方法 A：拖拽视频文件**
1. 找到你的视频文件（如 `2759477-uhd_3840_2160_30fps.mp4`）
2. 直接拖拽到 `push_rtsp_stream.bat` 文件上
3. 自动开始推流

**方法 B：手动运行**
1. 双击 `push_rtsp_stream.bat`
2. 将视频文件拖拽到命令行窗口
3. 按回车开始推流

### 步骤 3：在 Android 上播放

#### 获取电脑 IP 地址

在命令行运行：
```bash
ipconfig
```

找到 `IPv4 地址`，例如：`192.168.1.100`

#### Android 代码

```java
// 在 MainActivity 中添加
Button btnRtspTest = findViewById(R.id.btn_rtsp_test);
btnRtspTest.setOnClickListener(v -> {
    // 替换为你的电脑 IP
    String rtspUrl = "rtsp://192.168.1.100:8554/live";
    
    // RTSP 需要使用 ExoPlayer
    mVideoView.selectPlayerFactory("exo");
    
    mVideoView.setUp(rtspUrl, true, "RTSP 直播测试");
    mVideoView.startPlayLogic();
    
    appendLog("播放 RTSP 流: " + rtspUrl);
});
```

## 已知问题

### 1. 全屏切换时播放中断 ⚠️

**状态**：已知问题，暂无完美解决方案

**原因**：
- ExoPlayer 在全屏切换时需要重新创建 Surface
- 直播流（RTSP/HLS）没有缓冲，Surface 销毁会导致连接中断
- 与点播视频不同，直播流无法从当前位置恢复

**临时解决方案**：
1. **退出全屏后重新播放**：点击播放按钮重新连接直播流
2. **使用 HLS 流**：HLS 协议对全屏切换的支持相对更好
3. **避免全屏切换**：在小窗模式下观看直播
4. **使用竖屏全屏**：点击"竖屏全屏"按钮，避免横竖屏切换

**技术说明**：
- Android Q+ 的 SurfaceControl.reparent() 可以实现无缝切换
- 但直播流的特殊性（无缓冲、实时性）导致切换时仍会中断
- 这是 ExoPlayer + 直播流的已知限制，不是播放器 bug

**未来改进方向**：
- 探索使用 TextureView 代替 SurfaceView（牺牲性能换取稳定性）
- 实现全屏切换时的自动重连机制
- 添加"锁定竖屏全屏"模式，避免横竖屏切换

### 2. 播放卡顿

**原因**：
- WiFi 信号不稳定
- 推流码率过高
- 视频源分辨率过高（4K）

**解决方案**：

#### 方案 A：降低推流码率（推荐）

编辑 `push_rtsp_stream.bat`，将：
```bash
-b:v 2M -maxrate 2M -bufsize 4M
```

改为：
```bash
-b:v 1M -maxrate 1M -bufsize 2M
```

#### 方案 B：降低分辨率

编辑 `push_rtsp_stream.bat`，将：
```bash
-vf "scale=1280:720"
```

改为：
```bash
-vf "scale=854:480"
```

#### 方案 C：使用有线网络

- 电脑连接网线
- 手机使用 5GHz WiFi

## 支持的视频格式

- MP4
- AVI
- MKV
- MOV
- FLV
- WebM
- 等所有 FFmpeg 支持的格式

## 推流参数说明

- **分辨率**：自动缩放到 1280x720（减少带宽）
- **码率**：2Mbps（适合局域网）
- **编码**：H.264 (ultrafast preset)
- **音频**：AAC 128kbps
- **循环播放**：视频结束后自动重新开始

## 故障排查

### 问题 1：推流失败

**原因**：RTSP 服务器未启动

**解决**：先运行 `start_rtsp_server.bat`

### 问题 2：Android 无法播放

**原因**：IP 地址错误或防火墙阻止

**解决**：
1. 确认电脑和手机在同一局域网
2. 关闭 Windows 防火墙或添加端口 8554 例外
3. 使用 `ipconfig` 确认正确的 IP 地址
4. **不要使用 `localhost`，必须使用电脑的局域网 IP**

### 问题 3：播放卡顿

**原因**：网络带宽不足或视频码率过高

**解决**：
- 降低推流码率（修改脚本中的 `-b:v 2M` 为 `-b:v 1M`）
- 使用有线网络代替 WiFi

## 高级配置

### 修改推流分辨率

编辑 `push_rtsp_stream.bat`，修改：
```bash
-vf "scale=1280:720"
```

改为：
```bash
-vf "scale=1920:1080"  # 1080p
-vf "scale=854:480"    # 480p
```

### 修改码率

编辑 `push_rtsp_stream.bat`，修改：
```bash
-b:v 2M -maxrate 2M -bufsize 4M
```

### 不循环播放

删除脚本中的 `-stream_loop -1` 参数

## 测试用的公开直播流

如果不想自己推流，可以直接使用这些测试流：

```java
// HLS 直播流（推荐，支持全屏切换）
String hlsUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8";

// Apple 测试流
String appleUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8";
```

## 相关文件

- `start_rtsp_server.bat` - 启动 RTSP 服务器
- `push_rtsp_stream.bat` - 推流脚本
- `rtsp_test_guide.bat` - 查看完整指南
