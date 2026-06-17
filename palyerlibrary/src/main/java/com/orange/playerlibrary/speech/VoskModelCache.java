package com.orange.playerlibrary.speech;

import android.content.Context;
import android.util.Log;

import org.vosk.Model;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Vosk 模型缓存管理器
 * 将模型缓存在内存中，避免每次重新加载
 */
public class VoskModelCache {
    
    private static final String TAG = "VoskModelCache";
    private static VoskModelCache sInstance;
    
    // 缓存的模型 <语言代码, Model>
    private final Map<String, Model> mModelCache = new HashMap<>();
    
    private VoskModelCache() {
    }
    
    public static synchronized VoskModelCache getInstance() {
        if (sInstance == null) {
            sInstance = new VoskModelCache();
        }
        return sInstance;
    }
    
    /**
     * 获取缓存的模型
     * @param language 语言代码
     * @return 缓存的模型，如果不存在返回 null
     */
    public synchronized Model getCachedModel(String language) {
        return mModelCache.get(language);
    }
    
    /**
     * 加载并缓存模型
     * @param context 上下文
     * @param language 语言代码
     * @return 加载的模型，失败返回 null
     */
    public synchronized Model loadAndCacheModel(Context context, String language) {
        // 检查是否已缓存
        Model cachedModel = mModelCache.get(language);
        if (cachedModel != null) {
            Log.d(TAG, "Model already cached for language: " + language);
            return cachedModel;
        }
        
        // 获取模型路径
        VoskModelManager modelManager = new VoskModelManager(context);
        String modelPath = modelManager.getInstalledModelPath(language);
        
        if (modelPath == null) {
            // 回退到旧的检查方式
            modelPath = VoskSpeechEngine.getModelPath(language);
        }
        
        if (modelPath == null) {
            Log.e(TAG, "Model path not found for language: " + language);
            return null;
        }
        
        File modelDir = new File(context.getFilesDir(), modelPath);
        if (!modelDir.exists()) {
            Log.e(TAG, "Model directory not found: " + modelDir.getAbsolutePath());
            return null;
        }
        
        try {
            Log.d(TAG, "Loading model for language: " + language + ", path: " + modelPath);
            Model model = new Model(modelDir.getAbsolutePath());
            mModelCache.put(language, model);
            Log.d(TAG, "Model loaded and cached successfully");
            return model;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load model", e);
            return null;
        }
    }
    
    /**
     * 检查模型是否已缓存
     */
    public synchronized boolean isModelCached(String language) {
        return mModelCache.containsKey(language);
    }
    
    /**
     * 清除指定语言的缓存
     */
    public synchronized void clearCache(String language) {
        Model model = mModelCache.remove(language);
        if (model != null) {
            try {
                model.close();
                Log.d(TAG, "Model cache cleared for language: " + language);
            } catch (Exception e) {
                Log.e(TAG, "Error closing model", e);
            }
        }
    }
    
    /**
     * 清除所有缓存
     */
    public synchronized void clearAllCache() {
        for (Map.Entry<String, Model> entry : mModelCache.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing model: " + entry.getKey(), e);
            }
        }
        mModelCache.clear();
        Log.d(TAG, "All model cache cleared");
    }
}
