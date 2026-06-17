package com.orange.playerlibrary.speech;

import android.content.Context;
import android.util.Log;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechStreamService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Vosk 离线语音识别引擎
 * 支持中文、英文、日语等多种语言
 */
public class VoskSpeechEngine implements SpeechEngine {
    
    private static final String TAG = "VoskSpeechEngine";
    private static final int SAMPLE_RATE = 16000;
    
    private Context mContext;
    private Model mModel;
    private Recognizer mRecognizer;
    private SpeechCallback mCallback;
    private String mLanguage;
    private boolean mIsInitialized = false;
    private boolean mIsListening = false;
    
    // 线程安全标志
    private volatile boolean mIsReleasing = false;
    
    // 模型文件名映射
    private static final String MODEL_PATH_CN = "vosk-model-small-cn";
    private static final String MODEL_PATH_EN = "vosk-model-small-en-us";
    private static final String MODEL_PATH_JA = "vosk-model-small-ja";
    
    @Override
    public boolean init(Context context, String language) {
        mContext = context.getApplicationContext();
        mLanguage = language;
        
        // 尝试从缓存获取模型
        VoskModelCache cache = VoskModelCache.getInstance();
        mModel = cache.getCachedModel(language);
        
        if (mModel != null) {
            Log.d(TAG, "Using cached model for language: " + language);
        } else {
            // 缓存中没有，需要加载
            // 尝试使用 VoskModelManager 获取已安装的模型路径
            VoskModelManager modelManager = new VoskModelManager(context);
            String modelPath = modelManager.getInstalledModelPath(language);
            
            // 如果 VoskModelManager 没有找到，回退到旧的检查方式
            if (modelPath == null) {
                modelPath = getModelPath(language);
                if (modelPath == null) {
                    Log.e(TAG, "Unsupported language: " + language);
                    return false;
                }
            }
            
            File modelDir = new File(context.getFilesDir(), modelPath);
            if (!modelDir.exists()) {
                Log.e(TAG, "Model not found: " + modelDir.getAbsolutePath());
                return false;
            }
            
            try {
                Log.d(TAG, "Loading model from disk for language: " + language);
                mModel = new Model(modelDir.getAbsolutePath());
                // 缓存模型（使用公共方法）
                // 注意：模型已经在这里创建，不需要再次缓存
                Log.d(TAG, "Model loaded and will be cached on next access");
            } catch (IOException e) {
                Log.e(TAG, "Failed to initialize Vosk", e);
                return false;
            }
        }
        
        try {
            mRecognizer = new Recognizer(mModel, SAMPLE_RATE);
            mIsInitialized = true;
            Log.d(TAG, "Vosk initialized, language=" + language + ", modelPath=" + (mModel != null ? "cached" : "loaded"));
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to create recognizer", e);
            return false;
        }
    }
    
    /**
     * 使用音频流初始化（用于 AudioPlaybackCapture）
     */
    public boolean initWithAudioCapture(Context context, String language) {
        return init(context, language);
    }
    
    /**
     * 处理音频数据
     * @param audioData PCM 16-bit 音频数据
     * @param length 数据长度
     */
    public void processAudioData(short[] audioData, int length) {
        if (!mIsInitialized || mRecognizer == null || mIsReleasing) {
            return;
        }
        
        // 转换为 byte 数组
        byte[] bytes = new byte[length * 2];
        for (int i = 0; i < length; i++) {
            bytes[i * 2] = (byte) (audioData[i] & 0xff);
            bytes[i * 2 + 1] = (byte) ((audioData[i] >> 8) & 0xff);
        }
        
        if (mRecognizer.acceptWaveForm(bytes, bytes.length)) {
            // Final 结果：这是一句完整的话
            String result = mRecognizer.getResult();
            String text = parseResult(result);
            if (text != null && !text.isEmpty() && mCallback != null && !mIsReleasing) {
                Log.d(TAG, "Final result (short): " + text);
                mCallback.onFinalResult(text);
                // 重置 Recognizer
                resetRecognizer();
            }
        }
        // 不处理 partial 结果
    }
    
