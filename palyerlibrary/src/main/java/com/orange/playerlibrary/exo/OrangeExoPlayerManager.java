package com.orange.playerlibrary.exo;

import android.content.Context;
import android.media.AudioManager;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Build;
import android.os.Message;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.video.PlaceholderSurface;

import com.shuyu.gsyvideoplayer.cache.ICacheManager;
import com.shuyu.gsyvideoplayer.model.GSYModel;
import com.shuyu.gsyvideoplayer.model.VideoOptionModel;
import com.shuyu.gsyvideoplayer.player.BasePlayerManager;

import java.util.List;

import tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;

/**
 * 自定义 ExoPlayer 管理器
 * 
 * 核心改进：使用 SurfaceControl.reparent() 实现无缝 Surface 切换
 * 解决横竖屏切换时 MediaCodec IllegalStateException 问题
 * 
 * 参考 GSY 官方 app 中的 GSYExoPlayerManager 实现
 */
public class OrangeExoPlayerManager extends BasePlayerManager {

    private static final String TAG = "OrangeExoPlayerManager";
    private static final String SURFACE_CONTROL_NAME = "OrangeExoSurface";

    private Context context;
    private IjkExo2MediaPlayer mediaPlayer;
    private Surface surface;
    private PlaceholderSurface dummySurface;

    // SurfaceControl 相关 (Android Q+)
    private SurfaceControl surfaceControl;
    private Surface videoSurface;
    
    // 是否强制使用 TextureView 模式（禁用 SurfaceControl）
    private static boolean sForceTextureViewMode = false;
    
    // 当前视频是否为直播流
    private boolean isLiveStream = false;

    private long lastTotalRxBytes = 0;
    private long lastTimeStamp = 0;
    
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
    
    /**
     * 检测 URL 是否为直播流
     * 直播流协议：RTSP, RTMP, HLS (m3u8), FLV, WebRTC
     */
    private boolean isLiveStreamUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        String lowerUrl = url.toLowerCase();
        
        // RTSP 协议
        if (lowerUrl.startsWith("rtsp://")) {
            return true;
        }
        
        // udp 协议
        if (lowerUrl.startsWith("udp://")) {
            return true;
        }
        // tcp 协议
        if (lowerUrl.startsWith("tcp://")) {
            return true;
        }
        
        // RTMP 协议
        if (lowerUrl.startsWith("rtmp://") || lowerUrl.startsWith("rtmps://")) {
            return true;
        }
        
        // HLS 直播流（m3u8）
        if (lowerUrl.contains(".m3u8")) {
            return true;
        }
        
        // FLV 直播流
        if (lowerUrl.contains(".flv")) {
            return true;
        }
        
        // WebRTC
        if (lowerUrl.startsWith("webrtc://")) {
            return true;
        }
        
