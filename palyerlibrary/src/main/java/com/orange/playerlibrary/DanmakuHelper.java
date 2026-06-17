package com.orange.playerlibrary;

import android.content.Context;
import android.widget.Toast;

/**
 * 弹幕功能辅助类
 * 提供弹幕库检测和功能可用性判断
 */
public class DanmakuHelper {
    
    private static final String TAG = "DanmakuHelper";
    
    // 弹幕库类名
    private static final String DANMAKU_VIEW_CLASS = "master.flame.danmaku.ui.widget.DanmakuView";
    private static final String DANMAKU_CONTEXT_CLASS = "master.flame.danmaku.danmaku.model.android.DanmakuContext";
    
    // 缓存检测结果
    private static Boolean sDanmakuAvailable = null;
    
    /**
     * 检测弹幕库是否可用
     * @return true 如果弹幕库已导入
     */
    public static boolean isDanmakuLibraryAvailable() {
        if (sDanmakuAvailable != null) {
            return sDanmakuAvailable;
        }
        
        try {
            Class.forName(DANMAKU_VIEW_CLASS);
            Class.forName(DANMAKU_CONTEXT_CLASS);
            sDanmakuAvailable = true;
        } catch (ClassNotFoundException e) {
            sDanmakuAvailable = false;
        }
        
        return sDanmakuAvailable;
    }
    
    /**
     * 显示弹幕功能未导入提示
     * @param context 上下文
     */
    public static void showDanmakuNotAvailableToast(Context context) {
        Toast.makeText(context, "弹幕功能未导入，请在 app 模块添加弹幕依赖", Toast.LENGTH_LONG).show();
    }
    
    /**
     * 检查弹幕功能并显示提示
     * @param context 上下文
     * @return true 如果弹幕可用
     */
    public static boolean checkDanmakuAvailable(Context context) {
        if (!isDanmakuLibraryAvailable()) {
            showDanmakuNotAvailableToast(context);
            return false;
        }
        return true;
    }
    
    /**
     * 获取弹幕库版本信息
     * @return 版本信息字符串，如果不可用返回 null
     */
    public static String getDanmakuLibraryInfo() {
        if (!isDanmakuLibraryAvailable()) {
            return null;
        }
        return "DanmakuFlameMaster";
    }
}
