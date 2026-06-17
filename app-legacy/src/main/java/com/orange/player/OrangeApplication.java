package com.orange.player;

import androidx.multidex.MultiDexApplication;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 应用程序类
 * 负责全局初始化
 */
public class OrangeApplication extends MultiDexApplication {
    
    private static final String TAG = "OrangeApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 设置全局异常处理器
        setupCrashHandler();
        
        Log.d(TAG, "OrangePlayer Legacy 启动");
        Log.d(TAG, "Android 版本: " + android.os.Build.VERSION.RELEASE);
        Log.d(TAG, "API Level: " + android.os.Build.VERSION.SDK_INT);
        Log.d(TAG, "设备型号: " + android.os.Build.MODEL);
        Log.d(TAG, "设备厂商: " + android.os.Build.MANUFACTURER);
        
        // 初始化 VideoDownloader（用于 M3U8、MP4 等视频下载）
        try {
            com.orange.playerlibrary.download.VideoDownloaderWrapper downloaderWrapper =
                com.orange.playerlibrary.download.VideoDownloaderWrapper.getInstance(this);
            downloaderWrapper.init();
            Log.d(TAG, "VideoDownloader initialized successfully");
            Log.d(TAG, "FFmpeg merge enabled=" + downloaderWrapper.isFfmpegMergeEnabled()
                + ", java fallback enabled=" + downloaderWrapper.isJavaMergeFallbackEnabled());
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize VideoDownloader", e);
        }

        
        // 注意：播放器核心的初始化不在这里进行
        // 因为 GSYVideoManager 需要 Activity Context
        // 播放核心会在 OrangevideoView 第一次初始化时设置
    }
    
    /**
     * 设置全局异常处理器，捕获崩溃日志
     */
    private void setupCrashHandler() {
        final Thread.UncaughtExceptionHandler defaultHandler = 
            Thread.getDefaultUncaughtExceptionHandler();
        
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                // 记录崩溃日志
                String logPath = saveCrashLog(throwable);
                
                // 显示崩溃提示（在主线程显示 Toast）
                showCrashToast(throwable, logPath);
                
                // 延迟一下，让 Toast 有时间显示
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                // 调用系统默认处理器
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, throwable);
                }
            }
        });
    }
    
    /**
     * 显示崩溃提示
     */
    private void showCrashToast(final Throwable throwable, final String logPath) {
        try {
            // 在主线程显示 Toast
            new Thread(new Runnable() {
                @Override
                public void run() {
                    android.os.Looper.prepare();
                    
                    String errorMsg = throwable.getClass().getSimpleName();
                    String message = "应用崩溃：" + errorMsg;
                    
                    if (logPath != null) {
                        message += "\n日志已保存";
                    }
                    
                    android.widget.Toast.makeText(
                        OrangeApplication.this,
                        message,
                        android.widget.Toast.LENGTH_LONG
                    ).show();
                    
                    android.os.Looper.loop();
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "显示崩溃提示失败", e);
        }
    }
    
    /**
     * 保存崩溃日志到文件
     * @return 日志文件路径，如果保存失败返回 null
     */
    private String saveCrashLog(Throwable throwable) {
        try {
            // 创建日志目录
            File logDir = new File(getExternalFilesDir(null), "crash_logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            // 创建日志文件
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                .format(new Date());
            File logFile = new File(logDir, "crash_" + timestamp + ".txt");
            
            // 写入日志
            FileWriter writer = new FileWriter(logFile);
            PrintWriter printWriter = new PrintWriter(writer);
            
            // 写入设备信息
            printWriter.println("=== 崩溃信息 ===");
            printWriter.println("时间: " + timestamp);
            printWriter.println("应用包名: " + getPackageName());
            try {
                String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
                printWriter.println("应用版本: " + versionName);
                printWriter.println("版本号: " + versionCode);
            } catch (Exception e) {
                printWriter.println("应用版本: 未知");
            }
            printWriter.println();
            
            printWriter.println("=== 设备信息 ===");
            printWriter.println("Android 版本: " + android.os.Build.VERSION.RELEASE);
            printWriter.println("API Level: " + android.os.Build.VERSION.SDK_INT);
            printWriter.println("设备型号: " + android.os.Build.MODEL);
            printWriter.println("设备厂商: " + android.os.Build.MANUFACTURER);
            printWriter.println("设备品牌: " + android.os.Build.BRAND);
            printWriter.println("CPU ABI: " + android.os.Build.CPU_ABI);
            printWriter.println();
            
            printWriter.println("=== 异常堆栈 ===");
            throwable.printStackTrace(printWriter);
            
            printWriter.close();
            writer.close();
            
            Log.e(TAG, "崩溃日志已保存到: " + logFile.getAbsolutePath());
            
            return logFile.getAbsolutePath();
            
        } catch (Exception e) {
            Log.e(TAG, "保存崩溃日志失败", e);
            return null;
        }
    }
}
