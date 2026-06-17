# API 文档

完整的 OrangePlayer API 参考文档。

## 目录

- [OrangevideoView](#orangevideoview) - 主播放器视图
- [OrangeVideoController](#orangevideocontroller) - 播放器控制器
- [SubtitleManager](#subtitlemanager) - 字幕管理器
- [LanguagePackManager](#languagepackmanager) - OCR 语言包管理
- [PlayerSettingsManager](#playersettingsmanager) - 设置管理器
- [PlayerConstants](#playerconstants) - 常量定义
- [视频下载 API](#视频下载-api) - 下载与选集下载功能
- [监听器接口](#监听器接口) - 回调接口
- [UI组件访问](#ui组件访问) - 自定义控件样式和行为

---

## OrangevideoView

主播放器视图类，继承自 GSYVideoPlayer。

### 基础方法

#### setUp()

设置视频播放地址和参数。

```java
// 基础设置
boolean setUp(String url, boolean cacheWithPlay, String title)

// 带缓存路径
boolean setUp(String url, boolean cacheWithPlay, File cachePath, String title)

// 带请求头
boolean setUp(String url, boolean cacheWithPlay, File cachePath, 
              Map<String, String> headers, String title)
```

**参数说明：**
- `url` - 视频地址（支持 HTTP/HTTPS/RTSP/本地文件）
- `cacheWithPlay` - 是否边播边缓存
- `cachePath` - 缓存路径（可选）
- `headers` - HTTP 请求头（可选）
- `title` - 视频标题

**返回值：**
- `true` - 设置成功
- `false` - 设置失败

**示例：**

```java
// 基础用法
videoView.setUp("https://example.com/video.mp4", true, "示例视频");

// 带请求头
Map<String, String> headers = new HashMap<>();
headers.put("User-Agent", "MyPlayer/1.0");
headers.put("Referer", "https://example.com");
videoView.setUp(url, true, null, headers, "示例视频");
```

#### setUrl()

简化的设置视频地址方法。

```java
void setUrl(String url)
void setUrl(String url, Map<String, String> headers)
```

**示例：**

```java
// 不带请求头
videoView.setUrl("https://example.com/video.mp4");

// 带请求头
Map<String, String> headers = new HashMap<>();
headers.put("Authorization", "Bearer token");
videoView.setUrl(url, headers);
```

#### startPlayLogic()

开始播放逻辑，会触发视频加载和播放。

```java
void startPlayLogic()
```

**注意：** 必须先调用 `setUp()` 或 `setUrl()` 设置视频地址。

#### pause()

暂停播放。

```java
void pause()
```

#### resume()

恢复播放。

```java
void resume()
```

#### seekTo()

跳转到指定播放位置。

```java
void seekTo(long position)
```

**参数：**
- `position` - 目标位置（毫秒）

**示例：**

```java
// 跳转到 30 秒
videoView.seekTo(30 * 1000);

// 跳转到 50% 位置
long duration = videoView.getDuration();
videoView.seekTo(duration / 2);
```

#### release()

释放播放器资源。

```java
void release()
```

**注意：** 在 Activity 的 `onDestroy()` 中调用，避免内存泄漏。

### 播放状态查询

#### isPlaying()

检查是否正在播放。

```java
boolean isPlaying()
```

**返回值：**
- `true` - 正在播放
- `false` - 未播放（暂停/停止/错误）

#### getCurrentPositionWhenPlaying()

获取当前播放位置。

```java
long getCurrentPositionWhenPlaying()
```

**返回值：** 当前播放位置（毫秒）

**示例：**

```java
long position = videoView.getCurrentPositionWhenPlaying();
String time = formatTime(position);  // "01:23"
```

#### getDuration()

获取视频总时长。

```java
long getDuration()
```

**返回值：** 视频总时长（毫秒），如果是直播返回 0

**示例：**

```java
long duration = videoView.getDuration();
if (duration > 0) {
    // 点播视频
    int progress = (int) (position * 100 / duration);
} else {
    // 直播视频
}
```

#### getBuffterPoint()

获取缓冲进度百分比。

```java
int getBuffterPoint()
```

**返回值：** 缓冲进度（0-100）

**示例：**

```java
int buffered = videoView.getBuffterPoint();
mSecondaryProgress.setProgress(buffered);
```

#### getCurrentVideoWidth() / getCurrentVideoHeight()

获取视频原始尺寸。

```java
int getCurrentVideoWidth()
int getCurrentVideoHeight()
```

**示例：**

```java
int width = videoView.getCurrentVideoWidth();
int height = videoView.getCurrentVideoHeight();
float aspectRatio = (float) width / height;
```

### 播放控制

#### setSpeed()

设置播放倍速。

```java
void setSpeed(float speed)
```

**参数：**
- `speed` - 倍速值（0.35 - 10.0）
  - IJK 内核：最高 2.0x
  - 其他内核：最高 5.0x

**示例：**

```java
// 1.5 倍速播放
videoView.setSpeed(1.5f);

// 慢速播放
videoView.setSpeed(0.5f);

// 快速播放
videoView.setSpeed(3.0f);
```

#### getSpeed()

获取当前倍速。

```java
float getSpeed()
```

**返回值：** 当前倍速值

#### setLooping()

设置是否循环播放。

```java
void setLooping(boolean looping)
```

**示例：**

```java
// 单曲循环
videoView.setLooping(true);
```

#### setAutoRotateOnFullscreen()

设置全屏时是否自动旋转。

```java
void setAutoRotateOnFullscreen(boolean autoRotate)
```

**示例：**

```java
// 启用全屏自动旋转
videoView.setAutoRotateOnFullscreen(true);
```

### 播放内核

#### selectPlayerFactory()

切换播放内核。

```java
void selectPlayerFactory(String engine)
```

**参数：**
- `PlayerConstants.ENGINE_DEFAULT` - 系统播放器（MediaPlayer）
- `PlayerConstants.ENGINE_EXO` - ExoPlayer（推荐）
- `PlayerConstants.ENGINE_IJK` - IJK 播放器
- `PlayerConstants.ENGINE_ALI` - 阿里云播放器

**示例：**

```java
// 切换到 ExoPlayer
videoView.selectPlayerFactory(PlayerConstants.ENGINE_EXO);

// 切换后需要重新播放
videoView.startPlayLogic();
```

**内核对比：**

| 内核 | 优点 | 缺点 | 适用场景 |
|------|------|------|---------|
| 系统 | 无需额外依赖 | 格式支持有限 | 普通 MP4 |
| ExoPlayer | 格式支持全，性能好 | 包体积较大 | 推荐默认 |
| IJK | 格式支持最全 | 包体积大 | 特殊格式 |
| 阿里云 | 性能最好 | 需要 License | 商业项目 |

### 全屏控制

#### startWindowFullscreen()

进入全屏模式。

```java
void startWindowFullscreen(Context context, boolean actionBar, boolean statusBar)
```

**参数：**
- `context` - Activity 上下文
- `actionBar` - 是否隐藏 ActionBar
- `statusBar` - 是否隐藏状态栏

**示例：**

```java
// 进入全屏（隐藏状态栏和 ActionBar）
videoView.startWindowFullscreen(this, true, true);
```

#### exitFullScreen()

退出全屏模式。

```java
void exitFullScreen()
```

#### isFullScreen()

检查是否全屏。

```java
boolean isFullScreen()
```

**示例：**

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

### 组件控制

#### getVideoController()

获取播放器控制器。

```java
OrangeVideoController getVideoController()
```

**返回值：** 控制器实例，如果未初始化返回 null

**示例：**

```java
OrangeVideoController controller = videoView.getVideoController();
if (controller != null) {
    controller.setTitle("新标题");
}
```

#### setVideoController()

设置自定义控制器（可选）。

```java
void setVideoController(OrangeVideoController controller)
```

**注意：** 大多数情况下不需要调用，播放器会自动创建控制器。

#### enableOrangeComponents()

启用 Orange 自定义组件。

```java
void enableOrangeComponents()
```

**注意：** 默认已启用，无需手动调用。

#### getPrepareView() / getTitleView() / getVodControlView()

获取各个 UI 组件。

```java
PrepareView getPrepareView()
TitleView getTitleView()
VodControlView getVodControlView()
CompleteView getCompleteView()
ErrorView getErrorView()
```

**示例：**

```java
// 自定义准备视图的缩略图
PrepareView prepareView = videoView.getPrepareView();
if (prepareView != null) {
    prepareView.setThumbnail(R.drawable.video_thumb);
}

// 自定义标题
TitleView titleView = videoView.getTitleView();
if (titleView != null) {
    titleView.setTitle("自定义标题");
}
```

#### setDebugLogCallback()

设置调试日志回调。

```java
void setDebugLogCallback(DebugLogCallback callback)

interface DebugLogCallback {
    void onLog(String msg);
}
```

**示例：**

```java
videoView.setDebugLogCallback(msg -> {
    Log.d("VideoPlayer", msg);
});
```

### 监听器

#### addOnStateChangeListener()

添加状态变化监听器。

```java
void addOnStateChangeListener(OnStateChangeListener listener)
```

**示例：**

```java
videoView.addOnStateChangeListener(new OnStateChangeListener() {
    @Override
    public void onPlayerStateChanged(int playerState) {
        // 播放器状态变化（普通/全屏）
    }
    
    @Override
    public void onPlayStateChanged(int playState) {
        // 播放状态变化（播放/暂停/错误等）
    }
});
```

#### setOnProgressListener()

设置播放进度监听器。

```java
void setOnProgressListener(OnProgressListener listener)
```

**示例：**

```java
videoView.setOnProgressListener((currentPosition, duration) -> {
    // 更新进度条
    int progress = (int) (currentPosition * 100 / duration);
    progressBar.setProgress(progress);
});
```

#### setOnPlayCompleteListener()

设置播放完成监听器。

```java
void setOnPlayCompleteListener(OnPlayCompleteListener listener)
```

**示例：**

```java
videoView.setOnPlayCompleteListener(() -> {
    // 播放完成，自动播放下一个
    playNextVideo();
});
```

### 生命周期方法

在 Activity 的生命周期方法中调用对应方法：

```java
@Override
protected void onPause() {
    super.onPause();
    videoView.onVideoPause();
}

@Override
protected void onResume() {
    super.onResume();
    videoView.onVideoResume();
}

@Override
protected void onDestroy() {
    super.onDestroy();
    videoView.release();
}

@Override
public void onBackPressed() {
    if (videoView.isFullScreen()) {
        videoView.exitFullScreen();
        return;
    }
    super.onBackPressed();
}

@Override
public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    // 横竖屏切换时调用
    videoView.onConfigurationChanged(this, newConfig, 
        mOrientationUtils, true, true);
}
```

---

## OrangeVideoController

播放器控制器，管理所有 UI 组件和交互逻辑。

### 字幕控制

#### loadSubtitle()

加载字幕文件。

```java
void loadSubtitle(String url, SubtitleManager.OnSubtitleLoadListener listener)
```

**参数：**
- `url` - 字幕文件地址（支持 HTTP/本地文件）
- `listener` - 加载结果回调

**支持格式：**
- SRT (SubRip)
- ASS/SSA (Advanced SubStation Alpha)
- VTT (WebVTT)

**示例：**

```java
OrangeVideoController controller = videoView.getVideoController();
controller.loadSubtitle("https://example.com/subtitle.srt", 
    new SubtitleManager.OnSubtitleLoadListener() {
        @Override
        public void onLoadSuccess(int count) {
            Toast.makeText(context, "加载成功，共 " + count + " 条字幕", 
                Toast.LENGTH_SHORT).show();
        }
        
        @Override
        public void onLoadFailed(String error) {
            Toast.makeText(context, "加载失败：" + error, 
                Toast.LENGTH_SHORT).show();
        }
    });
```

#### getSubtitleManager()

获取字幕管理器实例。

```java
SubtitleManager getSubtitleManager()
```

**返回值：** 字幕管理器实例

#### isSubtitleLoaded()

检查是否已加载字幕。

```java
boolean isSubtitleLoaded()
```

#### startSubtitle() / stopSubtitle()

开始/停止字幕显示。

```java
void startSubtitle()
void stopSubtitle()
```

### 弹幕控制

#### getDanmakuController()

获取弹幕控制器。

```java
IDanmakuController getDanmakuController()
```

**返回值：** 弹幕控制器实例

**示例：**

```java
IDanmakuController danmaku = controller.getDanmakuController();
if (danmaku != null) {
    danmaku.sendDanmaku("这是一条弹幕", 0xFFFFFFFF);
}
```

#### sendDanmaku()

发送弹幕。

```java
void sendDanmaku(String text)
void sendDanmaku(String text, int color)
```

**参数：**
- `text` - 弹幕文本
- `color` - 弹幕颜色（ARGB 格式）

**示例：**

```java
// 发送白色弹幕
controller.sendDanmaku("666");

// 发送彩色弹幕
controller.sendDanmaku("红色弹幕", 0xFFFF0000);
controller.sendDanmaku("蓝色弹幕", 0xFF0000FF);
```

#### showDanmaku() / hideDanmaku()

显示/隐藏弹幕。

```java
void showDanmaku()
void hideDanmaku()
```

### 进度条拖动预览

#### 功能说明

在全屏模式下拖动进度条时，会显示预览窗口，展示目标时间点的视频帧。

**特性：**
- ✅ 仅在全屏模式下启用
- ✅ 拖动超过 400ms 后显示预览窗口
- ✅ 三级加载策略，优化加载速度
- ✅ 播放状态下不会随控制器自动隐藏

**加载策略：**

1. **Glide（最高优先级）** - 50-200ms
   - 支持：MP4, AVI, MKV, MOV 等本地格式
   - 不支持：M3U8/HLS, RTSP, RTMP, FLV 等流媒体

2. **ScreenshotManager（中优先级）** - 200-500ms
   - 支持所有播放器能播放的格式
   - 需要播放器已准备就绪

3. **VideoThumbnailHelper（兜底）** - 200-500ms
   - 使用 MediaMetadataRetriever
   - 支持所有格式

**性能对比：**

| 格式 | 加载方式 | 耗时 | 说明 |
|------|---------|------|------|
| MP4 | Glide | 50-200ms | 最快 |
| HLS/M3U8 | ScreenshotManager | 200-500ms | 流媒体 |
| RTSP/RTMP | VideoThumbnailHelper | 200-500ms | 直播流 |

**自动优化：**
- 复用 MediaMetadataRetriever 实例
- Android 8.0+ 使用缩略图 API
- 使用 OPTION_CLOSEST_SYNC 快速定位关键帧
- 200ms 节流防止频繁请求

**用户体验：**
- 拖动时实时显示预览
- 松手后预览窗口延迟 500ms 消失
- 播放状态下预览窗口不会自动隐藏

### 播放列表

#### addVideo()

添加视频到播放列表。

```java
// 基础方法
void addVideo(String name, String url)

// 带请求头
void addVideo(String name, String url, HashMap<String, String> headers)

// 独立视频（不拼接系列标题）
void addVideo(String name, String url, boolean isIndependent)

// 独立视频 + 请求头
void addVideo(String name, String url, boolean isIndependent, HashMap<String, String> headers)
```

**参数：**
- `name` - 视频名称（如"第1集"）
- `url` - 视频地址
- `headers` - 可选的 HTTP 请求头
- `isIndependent` - 是否为独立视频（已废弃，现在所有视频都直接显示名称）

**特性：**
- 添加第一个视频时，如果播放器还没有设置视频地址，会自动设置第一个视频
- 支持自定义 HTTP 请求头（如 Referer、User-Agent 等）

**示例：**

```java
OrangeVideoController controller = videoView.getVideoController();

// 添加基础视频
controller.addVideo("第1集", "https://example.com/video1.mp4");
controller.addVideo("第2集", "https://example.com/video2.mp4");

// 添加带请求头的视频
HashMap<String, String> headers = new HashMap<>();
headers.put("Referer", "https://example.com");
controller.addVideo("第3集", "https://example.com/video3.mp4", headers);

// 第一个视频会自动设置到播放器，可以直接播放
videoView.startPlayLogic();
```

#### setVideoList()

设置播放列表（批量设置）。

```java
void setVideoList(ArrayList<HashMap<String, Object>> list)
```

**参数：**
- `list` - 视频列表，每个元素包含以下字段：
  - `name` (String) - 视频名称
  - `url` (String) - 视频地址
  - `headers` (HashMap<String, String>) - 可选的请求头

**特性：**
- 设置列表时，如果播放器还没有设置视频地址，会自动设置第一个视频
- 自动为每个视频添加空的 headers 字段（如果不存在）

**示例：**

```java
ArrayList<HashMap<String, Object>> playlist = new ArrayList<>();

HashMap<String, Object> video1 = new HashMap<>();
video1.put("name", "第1集");
video1.put("url", "https://example.com/video1.mp4");
playlist.add(video1);

HashMap<String, Object> video2 = new HashMap<>();
video2.put("name", "第2集");
video2.put("url", "https://example.com/video2.mp4");
// 可选：添加请求头
HashMap<String, String> headers = new HashMap<>();
headers.put("Referer", "https://example.com");
video2.put("headers", headers);
playlist.add(video2);

controller.setVideoList(playlist);

// 第一个视频会自动设置到播放器
videoView.startPlayLogic();
```

#### getVideoList()

获取当前播放列表。

```java
ArrayList<HashMap<String, Object>> getVideoList()
```

**返回：** 视频列表，如果没有设置则返回 null

#### removeVideoList()

清空播放列表。

```java
void removeVideoList()
```

**示例：**

```java
controller.removeVideoList();
```

### 播放控制

#### playNextEpisode()

播放下一集（通过 VideoEventManager）。

```java
VideoEventManager eventManager = controller.getVideoEventManager();
if (eventManager != null) {
    eventManager.playNextEpisode();
}
```

**行为：**
- 自动查找当前视频在列表中的位置
- 播放下一个视频
- 如果已是最后一集，显示提示"已经是最后一集了"

#### hasNextEpisode()

检查是否有下一集。

```java
VideoEventManager eventManager = controller.getVideoEventManager();
if (eventManager != null) {
    boolean hasNext = eventManager.hasNextEpisode();
}
```

#### handlePlaybackCompleted()

处理播放完成事件（根据播放模式自动处理）。

```java
VideoEventManager eventManager = controller.getVideoEventManager();
if (eventManager != null) {
    eventManager.handlePlaybackCompleted();
}
```

**播放模式：**
- `sequential` - 顺序播放：自动播放下一集
- `single_loop` - 单集循环：重新播放当前视频
- `play_pause` - 播放暂停：停止播放

**注意：** 此方法会在播放完成时自动调用，通常不需要手动调用。

### 播放模式设置

播放模式通过设置界面配置，保存在 `PlayerSettingsManager` 中：

```java
// 获取当前播放模式
String mode = PlayerSettingsManager.getInstance(context).getPlayMode();

// 设置播放模式
PlayerSettingsManager.getInstance(context).setPlayMode("sequential");
```

**可用模式：**
- `"sequential"` - 顺序播放
- `"single_loop"` - 单集循环
- `"play_pause"` - 播放暂停（默认）

### UI 控制

#### show() / hide()

显示/隐藏控制器。

```java
void show()
void hide()
```

**示例：**

```java
// 点击屏幕切换控制器显示状态
videoView.setOnClickListener(v -> {
    if (controller.isShowing()) {
        controller.hide();
    } else {
        controller.show();
    }
});
```

#### isShowing()

检查控制器是否正在显示。

```java
boolean isShowing()
```

#### setTitle()

设置视频标题。

```java
void setTitle(String title)
```

**示例：**

```java
controller.setTitle("新的视频标题");
```

#### setThumbnail()

设置视频缩略图。

```java
void setThumbnail(Object thumbnail)
```

**参数：**
- `thumbnail` - 缩略图（支持 Bitmap、资源 ID、URL 字符串、File 对象）

**示例：**

```java
// 使用资源 ID
controller.setThumbnail(R.drawable.video_thumb);

// 使用 URL
controller.setThumbnail("https://example.com/thumb.jpg");

// 使用 Bitmap
Bitmap bitmap = ...;
controller.setThumbnail(bitmap);
```

#### toggleLockState()

切换锁定状态（全屏时有效）。

```java
void toggleLockState()
```

#### setLocked()

设置锁定状态。

```java
void setLocked(boolean locked)
```

**参数：**
- `locked` - true 锁定，false 解锁

**说明：** 锁定后会隐藏除锁定按钮外的所有控制组件，防止误触。

#### isLocked()

检查是否已锁定。

```java
boolean isLocked()
```

#### setControllerVisibilityEnabled()

设置控制器可见性是否启用。用于某些播放模式需要保留控制器功能但不显示UI。

```java
void setControllerVisibilityEnabled(boolean enabled)
```

**参数：**
- `enabled` - true: 允许显示控制器(默认), false: 禁止显示控制器UI

**说明：** 
- 禁用后，控制器UI不会显示，但控制器功能仍然可用
- 适用于需要后台控制但不希望用户看到UI的场景
- 可通过 `OrangevideoView.setControllerVisibilityEnabled()` 或 `OrangeVideoController.setControllerVisibilityEnabled()` 调用

**示例：**

```java
// 方式1：通过播放器视图设置
videoView.setControllerVisibilityEnabled(false);  // 禁用控制器UI显示

// 方式2：通过控制器设置
OrangeVideoController controller = videoView.getVideoController();
if (controller != null) {
    controller.setControllerVisibilityEnabled(false);
}

// 恢复控制器UI显示
videoView.setControllerVisibilityEnabled(true);
```

#### isControllerVisibilityEnabled()

检查控制器可见性是否启用。

```java
boolean isControllerVisibilityEnabled()
```

**返回值：** true: 允许显示, false: 禁止显示

**示例：**

```java
if (videoView.isControllerVisibilityEnabled()) {
    // 控制器UI可以显示
} else {
    // 控制器UI被禁用
}
```

### 事件管理器

#### getVideoEventManager()

获取视频事件管理器。

```java
VideoEventManager getVideoEventManager()
```

**返回值：** 事件管理器实例，用于访问高级功能（OCR、语音识别等）

**示例：**

```java
VideoEventManager eventManager = controller.getVideoEventManager();
if (eventManager != null) {
    // 启动语音识别
    eventManager.startSpeechTranslate();
    
    // 启动 OCR 识别
    eventManager.startOcrTranslate();
}

---

## SubtitleManager

字幕管理器，负责字幕的加载、解析和显示。

### 加载字幕

#### loadSubtitle()

从 URL 或 Uri 加载字幕文件。

```java
void loadSubtitle(String url, OnSubtitleLoadListener listener)
void loadSubtitle(Uri uri, OnSubtitleLoadListener listener)
```

**参数：**
- `url` - 字幕文件 URL（HTTP/HTTPS/本地文件路径）
- `uri` - 字幕文件 Uri
- `listener` - 加载结果回调

**支持格式：**
- `.srt` - SubRip 格式
- `.ass` / `.ssa` - Advanced SubStation Alpha 格式
- `.vtt` - WebVTT 格式

**示例：**

```java
SubtitleManager manager = videoView.getVideoController().getSubtitleManager();

// 从 URL 加载
manager.loadSubtitle("https://example.com/subtitle.srt", 
    new SubtitleManager.OnSubtitleLoadListener() {
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

// 从本地文件加载
File subtitleFile = new File(getExternalFilesDir(null), "subtitle.srt");
if (subtitleFile.exists()) {
    manager.loadSubtitle(Uri.fromFile(subtitleFile), listener);
}

// 从 ContentProvider 加载（文件选择器）
Uri uri = data.getData();  // Intent data from file picker
manager.loadSubtitle(uri, listener);
```

### 字幕控制

#### start() / stop()

开始/停止字幕显示。

```java
void start()
void stop()
```

**示例：**

```java
// 开始显示字幕
manager.start();

// 暂停字幕显示
manager.stop();
```

#### setTextSize()

设置字幕文字大小。

```java
void setTextSize(float size)
```

**参数：**
- `size` - 字体大小（12-36 sp）

**示例：**

```java
// 设置为 18sp
manager.setTextSize(18f);

// 字幕大小选择器
String[] sizes = {"小 (14sp)", "中 (18sp)", "大 (22sp)", "超大 (26sp)"};
float[] values = {14f, 18f, 22f, 26f};

new AlertDialog.Builder(context)
    .setTitle("字幕大小")
    .setItems(sizes, (dialog, which) -> {
        manager.setTextSize(values[which]);
    })
    .show();
```

#### getSubtitleCount()

获取字幕条目数量。

```java
int getSubtitleCount()
```

**返回值：** 字幕总数

**示例：**

```java
int count = manager.getSubtitleCount();
if (count > 0) {
    Log.d(TAG, "已加载 " + count + " 条字幕");
}
```

### 回调接口

#### OnSubtitleLoadListener

字幕加载结果回调。

```java
interface OnSubtitleLoadListener {
    void onLoadSuccess(int count);
    void onLoadFailed(String error);
}
```

**方法说明：**
- `onLoadSuccess(int count)` - 加载成功，返回字幕条目数
- `onLoadFailed(String error)` - 加载失败，返回错误信息

---

## LanguagePackManager

OCR 语言包管理器，用于下载和管理 Tesseract OCR 语言包。

### 语言包查询

#### getAvailableLanguages()

获取所有可用的语言包列表。

```java
List<LanguagePack> getAvailableLanguages()
```

**返回值：** 可用语言包列表

**LanguagePack 结构：**
```java
class LanguagePack {
    String code;          // 语言代码，如 "chi_sim"
    String displayName;   // 显示名称，如 "简体中文"
    String downloadUrl;   // 下载地址
    long fileSize;        // 文件大小（字节）
}
```

**示例：**

```java
LanguagePackManager manager = new LanguagePackManager(context);
List<LanguagePack> languages = manager.getAvailableLanguages();

for (LanguagePack lang : languages) {
    Log.d(TAG, lang.displayName + " (" + lang.code + ") - " + 
        formatFileSize(lang.fileSize));
}
```

#### getInstalledLanguages()

获取已安装的语言包列表。

```java
List<String> getInstalledLanguages()
```

**返回值：** 已安装语言包的代码列表

**示例：**

```java
List<String> installed = manager.getInstalledLanguages();
if (installed.contains("chi_sim")) {
    Log.d(TAG, "简体中文语言包已安装");
}
```

#### isLanguageInstalled()

检查指定语言包是否已安装。

```java
boolean isLanguageInstalled(String languageCode)
```

**参数：**
- `languageCode` - 语言代码（如 "chi_sim"、"eng"）

**返回值：** true 已安装，false 未安装

**示例：**

```java
if (manager.isLanguageInstalled("chi_sim")) {
    // 可以直接使用
    startOcr("chi_sim");
} else {
    // 需要先下载
    downloadLanguagePack("chi_sim");
}
```

### 语言包管理

#### downloadLanguage()

下载指定语言包。

```java
void downloadLanguage(String languageCode, DownloadCallback callback)
```

**参数：**
- `languageCode` - 语言代码
- `callback` - 下载进度回调

**示例：**

```java
manager.downloadLanguage("chi_sim", new LanguagePackManager.DownloadCallback() {
    @Override
    public void onProgress(int progress, long downloaded, long total) {
        // 更新进度条
        progressBar.setProgress(progress);
        String text = String.format("下载中... %d%% (%s / %s)",
            progress,
            formatFileSize(downloaded),
            formatFileSize(total));
        progressText.setText(text);
    }
    
    @Override
    public void onSuccess() {
        Toast.makeText(context, "下载成功", Toast.LENGTH_SHORT).show();
        progressDialog.dismiss();
    }
    
    @Override
    public void onError(String error) {
        Toast.makeText(context, "下载失败：" + error, Toast.LENGTH_LONG).show();
        progressDialog.dismiss();
    }
});
```

#### deleteLanguage()

删除指定语言包。

```java
boolean deleteLanguage(String languageCode)
```

**参数：**
- `languageCode` - 语言代码

**返回值：** true 删除成功，false 删除失败

**示例：**

```java
new AlertDialog.Builder(context)
    .setTitle("删除语言包")
    .setMessage("确定要删除简体中文语言包吗？")
    .setPositiveButton("删除", (dialog, which) -> {
        if (manager.deleteLanguage("chi_sim")) {
            Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show();
        }
    })
    .setNegativeButton("取消", null)
    .show();
```

### 工具方法

#### getLanguageDisplayName()

获取语言的显示名称。

```java
static String getLanguageDisplayName(String langCode)
```

**参数：**
- `langCode` - 语言代码

**返回值：** 语言显示名称

**支持的语言代码：**

| 代码 | 显示名称 | 文件大小 |
|------|---------|---------|
| chi_sim | 简体中文 | 2.35 MB |
| chi_tra | 繁体中文 | 2.26 MB |
| eng | 英语 | 3.92 MB |
| jpn | 日语 | 2.36 MB |
| kor | 韩语 | 1.60 MB |
| fra | 法语 | 2.19 MB |
| deu | 德语 | 1.99 MB |
| spa | 西班牙语 | 2.18 MB |
| rus | 俄语 | 2.84 MB |
| ara | 阿拉伯语 | 1.87 MB |

**示例：**

```java
String displayName = LanguagePackManager.getLanguageDisplayName("chi_sim");
// 返回 "简体中文"
```

### 回调接口

#### DownloadCallback

语言包下载回调。

```java
interface DownloadCallback {
    void onProgress(int progress, long downloaded, long total);
    void onSuccess();
    void onError(String error);
}
```

**方法说明：**
- `onProgress(int progress, long downloaded, long total)` - 下载进度更新
  - `progress` - 进度百分比（0-100）
  - `downloaded` - 已下载字节数
  - `total` - 总字节数
- `onSuccess()` - 下载成功
- `onError(String error)` - 下载失败，返回错误信息

---

## PlayerSettingsManager

设置管理器。

```java
// 获取实例
static PlayerSettingsManager getInstance(Context context)

// 播放内核
void setPlayerEngine(String engine)
String getPlayerEngine()

// 播放模式
void setPlayMode(String mode)  // "sequential", "single_loop", "play_pause"
String getPlayMode()

// 倍速设置
void setLongPressSpeed(float speed)
float getLongPressSpeed()

// 跳过片头片尾
void setSkipOpening(int seconds)
int getSkipOpening()
void setSkipEnding(int seconds)
int getSkipEnding()

// 弹幕设置
void setDanmakuTextSize(float size)
void setDanmakuSpeed(float speed)
void setDanmakuAlpha(float alpha)

// 记忆播放设置（默认关闭）
void setMemoryPlayEnabled(boolean enabled)
boolean isMemoryPlayEnabled()

// 下载功能开关
void setDownloadEnabled(boolean enabled)
boolean isDownloadEnabled()

// 底部进度条
void setBottomProgressEnabled(boolean enabled)
boolean isBottomProgressEnabled()
```

---

## PlayerConstants

常量定义。

```java
// 播放内核
String ENGINE_DEFAULT = "system"
String ENGINE_EXO = "exo"
String ENGINE_IJK = "ijk"
String ENGINE_ALI = "ali"

// 播放状态
int STATE_IDLE = 0
int STATE_PREPARING = 1
int STATE_PREPARED = 2
int STATE_PLAYING = 3
int STATE_PAUSED = 4
int STATE_PLAYBACK_COMPLETED = 5
int STATE_ERROR = 6

// 播放器状态
int PLAYER_NORMAL = 10
int PLAYER_FULL_SCREEN = 11
```

---

## 视频下载 API

OrangePlayer 提供了强大的下载管理功能，支持直接下载 MP4、M3U8，并提供了统一的 API 以及完善的 UI（包括下载管理弹窗和多选集下载面板）。核心 API 封装在 `SimpleDownloadManager` 中。

### 基础调用

通过单例模式获取下载管理器：

```java
import com.orange.playerlibrary.download.SimpleDownloadManager;

SimpleDownloadManager downloadManager = SimpleDownloadManager.getInstance(context);
```

### 核心方法

#### startDownload()

强制开始一个新的下载任务。

```java
long startDownload(String url, String title, String description)
```

**参数：**
- `url` - 视频地址（支持 MP4、FLV、M3U8 自动解析合并）
- `title` - 视频标题（将作为最终保存的文件名）
- `description` - 描述信息（可选）

#### startDownloadWithLocalCheck()

带本地检查的安全下载（推荐）。如果在下载中或已下载完成，会直接 Toast 提示拦截。

```java
String startDownloadWithLocalCheck(String url, String title)
```

**返回值：**
- 如果已下载，返回本地绝对路径（可直接传给播放器播放）。
- 如果未下载或正在下载，返回 `null`。

#### getLocalVideoPath()

查询某个视频是否已下载完成。

```java
String getLocalVideoPath(String url)
```

#### isDownloading()

查询某个视频是否正在下载中。

```java
boolean isDownloading(String url)
```

### 全局下载开关控制

你可以通过 `PlayerSettingsManager` 随时禁用或启用播放器内置的下载功能。禁用后，播放器右上角的下载按钮将被拦截并给出提示。

```java
PlayerSettingsManager settingsManager = PlayerSettingsManager.getInstance(context);

// 禁用下载功能
settingsManager.setDownloadEnabled(false);

// 检查当前下载功能状态
boolean isEnabled = settingsManager.isDownloadEnabled();
```

### 选集批量下载

当播放器设置了选集列表 (`setVideoList()`) 时，点击下载按钮会自动弹出“下载选集”面板。用户可以在面板中进行多选。
如果在业务层需要手动触发选集下载面板，可调用 `VideoEventManager`：

```java
if (mController != null && mController.getVideoEventManager() != null) {
    // 这将自动调起包含多选、已下载状态识别的选集下载面板
    mController.getVideoEventManager().showDownloadPlaylistDialog();
}
```

---

## 监听器

### OnStateChangeListener

```java
interface OnStateChangeListener {
    void onPlayerStateChanged(int playerState);
    void onPlayStateChanged(int playState);
}

// 使用
videoView.addOnStateChangeListener(new OnStateChangeListener() {
    @Override
    public void onPlayerStateChanged(int playerState) {
        // PLAYER_NORMAL, PLAYER_FULL_SCREEN
    }
    
    @Override
    public void onPlayStateChanged(int playState) {
        // STATE_IDLE, STATE_PLAYING, STATE_PAUSED, etc.
    }
});
```

### OnPlayCompleteListener

```java
interface OnPlayCompleteListener {
    void onPlayComplete();
}
```

### OnProgressListener

```java
interface OnProgressListener {
    void onProgress(int duration, int position);
}
```

---

## UI组件访问

OrangePlayer 提供了完整的 UI 组件访问 API，允许用户自定义控件样式、隐藏/显示控件、替换图标资源等。

### 获取组件

通过 `OrangevideoView` 获取各个组件：

```java
// 获取标题栏组件
TitleView titleView = videoView.getTitleView();

// 获取控制栏组件
VodControlView controlView = videoView.getVodControlView();

// 获取准备视图组件
PrepareView prepareView = videoView.getPrepareView();

// 获取完成视图组件
CompleteView completeView = videoView.getCompleteView();

// 获取错误视图组件
ErrorView errorView = videoView.getErrorView();
```

### TitleView 控件

标题栏包含返回按钮、标题文本、设置按钮、投屏按钮、小窗按钮、电量图标、时间文本等。

#### 获取控件

```java
TitleView titleView = videoView.getTitleView();

// 按钮控件
ImageView backButton = titleView.getBackButton();
TextView titleText = titleView.getTitleTextView();
ImageView settingsButton = titleView.getSettingsButton();
ImageView castButton = titleView.getCastButton();
ImageView windowButton = titleView.getWindowButton();
ImageView timerButton = titleView.getTimerButton();
ImageView sniffingButton = titleView.getSniffingButton();

// 电量与时间
ImageView batteryIcon = titleView.getBatteryIcon();
TextView sysTimeText = titleView.getSysTimeText();
LinearLayout batteryTimeContainer = titleView.getBatteryTimeContainer();
ImageView liveIndicator = titleView.getLiveIndicator();
```

#### 便捷方法

```java
// 设置电量图标可见性
titleView.setBatteryVisible(true);

// 设置系统时间可见性
titleView.setSysTimeVisible(true);

// 设置电量和时间区域整体可见性
titleView.setBatteryTimeVisible(true);

// 自定义电池图标
titleView.setBatteryIconResource(R.drawable.custom_battery);

// 设置直播标识可见性
titleView.setLiveVisible(true);
```

### VodControlView 控件

控制栏包含播放按钮、进度条、时间文本、全屏按钮、倍速按钮、选集按钮、弹幕控件等。

#### 获取控件

```java
VodControlView controlView = videoView.getVodControlView();

// 播放控制
ImageView playButton = controlView.getPlayButton();
ImageView fullScreenButton = controlView.getFullScreenButton();
SeekBar progressBar = controlView.getProgressBar();
ProgressBar bottomProgress = controlView.getBottomProgressBar();

// 时间显示
TextView currentTimeText = controlView.getCurrentTimeText();
TextView totalTimeText = controlView.getTotalTimeText();

// 功能按钮
TextView speedButton = controlView.getSpeedControlButton();
TextView episodeButton = controlView.getEpisodeSelectButton();
TextView skipButton = controlView.getSkipButton();
ImageView subtitleButton = controlView.getSubtitleToggleButton();
ImageView playNextButton = controlView.getPlayNextButton();
ImageView lockButton = controlView.getLockButton();

// 弹幕控件
ImageView danmuToggleButton = controlView.getDanmuToggle();
ImageView danmuSetButton = controlView.getDanmuSet();
EditText danmuInput = controlView.getDanmuInput();
LinearLayout danmuContainer = controlView.getDanmuContainer();

// 容器
LinearLayout bottomContainer = controlView.getBottomContainer();
```

#### 便捷方法

```java
// 设置按钮图标
controlView.setPlayButtonIcon(R.drawable.custom_play);
controlView.setFullScreenButtonIcon(R.drawable.custom_fullscreen);

// 设置按钮可见性
controlView.setSpeedButtonVisible(false);
controlView.setEpisodeButtonVisible(true);
controlView.setSkipButtonVisible(false);
controlView.setSubtitleButtonVisible(true);
controlView.setPlayNextButtonVisible(true);
controlView.setLockButtonVisible(true);
controlView.setBottomProgressVisible(false);

// 设置弹幕区域可见性
controlView.setDanmuContainerVisible(false);
```

### PrepareView 控件

准备视图显示缩略图、开始播放按钮、加载进度、网络警告等。

#### 获取控件

```java
PrepareView prepareView = videoView.getPrepareView();

ImageView thumb = prepareView.getThumb();           // 缩略图
ImageView startPlay = prepareView.getStartPlay();   // 开始播放按钮
ProgressBar loadingProgress = prepareView.getLoadingProgress();  // 加载进度
```

### CompleteView 控件

完成视图显示重播按钮和退出全屏按钮。

#### 获取控件

```java
CompleteView completeView = videoView.getCompleteView();

ImageView replayButton = completeView.getReplayButton();
ImageView stopFullscreenButton = completeView.getStopFullscreenButton();
```

### ErrorView 控件

错误视图显示重试按钮、设置按钮和退出全屏按钮。

#### 获取控件

```java
ErrorView errorView = videoView.getErrorView();

ImageView retryButton = errorView.getRetryButton();
ImageView settingsButton = errorView.getSettingsButton();
ImageView stopFullscreenButton = errorView.getStopFullscreenButton();
```

### 使用示例

#### 自定义播放器UI

```java
// 隐藏倍速按钮
videoView.getVodControlView().setSpeedButtonVisible(false);

// 隐藏选集按钮
videoView.getVodControlView().setEpisodeButtonVisible(false);

// 自定义播放按钮图标
videoView.getVodControlView().setPlayButtonIcon(R.drawable.ic_custom_play);

// 自定义标题文本样式
TextView titleText = videoView.getTitleView().getTitleTextView();
titleText.setTextColor(Color.RED);
titleText.setTextSize(18);

// 隐藏电量显示
videoView.getTitleView().setBatteryVisible(false);
```

#### 动态控制按钮显示

```java
// 根据视频类型控制按钮显示
if (isLiveStream) {
    // 直播模式：隐藏进度条、选集、倍速
    videoView.getVodControlView().getProgressBar().setVisibility(View.GONE);
    videoView.getVodControlView().setEpisodeButtonVisible(false);
    videoView.getVodControlView().setSpeedButtonVisible(false);
    // 显示直播标识
    videoView.getTitleView().setLiveVisible(true);
} else {
    // 点播模式：显示所有控件
    videoView.getVodControlView().getProgressBar().setVisibility(View.VISIBLE);
    videoView.getVodControlView().setEpisodeButtonVisible(true);
    videoView.getVodControlView().setSpeedButtonVisible(true);
    videoView.getTitleView().setLiveVisible(false);
}
```

## M3U8去广告功能

播放器支持自动检测并移除HTTP m3u8视频中的广告片段。

### 功能特点

- 自动检测HTTP m3u8链接
- 支持多种广告检测方式：路径模式变化、DISCONTINUITY标记、短片段组
- 支持开头广告、中间广告、结尾广告检测
- 本地缓存处理结果，避免重复请求
- 请求失败自动降级播放原始URL
- 播放错误时自动清除缓存

### 开启/关闭去广告

```java
// 获取M3U8去广告管理器
M3U8AdManager adManager = M3U8AdManager.getInstance(context);

// 开启去广告功能（默认开启）
adManager.setEnabled(true);

// 关闭去广告功能
adManager.setEnabled(false);

// 检查是否启用
boolean isEnabled = adManager.isEnabled();
```

### 清除缓存

```java
// 清除所有缓存
adManager.clearCache();

// 清除特定URL的缓存
adManager.clearCacheForUrl("https://example.com/video.m3u8");
```

### 手动处理URL

如果需要在播放前手动处理m3u8 URL：

```java
// 异步处理
adManager.processVideoUrl(videoUrl, new M3U8AdManager.Callback() {
    @Override
    public void onResult(String playUrl, boolean isLocalFile, int adSegmentsRemoved, String message) {
        // playUrl: 可用于播放的URL
        // isLocalFile: 是否是本地文件
        // adSegmentsRemoved: 移除的广告片段数
        // message: 处理结果消息
        
        // 使用playUrl播放
        videoView.setUp(playUrl, true, "视频标题");
        videoView.startPlayLogic();
    }
});

// 同步处理（阻塞当前线程，建议在后台线程调用）
String playUrl = adManager.processVideoUrlSync(videoUrl, 5000); // 5秒超时
```

### 检查URL类型

```java
// 检查URL是否是HTTP m3u8
if (M3U8AdRemover.isHttpM3U8(url)) {
    // 是HTTP m3u8链接，会自动进行去广告处理
}
```

### 使用示例

```java
// 在播放前关闭去广告功能
M3U8AdManager.getInstance(this).setEnabled(false);

// 播放视频
videoView.setUp(videoUrl, true, "视频标题");
videoView.startPlayLogic();

// 播放后重新开启
M3U8AdManager.getInstance(this).setEnabled(true);
```
