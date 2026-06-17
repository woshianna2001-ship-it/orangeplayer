package com.orange.playerlibrary.speech;

/**
 * Vosk 语音识别功能可用性检查器
 * 检测 Vosk SDK 是否已安装
 */
public class VoskAvailabilityChecker {
    
    private static Boolean sVoskAvailable = null;
    
    /**
     * 检查 Vosk SDK 是否可用
     */
    public static boolean isVoskAvailable() {
        if (sVoskAvailable == null) {
            try {
                Class.forName("org.vosk.Model");
                sVoskAvailable = true;
            } catch (ClassNotFoundException e) {
                sVoskAvailable = false;
            }
        }
        return sVoskAvailable;
    }
    
    /**
     * 获取缺失依赖的提示信息
     */
    public static String getMissingDependenciesMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("语音识别功能需要安装 Vosk SDK：\n\n");
        sb.append("请在 app/build.gradle 中添加：\n\n");
        sb.append("// Vosk 离线语音识别\n");
        sb.append("implementation 'com.alphacephei:vosk-android:0.3.47'\n\n");
        sb.append("注意：需要 Android 10 (API 29) 或更高版本");
        
        return sb.toString();
    }
    
    /**
     * 重置缓存（用于测试）
     */
    public static void resetCache() {
        sVoskAvailable = null;
    }
}
