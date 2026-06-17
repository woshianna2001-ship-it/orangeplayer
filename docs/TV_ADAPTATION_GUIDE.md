# Android TV 适配指南

本文档详细说明如何将 OrangePlayer 适配到 Android TV 平台。

## 目录

1. [TV 平台特点](#tv-平台特点)
2. [快速开始](#快速开始)
3. [AndroidManifest 配置](#androidmanifest-配置)
4. [焦点处理](#焦点处理)
5. [遥控器支持](#遥控器支持)
6. [UI 适配](#ui-适配)
7. [Leanback 界面](#leanback-界面)
8. [测试指南](#测试指南)

---

## TV 平台特点

### 与手机/平板的主要区别

| 特性 | 手机/平板 | Android TV |
|-----|---------|-----------|
| **输入方式** | 触摸屏 | 遥控器/游戏手柄 |
| **导航方式** | 点击/滑动 | 方向键焦点导航 |
| **屏幕尺寸** | 小屏幕 | 大屏幕（10英尺距离） |
| **分辨率** | 多样 | 通常 1080p/4K |
| **交互距离** | 近距离 | 远距离（3米+） |
| **UI 设计** | 密集布局 | 大按钮、大字体 |

### TV 应用要求

1. **必须支持方向键导航**
2. **所有可交互元素必须可获得焦点**
3. **必须声明 TV 支持**
4. **推荐使用 Leanback 库**
5. **必须在 10 英尺距离下可用**

---

## 快速开始

### 1. 添加依赖

在 `build.gradle` 中添加 Leanback 库：

```gradle
dependencies {
    // Android TV Leanback 库
    implementation 'androidx.leanback:leanback:1.2.0-alpha04'
    
    // TV Provider（可选，用于内容推荐）
    implementation 'androidx.tvprovider:tvprovider:1.0.0'
    
    // 现有的播放器依赖
    implementation project(':palyerlibrary')
    // ...
}
```

### 2. 创建 TV 模块（推荐）

建议创建独立的 TV 模块：

```
your-project/
├── app/              # 手机/平板应用
├── app-legacy/       # 兼容旧设备
├── app-tv/          # TV 应用（新建）
└── palyerlibrary/   # 播放器库
```

---

## AndroidManifest 配置

### 1. 声明 TV 支持

在 `AndroidManifest.xml` 中添加：

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- 声明这是一个 TV 应用 -->
    <uses-feature
        android:name="android.software.leanback"
        android:required="true" />
    
    <!-- 声明不需要触摸屏（TV 没有触摸屏） -->
    <uses-feature
        android:name="android.hardware.touchscreen"
        android:required="false" />
    
    <!-- 可选：声明支持游戏手柄 -->
    <uses-feature
        android:name="android.hardware.gamepad"
        android:required="false" />
    
    <application
        android:name=".OrangeApplication"
        android:banner="@drawable/tv_banner"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.Leanback">
        
        <!-- TV 启动器 Activity -->
        <activity
            android:name=".tv.TvMainActivity"
            android:exported="true"
            android:screenOrientation="landscape">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
        </activity>
        
        <!-- 播放器 Activity -->
        <activity
            android:name=".tv.TvPlayerActivity"
            android:configChanges="orientation|keyboardHidden|screenSize"
            android:screenOrientation="landscape"
            android:theme="@style/Theme.Leanback.Player" />
        
    </application>
</manifest>
```

### 2. TV Banner 图标

TV 应用需要 Banner 图标（320x180 dp）：

- 创建 `res/drawable/tv_banner.xml` 或 `res/drawable-xhdpi/tv_banner.png`
- 尺寸：320x180 dp
- 格式：PNG 或 XML drawable

---

## 焦点处理

### 1. 基本焦点配置

所有可交互的 View 必须设置 `focusable="true"`：

```xml
<!-- 播放/暂停按钮 -->
<ImageButton
    android:id="@+id/btn_play"
    android:layout_width="80dp"
    android:layout_height="80dp"
    android:focusable="true"
    android:focusableInTouchMode="false"
    android:background="@drawable/tv_button_focus_bg"
    android:src="@drawable/ic_play" />

<!-- 进度条（可选焦点） -->
<SeekBar
    android:id="@+id/seek_bar"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:focusable="true"
    android:focusableInTouchMode="false" />
```

### 2. 焦点导航顺序

使用 `nextFocus*` 属性控制焦点流转：

```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal">
    
    <!-- 后退按钮 -->
    <ImageButton
        android:id="@+id/btn_back"
        android:nextFocusRight="@id/btn_play"
        android:nextFocusDown="@id/seek_bar"
        android:focusable="true" />
    
    <!-- 播放按钮 -->
    <ImageButton
        android:id="@+id/btn_play"
        android:nextFocusLeft="@id/btn_back"
        android:nextFocusRight="@id/btn_forward"
        android:nextFocusDown="@id/seek_bar"
        android:focusable="true" />
    
    <!-- 快进按钮 -->
    <ImageButton
        android:id="@+id/btn_forward"
        android:nextFocusLeft="@id/btn_play"
        android:nextFocusDown="@id/seek_bar"
        android:focusable="true" />
    
    <!-- 进度条 -->
    <SeekBar
        android:id="@+id/seek_bar"
        android:nextFocusUp="@id/btn_play"
        android:focusable="true" />
</LinearLayout>
```

### 3. 焦点样式

创建焦点状态的 drawable：

**res/drawable/tv_button_focus_bg.xml**:
```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- 获得焦点时 -->
    <item android:state_focused="true">
        <shape android:shape="rectangle">
            <solid android:color="#4D000000" />
            <stroke
                android:width="3dp"
                android:color="#FFFFFF" />
            <corners android:radius="8dp" />
        </shape>
    </item>
    
    <!-- 按下时 -->
    <item android:state_pressed="true">
        <shape android:shape="rectangle">
            <solid android:color="#80000000" />
            <stroke
                android:width="3dp"
                android:color="#FFD700" />
            <corners android:radius="8dp" />
        </shape>
    </item>
    
    <!-- 默认状态 -->
    <item>
        <shape android:shape="rectangle">
            <solid android:color="#33000000" />
            <corners android:radius="8dp" />
        </shape>
    </item>
</selector>
```

### 4. 代码中处理焦点

```java
public class TvPlayerActivity extends AppCompatActivity {
    
    private ImageButton btnPlay;
    private SeekBar seekBar;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tv_player);
        
        btnPlay = findViewById(R.id.btn_play);
        seekBar = findViewById(R.id.seek_bar);
        
        // 设置默认焦点
        btnPlay.requestFocus();
        
        // 监听焦点变化
        btnPlay.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // 放大动画
                v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start();
            } else {
                // 恢复原大小
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
            }
        });
    }
}
```

---

## 遥控器支持

### 1. 处理遥控器按键

```java
public class TvPlayerActivity extends AppCompatActivity {
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:  // 确认键
            case KeyEvent.KEYCODE_ENTER:
                // 播放/暂停
                togglePlayPause();
                return true;
                
            case KeyEvent.KEYCODE_DPAD_LEFT:    // 左键
                // 快退 10 秒
                seekBackward(10000);
                return true;
                
            case KeyEvent.KEYCODE_DPAD_RIGHT:   // 右键
                // 快进 10 秒
                seekForward(10000);
                return true;
                
            case KeyEvent.KEYCODE_DPAD_UP:      // 上键
                // 显示控制栏
                showControlBar();
                return true;
                
            case KeyEvent.KEYCODE_DPAD_DOWN:    // 下键
                // 隐藏控制栏
                hideControlBar();
                return true;
                
            case KeyEvent.KEYCODE_BACK:         // 返回键
                // 退出播放
                finish();
                return true;
                
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:  // 媒体播放/暂停键
                togglePlayPause();
                return true;
                
            case KeyEvent.KEYCODE_MEDIA_PLAY:   // 媒体播放键
                play();
                return true;
                
            case KeyEvent.KEYCODE_MEDIA_PAUSE:  // 媒体暂停键
                pause();
                return true;
                
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:  // 快进键
                seekForward(30000);
                return true;
                
            case KeyEvent.KEYCODE_MEDIA_REWIND:  // 快退键
                seekBackward(30000);
                return true;
        }
        
        return super.onKeyDown(keyCode, event);
    }
    
    private void togglePlayPause() {
        if (videoPlayer.isPlaying()) {
            videoPlayer.pause();
        } else {
            videoPlayer.start();
        }
    }
    
    private void seekForward(long milliseconds) {
        long currentPosition = videoPlayer.getCurrentPosition();
        long duration = videoPlayer.getDuration();
        long newPosition = Math.min(currentPosition + milliseconds, duration);
        videoPlayer.seekTo(newPosition);
    }
    
    private void seekBackward(long milliseconds) {
        long currentPosition = videoPlayer.getCurrentPosition();
        long newPosition = Math.max(currentPosition - milliseconds, 0);
        videoPlayer.seekTo(newPosition);
    }
}
```

### 2. 游戏手柄支持（可选）

```java
@Override
public boolean onGenericMotionEvent(MotionEvent event) {
    // 处理游戏手柄摇杆
    if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            && event.getAction() == MotionEvent.ACTION_MOVE) {
        
        // 左摇杆 X 轴（快进/快退）
        float x = event.getAxisValue(MotionEvent.AXIS_X);
        if (Math.abs(x) > 0.5f) {
            if (x > 0) {
                seekForward(1000);
            } else {
                seekBackward(1000);
            }
            return true;
        }
    }
    
    return super.onGenericMotionEvent(event);
}
```

---

## UI 适配

### 1. TV 专用布局

创建 `res/layout-television/` 目录存放 TV 专用布局：

```
res/
├── layout/                    # 手机/平板布局
│   └── activity_player.xml
└── layout-television/         # TV 专用布局
    └── activity_player.xml
