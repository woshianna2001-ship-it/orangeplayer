package com.orange.playerlibrary.download;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 下载管理器（单例）
 * 负责管理所有下载任务
 */
public class DownloadManager {
    
    private static final String TAG = "DownloadManager";
    private static volatile DownloadManager sInstance;
    
    private Context mContext;
    private ExecutorService mExecutor;
    private Handler mMainHandler;
    
    // 任务管理
    private Map<String, DownloadTask> mTasks;
    private Map<String, DownloadWorker> mWorkers;
    private List<DownloadListener> mListeners;
    
    // 数据库
    private DownloadDatabase mDatabase;
    
    // 配置
    private int mMaxConcurrentDownloads = 3;
    private String mDefaultSavePath;
    
    private DownloadManager(Context context) {
        mContext = context.getApplicationContext();
        mExecutor = Executors.newFixedThreadPool(mMaxConcurrentDownloads);
        mMainHandler = new Handler(Looper.getMainLooper());
        mTasks = new ConcurrentHashMap<>();
        mWorkers = new ConcurrentHashMap<>();
        mListeners = new ArrayList<>();
        
        // 初始化数据库
        mDatabase = DownloadDatabase.getInstance(mContext);
        
        // 默认保存路径：/sdcard/Android/data/包名/files/Download
        mDefaultSavePath = mContext.getExternalFilesDir("Download").getAbsolutePath();
        
        // 从数据库恢复未完成的任务
        restoreTasksFromDatabase();
    }
    
    /**
     * 从数据库恢复未完成的任务
     */
    private void restoreTasksFromDatabase() {
        List<DownloadTask> tasks = mDatabase.queryDownloadingTasks();
        for (DownloadTask task : tasks) {
            // 将状态改为暂停，等待用户手动恢复
            task.setState(DownloadTask.STATE_PAUSED);
            mDatabase.updateTask(task);
            mTasks.put(task.getTaskId(), task);
        }
    }
    
