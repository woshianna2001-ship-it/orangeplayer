# orange-downloader

## 简介

`orange-downloader` 是 OrangePlayer 的独立下载模块，覆盖 M3U8 下载、分片管理、合并、缓存与下载管理 UI。该模块默认联动 `orange-ffmpeg` 处理加密 m3u8 的合并需求，非加密任务在未集成 `orange-ffmpeg` 时可回退到 Java 合并。

## 项目结构

### 树状图

```
orange-downloader/
├── src/
│   ├── main/
│   │   ├── java/com/orange/downloader/   # 下载核心逻辑
│   │   │   ├── merge/                    # 合并与重试逻辑
│   │   │   ├── task/                     # 下载任务与分片下载
│   │   │   ├── ui/                       # 下载列表 UI
│   │   │   ├── m3u8/                     # M3U8 解析与模型
│   │   │   └── utils/                    # 下载工具与线程调度
│   │   ├── res/                          # 下载 UI 资源
│   │   └── AndroidManifest.xml
│   └── test/                             # 模块单元测试（如有）
├── build.gradle
├── consumer-rules.pro
└── proguard-rules.pro
```

### 目录职责与依赖关系

- `src/main/java/com/orange/downloader`
  - `VideoDownloadManager`：下载任务总入口
  - `task/`：分片下载与任务调度
  - `merge/`：M3U8 合并与容错重试
  - `ui/`：下载列表与弹窗 UI
  - 依赖：`orange-ffmpeg`（反射调用）
- `src/main/res`
  - 下载列表与操作面板资源
  - 与 `palyerlibrary` UI 不共享，避免冲突
- `src/test`
  - 若无测试文件则为空目录
- `lib/`、`bin/`、`docs/`
  - 模块内未使用，文档统一在仓库 `docs/`

## 快速开始

### 环境变量

```bash
ANDROID_HOME=/path/to/Android/Sdk
JAVA_HOME=/path/to/JDK17
```

### 最小可运行示例

```bash
./gradlew :app:installDebug
```

### 代码示例

```java
SimpleDownloadManager manager = SimpleDownloadManager.getInstance(context);
manager.startDownload(url, title);
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

- Windows：`docs/assets/downloader-windows.png`
- macOS：`docs/assets/downloader-macos.png`
- Linux：`docs/assets/downloader-linux.png`

## 目前集成的 FFmpeg 功能清单

本模块通过 `orange-ffmpeg` 完成加密 m3u8 的合并。其它功能尚未在本模块封装。

| 功能 | 封装状态 | 输入格式 | 输出格式 | 关键参数 | 1080p/30fps 基准 | CPU 占用 |
| --- | --- | --- | --- | --- | --- | --- |
| m3u8 合并（copy） | 已封装 | m3u8(ts) | mp4 | `-i` `-c copy` | 待测 | 待测 |
| 加密 m3u8 解复用 | 已封装 | m3u8(aes-128) | mp4 | `-protocol_whitelist` | 待测 | 待测 |
|

说明：

- 性能基准需在统一环境采集，建议 8 核 CPU、16G RAM
- 若需要标准化基准，可在 CI 增加基准脚本采集

## 自定义 exec 命令接口

`orange-downloader` 本身不直接暴露 exec 接口，执行由 `orange-ffmpeg` 提供。

### 原因

- 下载模块的职责是任务管理与合并流程，不直接承载通用编解码命令
- 需要避免在下载层引入不必要的 FFmpeg API 面

### 替代方案

- 使用 `orange-ffmpeg` 的 `FFmpegKit.execute(...)`
- 通过 PR 扩展 `orange-downloader` 合并策略或新增插件式入口

## Roadmap

本模块依赖 `orange-ffmpeg`，Roadmap 以 `orange-ffmpeg` 为主，详见 `orange-ffmpeg/README.md`。

