package com.orange.downloader.task;

import com.orange.downloader.common.DownloadConstants;
import com.orange.downloader.listener.IDownloadTaskListener;
import com.orange.downloader.model.VideoTaskItem;
import com.orange.downloader.utils.LogUtils;
import com.orange.downloader.utils.VideoDownloadUtils;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

public abstract class VideoDownloadTask {

    protected static final int THREAD_COUNT = 6;
    protected static final int BUFFER_SIZE = VideoDownloadUtils.DEFAULT_BUFFER_SIZE;
    protected final VideoTaskItem mTaskItem;
    protected final String mFinalUrl;
    protected Map<String, String> mHeaders;
    protected File mSaveDir;
    protected String mSaveName;
    protected ThreadPoolExecutor mDownloadExecutor;
    protected IDownloadTaskListener mDownloadTaskListener;
    protected volatile boolean mDownloadFinished = false;
    protected final Object mDownloadLock = new Object();
    protected long mLastCachedSize = 0L;
    protected long mCurrentCachedSize = 0L;
    protected long mLastInvokeTime = 0L;
    protected float mSpeed = 0.0f;
    protected float mPercent = 0.0f;

    protected VideoDownloadTask(VideoTaskItem taskItem, Map<String, String> headers) {
        LogUtils.i(DownloadConstants.TAG, "VideoDownloadTask constructor start");
        mTaskItem = taskItem;
        mHeaders = headers;
        mFinalUrl = taskItem.getFinalUrl();
        mSaveName = VideoDownloadUtils.computeMD5(taskItem.getUrl());
        LogUtils.i(DownloadConstants.TAG, "  SaveName (MD5): " + mSaveName);
        
        String cacheRoot = VideoDownloadUtils.getDownloadConfig().getCacheRoot();
        LogUtils.i(DownloadConstants.TAG, "  CacheRoot: " + cacheRoot);
        
        mSaveDir = new File(cacheRoot, mSaveName);
        LogUtils.i(DownloadConstants.TAG, "  SaveDir path: " + mSaveDir.getAbsolutePath());
        LogUtils.i(DownloadConstants.TAG, "  SaveDir exists before mkdir: " + mSaveDir.exists());
        
        if (!mSaveDir.exists()) {
            boolean mkdirResult = mSaveDir.mkdirs();  // 使用 mkdirs() 创建所有必需的父目录
            LogUtils.i(DownloadConstants.TAG, "  mkdirs() result: " + mkdirResult);
            LogUtils.i(DownloadConstants.TAG, "  SaveDir exists after mkdirs: " + mSaveDir.exists());
            
            if (!mkdirResult) {
                LogUtils.e(DownloadConstants.TAG, "  Failed to create directory!");
                LogUtils.e(DownloadConstants.TAG, "  Parent exists: " + mSaveDir.getParentFile().exists());
                LogUtils.e(DownloadConstants.TAG, "  Parent canWrite: " + mSaveDir.getParentFile().canWrite());
            }
        }
        mTaskItem.setSaveDir(mSaveDir.getAbsolutePath());
        LogUtils.i(DownloadConstants.TAG, "VideoDownloadTask constructor end");
    }

    public void setDownloadTaskListener(IDownloadTaskListener listener) {
        mDownloadTaskListener = listener;
    }

    public abstract void startDownload();

    public abstract void resumeDownload();

    public abstract void pauseDownload();

    protected void notifyOnTaskPaused() {
        if (mDownloadTaskListener != null) {
            mDownloadTaskListener.onTaskPaused();
        }
    }

    protected void notifyOnTaskFailed(Exception e) {
        if (mDownloadExecutor != null && mDownloadExecutor.isShutdown()) {
            return;
        }
        mDownloadExecutor.shutdownNow();
        mDownloadTaskListener.onTaskFailed(e);
    }

    protected void setThreadPoolArgument(int corePoolSize, int maxPoolSize) {
        if (mDownloadExecutor != null && !mDownloadExecutor.isShutdown()) {
            mDownloadExecutor.setCorePoolSize(corePoolSize);
            mDownloadExecutor.setMaximumPoolSize(maxPoolSize);
        }
    }
}
