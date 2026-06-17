package com.orange.playerlibrary.ocr;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OCR 语言包管理器
 * 支持在线下载和管理 Tesseract 语言包
 * 同时支持扫描 assets/tessdata 中的预置语言包
 */
public class LanguagePackManager {
    
    private static final String TAG = "LanguagePackManager";
    private static final String TESSDATA_DIR = "tessdata";
    private static final String ASSETS_TESSDATA_DIR = "tessdata";
    
    // GitHub 下载地址（使用 tessdata_fast 版本，文件更小）
    private static final String DOWNLOAD_BASE_URL = 
        "https://github.com/tesseract-ocr/tessdata_fast/raw/main/";
    
    // 国内镜像地址（ghproxy 加速）
    private static final String CHINA_MIRROR_URL = 
        "https://ghproxy.com/https://github.com/tesseract-ocr/tessdata_fast/raw/main/";
    
    // 备用镜像地址
    private static final String MIRROR_URL_2 = 
        "https://mirror.ghproxy.com/https://github.com/tesseract-ocr/tessdata_fast/raw/main/";
    
    // jsdelivr CDN 镜像
    private static final String JSDELIVR_URL = 
        "https://cdn.jsdelivr.net/gh/tesseract-ocr/tessdata_fast@main/";
    
    private final Context mContext;
    private final ExecutorService mExecutor;
    private final Handler mMainHandler;
    
    // 语言代码到显示名称的映射
    private static final Map<String, String> LANG_NAME_MAP = new HashMap<>();
    static {
        LANG_NAME_MAP.put("chi_sim", "简体中文");
        LANG_NAME_MAP.put("chi_tra", "繁体中文");
        LANG_NAME_MAP.put("eng", "英语");
        LANG_NAME_MAP.put("jpn", "日语");
        LANG_NAME_MAP.put("kor", "韩语");
        LANG_NAME_MAP.put("fra", "法语");
        LANG_NAME_MAP.put("deu", "德语");
        LANG_NAME_MAP.put("spa", "西班牙语");
        LANG_NAME_MAP.put("rus", "俄语");
        LANG_NAME_MAP.put("ara", "阿拉伯语");
        LANG_NAME_MAP.put("tha", "泰语");
        LANG_NAME_MAP.put("vie", "越南语");
        LANG_NAME_MAP.put("por", "葡萄牙语");
        LANG_NAME_MAP.put("ita", "意大利语");
        LANG_NAME_MAP.put("nld", "荷兰语");
        LANG_NAME_MAP.put("pol", "波兰语");
        LANG_NAME_MAP.put("tur", "土耳其语");
        LANG_NAME_MAP.put("hin", "印地语");
        LANG_NAME_MAP.put("ind", "印尼语");
        LANG_NAME_MAP.put("msa", "马来语");
    }
    
    /**
     * 获取语言显示名称
     */
    public static String getLanguageDisplayName(String langCode) {
        String name = LANG_NAME_MAP.get(langCode);
        return name != null ? name : langCode;
    }
    
    /**
     * 获取语言名称映射表
     */
    public static Map<String, String> getLanguageNameMap() {
        return new HashMap<>(LANG_NAME_MAP);
    }
    
    /**
     * 语言包信息
     */
    public static class LanguagePack {
        public final String code;        // 语言代码，如 "eng", "chi_sim"
        public final String name;        // 显示名称
        public final String description; // 描述
        public final long estimatedSize; // 预估大小（字节）
        public boolean installed;        // 是否已安装
        
        public LanguagePack(String code, String name, String description, long estimatedSize) {
            this.code = code;
            this.name = name;
            this.description = description;
            this.estimatedSize = estimatedSize;
            this.installed = false;
        }
        
        public String getFileName() {
            return code + ".traineddata";
        }
        
        public String getSizeText() {
            if (estimatedSize < 1024) {
                return estimatedSize + " B";
            } else if (estimatedSize < 1024 * 1024) {
                return String.format("%.1f KB", estimatedSize / 1024.0);
            } else {
                return String.format("%.1f MB", estimatedSize / (1024.0 * 1024));
            }
        }
    }
    
    /**
     * 下载回调
     */
    public interface DownloadCallback {
        void onProgress(int progress, long downloaded, long total);
        void onSuccess();
        void onError(String error);
    }
    
