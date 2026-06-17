# 语音识别翻译功能方案

## 概述

基于 Android `SpeechRecognizer` + Google ML Kit Translation 实现实时语音识别翻译字幕功能。

**实现状态**: ✅ 已完成

## 架构设计

参考现有 OCR 翻译字幕架构，采用相似的模块化设计：

```
┌─────────────────────────────────────────────────────────────┐
│                    SpeechSubtitleManager                     │
│  (语音字幕管理器 - 协调语音识别和翻译)                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────────┐    ┌─────────────────────┐         │
│  │    SpeechEngine     │    │  TranslationEngine  │         │
│  │   (语音识别接口)     │    │    (翻译引擎接口)    │         │
│  └──────────┬──────────┘    └──────────┬──────────┘         │
│             │                          │                     │
│  ┌──────────▼──────────┐    ┌──────────▼──────────┐         │
│  │ AndroidSpeechEngine │    │MlKitTranslationEngine│         │
│  │(SpeechRecognizer实现)│    │   (ML Kit 翻译实现)  │         │
│  └─────────────────────┘    └─────────────────────┘         │
│                                                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │  SubtitleView   │
                    │   (字幕显示)     │
                    └─────────────────┘
```

## 接口定义

### 1. SpeechEngine 接口

```java
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
```

### 2. AndroidSpeechEngine 实现

```java
package com.orange.playerlibrary.speech;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

/**
 * Android SpeechRecognizer 实现
 */
public class AndroidSpeechEngine implements SpeechEngine {
    
    private static final String TAG = "AndroidSpeechEngine";
    
    private Context mContext;
    private SpeechRecognizer mSpeechRecognizer;
    private SpeechCallback mCallback;
    private String mLanguage = "zh-CN";
    private boolean mIsInitialized = false;
    private boolean mIsListening = false;
    private boolean mContinuousMode = true; // 持续识别模式
    
    @Override
    public boolean init(Context context, String language) {
        mContext = context.getApplicationContext();
        mLanguage = language;
        
        if (!SpeechRecognizer.isRecognitionAvailable(mContext)) {
            Log.e(TAG, "Speech recognition not available");
            return false;
        }
        
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext);
        mSpeechRecognizer.setRecognitionListener(mRecognitionListener);
        mIsInitialized = true;
        
        Log.d(TAG, "SpeechRecognizer initialized, language=" + language);
        return true;
    }
    
    @Override
    public void startListening() {
        if (!mIsInitialized || mSpeechRecognizer == null) {
            Log.e(TAG, "Not initialized");
            return;
        }
        
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, mLanguage);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        
        mSpeechRecognizer.startListening(intent);
        mIsListening = true;
        
        Log.d(TAG, "Started listening");
    }
    
    @Override
    public void stopListening() {
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.stopListening();
        }
        mIsListening = false;
        mContinuousMode = false;
        Log.d(TAG, "Stopped listening");
    }
    
    @Override
    public void setCallback(SpeechCallback callback) {
        mCallback = callback;
    }
    
    @Override
    public void release() {
        stopListening();
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.destroy();
            mSpeechRecognizer = null;
        }
        mIsInitialized = false;
        Log.d(TAG, "Released");
    }
    
    @Override
    public boolean isInitialized() {
        return mIsInitialized;
    }
    
    @Override
    public boolean isListening() {
        return mIsListening;
    }
    
    private final RecognitionListener mRecognitionListener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "onReadyForSpeech");
            if (mCallback != null) mCallback.onReady();
        }
        
        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech");
            if (mCallback != null) mCallback.onSpeechStart();
        }
        
        @Override
        public void onRmsChanged(float rmsdB) {}
        
        @Override
        public void onBufferReceived(byte[] buffer) {}
        
        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech");
            if (mCallback != null) mCallback.onSpeechEnd();
        }
        
        @Override
        public void onError(int error) {
            String errorMessage = getErrorMessage(error);
            Log.e(TAG, "onError: " + error + " - " + errorMessage);
            
            if (mCallback != null) {
                mCallback.onError(error, errorMessage);
            }
            
            // 持续模式下自动重启
            if (mContinuousMode && mIsListening) {
                restartListening();
            }
        }
        
        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String text = matches.get(0);
                Log.d(TAG, "onResults: " + text);
                if (mCallback != null) mCallback.onFinalResult(text);
            }
            
            // 持续模式下自动重启
            if (mContinuousMode && mIsListening) {
                restartListening();
            }
        }
        
        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> matches = partialResults.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String text = matches.get(0);
                Log.d(TAG, "onPartialResults: " + text);
                if (mCallback != null) mCallback.onPartialResult(text);
            }
        }
        
        @Override
        public void onEvent(int eventType, Bundle params) {}
    };
    
    private void restartListening() {
        // 延迟重启，避免过于频繁
        new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(this::startListening, 100);
    }
    
    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO: return "音频录制错误";
            case SpeechRecognizer.ERROR_CLIENT: return "客户端错误";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "权限不足";
            case SpeechRecognizer.ERROR_NETWORK: return "网络错误";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "网络超时";
            case SpeechRecognizer.ERROR_NO_MATCH: return "无匹配结果";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "识别器忙";
            case SpeechRecognizer.ERROR_SERVER: return "服务器错误";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "语音超时";
            default: return "未知错误: " + errorCode;
        }
    }
}
```

