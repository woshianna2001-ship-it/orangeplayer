# Android TV 模块创建记录

## 概述

成功创建了 `app-tv` 模块，这是一个完整的 Android TV 应用示例，展示了如何使用 OrangePlayer 在 TV 设备上播放视频。

## 模块结构

```
app-tv/
├── src/main/
│   ├── java/com/orange/player/tv/
│   │   ├── TvApplication.java          # 应用初始化
│   │   ├── ui/
│   │   │   ├── TvMainActivity.java     # 主界面 Activity
│   │   │   ├── TvMainFragment.java     # Leanback 视频列表
│   │   │   └── TvPlayerActivity.java   # 播放器 Activity
│   │   ├── presenter/
│   │   │   └── VideoCardPresenter.java # 视频卡片展示
│   │   └── model/
│   │       └── Video.java              # 视频数据模型
│   ├── res/
│   │   ├── layout/                     # 布局文件
│   │   ├── drawable/                   # 图标和背景
│   │   ├── mipmap/                     # 应用图标
│   │   └── values/                     # 字符串和主题
│   └── AndroidManifest.xml
├── build.gradle
└── README.md
```

## 技术特点

### 1. 播放器配置
- **内核**: ExoPlayer (推荐用于 TV)
- **渲染**: SurfaceView
- **解码**: 硬件解码
- **缓存**: 已禁用（避免网络问题）

### 2. TV 适配
- Leanback 界面设计
- 遥控器导航支持
- 焦点管理
- 大按钮设计（适合 10-foot UI）
- 自动隐藏控制栏

### 3. 遥控器支持
- 方向键导航
- 确认键播放/暂停
- 左右键快进/快退
- 返回键退出
- 媒体按键支持

## 问题解决记录

### 问题 1: 点击播放返回桌面
**原因**: 未找到具体原因，但通过完善代码解决

**解决方案**:
- 完善了 TvPlayerActivity 的生命周期管理
- 添加了错误处理和 Toast 提示
- 确保视频 URL 正确传递

### 问题 2: 视频一直加载中
**原因**: HttpProxyCache 尝试缓存视频导致网络超时

**错误日志**:
```
E HttpProxyCacheDebuger: Error fetching info from http://...
W System.err: java.net.SocketTimeoutException: failed to connect
```

**解决方案**:
1. 在 TvApplication 中禁用缓存:
```java
CacheFactory.setCacheManager(null);
```

2. 使用稳定的视频源（阿里云 CDN）

### 问题 3: 国外视频源无法访问
**原因**: 模拟器网络环境无法访问 Google 的测试视频

**解决方案**:
- 替换为国内可访问的视频源
- 使用阿里云 CDN: `http://player.alicdn.com/video/aliyunmedia.mp4`
- 使用 W3C 测试视频: `https://media.w3.org/2010/05/sintel/trailer.mp4`

## 测试结果

### 成功测试项
✅ 应用启动正常  
✅ Leanback 界面显示正常  
✅ 视频列表展示正常  
✅ 点击视频进入播放器  
✅ 视频成功播放  
✅ 遥控器导航正常  
✅ 焦点管理正常  

### 播放日志
```
D OrangevideoView: setStateAndUi: PREPARING (1)     # 准备中
D OrangevideoView: setStateAndUi: PLAYING (2)       # 开始播放
D OrangevideoView: setStateAndUi: BUFFERING_START (3) # 缓冲
D OrangevideoView: setStateAndUi: PLAYING (2)       # 继续播放
D OrangevideoView: hideAllWidget: state=2           # 正在播放
```

## 配置要求

### 最低要求
- **minSdk**: 23 (Android 6.0)
- **targetSdk**: 36
- **播放器内核**: ExoPlayer (Media3)

### 依赖项
```gradle
// Android TV Leanback
implementation 'androidx.leanback:leanback:1.2.0-alpha04'
implementation 'androidx.tvprovider:tvprovider:1.0.0'

// 播放器模块
implementation project(':palyerlibrary')
implementation project(':gsyVideoPlayer-java')
implementation project(':gsyVideoPlayer-exo_player2')

// Native so 文件
implementation project(':gsyVideoPlayer-armv7a')
implementation project(':gsyVideoPlayer-armv64')
implementation project(':gsyVideoPlayer-x86')
implementation project(':gsyVideoPlayer-x86_64')
```

## 使用方法

### 1. 编译安装
```bash
# 编译
gradlew :app-tv:assembleDebug

# 安装到 TV 设备
adb install -r app-tv/build/outputs/apk/debug/app-tv-debug.apk

# 或使用快捷脚本
run-tv-demo.bat
```

### 2. 启动应用
```bash
adb shell am start -n com.orange.player.tv/.ui.TvMainActivity
```

### 3. 查看日志
```bash
adb logcat | findstr "TvPlayerActivity\|OrangevideoView"
```

## 视频源配置

### 当前使用的视频源
```java
// 阿里云 CDN（稳定）
"http://player.alicdn.com/video/aliyunmedia.mp4"

// W3C 测试视频
"https://media.w3.org/2010/05/sintel/trailer.mp4"

// 时光网测试视频
"http://vfx.mtime.cn/Video/2019/03/21/mp4/190321153853126488.mp4"
```

### 添加自定义视频
在 `TvMainFragment.java` 中修改:
```java
private List<Video> createDemoVideos() {
    List<Video> videos = new ArrayList<>();
    videos.add(new Video(
        "视频标题",
        "视频URL",
        "封面URL（可选）",
        "时长"
    ));
    return videos;
}
```

## 后续优化建议

### 功能增强
1. 添加视频搜索功能
2. 支持视频分类和筛选
3. 添加播放历史记录
4. 支持字幕和弹幕
5. 添加设置界面

### 性能优化
1. 实现视频预加载
2. 优化图片加载（使用 Glide）
3. 添加网络状态检测
4. 实现断点续播

### UI 优化
1. 添加更多动画效果
2. 优化焦点指示器
3. 添加视频详情页
4. 支持横向和纵向布局

## 相关文档

- [TV 快速开始指南](TV_QUICK_START.md)
- [TV 适配完整指南](TV_ADAPTATION_GUIDE.md)
- [TV 播放器示例代码](TV_PLAYER_EXAMPLE.md)
- [播放器内核 API 支持](PLAYER_ENGINE_API_SUPPORT.md)

## 总结

成功创建了一个功能完整的 Android TV 应用示例，展示了 OrangePlayer 在 TV 平台上的使用方法。通过解决缓存和网络问题，实现了稳定的视频播放功能。该模块可以作为开发者集成 OrangePlayer 到 TV 应用的参考示例。