        return false;
    }

    @Override
    public IMediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    @Override
    public void initVideoPlayer(Context context, Message msg, List<VideoOptionModel> optionModelList, ICacheManager cacheManager) {
        this.context = context.getApplicationContext();
        mediaPlayer = new IjkExo2MediaPlayer(context);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        
        // 创建 PlaceholderSurface
        if (dummySurface == null) {
            dummySurface = PlaceholderSurface.newInstanceV17(context, false);
        }
        
        GSYModel gsyModel = (GSYModel) msg.obj;
        
        // 检测是否为直播流
        isLiveStream = isLiveStreamUrl(gsyModel.getUrl());
        android.util.Log.d(TAG, "initVideoPlayer: url=" + gsyModel.getUrl() + ", isLiveStream=" + isLiveStream);
        android.util.Log.d(TAG, "initVideoPlayer: isCache=" + gsyModel.isCache() + ", cacheManager=" + cacheManager);
        
        try {
            mediaPlayer.setLooping(gsyModel.isLooping());
            mediaPlayer.setPreview(gsyModel.getMapHeadData() != null && gsyModel.getMapHeadData().size() > 0);
            
            if (gsyModel.isCache() && cacheManager != null) {
                cacheManager.doCacheLogic(context, mediaPlayer, gsyModel.getUrl(), gsyModel.getMapHeadData(), gsyModel.getCachePath());
            } else {
                mediaPlayer.setCache(gsyModel.isCache());
                mediaPlayer.setCacheDir(gsyModel.getCachePath());
                mediaPlayer.setOverrideExtension(gsyModel.getOverrideExtension());
                mediaPlayer.setDataSource(context, Uri.parse(gsyModel.getUrl()), gsyModel.getMapHeadData());
            }
            
            if (gsyModel.getSpeed() != 1 && gsyModel.getSpeed() > 0) {
                mediaPlayer.setSpeed(gsyModel.getSpeed(), 1);
            }
            
            // Android Q+ 使用 SurfaceControl 实现无缝切换
            // 关键修复：直播流强制使用 SurfaceControl，即使 OCR 开启也不例外
            // 原因：
            // 1. 直播流没有缓冲，Surface 销毁会导致连接中断
            // 2. OCR 功能会设置 sForceTextureViewMode=true，但这会影响直播流
            // 3. 直播流必须使用 SurfaceControl 才能在横竖屏切换时不崩溃
            // 4. 点播视频可以根据 sForceTextureViewMode 决定是否使用 SurfaceControl
            boolean shouldUseSurfaceControl = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
                                             (!sForceTextureViewMode || isLiveStream);  // 直播流强制启用
            
            if (shouldUseSurfaceControl) {
                surfaceControl = new SurfaceControl.Builder()
                    .setName(SURFACE_CONTROL_NAME)
                    .setBufferSize(0, 0)
                    .build();
                videoSurface = new Surface(surfaceControl);
                mediaPlayer.setSurface(videoSurface);
                android.util.Log.d(TAG, "initVideoPlayer: 使用 SurfaceControl 模式 (isLiveStream=" + isLiveStream + ")");
            } else {
                android.util.Log.d(TAG, "initVideoPlayer: 使用 TextureView 模式 (forceTextureView=" + sForceTextureViewMode + ", isLiveStream=" + isLiveStream + ")");
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "initVideoPlayer 异常: " + e.getMessage());
            e.printStackTrace();
        }
        initSuccess(gsyModel);
    }

    /**
     * 显示 Surface - 核心方法
     * 
     * Android Q+ 使用 SurfaceControl.reparent() 实现无缝切换
     * 低版本或 TextureView 模式使用传统方式
     * 
     * 关键修复：TextureView 模式下横竖屏切换时，先切换到 PlaceholderSurface
     * 避免 MediaCodec 渲染到已销毁的 Surface 导致崩溃
     * 
     * RTMP 直播流修复：在 Surface 切换时保持 Surface 引用，避免过早释放
     * 
     * m3u8 seek 修复：完全忽略 null Surface 调用，保持当前 Surface 继续渲染
     */
    @Override
    public void showDisplay(final Message msg) {
        if (mediaPlayer == null) {
            return;
        }
        
        if (msg.obj == null) {
            // Surface 为 null - 完全忽略这个调用
            // 
            // 问题分析：
            // 1. GSY 框架在某些情况下（如 seek）会调用 showDisplay(null)
            // 2. 之前的实现会切换到 PlaceholderSurface，导致画面变白/变绿
            // 3. 正确的做法是：完全忽略这个调用，让 MediaCodec 继续使用当前 Surface
            // 
            // 适用场景：
            // - m3u8 视频 seek 时
            // - 横竖屏切换时（SurfaceControl 模式会自动处理）
            // - 任何不应该中断渲染的场景
            android.util.Log.d(TAG, "showDisplay: Surface 为 null, 完全忽略此调用，保持当前 Surface");
            return;
        } else {
            // 检查是否是 SurfaceView (Android Q+ 且有 SurfaceControl 时使用 reparent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && surfaceControl != null && msg.obj instanceof SurfaceView) {
                reparent((SurfaceView) msg.obj);
            } else if (msg.obj instanceof Surface) {
                // TextureView 模式或低版本：直接设置 Surface
                Surface holder = (Surface) msg.obj;
                if (holder != null && holder.isValid()) {
                    // 保存旧的 surface 引用（直播流需要保持引用避免被释放）
                    Surface oldSurface = surface;
                    surface = holder;
                    
                    try {
                        mediaPlayer.setSurface(holder);
                        android.util.Log.d(TAG, "showDisplay: 直接设置 Surface (TextureView 模式)");
                        
                        // 成功设置新 Surface 后，才释放旧 Surface（仅点播视频）
                        // 直播流保持旧 Surface 引用，避免过早释放
                        if (!isLiveStream && oldSurface != null && oldSurface != holder) {
                            try {
                                oldSurface.release();
                                android.util.Log.d(TAG, "showDisplay: 已释放旧 Surface");
                            } catch (Exception ex) {
                                android.util.Log.w(TAG, "showDisplay: 释放旧 Surface 失败: " + ex.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "showDisplay: 设置 Surface 失败: " + e.getMessage());
                        // 回退到 PlaceholderSurface
                        if (dummySurface != null && dummySurface.isValid()) {
                            mediaPlayer.setSurface(dummySurface);
                        }
                        // 恢复旧 surface 引用
                        surface = oldSurface;
                    }
                } else {
                    android.util.Log.w(TAG, "showDisplay: Surface 无效, 使用 PlaceholderSurface");
                    if (dummySurface != null && dummySurface.isValid()) {
                        mediaPlayer.setSurface(dummySurface);
                    }
                }
            } else if (msg.obj instanceof SurfaceView) {
                // TextureView 模式下收到 SurfaceView，获取其 Surface
                SurfaceView sv = (SurfaceView) msg.obj;
                if (sv.getHolder() != null && sv.getHolder().getSurface() != null && sv.getHolder().getSurface().isValid()) {
                    Surface newSurface = sv.getHolder().getSurface();
                    Surface oldSurface = surface;
                    surface = newSurface;
                    
                    try {
                        mediaPlayer.setSurface(newSurface);
                        android.util.Log.d(TAG, "showDisplay: 从 SurfaceView 获取 Surface (TextureView 模式)");
                        
                        // 成功设置新 Surface 后，才释放旧 Surface（仅点播视频）
                        if (!isLiveStream && oldSurface != null && oldSurface != newSurface) {
                            try {
                                oldSurface.release();
                                android.util.Log.d(TAG, "showDisplay: 已释放旧 Surface");
                            } catch (Exception ex) {
                                android.util.Log.w(TAG, "showDisplay: 释放旧 Surface 失败: " + ex.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "showDisplay: 从 SurfaceView 设置 Surface 失败: " + e.getMessage());
                        if (dummySurface != null && dummySurface.isValid()) {
                            mediaPlayer.setSurface(dummySurface);
                        }
                        surface = oldSurface;
                    }
                } else {
                    android.util.Log.w(TAG, "showDisplay: SurfaceView 的 Surface 无效, 使用 PlaceholderSurface");
                    if (dummySurface != null && dummySurface.isValid()) {
                        mediaPlayer.setSurface(dummySurface);
                    }
                }
            }
        }
    }
    
    /**
     * 使用 SurfaceControl.reparent 切换 Surface (Android Q+)
     * 
     * 这是解决 ExoPlayer 横竖屏切换问题的关键方法
     * 通过 reparent 将视频画面从一个 SurfaceView 转移到另一个
     * 而不是重新设置 Surface，避免 MediaCodec 被释放
     * 
     * 参考 GSY 官方实现：使用 setBufferSize 而不是 setMatrix
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void reparent(@Nullable SurfaceView surfaceView) {
        if (surfaceControl == null) {
            // 回退到传统方式
            if (surfaceView != null && mediaPlayer != null) {
                android.util.Log.d(TAG, "reparent: surfaceControl 为空, 回退到传统 setSurface");
                mediaPlayer.setSurface(surfaceView.getHolder().getSurface());
            }
            return;
        }
        
        try {
            if (surfaceView == null) {
                // reparent 到空，隐藏视频
                new SurfaceControl.Transaction()
                    .reparent(surfaceControl, null)
                    .setBufferSize(surfaceControl, 0, 0)
                    .setVisibility(surfaceControl, false)
                    .apply();
                android.util.Log.d(TAG, "reparent: 隐藏视频");
            } else {
                // reparent 到新的 SurfaceView
                SurfaceControl newParentSurfaceControl = surfaceView.getSurfaceControl();
                int width = surfaceView.getWidth();
                int height = surfaceView.getHeight();
                
                android.util.Log.d(TAG, "reparent: SurfaceView size=" + width + "x" + height);
                
                // 关键修复：如果尺寸为 0 或太小，说明布局还没完成
                // 先用父容器的尺寸，或者延迟重新设置
                if (width <= 0 || height <= 0) {
                    // 尝试获取父容器尺寸
                    if (surfaceView.getParent() instanceof android.view.View) {
                        android.view.View parent = (android.view.View) surfaceView.getParent();
                        width = parent.getWidth();
                        height = parent.getHeight();
                        android.util.Log.d(TAG, "reparent: 使用父容器尺寸=" + width + "x" + height);
                    }
                }
                
                // 如果还是 0，使用屏幕尺寸作为临时值
                if (width <= 0 || height <= 0) {
                    android.util.DisplayMetrics dm = surfaceView.getContext().getResources().getDisplayMetrics();
                    width = dm.widthPixels;
                    height = dm.heightPixels;
                    android.util.Log.d(TAG, "reparent: 使用屏幕尺寸=" + width + "x" + height);
                }
                
                // GSY 官方实现：只用 setBufferSize，不用 setMatrix
                new SurfaceControl.Transaction()
                    .reparent(surfaceControl, newParentSurfaceControl)
                    .setBufferSize(surfaceControl, width, height)
                    .setVisibility(surfaceControl, true)
                    .apply();
                
                android.util.Log.d(TAG, "reparent: 成功");
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "reparent 异常: " + e.getMessage());
            e.printStackTrace();
            // 出错时回退到传统方式
            if (surfaceView != null && mediaPlayer != null) {
                try {
                    mediaPlayer.setSurface(surfaceView.getHolder().getSurface());
                } catch (Exception ex) {
                    android.util.Log.e(TAG, "回退 setSurface 也失败: " + ex.getMessage());
                }
            }
        }
    }
    
    /**
     * 提供给外部调用的 Surface 切换方法
     * 用于 OrangevideoView 中的 setDisplay 重写
     */
    public void setDisplayNew(Object holder) {
        Message msg = new Message();
        msg.obj = holder;
        showDisplay(msg);
    }
    
    /**
     * 更新 SurfaceControl 的尺寸和位置 (Android Q+)
     * 在布局完成后调用，确保全屏切换和比例切换时画面正确
     * 
     * @param surfaceView 当前的 SurfaceView
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void updateSurfaceControlSize(SurfaceView surfaceView) {
        if (surfaceControl == null || surfaceView == null) {
            return;
        }
        
        try {
            int width = surfaceView.getWidth();
            int height = surfaceView.getHeight();
            
            if (width > 0 && height > 0) {
                android.util.Log.d(TAG, "updateSurfaceControlSize: " + width + "x" + height);
                
                // 关键：重新 reparent 并设置正确的尺寸
                // 这样可以确保位置也被正确重置
                SurfaceControl parentSurfaceControl = surfaceView.getSurfaceControl();
                if (parentSurfaceControl != null) {
                    new SurfaceControl.Transaction()
                        .reparent(surfaceControl, parentSurfaceControl)
                        .setBufferSize(surfaceControl, width, height)
                        .setVisibility(surfaceControl, true)
                        .apply();
                } else {
                    // 如果获取不到父 SurfaceControl，只更新尺寸
                    new SurfaceControl.Transaction()
                        .setBufferSize(surfaceControl, width, height)
                        .apply();
                }
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "updateSurfaceControlSize 异常: " + e.getMessage());
        }
    }

    @Override
    public void setSpeed(final float speed, final boolean soundTouch) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.setSpeed(speed, 1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setNeedMute(final boolean needMute) {
        if (mediaPlayer != null) {
            if (needMute) {
                mediaPlayer.setVolume(0, 0);
            } else {
                mediaPlayer.setVolume(1, 1);
            }
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
        // 直播流不释放 surface 引用，避免 Surface.finalize() 崩溃
        // 点播视频可以安全释放
        if (surface != null && !isLiveStream) {
            try {
                surface.release();
                android.util.Log.d(TAG, "releaseSurface: 已释放 Surface");
            } catch (Exception e) {
                android.util.Log.w(TAG, "releaseSurface: 释放 Surface 失败: " + e.getMessage());
            }
        }
        surface = null;
    }

    @Override
    public void release() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.setSurface(null);
                mediaPlayer.release();
            } catch (Exception e) {
                android.util.Log.e(TAG, "release: 释放 mediaPlayer 失败: " + e.getMessage());
            }
            mediaPlayer = null;
        }
        if (dummySurface != null) {
            try {
                dummySurface.release();
            } catch (Exception e) {
                android.util.Log.w(TAG, "release: 释放 dummySurface 失败: " + e.getMessage());
            }
            dummySurface = null;
        }
        // 释放 SurfaceControl
        if (surfaceControl != null) {
            try {
                surfaceControl.release();
            } catch (Exception e) {
                android.util.Log.w(TAG, "release: 释放 surfaceControl 失败: " + e.getMessage());
            }
            surfaceControl = null;
        }
        if (videoSurface != null) {
            try {
                videoSurface.release();
            } catch (Exception e) {
                android.util.Log.w(TAG, "release: 释放 videoSurface 失败: " + e.getMessage());
            }
            videoSurface = null;
        }
        // 最后释放 surface
        if (surface != null) {
            try {
                surface.release();
                android.util.Log.d(TAG, "release: 已释放 surface");
            } catch (Exception e) {
                android.util.Log.w(TAG, "release: 释放 surface 失败: " + e.getMessage());
            }
            surface = null;
        }
        lastTotalRxBytes = 0;
        lastTimeStamp = 0;
        isLiveStream = false;
    }

    @Override
    public int getBufferedPercentage() {
        if (mediaPlayer != null) {
            return mediaPlayer.getBufferedPercentage();
        }
        return 0;
    }

    @Override
    public long getNetSpeed() {
        if (mediaPlayer != null) {
            return getNetSpeed(context);
        }
        return 0;
    }

    @Override
    public void setSpeedPlaying(float speed, boolean soundTouch) {
    }

    @Override
    public void start() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    @Override
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    @Override
    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    @Override
    public int getVideoWidth() {
        if (mediaPlayer != null) {
            return mediaPlayer.getVideoWidth();
        }
        return 0;
    }

    @Override
    public int getVideoHeight() {
        if (mediaPlayer != null) {
            return mediaPlayer.getVideoHeight();
        }
        return 0;
    }

    @Override
    public boolean isPlaying() {
        if (mediaPlayer != null) {
            return mediaPlayer.isPlaying();
        }
        return false;
    }

    @Override
    public void seekTo(long time) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(time);
        }
    }

    @Override
    public long getCurrentPosition() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public long getDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    @Override
    public int getVideoSarNum() {
        if (mediaPlayer != null) {
            return mediaPlayer.getVideoSarNum();
        }
        return 1;
    }

    @Override
    public int getVideoSarDen() {
        if (mediaPlayer != null) {
            return mediaPlayer.getVideoSarDen();
        }
        return 1;
    }

    @Override
    public boolean isSurfaceSupportLockCanvas() {
        return false;
    }

    /**
     * 设置 seek 的临近帧
     */
    public void setSeekParameter(@Nullable SeekParameters seekParameters) {
        if (mediaPlayer != null) {
            mediaPlayer.setSeekParameter(seekParameters);
        }
    }

    private long getNetSpeed(Context context) {
        if (context == null) {
            return 0;
        }
        long nowTotalRxBytes = TrafficStats.getUidRxBytes(context.getApplicationInfo().uid) == TrafficStats.UNSUPPORTED ? 0 : TrafficStats.getTotalRxBytes();
        long nowTimeStamp = System.currentTimeMillis();
        long calculationTime = (nowTimeStamp - lastTimeStamp);
        if (calculationTime == 0) {
            return 0;
        }
        // 返回字节/秒（不是 KB/s）
        long speed = ((nowTotalRxBytes - lastTotalRxBytes) * 1000 / calculationTime);
        lastTimeStamp = nowTimeStamp;
        lastTotalRxBytes = nowTotalRxBytes;
        return speed;
    }
    
    /**
     * 是否使用 SurfaceControl 模式
     */
    public boolean isUseSurfaceControl() {
        return surfaceControl != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }
}
