package com.orange.playerlibrary.utils;

import android.util.Log;

import com.orange.playerlibrary.PlayerConstants;

/**
 * 播放器内核智能选择器
 * 
 * 根据视频 URL 协议自动选择最合适的播放器内核
 * 
 * 使用示例：
 * <pre>
 * String engine = PlayerEngineSelector.selectEngine(url);
 * videoView.selectPlayerFactory(engine);
 * videoView.setUp(url, true, title);
 * videoView.startPlayLogic();
 * </pre>
 * 
 * @author OrangePlayer
 */
public class PlayerEngineSelector {
    
    private static final String TAG = "PlayerEngineSelector";
    
    /**
     * 根据 URL 自动选择最合适的播放器内核
     * 
     * 选择规则：
     * - RTSP 协议：优先 ExoPlayer，备选 IJK（阿里云不支持）
     * - HLS (m3u8)：优先阿里云，备选 ExoPlayer
     * - RTMP 协议：优先阿里云，备选 IJK
     * - FLV 协议：优先阿里云，备选 IJK
     * - HTTP/HTTPS：所有内核均可，优先 ExoPlayer
     * - 其他：默认 ExoPlayer
     * 
     * @param url 视频 URL
     * @return 推荐的播放器内核常量（String）
     */
    public static String selectEngine(String url) {
        return selectEngine(url, PlayerConstants.ENGINE_EXO);
    }
    
    /**
     * 根据 URL 自动选择最合适的播放器内核
     * 
     * @param url 视频 URL
     * @param defaultEngine 默认内核（当无法判断时使用）
     * @return 推荐的播放器内核常量（String）
     */
    public static String selectEngine(String url, String defaultEngine) {
        if (url == null || url.isEmpty()) {
            Log.w(TAG, "URL 为空，使用默认内核: " + getEngineName(defaultEngine));
            return defaultEngine;
        }
        
        String lowerUrl = url.toLowerCase();
        
        // RTSP 协议：ExoPlayer 或 IJK（阿里云不支持）
        if (lowerUrl.startsWith("rtsp://")) {
            Log.i(TAG, "检测到 RTSP 协议，推荐使用 ExoPlayer 内核");
            return PlayerConstants.ENGINE_EXO;
        }
        
        // UDP/TCP 协议：ExoPlayer 或 IJK
        if (lowerUrl.startsWith("udp://") || lowerUrl.startsWith("tcp://")) {
            Log.i(TAG, "检测到 UDP/TCP 协议，推荐使用 ExoPlayer 内核");
            return PlayerConstants.ENGINE_EXO;
        }
        
        // HLS 直播流：阿里云性能最好
        if (lowerUrl.contains(".m3u8")) {
            Log.i(TAG, "检测到 HLS (m3u8) 协议，推荐使用阿里云内核");
            return PlayerConstants.ENGINE_ALI;
        }
        
        // RTMP 协议：阿里云或 IJK
        if (lowerUrl.startsWith("rtmp://") || lowerUrl.startsWith("rtmps://")) {
            Log.i(TAG, "检测到 RTMP 协议，推荐使用阿里云内核");
            return PlayerConstants.ENGINE_ALI;
        }
        
        // FLV 直播流：阿里云或 IJK
        if (lowerUrl.contains(".flv")) {
            Log.i(TAG, "检测到 FLV 协议，推荐使用阿里云内核");
            return PlayerConstants.ENGINE_ALI;
        }
        
        // WebRTC：ExoPlayer
        if (lowerUrl.startsWith("webrtc://")) {
            Log.i(TAG, "检测到 WebRTC 协议，推荐使用 ExoPlayer 内核");
            return PlayerConstants.ENGINE_EXO;
        }
        
        // HTTP/HTTPS 点播视频：ExoPlayer 性能好
        if (lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://")) {
            Log.i(TAG, "检测到 HTTP/HTTPS 协议，推荐使用 ExoPlayer 内核");
            return PlayerConstants.ENGINE_EXO;
        }
        
        // 本地文件：ExoPlayer
        if (lowerUrl.startsWith("file://") || lowerUrl.startsWith("/")) {
            Log.i(TAG, "检测到本地文件，推荐使用 ExoPlayer 内核");
            return PlayerConstants.ENGINE_EXO;
        }
        
        // 无法判断，使用默认内核
        Log.w(TAG, "无法识别协议，使用默认内核: " + getEngineName(defaultEngine));
        return defaultEngine;
    }
    
