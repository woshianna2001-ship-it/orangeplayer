package com.orange.playerlibrary.component;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orange.playerlibrary.OrangeVideoController;
import com.orange.playerlibrary.PlayerConstants;
import com.orange.playerlibrary.R;
import com.orange.playerlibrary.interfaces.ControlWrapper;
import com.orange.playerlibrary.interfaces.IControlComponent;

/**
 * 播放完成视图
 * 显示重播按钮
 * 
 * Requirements: 3.5
 */
public class CompleteView extends FrameLayout implements IControlComponent {
    
    private static final String TAG = "CompleteView";
    private static boolean sDebug = false;
    
    private ControlWrapper mControlWrapper;
    private OrangeVideoController mOrangeController;
    
    // UI 组件
    private ImageView mStopFullscreen;
    private ImageView mReplayButton;

    public CompleteView(@NonNull Context context) {
        super(context);
        init();
    }

    public CompleteView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CompleteView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setVisibility(GONE);
        LayoutInflater.from(getContext()).inflate(R.layout.orange_layout_complete_view, this, true);
        
        // 重播按钮
        mReplayButton = findViewById(R.id.iv_replay);
        if (mReplayButton != null) {
            mReplayButton.setOnClickListener(v -> {
                if (mControlWrapper != null) {
                    mControlWrapper.replay(true);
                }
            });
        }
        
        // 退出全屏按钮
        mStopFullscreen = findViewById(R.id.stop_fullscreen);
        if (mStopFullscreen != null) {
            mStopFullscreen.setOnClickListener(v -> stopFullScreen());
        }
        
        setClickable(true);
    }

    public void setOrangeVideoController(OrangeVideoController controller) {
        mOrangeController = controller;
    }

    @Override
    public void attach(@NonNull ControlWrapper controlWrapper) {
        mControlWrapper = controlWrapper;
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void onVisibilityChanged(boolean isVisible, Animation anim) {
        // 不需要处理
    }

    @Override
    public void onPlayStateChanged(int playState) {
        if (playState == PlayerConstants.STATE_PLAYBACK_COMPLETED) {
            setVisibility(VISIBLE);
            if (mStopFullscreen != null && mControlWrapper != null) {
                mStopFullscreen.setVisibility(mControlWrapper.isFullScreen() ? VISIBLE : GONE);
            }
            bringToFront();
        } else {
            setVisibility(GONE);
        }
    }

    @Override
    public void onPlayerStateChanged(int playerState) {
        if (playerState == PlayerConstants.PLAYER_FULL_SCREEN) {
            if (mStopFullscreen != null) {
                mStopFullscreen.setVisibility(VISIBLE);
            }
        } else if (playerState == PlayerConstants.PLAYER_NORMAL) {
            if (mStopFullscreen != null) {
                mStopFullscreen.setVisibility(GONE);
            }
        }
    }

    @Override
    public void setProgress(int duration, int position) {
        // 不需要处理
    }

    @Override
    public void onLockStateChanged(boolean isLocked) {
        // 锁定时隐藏完成视图
        if (isLocked && getVisibility() == VISIBLE) {
            setVisibility(GONE);
        }
    }

    private void stopFullScreen() {
        if (mControlWrapper == null || !mControlWrapper.isFullScreen()) {
            return;
        }
        
        // 使用 ControlWrapper 退出全屏，它会正确处理全屏切换
        mControlWrapper.toggleFullScreen();
    }
    
    // ===== 控件获取方法 =====
    
    /**
     * 获取重播按钮
     * @return 重播按钮ImageView
     */
    public ImageView getReplayButton() {
        return mReplayButton;
    }
    
    /**
     * 获取退出全屏按钮
     * @return 退出全屏按钮ImageView
     */
    public ImageView getStopFullscreenButton() {
        return mStopFullscreen;
    }

    public void setDebug(boolean debug) {
        sDebug = debug;
    }
}
