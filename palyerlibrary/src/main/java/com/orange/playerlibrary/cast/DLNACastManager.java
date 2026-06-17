package com.orange.playerlibrary.cast;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

/**
 * DLNA 投屏管理器
 * 封装 UaoanDLNA 投屏功能
 * 
 * 使用前需要在 app 模块添加依赖:
 * implementation 'com.github.uaoan:UaoanDLNA:1.0.1'
 * implementation 'com.squareup.okhttp3:okhttp:4.9.0'
 * implementation 'com.squareup.okio:okio:2.10.0'
 * implementation 'org.jetbrains.kotlin:kotlin-stdlib:1.5.30'
 */
public class DLNACastManager {
    
    private static final String TAG = "DLNACastManager";
    
    private static volatile DLNACastManager sInstance;
    private Context mContext;
    private boolean mIsCasting = false;
    
    // 投屏状态监听器
    private OnCastStateListener mCastStateListener;
    
    private DLNACastManager() {}
    
    public static DLNACastManager getInstance() {
        if (sInstance == null) {
            synchronized (DLNACastManager.class) {
                if (sInstance == null) {
                    sInstance = new DLNACastManager();
                }
            }
        }
        return sInstance;
    }
    
    /**
     * 检查 DLNA 投屏库是否可用 (UaoanDLNA)
     */
    public static boolean isDLNAAvailable() {
        try {
            Class.forName("com.uaoanlao.tv.Screen");
            Log.d(TAG, "DLNA library found: com.uaoanlao.tv.Screen");
            return true;
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "DLNA library not found");
            return false;
        }
    }
    
    /**
     * 获取 Screen 类
     */
    private static Class<?> getScreenClass() {
        try {
            return Class.forName("com.uaoanlao.tv.Screen");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
    
    /**
     * 初始化
     */
    public void init(Context context) {
        mContext = context.getApplicationContext();
    }
    
    /**
     * 开始投屏 - 使用 UaoanDLNA 的 Screen 类
     * 会打开设备选择界面
     */
    public void startCast(Activity activity, String videoUrl, String title, String imageUrl) {
        Class<?> screenClass = getScreenClass();
        if (screenClass == null) {
            Log.w(TAG, "DLNA library not available");
            if (mCastStateListener != null) {
                mCastStateListener.onCastError("投屏库未导入");
            }
            return;
        }
        
        try {
            // 使用反射调用 Screen 类
            Object screen = screenClass.newInstance();
            
            // setStaerActivity
            screenClass.getMethod("setStaerActivity", Activity.class).invoke(screen, activity);
            
            // setName
            if (title != null && !title.isEmpty()) {
                screenClass.getMethod("setName", String.class).invoke(screen, title);
            }
            
            // setUrl
            screenClass.getMethod("setUrl", String.class).invoke(screen, videoUrl);
            
            // setImageUrl (可选)
            if (imageUrl != null && !imageUrl.isEmpty()) {
                screenClass.getMethod("setImageUrl", String.class).invoke(screen, imageUrl);
            }
            
            // show
            screenClass.getMethod("show").invoke(screen);
            
            mIsCasting = true;
            Log.d(TAG, "Starting cast: " + videoUrl);
            
            if (mCastStateListener != null) {
                mCastStateListener.onCastStarted();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start cast", e);
            if (mCastStateListener != null) {
                mCastStateListener.onCastError("投屏失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 简化版开始投屏
     */
    public void startCast(Activity activity, String videoUrl, String title) {
        startCast(activity, videoUrl, title, null);
    }
    
    public boolean isCasting() {
        return mIsCasting;
    }
    
    public void setIsCasting(boolean casting) {
        mIsCasting = casting;
    }
    
    public void setOnCastStateListener(OnCastStateListener listener) {
        mCastStateListener = listener;
    }
    
    /**
     * 投屏状态监听器
     */
    public interface OnCastStateListener {
        void onCastStarted();
        void onCastStopped();
        void onCastError(String message);
    }
}
