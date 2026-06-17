package com.orange.playerlibrary.component;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orange.playerlibrary.interfaces.IControlComponent;
import com.orange.playerlibrary.interfaces.ControlWrapper;

/**
 * 延迟显示组件
 * 用于显示直播流的延迟时间
 * 
 * 使用方法：
 * 1. 推流时在视频上添加时间戳水印（使用 push_rtsp_with_timestamp.bat）
 * 2. 手动对比视频中的时间戳与当前时间，计算延迟
 * 3. 或者使用 setLatency() 方法手动设置延迟值
 */
public class LatencyView extends FrameLayout implements IControlComponent {
    
    private static final String TAG = "LatencyView";
    
    private TextView mLatencyText;
    private ControlWrapper mControlWrapper;
    private boolean mIsShowing = false;
    private long mLatencyMs = 0;
    
    public LatencyView(@NonNull Context context) {
        super(context);
        init(context);
    }
    
    public LatencyView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    private void init(Context context) {
        setVisibility(GONE);
        
        // 创建延迟显示文本
        mLatencyText = new TextView(context);
        mLatencyText.setTextColor(Color.YELLOW);
        mLatencyText.setTextSize(14);
        mLatencyText.setShadowLayer(2, 1, 1, Color.BLACK);
        mLatencyText.setBackgroundColor(Color.argb(128, 0, 0, 0));
        mLatencyText.setPadding(dp2px(8), dp2px(4), dp2px(8), dp2px(4));
        mLatencyText.setText("延迟: -- ms");
        
        // 添加到右上角
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.TOP | Gravity.END;
        params.topMargin = dp2px(50);  // 避开标题栏
        params.rightMargin = dp2px(10);
        
        addView(mLatencyText, params);
    }
    
    /**
     * 设置延迟值（毫秒）
     */
    public void setLatency(long latencyMs) {
        mLatencyMs = latencyMs;
        updateLatencyText();
    }
    
    /**
     * 显示延迟信息
     */
    public void show() {
        if (!mIsShowing) {
            mIsShowing = true;
            setVisibility(VISIBLE);
        }
    }
    
    /**
     * 隐藏延迟信息
     */
    public void hide() {
        if (mIsShowing) {
            mIsShowing = false;
            setVisibility(GONE);
        }
    }
    
    /**
     * 切换显示/隐藏
     */
    public void toggle() {
        if (mIsShowing) {
            hide();
        } else {
            show();
        }
    }
    
    private void updateLatencyText() {
        if (mLatencyMs <= 0) {
            mLatencyText.setText("延迟: -- ms");
        } else if (mLatencyMs < 1000) {
            mLatencyText.setText(String.format("延迟: %d ms", mLatencyMs));
        } else {
            float seconds = mLatencyMs / 1000f;
            mLatencyText.setText(String.format("延迟: %.1f s", seconds));
        }
        
        // 根据延迟大小改变颜色
        if (mLatencyMs < 500) {
            mLatencyText.setTextColor(Color.GREEN);  // 低延迟 - 绿色
        } else if (mLatencyMs < 1000) {
            mLatencyText.setTextColor(Color.YELLOW);  // 中等延迟 - 黄色
        } else {
            mLatencyText.setTextColor(Color.RED);  // 高延迟 - 红色
        }
    }
    
    private int dp2px(float dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
    
    // ========== IControlComponent 接口实现 ==========
    
    @Override
    public void attach(@NonNull ControlWrapper controlWrapper) {
        mControlWrapper = controlWrapper;
    }
    
    @Override
    public android.view.View getView() {
        return this;
    }
    
    @Override
    public void onVisibilityChanged(boolean isVisible, android.view.animation.Animation anim) {
        // 延迟显示不受控制器显示/隐藏影响
    }
    
    @Override
    public void onPlayStateChanged(int playState) {
        // 不处理播放状态变化
    }
    
    @Override
    public void onPlayerStateChanged(int playerState) {
        // 不处理播放器状态变化
    }
    
    @Override
    public void setProgress(int duration, int position) {
        // 不处理进度更新
    }
    
    @Override
    public void onLockStateChanged(boolean isLocked) {
        // 不处理锁定状态变化
    }
}
