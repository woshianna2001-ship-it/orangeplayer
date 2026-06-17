package com.orange.playerlibrary;

import android.os.Handler;
import android.os.Looper;

/**
 * 跳过片头片尾管理器
 * 
 * Requirements: 6.4 - THE OrangevideoView SHALL 支持跳过片头片尾功能
 */
public class SkipManager {

    private static final String TAG = "SkipManager";

    /** 跳过片头时长（毫秒）*/
    private long mSkipIntroTime = 0;
    
    /** 跳过片尾时长（毫秒）*/
    private long mSkipOutroTime = 0;
    
    /** 是否启用跳过片头 */
    private boolean mSkipIntroEnabled = false;
    
    /** 是否启用跳过片尾 */
    private boolean mSkipOutroEnabled = false;
    
    /** 是否已跳过片头 */
    private boolean mIntroSkipped = false;
    
    /** 是否已跳过片尾 */
    private boolean mOutroSkipped = false;
    
    /** 关联的播放器 */
    private OrangevideoView mVideoView;
    
    /** 主线程 Handler */
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    
    /** 检查片尾的 Runnable */
    private Runnable mOutroCheckRunnable;
    
    /** 检查间隔（毫秒）*/
    private static final long CHECK_INTERVAL = 500;
    
    /** 跳过回调 */
    private OnSkipListener mSkipListener;

    /**
     * 跳过监听器
     */
    public interface OnSkipListener {
        /**
         * 跳过片头
         * @param skipTime 跳过的时长（毫秒）
         */
        void onSkipIntro(long skipTime);
        
        /**
         * 跳过片尾
         * @param skipTime 跳过的时长（毫秒）
         */
        void onSkipOutro(long skipTime);
    }

    public SkipManager() {
    }

    /**
     * 绑定播放器
     * @param videoView 播放器视图
     */
    public void attachVideoView(OrangevideoView videoView) {
        mVideoView = videoView;
    }

    /**
     * 解绑播放器
     */
    public void detachVideoView() {
        stopOutroCheck();
        mVideoView = null;
    }

    // ==================== 跳过片头 ====================

    /**
     * 设置跳过片头时长
     * @param timeMs 时长（毫秒）
     */
    public void setSkipIntroTime(long timeMs) {
        android.util.Log.d(TAG, "setSkipIntroTime: " + timeMs + "ms");
        mSkipIntroTime = timeMs;
        mSkipIntroEnabled = timeMs > 0;
    }

    /**
     * 设置跳过片头时长（秒）
     * @param seconds 时长（秒）
     */
    public void setSkipIntroSeconds(int seconds) {
        setSkipIntroTime(seconds * 1000L);
    }

    /**
     * 获取跳过片头时长
     * @return 时长（毫秒）
     */
    public long getSkipIntroTime() {
        return mSkipIntroTime;
    }

    /**
     * 是否启用跳过片头
     * @return true 启用
     */
    public boolean isSkipIntroEnabled() {
        return mSkipIntroEnabled;
    }

    /**
     * 设置是否启用跳过片头
     * @param enabled 是否启用
     */
    public void setSkipIntroEnabled(boolean enabled) {
        mSkipIntroEnabled = enabled;
    }

    /**
     * 执行跳过片头
     * 在视频准备完成后调用
     */
    public void performSkipIntro() {
        android.util.Log.d(TAG, "performSkipIntro() called - enabled=" + mSkipIntroEnabled 
                + ", skipped=" + mIntroSkipped 
                + ", skipTime=" + mSkipIntroTime 
                + ", videoView=" + (mVideoView != null)
                + ", videoView.isPlaying=" + (mVideoView != null && mVideoView.isPlaying()));
        
        if (!mSkipIntroEnabled || mIntroSkipped || mVideoView == null || mSkipIntroTime <= 0) {
            android.util.Log.d(TAG, "performSkipIntro() skipped - conditions not met");
            return;
        }
        
        mIntroSkipped = true;
        android.util.Log.d(TAG, "performSkipIntro() seeking to " + mSkipIntroTime + "ms");
        mVideoView.seekTo(mSkipIntroTime);
        
        if (mSkipListener != null) {
            mSkipListener.onSkipIntro(mSkipIntroTime);
        }
    }

    // ==================== 跳过片尾 ====================

