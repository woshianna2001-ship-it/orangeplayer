package com.orange.playerlibrary;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

/**
 * 播放进度管理器
 * 用于保存和恢复视频播放进度
 * 
 * Requirements: 6.3 - THE OrangevideoView SHALL 支持记忆播放位置功能 (setKeepVideoPlaying)
 */
public class PlaybackProgressManager {

    private static final String TAG = "PlaybackProgressManager";
    
    /** SharedPreferences 名称 */
    private static final String PREF_NAME = "orange_player_progress";
    
    /** 进度前缀 */
    private static final String KEY_PREFIX_PROGRESS = "progress_";
    
    /** 时长前缀 */
    private static final String KEY_PREFIX_DURATION = "duration_";
    
    /** 最大保存数量 */
    private static final int MAX_SAVED_COUNT = 100;
    
    /** 单例实例 */
    private static volatile PlaybackProgressManager sInstance;
    
    /** SharedPreferences */
    private SharedPreferences mPreferences;
    
    /** 内存缓存 */
    private final Map<String, Long> mProgressCache = new HashMap<>();
    private final Map<String, Long> mDurationCache = new HashMap<>();

    /**
     * 获取单例实例
     * @param context 上下文
     * @return 实例
     */
    public static PlaybackProgressManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (PlaybackProgressManager.class) {
                if (sInstance == null) {
                    sInstance = new PlaybackProgressManager(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    private PlaybackProgressManager(Context context) {
        mPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 保存播放进度
     * @param url 视频地址
     * @param position 当前位置（毫秒）
     * @param duration 总时长（毫秒）
     */
    public void saveProgress(String url, long position, long duration) {
        if (url == null || url.isEmpty()) {
            return;
        }
        
        String key = generateKey(url);
        
        // 保存到内存缓存
        mProgressCache.put(key, position);
        mDurationCache.put(key, duration);
        
        // 保存到 SharedPreferences
        mPreferences.edit()
                .putLong(KEY_PREFIX_PROGRESS + key, position)
                .putLong(KEY_PREFIX_DURATION + key, duration)
                .apply();
    }

    /**
     * 获取保存的播放进度
     * @param url 视频地址
     * @return 播放位置（毫秒），如果没有保存则返回 0
     */
    public long getProgress(String url) {
        if (url == null || url.isEmpty()) {
            return 0;
        }
        
        String key = generateKey(url);
        
        // 先从内存缓存获取
        Long cached = mProgressCache.get(key);
        if (cached != null) {
            return cached;
        }
        
        // 从 SharedPreferences 获取
        return mPreferences.getLong(KEY_PREFIX_PROGRESS + key, 0);
    }

    /**
     * 获取保存的视频时长
     * @param url 视频地址
     * @return 视频时长（毫秒），如果没有保存则返回 0
     */
    public long getDuration(String url) {
        if (url == null || url.isEmpty()) {
            return 0;
        }
        
        String key = generateKey(url);
        
        // 先从内存缓存获取
        Long cached = mDurationCache.get(key);
        if (cached != null) {
            return cached;
        }
        
        // 从 SharedPreferences 获取
        return mPreferences.getLong(KEY_PREFIX_DURATION + key, 0);
    }

    /**
     * 获取播放进度百分比
     * @param url 视频地址
     * @return 进度百分比 (0-100)
     */
    public int getProgressPercent(String url) {
        long progress = getProgress(url);
        long duration = getDuration(url);
        
        if (duration <= 0) {
            return 0;
        }
        
        return (int) (progress * 100 / duration);
    }

    /**
     * 检查是否有保存的进度
     * @param url 视频地址
     * @return true 有保存的进度
     */
    public boolean hasProgress(String url) {
        return getProgress(url) > 0;
    }

    /**
     * 删除指定视频的进度
     * @param url 视频地址
     */
    public void removeProgress(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        
        String key = generateKey(url);
        
        // 从内存缓存移除
        mProgressCache.remove(key);
        mDurationCache.remove(key);
        
        // 从 SharedPreferences 移除
        mPreferences.edit()
                .remove(KEY_PREFIX_PROGRESS + key)
                .remove(KEY_PREFIX_DURATION + key)
                .apply();
    }

    /**
     * 清除所有保存的进度
     */
    public void clearAllProgress() {
        mProgressCache.clear();
        mDurationCache.clear();
        mPreferences.edit().clear().apply();
    }

    /**
     * 生成存储键
     * @param url 视频地址
     * @return 存储键
     */
    private String generateKey(String url) {
        // 使用 URL 的哈希值作为键，避免 URL 过长
        return String.valueOf(url.hashCode());
    }

    /**
     * 判断是否应该恢复播放
     * 只有当保存的进度大于 1 分钟，且距离结尾大于 1 分钟时才恢复
     * 并且优先级需要大于跳过片头功能（恢复时会覆盖跳过片头）
     * @param url 视频地址
     * @param context 上下文（用于获取全局设置）
     * @return true 应该恢复播放
     */
    public boolean shouldResumePlayback(String url, Context context) {
        // 首先检查全局记忆播放开关是否开启
        if (context != null && !PlayerSettingsManager.getInstance(context).isMemoryPlayEnabled()) {
            return false;
        }
        
        long progress = getProgress(url);
        long duration = getDuration(url);
        
        // 如果进度小于 1 分钟，不触发记忆播放
        if (progress < 60000) {
            return false;
        }
        
        // 如果距离结尾小于 1 分钟，不触发记忆播放
        if (duration > 0 && (duration - progress) < 60000) {
            return false;
        }
        
        return true;
    }

    /**
     * 获取恢复播放的位置
     * 如果不应该恢复，返回 0
     * @param url 视频地址
     * @param context 上下文
     * @return 恢复位置（毫秒）
     */
    public long getResumePosition(String url, Context context) {
        if (shouldResumePlayback(url, context)) {
            return getProgress(url);
        }
        return 0;
    }
}
