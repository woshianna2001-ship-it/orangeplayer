package com.orange.playerlibrary;

import android.util.Log;

/**
 * 播放状态管理器
 * 负责保存和恢复播放状态，处理表面丢失和恢复
 * Requirements: 5.1, 5.2, 5.3, 5.4
 */
public class PlaybackStateManager {
    
    private static final String TAG = "PlaybackStateManager";
    
    // 保存的播放位置（毫秒）
    private long mSavedPosition = 0;
    
    // 是否正在播放
    private boolean mWasPlaying = false;
    
    // 表面是否丢失
    private boolean mSurfaceLost = false;
    
    // 播放速度
    private float mSpeed = 1.0f;
    
    // 播放器状态
    private int mPlayerState = PlayerConstants.PLAYER_NORMAL;
    
    // 播放状态
    private int mPlayState = PlayerConstants.STATE_IDLE;
    
    /**
     * 保存当前播放状态
     * Requirements: 5.1, 5.2
     * 
     * @param videoView 视频播放器
     */
    public void saveState(OrangevideoView videoView) {
        if (videoView == null) {
            Log.w(TAG, "saveState: videoView is null");
            return;
        }
        
        // 保存播放位置
        mSavedPosition = videoView.getCurrentPositionWhenPlaying();
        
        // 保存播放状态
        mWasPlaying = videoView.isPlaying();
        
        // 保存播放速度
        mSpeed = videoView.getSpeed();
        
        // 保存播放器状态
        mPlayerState = videoView.getPlayerState();
        
        // 保存播放状态
        mPlayState = videoView.getPlayState();
        
        Log.d(TAG, "saveState: position=" + mSavedPosition 
                + ", wasPlaying=" + mWasPlaying 
                + ", speed=" + mSpeed
                + ", playerState=" + mPlayerState
                + ", playState=" + mPlayState);
    }
    
    /**
     * 恢复播放状态
     * Requirements: 5.3, 5.4
     * 
     * @param videoView 视频播放器
     */
    public void restoreState(OrangevideoView videoView) {
        if (videoView == null) {
            Log.w(TAG, "restoreState: videoView is null");
            return;
        }
        
        Log.d(TAG, "restoreState: position=" + mSavedPosition 
                + ", wasPlaying=" + mWasPlaying 
                + ", speed=" + mSpeed
                + ", userPaused=" + videoView.isUserPaused());
        
        // 恢复播放位置
        if (mSavedPosition > 0) {
            videoView.seekTo(mSavedPosition);
        }
        
        // 恢复播放速度
        if (mSpeed != 1.0f) {
            videoView.setSpeed(mSpeed);
        }
        
        // 恢复播放状态 - 但要检查用户是否主动暂停了
        if (mWasPlaying && !videoView.isUserPaused()) {
            // 如果之前在播放且用户没有主动暂停，继续播放
            videoView.startPlayLogic();
        } else {
            // 如果之前暂停或用户主动暂停了，保持暂停状态
            Log.d(TAG, "restoreState: keeping paused state");
        }
    }
    
    /**
     * 标记表面丢失
     * Requirements: 7.2, 7.3
     */
    public void markSurfaceLost() {
        mSurfaceLost = true;
        Log.d(TAG, "markSurfaceLost: surface lost");
    }
    
    /**
     * 恢复表面
     * Requirements: 7.3, 7.4
     * 
     * @param videoView 视频播放器
     */
    public void restoreSurface(OrangevideoView videoView) {
        if (videoView == null) {
            Log.w(TAG, "restoreSurface: videoView is null");
            return;
        }
        
        if (mSurfaceLost && mSavedPosition > 0) {
            Log.d(TAG, "restoreSurface: restoring surface at position=" + mSavedPosition
                + ", userPaused=" + videoView.isUserPaused());
            
            // 重新渲染最后一帧
            videoView.seekTo(mSavedPosition);
            
            // 如果之前在播放且用户没有主动暂停，继续播放
            if (mWasPlaying && !videoView.isUserPaused()) {
                videoView.startPlayLogic();
            }
            
            mSurfaceLost = false;
        }
    }
    
    /**
     * 获取保存的播放位置
     * 
     * @return 播放位置（毫秒）
     */
    public long getSavedPosition() {
        return mSavedPosition;
    }
    
    /**
     * 是否正在播放
     * 
     * @return true 正在播放
     */
    public boolean wasPlaying() {
        return mWasPlaying;
    }
    
    /**
     * 表面是否丢失
     * 
     * @return true 表面丢失
     */
    public boolean isSurfaceLost() {
        return mSurfaceLost;
    }
    
    /**
     * 获取保存的播放速度
     * 
     * @return 播放速度
     */
    public float getSpeed() {
        return mSpeed;
    }
    
    /**
     * 获取保存的播放器状态
     * 
     * @return 播放器状态
     */
    public int getPlayerState() {
        return mPlayerState;
    }
    
    /**
     * 获取保存的播放状态
     * 
     * @return 播放状态
     */
    public int getPlayState() {
        return mPlayState;
    }
    
    /**
     * 重置状态
     */
    public void reset() {
        mSavedPosition = 0;
        mWasPlaying = false;
        mSurfaceLost = false;
        mSpeed = 1.0f;
        mPlayerState = PlayerConstants.PLAYER_NORMAL;
        mPlayState = PlayerConstants.STATE_IDLE;
        Log.d(TAG, "reset: state reset");
    }
}