    public LanguagePackManager(Context context) {
        mContext = context.getApplicationContext();
        mExecutor = Executors.newSingleThreadExecutor();
        mMainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 获取常用语言包列表
     */
    public List<LanguagePack> getAvailableLanguages() {
        List<LanguagePack> languages = new ArrayList<>();
        
        // 常用语言包（使用 tessdata_fast 版本的大小）
        languages.add(new LanguagePack("chi_sim", "简体中文", "识别简体中文字幕", 2_500_000));
        languages.add(new LanguagePack("chi_tra", "繁体中文", "识别繁体中文字幕", 2_400_000));
        languages.add(new LanguagePack("eng", "英语", "识别英文字幕", 4_100_000));
        languages.add(new LanguagePack("jpn", "日语", "识别日文字幕", 2_300_000));
        languages.add(new LanguagePack("kor", "韩语", "识别韩文字幕", 1_500_000));
        languages.add(new LanguagePack("fra", "法语", "识别法文字幕", 1_300_000));
        languages.add(new LanguagePack("deu", "德语", "识别德文字幕", 1_200_000));
        languages.add(new LanguagePack("spa", "西班牙语", "识别西班牙文字幕", 1_200_000));
        languages.add(new LanguagePack("rus", "俄语", "识别俄文字幕", 1_400_000));
        languages.add(new LanguagePack("ara", "阿拉伯语", "识别阿拉伯文字幕", 1_100_000));
        languages.add(new LanguagePack("tha", "泰语", "识别泰文字幕", 1_000_000));
        languages.add(new LanguagePack("vie", "越南语", "识别越南文字幕", 600_000));
        
        // 检查已安装状态
        for (LanguagePack pack : languages) {
            pack.installed = isLanguageInstalled(pack.code);
        }
        
        return languages;
    }
    
    /**
     * 检查语言包是否已安装（包括下载的和 assets 中的）
     */
    public boolean isLanguageInstalled(String languageCode) {
        // 检查下载目录
        File tessDataDir = new File(mContext.getFilesDir(), TESSDATA_DIR);
        File trainedDataFile = new File(tessDataDir, languageCode + ".traineddata");
        if (trainedDataFile.exists() && trainedDataFile.length() > 0) {
            return true;
        }
        
        // 检查 assets
        return isLanguageInAssets(languageCode);
    }
    
    /**
     * 获取语言包文件
     */
    public File getLanguageFile(String languageCode) {
        File tessDataDir = new File(mContext.getFilesDir(), TESSDATA_DIR);
        return new File(tessDataDir, languageCode + ".traineddata");
    }
    
    /**
     * 下载语言包
     */
    public void downloadLanguage(String languageCode, DownloadCallback callback) {
        mExecutor.execute(() -> {
            try {
                downloadLanguageInternal(languageCode, callback);
            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
                postError(callback, "下载失败: " + e.getMessage());
            }
        });
    }

    
    private void downloadLanguageInternal(String languageCode, DownloadCallback callback) {
        String fileName = languageCode + ".traineddata";
        
        // 确保目录存在
        File tessDataDir = new File(mContext.getFilesDir(), TESSDATA_DIR);
        if (!tessDataDir.exists()) {
            tessDataDir.mkdirs();
        }
        
        File targetFile = new File(tessDataDir, fileName);
        File tempFile = new File(tessDataDir, fileName + ".tmp");
        
        // 尝试多个下载源
        String[] urls = {
            CHINA_MIRROR_URL + fileName,      // 国内镜像优先
            JSDELIVR_URL + fileName,          // jsdelivr CDN
            MIRROR_URL_2 + fileName,          // 备用镜像
            DOWNLOAD_BASE_URL + fileName      // GitHub 原地址
        };
        
        Exception lastError = null;
        
        for (String downloadUrl : urls) {
            Log.d(TAG, "Trying download from: " + downloadUrl);
            
            HttpURLConnection connection = null;
            InputStream inputStream = null;
            FileOutputStream outputStream = null;
            
            try {
                URL url = new URL(downloadUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(30000);
                connection.setRequestProperty("User-Agent", "OrangePlayer/1.0");
                connection.setInstanceFollowRedirects(true);
                
                int responseCode = connection.getResponseCode();
                
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.w(TAG, "HTTP " + responseCode + " from " + downloadUrl);
                    connection.disconnect();
                    continue;
                }
                
                long totalSize = connection.getContentLength();
                Log.d(TAG, "File size: " + totalSize);
                
                inputStream = connection.getInputStream();
                outputStream = new FileOutputStream(tempFile);
                
                byte[] buffer = new byte[8192];
                long downloaded = 0;
                int bytesRead;
                int lastProgress = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    downloaded += bytesRead;
                    
                    if (totalSize > 0) {
                        int progress = (int) (downloaded * 100 / totalSize);
                        if (progress != lastProgress) {
                            lastProgress = progress;
                            final long finalDownloaded = downloaded;
                            final long finalTotal = totalSize;
                            postProgress(callback, progress, finalDownloaded, finalTotal);
                        }
                    }
                }
                
                outputStream.close();
                outputStream = null;
                
                // 重命名临时文件
                if (targetFile.exists()) {
                    targetFile.delete();
                }
                if (tempFile.renameTo(targetFile)) {
                    Log.d(TAG, "Download completed: " + targetFile.getAbsolutePath());
                    postSuccess(callback);
                    return; // 成功，退出
                } else {
                    throw new Exception("无法保存文件");
                }
                
            } catch (Exception e) {
                Log.w(TAG, "Download failed from " + downloadUrl + ": " + e.getMessage());
                lastError = e;
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            } finally {
                try {
                    if (outputStream != null) outputStream.close();
                    if (inputStream != null) inputStream.close();
                    if (connection != null) connection.disconnect();
                } catch (Exception ignored) {}
            }
        }
        
        // 所有源都失败
        String errorMsg = lastError != null ? lastError.getMessage() : "下载失败";
        postError(callback, errorMsg + "\n如无法下载，请尝试使用代理");
    }
    