### 3. SpeechSubtitleManager 管理器

```java
package com.orange.playerlibrary.speech;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.orange.playerlibrary.ocr.TranslationEngine;
import com.orange.playerlibrary.ocr.MlKitTranslationEngine;

/**
 * 语音字幕管理器
 * 协调语音识别和翻译功能
 */
public class SpeechSubtitleManager {
    
    private static final String TAG = "SpeechSubtitleManager";
    
    private Context mContext;
    private SpeechEngine mSpeechEngine;
    private TranslationEngine mTranslationEngine;
    private Handler mMainHandler;
    
    private boolean mIsRunning = false;
    private String mSourceLanguage = "zh-CN";
    private String mTargetLanguage = "en";
    private boolean mTranslationEnabled = true;
    
    private SpeechSubtitleCallback mCallback;
    
    /**
     * 语音字幕回调
     */
    public interface SpeechSubtitleCallback {
        /** 识别到字幕（实时） */
        void onPartialSubtitle(String text, String translatedText);
        /** 最终字幕 */
        void onFinalSubtitle(String text, String translatedText);
        /** 错误 */
        void onError(String error);
    }
    
    public SpeechSubtitleManager(Context context) {
        mContext = context.getApplicationContext();
        mMainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 检查语音识别是否可用
     */
    public static boolean isAvailable(Context context) {
        return android.speech.SpeechRecognizer.isRecognitionAvailable(context);
    }
    
    /**
     * 初始化语音识别
     */
    public boolean initSpeech(String sourceLanguage) {
        mSourceLanguage = sourceLanguage;
        mSpeechEngine = new AndroidSpeechEngine();
        
        boolean success = mSpeechEngine.init(mContext, sourceLanguage);
        if (success) {
            mSpeechEngine.setCallback(mSpeechCallback);
        }
        return success;
    }
    
    /**
     * 初始化翻译（复用 OCR 的翻译引擎）
     */
    public void initTranslation(String targetLanguage, 
            TranslationEngine.ModelDownloadCallback callback) {
        mTargetLanguage = targetLanguage;
        mTranslationEngine = new MlKitTranslationEngine();
        mTranslationEngine.init(mContext, mSourceLanguage, targetLanguage);
        mTranslationEngine.downloadModel(callback);
    }
    
    /**
     * 设置回调
     */
    public void setCallback(SpeechSubtitleCallback callback) {
        mCallback = callback;
    }
    
    /**
     * 开始语音识别
     */
    public void start() {
        if (mIsRunning) return;
        
        if (mSpeechEngine == null || !mSpeechEngine.isInitialized()) {
            Log.e(TAG, "Speech engine not initialized");
            return;
        }
        
        mIsRunning = true;
        mSpeechEngine.startListening();
        Log.d(TAG, "Speech subtitle started");
    }
    
    /**
     * 停止语音识别
     */
    public void stop() {
        mIsRunning = false;
        if (mSpeechEngine != null) {
            mSpeechEngine.stopListening();
        }
        Log.d(TAG, "Speech subtitle stopped");
    }
    
    /**
     * 释放资源
     */
    public void release() {
        stop();
        if (mSpeechEngine != null) {
            mSpeechEngine.release();
            mSpeechEngine = null;
        }
        if (mTranslationEngine != null) {
            mTranslationEngine.release();
            mTranslationEngine = null;
        }
    }
    
    /**
     * 是否正在运行
     */
    public boolean isRunning() {
        return mIsRunning;
    }
    
    /**
     * 设置是否启用翻译
     */
    public void setTranslationEnabled(boolean enabled) {
        mTranslationEnabled = enabled;
    }
    
    private final SpeechEngine.SpeechCallback mSpeechCallback = 
            new SpeechEngine.SpeechCallback() {
        @Override
        public void onPartialResult(String text) {
            if (mTranslationEnabled && mTranslationEngine != null 
                    && mTranslationEngine.isInitialized()) {
                // 实时翻译
                mTranslationEngine.translate(text, new TranslationEngine.TranslationCallback() {
                    @Override
                    public void onSuccess(String translatedText) {
                        notifyPartialSubtitle(text, translatedText);
                    }
                    @Override
                    public void onError(String error) {
                        notifyPartialSubtitle(text, null);
                    }
                });
            } else {
                notifyPartialSubtitle(text, null);
            }
        }
        
        @Override
        public void onFinalResult(String text) {
            if (mTranslationEnabled && mTranslationEngine != null 
                    && mTranslationEngine.isInitialized()) {
                mTranslationEngine.translate(text, new TranslationEngine.TranslationCallback() {
                    @Override
                    public void onSuccess(String translatedText) {
                        notifyFinalSubtitle(text, translatedText);
                    }
                    @Override
                    public void onError(String error) {
                        notifyFinalSubtitle(text, null);
                    }
                });
            } else {
                notifyFinalSubtitle(text, null);
            }
        }
        
        @Override
        public void onError(int errorCode, String errorMessage) {
            if (mCallback != null) {
                mMainHandler.post(() -> mCallback.onError(errorMessage));
            }
        }
        
        @Override
        public void onReady() {
            Log.d(TAG, "Speech recognition ready");
        }
        
        @Override
        public void onSpeechStart() {
            Log.d(TAG, "Speech started");
        }
        
        @Override
        public void onSpeechEnd() {
            Log.d(TAG, "Speech ended");
        }
    };
    
    private void notifyPartialSubtitle(String text, String translatedText) {
        if (mCallback != null) {
            mMainHandler.post(() -> mCallback.onPartialSubtitle(text, translatedText));
        }
    }
    
    private void notifyFinalSubtitle(String text, String translatedText) {
        if (mCallback != null) {
            mMainHandler.post(() -> mCallback.onFinalSubtitle(text, translatedText));
        }
    }
}
```

