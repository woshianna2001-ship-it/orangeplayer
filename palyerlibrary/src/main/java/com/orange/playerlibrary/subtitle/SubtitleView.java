package com.orange.playerlibrary.subtitle;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

/**
 * 字幕显示视图（增强版）
 * 支持自动显示/隐藏、淡入淡出动画
 */
public class SubtitleView extends AppCompatTextView {

    private static final String TAG = "SubtitleView";
    
    private static final float DEFAULT_TEXT_SIZE = 16f; // sp
    private static final int DEFAULT_TEXT_COLOR = Color.WHITE;
    private static final int DEFAULT_SHADOW_COLOR = Color.BLACK;
    private static final float DEFAULT_SHADOW_RADIUS = 4f;
    private static final int DEFAULT_PADDING = 8; // dp
    
    /** 默认隐藏延迟（毫秒） */
    public static final int DEFAULT_HIDE_DELAY = 2000;
    /** 动画持续时间（毫秒） */
    private static final int ANIMATION_DURATION = 200;
    
    private Handler mHandler;
    private int mHideDelay = DEFAULT_HIDE_DELAY;
    private boolean mAutoHideEnabled = true;
    private boolean mAnimationEnabled = true;
    
    private ObjectAnimator mFadeInAnimator;
    private ObjectAnimator mFadeOutAnimator;
    private boolean mIsAnimating = false;
    private boolean mIsShowing = false;
    
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hideWithAnimation();
        }
    };

    public SubtitleView(@NonNull Context context) {
        super(context);
        init();
    }

    public SubtitleView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SubtitleView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mHandler = new Handler(Looper.getMainLooper());
        
        // 默认样式
        setTextSize(TypedValue.COMPLEX_UNIT_SP, DEFAULT_TEXT_SIZE);
        setTextColor(DEFAULT_TEXT_COLOR);
        setGravity(Gravity.CENTER);
        setTypeface(Typeface.DEFAULT_BOLD);
        
        // 文字阴影，增强可读性
        setShadowLayer(DEFAULT_SHADOW_RADIUS, 2, 2, DEFAULT_SHADOW_COLOR);
        
        // 内边距
        int padding = dpToPx(DEFAULT_PADDING);
        setPadding(padding * 2, padding, padding * 2, padding);
        
        // 半透明背景
        setBackgroundColor(0x80000000);
        
        // 默认隐藏
        setVisibility(GONE);
        setAlpha(0f);
        mIsShowing = false;
        
        // 初始化动画
        initAnimators();
    }
    
    /**
     * 初始化淡入淡出动画
     */
    private void initAnimators() {
        // 淡入动画
        mFadeInAnimator = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f);
        mFadeInAnimator.setDuration(ANIMATION_DURATION);
        mFadeInAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mFadeInAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mIsAnimating = true;
                setVisibility(VISIBLE);
            }
            
            @Override
            public void onAnimationEnd(Animator animation) {
                mIsAnimating = false;
                mIsShowing = true;
            }
            
            @Override
            public void onAnimationCancel(Animator animation) {
                mIsAnimating = false;
            }
        });
        
        // 淡出动画
        mFadeOutAnimator = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f);
        mFadeOutAnimator.setDuration(ANIMATION_DURATION);
        mFadeOutAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mFadeOutAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mIsAnimating = true;
            }
            
            @Override
            public void onAnimationEnd(Animator animation) {
                mIsAnimating = false;
                mIsShowing = false;
                setVisibility(GONE);
            }
            
            @Override
            public void onAnimationCancel(Animator animation) {
                mIsAnimating = false;
            }
        });
    }
    
    /**
     * 设置字幕文本（自动处理显示/隐藏）
     * @param text 字幕文本，null 或空字符串会触发延迟隐藏
     */
    public void setSubtitleText(String text) {
        // 取消之前的隐藏任务
        cancelHideTask();
        
        if (TextUtils.isEmpty(text)) {
            // 文本为空，延迟隐藏
            if (mAutoHideEnabled && mIsShowing) {
                scheduleHide();
            }
        } else {
            // 有文本，显示字幕
            setText(text);
            showWithAnimation();
        }
    }
    
    /**
     * 设置原文和译文
     * @param originalText 原文
     * @param translatedText 译文（可为 null）
     */
    public void setSubtitleText(String originalText, String translatedText) {
        // 取消之前的隐藏任务
        cancelHideTask();
        
        if (TextUtils.isEmpty(originalText)) {
            // 原文为空，延迟隐藏
            if (mAutoHideEnabled && mIsShowing) {
                scheduleHide();
            }
        } else {
            // 构建显示文本
            String displayText;
            if (TextUtils.isEmpty(translatedText)) {
                displayText = originalText;
            } else {
                displayText = originalText + "\n" + translatedText;
            }
            setText(displayText);
            showWithAnimation();
        }
    }
    
    /**
     * 清除字幕（触发延迟隐藏）
     */
    public void clearSubtitle() {
        setText("");
        if (mAutoHideEnabled && mIsShowing) {
            scheduleHide();
        }
    }
    
    /**
     * 设置隐藏延迟时间
     * @param delayMs 延迟毫秒数
     */
    public void setHideDelay(int delayMs) {
        mHideDelay = Math.max(0, delayMs);
    }
    
    /**
     * 获取隐藏延迟时间
     */
    public int getHideDelay() {
        return mHideDelay;
    }
    
    /**
     * 设置是否启用自动隐藏
     */
    public void setAutoHideEnabled(boolean enabled) {
        mAutoHideEnabled = enabled;
        if (!enabled) {
            cancelHideTask();
        }
    }
    
    /**
     * 是否启用自动隐藏
     */
    public boolean isAutoHideEnabled() {
        return mAutoHideEnabled;
    }
    
    /**
     * 设置是否启用动画
     */
    public void setAnimationEnabled(boolean enabled) {
        mAnimationEnabled = enabled;
    }
    
    /**
     * 是否启用动画
     */
    public boolean isAnimationEnabled() {
        return mAnimationEnabled;
    }
    
    /**
     * 立即隐藏（无动画）
     */
    public void hideImmediately() {
        cancelHideTask();
        cancelAnimations();
        setAlpha(0f);
        setVisibility(GONE);
        mIsShowing = false;
    }
    
    /**
     * 立即显示（无动画）
     */
    public void showImmediately() {
        cancelHideTask();
        cancelAnimations();
        setAlpha(1f);
        setVisibility(VISIBLE);
        mIsShowing = true;
    }
    
    /**
     * 带动画显示
     */
    private void showWithAnimation() {
        cancelHideTask();
        
        if (mIsShowing && !mIsAnimating) {
            // 已经显示，无需动画
            return;
        }
        
        cancelAnimations();
        
        if (mAnimationEnabled) {
            mFadeInAnimator.start();
        } else {
            showImmediately();
        }
    }
    
    /**
     * 带动画隐藏
     */
    private void hideWithAnimation() {
        if (!mIsShowing && !mIsAnimating) {
            // 已经隐藏，无需动画
            return;
        }
        
        cancelAnimations();
        
        if (mAnimationEnabled) {
            mFadeOutAnimator.start();
        } else {
            hideImmediately();
        }
    }
    
    /**
     * 安排延迟隐藏
     */
    private void scheduleHide() {
        cancelHideTask();
        if (mHideDelay > 0) {
            mHandler.postDelayed(mHideRunnable, mHideDelay);
        } else {
            hideWithAnimation();
        }
    }
    
    /**
     * 取消隐藏任务
     */
    private void cancelHideTask() {
        mHandler.removeCallbacks(mHideRunnable);
    }
    
    /**
     * 取消所有动画
     */
    private void cancelAnimations() {
        if (mFadeInAnimator != null && mFadeInAnimator.isRunning()) {
            mFadeInAnimator.cancel();
        }
        if (mFadeOutAnimator != null && mFadeOutAnimator.isRunning()) {
            mFadeOutAnimator.cancel();
        }
    }
    
    /**
     * 字幕是否正在显示
     */
    public boolean isSubtitleShowing() {
        return mIsShowing;
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // 清理资源
        cancelHideTask();
        cancelAnimations();
    }

    /**
     * 设置字幕文字大小
     */
    public void setTextSize(float sizeSp) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
    }

    /**
     * 设置字幕背景颜色
     */
    @Override
    public void setBackgroundColor(int color) {
        super.setBackgroundColor(color);
    }

    /**
     * 设置字幕阴影
     */
    public void setShadow(float radius, int color) {
        setShadowLayer(radius, 2, 2, color);
    }

    /**
     * 设置字幕样式
     */
    public void setStyle(float textSizeSp, int textColor, int bgColor) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
        setTextColor(textColor);
        setBackgroundColor(bgColor);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
