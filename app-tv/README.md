# OrangePlayer TV Demo

Android TV 版本的 OrangePlayer 演示应用。

## 功能特性

- ✅ Leanback 界面设计
- ✅ 遥控器完整支持
- ✅ 焦点导航和动画
- ✅ 自动隐藏控制栏
- ✅ ExoPlayer 播放内核
- ✅ 示例视频列表

## 系统要求

- **Android 5.0+ (API 21+)** - TV 推荐版本
- **Android TV 设备** 或 **Android TV 模拟器**

## 编译和运行

### 1. 使用 Android Studio

1. 打开项目根目录
2. 选择 `app-tv` 模块
3. 点击 Run 按钮

### 2. 使用命令行

```bash
# 编译 Debug 版本
./gradlew :app-tv:assembleDebug

# 安装到设备
./gradlew :app-tv:installDebug

# 或者直接运行
adb install app-tv/build/outputs/apk/debug/app-tv-debug.apk
```

## 测试方法

### 使用 Android TV 模拟器

1. **创建 TV 模拟器**:
   - Tools → Device Manager → Create Device
   - 选择 TV → Android TV (1080p)
   - 选择系统镜像 (API 21+)
   - 启动模拟器

2. **键盘映射**:
   - 方向键: ↑ ↓ ← →
   - 确认键: Enter
   - 返回键: Esc
   - 播放/暂停: Space

### 使用真实 TV 设备

通过 ADB 连接：

```bash
# 通过 WiFi 连接（TV 和电脑在同一网络）
adb connect <TV_IP_ADDRESS>:5555

# 安装 APK
adb install app-tv/build/outputs/apk/debug/app-tv-debug.apk

# 查看日志
adb logcat | findstr "OrangePlayer"
```

## 遥控器按键说明

| 按键 | 功能 |
|-----|------|
| 方向键 ↑ | 显示控制栏 |
| 方向键 ↓ | 隐藏控制栏 |
| 方向键 ← | 快退 10 秒（控制栏隐藏时） |
| 方向键 → | 快进 10 秒（控制栏隐藏时） |
| 确认键 | 播放/暂停（控制栏隐藏时）<br>点击按钮（控制栏显示时） |
| 返回键 | 退出播放 |
| 播放/暂停键 | 播放/暂停 |
| 快进键 | 快进 30 秒 |
| 快退键 | 快退 30 秒 |

## 项目结构

```
app-tv/
├── src/main/
│   ├── java/com/orange/player/tv/
│   │   ├── TvApplication.java          # Application 类
│   │   ├── model/
│   │   │   └── Video.java              # 视频数据模型
│   │   ├── presenter/
│   │   │   └── VideoCardPresenter.java # Leanback 卡片展示
│   │   └── ui/
│   │       ├── TvMainActivity.java     # 主界面
│   │       ├── TvMainFragment.java     # Leanback 列表
│   │       └── TvPlayerActivity.java   # 播放器界面
│   ├── res/
│   │   ├── drawable/                   # 图标和背景
│   │   ├── layout/                     # 布局文件
│   │   ├── mipmap/                     # 应用图标
│   │   └── values/                     # 字符串和主题
│   └── AndroidManifest.xml             # TV 配置
├── build.gradle                        # 构建配置
└── README.md                           # 本文件
```

## 示例视频

应用内置了以下开源测试视频：

1. **Big Buck Bunny** (09:56)
2. **Elephant Dream** (10:53)
3. **Sintel** (14:48)
4. **Tears of Steel** (12:14)

这些视频来自 Blender 开源电影项目，可用于测试播放功能。

## 自定义视频

要添加自己的视频，编辑 `TvMainFragment.java`:

```java
private List<Video> createDemoVideos() {
    List<Video> videos = new ArrayList<>();
    
    videos.add(new Video(
            "我的视频",                    // 标题
            "http://example.com/video.mp4", // URL
            "http://example.com/thumb.jpg", // 缩略图
            "10:30"                         // 时长
    ));
    
    return videos;
}
```

## 支持的视频格式

- MP4 (H.264/H.265)
- HLS (m3u8)
- DASH
- RTSP 直播
- HTTP/HTTPS 点播

## 已知问题

1. **模拟器音频问题**: TV 模拟器可能存在音频播放问题，建议在真实设备上测试
2. **焦点丢失**: 某些情况下焦点可能丢失，按方向键可恢复

## 相关文档

- [TV 适配指南](../docs/TV_ADAPTATION_GUIDE.md)
- [TV 快速开始](../docs/TV_QUICK_START.md)
- [TV 示例代码](../docs/TV_PLAYER_EXAMPLE.md)

## 技术支持

如有问题，请查看：
- [常见问题](../docs/FAQ.md)
- [GitHub Issues](https://github.com/706412584/orangeplayer/issues)

## 许可证

Apache 2.0 License
