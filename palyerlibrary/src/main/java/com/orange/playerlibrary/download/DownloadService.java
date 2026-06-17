package com.orange.playerlibrary.download;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

/**
 * 下载服务
 * 使用前台服务保证下载任务在后台持续运行
 */
public class DownloadService extends Service {
    
    private static final String TAG = "DownloadService";
    private static final int FOREGROUND_NOTIFICATION_ID = 1001;
    
    private DownloadManager mDownloadManager;
    private DownloadNotification mNotification;
    private DownloadDatabase mDatabase;
    private DownloadBinder mBinder = new DownloadBinder();
    
    // 下载监听器
    private DownloadListener mDownloadListener = new DownloadListener() {
        @Override
        public void onDownloadStart(DownloadTask task) {
            // 更新数据库
            if (mDatabase != null) {
                mDatabase.insertTask(task);
            }
            // 显示通知
            if (mNotification != null) {
                mNotification.showDownloadStart(task);
            }
        }
        
        @Override
        public void onDownloadProgress(DownloadTask task, int progress, long speed) {
            // 更新数据库
            if (mDatabase != null) {
                mDatabase.updateTask(task);
            }
            // 更新通知
            if (mNotification != null) {
                mNotification.updateDownloadProgress(task);
            }
        }
        
        @Override
        public void onDownloadPaused(DownloadTask task) {
            // 更新数据库
            if (mDatabase != null) {
                mDatabase.updateTask(task);
            }
            // 显示暂停通知
            if (mNotification != null) {
                mNotification.showDownloadPaused(task);
            }
            // 检查是否还有下载任务，没有则停止前台服务
            checkAndStopForeground();
        }
        
        @Override
        public void onDownloadCompleted(DownloadTask task) {
            // 更新数据库
            if (mDatabase != null) {
                mDatabase.updateTask(task);
            }
            // 显示完成通知
            if (mNotification != null) {
                mNotification.showDownloadCompleted(task);
            }
            // 检查是否还有下载任务，没有则停止前台服务
            checkAndStopForeground();
        }
        
        @Override
        public void onDownloadFailed(DownloadTask task, String errorMessage) {
            // 更新数据库
            if (mDatabase != null) {
                mDatabase.updateTask(task);
            }
            // 显示失败通知
            if (mNotification != null) {
                mNotification.showDownloadFailed(task);
            }
            // 检查是否还有下载任务，没有则停止前台服务
            checkAndStopForeground();
        }
        
        @Override
        public void onDownloadCancelled(DownloadTask task) {
            // 从数据库删除
            if (mDatabase != null) {
                mDatabase.deleteTask(task.getTaskId());
            }
            // 取消通知
            if (mNotification != null) {
                mNotification.cancelNotification(task.getTaskId());
            }
            // 检查是否还有下载任务，没有则停止前台服务
            checkAndStopForeground();
        }
    };
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 初始化下载管理器
        mDownloadManager = DownloadManager.getInstance(this);
        mDownloadManager.addListener(mDownloadListener);
        
        // 初始化通知
        mNotification = new DownloadNotification(this);
        
        // 初始化数据库
        mDatabase = DownloadDatabase.getInstance(this);
        
        // 启动前台服务
        startForeground(FOREGROUND_NOTIFICATION_ID, mNotification.createForegroundNotification());
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_START_DOWNLOAD:
                        handleStartDownload(intent);
                        break;
                    case ACTION_PAUSE_DOWNLOAD:
                        handlePauseDownload(intent);
                        break;
                    case ACTION_RESUME_DOWNLOAD:
                        handleResumeDownload(intent);
                        break;
                    case ACTION_CANCEL_DOWNLOAD:
                        handleCancelDownload(intent);
                        break;
                }
            }
        }
        
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // 移除监听器
        if (mDownloadManager != null) {
            mDownloadManager.removeListener(mDownloadListener);
        }
        
        // 取消所有通知
        if (mNotification != null) {
            mNotification.cancelAllNotifications();
        }
    }
    
    /**
     * 检查并停止前台服务
     * 如果没有正在下载的任务，停止前台服务
     */
    private void checkAndStopForeground() {
        if (mDatabase != null) {
            int downloadingCount = mDatabase.getTaskCountByState(DownloadTask.STATE_DOWNLOADING);
            int waitingCount = mDatabase.getTaskCountByState(DownloadTask.STATE_WAITING);
            
            if (downloadingCount == 0 && waitingCount == 0) {
                // 没有正在下载的任务，停止前台服务
                stopForeground(true);
                stopSelf();
            }
        }
    }
    
    // ===== Intent Actions =====
    
    public static final String ACTION_START_DOWNLOAD = "com.orange.playerlibrary.download.START_DOWNLOAD";
    public static final String ACTION_PAUSE_DOWNLOAD = "com.orange.playerlibrary.download.PAUSE_DOWNLOAD";
    public static final String ACTION_RESUME_DOWNLOAD = "com.orange.playerlibrary.download.RESUME_DOWNLOAD";
    public static final String ACTION_CANCEL_DOWNLOAD = "com.orange.playerlibrary.download.CANCEL_DOWNLOAD";
    
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_SAVE_PATH = "save_path";
    public static final String EXTRA_TASK_ID = "task_id";
    
    /**
     * 处理开始下载
     */
    private void handleStartDownload(Intent intent) {
        String url = intent.getStringExtra(EXTRA_URL);
        String title = intent.getStringExtra(EXTRA_TITLE);
        String savePath = intent.getStringExtra(EXTRA_SAVE_PATH);
        
        android.util.Log.d(TAG, "handleStartDownload: url=" + url + ", title=" + title + ", savePath=" + savePath);
        
        if (url != null && mDownloadManager != null) {
            android.util.Log.d(TAG, "Starting download task...");
            if (savePath != null) {
                mDownloadManager.addDownload(url, title, savePath);
            } else {
                mDownloadManager.addDownload(url, title);
            }
        } else {
            android.util.Log.e(TAG, "Cannot start download: url=" + url + ", manager=" + mDownloadManager);
        }
    }
    
    /**
     * 处理暂停下载
     */
    private void handlePauseDownload(Intent intent) {
        String taskId = intent.getStringExtra(EXTRA_TASK_ID);
        if (taskId != null && mDownloadManager != null) {
            mDownloadManager.pauseDownload(taskId);
        }
    }
    
    /**
     * 处理恢复下载
     */
    private void handleResumeDownload(Intent intent) {
        String taskId = intent.getStringExtra(EXTRA_TASK_ID);
        if (taskId != null && mDownloadManager != null) {
            mDownloadManager.resumeDownload(taskId);
        }
    }
    
    /**
     * 处理取消下载
     */
    private void handleCancelDownload(Intent intent) {
        String taskId = intent.getStringExtra(EXTRA_TASK_ID);
        if (taskId != null && mDownloadManager != null) {
            mDownloadManager.cancelDownload(taskId);
        }
    }
    
    /**
     * Binder 类，用于绑定服务
     */
    public class DownloadBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
        
        public DownloadManager getDownloadManager() {
            return mDownloadManager;
        }
        
        public DownloadDatabase getDatabase() {
            return mDatabase;
        }
    }
}
