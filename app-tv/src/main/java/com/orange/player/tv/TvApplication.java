package com.orange.player.tv;

import android.app.Application;
import android.util.Log;

import com.orange.playerlibrary.OrangePlayerConfig;
import com.shuyu.gsyvideoplayer.cache.CacheFactory;
import com.shuyu.gsyvideoplayer.player.PlayerFactory;
import com.shuyu.gsyvideoplayer.utils.GSYVideoType;

import tv.danmaku.ijk.media.exo2.Exo2PlayerManager;

/**
 * TV 应用 Application
 */
public class TvApplication extends Application {
    
    private static final String TAG = "TvApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.d(TAG, "OrangePlayer TV Application started");
        
        // 强制启用 TV 模式
        OrangePlayerConfig.setTvMode(true);
        
        // 初始化播放器
        initPlayer();
    }
    
    /**
     * 初始化播放器配置
     */
    private void initPlayer() {
        // 使用 ExoPlayer 内核（推荐用于 TV）
        PlayerFactory.setPlayManager(Exo2PlayerManager.class);
        
        // 设置渲染模式为 SurfaceView（TV 推荐）
        GSYVideoType.setRenderType(GSYVideoType.SURFACE);
        
        // 启用硬件解码
        GSYVideoType.enableMediaCodec();
        
        // 禁用缓存（避免网络问题）
        CacheFactory.setCacheManager(null);
        
        Log.d(TAG, "Player initialized with ExoPlayer, TV mode enabled, cache disabled");
    }
}