    /**
     * 删除语言包
     */
    public boolean deleteLanguage(String languageCode) {
        File file = getLanguageFile(languageCode);
        if (file.exists()) {
            boolean deleted = file.delete();
            Log.d(TAG, "Delete " + languageCode + ": " + deleted);
            return deleted;
        }
        return true;
    }
    
    /**
     * 获取已安装的语言包大小
     */
    public long getInstalledSize() {
        File tessDataDir = new File(mContext.getFilesDir(), TESSDATA_DIR);
        if (!tessDataDir.exists()) {
            return 0;
        }
        
        long totalSize = 0;
        File[] files = tessDataDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".traineddata")) {
                    totalSize += file.length();
                }
            }
        }
        return totalSize;
    }
    
    /**
     * 获取已安装的语言列表（包括 assets 和下载的）
     */
    public List<String> getInstalledLanguages() {
        Set<String> installed = new HashSet<>();
        
        // 1. 扫描下载目录
        File tessDataDir = new File(mContext.getFilesDir(), TESSDATA_DIR);
        if (tessDataDir.exists()) {
            File[] files = tessDataDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String name = file.getName();
                    if (name.endsWith(".traineddata")) {
                        installed.add(name.replace(".traineddata", ""));
                    }
                }
            }
        }
        
        // 2. 扫描 assets/tessdata 目录
        try {
            AssetManager assetManager = mContext.getAssets();
            String[] assetFiles = assetManager.list(ASSETS_TESSDATA_DIR);
            if (assetFiles != null) {
                for (String fileName : assetFiles) {
                    if (fileName.endsWith(".traineddata")) {
                        installed.add(fileName.replace(".traineddata", ""));
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to scan assets tessdata: " + e.getMessage());
        }
        
        return new ArrayList<>(installed);
    }
    
    /**
     * 检查语言包是否在 assets 中
     */
    public boolean isLanguageInAssets(String languageCode) {
        try {
            AssetManager assetManager = mContext.getAssets();
            String[] assetFiles = assetManager.list(ASSETS_TESSDATA_DIR);
            if (assetFiles != null) {
                String targetFile = languageCode + ".traineddata";
                for (String fileName : assetFiles) {
                    if (fileName.equals(targetFile)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to check assets: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * 从 assets 复制语言包到内部存储
     */
    public boolean copyLanguageFromAssets(String languageCode) {
        String fileName = languageCode + ".traineddata";
        File tessDataDir = new File(mContext.getFilesDir(), TESSDATA_DIR);
        if (!tessDataDir.exists()) {
            tessDataDir.mkdirs();
        }
        
        File targetFile = new File(tessDataDir, fileName);
        if (targetFile.exists()) {
            return true; // 已存在
        }
        
        try {
            AssetManager assetManager = mContext.getAssets();
            InputStream inputStream = assetManager.open(ASSETS_TESSDATA_DIR + "/" + fileName);
            FileOutputStream outputStream = new FileOutputStream(targetFile);
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            outputStream.close();
            inputStream.close();
            
            Log.d(TAG, "Copied from assets: " + fileName);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy from assets: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取语言包文件路径（优先使用下载的，其次使用 assets）
     * 如果语言包在 assets 中，会自动复制到内部存储
     */
    public File getLanguageFileWithAssetsFallback(String languageCode) {
        File tessDataDir = new File(mContext.getFilesDir(), TESSDATA_DIR);
        File downloadedFile = new File(tessDataDir, languageCode + ".traineddata");
        
        // 优先使用下载的文件
        if (downloadedFile.exists() && downloadedFile.length() > 0) {
            return downloadedFile;
        }
        
        // 尝试从 assets 复制
        if (isLanguageInAssets(languageCode)) {
            if (copyLanguageFromAssets(languageCode)) {
                return downloadedFile;
            }
        }
        
        return null;
    }
    
    private void postProgress(DownloadCallback callback, int progress, long downloaded, long total) {
        if (callback != null) {
            mMainHandler.post(() -> callback.onProgress(progress, downloaded, total));
        }
    }
    
    private void postSuccess(DownloadCallback callback) {
        if (callback != null) {
            mMainHandler.post(callback::onSuccess);
        }
    }
    
    private void postError(DownloadCallback callback, String error) {
        if (callback != null) {
            mMainHandler.post(() -> callback.onError(error));
        }
    }
    
    public void shutdown() {
        mExecutor.shutdown();
    }
}