```

### 2. TV 布局设计原则

```xml
<!-- TV 播放器布局示例 -->
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000">
    
    <!-- 视频播放器 -->
    <com.orange.playerlibrary.OrangevideoView
        android:id="@+id/video_player"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
    
    <!-- 控制栏（底部） -->
    <LinearLayout
        android:id="@+id/control_bar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_alignParentBottom="true"
        android:orientation="vertical"
        android:padding="48dp"
        android:background="@drawable/tv_control_bar_bg">
        
        <!-- 标题 -->
        <TextView
            android:id="@+id/tv_title"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="32sp"
            android:textColor="#FFFFFF"
            android:layout_marginBottom="24dp" />
        
        <!-- 进度条 -->
        <SeekBar
            android:id="@+id/seek_bar"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:focusable="true"
            android:layout_marginBottom="24dp"
            android:minHeight="8dp"
            android:progressDrawable="@drawable/tv_seekbar_progress"
            android:thumb="@drawable/tv_seekbar_thumb" />
        
        <!-- 时间信息 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginBottom="24dp">
            
            <TextView
                android:id="@+id/tv_current_time"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="24sp"
                android:textColor="#CCCCCC"
                android:text="00:00" />
            
            <TextView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:gravity="center"
                android:textSize="24sp"
                android:textColor="#CCCCCC"
                android:text="/" />
            
            <TextView
                android:id="@+id/tv_duration"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="24sp"
                android:textColor="#CCCCCC"
                android:text="00:00" />
        </LinearLayout>
        
        <!-- 控制按钮 -->
        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center_horizontal"
            android:orientation="horizontal">
            
            <!-- 快退按钮 -->
            <ImageButton
                android:id="@+id/btn_rewind"
                android:layout_width="80dp"
                android:layout_height="80dp"
                android:layout_margin="16dp"
                android:focusable="true"
                android:background="@drawable/tv_button_focus_bg"
                android:src="@drawable/ic_rewind"
                android:contentDescription="@string/rewind" />
            
            <!-- 播放/暂停按钮 -->
            <ImageButton
                android:id="@+id/btn_play_pause"
                android:layout_width="96dp"
                android:layout_height="96dp"
                android:layout_margin="16dp"
                android:focusable="true"
                android:background="@drawable/tv_button_focus_bg"
                android:src="@drawable/ic_play"
                android:contentDescription="@string/play_pause" />
            
            <!-- 快进按钮 -->
            <ImageButton
                android:id="@+id/btn_forward"
                android:layout_width="80dp"
                android:layout_height="80dp"
                android:layout_margin="16dp"
                android:focusable="true"
                android:background="@drawable/tv_button_focus_bg"
                android:src="@drawable/ic_forward"
                android:contentDescription="@string/forward" />
        </LinearLayout>
    </LinearLayout>
    
    <!-- 加载提示 -->
    <ProgressBar
        android:id="@+id/loading"
        android:layout_width="80dp"
        android:layout_height="80dp"
        android:layout_centerInParent="true"
        android:visibility="gone" />
