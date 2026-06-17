# 进度条拖动预览功能增强

## 概述

优化了进度条拖动时的视频预览功能，使用 ScreenshotManager 截图功能来提供更准确、更快速的预览体验。

## 改进方案

### 原有实现

之前使用 Glide 的 `.frame(timeMs)` 方法从视频文件中提取指定时间点的帧：

**优点：**
- 可以预览任意时间点
- 不影响当前播放

**缺点：**
- 依赖 Glide 的视频解码能力，某些格式可能不支持
- 性能开销较大（需要解码视频文件）
- 某些播放内核（Exo/系统）在 SurfaceControl 模式下可能失败
- 无法显示字幕、弹幕等叠加内容

### 新实现（混合方案）

采用智能混合策略，根据拖动位置自动选择最佳方案：

#### 方案 1：实时截图（拖动位置接近当前播放位置）

当拖动位置距离当前播放位置 **±3秒** 内时，使用 ScreenshotManager 截取当前播放画面：

```java
// 判断是否接近当前播放位置
long currentPosition = videoView.getCurrentPositionWhenPlaying();
long positionDiff = Math.abs(timeMs - currentPosition);

if (positionDiff < 3000) {
    // 使用实时截图
    loadPreviewWithScreenshot(videoView, timeMs);
}
```

**优点：**
- ✅ 速度快（直接从渲染表面获取）
- ✅ 支持所有播放内核和视频格式
- ✅ 显示实际播放画面（包括字幕、弹幕等）
- ✅ 画质更好（直接截取渲染结果）

**适用场景：**
- 用户在当前播放位置附近微调进度
- 快速预览前后几秒的内容

#### 方案 2：视频帧提取（拖动到远离当前位置）

当拖动位置距离当前播放位置 **超过 3秒** 时，使用 Glide 从视频文件提取帧：

```java
else {
    // 使用 Glide 提取视频帧
    loadPreviewImageWithGlide(timeMs);
}
```

**优点：**
- ✅ 可以预览任意时间点
- ✅ 不影响当前播放状态
- ✅ 有缓存机制

**适用场景：**
- 用户大幅度拖动进度条
- 跳转到视频的其他部分

### 错误处理

如果 ScreenshotManager 截图失败，会自动回退到 Glide 方案：

```java
@Override
public void onError(String error) {
    // 截图失败，回退到 Glide 方案
    android.util.Log.w(TAG, "Screenshot failed, fallback to Glide: " + error);
    loadPreviewImageWithGlide(timeMs);
}
```

## 技术实现

### 核心方法

#### loadPreviewImage(long timeMs)

主入口方法，根据拖动位置选择预览方案：

```java
private void loadPreviewImage(long timeMs) {
    OrangevideoView videoView = getVideoView();
    long currentPosition = videoView != null ? videoView.getCurrentPositionWhenPlaying() : -1;
    long positionDiff = Math.abs(timeMs - currentPosition);
    
    // 如果拖动位置接近当前播放位置（3秒内），使用实时截图
    if (videoView != null && positionDiff < 3000) {
        loadPreviewWithScreenshot(videoView, timeMs);
    } else {
        // 否则使用 Glide 提取视频帧
        loadPreviewImageWithGlide(timeMs);
    }
}
```

#### loadPreviewWithScreenshot(OrangevideoView videoView, long timeMs)

使用 ScreenshotManager 截取当前画面：

```java
private void loadPreviewWithScreenshot(OrangevideoView videoView, long timeMs) {
    com.orange.playerlibrary.screenshot.ScreenshotManager screenshotManager = 
        new com.orange.playerlibrary.screenshot.ScreenshotManager(getContext(), videoView);
    
    screenshotManager.takeScreenshot(false, new ScreenshotCallback() {
        @Override
        public void onSuccess(Bitmap bitmap, String message) {
            // 缩放并显示预览图
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(
                bitmap, PREVIEW_WIDTH, PREVIEW_HEIGHT, true);
            mPreviewImage.setImageBitmap(scaledBitmap);
            
            // 回收原始 Bitmap
            if (bitmap != scaledBitmap) {
                bitmap.recycle();
            }
        }
        
        @Override
        public void onError(String error) {
            // 回退到 Glide 方案
            loadPreviewImageWithGlide(timeMs);
        }
    });
}
```

#### loadPreviewImageWithGlide(long timeMs)

使用 Glide 从视频文件提取帧（回退方案）：

```java
private void loadPreviewImageWithGlide(long timeMs) {
    RequestOptions options = new RequestOptions()
        .frame(timeMs * 1000)  // 微秒
        .override(PREVIEW_WIDTH, PREVIEW_HEIGHT)
        .fitCenter()
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC);
    
    Glide.with(context)
        .asBitmap()
        .load(sVideoUrl)
        .apply(options)
        .into(mCurrentPreviewTarget);
}
```

## 性能优化

### 1. 节流机制

预览图加载有 500ms 的节流间隔，避免频繁加载：

```java
private static final long PREVIEW_THROTTLE_MS = 500;

if (currentTime - mLastPreviewTime > PREVIEW_THROTTLE_MS) {
    cancelPreviewLoad();
    loadPreviewImage(position);
    mLastPreviewTime = currentTime;
}
```

### 2. 延迟显示

拖动开始后延迟 400ms 才显示预览，避免快速点击时闪现：

```java
private static final long PREVIEW_DELAY_MS = 400;

mDelayHandler.postDelayed(mShowPreviewRunnable, PREVIEW_DELAY_MS);
```

