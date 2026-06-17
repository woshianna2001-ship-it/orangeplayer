package com.orange.playerlibrary.cache;

import android.content.Context;
import android.util.Log;

import com.danikula.videocache.HttpProxyCacheServer;
import com.shuyu.gsyvideoplayer.cache.ProxyCacheManager;
import tv.danmaku.ijk.media.player.IMediaPlayer;

import java.io.File;
import java.util.Map;

/**
 * 自定义缓存管理器 - 继承ProxyCacheManager
 * 支持指定外部存储目录，用户可见
 */
public class ExternalProxyCacheManager extends ProxyCacheManager {
    
    private static final String TAG = "ExternalProxyCacheManager";
    
    // 静态缓存目录，供CacheFactory创建实例时使用
    private static File sCacheDirectory;
    
    private HttpProxyCacheServer proxyCacheServer;
    
    /**
     * 静态设置缓存目录（在CacheFactory创建实例前调用）
     */
    public static void setCacheDirectory(File cacheDirectory) {
        sCacheDirectory = cacheDirectory;
        Log.d(TAG, "Static cache directory set: " + (cacheDirectory != null ? cacheDirectory.getAbsolutePath() : "null"));
    }
    
    /**
     * 获取缓存目录
     */
    private File getCacheDirectory(Context context) {
        if (sCacheDirectory != null) {
            return sCacheDirectory;
        }
        // 默认使用外部缓存目录
        File externalCacheDir = context.getExternalCacheDir();
        File videoCacheDir = new File(externalCacheDir, "video-cache");
        if (!videoCacheDir.exists()) {
            videoCacheDir.mkdirs();
        }
        return videoCacheDir;
    }
    
    /**
     * 获取或创建代理缓存服务器
     */
    private HttpProxyCacheServer getProxyCacheServer(Context context) {
        if (proxyCacheServer == null) {
            File cacheDir = getCacheDirectory(context);
            proxyCacheServer = new HttpProxyCacheServer.Builder(context.getApplicationContext())
                .cacheDirectory(cacheDir)
                .maxCacheSize(1024 * 1024 * 1024)  // 1GB
                .maxCacheFilesCount(50)
                .build();
            Log.d(TAG, "HttpProxyCacheServer created with cacheDir=" + cacheDir.getAbsolutePath());
        }
        return proxyCacheServer;
    }
    
    @Override
    public void doCacheLogic(Context context, IMediaPlayer mediaPlayer, String url, Map<String, String> header, File cachePath) {
        Log.d(TAG, "doCacheLogic: url=" + url);
        
        // M3U8/HLS流媒体不支持代理缓存（播放列表中的TS片段是相对路径）
        // 直接使用原始URL播放
        if (isHlsUrl(url)) {
            Log.d(TAG, "HLS/M3U8 URL detected, skip proxy cache: " + url);
            try {
                mediaPlayer.setDataSource(url);
            } catch (Exception e) {
                Log.e(TAG, "Failed to set HLS URL", e);
            }
            return;
        }
        
        HttpProxyCacheServer proxy = getProxyCacheServer(context);
        String proxyUrl = proxy.getProxyUrl(url);
        
        Log.d(TAG, "Original URL: " + url);
        Log.d(TAG, "Proxy URL: " + proxyUrl);
        Log.d(TAG, "Is cached: " + proxy.isCached(url));
        
        try {
            mediaPlayer.setDataSource(proxyUrl);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set proxy URL, falling back to original", e);
            try {
                mediaPlayer.setDataSource(url);
            } catch (Exception ex) {
                Log.e(TAG, "Failed to set original URL", ex);
            }
        }
    }
    
    /**
     * 检测是否为HLS/M3U8流媒体
     */
    private boolean isHlsUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.contains(".m3u8") || lower.contains("m3u8") || 
               lower.startsWith("hls://") || lower.contains("hls");
    }
    
    /**
     * 获取缓存目录路径
     */
    public static String getCacheDirectoryPath() {
        return sCacheDirectory != null ? sCacheDirectory.getAbsolutePath() : null;
    }
}