</RelativeLayout>
```

### 3. TV 尺寸规范

```xml
<!-- res/values-television/dimens.xml -->
<resources>
    <!-- TV 按钮尺寸 -->
    <dimen name="tv_button_size">80dp</dimen>
    <dimen name="tv_button_large_size">96dp</dimen>
    
    <!-- TV 文字大小 -->
    <dimen name="tv_text_title">32sp</dimen>
    <dimen name="tv_text_subtitle">24sp</dimen>
    <dimen name="tv_text_body">20sp</dimen>
    
    <!-- TV 间距 -->
    <dimen name="tv_padding">48dp</dimen>
    <dimen name="tv_margin">24dp</dimen>
    
    <!-- TV 焦点边框 -->
    <dimen name="tv_focus_border_width">3dp</dimen>
</resources>
```

---

## Leanback 界面

### 1. 使用 Leanback Fragment

```java
public class TvMainFragment extends BrowseSupportFragment {
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        setupUI();
        loadData();
    }
    
    private void setupUI() {
        // 设置标题
        setTitle("OrangePlayer");
        
        // 设置头部颜色
        setBrandColor(getResources().getColor(R.color.primary));
        
        // 设置搜索图标
        setSearchAffordanceColor(getResources().getColor(R.color.accent));
    }
    
    private void loadData() {
        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        
        // 添加视频分类
        HeaderItem header1 = new HeaderItem(0, "最近播放");
        ArrayObjectAdapter listRowAdapter1 = new ArrayObjectAdapter(new VideoCardPresenter());
        // 添加视频项...
        rowsAdapter.add(new ListRow(header1, listRowAdapter1));
        
        HeaderItem header2 = new HeaderItem(1, "推荐视频");
        ArrayObjectAdapter listRowAdapter2 = new ArrayObjectAdapter(new VideoCardPresenter());
        // 添加视频项...
        rowsAdapter.add(new ListRow(header2, listRowAdapter2));
        
        setAdapter(rowsAdapter);
        
        // 设置点击监听
        setOnItemViewClickedListener((itemViewHolder, item, rowViewHolder, row) -> {
            if (item instanceof Video) {
                playVideo((Video) item);
            }
        });
    }
    
    private void playVideo(Video video) {
        Intent intent = new Intent(getActivity(), TvPlayerActivity.class);
        intent.putExtra("video_url", video.getUrl());
        intent.putExtra("video_title", video.getTitle());
        startActivity(intent);
    }
}
```

### 2. 自定义 Presenter

```java
public class VideoCardPresenter extends Presenter {
    
