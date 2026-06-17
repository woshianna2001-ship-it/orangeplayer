package com.orange.playerlibrary.component;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orange.playerlibrary.OrangePlayerConfig;
import com.orange.playerlibrary.OrangeVideoController;
import com.orange.playerlibrary.PlayerConstants;
import com.orange.playerlibrary.R;
import com.orange.playerlibrary.interfaces.ControlWrapper;
import com.orange.playerlibrary.interfaces.IControlComponent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 视频播放器标题视图组件
 * 显示标题、系统时间、电池状态等信息
 * 
 * 功能特性:
 * - 显示视频标题（支持跑马灯效果）
 * - 显示系统时间（全屏时）
 * - 显示电池电量（通过广播接收器实时更新）
 * - 显示直播标识（直播模式时）
 * - 返回按钮（全屏时退出全屏，非全屏时关闭Activity）
 * - 投屏按钮（可选，TV 模式下隐藏）
 * - 设置按钮（可选）
 * - 小窗按钮（可选，TV 模式下隐藏）
 * - 定时按钮（可选）
 * 
 * Requirements: 1.2, 1.3, 2.1, 2.2, 3.1, 3.2, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7
 */
public class TitleView extends FrameLayout implements IControlComponent {
    
    private static final String TAG = "TitleView";
    private static boolean sDebug = false;
    
    // 控制包装器
    private ControlWrapper mControlWrapper;
    private OrangeVideoController mOrangeVideoController;
    
    // UI 组件
    private LinearLayout mTitleContainer;
    private LinearLayout mBatteryTimeContainer;
    private ImageView mBack;
    private TextView mTitle;
    private TextView mSysTime;
    private ImageView mBattery;
    private ImageView mLive;
    private ImageView mCast;
    private ImageView mSettings;
    private ImageView mWindow;
    private ImageView mTimer;
    private ImageView mSniffing;
    
    // 电池接收器
    private BatteryReceiver mBatteryReceiver;
    private boolean mIsRegister = false;
    
    // TV 模式
    private boolean mIsTvMode = false;
    
    // 点击事件监听器
    private View.OnClickListener mOnSettingsClickListener;
    private View.OnClickListener mOnCastClickListener;
    private View.OnClickListener mOnWindowClickListener;
    private View.OnClickListener mOnTimerClickListener;
    private View.OnClickListener mOnSniffingClickListener;

    public TitleView(@NonNull Context context) {
        super(context);
        initView();
    }

    public TitleView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public TitleView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    /**
     * 初始化视图组件
     * 
     * 加载 XML 布局并初始化所有 UI 组件:
     * 1. 加载 orange_layout_title_view.xml 布局
     * 2. 通过 findViewById 获取所有视图引用
     * 3. 设置按钮点击事件监听器
     * 4. 初始化电池接收器
     * 5. TV 模式下隐藏投屏和小窗按钮
     * 
     * Requirements: 2.1, 3.2
     */
    private void initView() {
        setVisibility(GONE);
        
        // 检测 TV 模式
        mIsTvMode = OrangePlayerConfig.isTvMode(getContext());
        
        LayoutInflater.from(getContext()).inflate(R.layout.orange_layout_title_view, this, true);
        
        mTitleContainer = findViewById(R.id.title_container);
        mBatteryTimeContainer = findViewById(R.id.battery_time_container);
        mBack = findViewById(R.id.back);
        mTitle = findViewById(R.id.title);
        mSysTime = findViewById(R.id.sys_time);
        mBattery = findViewById(R.id.iv_battery);
        mLive = findViewById(R.id.live);
        mCast = findViewById(R.id.iv_cast);
        mSettings = findViewById(R.id.iv_settings);
        mWindow = findViewById(R.id.iv_window);
        mTimer = findViewById(R.id.iv_timer);
        mSniffing = findViewById(R.id.iv_sniffing);
        
        // 初始化直播标识为隐藏
        if (mLive != null) {
            mLive.setVisibility(GONE);
        }
        
        // TV 模式下隐藏投屏和小窗按钮，但保留设置按钮
        if (mIsTvMode) {
            if (mCast != null) {
                mCast.setVisibility(GONE);
            }
            if (mWindow != null) {
                mWindow.setVisibility(GONE);
            }
            // TV 模式下显示设置按钮
            if (mSettings != null) {
                mSettings.setVisibility(VISIBLE);
            }
        } else {
            // 默认显示设置和投屏按钮（全屏时可见）
            if (mSettings != null) {
                mSettings.setVisibility(VISIBLE);
            }
            if (mCast != null) {
                mCast.setVisibility(VISIBLE);
            }
        }
        
        // 设置返回按钮点击事件
        if (mBack != null) {
            mBack.setOnClickListener(v -> onBackClick());
        }
        
        // 设置投屏按钮点击事件
        if (mCast != null && !mIsTvMode) {
            mCast.setOnClickListener(v -> onCastClick());
        }
        
        // 设置设置按钮点击事件
        if (mSettings != null) {
            mSettings.setOnClickListener(v -> onSettingsClick());
        }
        
        // 设置小窗按钮点击事件
        if (mWindow != null && !mIsTvMode) {
            mWindow.setOnClickListener(v -> onWindowClick());
        }
        
        // 设置定时按钮点击事件
        if (mTimer != null) {
            mTimer.setOnClickListener(v -> onTimerClick());
        }
        
        // 设置嗅探按钮点击事件
        if (mSniffing != null) {
            mSniffing.setOnClickListener(v -> onSniffingClick());
        }
        
        // 初始化电池接收器
        mBatteryReceiver = new BatteryReceiver(mBattery);
    }