## 依赖配置

### build.gradle

```groovy
dependencies {
    // ML Kit 翻译（已有，复用）
    implementation 'com.google.mlkit:translate:17.0.1'
}
```

### AndroidManifest.xml

```xml
<!-- 录音权限 -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- 网络权限（语音识别需要） -->
<uses-permission android:name="android.permission.INTERNET" />
```

## 使用示例

```java
// 1. 创建管理器
SpeechSubtitleManager manager = new SpeechSubtitleManager(context);

// 2. 初始化语音识别
if (SpeechSubtitleManager.isAvailable(context)) {
    manager.initSpeech("zh-CN"); // 中文
    
    // 3. 初始化翻译（可选）
    manager.initTranslation("en", new TranslationEngine.ModelDownloadCallback() {
        @Override
        public void onSuccess() {
            Log.d(TAG, "Translation model ready");
        }
        @Override
        public void onError(String error) {
            Log.e(TAG, "Translation model download failed: " + error);
        }
        @Override
        public void onProgress(int progress) {}
    });
}

// 4. 设置回调
manager.setCallback(new SpeechSubtitleManager.SpeechSubtitleCallback() {
    @Override
    public void onPartialSubtitle(String text, String translatedText) {
        // 实时显示（可选）
        subtitleView.setText(text);
    }
    
    @Override
    public void onFinalSubtitle(String text, String translatedText) {
        // 显示最终字幕
        if (translatedText != null) {
            subtitleView.setText(text + "\n" + translatedText);
        } else {
            subtitleView.setText(text);
        }
    }
    
    @Override
    public void onError(String error) {
        Log.e(TAG, "Speech error: " + error);
    }
});

// 5. 开始/停止
manager.start();
// ...
manager.stop();

// 6. 释放
manager.release();
```

## 与 OCR 翻译的对比

| 特性 | OCR 翻译 | 语音识别翻译 |
|------|---------|-------------|
| 输入源 | 视频画面截图 | 音频流 |
| 识别引擎 | Tesseract OCR | Android SpeechRecognizer |
| 翻译引擎 | ML Kit Translation | ML Kit Translation（复用） |
| 实时性 | 1秒/帧 | 实时 |
| 准确度 | 依赖画面清晰度 | 依赖音频质量 |
| 离线支持 | 支持（Tesseract） | 部分支持（依赖设备） |
| 权限要求 | 无 | RECORD_AUDIO |

## 注意事项

1. **权限处理**：需要动态申请 `RECORD_AUDIO` 权限
2. **网络依赖**：Android SpeechRecognizer 通常需要网络连接
3. **持续识别**：SpeechRecognizer 会在静音后自动停止，需要实现自动重启逻辑
4. **音频冲突**：播放视频时使用语音识别可能存在音频冲突，建议使用外部麦克风或耳机
5. **语言支持**：支持的语言取决于设备上安装的语音识别服务

## 文件结构

```
palyerlibrary/src/main/java/com/orange/playerlibrary/
├── speech/
│   ├── SpeechEngine.java              # 语音识别接口
│   ├── AndroidSpeechEngine.java       # Android 实现
│   └── SpeechSubtitleManager.java     # 语音字幕管理器
├── ocr/
│   ├── TranslationEngine.java         # 翻译接口（复用）
│   └── MlKitTranslationEngine.java    # ML Kit 实现（复用）
└── subtitle/
    └── SubtitleView.java              # 字幕显示（复用）
```
