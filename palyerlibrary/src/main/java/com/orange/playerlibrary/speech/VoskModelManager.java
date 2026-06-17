package com.orange.playerlibrary.speech;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Vosk 模型管理器
 * 负责下载和解压语言模型
 * 支持多语言和多品质选择
 */
public class VoskModelManager {
    
    private static final String TAG = "VoskModelManager";
    private static final int BUFFER_SIZE = 8192;
    private static final int CONNECT_TIMEOUT = 30000;
    private static final int READ_TIMEOUT = 60000;
    private static final String PREF_NAME = "vosk_model_prefs";
    private static final String PREF_INSTALLED_QUALITY_PREFIX = "installed_quality_";
    
    private Context mContext;
    private ExecutorService mExecutor;
    private Handler mMainHandler;
    private volatile boolean mIsCancelled = false;
    private SharedPreferences mPrefs;
    
    // ==================== 数据类定义 ====================
    
    /**
     * 模型品质等级
     */
    public enum ModelQuality {
        SMALL("small", "小型", "快速下载，基础准确度"),
        STANDARD("standard", "标准", "平衡大小和准确度"),
        LARGE("large", "大型", "最高准确度，较大体积");
        
        public final String code;
        public final String displayName;
        public final String description;
        
        ModelQuality(String code, String displayName, String description) {
            this.code = code;
            this.displayName = displayName;
            this.description = description;
        }
        
        /**
         * 根据代码获取品质等级
         */
        public static ModelQuality fromCode(String code) {
            if (code == null) return null;
            for (ModelQuality quality : values()) {
                if (quality.code.equals(code)) {
                    return quality;
                }
            }
            return null;
        }
    }
    
    /**
     * 模型信息
     */
    public static class ModelInfo {
        public String downloadUrl;       // 下载地址
        public long estimatedSize;       // 预估大小（字节）
        public String modelPath;         // 模型路径
        
        public ModelInfo(String downloadUrl, long estimatedSize, String modelPath) {
            this.downloadUrl = downloadUrl;
            this.estimatedSize = estimatedSize;
            this.modelPath = modelPath;
        }
        
        /**
         * 获取格式化的大小描述
         */
        public String getSizeDescription() {
            if (estimatedSize < 1024 * 1024) {
                return String.format("%.1f KB", estimatedSize / 1024.0);
            } else if (estimatedSize < 1024 * 1024 * 1024) {
                return String.format("%.1f MB", estimatedSize / (1024.0 * 1024));
            } else {
                return String.format("%.2f GB", estimatedSize / (1024.0 * 1024 * 1024));
            }
        }
    }
    
    /**
     * 语言模型信息
     */
    public static class LanguageModel {
        public String languageCode;      // 语言代码 (zh-CN, en-US, etc.)
        public String displayName;       // 显示名称
        public Map<ModelQuality, ModelInfo> qualityOptions;  // 品质选项
        public boolean isInstalled;      // 是否已安装
        public ModelQuality installedQuality;  // 已安装的品质
        
        public LanguageModel(String languageCode, String displayName) {
            this.languageCode = languageCode;
            this.displayName = displayName;
            this.qualityOptions = new HashMap<>();
            this.isInstalled = false;
            this.installedQuality = null;
        }
        
        /**
         * 添加品质选项
         */
        public LanguageModel addQualityOption(ModelQuality quality, ModelInfo info) {
            qualityOptions.put(quality, info);
            return this;
        }
        
        /**
         * 获取可用的品质列表
         */
        public List<ModelQuality> getAvailableQualities() {
            List<ModelQuality> qualities = new ArrayList<>();
            // 按顺序添加
            if (qualityOptions.containsKey(ModelQuality.SMALL)) {
                qualities.add(ModelQuality.SMALL);
            }
            if (qualityOptions.containsKey(ModelQuality.STANDARD)) {
                qualities.add(ModelQuality.STANDARD);
            }
            if (qualityOptions.containsKey(ModelQuality.LARGE)) {
                qualities.add(ModelQuality.LARGE);
            }
            return qualities;
        }
        
        /**
         * 获取指定品质的模型信息
         */
        public ModelInfo getModelInfo(ModelQuality quality) {
            return qualityOptions.get(quality);
        }
        
