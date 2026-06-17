# 视频截图功能设计

## 功能概述
截取当前视频画面，保存到相册或分享。

## GSY 官方实现参考

### 关键代码
```java
// 获取截图
videoPlayer.taskShotPic(new GSYVideoShotListener() {
    @Override
    public void getBitmap(Bitmap bitmap) {
        // 处理截图 bitmap
        saveBitmapToGallery(bitmap);
    }
});

// 高清截图
videoPlayer.taskShotPic(listener, true);

// 保存到文件
videoPlayer.saveFrame(file, high, new GSYVideoShotSaveListener() {
    @Override
    public void result(boolean success, File file) {
        // 保存结果
    }
});
```

### 实现原理
- TextureView: 使用 `getBitmap()` 获取当前帧
- SurfaceView: 使用 PixelCopy API (Android N+)
- GLSurfaceView: 从 OpenGL 缓冲区读取像素

## 实现方案

### 1. 截图管理器 (ScreenshotManager)
```java
public class ScreenshotManager {
    private OrangevideoView mVideoView;
    
    // 截图
    public void takeScreenshot(ScreenshotCallback callback);
    public void takeScreenshot(boolean highQuality, ScreenshotCallback callback);
    
    // 保存到相册
    public void saveToGallery(Bitmap bitmap, SaveCallback callback);
    
    // 分享截图
    public void shareScreenshot(Bitmap bitmap, Context context);
}

public interface ScreenshotCallback {
    void onSuccess(Bitmap bitmap);
    void onError(String message);
}
```

### 2. 截图按钮
- 位置：全屏控制栏或设置菜单
- 点击后显示截图预览
- 提供保存和分享选项

## UI 设计

### 截图按钮
- 图标：相机图标
- 位置：全屏模式下的控制栏

### 截图预览弹窗
```
┌─────────────────────────┐
│     [截图预览图片]       │
│                         │
│  [保存到相册]  [分享]    │
└─────────────────────────┘
```

### 截图成功提示
- Toast 提示 "截图已保存"
- 显示保存路径

## 文件结构
```
palyerlibrary/src/main/java/com/orange/playerlibrary/
├── screenshot/
│   ├── ScreenshotManager.java    # 截图管理器
│   ├── ScreenshotDialog.java     # 截图预览弹窗
│   └── ScreenshotUtils.java      # 截图工具类
```

## 权限要求
```xml
<!-- Android 10 以下需要 -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

<!-- Android 10+ 使用 MediaStore API，无需额外权限 -->
```

## 使用示例
```java
// 截图并保存
videoView.takeScreenshot(bitmap -> {
    ScreenshotManager.saveToGallery(context, bitmap, success -> {
        if (success) {
            Toast.makeText(context, "截图已保存", Toast.LENGTH_SHORT).show();
        }
    });
});

// 截图并分享
videoView.takeScreenshot(bitmap -> {
    ScreenshotManager.share(context, bitmap);
});
```

## 注意事项
1. 某些 DRM 保护的视频可能无法截图
2. 高清截图会消耗更多内存
3. Android 10+ 需要使用 MediaStore API 保存到相册
