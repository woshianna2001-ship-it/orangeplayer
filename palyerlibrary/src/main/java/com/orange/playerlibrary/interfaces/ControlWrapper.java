package com.orange.playerlibrary.interfaces;

/**
 * 控制包装器
 * 封装播放器控制方法，供控制组件调用
 */
public interface ControlWrapper {
    
    /**
     * 开始播放
     */
    void start();
    
    /**
     * 暂停播放
     */
    void pause();
    
    /**
     * 跳转到指定位置
     * @param position 目标位置（毫秒）
     */
    void seekTo(long position);
    
    /**
     * 获取视频总时长
     * @return 总时长（毫秒）
     */
    long getDuration();
    
    /**
     * 获取当前播放位置
     * @return 当前位置（毫秒）
     */
    long getCurrentPosition();
    
    /**
     * 是否正在播放
     * @return true 正在播放
     */
    boolean isPlaying();
    
    /**
     * 切换播放/暂停状态
     */
    void togglePlay();
    
    /**
     * 切换全屏状态
     */
    void toggleFullScreen();
    
    /**
     * 切换锁屏状态
     */
    void toggleLockState();
    
    /**
     * 设置锁屏状态
     * @param locked 是否锁屏
     */
    void setLocked(boolean locked);
    
    /**
     * 锁定状态变化回调（用于立即更新 UI）
     * @param locked 是否锁定
     */
    void onLockStateChanged(boolean locked);
    
    /**
     * 是否全屏
     * @return true 全屏
     */
    boolean isFullScreen();
    
    /**
     * 是否锁屏
     * @return true 锁屏
     */
    boolean isLocked();
    
    /**
     * 设置播放速度
     * @param speed 播放速度
     */
    void setSpeed(float speed);
    
    /**
     * 获取当前播放速度
     * @return 播放速度
     */
    float getSpeed();
    
    /**
     * 获取缓冲百分比
     * @return 缓冲百分比 0-100
     */
    int getBufferedPercentage();
    
    /**
     * 设置静音
     * @param isMute 是否静音
     */
    void setMute(boolean isMute);
    
    /**
     * 是否静音
     * @return true 静音
     */
    boolean isMute();
    
    /**
     * 设置音量
     * @param volume 音量 0.0-1.0
     */
    void setVolume(float volume);
    
    /**
     * 重播
     * @param resetPosition 是否重置播放位置
     */
    void replay(boolean resetPosition);
    
    /**
     * 隐藏控制器
     */
    void hide();
    
    /**
     * 显示控制器
     */
    void show();
    
    /**
     * 控制器是否正在显示
     * @return true 正在显示
     */
    boolean isShowing();

    /**
     * 停止进度更新
     */
    void stopProgress();
    
    /**
     * 开始进度更新
     */
    void startProgress();
    
    /**
     * 停止自动隐藏
     */
    void stopFadeOut();
    
    /**
     * 开始自动隐藏倒计时
     */
    void startFadeOut();
    
    /**
     * 是否有刘海屏
     * @return true 有刘海屏
     */
    boolean hasCutout();
    
    /**
     * 获取刘海高度
     * @return 刘海高度
     */
    int getCutoutHeight();
    
    /**
     * 获取视频宽度
     * @return 视频宽度
     */
    int getVideoWidth();
    
    /**
     * 获取视频高度
     * @return 视频高度
     */
    int getVideoHeight();
    
    /**
     * 获取视频URL
     * @return 视频URL
     */
    String getVideoUrl();
    
    /**
     * 获取视频标题
     * @return 视频标题
     */
    String getVideoTitle();
}