    public static DownloadManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (DownloadManager.class) {
                if (sInstance == null) {
                    sInstance = new DownloadManager(context);
                }
            }
        }
        return sInstance;
    }
    
    /**
     * 添加下载任务
     */
    public String addDownload(String url, String title) {
        return addDownload(url, title, mDefaultSavePath);
    }
    
    /**
     * 添加下载任务
     */
    public String addDownload(String url, String title, String savePath) {
        android.util.Log.d(TAG, "addDownload: url=" + url + ", title=" + title + ", savePath=" + savePath);
        
        DownloadTask task = new DownloadTask(url, title, savePath);
        mTasks.put(task.getTaskId(), task);
        
        android.util.Log.d(TAG, "Created task: " + task.getTaskId() + ", state=" + task.getState());
        
        // 保存到数据库
        if (mDatabase != null) {
            mDatabase.insertTask(task);
            android.util.Log.d(TAG, "Task saved to database");
        }
        
        // 通知监听器
        notifyDownloadStart(task);
        android.util.Log.d(TAG, "Notified listeners");
        
        // 开始下载
        startDownload(task);
        android.util.Log.d(TAG, "Download started");
        
        return task.getTaskId();
    }
    
    /**
     * 开始下载
     */
    private void startDownload(DownloadTask task) {
        if (task.getState() == DownloadTask.STATE_DOWNLOADING) {
            return;
        }
        
        task.setState(DownloadTask.STATE_DOWNLOADING);
        
        DownloadWorker worker = new DownloadWorker(task);
        mWorkers.put(task.getTaskId(), worker);
        mExecutor.execute(worker);
    }
    
    /**
     * 暂停下载
     */
    public void pauseDownload(String taskId) {
        DownloadTask task = mTasks.get(taskId);
        if (task == null) return;
        
        DownloadWorker worker = mWorkers.get(taskId);
        if (worker != null) {
            worker.pause();
        }
        
        task.setState(DownloadTask.STATE_PAUSED);
        
        // 更新数据库
        if (mDatabase != null) {
            mDatabase.updateTask(task);
        }
        
        notifyDownloadPaused(task);
    }
    
    /**
     * 恢复下载
     */
    public void resumeDownload(String taskId) {
        DownloadTask task = mTasks.get(taskId);
        if (task == null) return;
        
        startDownload(task);
    }
    
    /**
     * 取消下载
     */
    public void cancelDownload(String taskId) {
        DownloadTask task = mTasks.get(taskId);
        if (task == null) return;
        
        DownloadWorker worker = mWorkers.get(taskId);
        if (worker != null) {
            worker.cancel();
        }
        
        task.setState(DownloadTask.STATE_CANCELLED);
        notifyDownloadCancelled(task);
        
        // 删除未完成的文件
        File file = new File(task.getFullPath());
        if (file.exists() && task.getProgress() < 100) {
            file.delete();
        }
        
        // 从数据库删除
        if (mDatabase != null) {
            mDatabase.deleteTask(taskId);
        }
        
        mTasks.remove(taskId);
        mWorkers.remove(taskId);
    }
    
    /**
     * 获取任务
     */
    public DownloadTask getTask(String taskId) {
        return mTasks.get(taskId);
    }
    
    /**
     * 获取所有任务
     */
    public List<DownloadTask> getAllTasks() {
        return new ArrayList<>(mTasks.values());
    }
    
    /**
     * 添加监听器
     */
    public void addListener(DownloadListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }
    
    /**
     * 移除监听器
     */
    public void removeListener(DownloadListener listener) {
        mListeners.remove(listener);
    }
    
    /**
     * 设置最大并发下载数
     */
    public void setMaxConcurrentDownloads(int max) {
        mMaxConcurrentDownloads = max;
    }
    
    /**
     * 设置默认保存路径
     */
    public void setDefaultSavePath(String path) {
        mDefaultSavePath = path;
    }
    
    // ===== 通知方法 =====
    
    private void notifyDownloadStart(final DownloadTask task) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (DownloadListener listener : mListeners) {
                    listener.onDownloadStart(task);
                }
            }
        });
    }
    
    private void notifyDownloadProgress(final DownloadTask task, final int progress, final long speed) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (DownloadListener listener : mListeners) {
                    listener.onDownloadProgress(task, progress, speed);
                }
            }
        });
    }
    
    private void notifyDownloadPaused(final DownloadTask task) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (DownloadListener listener : mListeners) {
                    listener.onDownloadPaused(task);
                }
            }
        });
    }
    
    private void notifyDownloadCompleted(final DownloadTask task) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (DownloadListener listener : mListeners) {
                    listener.onDownloadCompleted(task);
                }
            }
        });
    }
    
    private void notifyDownloadFailed(final DownloadTask task, final String errorMessage) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (DownloadListener listener : mListeners) {
                    listener.onDownloadFailed(task, errorMessage);
                }
            }
        });
    }
    
    private void notifyDownloadCancelled(final DownloadTask task) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (DownloadListener listener : mListeners) {
                    listener.onDownloadCancelled(task);
                }
            }
        });
    }
    
    // ===== 下载工作线程 =====
    
    private class DownloadWorker implements Runnable {
        
        private DownloadTask mTask;
        private volatile boolean mPaused = false;
        private volatile boolean mCancelled = false;
        
        public DownloadWorker(DownloadTask task) {
            mTask = task;
        }
        
        public void pause() {
            mPaused = true;
        }
        
        public void cancel() {
            mCancelled = true;
        }
        
        @Override
        public void run() {
            HttpURLConnection connection = null;
            InputStream inputStream = null;
            FileOutputStream outputStream = null;
            
            try {
                // 创建保存目录
                File saveDir = new File(mTask.getSavePath());
                if (!saveDir.exists()) {
                    saveDir.mkdirs();
                }
                
                File file = new File(mTask.getFullPath());
                long downloadedSize = 0;
                
                // 断点续传：检查文件是否已存在
                if (file.exists()) {
                    downloadedSize = file.length();
                    mTask.setDownloadedSize(downloadedSize);
                }
                
                // 建立连接
                URL url = new URL(mTask.getUrl());
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                // 设置断点续传
                if (downloadedSize > 0) {
                    connection.setRequestProperty("Range", "bytes=" + downloadedSize + "-");
                }
                
                connection.connect();
                
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK && 
                    responseCode != HttpURLConnection.HTTP_PARTIAL) {
                    throw new Exception("HTTP error code: " + responseCode);
                }
                
                long totalSize = connection.getContentLength() + downloadedSize;
                mTask.setTotalSize(totalSize);
                
                inputStream = connection.getInputStream();
                outputStream = new FileOutputStream(file, downloadedSize > 0);
                
                byte[] buffer = new byte[8192];
                int len;
                long lastUpdateTime = System.currentTimeMillis();
                long lastDownloadedSize = downloadedSize;
                
                while ((len = inputStream.read(buffer)) != -1) {
                    if (mCancelled) {
                        mTask.setState(DownloadTask.STATE_CANCELLED);
                        return;
                    }
                    
                    if (mPaused) {
                        mTask.setState(DownloadTask.STATE_PAUSED);
                        return;
                    }
                    
                    outputStream.write(buffer, 0, len);
                    downloadedSize += len;
                    
                    // 更新进度（每秒更新一次）
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastUpdateTime >= 1000) {
                        long timeElapsed = currentTime - lastUpdateTime;
                        long bytesDownloaded = downloadedSize - lastDownloadedSize;
                        long speed = bytesDownloaded * 1000 / timeElapsed;
                        
                        mTask.updateProgress(downloadedSize, totalSize);
                        mTask.setSpeed(speed);
                        
                        notifyDownloadProgress(mTask, mTask.getProgress(), speed);
                        
                        lastUpdateTime = currentTime;
                        lastDownloadedSize = downloadedSize;
                    }
                }
                
                // 下载完成
                mTask.setState(DownloadTask.STATE_COMPLETED);
                mTask.setProgress(100);
                
                // 更新数据库
                if (mDatabase != null) {
                    mDatabase.updateTask(mTask);
                }
                
                notifyDownloadCompleted(mTask);
                
            } catch (Exception e) {
                android.util.Log.e(TAG, "Download failed: " + mTask.getUrl(), e);
                mTask.setState(DownloadTask.STATE_FAILED);
                mTask.setErrorMessage(e.getMessage());
                
                // 更新数据库
                if (mDatabase != null) {
                    mDatabase.updateTask(mTask);
                }
                
                notifyDownloadFailed(mTask, e.getMessage());
            } finally {
                try {
                    if (outputStream != null) outputStream.close();
                    if (inputStream != null) inputStream.close();
                    if (connection != null) connection.disconnect();
                } catch (Exception e) {
                    // Ignore
                }
                
                mWorkers.remove(mTask.getTaskId());
            }
        }
    }
}
