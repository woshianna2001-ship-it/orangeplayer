# Orange Player Library

一个功能强大的 Android 视频播放器库，支持多内核切换、无缝全屏切换、智能内核选择等特性。

## 目录

- [Orange Player Library](#orange-player-library)
  - [目录](#目录)
  - [播放器架构图](#播放器架构图)
  - [内核类型常量](#内核类型常量)
  - [公开 API 列表](#公开-api-列表)
    - [OrangevideoView 播放器视图](#orangevideoview-播放器视图)
      - [播放器内核设置与获取](#播放器内核设置与获取)
      - [播放控制](#播放控制)
      - [URL 设置](#url-设置)
      - [全屏控制](#全屏控制)
      - [渲染设置](#渲染设置)
      - [状态监听](#状态监听)
      - [控制器设置](#控制器设置)
      - [缩略图](#缩略图)
      - [播放进度保存/恢复](#播放进度保存恢复)
      - [跳过片头片尾](#跳过片头片尾)
      - [直播相关](#直播相关)
      - [视频嗅探](#视频嗅探)
      - [画中画](#画中画)
      - [其他](#其他)
    - [PlayerSettingsManager 设置管理器](#playersettingsmanager-设置管理器)
      - [播放器内核设置](#播放器内核设置)
      - [播放设置](#播放设置)
      - [跳过片头片尾](#跳过片头片尾-1)
      - [UI 设置](#ui-设置)
      - [弹幕设置](#弹幕设置)
      - [字幕设置](#字幕设置)
      - [嗅探设置](#嗅探设置)
    - [播放器管理器类](#播放器管理器类)
    - [播放器内核对象类型](#播放器内核对象类型)
  - [使用示例](#使用示例)
    - [1. 获取播放器内核对象](#1-获取播放器内核对象)
    - [2. 获取播放器管理器](#2-获取播放器管理器)
    - [3. 切换播放器内核](#3-切换播放器内核)
    - [4. 检查内核可用性](#4-检查内核可用性)
    - [5. 监听内核变更](#5-监听内核变更)
  - [默认内核选择逻辑](#默认内核选择逻辑)
  - [内核检测机制](#内核检测机制)
  - [依赖配置](#依赖配置)
  - [文件结构](#文件结构)

## 播放器架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           OrangevideoView (主入口)                           │
│                     d:\android\projecet_iade\orangeplayer\                   │
│                   palyerlibrary\src\main\java\com\orange\                    │
│                          playerlibrary\OrangevideoView.java                  │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    │                 │                 │
                    ▼                 ▼                 ▼
┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐
│  PlayerSettingsManager │  │  VideoEventManager   │  │ OrangeVideoController│
│   (设置管理器)          │  │   (事件管理器)        │  │    (控制器)           │
│                      │  │                      │  │                      │
│ - getPlayerEngine()  │  │ - selectEngine()     │  │ - UI控制              │
│ - setPlayerEngine()  │  │ - setupEngineButtons()│  │ - 手势处理            │
│ - isAutoSelectEngine()│  │ - updateEngineButtonsUI│  │ - 进度条              │
└──────────────────────┘  └──────────────────────┘  └──────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          PlayerFactory (播放器工厂)                          │
│                        GSYVideoPlayer 框架提供                                │
└─────────────────────────────────────────────────────────────────────────────┘
                    │
    ┌───────────────┼───────────────┬───────────────┐
    │               │               │               │
    ▼               ▼               ▼               ▼
┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐
│ExoPlayer   │ │IJKPlayer   │ │系统播放器   │ │阿里云播放器 │
│Manager     │ │Manager     │ │Manager     │ │Manager     │
│            │ │            │ │            │ │            │
│OrangeExo   │ │OrangeIjk   │ │OrangeSystem│ │AliPlayer   │
│PlayerManager│ │PlayerManager│ │PlayerManager│ │Manager    │
└────────────┘ └────────────┘ └────────────┘ └────────────┘
    │               │               │               │
    ▼               ▼               ▼               ▼
┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐
│IjkExo2     │ │IjkMedia    │ │MediaPlayer │ │AliPlayer   │
│MediaPlayer │ │Player      │ │(Android)   │ │(阿里云SDK) │
│(ExoPlayer) │ │(IJK SDK)   │ │            │ │            │
└────────────┘ └────────────┘ └────────────┘ └────────────┘
```

## 内核类型常量

| 常量 | 值 | 说明 |
|------|-----|------|
| `PlayerConstants.ENGINE_EXO` | `"exo"` | ExoPlayer 内核 |
| `PlayerConstants.ENGINE_IJK` | `"ijk"` | IJKPlayer 内核 |
| `PlayerConstants.ENGINE_DEFAULT` | `"default"` | 系统播放器内核 |
| `PlayerConstants.ENGINE_ALI` | `"ali"` | 阿里云播放器内核 |

## 公开 API 列表

### OrangevideoView 播放器视图

#### 播放器内核设置与获取

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `getPlayerManager(Class<T>)` | 管理器类型 | T | 泛型获取播放器管理器 |
| `getPlayerManager()` | - | IPlayerManager | 获取原始管理器接口 |
| `getMediaPlayer(Class<T>)` | 播放器类型 | T | 泛型获取内核对象 |
| `getMediaPlayer()` | - | IMediaPlayer | 获取原始内核接口 |
| `getCurrentEngineType()` | - | String | 获取当前内核类型 |
| `isExoPlayerEngine()` | - | boolean | 是否 ExoPlayer 内核 |
| `isIjkPlayerEngine()` | - | boolean | 是否 IJK 内核 |
| `isSystemPlayerEngine()` | - | boolean | 是否系统播放器内核 |
| `isAliPlayerEngine()` | - | boolean | 是否阿里云内核 |
| `isEngineAvailable(String)` | 内核类型 | boolean | 检查内核是否可用 |
| `selectPlayerFactory(String)` | 内核类型 | void | 切换播放器内核 |

#### 播放控制

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `start()` | - | void | 开始播放 |
| `pause()` | - | void | 暂停播放 |
| `resume()` | - | void | 恢复播放 |
| `release()` | - | void | 释放播放器 |
| `seekTo(long)` | 位置(ms) | void | 跳转到指定位置 |
| `seekTo(int)` | 位置(ms) | void | 跳转到指定位置 |
| `replay(boolean)` | 是否重置位置 | void | 重播 |
| `isPlaying()` | - | boolean | 是否正在播放 |
| `getDuration()` | - | long | 获取视频总时长 |
| `getCurrentPosition()` | - | long | 获取当前播放位置 |
| `getCurrentPositionWhenPlaying()` | - | long | 获取播放中当前位置 |
| `setSpeed(float)` | 倍速 | void | 设置播放倍速 |
| `getSpeed()` | - | float | 获取当前倍速 |
| `setVolume(float)` | 音量 | void | 设置音量(0-1) |
| `setMute(boolean)` | 是否静音 | void | 设置静音 |
| `isMute()` | - | boolean | 是否静音 |

#### URL 设置

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `setUp(String, boolean, String)` | url, 缓存, 标题 | boolean | 设置播放地址 |
| `setUp(String, boolean, File, String)` | url, 缓存, 缓存路径, 标题 | boolean | 设置播放地址 |
| `setUrl(String)` | url | void | 设置播放地址 |
| `setUrl(String, Map)` | url, headers | void | 设置播放地址和请求头 |
| `getUrl()` | - | String | 获取播放地址 |
| `getVideoUrl()` | - | String | 获取视频地址 |
| `getVideoHeaders()` | - | Map | 获取请求头 |

#### 全屏控制

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `startFullScreen()` | - | void | 进入全屏 |
| `stopFullScreen()` | - | void | 退出全屏 |
| `isFullScreen()` | - | boolean | 是否全屏 |
| `startPortraitFullScreen()` | - | void | 进入竖屏全屏 |
| `stopPortraitFullScreen()` | - | void | 退出竖屏全屏 |
| `isPortraitFullScreen()` | - | boolean | 是否竖屏全屏 |
| `setSmartFullscreenEnabled(boolean)` | 是否启用 | void | 设置智能全屏 |
| `isSmartFullscreenEnabled()` | - | boolean | 是否启用智能全屏 |

#### 渲染设置

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `setRenderMode(boolean)` | 是否TextureView | void | 设置渲染模式 |
| `isTextureViewMode()` | - | boolean | 是否TextureView模式 |
| `getVideoWidth()` | - | int | 获取视频宽度 |
| `getVideoHeight()` | - | int | 获取视频高度 |
| `refreshVideoShowType()` | - | void | 刷新视频显示模式 |

#### 状态监听

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `addOnStateChangeListener(OnStateChangeListener)` | 监听器 | void | 添加状态监听 |
| `removeOnStateChangeListener(OnStateChangeListener)` | 监听器 | void | 移除状态监听 |
| `clearOnStateChangeListeners()` | - | void | 清除所有监听 |
| `getPlayState()` | - | int | 获取播放状态 |
| `getPlayerState()` | - | int | 获取播放器状态 |
| `setOnProgressListener(OnProgressListener)` | 监听器 | void | 设置进度监听 |
| `setOnPlayCompleteListener(OnPlayCompleteListener)` | 监听器 | void | 设置播放完成监听 |

#### 控制器设置

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `setVideoController(OrangeVideoController)` | 控制器 | void | 设置控制器 |
| `showController()` | - | void | 显示控制器 |
| `hideController()` | - | void | 隐藏控制器 |
| `isControllerShowing()` | - | boolean | 控制器是否显示 |
| `setControllerVisibilityEnabled(boolean)` | 是否启用 | void | 设置控制器可见性 |
| `isControllerVisibilityEnabled()` | - | boolean | 是否启用控制器可见性 |

#### 缩略图

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `setAutoThumbnailEnabled(boolean)` | 是否启用 | void | 设置自动缩略图 |
| `isAutoThumbnailEnabled()` | - | boolean | 是否启用自动缩略图 |
| `setDefaultThumbnail(Object)` | 缩略图 | void | 设置默认缩略图 |
| `getDefaultThumbnail()` | - | Object | 获取默认缩略图 |
| `getVideoFirstFrameAsync(ThumbnailCallback)` | 回调 | void | 异步获取首帧 |
| `getFrameAtTimeAsync(long, ThumbnailCallback)` | 时间, 回调 | void | 异步获取指定帧 |

#### 播放进度保存/恢复

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `savePlaybackProgress()` | - | void | 保存播放进度 |
| `restorePlaybackProgress()` | - | boolean | 恢复播放进度 |
| `getSavedProgress()` | - | long | 获取已保存进度 |
| `hasSavedProgress()` | - | boolean | 是否有保存进度 |
| `clearSavedProgress()` | - | void | 清除保存进度 |
| `getHistoryProgress()` | - | long | 获取历史进度 |
| `hasPlayHistory()` | - | boolean | 是否有播放历史 |

#### 跳过片头片尾

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `setSkipIntroTime(long)` | 时间(ms) | void | 设置跳过片头时间 |
| `setSkipIntroSeconds(int)` | 秒数 | void | 设置跳过片头秒数 |
| `getSkipIntroTime()` | - | long | 获取跳过片头时间 |
| `setSkipIntroEnabled(boolean)` | 是否启用 | void | 设置跳过片头开关 |
| `isSkipIntroEnabled()` | - | boolean | 是否启用跳过片头 |
| `setSkipOutroTime(long)` | 时间(ms) | void | 设置跳过片尾时间 |
| `setSkipOutroSeconds(int)` | 秒数 | void | 设置跳过片尾秒数 |
| `getSkipOutroTime()` | - | long | 获取跳过片尾时间 |
| `setSkipOutroEnabled(boolean)` | 是否启用 | void | 设置跳过片尾开关 |
| `isSkipOutroEnabled()` | - | boolean | 是否启用跳过片尾 |

#### 直播相关

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `isLiveVideo()` | - | boolean | 是否直播视频 |
| `setLiveVideo(boolean)` | 是否直播 | void | 设置直播模式 |
| `getNetSpeed()` | - | long | 获取网速(bytes/s) |
| `getTcpSpeed()` | - | long | 获取TCP网速 |
| `getNetSpeedText()` | - | String | 获取网速文本 |

#### 视频嗅探

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `isSniffing()` | - | boolean | 是否正在嗅探 |
| `startSniffing()` | - | void | 开始嗅探 |
| `startSniffing(String, Map)` | url, headers | void | 开始嗅探指定地址 |
| `stopSniffing()` | - | void | 停止嗅探 |

#### 画中画

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `setEnteringPiPMode(boolean)` | 是否进入 | void | 设置进入画中画模式 |
| `isEnteringPiPMode()` | - | boolean | 是否进入画中画模式 |
| `setKeepVideoPlaying(boolean)` | 是否保持 | void | 画中画时保持播放 |
| `isKeepVideoPlaying()` | - | boolean | 是否保持播放 |

#### 其他

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `setDebugLogCallback(DebugLogCallback)` | 回调 | void | 设置调试日志回调 |
| `enableOrangeComponents()` | - | void | 启用Orange组件 |
| `isUseOrangeComponents()` | - | boolean | 是否使用Orange组件 |
| `setLoadingIndicator(Indicator)` | 指示器 | void | 设置加载指示器 |
| `setAutoRotateOnFullscreen(boolean)` | 是否自动旋转 | void | 全屏时自动旋转 |
| `isAutoRotateOnFullscreen()` | - | boolean | 是否自动旋转 |
| `backFromFull(Context)` | 上下文 | boolean | 从全屏返回 |
| `releaseVideos()` | - | void | 释放所有视频 |

### PlayerSettingsManager 设置管理器

#### 播放器内核设置

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `setPlayerEngine(String)` | 内核类型 | void | 设置播放器内核 |
| `getPlayerEngine()` | - | String | 获取当前内核设置 |
| `hasUserSetEngine()` | - | boolean | 用户是否手动设置过 |
| `setEngineChangeListener(EngineChangeListener)` | 监听器 | void | 设置变更监听 |
| `setAutoSelectEngine(boolean)` | 是否启用 | void | 设置自动选择内核 |
| `isAutoSelectEngine()` | - | boolean | 是否启用自动选择 |

#### 播放设置

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `setLongPressSpeed(float)` | 倍速 | void | 设置长按倍速 |
| `getLongPressSpeed()` | - | float | 获取长按倍速 |
| `setPlayMode(String)` | 模式 | void | 设置播放模式 |
| `getPlayMode()` | - | String | 获取播放模式 |
| `setVideoScale(String)` | 缩放类型 | void | 设置画面比例 |
| `getVideoScale()` | - | String | 获取画面比例 |
| `setDecodeMode(String)` | 解码方式 | void | 设置解码方式 |
| `getDecodeMode()` | - | String | 获取解码方式 |
| `isHardwareDecode()` | - | boolean | 是否硬件解码 |

#### 跳过片头片尾

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `setSkipOpening(int)` | 毫秒 | void | 设置跳过片头 |
| `getSkipOpening()` | - | int | 获取跳过片头 |
| `setSkipEnding(int)` | 毫秒 | void | 设置跳过片尾 |
| `getSkipEnding()` | - | int | 获取跳过片尾 |

#### UI 设置

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `setBottomProgressEnabled(boolean)` | 是否启用 | void | 设置底部进度条 |
| `isBottomProgressEnabled()` | - | boolean | 是否启用底部进度条 |
| `setAutoRotateEnabled(boolean)` | 是否启用 | void | 设置自动旋转 |
| `isAutoRotateEnabled()` | - | boolean | 是否启用自动旋转 |
| `setSmartFullscreenEnabled(boolean)` | 是否启用 | void | 设置智能全屏 |
| `isSmartFullscreenEnabled()` | - | boolean | 是否启用智能全屏 |

#### 弹幕设置

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `setDanmakuEnabled(boolean)` | 是否启用 | void | 设置弹幕开关 |
| `isDanmakuEnabled()` | - | boolean | 是否启用弹幕 |
| `setDanmakuTextSize(float)` | 大小 | void | 设置弹幕字体大小 |
| `getDanmakuTextSize()` | - | float | 获取弹幕字体大小 |
| `setDanmakuSpeed(float)` | 速度 | void | 设置弹幕速度 |
| `getDanmakuSpeed()` | - | float | 获取弹幕速度 |
| `setDanmakuAlpha(float)` | 透明度 | void | 设置弹幕透明度 |
| `getDanmakuAlpha()` | - | float | 获取弹幕透明度 |

#### 字幕设置

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `setSubtitleEnabled(boolean)` | 是否启用 | void | 设置字幕开关 |
| `isSubtitleEnabled()` | - | boolean | 是否启用字幕 |
| `setSubtitleSize(float)` | 大小 | void | 设置字幕大小 |
| `getSubtitleSize()` | - | float | 获取字幕大小 |
| `setSubtitleUrlForVideo(String, String)` | 视频URL, 字幕URL | void | 设置视频字幕URL |
| `getSubtitleUrlForVideo(String)` | 视频URL | String | 获取视频字幕URL |
| `setSubtitleLocalForVideo(String, String)` | 视频URL, 本地URI | void | 设置本地字幕 |
| `getSubtitleLocalForVideo(String)` | 视频URL | String | 获取本地字幕 |
| `clearSubtitleForVideo(String)` | 视频URL | void | 清除视频字幕 |

#### 嗅探设置

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `setSniffingAutoPlayEnabled(boolean)` | 是否启用 | void | 设置嗅探自动播放 |
| `isSniffingAutoPlayEnabled()` | - | boolean | 是否启用嗅探自动播放 |

### 播放器管理器类

| 类名 | 内核类型 | 源码位置 |
|------|----------|----------|
| `OrangeExoPlayerManager` | ExoPlayer | palyerlibrary/src/main/java/com/orange/playerlibrary/exo/OrangeExoPlayerManager.java |
| `OrangeIjkPlayerManager` | IJK | palyerlibrary/src/main/java/com/orange/playerlibrary/player/OrangeIjkPlayerManager.java |
| `OrangeSystemPlayerManager` | 系统 | palyerlibrary/src/main/java/com/orange/playerlibrary/player/OrangeSystemPlayerManager.java |

### 播放器内核对象类型

| 内核类型 | 管理器类 | 内核对象类 |
|----------|----------|------------|
| ExoPlayer | `OrangeExoPlayerManager` | `tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer` |
| IJK | `OrangeIjkPlayerManager` | `tv.danmaku.ijk.media.player.IjkMediaPlayer` |
| 系统 | `OrangeSystemPlayerManager` | `android.media.MediaPlayer` |
| 阿里云 | `AliPlayerManager` | `com.aliyun.player.AliPlayer` |

## 使用示例

### 1. 获取播放器内核对象

```java
// 获取 ExoPlayer 内核对象
tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer exoPlayer = 
    videoView.getMediaPlayer(tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer.class);
if (exoPlayer != null) {
    // 使用 ExoPlayer 特有功能
}

// 获取 IJK 内核对象
tv.danmaku.ijk.media.player.IjkMediaPlayer ijkPlayer = 
    videoView.getMediaPlayer(tv.danmaku.ijk.media.player.IjkMediaPlayer.class);
if (ijkPlayer != null) {
    // 使用 IJK 特有功能
}

// 获取系统播放器内核对象
android.media.MediaPlayer systemPlayer = 
    videoView.getMediaPlayer(android.media.MediaPlayer.class);
if (systemPlayer != null) {
    // 使用系统播放器特有功能
}
```

### 2. 获取播放器管理器

```java
// 获取 ExoPlayer 管理器
OrangeExoPlayerManager exoManager = 
    videoView.getPlayerManager(OrangeExoPlayerManager.class);
if (exoManager != null) {
    // 使用 ExoPlayer 管理器特有功能
}

// 获取 IJK 管理器
OrangeIjkPlayerManager ijkManager = 
    videoView.getPlayerManager(OrangeIjkPlayerManager.class);
```

### 3. 切换播放器内核

```java
// 方式1：通过设置管理器（推荐，会持久化）
PlayerSettingsManager.getInstance(context).setPlayerEngine(PlayerConstants.ENGINE_EXO);

// 方式2：直接切换（不持久化）
videoView.selectPlayerFactory(PlayerConstants.ENGINE_EXO);
```

### 4. 检查内核可用性

```java
// 检查 ExoPlayer 是否可用
if (videoView.isEngineAvailable(PlayerConstants.ENGINE_EXO)) {
    // ExoPlayer 可用
}

// 检查 IJK 是否可用（包括 SO 库）
if (videoView.isEngineAvailable(PlayerConstants.ENGINE_IJK)) {
    // IJK 可用
}
```

### 5. 监听内核变更

```java
PlayerSettingsManager.getInstance(context).setEngineChangeListener(newEngine -> {
    // 内核已变更，更新 UI
    updateEngineUI(newEngine);
});
```

## 默认内核选择逻辑

当用户未手动设置内核时，系统会智能选择默认内核：

```
优先级：
1. 用户手动设置的内核（最高优先级）
2. 智能检测可用内核：
   - ExoPlayer 可用 → 使用 ExoPlayer
   - ExoPlayer 不可用 → 使用系统播放器
```

**注意**：默认不再使用 IJK，因为很多项目可能不包含 IJK 依赖。

## 内核检测机制

| 内核 | 检测方式 |
|------|----------|
| ExoPlayer | 检测 `tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer` 或 `androidx.media3.exoplayer.ExoPlayer` |
| IJK | 检测 Java 类 + SO 库加载状态 |
| 系统 | 始终可用 |
| 阿里云 | 检测 `com.aliyun.player.AliPlayer` |

## 依赖配置

```gradle
// app/build.gradle
dependencies {
    // 播放器核心库
    implementation project(':palyerlibrary')
    
    // ExoPlayer 内核（可选）
    implementation project(':gsyVideoPlayer-exo_player2')
    
    // IJK 内核（可选，需要 SO 库）
    implementation project(':gsyVideoPlayer-armv64')  // ARM64
    implementation project(':gsyVideoPlayer-armv7a') // ARMv7
    implementation project(':gsyVideoPlayer-x86')    // x86
    implementation project(':gsyVideoPlayer-x86_64') // x86_64
}
```

## 文件结构

```
palyerlibrary/
├── src/main/java/com/orange/playerlibrary/
│   ├── OrangevideoView.java          # 主入口，播放器视图
│   ├── PlayerSettingsManager.java    # 设置管理器
│   ├── VideoEventManager.java        # 事件管理器
│   ├── OrangeVideoController.java    # 控制器
│   ├── PlayerConstants.java          # 常量定义
│   ├── exo/
│   │   └── OrangeExoPlayerManager.java  # ExoPlayer 管理器
│   ├── player/
│   │   ├── OrangeIjkPlayerManager.java  # IJK 管理器
│   │   └── OrangeSystemPlayerManager.java # 系统播放器管理器
│   └── utils/
│       └── PlayerEngineSelector.java    # 内核智能选择器
└── src/main/res/
    └── layout/
        └── setup_dialog.xml          # 设置对话框布局
```
