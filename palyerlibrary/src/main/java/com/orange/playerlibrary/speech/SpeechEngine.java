package com.orange.playerlibrary.speech;

import android.content.Context;

/**
 * 语音识别引擎接口
 */
public interface SpeechEngine {
    
    /**
     * 语音识别回调
     */
    interface SpeechCallback {
        /** 部分识别结果（实时） */
        void onPartialResult(String text);
        /** 最终识别结果 */
        void onFinalResult(String text);
        /** 错误回调 */
        void onError(int errorCode, String errorMessage);
        /** 准备就绪 */
        void onReady();
        /** 开始说话 */
        void onSpeechStart();
        /** 结束说话 */
        void onSpeechEnd();
    }
    
    /**
     * 初始化语音识别引擎
     * @param context 上下文
     * @param language 语言代码 (zh-CN, en-US, ja-JP, ko-KR)
     * @return 是否初始化成功
     */
    boolean init(Context context, String language);
    
    /**
     * 开始语音识别
     */
    void startListening();
    
    /**
     * 停止语音识别
     */
    void stopListening();
    
    /**
     * 设置回调
     */
    void setCallback(SpeechCallback callback);
    
    /**
     * 释放资源
     */
    void release();
    
    /**
     * 是否已初始化
     */
    boolean isInitialized();
    
    /**
     * 是否正在识别
     */
    boolean isListening();
}
