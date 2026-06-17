# OrangePlayer Legacy SDK 使用示例

## 快速开始

### 1. 添加依赖

在项目的 `build.gradle` 中添加：

```gradle
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    // OrangePlayer 核心库（支持 Android 4.0+）
    implementation 'io.github.706412584:orangeplayer:1.1.0-api14'
    
    // 选择至少一个 CPU 架构（必需）
    implementation 'com.shuyu.gsyvideoplayer:gsyVideoPlayer-armv7a:1.0.0'  // ARM 32位
    implementation 'com.shuyu.gsyvideoplayer:gsyVideoPlayer-armv64:1.0.0'  // ARM 64位
    
    // 可选：弹幕功能
    implementation 'com.github.bilibili:DanmakuFlameMaster:0.9.25'
    
    // 可选：DLNA 投屏（需要 Android 4.4+）
    implementation 'com.github.uaoan:UaoanDLNA:1.0.1'
    implementation 'com.squareup.okhttp3:okhttp:3.12.13'
}
```

### 2. 布局文件

```xml
<com.orange.playerlibrary.OrangevideoView
    android:id="@+id/video_player"
    android:layout_width="match_parent"
    android:layout_height="200dp" />
```

### 3. 基础使用

```java
public class MainActivity extends AppCompatActivity {
    private OrangevideoView videoPlayer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        videoPlayer = findViewById(R.id.video_player);
        
        // 播放网络视频
        videoPlayer.setUp("https://example.com/video.mp4", 
                         true, 
                         "视频标题");
        videoPlayer.startPlayLogic();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        videoPlayer.onVideoPause();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        videoPlayer.onVideoResume();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoPlayer.release();
    }
    
    @Override
    public void onBackPressed() {
        if (videoPlayer.onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }
}
```

---

## 高级功能

### 弹幕功能

```java
// 1. 启用弹幕
videoPlayer.setDanmakuEnabled(true);

// 2. 发送弹幕
videoPlayer.sendDanmaku("弹幕内容", true);

// 3. 设置弹幕样式
DanmakuContext danmakuContext = DanmakuContext.create();
danmakuContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3)
              .setDuplicateMergingEnabled(false)
              .setScrollSpeedFactor(1.2f);
videoPlayer.setDanmakuContext(danmakuContext);
```

### DLNA 投屏（需要 Android 4.4+）

```java
// 1. 检查 DLNA 是否可用
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
    // 2. 搜索设备
    DLNAManager.getInstance().searchDevices(new DeviceListener() {
        @Override
        public void onDeviceFound(Device device) {
            // 发现设备
        }
        
        @Override
        public void onDeviceLost(Device device) {
            // 设备丢失
        }
    });
    
    // 3. 投屏
    DLNAManager.getInstance().cast(device, videoUrl);
}
```

### 播放历史

```java
// 1. 保存播放位置
videoPlayer.setOnProgressListener(new OnProgressListener() {
    @Override
    public void onProgress(long position, long duration) {
        // 保存播放进度
        PlayHistoryManager.saveHistory(videoUrl, position, duration);
    }
});

// 2. 恢复播放位置
long lastPosition = PlayHistoryManager.getLastPosition(videoUrl);
if (lastPosition > 0) {
    videoPlayer.seekTo(lastPosition);
}
```

### 倍速播放

```java
// 设置播放速度（0.5x - 2.0x）
videoPlayer.setSpeed(1.5f);  // 1.5 倍速
```

### 自定义控制器

```java
// 隐藏默认控制器
videoPlayer.setShowFullAnimation(false);

// 添加自定义控制按钮
Button customButton = new Button(this);
customButton.setText("自定义");
customButton.setOnClickListener(v -> {
    // 自定义操作
});
videoPlayer.addCustomView(customButton);
```

---

## 版本兼容性

### Android 4.0-4.3 (API 14-18)

```java
// 检查版本并禁用不支持的功能
if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
    // 禁用 DLNA 投屏
    dlnaButton.setVisibility(View.GONE);
}
```

### Android 4.4+ (API 19+)

```java
// 所有功能可用
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
    // 启用 DLNA
    videoPlayer.setDLNAEnabled(true);
}
```

---

## 性能优化

### 1. 限制缓存大小

```java
// 设置缓存大小（适合老设备）
HttpProxyCacheServer proxy = new HttpProxyCacheServer.Builder(this)
    .maxCacheSize(100 * 1024 * 1024)  // 100 MB
    .build();
```

### 2. 降低视频质量

```java
// 根据设备性能选择视频质量
if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
    // Android 4.x 使用低质量
    videoPlayer.setUp(lowQualityUrl, true, "标题");
} else {
    // Android 5.0+ 使用高质量
    videoPlayer.setUp(highQualityUrl, true, "标题");
}
```

### 3. 限制弹幕数量

```java
// 老设备限制弹幕数量
if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
    danmakuContext.setMaximumLines(3);  // 最多 3 行
} else {
    danmakuContext.setMaximumLines(5);  // 最多 5 行
}
```

---

## 常见问题

### 1. 找不到 so 文件

**错误**: `java.lang.UnsatisfiedLinkError: dlopen failed: library "libijkplayer.so" not found`

**解决**: 确保添加了至少一个架构的依赖

```gradle
implementation 'com.shuyu.gsyvideoplayer:gsyVideoPlayer-armv7a:1.0.0'
```

### 2. MultiDex 问题

**错误**: `java.lang.NoClassDefFoundError`

**解决**: 启用 MultiDex（Android 4.x 必需）

```gradle
android {
    defaultConfig {
        multiDexEnabled true
    }
}

dependencies {
    implementation 'androidx.multidex:multidex:2.0.1'
}
```

### 3. 播放卡顿

**解决**: 
- 使用较低分辨率的视频
- 关闭弹幕
- 清理缓存
- 使用硬件解码

```java
// 启用硬件解码
PlayerFactory.setPlayManager(IjkPlayerManager.class);
IjkPlayerManager.setLogLevel(IjkMediaPlayer.IJK_LOG_SILENT);
```

---

## 完整示例项目

查看 `app-legacy` 模块获取完整示例：

```
orangeplayer/
├── app-legacy/          # Android 4.0+ 示例
│   └── src/main/java/com/orange/player/
│       └── MainActivity.java
└── palyerlibrary/       # SDK 源码
```

---

## 更多资源

- [GitHub 仓库](https://github.com/706412584/orangeplayer)
- [发布指南](PUBLISH_LEGACY_SDK.md)
- [API 文档](API.md)
- [常见问题](FAQ.md)