    private static final int CARD_WIDTH = 313;
    private static final int CARD_HEIGHT = 176;
    
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        ImageCardView cardView = new ImageCardView(parent.getContext());
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        return new ViewHolder(cardView);
    }
    
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        Video video = (Video) item;
        ImageCardView cardView = (ImageCardView) viewHolder.view;
        
        cardView.setTitleText(video.getTitle());
        cardView.setContentText(video.getDuration());
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
        
        // 加载缩略图
        Glide.with(cardView.getContext())
                .load(video.getThumbnailUrl())
                .into(cardView.getMainImageView());
    }
    
    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        ImageCardView cardView = (ImageCardView) viewHolder.view;
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
    }
}
```

---

## 测试指南

### 1. 使用 Android TV 模拟器

在 Android Studio 中创建 TV 模拟器：

1. Tools → Device Manager
2. Create Device → TV
3. 选择 TV 设备（如 Android TV 1080p）
4. 选择系统镜像（API 21+）
5. 启动模拟器

### 2. 使用真实 TV 设备

通过 ADB 连接：

```bash
# 通过 WiFi 连接（TV 和电脑在同一网络）
adb connect <TV_IP_ADDRESS>:5555

# 安装 APK
adb install app-tv-debug.apk

# 查看日志
adb logcat | findstr "OrangePlayer"
```

### 3. 测试清单

- [ ] 所有按钮都可以通过方向键获得焦点
- [ ] 焦点顺序符合逻辑
- [ ] 焦点状态清晰可见（边框/高亮）
- [ ] 遥控器所有按键都能正常工作
- [ ] 在 10 英尺距离下文字清晰可读
- [ ] 视频播放流畅，无卡顿
- [ ] 控制栏自动隐藏/显示
- [ ] 返回键正常工作
- [ ] 应用在 TV 启动器中显示

---

## 最佳实践

### 1. 性能优化

- 使用硬件加速解码
- 预加载缩略图
- 使用 RecyclerView 而不是 ListView
- 避免过度绘制

### 2. 用户体验

- 控制栏 5 秒后自动隐藏
- 焦点动画流畅自然
- 提供视觉反馈（声音/动画）
- 支持长按快速跳转

### 3. 兼容性

- 支持不同分辨率（720p/1080p/4K）
- 支持不同遥控器类型
- 处理网络中断情况
- 支持多种视频格式

---

## 参考资料

- [Android TV 开发指南](https://developer.android.com/training/tv)
- [Leanback 库文档](https://developer.android.com/reference/androidx/leanback/app/package-summary)
- [TV 设计规范](https://developer.android.com/design/tv)
- [TV 输入框架](https://source.android.com/devices/tv)

---

## 更新日志

- **2026-02-07**: 创建 TV 适配指南
- 包含完整的配置、焦点处理、遥控器支持
- 提供 Leanback 界面示例
- 添加测试指南和最佳实践
