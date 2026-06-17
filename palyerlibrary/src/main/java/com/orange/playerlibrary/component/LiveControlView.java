package com.orange.playerlibrary.component;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orange.playerlibrary.OrangeVideoController;
import com.orange.playerlibrary.PlayerConstants;
import com.orange.playerlibrary.R;
import com.orange.playerlibrary.interfaces.ControlWrapper;
import com.orange.playerlibrary.interfaces.IControlComponent;

/**
 * 直播控制视图
 * 直播模式控制
 * 
 * Requirements: 3.8
 */
public class LiveControlView extends FrameLayout implements IControlComponent, View.OnClickListener {
    
    private static final String TAG = "LiveControlView";
    private static boolean sDebug = false;
    
    private ControlWrapper mControlWrapper;
    private OrangeVideoController mOrangeController;
    
    // UI 组件
    private LinearLayout mBottomContainer;
    private ImageView mPlayButton;
    private ImageView mRefreshButton;
    private ImageView mFullScreen;

    public LiveControlView(@NonNull Context context) {
        super(context);
        init();
    }

    public LiveControlView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LiveControlView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setVisibility(GONE);
        LayoutInflater.from(getContext()).inflate(R.layout.orange_layout_live_control_view, this, true);
        
        mBottomContainer = findViewById(R.id.bottom_container);
        
        mPlayButton = findViewById(R.id.iv_play);
        if (mPlayButton != null) {
            mPlayButton.setOnClickListener(this);
        }
        
        mRefreshButton = findViewById(R.id.iv_refresh);
        if (mRefreshButton != null) {
            mRefreshButton.setOnClickListener(this);
        }
        
        mFullScreen = findViewById(R.id.fullscreen);
        if (mFullScreen != null) {
            mFullScreen.setOnClickListener(this);
        }
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
        if (isVisible) {
            if (getVisibility() == GONE) {
                setVisibility(VISIBLE);
                if (anim != null) {
                    startAnimation(anim);
                }
            }
        } else {
            if (getVisibility() == VISIBLE) {
                setVisibility(GONE);
                if (anim != null) {
                    startAnimation(anim);
                }
            }
        }
    }

    @Override
    public void onPlayStateChanged(int playState) {
        switch (playState) {
            case PlayerConstants.STATE_ERROR:
            case PlayerConstants.STATE_IDLE:
            case PlayerConstants.STATE_PREPARING:
            case PlayerConstants.STATE_PREPARED:
            case PlayerConstants.STATE_PLAYBACK_COMPLETED:
                setVisibility(GONE);
                break;
                
            case PlayerConstants.STATE_PLAYING:
                if (mPlayButton != null) {
                    mPlayButton.setSelected(true);
                }
                break;
                
            case PlayerConstants.STATE_PAUSED:
                if (mPlayButton != null) {
                    mPlayButton.setSelected(false);
                }
                break;
                
            case PlayerConstants.STATE_BUFFERING:
            case PlayerConstants.STATE_BUFFERED:
                if (mPlayButton != null && mControlWrapper != null) {
                    mPlayButton.setSelected(mControlWrapper.isPlaying());
                }
                break;
        }
    }

    @Override
    public void onPlayerStateChanged(int playerState) {
        switch (playerState) {
            case PlayerConstants.PLAYER_NORMAL:
                if (mFullScreen != null) {
                    mFullScreen.setSelected(false);
                }
                break;
                
            case PlayerConstants.PLAYER_FULL_SCREEN:
                if (mFullScreen != null) {
                    mFullScreen.setSelected(true);
                }
                break;
        }
        
        // 处理刘海屏
        if (mControlWrapper != null && mControlWrapper.hasCutout()) {
            Context context = getContext();
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                int orientation = activity.getRequestedOrientation();
                if (mBottomContainer != null) {
                    if (orientation == 1) { // 竖屏
                        mBottomContainer.setPadding(0, 0, 0, 0);
                    }
                }
            }
        }
    }

    @Override
    public void setProgress(int duration, int position) {
        // 直播不需要进度
    }

    @Override
    public void onLockStateChanged(boolean isLocked) {
        onVisibilityChanged(!isLocked, null);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.fullscreen) {
            toggleFullScreen();
        } else if (id == R.id.iv_play) {
            // 直播模式禁用播放暂停
        } else if (id == R.id.iv_refresh) {
            if (mControlWrapper != null) {
                mControlWrapper.replay(true);
            }
        }
    }

    private void toggleFullScreen() {
        Context context = getContext();
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (activity.isFinishing()) {
                return;
            }
            
            if (mControlWrapper != null && mControlWrapper.isFullScreen()) {
                activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                if (mOrangeController != null) {
                    mOrangeController.stopFullScreen();
                }
            } else {
                activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                if (mOrangeController != null) {
                    mOrangeController.startFullScreen();
                }
            }
        }
    }

    public void setDebug(boolean debug) {
        sDebug = debug;
    }
}
