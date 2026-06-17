package com.orange.playerlibrary;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

/**
 * 画中画 (PiP) 辅助类
 * 处理 PiP 模式下的播放位置保存和恢复
 */
public class PiPHelper {
    
    private static final String TAG = "PiPHelper";
    private static final String PREFS_NAME = "orange_pip_prefs";
    private static final String KEY_PIP_POSITION = "pip_position";
    private static final String KEY_PIP_URL = "pip_url";
    private static final String KEY_PIP_ACTIVE = "pip_active";
    
    private final Activity mActivity;
    private final OrangevideoView mVideoView;
    
    private long mPendingSeekPosition = -1;
    private boolean mRestoringFromPiP = false;
    private boolean mExitingPiP = false;
    private boolean mEnteringPiP = false;
    private boolean mPiPJustExited = false;  // 标记PiP刚退出，等待onResume处理
    
    public PiPHelper(Activity activity, OrangevideoView videoView) {
        mActivity = activity;
        mVideoView = videoView;
    }
    
    /**
     * 检查是否从 PiP 模式恢复
     * @param currentUrl 当前视频 URL
     * @return 需要恢复的播放位置，-1 表示不需要恢复
     */
    public long checkPiPRestore(String currentUrl) {
        SharedPreferences prefs = mActivity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean pipActive = prefs.getBoolean(KEY_PIP_ACTIVE, false);
        String pipUrl = prefs.getString(KEY_PIP_URL, "");
        long pipPosition = prefs.getLong(KEY_PIP_POSITION, -1);
        
        if (pipActive && pipPosition > 0 && currentUrl != null && currentUrl.equals(pipUrl)) {
            mPendingSeekPosition = pipPosition;
            mRestoringFromPiP = true;
            // 清除 PiP 状态
            clearPiPState();
            return pipPosition;
        }
        
        // 清除 PiP 状态
        clearPiPState();
        return -1;
    }
    
    /**
     * 保存 PiP 播放位置
     */
    public void savePiPPosition(String url, long position) {
        SharedPreferences prefs = mActivity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putBoolean(KEY_PIP_ACTIVE, true)
            .putString(KEY_PIP_URL, url)
            .putLong(KEY_PIP_POSITION, position)
            .apply();
    }
    
    /**
     * 清除 PiP 状态
     */
    public void clearPiPState() {
        SharedPreferences prefs = mActivity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putBoolean(KEY_PIP_ACTIVE, false)
            .putLong(KEY_PIP_POSITION, -1)
            .apply();
    }
    
    /**
     * 处理 onPause
     * @return true 表示应该跳过暂停，false 表示应该暂停
     */
    public boolean handleOnPause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            boolean isInPiP = mActivity.isInPictureInPictureMode();
            if (isInPiP || mEnteringPiP || mVideoView.isEnteringPiPMode()) {
                mEnteringPiP = false;
                return true; // 跳过暂停
            }
        }
        return false;
    }
    
    /**
     * 处理 onResume
     * @return true 表示应该跳过恢复，false 表示应该恢复
     */
    public boolean handleOnResume() {
        if (mExitingPiP) {
            mExitingPiP = false;
            return true; // 跳过恢复
        }
        
        // 检查是否刚从 PiP 退出（用户点击恢复按钮）
        if (mPiPJustExited) {
            android.util.Log.d(TAG, "handleOnResume: PiP was restored by user");
            mPiPJustExited = false;
            mRestoringFromPiP = true;
            // 显示控制器
            if (mVideoView.getVideoController() != null) {
                mVideoView.getVideoController().show();
            }
            return true; // 跳过默认恢复逻辑，让视频继续播放
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (mActivity.isInPictureInPictureMode()) {
                return true; // 跳过恢复
            }
        }
        return false;
    }
    
    /**
     * 处理 onStop
     * @return true 表示应该跳过处理
     */
    public boolean handleOnStop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (mActivity.isInPictureInPictureMode()) {
                return true;
            }
        }
        
        // 检查是否刚从 PiP 退出且进入了 onStop（用户点击 X 关闭）
        if (mPiPJustExited) {
            android.util.Log.d(TAG, "handleOnStop: PiP was closed by user (X button)");
            mPiPJustExited = false;
            mExitingPiP = true;
            
            // 暂停视频播放
            if (mVideoView.isPlaying()) {
                mVideoView.onVideoPause();
                android.util.Log.d(TAG, "PiP closed by user, pausing video");
            }
            
            // 如果处于全屏状态，退出全屏
            if (mVideoView.isFullScreen()) {
                mVideoView.stopFullScreen();
                android.util.Log.d(TAG, "PiP closed, exiting fullscreen");
            }
            
            return true; // 跳过默认的 onStop 处理
        }
        
        return false;
    }
    
    /**
     * 处理 PiP 模式变化
     * @param isInPictureInPictureMode 是否处于 PiP 模式
     * @param videoUrl 当前视频 URL
     */
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, String videoUrl) {
        long currentPosition = mVideoView.getCurrentPositionWhenPlaying();
        
        if (isInPictureInPictureMode) {
            // 进入 PiP 模式
            savePiPPosition(videoUrl, currentPosition);
            mVideoView.setEnteringPiPMode(false);
            mEnteringPiP = false;
            // 隐藏控制器
            if (mVideoView.getVideoController() != null) {
                mVideoView.getVideoController().hide();
            }
        } else {
            // 退出 PiP 模式
            savePiPPosition(videoUrl, currentPosition);
            
            // 延迟检查 Activity 状态，因为 onPictureInPictureModeChanged 可能在 onResume 之前或之后调用
            mVideoView.postDelayed(() -> {
                // 检查 Activity 是否有焦点
                // 有焦点 = 用户点击恢复按钮
                // 无焦点 = 用户点击 X 关闭
                boolean hasFocus = mActivity.hasWindowFocus();
                
                android.util.Log.d(TAG, "PiP exit check: hasFocus=" + hasFocus);
                
                if (hasFocus) {
                    // Activity 在前台 - 用户点击恢复按钮
                    android.util.Log.d(TAG, "PiP restored by user, continuing playback");
                    mRestoringFromPiP = true;
                    // 显示控制器
                    if (mVideoView.getVideoController() != null) {
                        mVideoView.getVideoController().show();
                    }
                } else {
                    // Activity 不在前台 - 用户点击 X 关闭
                    android.util.Log.d(TAG, "PiP closed by user (X button)");
                    mExitingPiP = true;
                    
                    // 暂停视频播放
                    if (mVideoView.isPlaying()) {
                        mVideoView.onVideoPause();
                        android.util.Log.d(TAG, "PiP closed, pausing video");
                    }
                    
                    // 如果处于全屏状态，退出全屏
                    if (mVideoView.isFullScreen()) {
                        mVideoView.stopFullScreen();
                        android.util.Log.d(TAG, "PiP closed, exiting fullscreen");
                    }
                }
            }, 100);
        }
    }
    
    /**
     * 设置正在进入 PiP 模式
     */
    public void setEnteringPiP(boolean entering) {
        mEnteringPiP = entering;
    }
    
    /**
     * 获取待恢复的播放位置
     */
    public long getPendingSeekPosition() {
        return mPendingSeekPosition;
    }
    
    /**
     * 清除待恢复的播放位置
     */
    public void clearPendingSeekPosition() {
        mPendingSeekPosition = -1;
        mRestoringFromPiP = false;
    }
    
    /**
     * 是否正在从 PiP 恢复
     */
    public boolean isRestoringFromPiP() {
        return mRestoringFromPiP;
    }
}
