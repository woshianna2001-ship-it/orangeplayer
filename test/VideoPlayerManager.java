package com.orange.player;

import android.app.Activity;
import android.view.ViewGroup;
import com.orange.playerlibrary.OrangeVideoController;
import com.orange.playerlibrary.OrangevideoView;
import com.orange.playerlibrary.PlayerSettingsManager;
import com.orange.playerlibrary.PlayerConstants;
import com.orange.playerlibrary.DanmakuControllerImpl;
import com.uaoanlao.tv.Screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Color;
import android.view.View;

/**
 * 播放器管理器
 * 封装播放器的创建、播放控制、生命周期管理等核心操作
 * 简化上层调用，统一管理播放器状态
 * 
 * 适配 iApp v3 手机编程
 */
public class VideoPlayerManager {
    
    private static volatile VideoPlayerManager instance;
    
    private Activity mActivity;
    private OrangevideoView mVideoView;
    private OrangeVideoController mVideoController;
    private ViewGroup mParentContainer;
    private String urlimage = "";
    
    // 弹幕控制器
    private DanmakuControllerImpl mDanmakuController;
    
    private static String TAG = "VideoPlayerManager";
    
    /**
     * 单例模式获取管理器实例
     */
    public static VideoPlayerManager getInstance() {
        if (instance == null) {
            synchronized (VideoPlayerManager.class) {
                if (instance == null) {
                    instance = new VideoPlayerManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 初始化播放器
     * @param activity 关联的Activity
     * @param parent 播放器要添加到的父布局容器
     */
    public void init(Activity activity, ViewGroup parent) {
        if (activity == null || parent == null) {
            throw new IllegalArgumentException("Activity和父容器不能为空");
        }
        release();
        
        this.mActivity = activity;
        this.mParentContainer = parent;
        com.orange.playerlibrary.M3U8AdManager.getInstance(activity).setEnabled(true);//开启M3U8去广告功能
        // 设置下载路径到内部私有目录测试 (不可见目录，如 /data/data/com.orange.player/files/MyCustomDownload)
        java.io.File privateDir = new java.io.File(activity.getFilesDir(), "MyCustomDownload");
        com.orange.playerlibrary.download.SimpleDownloadManager.getInstance(activity).setDownloadPath(privateDir.getAbsolutePath());
        com.orange.playerlibrary.PlayerSettingsManager settingsManager = com.orange.playerlibrary.PlayerSettingsManager.getInstance(this);
        //boolean isEnabled = settingsManager.isDownloadEnabled();
        settingsManager.setDownloadEnabled(true);//开启下载功能
        PlayerSettingsManager.getInstance(activity).setMemoryPlayEnabled(true);//开启记忆播放功能
        // 2. 针对当前视频实例启用记忆播放状态
        mVideoView.setKeepVideoPlaying(true);
        // 设置默认内核（使用实例方法）
        PlayerSettingsManager.getInstance(activity).setPlayerEngine(PlayerConstants.ENGINE_IJK);
        // 创建播放器视图
        this.mVideoView = new OrangevideoView(activity);
        this.mVideoView.selectPlayerFactory(PlayerConstants.ENGINE_IJK);
        // 创建控制器（新版SDK只接受Context参数）
        this.mVideoController = new OrangeVideoController(activity);
        
        // 设置预览功能
        this.mVideoController.setPreViewEnabled(true);
        
        // 设置控制器到播放器
        this.mVideoView.setVideoController(this.mVideoController);
        
        // 将播放器添加到父容器
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        this.mParentContainer.addView(this.mVideoView, params);
        
        // 初始化控制器组件（默认非直播模式）
        this.mVideoController.addDefaultControlComponent("视频标题", false);
        
        // 初始化弹幕控制器
        initDanmakuController();
    }
    
    /**
     * 初始化弹幕控制器
     */
    private void initDanmakuController() {
        if (mVideoView != null && mVideoController != null) {
            mDanmakuController = new DanmakuControllerImpl(mActivity);
            mDanmakuController.attachToContainer(mVideoView);
            mVideoController.setDanmakuController(mDanmakuController);
        }
    }
    
    /**
     * 设置播放内核
     * @param engine 内核类型: "default", "ijk", "exo", "ali"
     */
    public void selectEngine(String engine) {
        if (mActivity != null) {
            PlayerSettingsManager.getInstance(mActivity).setPlayerEngine(engine);
        }
    }
    
    /**
     * 设置视频源
     * @param videoUrl 视频播放地址
     * @param videoTitle 视频标题
     */
    public void setVideoSource(String videoUrl, String videoTitle) {
        checkInitState();
        this.mVideoView.setUrl(videoUrl);
        this.mVideoController.setVideoTitle(videoTitle);
    }
    
    /**
     * 设置视频源（带请求头）
     * @param videoUrl 视频播放地址
     * @param head 请求头
     * @param videoTitle 视频标题
     */
    public void setVideoSource(String videoUrl, Map<String, String> head, String videoTitle) {
        checkInitState();
        this.mVideoView.setUrl(videoUrl, head);
        this.mVideoController.setVideoTitle(videoTitle);
    }
    
    /**
     * 设置播放列表
     * @param videoList 视频列表
     */
    public void setVideoList(ArrayList<HashMap<String, Object>> videoList) {
        checkInitState();
        this.mVideoController.setVideoList(videoList);
    }
    
    /**
     * 设置视频封面
     */
    public void setVideoImage(String url) {
        this.urlimage = url;
    }
    
    /**
     * 开始播放
     */
    public void start() {
        checkInitState();
        clearDanmakus();
        if (!this.mVideoView.isPlaying()) {
            this.mVideoView.start();
        }
    }
    
    /**
     * 嗅探播放
     */
    public void startSniffing() {
        checkInitState();
        this.mVideoView.startSniffing();
    }
    
    /**
     * 设置加载动画 (1-28)
     */
    public void setLoading(int i) {
        if (this.mVideoController != null) {
            this.mVideoController.setLoading(i);
        }
    }
    
    /**
     * 暂停播放
     */
    public void pause() {
        checkInitState();
        if (this.mVideoView.isPlaying()) {
            this.mVideoView.pause();
        }
    }
    
    /**
     * 停止播放
     */
    public void stop() {
        checkInitState();
        clearDanmakus();
        this.mVideoView.pause();
        this.mVideoView.release();
    }
    
    /**
     * 跳转进度
     * @param position 目标进度（毫秒）
     */
    public void seekTo(int position) {
        checkInitState();
        if (position >= 0 && position <= this.mVideoView.getDuration()) {
            this.mVideoView.seekTo(position);
        }
    }
    
    /**
     * 设置播放速度
     * @param speed 播放速度
     */
    public void setPlaybackSpeed(float speed) {
        checkInitState();
        if (speed > 0) {
            this.mVideoView.setSpeed(speed);
            OrangevideoView.setSpeeds(speed);
        }
    }
    
    /**
     * 切换全屏/非全屏
     */
    public void toggleFullScreen() {
        checkInitState();
        if (this.mVideoView.isFullScreen()) {
            this.mVideoController.stopFullScreen();
        } else {
            this.mVideoController.startFullScreen();
        }
    }
    
    /**
     * 是否全屏
     */
    public boolean isFullScreen() {
        checkInitState();
        return this.mVideoView.isFullScreen();
    }
    
    /**
     * 设置音量 (0-100)
     */
    public void setVolume(int volume) {
        checkInitState();
        if (volume >= 0 && volume <= 100) {
            // 使用SDK提供的播放器音量API
            this.mVideoView.setPlayerVolumePercent(volume);
        }
    }
    
    /**
     * 获取当前播放进度
     */
    public long getCurrentPosition() {
        checkInitState();
        return this.mVideoView.getCurrentPosition();
    }
    
    /**
     * 获取视频总时长
     */
    public long getDuration() {
        checkInitState();
        return this.mVideoView.getDuration();
    }
    
    /**
     * 设置播放完成监听
     */
    public void setOnPlayCompleteListener(com.orange.playerlibrary.interfaces.OnPlayCompleteListener listener) {
        checkInitState();
        this.mVideoView.setOnPlayCompleteListener(listener);
    }
    
    /**
     * 设置进度更新监听
     */
    public void setOnProgressListener(com.orange.playerlibrary.interfaces.OnProgressListener listener) {
        checkInitState();
        this.mVideoView.setOnProgressListener(listener);
    }
    
    /**
     * 处理Activity onPause
     */
    public void onPause() {
        if (this.mVideoView != null && this.mVideoView.isPlaying()) {
            this.mVideoView.onVideoPause();
        }
    }
    
    /**
     * 处理Activity onResume
     */
    public void onVideoResume() {
        if (this.mVideoView != null) {
            this.mVideoView.resume();
        }
    }
    
    /**
     * 释放播放器资源
     */
    public void release() {
        // 释放弹幕控制器
        if (mDanmakuController != null) {
            mDanmakuController.releaseDanmaku();
            mDanmakuController = null;
        }
        
        if (this.mVideoView != null) {
            this.mVideoView.release();
            this.mVideoView = null;
        }
        if (this.mVideoController != null) {
            this.mVideoController = null;
        }
        if (this.mParentContainer != null) {
            this.mParentContainer.removeAllViews();
            this.mParentContainer = null;
        }
        this.mActivity = null;
    }
    
    /**
     * 检查是否已初始化
     */
    private void checkInitState() {
        if (this.mVideoView == null || this.mVideoController == null) {
            throw new IllegalStateException("播放器未初始化，请先调用init()方法");
        }
    }
    
    /**
     * 获取播放器视图
     */
    public OrangevideoView getVideoView() {
        return this.mVideoView;
    }
    
    /**
     * 获取控制器
     */
    public OrangeVideoController getVideoController() {
        return this.mVideoController;
    }
    
    /**
     * 投屏点击回调
     */
    public void ScreenTvOnClickListener() {
        new Screen().setStaerActivity(mActivity)
            .setName(this.mVideoController.getVideoTitle())
            .setUrl(this.mVideoView.getUrl())
            .setImageUrl(this.urlimage)
            .show();
    }
    
    /**
     * 添加单个视频
     * @param tile 标题
     * @param url 地址
     * @param is 是否独立视频
     */
    public synchronized void addVideo(String tile, String url, boolean is) {
        if (mVideoController != null) {
            mVideoController.addVideo(tile, url, is);
        }
    }
    
    /**
     * 从JSON对象添加视频
     */
    public void addVideoFromJson(String jsonObjStr) {
        try {
            JSONObject jsonObj = new JSONObject(jsonObjStr);
            String name = jsonObj.getString("name");
            String url = jsonObj.getString("url");
            boolean isSpecial = jsonObj.getBoolean("isSpecial");
            addVideo(name, url, isSpecial);
        } catch (JSONException e) {
            e.printStackTrace();
            debug("解析JSON失败：" + e.getMessage());
        }
    }
    
    /**
     * 从JSON数组批量添加视频
     */
    public void addVideosFromJsonArray(String jsonArrayStr) {
        try {
            JSONArray jsonArray = new JSONArray(jsonArrayStr);
            for (int i = 0; i < jsonArray.length(); i++) {
                addVideoFromJson(jsonArray.getJSONObject(i).toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            debug("解析JSON数组失败：" + e.getMessage());
        }
    }
    
    public void addVideosFromJsonArray(JSONArray jsonArray) {
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                addVideoFromJson(jsonArray.getJSONObject(i).toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            debug("解析JSON数组失败：" + e.getMessage());
        }
    }
    
    /**
     * 解析颜色字符串
     */
    private int parseColorString(String colorStr) {
        try {
            if (colorStr.length() == 4) {
                StringBuilder sb = new StringBuilder("#");
                for (int i = 1; i < 4; i++) {
                    char c = colorStr.charAt(i);
                    sb.append(c).append(c);
                }
                colorStr = sb.toString();
            } else if (colorStr.length() == 5) {
                StringBuilder sb = new StringBuilder("#");
                for (int i = 1; i < 5; i++) {
                    char c = colorStr.charAt(i);
                    sb.append(c).append(c);
                }
                colorStr = sb.toString();
            }
            return Color.parseColor(colorStr);
        } catch (Exception e) {
            e.printStackTrace();
            return Color.WHITE;
        }
    }
    
    /**
     * 从JSON设置弹幕列表
     */
    public void setDanmuListFromJson(String jsonStr) {
        if (mDanmakuController == null) {
            debug("弹幕控制器未初始化");
            return;
        }
        
        try {
            JSONArray jsonArray = new JSONArray(jsonStr);
            List<com.orange.playerlibrary.interfaces.IDanmakuController.DanmakuItem> danmakuList = new ArrayList<>();
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject danmakuObj = jsonArray.getJSONObject(i);
                String text = danmakuObj.getString("text");
                String colorStr = danmakuObj.getString("color");
                long timestamp = danmakuObj.getLong("timestamp");
                boolean isSelf = danmakuObj.getBoolean("isSelf");
                int color = parseColorString(colorStr);
                danmakuList.add(new com.orange.playerlibrary.interfaces.IDanmakuController.DanmakuItem(text, color, timestamp, isSelf));
            }
            
            mDanmakuController.setDanmakuData(danmakuList);
        } catch (JSONException e) {
            e.printStackTrace();
            debug("设置弹幕出错:" + e);
        }
    }
    
    public boolean setDanmuListFromJson(JSONArray jsonArray) {
        if (mDanmakuController == null) {
            debug("弹幕控制器未初始化");
            return false;
        }
        
        try {
            List<com.orange.playerlibrary.interfaces.IDanmakuController.DanmakuItem> danmakuList = new ArrayList<>();
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject danmakuObj = jsonArray.getJSONObject(i);
                String text = danmakuObj.getString("text");
                String colorStr = danmakuObj.getString("color");
                long timestamp = danmakuObj.getLong("timestamp");
                boolean isSelf = danmakuObj.getBoolean("isSelf");
                int color = parseColorString(colorStr);
                danmakuList.add(new com.orange.playerlibrary.interfaces.IDanmakuController.DanmakuItem(text, color, timestamp, isSelf));
            }
            
            mDanmakuController.setDanmakuData(danmakuList);
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            debug("设置弹幕出错:" + e);
            return false;
        }
    }
    
    /**
     * 发送弹幕
     */
    public void sendDanmu(String danmu, int color, boolean issf) {
        if (mDanmakuController != null) {
            mDanmakuController.sendDanmaku(danmu, color);
        }
    }
    
    /**
     * 发送弹幕（字符串颜色）
     */
    public void sendDanmu(String danmu, String colorStr, boolean issf) {
        int color = parseColorString(colorStr);
        sendDanmu(danmu, color, issf);
    }
    
    /**
     * 清空弹幕
     */
    public void clearDanmakus() {
        if (mDanmakuController != null) {
            mDanmakuController.clearDanmakus();
        }
    }
    
    /**
     * 调试日志
     */
    public static void debug(Object obj) {
        android.util.Log.d(TAG, String.valueOf(obj));
    }
}