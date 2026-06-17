# Android TV 快速集成清单

5 步快速将 OrangePlayer 适配到 Android TV。

## 步骤 1: 添加依赖 (5分钟)

在 `app/build.gradle` 或创建新的 `app-tv/build.gradle`:

```gradle
dependencies {
    // TV Leanback 库
    implementation 'androidx.leanback:leanback:1.2.0-alpha04'
    
    // 播放器库
    implementation project(':palyerlibrary')
    implementation project(':gsyVideoPlayer-java')
    implementation project(':gsyVideoPlayer-armv7a')
    implementation project(':gsyVideoPlayer-armv64')
    
    // 推荐使用 ExoPlayer (API 21+)
    implementation project(':gsyVideoPlayer-exo_player2')
}
```

---

## 步骤 2: 配置 AndroidManifest (10分钟)

在 `AndroidManifest.xml` 中添加：

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- 1. 声明 TV 支持 -->
    <uses-feature
        android:name="android.software.leanback"
        android:required="true" />
    
    <!-- 2. 声明不需要触摸屏 -->
    <uses-feature
        android:name="android.hardware.touchscreen"
        android:required="false" />
    
    <application
        android:banner="@drawable/tv_banner"
        android:theme="@style/Theme.Leanback">
        
        <!-- 3. TV 启动器 -->
        <activity
            android:name=".tv.TvMainActivity"
            android:exported="true"
            android:screenOrientation="landscape">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
        </activity>
        
    </application>
</manifest>
```

---

## 步骤 3: 创建 TV Banner (5分钟)

创建 `res/drawable/tv_banner.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#FF6200EE" />
    <size
        android:width="320dp"
        android:height="180dp" />
</shape>
```

或者使用 PNG 图片（320x180 dp）。

---

## 步骤 4: 适配播放器控制器 (20分钟)

### 方案 A: 修改现有布局（简单）

在 `res/layout/vod_control_view.xml` 中，为所有按钮添加：

```xml
<ImageButton
    android:id="@+id/btn_play"
    android:focusable="true"
    android:focusableInTouchMode="false"
    android:background="@drawable/tv_button_focus_bg"
    ... />
```

创建焦点背景 `res/drawable/tv_button_focus_bg.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_focused="true">
        <shape android:shape="rectangle">
            <stroke android:width="3dp" android:color="#FFFFFF" />
            <corners android:radius="8dp" />
        </shape>
    </item>
</selector>
```

### 方案 B: 创建 TV 专用布局（推荐）

创建 `res/layout-television/vod_control_view.xml`，使用更大的按钮和字体。

---

## 步骤 5: 添加遥控器支持 (15分钟)

在你的播放器 Activity 中添加：

```java
@Override
public boolean onKeyDown(int keyCode, KeyEvent event) {
    switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_CENTER:  // 确认键
        case KeyEvent.KEYCODE_ENTER:
            togglePlayPause();
            return true;
            
        case KeyEvent.KEYCODE_DPAD_LEFT:    // 左键 - 快退
            seekBackward(10000);
            return true;
            
        case KeyEvent.KEYCODE_DPAD_RIGHT:   // 右键 - 快进
            seekForward(10000);
            return true;
            
        case KeyEvent.KEYCODE_BACK:         // 返回键
            finish();
            return true;
            
        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:  // 播放/暂停
            togglePlayPause();
            return true;
    }
    
    return super.onKeyDown(keyCode, event);
}

private void togglePlayPause() {
    if (videoPlayer.isPlaying()) {
        videoPlayer.onVideoPause();
    } else {
        videoPlayer.onVideoResume();
    }
}

private void seekForward(long ms) {
    long pos = videoPlayer.getCurrentPositionWhenPlaying();
    videoPlayer.seekTo(pos + ms);
}

private void seekBackward(long ms) {
    long pos = videoPlayer.getCurrentPositionWhenPlaying();
    videoPlayer.seekTo(Math.max(0, pos - ms));
}
```

---

## 测试清单

### 在 Android Studio 中测试

1. **创建 TV 模拟器**:
   - Tools → Device Manager → Create Device
   - 选择 TV → Android TV (1080p)
   - 选择系统镜像 (API 21+)

2. **运行应用**:
   ```bash
   ./gradlew installDebug
   ```

3. **使用键盘模拟遥控器**:
   - 方向键: ↑ ↓ ← →
   - 确认键: Enter
   - 返回键: Esc

### 测试项目

- [ ] 应用在 TV 启动器中显示
- [ ] 所有按钮可以通过方向键获得焦点
- [ ] 焦点状态清晰可见（边框/高亮）
- [ ] 确认键可以播放/暂停
- [ ] 左右键可以快退/快进
- [ ] 返回键可以退出
- [ ] 视频播放流畅

---

## 常见问题

### Q: 应用在 TV 启动器中不显示？
**A**: 检查 AndroidManifest.xml:
1. 是否添加了 `android.software.leanback` feature
2. 是否添加了 `LEANBACK_LAUNCHER` category
3. 是否设置了 `android:banner`

### Q: 按钮无法获得焦点？
**A**: 确保设置了:
```xml
android:focusable="true"
android:focusableInTouchMode="false"
```

### Q: 焦点不明显？
**A**: 添加焦点状态的 drawable:
```xml
android:background="@drawable/tv_button_focus_bg"
```

### Q: 遥控器按键不响应？
**A**: 在 Activity 中重写 `onKeyDown()` 方法处理按键事件。

---

## 下一步

完成基础集成后，可以进一步优化：

1. **使用 Leanback 界面** - 查看 `TV_ADAPTATION_GUIDE.md`
2. **优化焦点流转** - 使用 `nextFocus*` 属性
3. **添加焦点动画** - 使用 `TvFocusManager`
4. **完整示例代码** - 查看 `TV_PLAYER_EXAMPLE.md`

---

## 参考文档

- [TV_ADAPTATION_GUIDE.md](./TV_ADAPTATION_GUIDE.md) - 完整适配指南
- [TV_PLAYER_EXAMPLE.md](./TV_PLAYER_EXAMPLE.md) - 示例代码
- [Android TV 官方文档](https://developer.android.com/training/tv)

---

**预计总时间**: 约 1 小时完成基础 TV 适配 ✅
