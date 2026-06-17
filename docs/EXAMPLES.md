# 完整使用示例

本文档包含 15 个实用的 OrangePlayer 使用示例，涵盖从基础播放到高级功能的各种场景。

## 目录

- [完整使用示例](#完整使用示例)
  - [目录](#目录)
  - [示例 1：基础播放器](#示例-1基础播放器)
  - [示例 2：带自定义请求头的播放](#示例-2带自定义请求头的播放)
  - [示例 3：播放状态监听](#示例-3播放状态监听)
  - [示例 4：播放进度监听](#示例-4播放进度监听)
  - [示例 5：播放完成监听](#示例-5播放完成监听)
  - [示例 6：倍速播放](#示例-6倍速播放)
  - [示例 7：字幕加载](#示例-7字幕加载)
  - [示例 8：弹幕功能](#示例-8弹幕功能)
  - [示例 9：播放列表](#示例-9播放列表)
  - [示例 10：画中画模式](#示例-10画中画模式)
  - [示例 11：投屏功能](#示例-11投屏功能)
  - [示例 12：OCR 字幕识别](#示例-12ocr-字幕识别)
  - [示例 13：语音识别字幕](#示例-13语音识别字幕)
  - [示例 14：播放器设置](#示例-14播放器设置)
  - [示例 15：错误处理](#示例-15错误处理)
  - [更多资源](#更多资源)

---

## 示例 1：基础播放器

最简单的视频播放实现：

```java
public class MainActivity extends AppCompatActivity {
    private OrangevideoView mVideoView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mVideoView = findViewById(R.id.video_player);
        
        // 设置视频地址和标题
        mVideoView.setUp("https://example.com/video.mp4", true, "示例视频");
        
        // 开始播放
        mVideoView.startPlayLogic();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mVideoView.onVideoPause();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        mVideoView.onVideoResume();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideoView.release();
    }
    
    @Override
    public void onBackPressed() {
        if (mVideoView.isFullScreen()) {
            mVideoView.stopFullScreen();
            return;
        }
        super.onBackPressed();
    }
}
```

---

## 示例 2：带自定义请求头的播放

```java
// 设置带请求头的视频地址
Map<String, String> headers = new HashMap<>();
headers.put("User-Agent", "MyPlayer/1.0");
headers.put("Referer", "https://example.com");
headers.put("Authorization", "Bearer your_token");

mVideoView.setUrl("https://example.com/video.mp4", headers);
mVideoView.startPlayLogic();
```

---

## 示例 3：播放状态监听

```java
import com.orange.playerlibrary.interfaces.OnStateChangeListener;
import com.orange.playerlibrary.PlayerConstants;

mVideoView.addOnStateChangeListener(new OnStateChangeListener() {
    @Override
    public void onPlayerStateChanged(int playerState) {
        switch (playerState) {
            case PlayerConstants.PLAYER_NORMAL:
                Log.d(TAG, "普通模式");
                break;
            case PlayerConstants.PLAYER_FULL_SCREEN:
                Log.d(TAG, "全屏模式");
                break;
        }
    }
    
    @Override
    public void onPlayStateChanged(int playState) {
        switch (playState) {
            case PlayerConstants.STATE_IDLE:
                Log.d(TAG, "空闲状态");
                break;
            case PlayerConstants.STATE_PREPARING:
                Log.d(TAG, "准备中");
                break;
            case PlayerConstants.STATE_PREPARED:
                Log.d(TAG, "准备完成");
                break;
            case PlayerConstants.STATE_PLAYING:
                Log.d(TAG, "播放中");
                updatePlayButton(true);
                break;
            case PlayerConstants.STATE_PAUSED:
                Log.d(TAG, "暂停");
                updatePlayButton(false);
                break;
            case PlayerConstants.STATE_PLAYBACK_COMPLETED:
                Log.d(TAG, "播放完成");
                break;
            case PlayerConstants.STATE_ERROR:
                Log.d(TAG, "播放错误");
                showErrorDialog();
                break;
        }
    }
});
```


## 示例 4：播放进度监听

```java
import com.orange.playerlibrary.interfaces.OnProgressListener;

mVideoView.setOnProgressListener(new OnProgressListener() {
    @Override
    public void onProgress(long currentPosition, long duration) {
        // 更新进度条
        int progress = (int) (currentPosition * 100 / duration);
        mProgressBar.setProgress(progress);
        
        // 更新时间显示
        String current = formatTime(currentPosition);
        String total = formatTime(duration);
        mTimeText.setText(current + " / " + total);
    }
});

private String formatTime(long milliseconds) {
    long seconds = milliseconds / 1000;
    long minutes = seconds / 60;
    long hours = minutes / 60;
    
    if (hours > 0) {
        return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
    } else {
        return String.format("%02d:%02d", minutes, seconds % 60);
    }
}
```

---

## 示例 5：播放完成监听

```java
import com.orange.playerlibrary.interfaces.OnPlayCompleteListener;

mVideoView.setOnPlayCompleteListener(new OnPlayCompleteListener() {
    @Override
    public void onPlayComplete() {
        // 播放完成后的操作
        Toast.makeText(MainActivity.this, "播放完成", Toast.LENGTH_SHORT).show();
        
        // 自动播放下一个视频
        playNextVideo();
        
        // 或者显示重播按钮
        showReplayButton();
    }
});
```

---

## 示例 6：倍速播放

```java
// 设置倍速
mVideoView.setSpeed(1.5f);  // 1.5 倍速

// 倍速选项按钮
String[] speeds = {"0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x"};
new AlertDialog.Builder(this)
    .setTitle("选择倍速")
    .setItems(speeds, (dialog, which) -> {
        float speed = Float.parseFloat(speeds[which].replace("x", ""));
        mVideoView.setSpeed(speed);
        Toast.makeText(this, "已设置为 " + speeds[which], Toast.LENGTH_SHORT).show();
    })
    .show();
```

---

## 示例 7：字幕加载

```java
import com.orange.playerlibrary.subtitle.SubtitleManager;

// 获取字幕管理器
SubtitleManager subtitleManager = mVideoView.getVideoController().getSubtitleManager();

// 加载字幕文件
subtitleManager.loadSubtitle("https://example.com/subtitle.srt", 
    new SubtitleManager.OnSubtitleLoadListener() {
        @Override
        public void onLoadSuccess(int count) {
            Toast.makeText(MainActivity.this, 
                "字幕加载成功，共 " + count + " 条", 
                Toast.LENGTH_SHORT).show();
            subtitleManager.start();
        }
        
        @Override
        public void onLoadFailed(String error) {
            Toast.makeText(MainActivity.this, 
                "字幕加载失败：" + error, 
                Toast.LENGTH_SHORT).show();
        }
    });

// 设置字幕大小
subtitleManager.setTextSize(18f);  // 18sp

// 从本地文件加载字幕
File subtitleFile = new File(getExternalFilesDir(null), "subtitle.srt");
if (subtitleFile.exists()) {
    subtitleManager.loadSubtitle(Uri.fromFile(subtitleFile), listener);
}
```

---

## 示例 8：弹幕功能

```java
import com.orange.playerlibrary.danmaku.IDanmakuController;

// 获取弹幕控制器
IDanmakuController danmakuController = mVideoView.getVideoController().getDanmakuController();

// 发送弹幕
danmakuController.sendDanmaku("这是一条弹幕", 0xFFFFFFFF);  // 白色弹幕

// 显示/隐藏弹幕
danmakuController.show();
danmakuController.hide();

// 设置弹幕参数
PlayerSettingsManager settings = PlayerSettingsManager.getInstance(this);
settings.setDanmakuTextSize(16f);      // 字体大小
settings.setDanmakuSpeed(1.2f);        // 滚动速度
settings.setDanmakuAlpha(0.8f);        // 透明度
```

---

## 示例 9：播放列表

```java
import java.util.ArrayList;
import java.util.HashMap;

// 方式1：使用 addVideo() 逐个添加
OrangeVideoController controller = mVideoView.getVideoController();
controller.addVideo("第1集", "https://example.com/video1.mp4");
controller.addVideo("第2集", "https://example.com/video2.mp4");
controller.addVideo("第3集", "https://example.com/video3.mp4");

// 第一个视频会自动设置到播放器，直接播放即可
mVideoView.startPlayLogic();

// 方式2：使用 setVideoList() 批量设置
ArrayList<HashMap<String, Object>> playlist = new ArrayList<>();

HashMap<String, Object> video1 = new HashMap<>();
video1.put("name", "第1集");
video1.put("url", "https://example.com/video1.mp4");
playlist.add(video1);

HashMap<String, Object> video2 = new HashMap<>();
video2.put("name", "第2集");
video2.put("url", "https://example.com/video2.mp4");
playlist.add(video2);

// 设置播放列表（第一个视频会自动设置到播放器）
controller.setVideoList(playlist);
mVideoView.startPlayLogic();

// 播放下一集
VideoEventManager eventManager = controller.getVideoEventManager();
if (eventManager != null) {
    eventManager.playNextEpisode();
}

// 设置播放模式（在设置界面配置，或通过代码设置）
PlayerSettingsManager.getInstance(this).setPlayMode("sequential"); // 顺序播放

// 播放完成后会根据播放模式自动处理（已内置，无需手动监听）
// - sequential: 自动播放下一集
// - single_loop: 重新播放当前视频
// - play_pause: 停止播放
```

---

## 示例 10：画中画模式

```java
import android.app.PictureInPictureParams;
import android.os.Build;
import android.util.Rational;

// 进入画中画模式（Android 8.0+）
private void enterPictureInPicture() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // 设置画中画参数
        Rational aspectRatio = new Rational(16, 9);
        PictureInPictureParams params = new PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .build();
        
        // 进入画中画
        enterPictureInPictureMode(params);
    }
}

@Override
public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, 
                                          Configuration newConfig) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
    
    if (isInPictureInPictureMode) {
        // 进入画中画模式，隐藏控制器
        mVideoView.getVideoController().hide();
    } else {
        // 退出画中画模式，显示控制器
        mVideoView.getVideoController().show();
    }
}
```

---

## 示例 11：投屏功能

```java
import com.orange.playerlibrary.cast.DLNACastManager;

// 检查投屏是否可用
if (DLNACastManager.isDLNAAvailable()) {
    // 开始投屏
    DLNACastManager.getInstance().startCast(
        this,
        mVideoView.getUrl(),
        mVideoView.getVideoController().getVideoTitle()
    );
    
    // 监听投屏状态
    DLNACastManager.getInstance().setOnCastStateListener(
        new DLNACastManager.OnCastStateListener() {
            @Override
            public void onCastStarted() {
                Toast.makeText(MainActivity.this, "投屏已开始", Toast.LENGTH_SHORT).show();
                // 暂停本地播放
                mVideoView.pause();
            }
            
            @Override
            public void onCastStopped() {
                Toast.makeText(MainActivity.this, "投屏已停止", Toast.LENGTH_SHORT).show();
                // 恢复本地播放
                mVideoView.resume();
            }
            
            @Override
            public void onCastError(String message) {
                Toast.makeText(MainActivity.this, 
                    "投屏错误：" + message, 
                    Toast.LENGTH_SHORT).show();
            }
        });
} else {
    Toast.makeText(this, "投屏功能不可用，请检查依赖", Toast.LENGTH_SHORT).show();
}
```

---

## 示例 12：OCR 字幕识别

```java
import com.orange.playerlibrary.ocr.OcrAvailabilityChecker;
import com.orange.playerlibrary.ocr.LanguagePackManager;

// 检查 OCR 功能是否可用
if (OcrAvailabilityChecker.isOcrTranslateAvailable()) {
    // 检查语言包
    LanguagePackManager manager = new LanguagePackManager(this);
    
    if (!manager.isLanguageInstalled("chi_sim")) {
        // 下载简体中文语言包
        manager.downloadLanguage("chi_sim", 
            new LanguagePackManager.DownloadCallback() {
                @Override
                public void onProgress(int progress, long downloaded, long total) {
                    // 更新下载进度
                    mProgressDialog.setProgress(progress);
                }
                
                @Override
                public void onSuccess() {
                    Toast.makeText(MainActivity.this, 
                        "语言包下载成功", 
                        Toast.LENGTH_SHORT).show();
                    // 启动 OCR
                    startOcrTranslate();
                }
                
                @Override
                public void onError(String error) {
                    Toast.makeText(MainActivity.this, 
                        "下载失败：" + error, 
                        Toast.LENGTH_SHORT).show();
                }
            });
    } else {
        // 直接启动 OCR
        startOcrTranslate();
    }
} else {
    // 显示缺少的依赖
    String message = OcrAvailabilityChecker.getMissingDependenciesMessage();
    new AlertDialog.Builder(this)
        .setTitle("OCR 功能不可用")
        .setMessage(message)
        .setPositiveButton("查看文档", (dialog, which) -> {
            // 打开文档链接
        })
        .show();
}

private void startOcrTranslate() {
    // 通过播放器 UI 启动 OCR
    // 用户点击字幕按钮 -> OCR 翻译字幕 -> 设置识别区域 -> 选择语言 -> 开始识别
}
```

---

## 示例 13：语音识别字幕

```java
import com.orange.playerlibrary.speech.SpeechSubtitleManager;
import com.orange.playerlibrary.speech.VoskModelManager;

// 检查语音识别是否支持
if (SpeechSubtitleManager.isSupported()) {
    // 检查是否已安装语言模型
    VoskModelManager modelManager = new VoskModelManager(this);
    
    if (!modelManager.isLanguageInstalled("zh-CN")) {
        // 提示用户下载模型
        new AlertDialog.Builder(this)
            .setTitle("需要下载语音模型")
            .setMessage("首次使用需要下载中文语音模型\n" +
                       "小型模型: 42MB\n" +
                       "标准模型: 250MB\n" +
                       "大型模型: 1.3GB")
            .setPositiveButton("管理语言包", (dialog, which) -> {
                // 打开语言包管理界面
                // 用户可以通过播放器设置按钮 -> 语音识别翻译 -> 管理语言包
                Toast.makeText(this, 
                    "请点击播放器设置 -> 语音识别翻译 -> 管理语言包", 
                    Toast.LENGTH_LONG).show();
            })
            .setNegativeButton("取消", null)
            .show();
    } else {
        // 语音识别通过播放器 UI 启动
        // 用户点击设置按钮 -> 语音识别翻译 -> 选择语言 -> 开始识别
        Toast.makeText(this, 
            "请点击播放器设置按钮，选择语音识别翻译功能", 
            Toast.LENGTH_SHORT).show();
    }
} else {
    // 显示不支持的原因
    String reason = SpeechSubtitleManager.getUnsupportedReason();
    Toast.makeText(this, "语音识别不可用: " + reason, Toast.LENGTH_LONG).show();
}
```

---

## 示例 14：播放器设置

```java
import com.orange.playerlibrary.PlayerSettingsManager;

PlayerSettingsManager settings = PlayerSettingsManager.getInstance(this);

// 播放内核设置
settings.setPlayerEngine(PlayerConstants.ENGINE_EXO);

// 播放模式
settings.setPlayMode("sequential");  // 顺序播放
// settings.setPlayMode("single_loop");  // 单曲循环
// settings.setPlayMode("play_pause");   // 播放后暂停

// 长按倍速
settings.setLongPressSpeed(2.0f);  // 长按 2 倍速

// 跳过片头片尾
settings.setSkipOpening(30);  // 跳过前 30 秒
settings.setSkipEnding(60);   // 跳过后 60 秒

// 底部进度条
settings.setBottomProgressEnabled(true);

// 自动旋转
settings.setAutoRotateEnabled(true);

// 硬件解码
settings.setHardwareDecode(true);
```

---

## 示例 15：错误处理

```java
mVideoView.addOnStateChangeListener(new OnStateChangeListener() {
    @Override
    public void onPlayerStateChanged(int playerState) {
        // 不处理
    }
    
    @Override
    public void onPlayStateChanged(int playState) {
        if (playState == PlayerConstants.STATE_ERROR) {
            // 播放错误，尝试切换播放内核
            handlePlaybackError();
        }
    }
});

private void handlePlaybackError() {
    new AlertDialog.Builder(this)
        .setTitle("播放错误")
        .setMessage("当前播放内核无法播放此视频，是否尝试切换播放内核？")
        .setPositiveButton("切换到 ExoPlayer", (dialog, which) -> {
            mVideoView.selectPlayerFactory(PlayerConstants.ENGINE_EXO);
            mVideoView.startPlayLogic();
        })
        .setNegativeButton("切换到 IJK", (dialog, which) -> {
            mVideoView.selectPlayerFactory(PlayerConstants.ENGINE_IJK);
            mVideoView.startPlayLogic();
        })
        .setNeutralButton("取消", null)
        .show();
}
```

---

## 更多资源

- [API 文档](API.md) - 完整的 API 参考
- [安装指南](INSTALLATION.md) - 依赖配置和环境设置
- [常见问题](FAQ.md) - 问题排查和解决方案
- [返回主页](../README.md)
