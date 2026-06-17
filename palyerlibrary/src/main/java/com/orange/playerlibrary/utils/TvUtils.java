package com.orange.playerlibrary.utils;

import android.app.UiModeManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;

/**
 * TV 设备检测工具类
 * 
 * 用于判断当前设备是否为 Android TV，以便调整 UI 和交互方式
 */
public class TvUtils {
    
    private static Boolean sIsTvDevice = null;
    
    /**
     * 检测当前设备是否为 TV 设备
     * 
     * @param context Context
     * @return true 如果是 TV 设备
     */
    public static boolean isTvDevice(Context context) {
        if (sIsTvDevice != null) {
            return sIsTvDevice;
        }
        
        if (context == null) {
            return false;
        }
        
        // 方法 1: 检查 Leanback 特性
        boolean hasLeanback = context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_LEANBACK);
        
        // 方法 2: 检查 TV 特性
        boolean hasTelevision = context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_TELEVISION);
        
        // 方法 3: 检查 UI 模式
        boolean isTvUiMode = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            UiModeManager uiModeManager = (UiModeManager) context
                    .getSystemService(Context.UI_MODE_SERVICE);
            if (uiModeManager != null) {
                isTvUiMode = uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
            }
        }
        
        // 方法 4: 检查是否没有触摸屏
        boolean noTouchScreen = !context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
        
        // 综合判断
        sIsTvDevice = hasLeanback || hasTelevision || isTvUiMode;
        
        return sIsTvDevice;
    }
    
    /**
     * 检测设备是否有触摸屏
     * 
     * @param context Context
     * @return true 如果有触摸屏
     */
    public static boolean hasTouchScreen(Context context) {
        if (context == null) {
            return true;
        }
        return context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
    }
    
    /**
     * 检测设备是否支持 Leanback
     * 
     * @param context Context
     * @return true 如果支持 Leanback
     */
    public static boolean hasLeanback(Context context) {
        if (context == null) {
            return false;
        }
        return context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_LEANBACK);
    }
    
    /**
     * 重置缓存（用于测试）
     */
    public static void resetCache() {
        sIsTvDevice = null;
    }
    
    /**
     * 获取推荐的按钮大小（dp）
     * TV 设备需要更大的按钮
     * 
     * @param context Context
     * @return 按钮大小（dp）
     */
    public static int getRecommendedButtonSize(Context context) {
        return isTvDevice(context) ? 80 : 48;
    }
    
    /**
     * 获取推荐的文字大小（sp）
     * TV 设备需要更大的文字
     * 
     * @param context Context
     * @return 文字大小（sp）
     */
    public static int getRecommendedTextSize(Context context) {
        return isTvDevice(context) ? 24 : 14;
    }
    
    /**
     * 是否应该启用手势控制
     * TV 设备通常不需要手势控制
     * 
     * @param context Context
     * @return true 如果应该启用手势控制
     */
    public static boolean shouldEnableGesture(Context context) {
        return !isTvDevice(context) && hasTouchScreen(context);
    }
}
