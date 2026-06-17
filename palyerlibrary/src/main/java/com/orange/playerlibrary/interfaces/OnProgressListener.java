package com.orange.playerlibrary.interfaces;

/**
 * 进度监听接口
 * 监听播放进度的变化
 */
public interface OnProgressListener {
    
    /**
     * 进度更新回调
     * @param duration 视频总时长（毫秒）
     * @param position 当前播放位置（毫秒）
     */
    void onProgressChanged(long duration, long position);
    
    /**
     * 缓冲进度更新回调
     * @param percent 缓冲百分比 0-100
     */
    default void onBufferingUpdate(int percent) {}
    
    /**
     * 开始拖动进度条
     */
    default void onStartTrackingTouch() {}
    
    /**
     * 停止拖动进度条
     */
    default void onStopTrackingTouch() {}
}
