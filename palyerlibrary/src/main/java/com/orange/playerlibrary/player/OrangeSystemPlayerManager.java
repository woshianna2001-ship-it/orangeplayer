package com.orange.playerlibrary.player;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Build;
import android.os.Message;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.shuyu.gsyvideoplayer.cache.ICacheManager;
import com.shuyu.gsyvideoplayer.model.GSYModel;
import com.shuyu.gsyvideoplayer.model.VideoOptionModel;
import com.shuyu.gsyvideoplayer.player.BasePlayerManager;

import java.util.List;
import java.util.Map;

import tv.danmaku.ijk.media.player.AndroidMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;

/**
 * 自定义系统播放器管理器
 * 使用 SurfaceControl.reparent() 实现无缝 Surface 切换
 */
public class OrangeSystemPlayerManager extends BasePlayerManager {

    private static final String TAG = "OrangeSystemPlayerManager";
    private static final String SURFACE_CONTROL_NAME = "OrangeSystemSurface";

    private Context context;
    private AndroidMediaPlayer mediaPlayer;
    private Surface surface;
    private SurfaceControl surfaceControl;
    private Surface videoSurface;
    private long lastTotalRxBytes = 0;
    private long lastTimeStamp = 0;
    
    // 是否强制使用 TextureView 模式（禁用 SurfaceControl）
    private static boolean sForceTextureViewMode = false;
    
    /**
     * 设置是否强制使用 TextureView 模式
     * 用于 OCR 功能需要截取画面时
     */
    public static void setForceTextureViewMode(boolean force) {
        sForceTextureViewMode = force;
        android.util.Log.d(TAG, "setForceTextureViewMode: " + force);
    }
    
    /**
     * 是否强制使用 TextureView 模式
     */
    public static boolean isForceTextureViewMode() {
        return sForceTextureViewMode;
    }

