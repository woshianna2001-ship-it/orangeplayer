package com.orange.playerlibrary.speech;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.orange.playerlibrary.ocr.MlKitTranslationEngine;
import com.orange.playerlibrary.ocr.OcrAvailabilityChecker;
import com.orange.playerlibrary.ocr.TranslationEngine;

/**
 * 语音字幕管理器
 * 使用 Vosk + AudioPlaybackCapture 识别视频中的语音
 * 需要 Android 10 (API 29) 以上
 */
public class SpeechSubtitleManager {
    
    private static final String TAG = "SpeechSubtitleManager";
    public static final int REQUEST_CODE_MEDIA_PROJECTION = 2001;
    
    private Context mContext;
    private Activity mActivity;
    private Handler mMainHandler;
    
    private AudioCaptureService mCaptureService;
    private boolean mServiceBound = false;
    private TranslationEngine mTranslationEngine;
    
    private boolean mIsRunning = false;
    private String mSourceLanguage = "zh-CN";
    private String mTargetLanguage = "en";
    private boolean mTranslationEnabled = true;
    
    // 用于动态控制翻译开关（运行时可修改）
    private boolean mRuntimeTranslationEnabled = true;
    
    private SpeechSubtitleCallback mCallback;
    private MediaProjectionManager mProjectionManager;
    private Intent mPendingProjectionData;
    
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
        /** 状态变化 */
        void onStateChanged(boolean isListening);
    }
    
    public SpeechSubtitleManager(Context context) {
        mContext = context.getApplicationContext();
        if (context instanceof Activity) {
            mActivity = (Activity) context;
        }
        mMainHandler = new Handler(Looper.getMainLooper());
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mProjectionManager = (MediaProjectionManager) 
                    mContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        }
    }
    
    /**
     * 检查是否支持（需要 Android 10+）
     */
    public static boolean isSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }
    
    /**
     * 获取不支持的原因
     */
    public static String getUnsupportedReason() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return "需要 Android 10 或更高版本才能捕获视频音频";
        }
        return null;
    }
    
    /**
     * 检查语言模型是否已下载
     */
    public static boolean isModelDownloaded(Context context, String language) {
        return VoskSpeechEngine.isModelDownloaded(context, language);
    }
    
    /**
     * 检查翻译功能是否可用
     */
    public static boolean isTranslationAvailable() {
        return OcrAvailabilityChecker.isMlKitTranslateAvailable();
    }
    
    /**
     * 设置回调
     */
    public void setCallback(SpeechSubtitleCallback callback) {
        mCallback = callback;
    }
    
    /**
     * 请求屏幕捕获权限
     * 需要在 Activity 中调用，并在 onActivityResult 中处理结果
     */
    public void requestMediaProjection(Activity activity) {
        if (!isSupported()) {
            if (mCallback != null) {
                mCallback.onError(getUnsupportedReason());
            }
            return;
        }
        
        mActivity = activity;
        if (mProjectionManager == null) {
            mProjectionManager = (MediaProjectionManager) 
                    activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        }
        
        Intent intent = mProjectionManager.createScreenCaptureIntent();
        activity.startActivityForResult(intent, REQUEST_CODE_MEDIA_PROJECTION);
    }
    
    /**
     * 处理屏幕捕获权限结果
     * @return true 如果权限已授予
     */
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "handleActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode + ", data=" + data);
        
        if (requestCode != REQUEST_CODE_MEDIA_PROJECTION) {
            Log.d(TAG, "handleActivityResult: requestCode mismatch, expected=" + REQUEST_CODE_MEDIA_PROJECTION);
            return false;
        }
        
        if (resultCode != Activity.RESULT_OK || data == null) {
            Log.e(TAG, "Media projection permission denied, resultCode=" + resultCode);
            if (mCallback != null) {
                mCallback.onError("需要授权才能捕获视频音频");
            }
            return false;
        }
        
        Log.d(TAG, "handleActivityResult: permission granted, saving projection data");
        mPendingProjectionData = data;
        return true;
    }
    
    /**
     * 初始化并开始语音识别
     * @param sourceLanguage 语言代码 (zh-CN, en-US, ja-JP)
     * @param targetLanguage 翻译目标语言 (zh, en, ja)，null 表示不翻译
     */
    public void start(String sourceLanguage, String targetLanguage) {
        Log.d(TAG, "start: sourceLanguage=" + sourceLanguage + ", targetLanguage=" + targetLanguage);
        
        if (!isSupported()) {
            Log.e(TAG, "start: not supported - " + getUnsupportedReason());
            if (mCallback != null) {
                mCallback.onError(getUnsupportedReason());
            }
            return;
        }
        
        mSourceLanguage = sourceLanguage;
        mTargetLanguage = targetLanguage;
        // 根据 targetLanguage 是否为 null 设置初始翻译状态
        mTranslationEnabled = targetLanguage != null;
        // 同步运行时翻译开关状态
        mRuntimeTranslationEnabled = mTranslationEnabled;
        
        // 检查模型是否已下载
        if (!VoskSpeechEngine.isModelDownloaded(mContext, sourceLanguage)) {
            Log.e(TAG, "start: model not downloaded for " + sourceLanguage);
            if (mCallback != null) {
                mCallback.onError("语音模型未下载，请先下载 " + getLanguageName(sourceLanguage) + " 模型");
            }
            return;
        }
        
        Log.d(TAG, "start: model downloaded, binding capture service");
        // 绑定服务
        bindCaptureService();
    }
    
    /**
     * 绑定音频捕获服务
     */
    private void bindCaptureService() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }
        
        Intent serviceIntent = new Intent(mContext, AudioCaptureService.class);
        
        // 先启动前台服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mContext.startForegroundService(serviceIntent);
        } else {
            mContext.startService(serviceIntent);
        }
        
        // 绑定服务
        mContext.bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }
    
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected");
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                AudioCaptureService.LocalBinder binder = (AudioCaptureService.LocalBinder) service;
                mCaptureService = binder.getService();
                mServiceBound = true;
                
                // 设置回调
                mCaptureService.setCallback(mCaptureCallback);
                
                // 通知用户正在加载模型
                if (mCallback != null) {
                    mCallback.onStateChanged(false); // 还未开始识别
                }
                
                // 异步初始化 Vosk（避免大型模型加载卡顿）
                mCaptureService.initVoskAsync(mSourceLanguage, success -> {
                    if (!success) {
                        if (mCallback != null) {
                            mMainHandler.post(() -> mCallback.onError("语音识别引擎初始化失败"));
                        }
                        return;
                    }
                    
                    Log.d(TAG, "Vosk initialized successfully");
                    
                    // 初始化翻译
                    if (mTranslationEnabled) {
                        initTranslation();
                    }
                    
                    // 开始捕获
                    startCapture();
                });
            }
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service disconnected");
            mCaptureService = null;
            mServiceBound = false;
        }
    };
    
    private final AudioCaptureService.AudioCaptureCallback mCaptureCallback = 
            new AudioCaptureService.AudioCaptureCallback() {
        @Override
        public void onPartialResult(String text) {
            if (text == null || text.isEmpty()) return;
            
            // 根据翻译开关状态决定是否调用翻译引擎
            if (isTranslationEnabled() && mTranslationEngine != null 
                    && mTranslationEngine.isInitialized()) {
                mTranslationEngine.translate(text, new TranslationEngine.TranslationCallback() {
                    @Override
                    public void onSuccess(String translatedText) {
                        // 翻译启用时返回原文+译文
                        notifyPartialSubtitle(text, translatedText);
                    }
                    @Override
                    public void onError(String error) {
                        // 翻译失败时只返回原文
                        notifyPartialSubtitle(text, null);
                    }
                });
            } else {
                // 翻译禁用时只返回原文，translatedText 为 null
                notifyPartialSubtitle(text, null);
            }
        }
        
        @Override
        public void onFinalResult(String text) {
            if (text == null || text.isEmpty()) return;
            
            // 根据翻译开关状态决定是否调用翻译引擎
            if (isTranslationEnabled() && mTranslationEngine != null 
                    && mTranslationEngine.isInitialized()) {
                mTranslationEngine.translate(text, new TranslationEngine.TranslationCallback() {
                    @Override
                    public void onSuccess(String translatedText) {
                        // 翻译启用时返回原文+译文
                        notifyFinalSubtitle(text, translatedText);
                    }
                    @Override
                    public void onError(String error) {
                        // 翻译失败时只返回原文
                        notifyFinalSubtitle(text, null);
                    }
                });
            } else {
                // 翻译禁用时只返回原文，translatedText 为 null
                notifyFinalSubtitle(text, null);
            }
        }
        
        @Override
        public void onError(String error) {
            if (mCallback != null) {
                mMainHandler.post(() -> mCallback.onError(error));
            }
        }
        
        @Override
        public void onStateChanged(boolean isCapturing) {
            mIsRunning = isCapturing;
            if (mCallback != null) {
                mMainHandler.post(() -> mCallback.onStateChanged(isCapturing));
            }
        }
    };
    
    /**
     * 开始音频捕获
     */
    private void startCapture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }
        
        if (mPendingProjectionData == null) {
            if (mCallback != null) {
                mCallback.onError("请先授权屏幕捕获权限");
            }
            return;
        }
        
        if (mCaptureService == null) {
            if (mCallback != null) {
                mCallback.onError("服务未连接");
            }
            return;
        }
        
        // 获取 MediaProjection
        MediaProjection projection = mProjectionManager.getMediaProjection(
                Activity.RESULT_OK, mPendingProjectionData);
        
        if (projection == null) {
            if (mCallback != null) {
                mCallback.onError("无法获取屏幕捕获权限");
            }
            return;
        }
        
        // 开始捕获
        mCaptureService.startCapture(projection);
        mIsRunning = true;
    }
    
    /**
     * 初始化翻译
     */
    private void initTranslation() {
        if (!isTranslationAvailable()) {
            Log.w(TAG, "Translation not available");
            return;
        }
        
        mTranslationEngine = new MlKitTranslationEngine();
        
        // 转换语言代码
        String sourceCode = convertLanguageCode(mSourceLanguage);
        mTranslationEngine.init(mContext, sourceCode, mTargetLanguage);
        
        // 下载模型（如果需要）
        mTranslationEngine.downloadModel(new TranslationEngine.ModelDownloadCallback() {
            @Override
            public void onProgress(int progress) {}
            
            @Override
            public void onSuccess() {
                Log.d(TAG, "Translation model ready");
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Translation model download failed: " + error);
            }
        });
    }
    
    /**
     * 停止语音识别
     */
    public void stop() {
        if (!mIsRunning) return;
        
        mIsRunning = false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mCaptureService != null) {
            mCaptureService.stopCapture();
        }
        
        if (mCallback != null) {
            mCallback.onStateChanged(false);
        }
        
        Log.d(TAG, "Stopped");
    }
    
    /**
     * 释放资源
     */
    public void release() {
        stop();
        
        if (mServiceBound) {
            mContext.unbindService(mServiceConnection);
            mServiceBound = false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mCaptureService != null) {
            mCaptureService.release();
            mCaptureService = null;
        }
        
        if (mTranslationEngine != null) {
            mTranslationEngine.release();
            mTranslationEngine = null;
        }
        
        // 停止服务
        Intent serviceIntent = new Intent(mContext, AudioCaptureService.class);
        mContext.stopService(serviceIntent);
        
        Log.d(TAG, "Released");
    }
    
    /**
     * 是否正在运行
     */
    public boolean isRunning() {
        return mIsRunning;
    }
    
    /**
     * 获取源语言
     */
    public String getSourceLanguage() {
        return mSourceLanguage;
    }
    
    /**
     * 获取目标语言
     */
    public String getTargetLanguage() {
        return mTargetLanguage;
    }
    
    /**
     * 设置翻译开关状态（运行时动态控制）
     * 根据开关状态决定是否调用翻译引擎
     * @param enabled true 启用翻译，false 禁用翻译
     */
    public void setTranslationEnabled(boolean enabled) {
        mRuntimeTranslationEnabled = enabled;
        Log.d(TAG, "Translation enabled: " + enabled);
    }
    
    /**
     * 获取翻译开关状态
     * @return true 如果翻译已启用
     */
    public boolean isTranslationEnabled() {
        return mTranslationEnabled && mRuntimeTranslationEnabled;
    }
    
    /**
     * 转换语言代码
     */
    private String convertLanguageCode(String bcp47Code) {
        if (bcp47Code == null) return "zh";
        
        String lang = bcp47Code.toLowerCase();
        if (lang.startsWith("zh")) return "zh";
        if (lang.startsWith("en")) return "en";
        if (lang.startsWith("ja")) return "ja";
        if (lang.startsWith("ko")) return "ko";
        
        if (lang.length() >= 2) {
            return lang.substring(0, 2);
        }
        return lang;
    }
    
    /**
     * 获取语言名称
     */
    private String getLanguageName(String code) {
        if (code == null) return "未知";
        String lang = code.toLowerCase();
        if (lang.startsWith("zh")) return "中文";
        if (lang.startsWith("en")) return "英语";
        if (lang.startsWith("ja")) return "日语";
        if (lang.startsWith("ko")) return "韩语";
        return code;
    }
    
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
