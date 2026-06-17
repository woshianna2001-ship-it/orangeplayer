# TV 播放器实现示例

本文档提供完整的 TV 播放器实现代码示例。

## 目录

1. [TvPlayerActivity 完整实现](#tvplayeractivity-完整实现)
2. [TV 控制器适配](#tv-控制器适配)
3. [焦点管理器](#焦点管理器)
4. [遥控器事件处理](#遥控器事件处理)

---

## TvPlayerActivity 完整实现

```java
package com.orange.player.tv;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.orange.playerlibrary.OrangevideoView;
import com.orange.playerlibrary.listener.VideoListener;

/**
 * Android TV 播放器 Activity
 * 
 * 特点：
 * 1. 支持遥控器导航
 * 2. 自动隐藏控制栏
 * 3. 焦点管理
 * 4. 大按钮设计
 */
public class TvPlayerActivity extends AppCompatActivity {
    
    private static final int CONTROL_BAR_HIDE_DELAY = 5000; // 5秒后隐藏控制栏
    
    // 播放器
    private OrangevideoView videoPlayer;
    
    // 控制栏
    private View controlBar;
    private ImageButton btnPlayPause;
    private ImageButton btnRewind;
    private ImageButton btnForward;
    private SeekBar seekBar;
    private TextView tvCurrentTime;
    private TextView tvDuration;
    private TextView tvTitle;
    private ProgressBar loading;
    
    // 状态
    private boolean isControlBarVisible = true;
    private Handler hideHandler = new Handler(Looper.getMainLooper());
    private Runnable hideRunnable = this::hideControlBar;
    
    // 视频信息
    private String videoUrl;
    private String videoTitle;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tv_player);
        
        // 获取视频信息
        videoUrl = getIntent().getStringExtra("video_url");
        videoTitle = getIntent().getStringExtra("video_title");
        
        initViews();
        setupPlayer();
        setupControls();
        startPlayback();
    }
    
    private void initViews() {
        videoPlayer = findViewById(R.id.video_player);
        controlBar = findViewById(R.id.control_bar);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnRewind = findViewById(R.id.btn_rewind);
        btnForward = findViewById(R.id.btn_forward);
        seekBar = findViewById(R.id.seek_bar);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvDuration = findViewById(R.id.tv_duration);
        tvTitle = findViewById(R.id.tv_title);
        loading = findViewById(R.id.loading);
        
        // 设置标题
        if (videoTitle != null) {
            tvTitle.setText(videoTitle);
        }
        
        // 默认焦点在播放按钮
        btnPlayPause.requestFocus();
    }
    
    private void setupPlayer() {
        videoPlayer.setVideoListener(new VideoListener() {
            @Override
            public void onPrepared() {
                loading.setVisibility(View.GONE);
                updateDuration();
                startProgressUpdate();
            }
            
            @Override
            public void onCompletion() {
                // 播放完成
                btnPlayPause.setImageResource(R.drawable.ic_replay);
            }
            
            @Override
            public void onError(int what, int extra) {
                loading.setVisibility(View.GONE);
                showError("播放出错");
            }
            
            @Override
            public void onBufferingUpdate(int percent) {
                // 更新缓冲进度
            }
        });
    }
    
    private void setupControls() {
        // 播放/暂停按钮
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnPlayPause.setOnFocusChangeListener((v, hasFocus) -> {
            animateFocus(v, hasFocus);
            if (hasFocus) {
                resetHideTimer();
            }
        });
        
        // 快退按钮
        btnRewind.setOnClickListener(v -> seekBackward(10000));
        btnRewind.setOnFocusChangeListener((v, hasFocus) -> {
            animateFocus(v, hasFocus);
            if (hasFocus) {
                resetHideTimer();
            }
        });
        
        // 快进按钮
        btnForward.setOnClickListener(v -> seekForward(10000));
        btnForward.setOnFocusChangeListener((v, hasFocus) -> {
            animateFocus(v, hasFocus);
            if (hasFocus) {
                resetHideTimer();
            }
        });
        
        // 进度条
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvCurrentTime.setText(formatTime(progress));
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopProgressUpdate();
                resetHideTimer();
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                videoPlayer.seekTo(seekBar.getProgress());
                startProgressUpdate();
            }
        });
        
        seekBar.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                resetHideTimer();
            }
        });
    }
    
    private void startPlayback() {
        loading.setVisibility(View.VISIBLE);
        videoPlayer.setUp(videoUrl, true, "");
        videoPlayer.startPlayLogic();
    }
    
    private void togglePlayPause() {
        if (videoPlayer.isPlaying()) {
            videoPlayer.onVideoPause();
            btnPlayPause.setImageResource(R.drawable.ic_play);
            stopProgressUpdate();
        } else {
            videoPlayer.onVideoResume();
            btnPlayPause.setImageResource(R.drawable.ic_pause);
            startProgressUpdate();
        }
        resetHideTimer();
    }
    
    private void seekForward(long milliseconds) {
        long currentPosition = videoPlayer.getCurrentPositionWhenPlaying();
        long duration = videoPlayer.getDuration();
        long newPosition = Math.min(currentPosition + milliseconds, duration);
        videoPlayer.seekTo(newPosition);
        updateProgress();
        resetHideTimer();
    }
    
    private void seekBackward(long milliseconds) {
        long currentPosition = videoPlayer.getCurrentPositionWhenPlaying();
        long newPosition = Math.max(currentPosition - milliseconds, 0);
        videoPlayer.seekTo(newPosition);
        updateProgress();
        resetHideTimer();
    }
    
    private void showControlBar() {
        if (!isControlBarVisible) {
            controlBar.setVisibility(View.VISIBLE);
            controlBar.animate()
                    .alpha(1.0f)
                    .setDuration(300)
                    .start();
            isControlBarVisible = true;
            
            // 恢复焦点到播放按钮
            btnPlayPause.requestFocus();
        }
        resetHideTimer();
    }
    
    private void hideControlBar() {
        if (isControlBarVisible && videoPlayer.isPlaying()) {
            controlBar.animate()
                    .alpha(0.0f)
                    .setDuration(300)
                    .withEndAction(() -> controlBar.setVisibility(View.GONE))
                    .start();
            isControlBarVisible = false;
        }
    }
    
    private void resetHideTimer() {
        hideHandler.removeCallbacks(hideRunnable);
        if (videoPlayer.isPlaying()) {
            hideHandler.postDelayed(hideRunnable, CONTROL_BAR_HIDE_DELAY);
        }
    }
    
    private void animateFocus(View view, boolean hasFocus) {
        if (hasFocus) {
            view.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(200)
                    .start();
        } else {
            view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start();
        }
    }
    
    // 进度更新
    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            progressHandler.postDelayed(this, 1000);
        }
    };
    
    private void startProgressUpdate() {
        progressHandler.removeCallbacks(progressRunnable);
        progressHandler.post(progressRunnable);
    }
    
    private void stopProgressUpdate() {
        progressHandler.removeCallbacks(progressRunnable);
    }
    
    private void updateProgress() {
        long currentPosition = videoPlayer.getCurrentPositionWhenPlaying();
        long duration = videoPlayer.getDuration();
        
        if (duration > 0) {
            seekBar.setMax((int) duration);
            seekBar.setProgress((int) currentPosition);
            tvCurrentTime.setText(formatTime(currentPosition));
        }
    }
    
    private void updateDuration() {
        long duration = videoPlayer.getDuration();
        tvDuration.setText(formatTime(duration));
        seekBar.setMax((int) duration);
    }
    
    private String formatTime(long milliseconds) {
        int seconds = (int) (milliseconds / 1000);
        int minutes = seconds / 60;
        int hours = minutes / 60;
        
        seconds = seconds % 60;
        minutes = minutes % 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
    
    private void showError(String message) {
        // 显示错误提示
        tvTitle.setText(message);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (!isControlBarVisible) {
                    showControlBar();
                    return true;
                }
                // 如果控制栏可见，让焦点的按钮处理
                return super.onKeyDown(keyCode, event);
                
            case KeyEvent.KEYCODE_DPAD_UP:
                if (!isControlBarVisible) {
                    showControlBar();
                    return true;
                }
                return super.onKeyDown(keyCode, event);
                
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (isControlBarVisible) {
                    hideControlBar();
                    return true;
                }
                return super.onKeyDown(keyCode, event);
                
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (!isControlBarVisible) {
                    seekBackward(10000);
                    showControlBar();
                    return true;
                }
                return super.onKeyDown(keyCode, event);
                
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (!isControlBarVisible) {
                    seekForward(10000);
                    showControlBar();
                    return true;
                }
                return super.onKeyDown(keyCode, event);
                
            case KeyEvent.KEYCODE_BACK:
                finish();
                return true;
                
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                togglePlayPause();
                return true;
                
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                if (!videoPlayer.isPlaying()) {
                    togglePlayPause();
                }
                return true;
                
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                if (videoPlayer.isPlaying()) {
                    togglePlayPause();
                }
                return true;
                
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                seekForward(30000);
                return true;
                
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                seekBackward(30000);
                return true;
        }
        
        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        videoPlayer.onVideoPause();
        stopProgressUpdate();
        hideHandler.removeCallbacks(hideRunnable);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        videoPlayer.onVideoResume();
        startProgressUpdate();
        resetHideTimer();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoPlayer.release();
        stopProgressUpdate();
        hideHandler.removeCallbacks(hideRunnable);
    }
}
```

---

## TV 控制器适配

如果要适配现有的 `VodControlView`，可以创建 TV 专用版本：

```java
package com.orange.playerlibrary.component;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;

/**
 * TV 专用控制器
 * 继承自 VodControlView，添加 TV 特性
 */
public class TvVodControlView extends VodControlView {
    
    private boolean isControlBarVisible = true;
    
    public TvVodControlView(Context context) {
        super(context);
        init();
    }
    
    public TvVodControlView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
        // 设置所有按钮可获得焦点
        setFocusableButtons();
        
        // 添加焦点监听
        setupFocusListeners();
    }
    
    private void setFocusableButtons() {
        // 播放按钮
        if (mStartButton != null) {
            mStartButton.setFocusable(true);
            mStartButton.setFocusableInTouchMode(false);
        }
        
        // 全屏按钮
        if (mFullscreenButton != null) {
            mFullscreenButton.setFocusable(true);
            mFullscreenButton.setFocusableInTouchMode(false);
        }
        
        // 进度条
        if (mProgressBar != null) {
            mProgressBar.setFocusable(true);
            mProgressBar.setFocusableInTouchMode(false);
        }
    }
    
    private void setupFocusListeners() {
        View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            if (hasFocus) {
                // 放大动画
                v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start();
                
                // 重置自动隐藏计时器
                resetHideTimer();
            } else {
                // 恢复原大小
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
            }
        };
        
        if (mStartButton != null) {
            mStartButton.setOnFocusChangeListener(focusListener);
        }
        
        if (mFullscreenButton != null) {
            mFullscreenButton.setOnFocusChangeListener(focusListener);
        }
        
        if (mProgressBar != null) {
            mProgressBar.setOnFocusChangeListener(focusListener);
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                // 如果控制栏隐藏，显示它
                if (!isControlBarVisible) {
                    show();
                    return true;
                }
                break;
                
            case KeyEvent.KEYCODE_DPAD_UP:
                // 显示控制栏
                if (!isControlBarVisible) {
                    show();
                    return true;
                }
                break;
                
            case KeyEvent.KEYCODE_DPAD_DOWN:
                // 隐藏控制栏
                if (isControlBarVisible) {
                    hide();
                    return true;
                }
                break;
        }
        
        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    public void show() {
        super.show();
        isControlBarVisible = true;
        
        // 恢复焦点
        if (mStartButton != null) {
            mStartButton.requestFocus();
        }
    }
    
    @Override
    public void hide() {
        super.hide();
        isControlBarVisible = false;
    }
}
```

---

## 焦点管理器

创建一个焦点管理器来简化焦点处理：

```java
package com.orange.player.tv.utils;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

import java.util.ArrayList;
import java.util.List;

/**
 * TV 焦点管理器
 * 
 * 功能：
 * 1. 统一管理焦点动画
 * 2. 记录焦点历史
 * 3. 提供焦点恢复
 */
public class TvFocusManager {
    
    private static final float FOCUS_SCALE = 1.1f;
    private static final int ANIMATION_DURATION = 200;
    
    private List<View> focusableViews = new ArrayList<>();
    private View lastFocusedView;
    
    /**
     * 注册可获得焦点的 View
     */
    public void registerFocusableView(View view) {
        if (view == null) return;
        
        focusableViews.add(view);
        view.setFocusable(true);
        view.setFocusableInTouchMode(false);
        
        // 添加焦点监听
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                onViewFocused(v);
            } else {
                onViewUnfocused(v);
            }
        });
    }
    
    /**
     * 批量注册
     */
    public void registerFocusableViews(View... views) {
        for (View view : views) {
            registerFocusableView(view);
        }
    }
    
    /**
     * View 获得焦点时的处理
     */
    private void onViewFocused(View view) {
        lastFocusedView = view;
        
        // 放大动画
        ScaleAnimation scaleUp = new ScaleAnimation(
                1.0f, FOCUS_SCALE,
                1.0f, FOCUS_SCALE,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleUp.setDuration(ANIMATION_DURATION);
        scaleUp.setFillAfter(true);
        view.startAnimation(scaleUp);
    }
    
    /**
     * View 失去焦点时的处理
     */
    private void onViewUnfocused(View view) {
        // 恢复原大小
        ScaleAnimation scaleDown = new ScaleAnimation(
                FOCUS_SCALE, 1.0f,
                FOCUS_SCALE, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleDown.setDuration(ANIMATION_DURATION);
        scaleDown.setFillAfter(true);
        view.startAnimation(scaleDown);
    }
    
    /**
     * 恢复上次的焦点
     */
    public void restoreLastFocus() {
        if (lastFocusedView != null) {
            lastFocusedView.requestFocus();
        } else if (!focusableViews.isEmpty()) {
            focusableViews.get(0).requestFocus();
        }
    }
    
    /**
     * 清除所有焦点
     */
    public void clearFocus() {
        for (View view : focusableViews) {
            view.clearFocus();
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        focusableViews.clear();
        lastFocusedView = null;
    }
}
```

使用示例：

```java
public class TvPlayerActivity extends AppCompatActivity {
    
    private TvFocusManager focusManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tv_player);
        
        // 初始化焦点管理器
        focusManager = new TvFocusManager();
        
        // 注册所有可获得焦点的 View
        focusManager.registerFocusableViews(
                btnPlayPause,
                btnRewind,
                btnForward,
                seekBar
        );
        
        // 恢复焦点
        focusManager.restoreLastFocus();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        focusManager.release();
    }
}
```

---

## 遥控器事件处理

创建一个遥控器事件处理器：

```java
package com.orange.player.tv.utils;

import android.view.KeyEvent;

/**
 * TV 遥控器事件处理器
 */
public class TvRemoteHandler {
    
    public interface RemoteListener {
        void onPlayPause();
        void onSeekForward(long milliseconds);
        void onSeekBackward(long milliseconds);
        void onShowControls();
        void onHideControls();
        void onBack();
    }
    
    private RemoteListener listener;
    
    public TvRemoteHandler(RemoteListener listener) {
        this.listener = listener;
    }
    
    /**
     * 处理按键事件
     * 
     * @return true 如果事件被处理
     */
    public boolean handleKeyEvent(int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }
        
        switch (keyCode) {
            // 确认键
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                listener.onPlayPause();
                return true;
                
            // 方向键
            case KeyEvent.KEYCODE_DPAD_LEFT:
                listener.onSeekBackward(10000);
                return true;
                
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                listener.onSeekForward(10000);
                return true;
                
            case KeyEvent.KEYCODE_DPAD_UP:
                listener.onShowControls();
                return true;
                
            case KeyEvent.KEYCODE_DPAD_DOWN:
                listener.onHideControls();
                return true;
                
            // 返回键
            case KeyEvent.KEYCODE_BACK:
                listener.onBack();
                return true;
                
            // 媒体控制键
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                listener.onPlayPause();
                return true;
                
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                listener.onPlayPause();
                return true;
                
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                listener.onSeekForward(30000);
                return true;
                
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                listener.onSeekBackward(30000);
                return true;
                
            default:
                return false;
        }
    }
}
```

使用示例：

```java
public class TvPlayerActivity extends AppCompatActivity {
    
    private TvRemoteHandler remoteHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 初始化遥控器处理器
        remoteHandler = new TvRemoteHandler(new TvRemoteHandler.RemoteListener() {
            @Override
            public void onPlayPause() {
                togglePlayPause();
            }
            
            @Override
            public void onSeekForward(long milliseconds) {
                seekForward(milliseconds);
            }
            
            @Override
            public void onSeekBackward(long milliseconds) {
                seekBackward(milliseconds);
            }
            
            @Override
            public void onShowControls() {
                showControlBar();
            }
            
            @Override
            public void onHideControls() {
                hideControlBar();
            }
            
            @Override
            public void onBack() {
                finish();
            }
        });
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 先让遥控器处理器处理
        if (remoteHandler.handleKeyEvent(keyCode, event)) {
            return true;
        }
        
        return super.onKeyDown(keyCode, event);
    }
}
```

---

## 总结

以上代码提供了完整的 TV 播放器实现，包括：

1. ✅ 完整的 TvPlayerActivity
2. ✅ TV 控制器适配
3. ✅ 焦点管理器
4. ✅ 遥控器事件处理

这些代码可以直接集成到你的项目中，实现完整的 Android TV 支持。