### 3. Bitmap 回收

及时回收不再使用的 Bitmap，避免内存泄漏：

```java
if (bitmap != scaledBitmap) {
    bitmap.recycle();
}
```

### 4. 取消加载

停止拖动时立即取消正在进行的加载任务：

```java
private void cancelPreviewLoad() {
    if (mCurrentPreviewTarget != null) {
        Glide.with(getContext()).clear(mCurrentPreviewTarget);
        mCurrentPreviewTarget = null;
    }
}
```

## 用户体验

### 视觉效果

预览图切换时有平滑的淡入和缩放动画：

```java
private void animatePreviewChange() {
    AnimationSet animationSet = new AnimationSet(true);
    animationSet.setDuration(150);
    
    AlphaAnimation fadeIn = new AlphaAnimation(0.7f, 1.0f);
    ScaleAnimation scale = new ScaleAnimation(
        0.97f, 1.0f, 0.97f, 1.0f,
        Animation.RELATIVE_TO_SELF, 0.5f,
        Animation.RELATIVE_TO_SELF, 0.5f);
    
    animationSet.addAnimation(fadeIn);
    animationSet.addAnimation(scale);
    mPreviewContainer.startAnimation(animationSet);
}
```

### 位置跟随

预览窗口跟随拖动位置移动，始终显示在进度条上方：

```java
private void updatePreviewPosition(SeekBar seekBar, int progress) {
    int thumbPosition = (seekBarWidth - seekBarPaddingLeft - seekBarPaddingRight) 
        * progress / seekBar.getMax();
    int thumbCenterX = seekBarLocation[0] - containerLocation[0] 
        + seekBarPaddingLeft + thumbPosition;
    
    int leftPosition = thumbCenterX - previewWidth / 2;
    // 边界检查，确保不超出屏幕
    leftPosition = Math.max(0, Math.min(leftPosition, parentWidth - previewWidth));
    
    params.leftMargin = leftPosition;
    mPreviewContainer.setLayoutParams(params);
}
```

## 兼容性

### 播放内核支持

| 内核 | ScreenshotManager | Glide Frame | 推荐方案 |
|------|------------------|-------------|---------|
| 系统播放器 | ✅ 支持 | ✅ 支持 | 混合 |
| ExoPlayer | ✅ 支持 | ✅ 支持 | 混合 |
| IJK | ✅ 支持 | ✅ 支持 | 混合 |
| 阿里云 | ✅ 支持 | ⚠️ 部分支持 | 优先截图 |

### 渲染模式支持

| 渲染模式 | ScreenshotManager | Glide Frame |
|---------|------------------|-------------|
| TextureView | ✅ 完美支持 | ✅ 支持 |
| SurfaceView | ✅ 支持（Android N+） | ✅ 支持 |
| SurfaceControl | ⚠️ 需切换到 TextureView | ✅ 支持 |

## 配置选项

### 启用/禁用预览

```java
// 全局启用/禁用预览功能
VodControlView.setPreviewEnabled(true);  // 默认启用
```

### 调整阈值

可以修改 3秒 的阈值来调整截图和帧提取的切换点：

```java
// 在 VodControlView.java 中修改
private static final long SCREENSHOT_THRESHOLD_MS = 3000;  // 3秒

// 使用时
if (positionDiff < SCREENSHOT_THRESHOLD_MS) {
    loadPreviewWithScreenshot(videoView, timeMs);
}
```

## 故障排查

### 预览不显示

1. 检查是否在全屏模式（预览仅在全屏时显示）
2. 检查拖动时间是否超过 400ms（延迟显示机制）
3. 查看日志是否有错误信息

### 预览图模糊

1. 调整预览尺寸常量：
   ```java
   private static final int PREVIEW_WIDTH = 320;
   private static final int PREVIEW_HEIGHT = 180;
   ```

2. 使用高质量截图：
   ```java
   screenshotManager.takeScreenshot(true, callback);  // true = 高质量
   ```

### 性能问题

1. 增加节流间隔：
   ```java
   private static final long PREVIEW_THROTTLE_MS = 800;  // 从 500ms 增加到 800ms
   ```

2. 禁用动画：
   ```java
   // 注释掉 animatePreviewChange() 调用
   ```

## 未来改进

### 可能的优化方向

1. **预加载关键帧**
   - 在视频加载时预先提取关键帧
   - 存储在内存缓存中
   - 拖动时直接显示缓存的关键帧

2. **智能缓存策略**
   - 根据用户拖动习惯预测可能查看的位置
   - 提前加载附近的帧

3. **更精确的时间同步**
   - 在截图前微调 seek 位置
   - 确保截图时间点更准确

4. **支持缩略图轨道**
   - 在进度条上显示缩略图轨道
   - 类似 YouTube 的预览效果

## 相关文件

- `palyerlibrary/src/main/java/com/orange/playerlibrary/component/VodControlView.java` - 主实现
- `palyerlibrary/src/main/java/com/orange/playerlibrary/screenshot/ScreenshotManager.java` - 截图管理器
- `palyerlibrary/src/main/res/layout/orange_layout_vod_control.xml` - 预览窗口布局

## 参考资料

- [ScreenshotManager 文档](../API.md#screenshotmanager)
- [Glide 视频帧提取](https://bumptech.github.io/glide/doc/options.html#video-frames)
- [Android PixelCopy API](https://developer.android.com/reference/android/view/PixelCopy)
