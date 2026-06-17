package com.orange.playerlibrary.ocr;

/**
 * OCR 功能可用性检查器
 * 检测 Tesseract 和 ML Kit Translation 是否已安装
 */
public class OcrAvailabilityChecker {
    
    private static Boolean sTesseractAvailable = null;
    private static Boolean sMlKitTranslateAvailable = null;
    
    /**
     * 检查 Tesseract OCR 是否可用
     */
    public static boolean isTesseractAvailable() {
        if (sTesseractAvailable == null) {
            try {
                Class.forName("com.googlecode.tesseract.android.TessBaseAPI");
                sTesseractAvailable = true;
            } catch (ClassNotFoundException e) {
                sTesseractAvailable = false;
            }
        }
        return sTesseractAvailable;
    }
    
    /**
     * 检查 ML Kit Translation 是否可用
     */
    public static boolean isMlKitTranslateAvailable() {
        if (sMlKitTranslateAvailable == null) {
            try {
                Class.forName("com.google.mlkit.nl.translate.Translator");
                sMlKitTranslateAvailable = true;
            } catch (ClassNotFoundException e) {
                sMlKitTranslateAvailable = false;
            }
        }
        return sMlKitTranslateAvailable;
    }
    
    /**
     * 检查 OCR 翻译功能是否完全可用
     */
    public static boolean isOcrTranslateAvailable() {
        return isTesseractAvailable() && isMlKitTranslateAvailable();
    }
    
    /**
     * 获取缺失依赖的提示信息
     */
    public static String getMissingDependenciesMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("OCR 翻译功能需要安装额外依赖：\n\n");
        sb.append("请在 app/build.gradle 中添加：\n\n");
        
        if (!isTesseractAvailable()) {
            sb.append("// OCR 文字识别\n");
            sb.append("implementation 'cz.adaptech.tesseract4android:tesseract4android:4.7.0'\n\n");
        }
        
        if (!isMlKitTranslateAvailable()) {
            sb.append("// 文字翻译\n");
            sb.append("implementation 'com.google.mlkit:translate:17.0.2'\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 重置缓存（用于测试）
     */
    public static void resetCache() {
        sTesseractAvailable = null;
        sMlKitTranslateAvailable = null;
    }
}
