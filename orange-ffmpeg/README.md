# orange-ffmpeg

## 简介

`orange-ffmpeg` 提供精简版 FFmpeg 能力，用于 M3U8 合并、加密流解复用与 mp4 封装。该模块通过 JNI 对外提供 `FFmpegKit` 接口，主要服务于 `orange-downloader` 的合并流程，同时保留自定义 exec 能力用于高级场景。

## 项目结构

### 树状图

```
orange-ffmpeg/
├── src/
│   ├── main/
│   │   ├── java/com/orange/ffmpeg/       # Java API 封装
│   │   ├── cpp/                          # JNI 与 Native 合并逻辑
│   │   │   └── third_party/ffmpeg/       # FFmpeg 静态库与头文件
│   │   └── AndroidManifest.xml
│   └── androidTest/                      # Native 集成测试
├── build.gradle
├── consumer-rules.pro
└── proguard-rules.pro
```

### 目录职责与依赖关系

- `src/main/java/com/orange/ffmpeg`
  - `FFmpegKit`：统一 exec 接口与错误码
- `src/main/cpp`
  - JNI 层与 remux 合并逻辑
  - 依赖 `third_party/ffmpeg` 静态库
- `third_party/ffmpeg`
  - 由 `scripts/build_ffmpeg.sh` 产出
- `src/androidTest`
  - JNI 合并集成测试入口
- `lib/`、`bin/`、`docs/`
  - 模块内未使用，文档统一在仓库 `docs/`

## 快速开始

### 环境变量

```bash
ANDROID_HOME=/path/to/Android/Sdk
ANDROID_NDK_HOME=/path/to/Android/Sdk/ndk/<version>
JAVA_HOME=/path/to/JDK17
```

### 最小可运行示例

```bash
./gradlew :app:installDebug
```

### 一行命令（构建 FFmpeg）

```bash
bash scripts/build_ffmpeg.sh
```

### Windows 验证

```powershell
.\gradlew :app:installDebug
adb shell am start -n com.orange.player/.MainActivity
```

预期输出（示例）：

```
Starting: Intent { cmp=com.orange.player/.MainActivity }
```

### macOS/Linux 验证

```bash
./gradlew :app:installDebug
adb shell am start -n com.orange.player/.MainActivity
```

预期输出（示例）：

```
Starting: Intent { cmp=com.orange.player/.MainActivity }
```

### 预期截图占位

- Windows：`docs/assets/ffmpeg-windows.png`
- macOS：`docs/assets/ffmpeg-macos.png`
- Linux：`docs/assets/ffmpeg-linux.png`

## 目前集成的 FFmpeg 功能清单

当前模块仅封装 m3u8 合并与加密流解复用。其他能力可通过 exec 自行调用。

| 功能 | 封装状态 | 输入格式 | 输出格式 | 关键参数 | 1080p/30fps 基准 | CPU 占用 |
| --- | --- | --- | --- | --- | --- | --- |
| m3u8 合并（copy） | 已封装 | m3u8(ts) | mp4 | `-i` `-c copy` | 待测 | 待测 |
| 加密 m3u8 解复用 | 已封装 | m3u8(aes-128) | mp4 | `-protocol_whitelist` | 待测 | 待测 |
| 转码（transcode） | 未封装 | mp4/mkv | mp4 | `-c:v` `-c:a` | 待测 | 待测 |
| 裁剪（trim） | 已封装 | mp4 | mp4 | `-ss` `-to` | 待测 | 待测 |
| 拼接（concat） | 已封装 | mp4/ts | mp4 | `concat` demuxer | 待测 | 待测 |
| 水印（watermark） | 已封装 | mp4 | mp4 | `overlay` | 待测 | 待测 |
| 缩略图（thumbnail） | 已封装 | mp4 | jpg/png | `-vf` `-frames:v` | 待测 | 待测 |
| 音视频分离 | 已封装 | mp4 | aac/mp3 | `-map` | 待测 | 待测 |

说明：