    @Override
    public IMediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }


    @Override
    public void initVideoPlayer(Context context, Message msg, List<VideoOptionModel> optionModelList, ICacheManager cacheManager) {
        this.context = context.getApplicationContext();
        mediaPlayer = new AndroidMediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        
        GSYModel gsyModel = (GSYModel) msg.obj;
        try {
            mediaPlayer.setLooping(gsyModel.isLooping());
            
            if (gsyModel.isCache() && cacheManager != null) {
                cacheManager.doCacheLogic(context, mediaPlayer, gsyModel.getUrl(), gsyModel.getMapHeadData(), gsyModel.getCachePath());
            } else {
                Map<String, String> headers = gsyModel.getMapHeadData();
                if (headers != null && !headers.isEmpty()) {
                    mediaPlayer.setDataSource(context, Uri.parse(gsyModel.getUrl()), headers);
                } else {
                    mediaPlayer.setDataSource(gsyModel.getUrl());
                }
            }
            
            // Android Q+ 使用 SurfaceControl 实现无缝切换（除非强制 TextureView 模式）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !sForceTextureViewMode) {
                surfaceControl = new SurfaceControl.Builder()
                    .setName(SURFACE_CONTROL_NAME)
                    .setBufferSize(0, 0)
                    .build();
                videoSurface = new Surface(surfaceControl);
                mediaPlayer.setSurface(videoSurface);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        initSuccess(gsyModel);
    }

    @Override
    public void showDisplay(final Message msg) {
        if (mediaPlayer == null) return;
        
        if (msg.obj == null) {
            // Surface 为 null - 完全忽略这个调用
            // 
            // 问题分析：
            // 1. GSY 框架在某些情况下（如 seek）会调用 showDisplay(null)
            // 2. 之前的实现会切换到 null 或调用 reparent(null)，导致画面消失
            // 3. 正确的做法是：完全忽略这个调用，让 MediaCodec 继续使用当前 Surface
            // 
            // 适用场景：
            // - m3u8 视频 seek 时
            // - 横竖屏切换时（SurfaceControl 模式会自动处理）
            // - 任何不应该中断渲染的场景
            android.util.Log.d(TAG, "showDisplay: Surface 为 null, 完全忽略此调用，保持当前 Surface");
            return;
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && surfaceControl != null && msg.obj instanceof SurfaceView) {
                reparent((SurfaceView) msg.obj);
            } else if (msg.obj instanceof Surface) {
                // TextureView 模式或低版本：直接设置 Surface
                Surface holder = (Surface) msg.obj;
                if (holder.isValid()) {
                    surface = holder;
                    try {
                        mediaPlayer.setSurface(surface);
                        android.util.Log.d(TAG, "showDisplay: 直接设置 Surface (TextureView 模式)");
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "showDisplay: 设置 Surface 失败: " + e.getMessage());
                    }
                } else {
                    android.util.Log.w(TAG, "showDisplay: Surface 无效");
                }
            } else if (msg.obj instanceof SurfaceView) {
                // TextureView 模式下收到 SurfaceView，获取其 Surface
                SurfaceView sv = (SurfaceView) msg.obj;
                if (sv.getHolder() != null && sv.getHolder().getSurface() != null && sv.getHolder().getSurface().isValid()) {
                    surface = sv.getHolder().getSurface();
                    try {
                        mediaPlayer.setSurface(surface);
                        android.util.Log.d(TAG, "showDisplay: 从 SurfaceView 获取 Surface (TextureView 模式)");
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "showDisplay: 从 SurfaceView 设置 Surface 失败: " + e.getMessage());
                    }
                } else {
                    android.util.Log.w(TAG, "showDisplay: SurfaceView 的 Surface 无效");
                }
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void reparent(@Nullable SurfaceView surfaceView) {
        if (surfaceControl == null) {
            if (surfaceView != null && mediaPlayer != null) {
                mediaPlayer.setSurface(surfaceView.getHolder().getSurface());
            }
            return;
        }
        
        try {
            if (surfaceView == null) {
                new SurfaceControl.Transaction()
                    .reparent(surfaceControl, null)
                    .setBufferSize(surfaceControl, 0, 0)
                    .setVisibility(surfaceControl, false)
                    .apply();
            } else {
                SurfaceControl newParentSurfaceControl = surfaceView.getSurfaceControl();
                int width = surfaceView.getWidth();
                int height = surfaceView.getHeight();
                
                if (width <= 0 || height <= 0) {
                    if (surfaceView.getParent() instanceof android.view.View) {
                        android.view.View parent = (android.view.View) surfaceView.getParent();
                        width = parent.getWidth();
                        height = parent.getHeight();
                    }
                }
                
                if (width <= 0 || height <= 0) {
                    android.util.DisplayMetrics dm = surfaceView.getContext().getResources().getDisplayMetrics();
                    width = dm.widthPixels;
                    height = dm.heightPixels;
                }
                
                new SurfaceControl.Transaction()
                    .reparent(surfaceControl, newParentSurfaceControl)
                    .setBufferSize(surfaceControl, width, height)
                    .setVisibility(surfaceControl, true)
                    .apply();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (surfaceView != null && mediaPlayer != null) {
                try {
                    mediaPlayer.setSurface(surfaceView.getHolder().getSurface());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
    
    public void setDisplayNew(Object holder) {
        Message msg = new Message();
        msg.obj = holder;
        showDisplay(msg);
    }
    
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void updateSurfaceControlSize(SurfaceView surfaceView) {
        if (surfaceControl == null || surfaceView == null) return;
        
        try {
            int width = surfaceView.getWidth();
            int height = surfaceView.getHeight();
            
            if (width > 0 && height > 0) {
                SurfaceControl parentSurfaceControl = surfaceView.getSurfaceControl();
                if (parentSurfaceControl != null) {
                    new SurfaceControl.Transaction()
                        .reparent(surfaceControl, parentSurfaceControl)
                        .setBufferSize(surfaceControl, width, height)
                        .setVisibility(surfaceControl, true)
                        .apply();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void setSpeed(final float speed, final boolean soundTouch) {
        if (mediaPlayer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                MediaPlayer internalPlayer = mediaPlayer.getInternalMediaPlayer();
                if (internalPlayer != null) {
                    internalPlayer.setPlaybackParams(internalPlayer.getPlaybackParams().setSpeed(speed));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setNeedMute(final boolean needMute) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(needMute ? 0 : 1, needMute ? 0 : 1);
        }
    }

    @Override
    public void setVolume(float left, float right) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(left, right);
        }
    }

    @Override
    public void releaseSurface() {
        surface = null;
    }

    @Override
    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.setSurface(null);
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (surfaceControl != null) {
            surfaceControl.release();
            surfaceControl = null;
        }
        if (videoSurface != null) {
            videoSurface.release();
            videoSurface = null;
        }
        lastTotalRxBytes = 0;
        lastTimeStamp = 0;
    }

    @Override
    public int getBufferedPercentage() {
        // AndroidMediaPlayer 没有 getBufferedPercentage 方法，返回 0
        return 0;
    }

    @Override
    public long getNetSpeed() {
        if (context == null) return 0;
        
        long nowTotalRxBytes = TrafficStats.getUidRxBytes(context.getApplicationInfo().uid);
        if (nowTotalRxBytes == TrafficStats.UNSUPPORTED) {
            nowTotalRxBytes = TrafficStats.getTotalRxBytes();
        }
        if (nowTotalRxBytes == TrafficStats.UNSUPPORTED) return 0;
        
        long nowTimeStamp = System.currentTimeMillis();
        long calculationTime = nowTimeStamp - lastTimeStamp;
        
        if (calculationTime <= 0) {
            lastTimeStamp = nowTimeStamp;
            lastTotalRxBytes = nowTotalRxBytes;
            return 0;
        }
        
        long speed = ((nowTotalRxBytes - lastTotalRxBytes) * 1000) / calculationTime;
        lastTimeStamp = nowTimeStamp;
        lastTotalRxBytes = nowTotalRxBytes;
        return Math.max(0, speed);
    }

    @Override
    public void setSpeedPlaying(float speed, boolean soundTouch) {
        setSpeed(speed, soundTouch);
    }

    @Override
    public void start() {
        if (mediaPlayer != null) mediaPlayer.start();
    }

    @Override
    public void stop() {
        if (mediaPlayer != null) mediaPlayer.stop();
    }

    @Override
    public void pause() {
        if (mediaPlayer != null) mediaPlayer.pause();
    }

    @Override
    public int getVideoWidth() {
        return mediaPlayer != null ? mediaPlayer.getVideoWidth() : 0;
    }

    @Override
    public int getVideoHeight() {
        return mediaPlayer != null ? mediaPlayer.getVideoHeight() : 0;
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    @Override
    public void seekTo(long time) {
        if (mediaPlayer != null) mediaPlayer.seekTo(time);
    }

    @Override
    public long getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    @Override
    public long getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    @Override
    public int getVideoSarNum() {
        return 1;
    }

    @Override
    public int getVideoSarDen() {
        return 1;
    }

    @Override
    public boolean isSurfaceSupportLockCanvas() {
        return false;
    }
    
    public boolean isUseSurfaceControl() {
        return surfaceControl != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }
}
