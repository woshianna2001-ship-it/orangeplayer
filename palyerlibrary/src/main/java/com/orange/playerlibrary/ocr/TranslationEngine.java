package com.orange.playerlibrary.ocr;

import android.content.Context;

/**
 * 翻译引擎接口
 */
public interface TranslationEngine {
    
    /**
     * 翻译回调
     */
    interface TranslationCallback {
        void onSuccess(String translatedText);
        void onError(String error);
    }
    
    /**
     * 模型下载回调
     */
    interface ModelDownloadCallback {
        void onProgress(int progress);
        void onSuccess();
        void onError(String error);
    }
    
    /**
     * 初始化翻译引擎
     * @param context 上下文
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     */
    void init(Context context, String sourceLanguage, String targetLanguage);
    
    /**
     * 检查模型是否已下载
     */
    boolean isModelDownloaded();
    
    /**
     * 下载翻译模型
     */
    void downloadModel(ModelDownloadCallback callback);
    
    /**
     * 翻译文字
     * @param text 原文
     * @param callback 回调
     */
    void translate(String text, TranslationCallback callback);
    
    /**
     * 释放资源
     */
    void release();
    
    /**
     * 是否已初始化
     */
    boolean isInitialized();
}
