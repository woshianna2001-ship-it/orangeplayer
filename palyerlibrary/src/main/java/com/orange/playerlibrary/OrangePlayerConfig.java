package com.orange.playerlibrary;

import android.content.Context;

import com.orange.playerlibrary.utils.TvUtils;

/**
 * OrangePlayer 全局配置
 * 
 * 用于配置播放器的全局行为，包括 TV 模式、调试模式等
 */
public class OrangePlayerConfig {
    
    private static Boolean sTvMode = null;
    private static boolean sDebugMode = false;
    private static boolean sAutoDetectTvMode = true;
    
    /**
     * 设置 TV 模式
     * 
     * @param tvMode true 启用 TV 模式，false 禁用，null 自动检测
     */
    public static void setTvMode(Boolean tvMode) {
        sTvMode = tvMode;
    }
    
    /**
     * 获取 TV 模式
     * 
     * @param context Context
     * @return true 如果启用 TV 模式
     */
    public static boolean isTvMode(Context context) {
        if (sTvMode != null) {
            return sTvMode;
        }
        
        if (sAutoDetectTvMode && context != null) {
            return TvUtils.isTvDevice(context);
        }
        
        return false;
    }
    
    /**
     * 设置是否自动检测 TV 模式
     * 
     * @param autoDetect true 自动检测
     */
    public static void setAutoDetectTvMode(boolean autoDetect) {
        sAutoDetectTvMode = autoDetect;
    }
    
    /**
     * 是否自动检测 TV 模式
     * 
     * @return true 如果自动检测
     */
    public static boolean isAutoDetectTvMode() {
        return sAutoDetectTvMode;
    }
    
    /**
     * 设置调试模式
     * 
     * @param debug true 启用调试模式
     */
    public static void setDebugMode(boolean debug) {
        sDebugMode = debug;
    }
    
    /**
     * 是否调试模式
     * 
     * @return true 如果启用调试模式
     */
    public static boolean isDebugMode() {
        return sDebugMode;
    }
    
    /**
     * 重置所有配置（用于测试）
     */
    public static void reset() {
        sTvMode = null;
        sDebugMode = false;
        sAutoDetectTvMode = true;
    }
}
