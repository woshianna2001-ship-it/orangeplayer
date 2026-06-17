package com.orange.playerlibrary.player;

import android.content.Context;
import android.os.Message;

import com.shuyu.gsyvideoplayer.cache.ICacheManager;
import com.shuyu.gsyvideoplayer.model.VideoOptionModel;
import com.shuyu.gsyvideoplayer.player.IjkPlayerManager;

import java.util.List;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * 自定义 IJK 播放器管理器
 * 主要解决本地文件播放的协议白名单问题
 */
public class OrangeIjkPlayerManager extends IjkPlayerManager {
    
    private static final String TAG = "OrangeIjkPlayerManager";
    
    @Override
    public void initVideoPlayer(Context context, Message msg, List<VideoOptionModel> optionModelList, ICacheManager cacheManager) {
        // 先调用父类方法创建播放器实例
        super.initVideoPlayer(context, msg, optionModelList, cacheManager);
        
        // 获取父类创建的播放器实例
        IjkMediaPlayer ijkMediaPlayer = (IjkMediaPlayer) getMediaPlayer();
        
        if (ijkMediaPlayer != null) {
            // 解决 IJK 播放器在 Android 10+ 播放 /data/user/0/... 内部存储文件报错 -10000 的问题
            // 禁用 IJKPlayer 内部某些对 file 协议的安全校验或缓存机制
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "safe", 0);
            
            // 设置协议白名单，添加 file 协议支持本地文件播放
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "protocol_whitelist", 
                "file,http,https,tls,rtp,tcp,udp,crypto,httpproxy,concat,subfile,data");
        }
    }
}
