package com.orange.playerlibrary.interfaces;

/**
 * 状态监听接口
 * 监听播放器状态和播放状态的变化
 */
public interface OnStateChangeListener {
    
    /**
     * 播放器状态改变回调
     * @param playerState 播放器状态
     *                    {@link com.orange.playerlibrary.PlayerConstants#PLAYER_NORMAL}
     *                    {@link com.orange.playerlibrary.PlayerConstants#PLAYER_FULL_SCREEN}
     *                    {@link com.orange.playerlibrary.PlayerConstants#PLAYER_TINY_SCREEN}
     */
    void onPlayerStateChanged(int playerState);
    
    /**
     * 播放状态改变回调
     * @param playState 播放状态
     *                  {@link com.orange.playerlibrary.PlayerConstants#STATE_IDLE}
     *                  {@link com.orange.playerlibrary.PlayerConstants#STATE_PREPARING}
     *                  {@link com.orange.playerlibrary.PlayerConstants#STATE_PREPARED}
     *                  {@link com.orange.playerlibrary.PlayerConstants#STATE_PLAYING}
     *                  {@link com.orange.playerlibrary.PlayerConstants#STATE_PAUSED}
     *                  {@link com.orange.playerlibrary.PlayerConstants#STATE_BUFFERING}
     *                  {@link com.orange.playerlibrary.PlayerConstants#STATE_BUFFERED}
     *                  {@link com.orange.playerlibrary.PlayerConstants#STATE_PLAYBACK_COMPLETED}
     *                  {@link com.orange.playerlibrary.PlayerConstants#STATE_ERROR}
     */
    void onPlayStateChanged(int playState);
}
