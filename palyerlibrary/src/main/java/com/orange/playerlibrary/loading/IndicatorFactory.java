package com.orange.playerlibrary.loading;

import com.orange.playerlibrary.OrangeVideoController;
import com.orange.playerlibrary.loading.indicators.BallPulseIndicator;
import com.orange.playerlibrary.loading.indicators.BallSpinFadeLoaderIndicator;
import com.orange.playerlibrary.loading.indicators.LineScalePulseOutIndicator;
import com.orange.playerlibrary.loading.indicators.LineSpinFadeLoaderIndicator;

/**
 * 加载动画指示器工厂
 * 
 * Requirements: 6.5 - THE OrangeVideoController SHALL 支持多种加载动画样式 (setLoading)
 */
public class IndicatorFactory {

    /**
     * 根据类型创建指示器
     * @param type 指示器类型
     * @return 指示器实例
     */
    public static Indicator createIndicator(OrangeVideoController.IndicatorType type) {
        if (type == null) {
            return new LineScalePulseOutIndicator();
        }
        
        switch (type) {
            case BALL_PULSE:
                return new BallPulseIndicator();
            case BALL_SPIN_FADE_LOADER:
                return new BallSpinFadeLoaderIndicator();
            case LINE_SPIN_FADE_LOADER:
                return new LineSpinFadeLoaderIndicator();
            case LINE_SCALE_PULSE_OUT:
                return new LineScalePulseOutIndicator();
            // 其他类型可以通过反射创建
            default:
                return createIndicatorByName(type.getIndicatorName());
        }
    }

    /**
     * 根据名称创建指示器
     * @param indicatorName 指示器名称
     * @return 指示器实例
     */
    public static Indicator createIndicatorByName(String indicatorName) {
        if (indicatorName == null || indicatorName.isEmpty()) {
            return new BallPulseIndicator();
        }
        
        try {
            String className;
            if (indicatorName.contains(".")) {
                className = indicatorName;
            } else {
                className = "com.orange.playerlibrary.loading.indicators." + indicatorName;
            }
            
            Class<?> clazz = Class.forName(className);
            return (Indicator) clazz.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return new BallPulseIndicator();
        }
    }

    /**
     * 根据 ID 创建指示器
     * @param typeId 指示器类型 ID
     * @return 指示器实例
     */
    public static Indicator createIndicatorById(int typeId) {
        OrangeVideoController.IndicatorType type = OrangeVideoController.IndicatorType.findById(typeId);
        return createIndicator(type);
    }
}