        /**
         * 获取默认品质（优先 SMALL）
         */
        public ModelQuality getDefaultQuality() {
            if (qualityOptions.containsKey(ModelQuality.SMALL)) {
                return ModelQuality.SMALL;
            } else if (qualityOptions.containsKey(ModelQuality.STANDARD)) {
                return ModelQuality.STANDARD;
            } else if (qualityOptions.containsKey(ModelQuality.LARGE)) {
                return ModelQuality.LARGE;
            }
            return null;
        }
    }
    
    // ==================== 回调接口 ====================
    
    public interface DownloadCallback {
        void onProgress(int progress, String status);
        void onSuccess();
        void onError(String error);
    }
    
    // ==================== 支持的语言配置 ====================
    
    /**
     * 支持的语言列表（静态配置）
     */
    private static final Map<String, LanguageModel> SUPPORTED_LANGUAGES = new HashMap<>();
    
    static {
        // 中文
        SUPPORTED_LANGUAGES.put("zh-CN", new LanguageModel("zh-CN", "中文")
            .addQualityOption(ModelQuality.SMALL, new ModelInfo(
                "https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip",
                42_000_000L, "vosk-model-small-cn"))
            .addQualityOption(ModelQuality.LARGE, new ModelInfo(
                "https://alphacephei.com/vosk/models/vosk-model-cn-0.22.zip",
                1_300_000_000L, "vosk-model-cn")));
        
        // 英语
        SUPPORTED_LANGUAGES.put("en-US", new LanguageModel("en-US", "英语")
            .addQualityOption(ModelQuality.SMALL, new ModelInfo(
                "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip",
                40_000_000L, "vosk-model-small-en-us"))
            .addQualityOption(ModelQuality.LARGE, new ModelInfo(
                "https://alphacephei.com/vosk/models/vosk-model-en-us-0.22.zip",
                1_800_000_000L, "vosk-model-en-us")));
        
        // 日语
        SUPPORTED_LANGUAGES.put("ja-JP", new LanguageModel("ja-JP", "日语")
            .addQualityOption(ModelQuality.SMALL, new ModelInfo(
                "https://alphacephei.com/vosk/models/vosk-model-small-ja-0.22.zip",
                48_000_000L, "vosk-model-small-ja"))
            .addQualityOption(ModelQuality.LARGE, new ModelInfo(
                "https://alphacephei.com/vosk/models/vosk-model-ja-0.22.zip",
                1_000_000_000L, "vosk-model-ja")));
        
        // 韩语
        SUPPORTED_LANGUAGES.put("ko-KR", new LanguageModel("ko-KR", "韩语")
            .addQualityOption(ModelQuality.SMALL, new ModelInfo(
                "https://alphacephei.com/vosk/models/vosk-model-small-ko-0.22.zip",
                82_000_000L, "vosk-model-small-ko")));
        
        // 法语
        SUPPORTED_LANGUAGES.put("fr-FR", new LanguageModel("fr-FR", "法语")
            .addQualityOption(ModelQuality.SMALL, new ModelInfo(
                "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip",
                41_000_000L, "vosk-model-small-fr"))
            .addQualityOption(ModelQuality.LARGE, new ModelInfo(
                "https://alphacephei.com/vosk/models/vosk-model-fr-0.22.zip",
                1_400_000_000L, "vosk-model-fr")));
        
        // 德语
        SUPPORTED_LANGUAGES.put("de-DE", new LanguageModel("de-DE", "德语")
            .addQualityOption(ModelQuality.SMALL, new ModelInfo(
                "https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip",
                45_000_000L, "vosk-model-small-de"))
            .addQualityOption(ModelQuality.LARGE, new ModelInfo(
                "https://alphacephei.com/vosk/models/vosk-model-de-0.21.zip",
                1_900_000_000L, "vosk-model-de")));
        
        // 西班牙语
        SUPPORTED_LANGUAGES.put("es-ES", new LanguageModel("es-ES", "西班牙语")
            .addQualityOption(ModelQuality.SMALL, new ModelInfo(
                "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip",
                39_000_000L, "vosk-model-small-es"))
            .addQualityOption(ModelQuality.LARGE, new ModelInfo(
                "https://alphacephei.com/vosk/models/vosk-model-es-0.42.zip",
                1_400_000_000L, "vosk-model-es")));
        
        // 俄语
        SUPPORTED_LANGUAGES.put("ru-RU", new LanguageModel("ru-RU", "俄语")
            .addQualityOption(ModelQuality.SMALL, new ModelInfo(
                "https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip",
                45_000_000L, "vosk-model-small-ru"))
            .addQualityOption(ModelQuality.LARGE, new ModelInfo(
                "https://alphacephei.com/vosk/models/vosk-model-ru-0.42.zip",
                1_800_000_000L, "vosk-model-ru")));
        
        // 意大利语
        SUPPORTED_LANGUAGES.put("it-IT", new LanguageModel("it-IT", "意大利语")
            .addQualityOption(ModelQuality.SMALL, new ModelInfo(
                "https://alphacephei.com/vosk/models/vosk-model-small-it-0.22.zip",
                48_000_000L, "vosk-model-small-it"))
            .addQualityOption(ModelQuality.LARGE, new ModelInfo(
                "https://alphacephei.com/vosk/models/vosk-model-it-0.22.zip",
                1_200_000_000L, "vosk-model-it")));
        
        // 葡萄牙语
        SUPPORTED_LANGUAGES.put("pt-PT", new LanguageModel("pt-PT", "葡萄牙语")
            .addQualityOption(ModelQuality.SMALL, new ModelInfo(
                "https://alphacephei.com/vosk/models/vosk-model-small-pt-0.3.zip",
                31_000_000L, "vosk-model-small-pt"))
            .addQualityOption(ModelQuality.LARGE, new ModelInfo(
                "https://alphacephei.com/vosk/models/vosk-model-pt-fb-v0.1.1-20220516_2113.zip",
                1_600_000_000L, "vosk-model-pt")));
    }
    
