# RTSP 推流性能优化指南

## 问题：添加时间戳后速度变慢

### 原因分析

添加时间戳水印需要 FFmpeg 进行额外的视频处理：
1. **解码**：读取原始视频帧
2. **绘制文字**：在每一帧上叠加时间戳（CPU 密集）
3. **重新编码**：将处理后的帧编码为 H.264

这个过程会大幅增加 CPU 负担，导致：
- 推流速度从 200KB/s 降到 20KB/s
- CPU 使用率飙升
- 可能出现丢帧

## 解决方案

### 方案 1：使用硬件加速（推荐）⭐

**脚本**：`push_rtsp_with_timestamp_fast.bat`

**优势**：
- 使用 GPU 编码，速度快 5-10 倍
- CPU 占用率低
- 画质更好

**要求**：
- NVIDIA 显卡（GTX 600 系列及以上）
- AMD 显卡（需要修改为 h264_amf）
- Intel 核显（需要修改为 h264_qsv）

**使用方法**：
```bash
# 拖拽视频到这个脚本
push_rtsp_with_timestamp_fast.bat
```

**如何检查是否有 GPU 加速**：
```bash
# 运行 FFmpeg 查看支持的编码器
ffmpeg -encoders | findstr h264

# 输出示例：
# h264_nvenc    - NVIDIA GPU 加速
# h264_amf      - AMD GPU 加速
# h264_qsv      - Intel 核显加速
```

### 方案 2：优化软件编码

**脚本**：`push_rtsp_with_timestamp.bat`（已优化）

**优化措施**：
1. 降低分辨率：854x480 → 640x360
2. 降低码率：1M → 800k
3. 减小字体：32 → 20
4. 使用 veryfast preset

**效果**：
- 速度提升约 30-50%
- 延迟略有增加（可接受）

### 方案 3：不使用时间戳

**脚本**：`push_rtsp_stream.bat`

**优势**：
- 速度最快
- CPU 占用最低
- 无额外处理

**缺点**：
- 无法直接测量延迟
- 需要使用秒表等其他方法

## 性能对比

| 方案 | 速度 | CPU 占用 | 延迟 | 画质 |
|------|------|----------|------|------|
| 硬件加速 + 时间戳 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 低 | 高 |
| 优化软件编码 + 时间戳 | ⭐⭐⭐ | ⭐⭐⭐ | 中 | 中 |
| 无时间戳 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 低 | 高 |
| 原始软件编码 + 时间戳 | ⭐ | ⭐ | 高 | 低 |

## 推荐配置

### 配置 1：高性能（有 GPU）

```bash
# 使用硬件加速
push_rtsp_with_timestamp_fast.bat

# 参数：
# - 分辨率: 854x480
# - 码率: 1M
# - 编码器: h264_nvenc (GPU)
# - 预期速度: 200-300 KB/s
```

### 配置 2：平衡（无 GPU）

```bash
# 使用优化的软件编码
push_rtsp_with_timestamp.bat

# 参数：
# - 分辨率: 640x360
# - 码率: 800k
# - 编码器: libx264 (CPU)
# - 预期速度: 80-120 KB/s
```

### 配置 3：最快（不测延迟）

```bash
# 不使用时间戳
push_rtsp_stream.bat

# 参数：
# - 分辨率: 854x480
# - 码率: 1M
# - 编码器: libx264 (CPU)
# - 预期速度: 150-200 KB/s
```

## 进一步优化

### 1. 降低分辨率

编辑脚本，修改 `scale` 参数：

```bash
# 从 640x360 降到 480x270
-vf "scale=480:270,drawtext=..."
```

### 2. 降低码率

```bash
# 从 800k 降到 500k
-b:v 500k -maxrate 500k -bufsize 1000k
```

### 3. 使用更快的 preset

```bash
# 从 veryfast 改为 superfast
-preset superfast
```

### 4. 减少 GOP 大小（降低延迟）

```bash
# 添加 -g 参数
-g 30  # 每 30 帧一个关键帧
```

## 故障排查

### 问题 1：硬件加速失败

**错误信息**：
```
Error initializing output stream 0:0 -- Error while opening encoder for output stream
```

**解决方案**：
1. 检查是否有 NVIDIA 显卡
2. 更新显卡驱动
3. 使用软件编码作为备选

### 问题 2：速度仍然很慢

**可能原因**：
- CPU 性能不足
- 源视频分辨率太高（4K）
- 其他程序占用 CPU

**解决方案**：
1. 关闭其他程序
2. 降低分辨率到 480p 或更低
3. 降低码率到 500k
4. 使用 superfast preset

### 问题 3：画面卡顿

**可能原因**：
- 编码速度跟不上实时播放
- 网络带宽不足

**解决方案**：
1. 使用硬件加速
2. 降低分辨率和码率
3. 检查网络连接

## 测试命令

### 测试 CPU 编码速度

```bash
ffmpeg -i video.mp4 -c:v libx264 -preset veryfast -f null -
```

### 测试 GPU 编码速度

```bash
ffmpeg -i video.mp4 -c:v h264_nvenc -preset p1 -f null -
```

### 查看实时编码速度

推流时观察 FFmpeg 输出：
```
frame= 1234 fps= 30 q=28.0 size=   12345kB time=00:00:41.13 bitrate=2456.7kbits/s speed=1.00x
```

- `fps=30`：编码帧率
- `speed=1.00x`：实时倍速（1.0x = 实时，<1.0x = 太慢）

## 总结

1. **有 GPU**：使用 `push_rtsp_with_timestamp_fast.bat`
2. **无 GPU**：使用 `push_rtsp_with_timestamp.bat`（已优化）
3. **不测延迟**：使用 `push_rtsp_stream.bat`（最快）

选择合适的方案，确保 `speed=1.0x` 或更高，即可流畅推流。