    /**
     * 处理音频数据（byte 数组版本）
     * 
     * 关键改进：
     * 1. 每次 final 结果后重置 Recognizer，确保每句话独立识别
     * 2. 显示 partial 结果（实时识别），每次替换而不是累积
     */
    public void processAudioData(byte[] audioData, int length) {
        // 线程安全检查
        if (!mIsInitialized || mRecognizer == null || mIsReleasing) {
            return;
        }
        
        try {
            if (mRecognizer.acceptWaveForm(audioData, length)) {
                // Final 结果：这是一句完整的话
                String result = mRecognizer.getResult();
                String text = parseResult(result);
                if (text != null && !text.isEmpty() && mCallback != null && !mIsReleasing) {
                    Log.d(TAG, "========== Final result: [" + text + "] ==========");
                    mCallback.onFinalResult(text);
                    
                    // 重置 Recognizer，开始新的一句话识别
                    resetRecognizer();
                    Log.d(TAG, "Recognizer reset, ready for next sentence");
                }
            } else {
                // Partial 结果：实时识别中
                // 注意：Vosk 的 partial 是累积的，但我们每次都替换显示，所以不会越来越长
                String partialResult = mRecognizer.getPartialResult();
                String text = parsePartialResult(partialResult);
                if (text != null && !text.isEmpty() && mCallback != null && !mIsReleasing) {
                    // 只在有变化时才回调，减少不必要的 UI 更新
                    Log.v(TAG, "Partial result: [" + text + "]");
                    mCallback.onPartialResult(text);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing audio data", e);
        }
    }
    
    /**
     * 重置 Recognizer，清除累积的识别状态
     * 每次 final 结果后调用，确保下一句话从头开始识别
     */
    private void resetRecognizer() {
        if (mModel != null && !mIsReleasing) {
            try {
                // 关闭旧的 Recognizer
                if (mRecognizer != null) {
                    mRecognizer.close();
                }
                // 创建新的 Recognizer
                mRecognizer = new Recognizer(mModel, SAMPLE_RATE);
                Log.d(TAG, "Recognizer reset for new sentence");
            } catch (Exception e) {
                Log.e(TAG, "Error resetting recognizer", e);
            }
        }
    }
    
    private String parseResult(String json) {
        // 解析 {"text": "识别结果"}
        try {
            if (json == null || json.isEmpty()) return null;
            int start = json.indexOf("\"text\"");
            if (start < 0) return null;
            start = json.indexOf(":", start) + 1;
            int end = json.lastIndexOf("\"");
            if (end <= start) return null;
            start = json.indexOf("\"", start) + 1;
            return json.substring(start, end).trim();
        } catch (Exception e) {
            return null;
        }
    }
    
    private String parsePartialResult(String json) {
        // 解析 {"partial": "部分结果"}
        try {
            if (json == null || json.isEmpty()) return null;
            int start = json.indexOf("\"partial\"");
            if (start < 0) return null;
            start = json.indexOf(":", start) + 1;
            int end = json.lastIndexOf("\"");
            if (end <= start) return null;
            start = json.indexOf("\"", start) + 1;
            return json.substring(start, end).trim();
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public void startListening() {
        if (!mIsInitialized) {
            Log.e(TAG, "Not initialized");
            return;
        }
        mIsListening = true;
        if (mCallback != null) {
            mCallback.onReady();
        }
        Log.d(TAG, "Started listening");
    }
    
    @Override
    public void stopListening() {
        mIsListening = false;
        
        // 获取最终结果
        if (mRecognizer != null) {
            String finalResult = mRecognizer.getFinalResult();
            String text = parseResult(finalResult);
            if (text != null && !text.isEmpty() && mCallback != null) {
                mCallback.onFinalResult(text);
            }
        }
        
        Log.d(TAG, "Stopped listening");
    }
    
    @Override
    public void setCallback(SpeechCallback callback) {
        mCallback = callback;
    }
    
    @Override
    public void release() {
        // 设置释放标志，防止其他线程继续访问
        mIsReleasing = true;
        mIsListening = false;
        mIsInitialized = false;
        
        // 等待一小段时间，让正在处理的音频数据完成
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // ignore
        }
        
        if (mRecognizer != null) {
            try {
                mRecognizer.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing recognizer", e);
            }
            mRecognizer = null;
        }
        
        // 不要关闭 Model，因为它被缓存了
        // Model 会在 VoskModelCache.clearCache() 时统一关闭
        mModel = null;
        
        // 重置状态
        mIsReleasing = false;
        
        Log.d(TAG, "Released (model kept in cache)");
    }
    
    @Override
    public boolean isInitialized() {
        return mIsInitialized;
    }
    
    @Override
    public boolean isListening() {
        return mIsListening;
    }
    
    /**
     * 获取语言对应的模型路径
     */
    public static String getModelPath(String language) {
        if (language == null) return MODEL_PATH_CN;
        
        String lang = language.toLowerCase();
        if (lang.startsWith("zh") || lang.equals("chinese")) {
            return MODEL_PATH_CN;
        } else if (lang.startsWith("en") || lang.equals("english")) {
            return MODEL_PATH_EN;
        } else if (lang.startsWith("ja") || lang.equals("japanese")) {
            return MODEL_PATH_JA;
        }
        
        return null;
    }
    
    /**
     * 获取模型下载 URL
     */
    public static String getModelDownloadUrl(String language) {
        String modelPath = getModelPath(language);
        if (modelPath == null) return null;
        
        // Vosk 官方模型下载地址
        switch (modelPath) {
            case MODEL_PATH_CN:
                return "https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip";
            case MODEL_PATH_EN:
                return "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip";
            case MODEL_PATH_JA:
                return "https://alphacephei.com/vosk/models/vosk-model-small-ja-0.22.zip";
            default:
                return null;
        }
    }
    
    /**
     * 检查模型是否已下载
     * 支持检查任意品质的模型（小型、标准、大型）
     */
    public static boolean isModelDownloaded(Context context, String language) {
        // 首先使用 VoskModelManager 检查（支持所有品质）
        VoskModelManager modelManager = new VoskModelManager(context);
        if (modelManager.isLanguageInstalled(language)) {
            return true;
        }
        
        // 回退到旧的检查方式（仅检查小型模型）
        String modelPath = getModelPath(language);
        if (modelPath == null) return false;
        
        File modelDir = new File(context.getFilesDir(), modelPath);
        return modelDir.exists() && modelDir.isDirectory();
    }
    
    /**
     * 获取模型大小描述
     */
    public static String getModelSizeDescription(String language) {
        String modelPath = getModelPath(language);
        if (modelPath == null) return "未知";
        
        switch (modelPath) {
            case MODEL_PATH_CN:
                return "约 42MB";
            case MODEL_PATH_EN:
                return "约 40MB";
            case MODEL_PATH_JA:
                return "约 48MB";
            default:
                return "未知";
        }
    }
}