- 性能基准需在统一环境采集，建议 8 核 CPU、16G RAM
- 建议在 CI 中加入基准脚本，避免手工误差

## 自定义 exec 命令接口

本模块已开放 exec 接口，可直接调用：

### 接口签名

```java
public static int init();
public static int execute(String[] args);
public static void executeAsync(String[] args, FFmpegCallback callback);
public static void cancel();
public static String getVersion();
```

### 参数校验规则

- `init()` 必须先调用
- `execute()` 参数为空或长度为 0 返回失败码
- `executeAsync()` 启动新线程执行
- `cancel()` 仅设置取消标记并通知 native

### 错误码定义

```
RESULT_OK = 0
RESULT_NOT_INITIALIZED = -1
RESULT_LIBRARY_LOAD_FAILED = -2
RESULT_EXECUTE_FAILED = -3
RESULT_CANCELLED = -4
```

### 同步示例

```java
int init = FFmpegKit.init();
int result = FFmpegKit.execute(new String[] {
    "-i", inputM3U8,
    "-c", "copy",
    outputMp4
});
```

### 异步示例

```java
FFmpegKit.executeAsync(new String[] {
    "-i", inputM3U8,
    "-c", "copy",
    outputMp4
}, (code, msg) -> Log.d("FFmpegKit", "code=" + code + ", msg=" + msg));
```

## 便捷接口

```java
FFmpegKit.trim(inputMp4, outputMp4, "00:00:05", "00:00:20");
FFmpegKit.concat(concatListFile, outputMp4);
FFmpegKit.watermark(inputMp4, watermarkPng, outputMp4, 24, 24);
FFmpegKit.thumbnail(inputMp4, outputImage, "00:00:03");
FFmpegKit.extractAudio(inputMp4, outputAudio, true);
```

## Roadmap（Q3–Q4）

### 2026 Q3

- GPU 硬编解码
  - 目标场景：移动端高分辨率快速转封装
  - 技术选型：MediaCodec + FFmpeg hwaccel
  - 里程碑：2026-07-30 实验性 API
  - API 变动：新增 `HardwareTranscodeOptions`
  - 兼容策略：默认关闭，需显式启用
- 分布式切片
  - 目标场景：多任务并行切片与合并
  - 技术选型：多进程队列 + 本地缓存索引
  - 里程碑：2026-08-20 内测
  - API 变动：新增 `DistributedSliceManager`
  - 兼容策略：旧接口不变
- 轻量能力扩展（低体积）
  - 目标场景：播放前预处理与统计输出
  - 技术选型：demux/mux + metadata + bsf
  - 里程碑：2026-09-10
  - API 变动：新增 `MediaProbe` 与 `RemuxOptions`
  - 兼容策略：新增接口，不影响现有合并路径

### 2026 Q4

- WebAssembly 浏览器端
  - 目标场景：前端预处理与预览
  - 技术选型：Emscripten + WASM FFmpeg
  - 里程碑：2026-10-30 Demo
  - API 变动：新增 Web 端独立包
  - 兼容策略：与 Android 版本并行维护
- AI 超分
  - 目标场景：低清内容增强
  - 技术选型：ONNX Runtime + SR model
  - 里程碑：2026-11-15 实验性支持
  - API 变动：新增 `SuperResolutionOptions`
  - 兼容策略：默认关闭，需明确调用
- HDR10/杜比视界
  - 目标场景：高端显示设备播放
  - 技术选型：HDR metadata passthrough
  - 里程碑：2026-12-20 试验版
  - API 变动：新增 HDR capability 查询
  - 兼容策略：不破坏现有 SDR 流程
- 轻量能力完善（低体积）
  - 目标场景：缩略图、裁剪、轨道导出
  - 技术选型：image2 muxer + scale/trim + stream copy
  - 里程碑：2026-12-25
  - API 变动：新增 `ThumbnailOptions` 与 `TrackExportOptions`
  - 兼容策略：仅新增选项，不影响现有接口
