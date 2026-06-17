package com.orange.playerlibrary.speech;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
    private Handler mMainHandler;
    private String mLanguage = "zh-CN";
    private boolean mIsInitialized = false;
    private boolean mIsListening = false;
    private boolean mContinuousMode = true;
    
    @Override
    public boolean init(Context context, String language) {
        mContext = context.getApplicationContext();
        mLanguage = language;
        mMainHandler = new Handler(Looper.getMainLooper());
        
        // 注意：某些设备 isRecognitionAvailable 返回 false 但实际可用
        // 所以我们只记录日志，不直接返回 false
        boolean available = SpeechRecognizer.isRecognitionAvailable(mContext);
        Log.d(TAG, "SpeechRecognizer.isRecognitionAvailable: " + available);
        
        // 尝试创建 SpeechRecognizer（同步方式，确保返回正确结果）
        try {
            // 在主线程同步创建
            if (Looper.myLooper() == Looper.getMainLooper()) {
                return createSpeechRecognizer(language);
            } else {
                // 如果不在主线程，使用同步方式
                final boolean[] result = {false};
                final Object lock = new Object();
                mMainHandler.post(() -> {
                    synchronized (lock) {
                        result[0] = createSpeechRecognizer(language);
                        lock.notify();
                    }
                });
                synchronized (lock) {
                    try {
                        lock.wait(2000); // 最多等待 2 秒
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Interrupted while waiting for init", e);
                    }
                }
                return result[0];
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to init SpeechRecognizer", e);
            return false;
        }
    }
    
    private boolean createSpeechRecognizer(String language) {
        try {
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext);
            if (mSpeechRecognizer == null) {
                Log.e(TAG, "createSpeechRecognizer returned null");
                return false;
            }
            mSpeechRecognizer.setRecognitionListener(mRecognitionListener);
            mIsInitialized = true;
            Log.d(TAG, "SpeechRecognizer initialized successfully, language=" + language);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create SpeechRecognizer", e);
            mIsInitialized = false;
            return false;
        }
    }
    
    @Override
    public void startListening() {
        if (!mIsInitialized) {
            Log.e(TAG, "Not initialized");
            return;
        }
        
        mContinuousMode = true;
        
        mMainHandler.post(() -> {
            if (mSpeechRecognizer == null) {
                Log.e(TAG, "SpeechRecognizer is null");
                return;
            }
            
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, mLanguage);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            // 设置较长的静音超时
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000);
            
            try {
                mSpeechRecognizer.startListening(intent);
                mIsListening = true;
                Log.d(TAG, "Started listening");
            } catch (Exception e) {
                Log.e(TAG, "Failed to start listening", e);
                mIsListening = false;
            }
        });
    }
    
    @Override
    public void stopListening() {
        mContinuousMode = false;
        mIsListening = false;
        
        mMainHandler.post(() -> {
            if (mSpeechRecognizer != null) {
                try {
                    mSpeechRecognizer.stopListening();
                    mSpeechRecognizer.cancel();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to stop listening", e);
                }
            }
        });
        
        Log.d(TAG, "Stopped listening");
    }
    
    @Override
    public void setCallback(SpeechCallback callback) {
        mCallback = callback;
    }
    
    @Override
    public void release() {
        stopListening();
        
        mMainHandler.post(() -> {
            if (mSpeechRecognizer != null) {
                try {
                    mSpeechRecognizer.destroy();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to destroy SpeechRecognizer", e);
                }
                mSpeechRecognizer = null;
            }
        });
        
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
            
            mIsListening = false;
            
            // 某些错误不需要通知用户，也不需要重试
            boolean shouldNotify = true;
            boolean shouldRetry = false;
            
            switch (error) {
                case SpeechRecognizer.ERROR_NO_MATCH:
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    // 没有匹配或超时，静默重试
                    shouldNotify = false;
                    shouldRetry = mContinuousMode;
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    // 客户端错误（通常是没有语音识别服务），不重试
                    shouldNotify = true;
                    shouldRetry = false;
                    mContinuousMode = false; // 停止持续模式
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    // 权限不足，不重试
                    shouldNotify = true;
                    shouldRetry = false;
                    mContinuousMode = false;
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    // 识别器忙，稍后重试
                    shouldNotify = false;
                    shouldRetry = mContinuousMode;
                    break;
                default:
                    // 其他错误，通知用户但可以重试
                    shouldNotify = true;
                    shouldRetry = mContinuousMode;
                    break;
            }
            
            if (mCallback != null && shouldNotify) {
                mCallback.onError(error, errorMessage);
            }
            
            // 持续模式下自动重启
            if (shouldRetry) {
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
            
            mIsListening = false;
            
            // 持续模式下自动重启
            if (mContinuousMode) {
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
        mMainHandler.postDelayed(() -> {
            if (mContinuousMode && mIsInitialized) {
                startListening();
            }
        }, 300);
    }
    
    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO: return "音频录制错误";
            case SpeechRecognizer.ERROR_CLIENT: return "没有可用的语音识别服务，请在系统设置中配置";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "权限不足，请授予录音权限";
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