    /**
     * 设置关联的控制器
     */
    public void setOrangeVideoController(OrangeVideoController controller) {
        mOrangeVideoController = controller;
    }

    /**
     * 返回按钮点击处理
     * 
     * 行为逻辑:
     * - 全屏模式: 调用 toggleFullScreen() 退出全屏
     * - 非全屏模式: 调用 activity.finish() 关闭 Activity
     * 
     * Requirements: 6.7
     */
    private void onBackClick() {
        Context context = getContext();
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (mControlWrapper != null && mControlWrapper.isFullScreen()) {
                // 退出全屏
                mControlWrapper.toggleFullScreen();
            } else {
                // 非全屏时，关闭 Activity
                activity.finish();
            }
        }
    }

    /**
     * 投屏按钮点击处理
     */
    private void onCastClick() {
        if (mOnCastClickListener != null) {
            mOnCastClickListener.onClick(mCast);
        }
    }

    /**
     * 设置按钮点击处理
     */
    private void onSettingsClick() {
        if (mOnSettingsClickListener != null) {
            mOnSettingsClickListener.onClick(mSettings);
        }
    }
    
    /**
     * 小窗按钮点击处理
     */
    private void onWindowClick() {
        if (mOnWindowClickListener != null) {
            mOnWindowClickListener.onClick(mWindow);
        }
    }
    
    /**
     * 定时按钮点击处理
     */
    private void onTimerClick() {
        if (mOnTimerClickListener != null) {
            mOnTimerClickListener.onClick(mTimer);
        }
    }
    
    /**
     * 嗅探按钮点击处理
     */
    private void onSniffingClick() {
        if (mOnSniffingClickListener != null) {
            mOnSniffingClickListener.onClick(mSniffing);
        }
    }
    
    /**
     * 设置设置按钮点击监听器
     */
    public void setOnSettingsClickListener(View.OnClickListener listener) {
        mOnSettingsClickListener = listener;
    }
    
    /**
     * 设置投屏按钮点击监听器
     */
    public void setOnCastClickListener(View.OnClickListener listener) {
        mOnCastClickListener = listener;
    }
    
    /**
     * 设置小窗按钮点击监听器
     */
    public void setOnWindowClickListener(View.OnClickListener listener) {
        mOnWindowClickListener = listener;
    }
    
    /**
     * 设置定时按钮点击监听器
     */
    public void setOnTimerClickListener(View.OnClickListener listener) {
        mOnTimerClickListener = listener;
    }
    
    /**
     * 设置嗅探按钮点击监听器
     */
    public void setOnSniffingClickListener(View.OnClickListener listener) {
        mOnSniffingClickListener = listener;
    }
    
    // ===== 按钮显示控制 =====
    
    /**
     * 显示设置按钮
     */
    public void showSettingsButton() {
        if (mSettings != null) {
            mSettings.setVisibility(VISIBLE);
        }
    }
    
    /**
     * 隐藏设置按钮
     */
    public void hideSettingsButton() {
        if (mSettings != null) {
            mSettings.setVisibility(GONE);
        }
    }
    
    /**
     * 显示投屏按钮
     */
    public void showCastButton() {
        if (mCast != null) {
            mCast.setVisibility(VISIBLE);
        }
    }
    
    /**
     * 隐藏投屏按钮
     */
    public void hideCastButton() {
        if (mCast != null) {
            mCast.setVisibility(GONE);
        }
    }
    
    /**
     * 显示小窗按钮
     */
    public void showWindowButton() {
        if (mWindow != null) {
            mWindow.setVisibility(VISIBLE);
        }
    }
    
    /**
     * 隐藏小窗按钮
     */
    public void hideWindowButton() {
        if (mWindow != null) {
            mWindow.setVisibility(GONE);
        }
    }
    
    /**
     * 显示定时按钮
     */
    public void showTimerButton() {
        if (mTimer != null) {
            mTimer.setVisibility(VISIBLE);
        }
    }
    
    /**
     * 隐藏定时按钮
     */
    public void hideTimerButton() {
        if (mTimer != null) {
            mTimer.setVisibility(GONE);
        }
    }
    
    /**
     * 显示嗅探按钮
     */
    public void showSniffingButton() {
        if (mSniffing != null) {
            mSniffing.setVisibility(VISIBLE);
        }
    }
    
    /**
     * 隐藏嗅探按钮
     */
    public void hideSniffingButton() {
        if (mSniffing != null) {
            mSniffing.setVisibility(GONE);
        }
    }
    
    /**
     * 更新嗅探按钮状态（根据是否有嗅探结果）
     */
    public void updateSniffingButton(boolean hasResults) {
        if (mSniffing != null) {
            mSniffing.setVisibility(hasResults ? VISIBLE : GONE);
        }
    }

    @Override
    public void attach(@NonNull ControlWrapper controlWrapper) {
        mControlWrapper = controlWrapper;
    }
    
    /**
     * 设置 OrangeVideoController（用于绑定事件）
     */
    public void setController(OrangeVideoController controller) {
        mOrangeVideoController = controller;
        
        // 绑定事件
        if (controller != null) {
            com.orange.playerlibrary.VideoEventManager eventManager = 
                    controller.getVideoEventManager();
            if (eventManager != null) {
                eventManager.bindTitleView(this);
            }
        }
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void onVisibilityChanged(boolean isVisible, Animation anim) {
        if (mControlWrapper == null) return;
        
        if (mControlWrapper.isFullScreen()) {
            if (isVisible) {
                if (getVisibility() == GONE) {
                    updateSysTime();
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
    }

    @Override
    public void onPlayStateChanged(int playState) {
        switch (playState) {
            case PlayerConstants.STATE_ERROR:
            case PlayerConstants.STATE_IDLE:
            case PlayerConstants.STATE_PREPARING:
            case PlayerConstants.STATE_PREPARED:
            case PlayerConstants.STATE_PLAYBACK_COMPLETED:
            case 8: // 移动网络警告
                setVisibility(GONE);
                break;
        }
    }

    @Override
    public void onPlayerStateChanged(int playerState) {
        if (playerState == PlayerConstants.PLAYER_FULL_SCREEN) {
            // 全屏模式
            if (mControlWrapper != null && mControlWrapper.isLocked()) {
                setVisibility(GONE);
            } else {
                setVisibility(VISIBLE);
                updateSysTime();
                // 全屏时更新标题显示
                if (mOrangeVideoController != null && mTitle != null) {
                    String title = mOrangeVideoController.getVideoTitle();
                    if (title != null && !title.isEmpty()) {
                        mTitle.setText(title);
                    }
                }
                if (mTitle != null) {
                    mTitle.setSelected(true);
                }
                // 显示电量和时间容器
                if (mBatteryTimeContainer != null) {
                    mBatteryTimeContainer.setVisibility(VISIBLE);
                }
            }
        } else {
            // 非全屏模式
            setVisibility(GONE);
            if (mTitle != null) {
                mTitle.setSelected(false);
            }
            // 隐藏电量和时间容器
            if (mBatteryTimeContainer != null) {
                mBatteryTimeContainer.setVisibility(GONE);
            }
        }
        
        // 直播模式标识
        if (mOrangeVideoController != null && mOrangeVideoController.isLiveVideoModel()) {
            if (mLive != null) {
                mLive.setVisibility(VISIBLE);
            }
        } else {
            if (mLive != null) {
                mLive.setVisibility(GONE);
            }
        }
    }

    @Override
    public void setProgress(int duration, int position) {
        // 空实现
    }

    @Override
    public void onLockStateChanged(boolean isLocked) {
        if (isLocked) {
            setVisibility(GONE);
        } else {
            // 解锁时，如果在全屏模式，立即显示标题栏
            if (mControlWrapper != null && mControlWrapper.isFullScreen()) {
                setVisibility(VISIBLE);
            }
        }
    }

    // ===== 标题设置 =====

    /**
     * 设置标题文本
     */
    public void setTitle(String title) {
        if (mTitle != null) {
            mTitle.setText(title);
        }
    }

    /**
     * 获取标题文本
     */
    public String getTitle() {
        return mTitle != null ? mTitle.getText().toString() : "";
    }

    // ===== 系统时间 =====

    /**
     * 更新系统时间显示
     * 
     * 在以下场景调用:
     * - 控制器可见性变化时
     * - 播放器状态变化时（进入全屏）
     * - 锁定状态变化时（解锁）
     * 
     * Requirements: 6.6
     */
    private void updateSysTime() {
        if (mSysTime != null) {
            mSysTime.setText(getCurrentSystemTime());
        }
    }

    /**
     * 获取当前系统时间
     */
    private String getCurrentSystemTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }

    // ===== 生命周期 =====

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mIsRegister) {
            getContext().registerReceiver(mBatteryReceiver, 
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            mIsRegister = true;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mIsRegister) {
            getContext().unregisterReceiver(mBatteryReceiver);
            mIsRegister = false;
        }
    }

    // ===== 电池接收器 =====

    /**
     * 电池状态接收器
     * 
     * 监听系统电池状态变化广播 (ACTION_BATTERY_CHANGED)
     * 并实时更新电池图标的显示级别
     * 
     * Requirements: 6.5
     */
    private static class BatteryReceiver extends BroadcastReceiver {
        private final ImageView mBatteryView;

        public BatteryReceiver(ImageView batteryView) {
            mBatteryView = batteryView;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            if (extras != null && mBatteryView != null) {
                int level = extras.getInt("level", 0);
                int scale = extras.getInt("scale", 100);
                int batteryLevel = (level * 100) / scale;
                int status = extras.getInt("status", 0);
                
                // 检查是否在充电
                boolean isCharging = status == 2 || status == 5; // BATTERY_STATUS_CHARGING or BATTERY_STATUS_FULL
                
                if (mBatteryView.getDrawable() != null) {
                    if (isCharging && batteryLevel < 100) {
                        // 充电中显示充电图标
                        mBatteryView.setImageResource(R.drawable.ic_battery_charging);
                    } else {
                        // 显示电量级别图标
                        mBatteryView.setImageResource(R.drawable.ic_battery_level);
                        // level-list 使用 0-100 范围
                        mBatteryView.getDrawable().setLevel(batteryLevel);
                    }
                }
            }
        }
    }

    // ===== 获取组件 =====

    public LinearLayout getTitleContainer() {
        return mTitleContainer;
    }

    public ImageView getBackButton() {
        return mBack;
    }

    public TextView getTitleTextView() {
        return mTitle;
    }

    public ImageView getCastButton() {
        return mCast;
    }

    public ImageView getSettingsButton() {
        return mSettings;
    }
    
    public ImageView getWindowButton() {
        return mWindow;
    }
    
    public ImageView getTimerButton() {
        return mTimer;
    }
    
    public ImageView getSniffingButton() {
        return mSniffing;
    }
    
    // ===== 电量与时间相关 =====
    
    /**
     * 获取电池图标控件
     * @return 电池图标ImageView
     */
    public ImageView getBatteryIcon() {
        return mBattery;
    }
    
    /**
     * 获取系统时间文本控件
     * @return 系统时间TextView
     */
    public TextView getSysTimeText() {
        return mSysTime;
    }
    
    /**
     * 获取电量和时间容器
     * @return 包含电池图标和时间文本的容器
     */
    public LinearLayout getBatteryTimeContainer() {
        return mBatteryTimeContainer;
    }
    
    /**
     * 设置电池图标是否可见
     * @param visible true显示，false隐藏
     */
    public void setBatteryVisible(boolean visible) {
        if (mBattery != null) {
            mBattery.setVisibility(visible ? VISIBLE : GONE);
        }
    }
    
    /**
     * 设置系统时间文本是否可见
     * @param visible true显示，false隐藏
     */
    public void setSysTimeVisible(boolean visible) {
        if (mSysTime != null) {
            mSysTime.setVisibility(visible ? VISIBLE : GONE);
        }
    }
    
    /**
     * 设置电量和时间区域整体是否可见
     * 注意：全屏模式下会自动显示，非全屏自动隐藏
     * @param visible true显示，false隐藏
     */
    public void setBatteryTimeVisible(boolean visible) {
        if (mBatteryTimeContainer != null) {
            mBatteryTimeContainer.setVisibility(visible ? VISIBLE : GONE);
        }
    }
    
    /**
     * 设置电池图标资源
     * @param resId drawable资源ID
     */
    public void setBatteryIconResource(int resId) {
        if (mBattery != null) {
            mBattery.setImageResource(resId);
        }
    }
    
    /**
     * 设置直播标识是否可见
     * @param visible true显示，false隐藏
     */
    public void setLiveVisible(boolean visible) {
        if (mLive != null) {
            mLive.setVisibility(visible ? VISIBLE : GONE);
        }
    }
    
    /**
     * 获取直播标识控件
     * @return 直播标识ImageView
     */
    public ImageView getLiveIndicator() {
        return mLive;
    }

    // ===== 调试相关 =====

    public void setDebug(boolean debug) {
        sDebug = debug;
    }

    private void debug(Object message) {
        if (sDebug && OrangeVideoController.isdebug()) {
        }
    }
}
