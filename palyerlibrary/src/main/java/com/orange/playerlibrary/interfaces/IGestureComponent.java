package com.orange.playerlibrary.interfaces;

/**
 * 手势组件接口
 * 扩展 IControlComponent，添加手势相关回调
 */
public interface IGestureComponent extends IControlComponent {
    
    /**
     * 开始滑动
     */
    void onStartSlide();
    
    /**
     * 停止滑动
     */
    void onStopSlide();
    
    /**
     * 进度改变
     * @param newPosition 新位置
     * @param currentPosition 当前位置
     * @param duration 总时长
     */
    void onPositionChange(int newPosition, int currentPosition, int duration);
    
    /**
     * 亮度改变
     * @param percent 亮度百分比 0-100
     */
    void onBrightnessChange(int percent);
    
    /**
     * 音量改变
     * @param percent 音量百分比 0-100
     */
    void onVolumeChange(int percent);
}
