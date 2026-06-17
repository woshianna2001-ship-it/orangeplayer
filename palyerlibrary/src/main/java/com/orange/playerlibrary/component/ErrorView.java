package com.orange.playerlibrary.component;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orange.playerlibrary.OrangeVideoController;
import com.orange.playerlibrary.OrangevideoView;
import com.orange.playerlibrary.PlayerConstants;
import com.orange.playerlibrary.R;
import com.orange.playerlibrary.VideoEventManager;
import com.orange.playerlibrary.interfaces.ControlWrapper;
import com.orange.playerlibrary.interfaces.IControlComponent;

/**
 * 错误视图
 * 显示错误信息和重试按钮
 * 
 * Requirements: 3.6
 */
public class ErrorView extends LinearLayout implements IControlComponent {
    
    private static final String TAG = "ErrorView";
    private static boolean sDebug = false;
    
    private ControlWrapper mControlWrapper;
    private OrangeVideoController mOrangeController;
    
    // UI 组件
    private ImageView mStopFullscreen;
    private ImageView mRetryButton;
    private ImageView mSettingsButton;
    
    // 触摸状态
    private float mDownX;
    private float mDownY;

    public ErrorView(@NonNull Context context) {
        super(context);
        init();
    }

    public ErrorView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ErrorView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setVisibility(GONE);
        LayoutInflater.from(getContext()).inflate(R.layout.orange_layout_error_view, this, true);
        
        // 重试按钮
        mRetryButton = findViewById(R.id.status_btn);
        if (mRetryButton != null) {
            mRetryButton.setOnClickListener(v -> {
                setVisibility(GONE);
                if (mControlWrapper != null) {
                    mControlWrapper.replay(false);
                }
            });
        }
        
        // 设置按钮
        mSettingsButton = findViewById(R.id.error_settings_btn);
        if (mSettingsButton != null) {
            mSettingsButton.setOnClickListener(v -> showSettingsDialog());
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
        if (playState == PlayerConstants.STATE_ERROR) {
            bringToFront();
            setVisibility(VISIBLE);
        } else if (playState == PlayerConstants.STATE_IDLE) {
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
        // 锁定时隐藏错误视图
        if (isLocked && getVisibility() == VISIBLE) {
            setVisibility(GONE);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getX();
                mDownY = event.getY();
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
                
            case MotionEvent.ACTION_MOVE:
                float diffX = Math.abs(event.getX() - mDownX);
                float diffY = Math.abs(event.getY() - mDownY);
                int touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
                
                if (diffX > touchSlop || diffY > touchSlop) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    private void stopFullScreen() {
        if (mControlWrapper == null || !mControlWrapper.isFullScreen()) {
            return;
        }
        
        // 使用 ControlWrapper 退出全屏，它会正确处理全屏切换
        mControlWrapper.toggleFullScreen();
    }
    
    /**
     * 显示设置对话框（用于切换播放内核）
     */
    private void showSettingsDialog() {
        // 获取 VideoView
        OrangevideoView videoView = getVideoView();
        if (videoView == null) {
            return;
        }
        
        // 获取 Activity
        Context context = getContext();
        Activity activity = null;
        if (context instanceof Activity) {
            activity = (Activity) context;
        }
        
        if (activity == null) {
            return;
        }
        
        // 获取 Controller
        OrangeVideoController controller = mOrangeController;
        if (controller == null && videoView != null) {
            // 尝试从 VideoView 获取 Controller
            controller = (OrangeVideoController) videoView.getVideoController();
        }
        
        if (controller == null) {
            return;
        }
        
        // 使用 VideoEventManager 显示设置对话框
        VideoEventManager eventManager = new VideoEventManager(activity, videoView, controller);
        eventManager.showSetupDialog();
    }
    
    /**
     * 获取 VideoView
     */
    private OrangevideoView getVideoView() {
        if (mControlWrapper != null) {
            View parent = (View) getParent();
            while (parent != null) {
                if (parent instanceof OrangevideoView) {
                    return (OrangevideoView) parent;
                }
                if (parent.getParent() instanceof View) {
                    parent = (View) parent.getParent();
                } else {
                    break;
                }
            }
        }
        return null;
    }
    
    // ===== 控件获取方法 =====
    
    /**
     * 获取重试按钮
     * @return 重试按钮ImageView
     */
    public ImageView getRetryButton() {
        return mRetryButton;
    }
    
    /**
     * 获取设置按钮
     * @return 设置按钮ImageView
     */
    public ImageView getSettingsButton() {
        return mSettingsButton;
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