    /**
     * 检查指定内核是否支持该 URL
     * 
     * @param url 视频 URL
     * @param engine 播放器内核（String）
     * @return true 表示支持，false 表示不支持
     */
    public static boolean isEngineSupported(String url, String engine) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        String lowerUrl = url.toLowerCase();
        
        if (PlayerConstants.ENGINE_EXO.equals(engine)) {
            // ExoPlayer 支持：RTSP, HLS, DASH, HTTP, 本地文件
            return lowerUrl.startsWith("rtsp://") ||
                   lowerUrl.startsWith("udp://") ||
                   lowerUrl.startsWith("tcp://") ||
                   lowerUrl.contains(".m3u8") ||
                   lowerUrl.contains(".mpd") ||
                   lowerUrl.startsWith("http://") ||
                   lowerUrl.startsWith("https://") ||
                   lowerUrl.startsWith("file://") ||
                   lowerUrl.startsWith("/");
        } else if (PlayerConstants.ENGINE_IJK.equals(engine)) {
            // IJK 支持几乎所有格式
            return true;
        } else if (PlayerConstants.ENGINE_ALI.equals(engine)) {
            // 阿里云不支持 RTSP
            return !lowerUrl.startsWith("rtsp://") &&
                   !lowerUrl.startsWith("udp://") &&
                   !lowerUrl.startsWith("tcp://");
        } else if (PlayerConstants.ENGINE_DEFAULT.equals(engine)) {
            // 系统播放器支持有限
            return lowerUrl.startsWith("http://") ||
                   lowerUrl.startsWith("https://") ||
                   lowerUrl.startsWith("file://") ||
                   lowerUrl.startsWith("/");
        } else {
            return false;
        }
    }
    
    /**
     * 获取内核名称
     */
    public static String getEngineName(String engine) {
        if (PlayerConstants.ENGINE_EXO.equals(engine)) {
            return "ExoPlayer";
        } else if (PlayerConstants.ENGINE_IJK.equals(engine)) {
            return "IJK";
        } else if (PlayerConstants.ENGINE_ALI.equals(engine)) {
            return "阿里云";
        } else if (PlayerConstants.ENGINE_DEFAULT.equals(engine)) {
            return "系统播放器";
        } else {
            return "未知";
        }
    }
    
    /**
     * 获取协议类型描述
     */
    public static String getProtocolType(String url) {
        if (url == null || url.isEmpty()) {
            return "未知";
        }
        
        String lowerUrl = url.toLowerCase();
        
        if (lowerUrl.startsWith("rtsp://")) {
            return "RTSP 直播";
        } else if (lowerUrl.startsWith("rtmp://") || lowerUrl.startsWith("rtmps://")) {
            return "RTMP 直播";
        } else if (lowerUrl.contains(".m3u8")) {
            return "HLS 直播";
        } else if (lowerUrl.contains(".flv")) {
            return "FLV 直播";
        } else if (lowerUrl.startsWith("udp://")) {
            return "UDP 流";
        } else if (lowerUrl.startsWith("tcp://")) {
            return "TCP 流";
        } else if (lowerUrl.startsWith("webrtc://")) {
            return "WebRTC";
        } else if (lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://")) {
            return "HTTP 点播";
        } else if (lowerUrl.startsWith("file://") || lowerUrl.startsWith("/")) {
            return "本地文件";
        } else {
            return "未知协议";
        }
    }
    
    /**
     * 打印内核支持情况
     */
    public static void printEngineSupportInfo(String url) {
        Log.i(TAG, "========== 播放器内核支持情况 ==========");
        Log.i(TAG, "URL: " + url);
        Log.i(TAG, "协议类型: " + getProtocolType(url));
        Log.i(TAG, "ExoPlayer: " + (isEngineSupported(url, PlayerConstants.ENGINE_EXO) ? "✅ 支持" : "❌ 不支持"));
        Log.i(TAG, "IJK: " + (isEngineSupported(url, PlayerConstants.ENGINE_IJK) ? "✅ 支持" : "❌ 不支持"));
        Log.i(TAG, "阿里云: " + (isEngineSupported(url, PlayerConstants.ENGINE_ALI) ? "✅ 支持" : "❌ 不支持"));
        Log.i(TAG, "系统播放器: " + (isEngineSupported(url, PlayerConstants.ENGINE_DEFAULT) ? "✅ 支持" : "❌ 不支持"));
        Log.i(TAG, "推荐内核: " + getEngineName(selectEngine(url)));
        Log.i(TAG, "======================================");
    }
}
