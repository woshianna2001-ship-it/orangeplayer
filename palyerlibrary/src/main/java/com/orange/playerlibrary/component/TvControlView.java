package com.orange.playerlibrary.component;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.orange.playerlibrary.R;
import com.shuyu.gsyvideoplayer.video.base.GSYBaseVideoPlayer;
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer;

/**
 * TV 专属控制栏组件
 * 
 * 特点：
 * 1. 大按钮设计（适合 10-foot UI）
 * 2. 支持遥控器导航
 * 3. 焦点管理
 * 4. 自动隐藏
 */
public class TvControlView extends LinearLayout {
    
    private GSYBaseVideoPlayer mVideoPlayer;
    
    // 控制栏
    private View mControlBar;
    private ImageButton mBtnPlayPause;
    private ImageButton mBtnRewind;
    private ImageButton mBtnForward;
    private SeekBar mSeekBar;
    private TextView mTvCurrentTime;
    private TextView mTvDuration;
    private TextView mTvTitle;
    private ProgressBar mLoading;
    
    // 自动隐藏
    private static final int HIDE_DELAY = 5000;
    private Runnable mHideRunnable = this::hideControlBar;
    
    public TvControlView(Context context) {
        super(context);
        init(context);
    }
    
    public TvControlView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public TvControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.tv_control_view, this, true);
        
        mControlBar = findViewById(R.id.tv_control_bar);
        mBtnPlayPause = findViewById(R.id.tv_btn_play_pause);
        mBtnRewind = findViewById(R.id.tv_btn_rewind);
        mBtnForward = findViewById(R.id.tv_btn_forward);
        mSeekBar = findViewById(R.id.tv_seek_bar);
        mTvCurrentTime = findViewById(R.id.tv_current_time);
        mTvDuration = findViewById(R.id.tv_duration);
        mTvTitle = findViewById(R.id.tv_title);
        mLoading = findViewById(R.id.tv_loading);
        
        setupListeners();
    }
    
    private void setupListeners() {
        // 播放/暂停
        if (mBtnPlayPause != null) {
            mBtnPlayPause.setOnClickListener(v -> togglePlayPause());
            mBtnPlayPause.setOnFocusChangeListener((v, hasFocus) -> {
                animateFocus(v, hasFocus);
                if (hasFocus) {
                    resetHideTimer();
                }
            });
        }
        
        // 快退
        if (mBtnRewind != null) {
            mBtnRewind.setOnClickListener(v -> seekBackward(10000));
            mBtnRewind.setOnFocusChangeListener((v, hasFocus) -> {
                animateFocus(v, hasFocus);
                if (hasFocus) {
                    resetHideTimer();
                }
            });
        }
        
        // 快进
        if (mBtnForward != null) {
            mBtnForward.setOnClickListener(v -> seekForward(10000));
            mBtnForward.setOnFocusChangeListener((v, hasFocus) -> {
                animateFocus(v, hasFocus);
                if (hasFocus) {
                    resetHideTimer();
                }
            });
        }
        
        // 进度条
        if (mSeekBar != null) {
            mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && mTvCurrentTime != null) {
                        mTvCurrentTime.setText(formatTime(progress));
                    }
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    resetHideTimer();
                }
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (mVideoPlayer != null) {
                        mVideoPlayer.seekTo(seekBar.getProgress());
                    }
                }
            });
            
            mSeekBar.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    resetHideTimer();
                }
            });
        }
    }
    
    /**
     * 绑定播放器
     */
    public void bindVideoPlayer(GSYBaseVideoPlayer videoPlayer) {
        this.mVideoPlayer = videoPlayer;
    }
    
    /**
     * 设置标题
     */
    public void setTitle(String title) {
        if (mTvTitle != null) {
            mTvTitle.setText(title);
        }
    }
    
    /**
     * 显示加载动画
     */
    public void showLoading() {
        if (mLoading != null) {
            mLoading.setVisibility(VISIBLE);
        }
    }
    
    /**
     * 隐藏加载动画
     */
    public void hideLoading() {
        if (mLoading != null) {
            mLoading.setVisibility(GONE);
        }
    }
    
    /**
     * 显示控制栏
     */
    public void showControlBar() {
        if (mControlBar != null && mControlBar.getVisibility() != VISIBLE) {
            mControlBar.setVisibility(VISIBLE);
            mControlBar.setAlpha(0f);
            mControlBar.animate()
                    .alpha(1.0f)
                    .setDuration(300)
                    .start();
            
            // 恢复焦点
            if (mBtnPlayPause != null) {
                mBtnPlayPause.requestFocus();
            }
        }
        resetHideTimer();
    }
    
    /**
     * 隐藏控制栏
     */
    public void hideControlBar() {
        if (mControlBar != null && mControlBar.getVisibility() == VISIBLE) {
            if (mVideoPlayer != null && mVideoPlayer.getCurrentState() == GSYVideoPlayer.CURRENT_STATE_PLAYING) {
                mControlBar.animate()
                        .alpha(0.0f)
                        .setDuration(300)
                        .withEndAction(() -> mControlBar.setVisibility(GONE))
                        .start();
            }
        }
    }
    
    /**
     * 切换控制栏显示/隐藏
     */
    public void toggleControlBar() {
        if (mControlBar != null) {
            if (mControlBar.getVisibility() == VISIBLE) {
                hideControlBar();
            } else {
                showControlBar();
            }
        }
    }
    
    /**
     * 更新播放状态
     */
    public void updatePlayState(boolean isPlaying) {
        if (mBtnPlayPause != null) {
            mBtnPlayPause.setImageResource(isPlaying ? 
                    R.drawable.dkplayer_ic_action_pause : 
                    R.drawable.dkplayer_ic_action_play_arrow);
        }
    }
    
    /**
     * 更新进度
     */
    public void updateProgress(long currentPosition, long duration) {
        if (mSeekBar != null && duration > 0) {
            mSeekBar.setMax((int) duration);
            mSeekBar.setProgress((int) currentPosition);
        }
        
        if (mTvCurrentTime != null) {
            mTvCurrentTime.setText(formatTime(currentPosition));
        }
        
        if (mTvDuration != null) {
            mTvDuration.setText(formatTime(duration));
        }
    }
    
    private void togglePlayPause() {
        if (mVideoPlayer != null) {
            if (mVideoPlayer.getCurrentState() == GSYVideoPlayer.CURRENT_STATE_PLAYING) {
                mVideoPlayer.onVideoPause();
            } else {
                mVideoPlayer.onVideoResume();
            }
        }
        resetHideTimer();
    }
    
    private void seekForward(long milliseconds) {
        if (mVideoPlayer != null) {
            long currentPosition = mVideoPlayer.getCurrentPositionWhenPlaying();
            long duration = mVideoPlayer.getDuration();
            long newPosition = Math.min(currentPosition + milliseconds, duration);
            mVideoPlayer.seekTo(newPosition);
        }
        resetHideTimer();
    }
    
    private void seekBackward(long milliseconds) {
        if (mVideoPlayer != null) {
            long currentPosition = mVideoPlayer.getCurrentPositionWhenPlaying();
            long newPosition = Math.max(currentPosition - milliseconds, 0);
            mVideoPlayer.seekTo(newPosition);
        }
        resetHideTimer();
    }
    
    private void resetHideTimer() {
        removeCallbacks(mHideRunnable);
        if (mVideoPlayer != null && mVideoPlayer.getCurrentState() == GSYVideoPlayer.CURRENT_STATE_PLAYING) {
            postDelayed(mHideRunnable, HIDE_DELAY);
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
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mHideRunnable);
    }
}
