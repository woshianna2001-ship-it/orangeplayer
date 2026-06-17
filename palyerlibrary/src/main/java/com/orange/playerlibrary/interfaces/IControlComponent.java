package com.orange.playerlibrary.interfaces;

import android.view.View;
import android.view.animation.Animation;

/**
 * 控制组件接口
 * 定义控制组件的标准接口，与原 DKPlayer 保持一致
 */
public interface IControlComponent {
    
    /**
     * 绑定控制包装器
     * @param controlWrapper 控制包装器
     */
    void attach(ControlWrapper controlWrapper);
    
    /**
     * 获取组件视图
     * @return 组件视图
     */
    View getView();
    
    /**
     * 可见性改变回调
     * @param isVisible 是否可见
     * @param anim 动画
     */
    void onVisibilityChanged(boolean isVisible, Animation anim);
    
    /**
     * 播放状态改变回调
     * @param playState 播放状态
     */
    void onPlayStateChanged(int playState);
    
    /**
     * 播放器状态改变回调
     * @param playerState 播放器状态
     */
    void onPlayerStateChanged(int playerState);
    
    /**
     * 设置播放进度
     * @param duration 总时长
     * @param position 当前位置
     */
    void setProgress(int duration, int position);
    
    /**
     * 锁屏状态改变回调
     * @param isLocked 是否锁屏
     */
    void onLockStateChanged(boolean isLocked);
}