    /**
     * 设置跳过片尾时长
     * @param timeMs 时长（毫秒）
     */
    public void setSkipOutroTime(long timeMs) {
        mSkipOutroTime = timeMs;
        mSkipOutroEnabled = timeMs > 0;
    }

    /**
     * 设置跳过片尾时长（秒）
     * @param seconds 时长（秒）
     */
    public void setSkipOutroSeconds(int seconds) {
        setSkipOutroTime(seconds * 1000L);
    }

    /**
     * 获取跳过片尾时长
     * @return 时长（毫秒）
     */
    public long getSkipOutroTime() {
        return mSkipOutroTime;
    }

    /**
     * 是否启用跳过片尾
     * @return true 启用
     */
    public boolean isSkipOutroEnabled() {
        return mSkipOutroEnabled;
    }

    /**
     * 设置是否启用跳过片尾
     * @param enabled 是否启用
     */
    public void setSkipOutroEnabled(boolean enabled) {
        mSkipOutroEnabled = enabled;
    }

    /**
     * 开始检查片尾
     * 在视频开始播放后调用
     */
    public void startOutroCheck() {
        if (!mSkipOutroEnabled || mSkipOutroTime <= 0) {
            return;
        }
        
        stopOutroCheck();
        
        mOutroCheckRunnable = new Runnable() {
            @Override
            public void run() {
                checkAndSkipOutro();
                if (mVideoView != null && mVideoView.isPlaying()) {
                    mHandler.postDelayed(this, CHECK_INTERVAL);
                }
            }
        };
        
        mHandler.postDelayed(mOutroCheckRunnable, CHECK_INTERVAL);
    }

    /**
     * 停止检查片尾
     */
    public void stopOutroCheck() {
        if (mOutroCheckRunnable != null) {
            mHandler.removeCallbacks(mOutroCheckRunnable);
            mOutroCheckRunnable = null;
        }
    }

    /**
     * 检查并跳过片尾
     */
    private void checkAndSkipOutro() {
        if (!mSkipOutroEnabled || mOutroSkipped || mVideoView == null || mSkipOutroTime <= 0) {
            return;
        }
        
        long duration = mVideoView.getDuration();
        long position = mVideoView.getCurrentPosition();
        
        if (duration <= 0) {
            return;
        }
        
        // 计算片尾开始位置
        long outroStartPosition = duration - mSkipOutroTime;
        
        // 如果当前位置已经到达片尾区域
        if (position >= outroStartPosition) {
            mOutroSkipped = true;
            stopOutroCheck();
            
            // 触发播放完成
            if (mSkipListener != null) {
                mSkipListener.onSkipOutro(mSkipOutroTime);
            }
        }
    }

    /**
     * 是否已跳过片头
     * @return true 已跳过
     */
    public boolean isIntroSkipped() {
        return mIntroSkipped;
    }
    
    /**
     * 设置是否已跳过片头
     * @param skipped 是否已跳过
     */
    public void setIntroSkipped(boolean skipped) {
        mIntroSkipped = skipped;
    }

    /**
     * 是否已跳过片尾
     * @return true 已跳过
     */
    public boolean isOutroSkipped() {
        return mOutroSkipped;
    }

    // ==================== 重置 ====================

    /**
     * 重置状态
     * 在切换视频时调用
     */
    public void reset() {
        android.util.Log.d(TAG, "reset() called - clearing intro/outro skipped flags, mVideoView=" + mVideoView);
        mIntroSkipped = false;
        mOutroSkipped = false;
        stopOutroCheck();
    }
    
    /**
     * 重置状态并重新绑定播放器
     * @param videoView 播放器视图
     */
    public void resetAndAttach(OrangevideoView videoView) {
        android.util.Log.d(TAG, "resetAndAttach() called, videoView=" + videoView);
        mVideoView = videoView;
        mIntroSkipped = false;
        mOutroSkipped = false;
        stopOutroCheck();
    }

    /**
     * 清除所有设置
     */
    public void clear() {
        reset();
        mSkipIntroTime = 0;
        mSkipOutroTime = 0;
        mSkipIntroEnabled = false;
        mSkipOutroEnabled = false;
    }

    // ==================== 监听器 ====================

    /**
     * 设置跳过监听器
     * @param listener 监听器
     */
    public void setOnSkipListener(OnSkipListener listener) {
        mSkipListener = listener;
    }

    /**
     * 获取跳过监听器
     * @return 监听器
     */
    public OnSkipListener getOnSkipListener() {
        return mSkipListener;
    }
}
