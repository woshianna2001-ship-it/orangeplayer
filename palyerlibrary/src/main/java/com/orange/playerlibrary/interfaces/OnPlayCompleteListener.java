package com.orange.playerlibrary.interfaces;

/**
 * 播放完成监听接口
 * 监听视频播放完成事件
 */
public interface OnPlayCompleteListener {
    
    /**
     * 播放完成回调
     */
    void onPlayComplete();
    
    /**
     * 自动播放下一集回调
     * @param hasNext 是否有下一集
     */
    default void onAutoPlayNext(boolean hasNext) {}
}