    // ==================== 语言查询方法 ====================
    
    /**
     * 获取所有支持的语言列表
     * @return 语言模型列表（包含安装状态）
     */
    public List<LanguageModel> getSupportedLanguages() {
        List<LanguageModel> languages = new ArrayList<>();
        for (Map.Entry<String, LanguageModel> entry : SUPPORTED_LANGUAGES.entrySet()) {
            LanguageModel model = entry.getValue();
            // 更新安装状态
            model.isInstalled = isLanguageInstalled(model.languageCode);
            model.installedQuality = getInstalledQuality(model.languageCode);
            languages.add(model);
        }
        return languages;
    }
    
    /**
     * 获取已安装的语言列表
     * @return 已安装的语言代码列表
     */
    public List<String> getInstalledLanguages() {
        List<String> installed = new ArrayList<>();
        for (String languageCode : SUPPORTED_LANGUAGES.keySet()) {
            if (isLanguageInstalled(languageCode)) {
                installed.add(languageCode);
            }
        }
        return installed;
    }
    
    /**
     * 检查语言是否已安装
     * @param languageCode 语言代码
     * @return 是否已安装
     */
    public boolean isLanguageInstalled(String languageCode) {
        LanguageModel model = SUPPORTED_LANGUAGES.get(languageCode);
        if (model == null) return false;
        
        // 检查所有品质的模型是否存在
        for (ModelInfo info : model.qualityOptions.values()) {
            File modelDir = new File(mContext.getFilesDir(), info.modelPath);
            if (modelDir.exists() && modelDir.isDirectory()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取已安装的品质等级
     * @param languageCode 语言代码
     * @return 已安装的品质等级，未安装返回 null
     */
    public ModelQuality getInstalledQuality(String languageCode) {
        // 首先从 SharedPreferences 读取
        String savedQuality = mPrefs.getString(PREF_INSTALLED_QUALITY_PREFIX + languageCode, null);
        if (savedQuality != null) {
            ModelQuality quality = ModelQuality.fromCode(savedQuality);
            if (quality != null) {
                // 验证模型文件是否存在
                LanguageModel model = SUPPORTED_LANGUAGES.get(languageCode);
                if (model != null) {
                    ModelInfo info = model.getModelInfo(quality);
                    if (info != null) {
                        File modelDir = new File(mContext.getFilesDir(), info.modelPath);
                        if (modelDir.exists() && modelDir.isDirectory()) {
                            return quality;
                        }
                    }
                }
            }
        }
        
        // 如果 SharedPreferences 没有记录，检查文件系统
        LanguageModel model = SUPPORTED_LANGUAGES.get(languageCode);
        if (model == null) return null;
        
        // 按优先级检查：LARGE > STANDARD > SMALL
        ModelQuality[] priorities = {ModelQuality.LARGE, ModelQuality.STANDARD, ModelQuality.SMALL};
        for (ModelQuality quality : priorities) {
            ModelInfo info = model.getModelInfo(quality);
            if (info != null) {
                File modelDir = new File(mContext.getFilesDir(), info.modelPath);
                if (modelDir.exists() && modelDir.isDirectory()) {
                    // 保存到 SharedPreferences
                    mPrefs.edit().putString(PREF_INSTALLED_QUALITY_PREFIX + languageCode, quality.code).apply();
                    return quality;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 获取语言模型信息
     * @param languageCode 语言代码
     * @return 语言模型信息，不支持的语言返回 null
     */
    public LanguageModel getLanguageModel(String languageCode) {
        LanguageModel model = SUPPORTED_LANGUAGES.get(languageCode);
        if (model != null) {
            model.isInstalled = isLanguageInstalled(languageCode);
            model.installedQuality = getInstalledQuality(languageCode);
        }
        return model;
    }
    
    /**
     * 获取语言的模型路径（用于 VoskSpeechEngine）
     * @param languageCode 语言代码
     * @return 模型路径，未安装返回 null
     */
    public String getInstalledModelPath(String languageCode) {
        ModelQuality quality = getInstalledQuality(languageCode);
        if (quality == null) return null;
        
        LanguageModel model = SUPPORTED_LANGUAGES.get(languageCode);
        if (model == null) return null;
        
        ModelInfo info = model.getModelInfo(quality);
        return info != null ? info.modelPath : null;
    }
    
    public VoskModelManager(Context context) {
        mContext = context.getApplicationContext();
        mExecutor = Executors.newSingleThreadExecutor();
        mMainHandler = new Handler(Looper.getMainLooper());
        mPrefs = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * 检查模型是否已下载（兼容旧接口）
     */
    public boolean isModelDownloaded(String language) {
        // 首先检查新的语言配置
        if (isLanguageInstalled(language)) {
            return true;
        }
        // 兼容旧的检查方式
        return VoskSpeechEngine.isModelDownloaded(mContext, language);
    }
    
    /**
     * 下载模型（兼容旧接口，使用默认品质）
     */
    public void downloadModel(String language, DownloadCallback callback) {
        LanguageModel model = SUPPORTED_LANGUAGES.get(language);
        if (model != null) {
            // 使用新的品质选择下载
            ModelQuality defaultQuality = model.getDefaultQuality();
            if (defaultQuality != null) {
                downloadModel(language, defaultQuality, callback);
                return;
            }
        }
        
        // 兼容旧的下载方式
        mIsCancelled = false;
        
        String url = VoskSpeechEngine.getModelDownloadUrl(language);
        if (url == null) {
            notifyError(callback, "不支持的语言: " + language);
            return;
        }
        
        String modelPath = VoskSpeechEngine.getModelPath(language);
        if (modelPath == null) {
            notifyError(callback, "无法获取模型路径");
            return;
        }
        
        mExecutor.execute(() -> {
            try {
                downloadAndExtract(url, modelPath, null, null, callback);
            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
                notifyError(callback, "下载失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 下载指定品质的模型
     * @param languageCode 语言代码
     * @param quality 品质等级
     * @param callback 下载回调
     */
    public void downloadModel(String languageCode, ModelQuality quality, DownloadCallback callback) {
        mIsCancelled = false;
        
        LanguageModel model = SUPPORTED_LANGUAGES.get(languageCode);
        if (model == null) {
            notifyError(callback, "不支持的语言: " + languageCode);
            return;
        }
        
        ModelInfo info = model.getModelInfo(quality);
        if (info == null) {
            notifyError(callback, "该语言不支持此品质: " + quality.displayName);
            return;
        }
        
        mExecutor.execute(() -> {
            try {
                // 检查是否需要替换现有模型
                ModelQuality existingQuality = getInstalledQuality(languageCode);
                
                // 下载新模型
                downloadAndExtract(info.downloadUrl, info.modelPath, languageCode, quality, callback);
                
                if (!mIsCancelled && existingQuality != null && existingQuality != quality) {
                    // 下载成功后，删除旧的低品质模型
                    deleteOtherQualityModels(languageCode, quality);
                }
            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
                notifyError(callback, "下载失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 删除其他品质的模型（保留指定品质）
     */
    private void deleteOtherQualityModels(String languageCode, ModelQuality keepQuality) {
        LanguageModel model = SUPPORTED_LANGUAGES.get(languageCode);
        if (model == null) return;
        
        for (Map.Entry<ModelQuality, ModelInfo> entry : model.qualityOptions.entrySet()) {
            if (entry.getKey() != keepQuality) {
                File modelDir = new File(mContext.getFilesDir(), entry.getValue().modelPath);
                if (modelDir.exists()) {
                    Log.d(TAG, "Deleting old quality model: " + entry.getValue().modelPath);
                    deleteRecursive(modelDir);
                }
            }
        }
    }
    
    /**
     * 取消下载
     */
    public void cancelDownload() {
        mIsCancelled = true;
    }
    
    /**
     * 删除模型（兼容旧接口）
     */
    public boolean deleteModel(String language) {
        // 首先尝试使用新的删除方式
        if (deleteLanguageModel(language)) {
            return true;
        }
        
        // 兼容旧的删除方式
        String modelPath = VoskSpeechEngine.getModelPath(language);
        if (modelPath == null) return false;
        
        File modelDir = new File(mContext.getFilesDir(), modelPath);
        return deleteRecursive(modelDir);
    }
    
    /**
     * 删除语言模型（新接口）
     * @param languageCode 语言代码
     * @return 是否删除成功
     */
    public boolean deleteLanguageModel(String languageCode) {
        LanguageModel model = SUPPORTED_LANGUAGES.get(languageCode);
        if (model == null) return false;
        
        boolean deleted = false;
        
        // 删除所有品质的模型
        for (ModelInfo info : model.qualityOptions.values()) {
            File modelDir = new File(mContext.getFilesDir(), info.modelPath);
            if (modelDir.exists()) {
                if (deleteRecursive(modelDir)) {
                    deleted = true;
                }
            }
        }
        
        // 清除 SharedPreferences 中的记录
        if (deleted) {
            mPrefs.edit().remove(PREF_INSTALLED_QUALITY_PREFIX + languageCode).apply();
        }
        
        return deleted;
    }
    
    /**
     * 获取模型大小（兼容旧接口）
     */
    public long getModelSize(String language) {
        // 首先尝试使用新的方式获取
        long size = getLanguageModelSize(language);
        if (size > 0) return size;
        
        // 兼容旧的方式
        String modelPath = VoskSpeechEngine.getModelPath(language);
        if (modelPath == null) return 0;
        
        File modelDir = new File(mContext.getFilesDir(), modelPath);
        return getDirectorySize(modelDir);
    }
    
    /**
     * 获取语言模型大小（新接口）
     * @param languageCode 语言代码
     * @return 模型大小（字节），未安装返回 0
     */
    public long getLanguageModelSize(String languageCode) {
        ModelQuality quality = getInstalledQuality(languageCode);
        if (quality == null) return 0;
        
        LanguageModel model = SUPPORTED_LANGUAGES.get(languageCode);
        if (model == null) return 0;
        
        ModelInfo info = model.getModelInfo(quality);
        if (info == null) return 0;
        
        File modelDir = new File(mContext.getFilesDir(), info.modelPath);
        return getDirectorySize(modelDir);
    }
    
    private void downloadAndExtract(String urlString, String modelPath, 
            String languageCode, ModelQuality quality, DownloadCallback callback) 
            throws IOException {
        
        notifyProgress(callback, 0, "正在连接服务器...");
        
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setRequestProperty("User-Agent", "OrangePlayer/1.0");
        
        try {
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error: " + responseCode);
            }
            
            long totalSize = connection.getContentLengthLong();
            if (totalSize <= 0) {
                totalSize = 50 * 1024 * 1024; // 估计 50MB
            }
            
            notifyProgress(callback, 5, "正在下载模型...");
            
            // 下载到临时文件
            File tempFile = new File(mContext.getCacheDir(), "vosk_model_temp.zip");
            downloadToFile(connection.getInputStream(), tempFile, totalSize, callback);
            
            if (mIsCancelled) {
                tempFile.delete();
                return;
            }
            
            notifyProgress(callback, 80, "正在解压模型...");
            
            // 解压
            File targetDir = mContext.getFilesDir();
            extractZip(tempFile, targetDir, modelPath, callback);
            
            // 删除临时文件
            tempFile.delete();
            
            // 保存已安装的品质信息
            if (languageCode != null && quality != null) {
                mPrefs.edit().putString(PREF_INSTALLED_QUALITY_PREFIX + languageCode, quality.code).apply();
                Log.d(TAG, "Saved installed quality: " + languageCode + " -> " + quality.code);
            }
            
            notifyProgress(callback, 100, "完成");
            notifySuccess(callback);
            
        } finally {
            connection.disconnect();
        }
    }
    
    private void downloadToFile(InputStream input, File outputFile, long totalSize, 
            DownloadCallback callback) throws IOException {
        
        BufferedInputStream bis = new BufferedInputStream(input);
        FileOutputStream fos = new FileOutputStream(outputFile);
        
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            long downloaded = 0;
            int bytesRead;
            int lastProgress = 5;
            
            while ((bytesRead = bis.read(buffer)) != -1) {
                if (mIsCancelled) {
                    break;
                }
                
                fos.write(buffer, 0, bytesRead);
                downloaded += bytesRead;
                
                // 更新进度 (5% - 80%)
                int progress = (int) (5 + (downloaded * 75 / totalSize));
                if (progress > lastProgress) {
                    lastProgress = progress;
                    int percent = (int) (downloaded * 100 / totalSize);
                    notifyProgress(callback, progress, "正在下载... " + percent + "%");
                }
            }
        } finally {
            fos.close();
            bis.close();
        }
    }
    
    private void extractZip(File zipFile, File targetDir, String modelPath, 
            DownloadCallback callback) throws IOException {
        
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(
                new java.io.FileInputStream(zipFile)));
        
        try {
            ZipEntry entry;
            byte[] buffer = new byte[BUFFER_SIZE];
            int fileCount = 0;
            
            while ((entry = zis.getNextEntry()) != null) {
                if (mIsCancelled) {
                    break;
                }
                
                String entryName = entry.getName();
                
                // 处理路径：zip 内可能有一层目录
                // 例如 vosk-model-small-cn-0.22/am/final.mdl
                // 需要提取到 vosk-model-small-cn/am/final.mdl
                int firstSlash = entryName.indexOf('/');
                if (firstSlash > 0) {
                    entryName = modelPath + entryName.substring(firstSlash);
                } else {
                    entryName = modelPath + "/" + entryName;
                }
                
                File outFile = new File(targetDir, entryName);
                
                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    // 确保父目录存在
                    outFile.getParentFile().mkdirs();
                    
                    FileOutputStream fos = new FileOutputStream(outFile);
                    try {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    } finally {
                        fos.close();
                    }
                    
                    fileCount++;
                    if (fileCount % 10 == 0) {
                        int progress = 80 + Math.min(fileCount / 5, 19);
                        notifyProgress(callback, progress, "正在解压... " + fileCount + " 个文件");
                    }
                }
                
                zis.closeEntry();
            }
        } finally {
            zis.close();
        }
    }
    
    private boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return file.delete();
    }
    
    private long getDirectorySize(File dir) {
        long size = 0;
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    size += getDirectorySize(file);
                }
            }
        } else {
            size = dir.length();
        }
        return size;
    }
    
    private void notifyProgress(DownloadCallback callback, int progress, String status) {
        if (callback != null && !mIsCancelled) {
            mMainHandler.post(() -> callback.onProgress(progress, status));
        }
    }
    
    private void notifySuccess(DownloadCallback callback) {
        if (callback != null && !mIsCancelled) {
            mMainHandler.post(callback::onSuccess);
        }
    }
    
    private void notifyError(DownloadCallback callback, String error) {
        if (callback != null) {
            mMainHandler.post(() -> callback.onError(error));
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        mIsCancelled = true;
        if (mExecutor != null) {
            mExecutor.shutdownNow();
        }
    }
}
