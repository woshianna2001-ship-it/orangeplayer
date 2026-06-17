package com.orange.downloader;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.orange.downloader.common.DownloadConstants;
import com.orange.downloader.database.VideoDownloadDatabaseHelper;
import com.orange.downloader.listener.DownloadListener;
import com.orange.downloader.listener.IDownloadInfosCallback;
import com.orange.downloader.listener.IDownloadTaskListener;
import com.orange.downloader.listener.IVideoInfoListener;
import com.orange.downloader.listener.IVideoInfoParseListener;
import com.orange.downloader.m3u8.M3U8;
import com.orange.downloader.model.Video;
import com.orange.downloader.model.VideoTaskItem;
import com.orange.downloader.model.VideoTaskState;
import com.orange.downloader.listener.IM3U8MergeResultListener;
import com.orange.downloader.merge.MergeFeatureToggle;
import com.orange.downloader.merge.TsMergeService;
import com.orange.downloader.task.BaseVideoDownloadTask;
import com.orange.downloader.legacy.DownloadService;


import com.orange.downloader.task.M3U8VideoDownloadTask;
import com.orange.downloader.task.MultiSegVideoDownloadTask;
import com.orange.downloader.task.VideoDownloadTask;
import com.orange.downloader.utils.ContextUtils;
import com.orange.downloader.utils.DownloadExceptionUtils;
import com.orange.downloader.utils.LogUtils;
import com.orange.downloader.utils.VideoDownloadUtils;
import com.orange.downloader.utils.VideoStorageUtils;
import com.orange.downloader.utils.WorkerThreadHandler;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class VideoDownloadManager {
    private static volatile VideoDownloadManager sInstance = null;
    private DownloadListener mGlobalDownloadListener = null;
    private VideoDownloadDatabaseHelper mVideoDatabaseHelper = null;
    private VideoDownloadQueue mVideoDownloadQueue;
    private Object mQueueLock = new Object();
    private VideoDownloadConfig mConfig;

    private VideoDownloadHandler mVideoDownloadHandler;
    private List<IDownloadInfosCallback> mDownloadInfoCallbacks = new CopyOnWriteArrayList<>();
    private Map<String, VideoDownloadTask> mVideoDownloadTaskMap = new ConcurrentHashMap<>();
    private Map<String, VideoTaskItem> mVideoItemTaskMap = new ConcurrentHashMap<>();
    private final TsMergeService mTsMergeService = new TsMergeService();
    private final Set<String> mMergingTaskUrls = Collections.newSetFromMap(new ConcurrentHashMap<>());


    public static class Build {
        private String mCacheRoot;
        private int mReadTimeOut = 60 * 1000;              // 60 seconds
        private int mConnTimeOut = 60 * 1000;              // 60 seconds
        private boolean mIgnoreCertErrors = false;
        private int mConcurrentCount = 3;
        private boolean mShouldM3U8Merged = false;

        public Build(Context context) {
            ContextUtils.initApplicationContext(context);
        }

        //设置下载目录
        public Build setCacheRoot(String cacheRoot) {
            mCacheRoot = cacheRoot;
            return this;
        }

        //设置超时时间
        public Build setTimeOut(int readTimeOut, int connTimeOut) {
            mReadTimeOut = readTimeOut;
            mConnTimeOut = connTimeOut;
            return this;
        }

        //设置并发下载的个数
        public Build setConcurrentCount(int count) {
            mConcurrentCount = count;
            return this;
        }

        //是否信任证书
        public Build setIgnoreCertErrors(boolean ignoreCertErrors) {
            mIgnoreCertErrors = ignoreCertErrors;
            return this;
        }

        //M3U8下载成功之后是否自动合并
        public Build setShouldM3U8Merged(boolean shouldM3U8Merged) {
            mShouldM3U8Merged = shouldM3U8Merged;
            return this;
        }

        public VideoDownloadConfig buildConfig() {
            return new VideoDownloadConfig(mCacheRoot, mReadTimeOut, mConnTimeOut, mIgnoreCertErrors, mConcurrentCount, mShouldM3U8Merged);
        }
    }

    public void setConcurrentCount(int count) {
        if (mConfig != null) {
            mConfig.setConcurrentCount(count);
        }
    }

    public void setIgnoreAllCertErrors(boolean enable) {
        if (mConfig != null) {
            mConfig.setIgnoreAllCertErrors(enable);
        }
    }

    public void setShouldM3U8Merged(boolean enable) {
        if (mConfig != null) {
            LogUtils.w(DownloadConstants.TAG, "setShouldM3U8Merged = " + enable);
            mConfig.setShouldM3U8Merged(enable);
        }
    }

    public static VideoDownloadManager getInstance() {
        if (sInstance == null) {
            synchronized (VideoDownloadManager.class) {
                if (sInstance == null) {
                    sInstance = new VideoDownloadManager();
                }
            }
        }
        return sInstance;
    }

    private VideoDownloadManager() {
        mVideoDownloadQueue = new VideoDownloadQueue();
    }
    
    /**
     * 根据 MimeType 获取文件扩展名
     */
    private String getFileExtensionFromMimeType(String mimeType) {
        if (TextUtils.isEmpty(mimeType)) {
            return VideoDownloadUtils.VIDEO_SUFFIX;
        }
        
        if (mimeType.contains(Video.Mime.MIME_TYPE_MP4)) {
            return Video.SUFFIX.SUFFIX_MP4;
        } else if (mimeType.contains(Video.Mime.MIME_TYPE_WEBM)) {
            return Video.SUFFIX.SUFFIX_WEBM;
        } else if (mimeType.contains(Video.Mime.MIME_TYPE_QUICKTIME)) {
            return Video.SUFFIX.SUFFIX_MOV;
        } else if (mimeType.contains(Video.Mime.MIME_TYPE_3GP)) {
            return Video.SUFFIX.SUFFIX_3GP;
        } else if (mimeType.contains(Video.Mime.MIME_TYPE_MKV)) {
            return Video.SUFFIX.SUFFIX_MKV;
        }
        
        return VideoDownloadUtils.VIDEO_SUFFIX;
    }

    public void initConfig(VideoDownloadConfig config) {
        //如果为null, 会crash
        mConfig = config;
        VideoDownloadUtils.setDownloadConfig(config);
        MergeFeatureToggle.initialize(ContextUtils.getApplicationContext());
        mVideoDatabaseHelper = new VideoDownloadDatabaseHelper(ContextUtils.getApplicationContext());
        HandlerThread stateThread = new HandlerThread("Video_download_state_thread");
        stateThread.start();
        mVideoDownloadHandler = new VideoDownloadHandler(stateThread.getLooper());
    }


    public VideoDownloadConfig downloadConfig() {
        return mConfig;
    }

    /**
     * 根据 URL 获取下载任务信息
     */
    public VideoTaskItem getDownloadTaskItem(String url) {
        if (!TextUtils.isEmpty(url) && mVideoItemTaskMap.containsKey(url)) {
            return mVideoItemTaskMap.get(url);
        }
        // 如果内存中没有，尝试从数据库获取
        if (mVideoDatabaseHelper != null && !TextUtils.isEmpty(url)) {
            List<VideoTaskItem> items = mVideoDatabaseHelper.getDownloadInfos();
            if (items != null) {
                for (VideoTaskItem item : items) {
                    if (url.equals(item.getUrl())) {
                        mVideoItemTaskMap.put(url, item);
                        return item;
                    }
                }
            }
        }
        return null;
    }

    public void fetchDownloadItems(IDownloadInfosCallback callback) {
        mDownloadInfoCallbacks.add(callback);
        mVideoDownloadHandler.obtainMessage(DownloadConstants.MSG_FETCH_DOWNLOAD_INFO).sendToTarget();
    }

    public void removeDownloadInfosCallback(IDownloadInfosCallback callback) {
        mDownloadInfoCallbacks.remove(callback);
    }

    public void setGlobalDownloadListener(DownloadListener downloadListener) {
        mGlobalDownloadListener = downloadListener;
    }

    public void startDownload(VideoTaskItem taskItem) {
        LogUtils.i(DownloadConstants.TAG, "[QUEUE] startDownload() called, url=" + (taskItem != null ? taskItem.getUrl() : "null"));
        if (taskItem == null || TextUtils.isEmpty(taskItem.getUrl())) {
            LogUtils.w(DownloadConstants.TAG, "[QUEUE] startDownload() rejected: taskItem or url is null");
            return;
        }
        updateForegroundService();

        if (mConfig != null && mConfig.shouldM3U8Merged() && taskItem.isHlsType()) {
            if (hasValidMergedOutputForStart(taskItem)) {
                taskItem.setTaskState(VideoTaskState.SUCCESS);
                taskItem.setIsCompleted(true);
                taskItem.setPercent(100f);
                mGlobalDownloadListener.onDownloadSuccess(taskItem);
                markDownloadFinishEvent(taskItem);
                return;
            }
            if (isMergeReadyPercent(taskItem) && hasMergeSourceForStart(taskItem)) {
                taskItem.setTaskState(VideoTaskState.PREPARE);
                taskItem.setPaused(false);
                mVideoDownloadHandler.obtainMessage(DownloadConstants.MSG_DOWNLOAD_PREPARE, (VideoTaskItem) taskItem.clone()).sendToTarget();
                doMergeTs(taskItem, mergedTask -> {
                    if (mergedTask != null && mergedTask.getErrorCode() != 0) {
                        mGlobalDownloadListener.onDownloadError(mergedTask);
                    } else {
                        mGlobalDownloadListener.onDownloadSuccess(mergedTask);
                    }
                    markDownloadFinishEvent(mergedTask);
                });
                return;
            }
        }

        synchronized (mQueueLock) {
            if (mVideoDownloadQueue.contains(taskItem)) {
                taskItem = mVideoDownloadQueue.getTaskItem(taskItem.getUrl());
                LogUtils.i(DownloadConstants.TAG, "[QUEUE] Task already in queue, using existing item");
            } else {
                mVideoDownloadQueue.offer(taskItem);
                LogUtils.i(DownloadConstants.TAG, "[QUEUE] Task added to queue, queueSize=" + mVideoDownloadQueue.size());
            }
        }
        
        taskItem.setPaused(false);
        taskItem.setDownloadCreateTime(taskItem.getDownloadCreateTime());
        taskItem.setTaskState(VideoTaskState.PENDING);
        LogUtils.i(DownloadConstants.TAG, "[QUEUE] Task state set to PENDING, fileHash=" + taskItem.getFileHash());
        VideoTaskItem tempTaskItem = (VideoTaskItem) taskItem.clone();
        mVideoDownloadHandler.obtainMessage(DownloadConstants.MSG_DOWNLOAD_PENDING, tempTaskItem).sendToTarget();
        startDownload(taskItem, null);
    }

    public boolean isMergedOutputReady(VideoTaskItem taskItem) {
        return hasValidMergedOutputForStart(taskItem);
    }

    public boolean ensureMergedForCompletedTask(VideoTaskItem taskItem) {
        if (taskItem == null || mConfig == null || !mConfig.shouldM3U8Merged()) {
            return false;
        }
        if (!isM3U8TaskLikely(taskItem)) {
            return false;
        }
        if (TextUtils.isEmpty(taskItem.getUrl())) {
            return false;
        }
        if (mMergingTaskUrls.contains(taskItem.getUrl())) {
            taskItem.setTaskState(VideoTaskState.PREPARE);
            taskItem.setPaused(false);
            mVideoDownloadHandler.obtainMessage(DownloadConstants.MSG_DOWNLOAD_PREPARE, (VideoTaskItem) taskItem.clone()).sendToTarget();
            return true;
        }
        if (hasValidMergedOutputForStart(taskItem)) {
            taskItem.setTaskState(VideoTaskState.SUCCESS);
            taskItem.setIsCompleted(true);
            taskItem.setPercent(100f);
            mGlobalDownloadListener.onDownloadSuccess(taskItem);
            markDownloadFinishEvent(taskItem);
            return true;
        }
        if (!hasMergeSourceForStart(taskItem)) {
            return false;
        }
        taskItem.setTaskState(VideoTaskState.PREPARE);
        taskItem.setPaused(false);
        mVideoDownloadHandler.obtainMessage(DownloadConstants.MSG_DOWNLOAD_PREPARE, (VideoTaskItem) taskItem.clone()).sendToTarget();
        mMergingTaskUrls.add(taskItem.getUrl());
        doMergeTs(taskItem, mergedTask -> {
            try {
                if (mergedTask != null && mergedTask.getErrorCode() != 0) {
                    mGlobalDownloadListener.onDownloadError(mergedTask);
                } else {
                    mGlobalDownloadListener.onDownloadSuccess(mergedTask);
                }
                markDownloadFinishEvent(mergedTask);
                updateForegroundService();
            } finally {
                mMergingTaskUrls.remove(taskItem.getUrl());
            }
        });
        return true;
    }

    private boolean isM3U8TaskLikely(VideoTaskItem taskItem) {
        if (taskItem == null) {
            return false;
        }
        if (taskItem.isHlsType()) {
            return true;
        }
        String url = taskItem.getUrl();
        if (!TextUtils.isEmpty(url) && url.toLowerCase().contains(".m3u8")) {
            return true;
        }
        if (TextUtils.isEmpty(taskItem.getSaveDir())) {
            return false;
        }
        File saveDir = new File(taskItem.getSaveDir());
        if (!saveDir.exists() || !saveDir.isDirectory()) {
            return false;
        }
        File[] playlistFiles = saveDir.listFiles((dir, name) -> name != null
                && (name.endsWith("_" + VideoDownloadUtils.LOCAL_M3U8)
                || name.endsWith("_" + VideoDownloadUtils.LOCAL_M3U8_WITH_KEY)));
        return playlistFiles != null && playlistFiles.length > 0;
    }

    public void startDownload(VideoTaskItem taskItem, Map<String, String> headers) {
        if (taskItem == null || TextUtils.isEmpty(taskItem.getUrl()))
            return;
        parseVideoDownloadInfo(taskItem, headers);
    }

    private void parseVideoDownloadInfo(VideoTaskItem taskItem, Map<String, String> headers) {
        String videoUrl = taskItem.getUrl();
        String saveName = VideoDownloadUtils.computeMD5(videoUrl);
        taskItem.setFileHash(saveName);
        // 保存headers到taskItem
        taskItem.setRequestHeaders(headersToJson(headers));
        boolean taskExisted = taskItem.getDownloadCreateTime() != 0;
        if (taskExisted) {
            parseExistVideoDownloadInfo(taskItem, headers);
        } else {
            parseNetworkVideoInfo(taskItem, headers);
        }
    }

    private void parseExistVideoDownloadInfo(final VideoTaskItem taskItem, final Map<String, String> headers) {
        if (taskItem.isHlsType()) {
            VideoInfoParserManager.getInstance().parseLocalM3U8File(taskItem, new IVideoInfoParseListener() {
                @Override
                public void onM3U8FileParseSuccess(VideoTaskItem info, M3U8 m3u8) {
                    startM3U8VideoDownloadTask(taskItem, m3u8, headers);
                }

                @Override
                public void onM3U8FileParseFailed(VideoTaskItem info, Throwable error) {
                    parseNetworkVideoInfo(taskItem, headers);
                }
            });
        } else {
            startBaseVideoDownloadTask(taskItem, headers);
        }
    }

    private void parseNetworkVideoInfo(final VideoTaskItem taskItem, final Map<String, String> headers) {
        VideoInfoParserManager.getInstance().parseVideoInfo(taskItem, new IVideoInfoListener() {
            @Override
            public void onFinalUrl(String finalUrl) {
            }

            @Override
            public void onBaseVideoInfoSuccess(VideoTaskItem taskItem) {
                startBaseVideoDownloadTask(taskItem, headers);
            }

            @Override
            public void onBaseVideoInfoFailed(Throwable error) {
                LogUtils.w(DownloadConstants.TAG, "onInfoFailed error=" + error);
                int errorCode = DownloadExceptionUtils.getErrorCode(error);
                taskItem.setErrorCode(errorCode);
                taskItem.setTaskState(VideoTaskState.ERROR);
                mVideoDownloadHandler.obtainMessage(DownloadConstants.MSG_DOWNLOAD_ERROR, taskItem).sendToTarget();
            }

            @Override
            public void onM3U8InfoSuccess(VideoTaskItem info, M3U8 m3u8) {
                taskItem.setMimeType(info.getMimeType());
                taskItem.setSaveDir(info.getSaveDir());
                taskItem.setVideoType(info.getVideoType());
                // 设置filePath用于后续合并
                if (!TextUtils.isEmpty(info.getSaveDir()) && !TextUtils.isEmpty(info.getFileHash())) {
                    taskItem.setFileHash(info.getFileHash());
                    taskItem.setFilePath(info.getSaveDir() + File.separator + info.getFileHash() + "_" + VideoDownloadUtils.LOCAL_M3U8);
                }
                startM3U8VideoDownloadTask(taskItem, m3u8, headers);
            }

            @Override
            public void onLiveM3U8Callback(VideoTaskItem info) {
                LogUtils.w(DownloadConstants.TAG, "onLiveM3U8Callback cannot be cached.");
                taskItem.setErrorCode(DownloadExceptionUtils.LIVE_M3U8_ERROR);
                taskItem.setTaskState(VideoTaskState.ERROR);
                mVideoDownloadHandler.obtainMessage(DownloadConstants.MSG_DOWNLOAD_ERROR, taskItem).sendToTarget();
            }

            @Override
            public void onM3U8InfoFailed(Throwable error) {
                LogUtils.w(DownloadConstants.TAG, "onM3U8InfoFailed : " + error);
                int errorCode = DownloadExceptionUtils.getErrorCode(error);
                taskItem.setErrorCode(errorCode);
                taskItem.setTaskState(VideoTaskState.ERROR);
                mVideoDownloadHandler.obtainMessage(DownloadConstants.MSG_DOWNLOAD_ERROR, taskItem).sendToTarget();
            }
        }, headers);
    }

    private void startM3U8VideoDownloadTask(final VideoTaskItem taskItem, M3U8 m3u8, Map<String, String> headers) {
        LogUtils.i(DownloadConstants.TAG, "[M3U8] startM3U8VideoDownloadTask called, url=" + taskItem.getUrl());
        LogUtils.i(DownloadConstants.TAG, "[M3U8] saveDir=" + taskItem.getSaveDir() + ", fileHash=" + taskItem.getFileHash() + ", filePath=" + taskItem.getFilePath());
        taskItem.setTaskState(VideoTaskState.PREPARE);
        mVideoItemTaskMap.put(taskItem.getUrl(), taskItem);
        VideoTaskItem tempTaskItem = (VideoTaskItem) taskItem.clone();
        mVideoDownloadHandler.obtainMessage(DownloadConstants.MSG_DOWNLOAD_PREPARE, tempTaskItem).sendToTarget();
        synchronized (mQueueLock) {
            if (!mVideoDownloadQueue.contains(taskItem)) {
                mVideoDownloadQueue.offer(taskItem);
            }
            int downloadingCount = mVideoDownloadQueue.getDownloadingCount();
            int concurrentCount = mConfig.getConcurrentCount();
            LogUtils.i(DownloadConstants.TAG, "[M3U8] downloadingCount=" + downloadingCount + ", concurrentCount=" + concurrentCount);
            if (downloadingCount >= concurrentCount) {
                LogUtils.w(DownloadConstants.TAG, "[M3U8] Concurrent limit reached, task will wait");
                return;
            }
        }
        VideoDownloadTask downloadTask = mVideoDownloadTaskMap.get(taskItem.getUrl());
        if (downloadTask == null) {
            downloadTask = new M3U8VideoDownloadTask(taskItem, m3u8, headers);
            mVideoDownloadTaskMap.put(taskItem.getUrl(), downloadTask);
        }
        startDownloadTask(downloadTask, taskItem);
    }

    private void startBaseVideoDownloadTask(VideoTaskItem taskItem, Map<String, String> headers) {
        LogUtils.i(DownloadConstants.TAG, "[BASE] startBaseVideoDownloadTask called, url=" + taskItem.getUrl());
        LogUtils.i(DownloadConstants.TAG, "[BASE] mimeType=" + taskItem.getMimeType() + ", fileHash=" + taskItem.getFileHash());
        taskItem.setTaskState(VideoTaskState.PREPARE);
        mVideoItemTaskMap.put(taskItem.getUrl(), taskItem);
        VideoTaskItem tempTaskItem = (VideoTaskItem) taskItem.clone();
        mVideoDownloadHandler.obtainMessage(DownloadConstants.MSG_DOWNLOAD_PREPARE, tempTaskItem).sendToTarget();
        synchronized (mQueueLock) {
            if (!mVideoDownloadQueue.contains(taskItem)) {
                mVideoDownloadQueue.offer(taskItem);
            }
            int downloadingCount = mVideoDownloadQueue.getDownloadingCount();
            int concurrentCount = mConfig.getConcurrentCount();
            LogUtils.i(DownloadConstants.TAG, "[BASE] downloadingCount=" + downloadingCount + ", concurrentCount=" + concurrentCount);
            if (downloadingCount >= concurrentCount) {
                LogUtils.w(DownloadConstants.TAG, "[BASE] Concurrent limit reached, task will wait");
                return;
            }
        }
        VideoDownloadTask downloadTask = mVideoDownloadTaskMap.get(taskItem.getUrl());
        if (downloadTask == null) {
            // 回退到单线程下载 BaseVideoDownloadTask
            // 多线程 MultiSegVideoDownloadTask 内部 RandomAccessFile 写入可能存在文件截断/覆盖导致的损坏问题
            downloadTask = new BaseVideoDownloadTask(taskItem, headers);
            // downloadTask = new MultiSegVideoDownloadTask(taskItem, headers);
            mVideoDownloadTaskMap.put(taskItem.getUrl(), downloadTask);
        }
        startDownloadTask(downloadTask, taskItem);
    }

    private void tryStartNextPendingTask() {
        LogUtils.i(DownloadConstants.TAG, "[SCHEDULE] tryStartNextPendingTask called");
        VideoTaskItem pendingTask;
        synchronized (mQueueLock) {
            if (mConfig == null) {
                LogUtils.w(DownloadConstants.TAG, "[SCHEDULE] mConfig is null, cannot start next");
                return;
            }
            int downloadingCount = mVideoDownloadQueue.getDownloadingCount();
            int concurrentCount = mConfig.getConcurrentCount();
            int pendingCount = mVideoDownloadQueue.getPendingCount();
            LogUtils.i(DownloadConstants.TAG, "[SCHEDULE] downloading=" + downloadingCount + ", concurrent=" + concurrentCount + ", pending=" + pendingCount);
            if (downloadingCount >= concurrentCount) {
                LogUtils.w(DownloadConstants.TAG, "[SCHEDULE] Concurrent limit reached, no new task started");
                return;
            }
            pendingTask = mVideoDownloadQueue.peekPendingTask();
        }

        if (pendingTask == null) {
            LogUtils.i(DownloadConstants.TAG, "[SCHEDULE] No pending task found");
            return;
        }
        LogUtils.i(DownloadConstants.TAG, "[SCHEDULE] Starting pending task: " + pendingTask.getUrl());
        startDownload(pendingTask, null);
    }

    private void startDownloadTask(VideoDownloadTask downloadTask, VideoTaskItem taskItem) {
        LogUtils.i(DownloadConstants.TAG, "startDownloadTask() called for URL: " + taskItem.getUrl());
        if (downloadTask != null) {
            downloadTask.setDownloadTaskListener(new IDownloadTaskListener() {
                @Override
                public void onTaskStart(String url) {
                    LogUtils.i(DownloadConstants.TAG, "onTaskStart: " + url);
                    taskItem.setTaskState(VideoTaskState.START);
                    mVideoDownloadHandler.obtainMessage(DownloadConstants.MSG_DOWNLOAD_START, taskItem).sendToTarget();
                    tryStartNextPendingTask();
                }

                @Override
                public void onTaskProgress(float percent, long cachedSize, long totalSize, float speed) {
                    if (!taskItem.isPaused() && !taskItem.isErrorState() && !taskItem.isSuccessState()) {
                        taskItem.setTaskState(VideoTaskState.DOWNLOADING);
                        taskItem.setPercent(percent);
                        taskItem.setSpeed(speed);
                        taskItem.setDownloadSize(cachedSize);
                        taskItem.setTotalSize(totalSize);
                        mVideoDownloadHandler.obtainMessage(DownloadConstants.MSG_DOWNLOAD_PROCESSING, taskItem).sendToTarget();
                    }
                }

                @Override
                public void onTaskProgressForM3U8(float percent, long cachedSize, int curTs, int totalTs, float speed) {
                    if (!taskItem.isPaused() && !taskItem.isErrorState() && !taskItem.isSuccessState()) {
                        taskItem.setTaskState(VideoTaskState.DOWNLOADING);
                        taskItem.setPercent(percent);
                        taskItem.setSpeed(speed);
                        taskItem.setDownloadSize(cachedSize);
                        taskItem.setCurTs(curTs);
                        taskItem.setTotalTs(totalTs);
                        mVideoDownloadHandler.obtainMessage(DownloadConstants.MSG_DOWNLOAD_PROCESSING, taskItem).sendToTarget();
                    }
                }

                @Override
                public void onTaskPaused() {
                    LogUtils.i(DownloadConstants.TAG, "onTaskPaused");
                    if (!taskItem.isErrorState() && !taskItem.isSuccessState()) {
                        taskItem.setTaskState(VideoTaskState.PAUSE);
                        taskItem.setPaused(true);
                        mVideoDownloadHandler.obtainMessage(DownloadConstants.MSG_DOWNLOAD_PAUSE, taskItem).sendToTarget();
                    }
                    tryStartNextPendingTask();
                }

                @Override
                public void onTaskFinished(long totalSize) {
                    LogUtils.i(DownloadConstants.TAG, "onTaskFinished: " + totalSize);
                    if (taskItem.getTaskState() != VideoTaskState.SUCCESS) {
                        taskItem.setTaskState(VideoTaskState.SUCCESS);
                        taskItem.setDownloadSize(totalSize);
                        taskItem.setIsCompleted(true);
                        taskItem.setPercent(100f);
                        if (taskItem.isHlsType()) {
                            taskItem.setFilePath(taskItem.getSaveDir() + File.separator + taskItem.getFileHash() + "_" + VideoDownloadUtils.LOCAL_M3U8);
                            taskItem.setFileName(taskItem.getFileHash() + "_" + VideoDownloadUtils.LOCAL_M3U8);
                        } else {
                            String extension = getFileExtensionFromMimeType(taskItem.getMimeType());
                            taskItem.setFilePath(taskItem.getSaveDir() + File.separator + taskItem.getFileHash() + extension);
                            taskItem.setFileName(taskItem.getFileHash() + extension);
                        }
                        mVideoDownloadHandler.obtainMessage(DownloadConstants.MSG_DOWNLOAD_SUCCESS, taskItem).sendToTarget();
                        mVideoDownloadHandler.removeMessages(DownloadConstants.MSG_DOWNLOAD_PROCESSING);
                    }
                    removeDownloadQueue(taskItem);
                    tryStartNextPendingTask();
                }

                @Override
                public void onTaskFailed(Throwable e) {
                    LogUtils.e(DownloadConstants.TAG, "onTaskFailed: " + e.getMessage());
                    LogUtils.e(DownloadConstants.TAG, "  Exception type: " + e.getClass().getName());
                    LogUtils.e(DownloadConstants.TAG, "  Stack trace: " + android.util.Log.getStackTraceString(e));
                    if (!taskItem.isSuccessState()) {
                        int errorCode = DownloadExceptionUtils.getErrorCode(e);
                        LogUtils.e(DownloadConstants.TAG, "  Error code: " + errorCode);
                        taskItem.setErrorCode(errorCode);
                        taskItem.setTaskState(VideoTaskState.ERROR);
                        mVideoDownloadHandler.obtainMessage(DownloadConstants.MSG_DOWNLOAD_ERROR, taskItem).sendToTarget();
                        mVideoDownloadHandler.removeMessages(DownloadConstants.MSG_DOWNLOAD_PROCESSING);
                    }
                }
            });

            LogUtils.i(DownloadConstants.TAG, "Calling downloadTask.startDownload()");
            try {
                downloadTask.startDownload();
                LogUtils.i(DownloadConstants.TAG, "downloadTask.startDownload() returned");
            } catch (Exception e) {
                LogUtils.e(DownloadConstants.TAG, "Exception in downloadTask.startDownload(): " + e.getMessage() + "\n" + android.util.Log.getStackTraceString(e));
                throw e;
            }
        }
    }

    public String getDownloadPath() {
        if (mConfig != null) {
            return mConfig.getCacheRoot();
        }
        return null;
    }

    public void deleteAllVideoFiles() {
        try {
            VideoStorageUtils.clearVideoCacheDir();
            mVideoItemTaskMap.clear();
            mVideoDownloadTaskMap.clear();
            mVideoDownloadHandler.obtainMessage(DownloadConstants.MSG_DELETE_ALL_FILES).sendToTarget();
        } catch (Exception e) {
            LogUtils.w(DownloadConstants.TAG, "clearVideoCacheDir failed, exception = " + e.getMessage());
        }
    }

    public void pauseAllDownloadTasks() {
        synchronized (mQueueLock) {
            List<VideoTaskItem> taskList = mVideoDownloadQueue.getDownloadList();
            LogUtils.i(DownloadConstants.TAG, "pauseAllDownloadTasks queue size="+taskList.size());
            List<String> pausedUrlList = new ArrayList<>();
            for (VideoTaskItem taskItem : taskList) {
                if (taskItem.isPendingTask()) {
                    mVideoDownloadQueue.remove(taskItem);
                    taskItem.setTaskState(VideoTaskState.PAUSE);
                    mVideoItemTaskMap.put(taskItem.getUrl(), taskItem);
                    mVideoDownloadHandler.obtainMessage(DownloadConstants.MSG_DOWNLOAD_PAUSE, taskItem).sendToTarget();
                } else {
                    pausedUrlList.add(taskItem.getUrl());
                }
            }
            pauseDownloadTask(pausedUrlList);
        }
    }

    public void pauseDownloadTask(List<String> urlList) {
        for (String url : urlList) {
            pauseDownloadTask(url);
        }
    }

    public void pauseDownloadTask(String videoUrl) {
        if (mVideoItemTaskMap.containsKey(videoUrl)) {
            VideoTaskItem taskItem = mVideoItemTaskMap.get(videoUrl);
            pauseDownloadTask(taskItem);
        }
    }

    public void pauseDownloadTask(VideoTaskItem taskItem) {
        if (taskItem == null || TextUtils.isEmpty(taskItem.getUrl()))
            return;
        synchronized (mQueueLock) {
            mVideoDownloadQueue.remove(taskItem);
        }
        String url = taskItem.getUrl();
        VideoDownloadTask task = mVideoDownloadTaskMap.get(url);
        if (task != null) {
            task.pauseDownload();
        }
    }

    public void resumeDownload(String videoUrl) {
        if (mVideoItemTaskMap.containsKey(videoUrl)) {
            VideoTaskItem taskItem = mVideoItemTaskMap.get(videoUrl);
            startDownload(taskItem);
        }
    }

    //Delete one task
    public void deleteVideoTask(VideoTaskItem taskItem, boolean shouldDeleteSourceFile) {
        String cacheFilePath = getDownloadPath();
        if (!TextUtils.isEmpty(cacheFilePath)) {
            pauseDownloadTask(taskItem);
            String saveName = VideoDownloadUtils.computeMD5(taskItem.getUrl());
            File file = new File(cacheFilePath + File.separator + saveName);
            WorkerThreadHandler.submitRunnableTask(() -> mVideoDatabaseHelper.deleteDownloadItemByUrl(taskItem));
            try {
                if (shouldDeleteSourceFile) {
                    VideoStorageUtils.delete(file);
                }
                if (mVideoDownloadTaskMap.containsKey(taskItem.getUrl())) {
                    mVideoDownloadTaskMap.remove(taskItem.getUrl());
                }
                taskItem.reset();
                mVideoDownloadHandler.obtainMessage(DownloadConstants.MSG_DOWNLOAD_DEFAULT, taskItem).sendToTarget();
            } catch (Exception e) {
                LogUtils.w(DownloadConstants.TAG, "Delete file: " + file + " failed, exception=" + e.getMessage());
            }
        }
    }

    public void deleteVideoTask(String videoUrl, boolean shouldDeleteSourceFile) {
        if (mVideoItemTaskMap.containsKey(videoUrl)) {
            VideoTaskItem taskItem = mVideoItemTaskMap.get(videoUrl);
            deleteVideoTask(taskItem, shouldDeleteSourceFile);
            mVideoItemTaskMap.remove(videoUrl);
        }
    }

    public void deleteVideoTasks(List<String> urlList, boolean shouldDeleteSourceFile) {
        for (String url : urlList) {
            deleteVideoTask(url, shouldDeleteSourceFile);
        }
    }

    public void deleteVideoTasks(VideoTaskItem[] taskItems, boolean shouldDeleteSourceFile) {
        String cacheFilePath = getDownloadPath();
        if (!TextUtils.isEmpty(cacheFilePath)) {
            for (VideoTaskItem item : taskItems) {
                deleteVideoTask(item, shouldDeleteSourceFile);
            }
        }
    }

    private void removeDownloadQueue(VideoTaskItem taskItem) {
        synchronized (mQueueLock) {
            mVideoDownloadQueue.remove(taskItem);
            LogUtils.w(DownloadConstants.TAG, "removeDownloadQueue size=" + mVideoDownloadQueue.size() + "," + mVideoDownloadQueue.getDownloadingCount() + "," + mVideoDownloadQueue.getPendingCount());
            int pendingCount = mVideoDownloadQueue.getPendingCount();
            int downloadingCount = mVideoDownloadQueue.getDownloadingCount();
            while (downloadingCount < mConfig.getConcurrentCount() && pendingCount > 0) {
                if (mVideoDownloadQueue.size() == 0)
                    break;
                if (downloadingCount == mVideoDownloadQueue.size())
                    break;
                VideoTaskItem item1 = mVideoDownloadQueue.peekPendingTask();
                startDownload(item1, null);
                pendingCount--;
                downloadingCount++;
            }
        }
    }


    class VideoDownloadHandler extends Handler {

        public VideoDownloadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == DownloadConstants.MSG_FETCH_DOWNLOAD_INFO) {
                dispatchDownloadInfos();
            } else if (msg.what == DownloadConstants.MSG_DELETE_ALL_FILES) {
                //删除数据库中所有记录
                WorkerThreadHandler.submitRunnableTask(() -> mVideoDatabaseHelper.deleteAllDownloadInfos());
            } else {
                dispatchDownloadMessage(msg.what, (VideoTaskItem) msg.obj);
            }
        }

        private void dispatchDownloadInfos() {
            WorkerThreadHandler.submitRunnableTask(() -> {
                List<VideoTaskItem> taskItems = mVideoDatabaseHelper.getDownloadInfos();
                for (VideoTaskItem taskItem : taskItems) {
                    if (isTransientCleanedM3U8Task(taskItem)) {
                        LogUtils.w(DownloadConstants.TAG, "[MERGE] skip transient cleaned m3u8 task. url=" + taskItem.getUrl());
                        mVideoDatabaseHelper.deleteDownloadItemByUrl(taskItem);
                        continue;
                    }
                    if (shouldHandleMergeRecovery(taskItem)) {
                        if (hasValidMergedOutput(taskItem)) {
                            taskItem.setTaskState(VideoTaskState.SUCCESS);
                            taskItem.setIsCompleted(true);
                            taskItem.setPercent(100f);
                            mVideoItemTaskMap.put(taskItem.getUrl(), taskItem);
                            continue;
                        }
                        if (!hasMergeSource(taskItem)) {
                            taskItem.setIsCompleted(false);
                            if (isMergeReadyPercent(taskItem)) {
                                taskItem.setErrorCode(TsMergeService.MERGE_ERROR_SOURCE_NOT_FOUND);
                                taskItem.setTaskState(VideoTaskState.ERROR);
                            } else {
                                taskItem.setTaskState(VideoTaskState.PAUSE);
                                taskItem.setPaused(true);
                                taskItem.setErrorCode(0);
                            }
                            LogUtils.w(DownloadConstants.TAG, "[MERGE] restore task without output. url=" + taskItem.getUrl() + ", filePath=" + taskItem.getFilePath() + ", percent=" + taskItem.getPercent());
                            mVideoItemTaskMap.put(taskItem.getUrl(), taskItem);
                            markDownloadFinishEvent(taskItem);
                            continue;
                        }
                        taskItem.setIsCompleted(false);
                        LogUtils.i(DownloadConstants.TAG, "[MERGE] restore task needs merge retry. url=" + taskItem.getUrl() + ", saveDir=" + taskItem.getSaveDir());
                        doMergeTs(taskItem, taskItem1 -> {
                            mVideoItemTaskMap.put(taskItem1.getUrl(), taskItem1);
                            markDownloadFinishEvent(taskItem1);
                        });
                    } else {
                        normalizeRestoredTaskState(taskItem);
                        mVideoItemTaskMap.put(taskItem.getUrl(), taskItem);
                    }
                }

                for (IDownloadInfosCallback callback : mDownloadInfoCallbacks) {
                    callback.onDownloadInfos(taskItems);
                }
            });
        }

        private boolean isTransientCleanedM3U8Task(VideoTaskItem taskItem) {
            if (taskItem == null || TextUtils.isEmpty(taskItem.getUrl())) {
                return false;
            }
            String lower = taskItem.getUrl().toLowerCase();
            if (!lower.startsWith("http://127.0.0.1:") || !lower.contains("/cleaned/") || !lower.contains(".m3u8")) {
                return false;
            }
            if (!TextUtils.isEmpty(taskItem.getSaveDir())) {
                File saveDir = new File(taskItem.getSaveDir());
                if (saveDir.exists() && saveDir.isDirectory()) {
                    return false;
                }
            }
            return true;
        }

        private boolean shouldHandleMergeRecovery(VideoTaskItem taskItem) {
            if (taskItem == null || mConfig == null || !mConfig.shouldM3U8Merged() || !taskItem.isHlsType()) {
                return false;
            }
            return taskItem.isCompleted()
                    || taskItem.isSuccessState()
                    || isMergeReadyPercent(taskItem);
        }

        private void normalizeRestoredTaskState(VideoTaskItem taskItem) {
            if (taskItem == null) {
                return;
            }
            if (taskItem.isSuccessState() || taskItem.isCompleted()) {
                return;
            }
            int state = taskItem.getTaskState();
            if (state == VideoTaskState.DOWNLOADING || state == VideoTaskState.START || state == VideoTaskState.PREPARE) {
                taskItem.setTaskState(VideoTaskState.PAUSE);
                taskItem.setPaused(true);
                return;
            }
            if (state == VideoTaskState.ERROR && !isMergeReadyPercent(taskItem)) {
                taskItem.setTaskState(VideoTaskState.PAUSE);
                taskItem.setPaused(true);
                taskItem.setErrorCode(0);
            }
        }

        private boolean hasValidMergedOutput(VideoTaskItem taskItem) {
            if (taskItem == null || TextUtils.isEmpty(taskItem.getFilePath())) {
                return false;
            }
            String filePath = taskItem.getFilePath();
            if (filePath.endsWith(File.separator + VideoDownloadUtils.LOCAL_M3U8)
                    || filePath.endsWith(File.separator + VideoDownloadUtils.LOCAL_M3U8_WITH_KEY)
                    || filePath.endsWith("_" + VideoDownloadUtils.LOCAL_M3U8)
                    || filePath.endsWith("_" + VideoDownloadUtils.LOCAL_M3U8_WITH_KEY)) {
                return false;
            }
            File outputFile = new File(filePath);
            return outputFile.exists() && outputFile.isFile() && outputFile.length() > 0;
        }

        private boolean hasMergeSource(VideoTaskItem taskItem) {
            if (taskItem == null || TextUtils.isEmpty(taskItem.getSaveDir())) {
                return false;
            }
            File saveDir = new File(taskItem.getSaveDir());
            if (!saveDir.exists() || !saveDir.isDirectory()) {
                return false;
            }
            if (!TextUtils.isEmpty(taskItem.getFilePath())) {
                File currentFile = new File(taskItem.getFilePath());
                if (currentFile.exists() && currentFile.isFile()) {
                    return true;
                }
            }
            String fileHash = taskItem.getFileHash();
            if (TextUtils.isEmpty(fileHash) && !TextUtils.isEmpty(taskItem.getUrl())) {
                fileHash = VideoDownloadUtils.computeMD5(taskItem.getUrl());
                taskItem.setFileHash(fileHash);
            }
            if (!TextUtils.isEmpty(fileHash)) {
                File localM3u8 = new File(saveDir, fileHash + "_" + VideoDownloadUtils.LOCAL_M3U8);
                if (localM3u8.exists() && localM3u8.isFile()) {
                    return true;
                }
                File keyLocalM3u8 = new File(saveDir, fileHash + "_" + VideoDownloadUtils.LOCAL_M3U8_WITH_KEY);
                if (keyLocalM3u8.exists() && keyLocalM3u8.isFile()) {
                    return true;
                }
            }
            File[] playlistFiles = saveDir.listFiles((dir, name) -> name != null
                    && (name.endsWith("_" + VideoDownloadUtils.LOCAL_M3U8)
                    || name.endsWith("_" + VideoDownloadUtils.LOCAL_M3U8_WITH_KEY)));
            return playlistFiles != null && playlistFiles.length > 0;
        }

        private void dispatchDownloadMessage(int msg, VideoTaskItem taskItem) {
            switch (msg) {
                case DownloadConstants.MSG_DOWNLOAD_DEFAULT:
                    handleOnDownloadDefault(taskItem);
                    break;
                case DownloadConstants.MSG_DOWNLOAD_PENDING:
                    handleOnDownloadPending(taskItem);
                    break;
                case DownloadConstants.MSG_DOWNLOAD_PREPARE:
                    handleOnDownloadPrepare(taskItem);
                    break;
                case DownloadConstants.MSG_DOWNLOAD_START:
                    handleOnDownloadStart(taskItem);
                    break;
                case DownloadConstants.MSG_DOWNLOAD_PROCESSING:
                    handleOnDownloadProcessing(taskItem);
                    break;
                case DownloadConstants.MSG_DOWNLOAD_PAUSE:
                    handleOnDownloadPause(taskItem);
                    break;
                case DownloadConstants.MSG_DOWNLOAD_ERROR:
                    handleOnDownloadError(taskItem);
                    break;
                case DownloadConstants.MSG_DOWNLOAD_SUCCESS:
                    handleOnDownloadSuccess(taskItem);
                    break;
            }
        }
    }

    private void handleOnDownloadDefault(VideoTaskItem taskItem) {
        mGlobalDownloadListener.onDownloadDefault(taskItem);
    }

    private void handleOnDownloadPending(VideoTaskItem taskItem) {
        mGlobalDownloadListener.onDownloadPending(taskItem);
    }

    private void handleOnDownloadPrepare(VideoTaskItem taskItem) {
        mGlobalDownloadListener.onDownloadPrepare(taskItem);
        markDownloadInfoAddEvent(taskItem);
        updateForegroundService();
    }

    private void handleOnDownloadStart(VideoTaskItem taskItem) {
        mGlobalDownloadListener.onDownloadStart(taskItem);
        updateForegroundService();
    }

    private void handleOnDownloadProcessing(VideoTaskItem taskItem) {
        mGlobalDownloadListener.onDownloadProgress(taskItem);
        markDownloadProgressInfoUpdateEvent(taskItem);
    }

    private void handleOnDownloadPause(VideoTaskItem taskItem) {
        mGlobalDownloadListener.onDownloadPause(taskItem);
        removeDownloadQueue(taskItem);
        updateForegroundService();
    }

    private void handleOnDownloadError(VideoTaskItem taskItem) {
        mGlobalDownloadListener.onDownloadError(taskItem);
        removeDownloadQueue(taskItem);
        updateForegroundService();
    }

    private void handleOnDownloadSuccess(VideoTaskItem taskItem) {
        removeDownloadQueue(taskItem);

        LogUtils.i(DownloadConstants.TAG, "handleOnDownloadSuccess shouldM3U8Merged="+mConfig.shouldM3U8Merged() + ", isHlsType="+taskItem.isHlsType());
        if (mConfig.shouldM3U8Merged() && taskItem.isHlsType()) {
            if (taskItem.getPercent() < 99.9f) {
                taskItem.setPercent(99.9f);
            }
            taskItem.setTaskState(VideoTaskState.PREPARE);
            taskItem.setPaused(false);
            mGlobalDownloadListener.onDownloadPrepare(taskItem);
            markDownloadProgressInfoUpdateEvent(taskItem);
            if (!TextUtils.isEmpty(taskItem.getUrl())) {
                mMergingTaskUrls.add(taskItem.getUrl());
            }
            updateForegroundService();
            doMergeTs(taskItem, taskItem1 -> {
                try {
                    if (taskItem1 != null && taskItem1.getErrorCode() != 0) {
                        LogUtils.e(DownloadConstants.TAG, "m3u8 merge failed, errorCode=" + taskItem1.getErrorCode());
                        mGlobalDownloadListener.onDownloadError(taskItem1);
                        markDownloadFinishEvent(taskItem1);
                        return;
                    }
                    mGlobalDownloadListener.onDownloadSuccess(taskItem1);
                    markDownloadFinishEvent(taskItem1);
                } finally {
                    if (!TextUtils.isEmpty(taskItem.getUrl())) {
                        mMergingTaskUrls.remove(taskItem.getUrl());
                    }
                    updateForegroundService();
                }
            });
        } else {
            mGlobalDownloadListener.onDownloadSuccess(taskItem);
            markDownloadFinishEvent(taskItem);
            updateForegroundService();
        }
    }


    private void doMergeTs(VideoTaskItem taskItem, IM3U8MergeResultListener listener) {
        mTsMergeService.merge(taskItem, listener::onCallback);
    }


    private void markDownloadInfoAddEvent(VideoTaskItem taskItem) {
        WorkerThreadHandler.submitRunnableTask(() -> mVideoDatabaseHelper.markDownloadInfoAddEvent(taskItem));
    }

    private void markDownloadProgressInfoUpdateEvent(VideoTaskItem taskItem) {
        long currentTime = System.currentTimeMillis();
        if (taskItem.getLastUpdateTime() + 1000 < currentTime) {
            WorkerThreadHandler.submitRunnableTask(() -> mVideoDatabaseHelper.markDownloadProgressInfoUpdateEvent(taskItem));
            taskItem.setLastUpdateTime(currentTime);
        }
    }

    private void markDownloadFinishEvent(VideoTaskItem taskItem) {
        WorkerThreadHandler.submitRunnableTask(() -> mVideoDatabaseHelper.markDownloadProgressInfoUpdateEvent(taskItem));
    }

    private void updateForegroundService() {
        Context context = ContextUtils.getApplicationContext();
        if (context == null) {
            return;
        }
        if (hasActiveTasks()) {
            DownloadService.start(context);
        } else {
            DownloadService.stop(context);
        }
    }

    private boolean hasActiveTasks() {
        synchronized (mQueueLock) {
            if (mVideoDownloadQueue.getDownloadingCount() > 0 || mVideoDownloadQueue.getPendingCount() > 0) {
                return true;
            }
        }
        if (!mMergingTaskUrls.isEmpty()) {
            return true;
        }
        for (VideoTaskItem item : mVideoItemTaskMap.values()) {
            if (item == null) {
                continue;
            }
            int state = item.getTaskState();
            if (state != VideoTaskState.SUCCESS && state != VideoTaskState.ERROR && state != VideoTaskState.PAUSE) {
                return true;
            }
        }
        return false;
    }

    private boolean hasValidMergedOutputForStart(VideoTaskItem taskItem) {
        if (taskItem == null || TextUtils.isEmpty(taskItem.getFilePath())) {
            return false;
        }
        String filePath = taskItem.getFilePath();
        if (filePath.endsWith(File.separator + VideoDownloadUtils.LOCAL_M3U8)
                || filePath.endsWith(File.separator + VideoDownloadUtils.LOCAL_M3U8_WITH_KEY)
                || filePath.endsWith("_" + VideoDownloadUtils.LOCAL_M3U8)
                || filePath.endsWith("_" + VideoDownloadUtils.LOCAL_M3U8_WITH_KEY)) {
            return false;
        }
        File outputFile = new File(filePath);
        return outputFile.exists() && outputFile.isFile() && outputFile.length() > 0;
    }

    private boolean hasMergeSourceForStart(VideoTaskItem taskItem) {
        if (taskItem == null || TextUtils.isEmpty(taskItem.getSaveDir())) {
            return false;
        }
        File saveDir = new File(taskItem.getSaveDir());
        if (!saveDir.exists() || !saveDir.isDirectory()) {
            return false;
        }
        String fileHash = taskItem.getFileHash();
        if (TextUtils.isEmpty(fileHash) && !TextUtils.isEmpty(taskItem.getUrl())) {
            fileHash = VideoDownloadUtils.computeMD5(taskItem.getUrl());
            taskItem.setFileHash(fileHash);
        }
        if (!TextUtils.isEmpty(fileHash)) {
            File localM3u8 = new File(saveDir, fileHash + "_" + VideoDownloadUtils.LOCAL_M3U8);
            if (localM3u8.exists() && localM3u8.isFile()) {
                return true;
            }
            File keyLocalM3u8 = new File(saveDir, fileHash + "_" + VideoDownloadUtils.LOCAL_M3U8_WITH_KEY);
            if (keyLocalM3u8.exists() && keyLocalM3u8.isFile()) {
                return true;
            }
        }
        File[] playlistFiles = saveDir.listFiles((dir, name) -> name != null
                && (name.endsWith("_" + VideoDownloadUtils.LOCAL_M3U8)
                || name.endsWith("_" + VideoDownloadUtils.LOCAL_M3U8_WITH_KEY)));
        return playlistFiles != null && playlistFiles.length > 0;
    }

    private boolean isMergeReadyPercent(VideoTaskItem taskItem) {
        if (taskItem == null) {
            return false;
        }
        float percent = taskItem.getPercent();
        return !Float.isNaN(percent) && !Float.isInfinite(percent) && percent >= 99.9f;
    }

    /**
     * 将 headers Map 序列化为 JSON 字符串
     */
    private static String headersToJson(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        try {
            JSONObject json = new JSONObject();
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
            return json.toString();
        } catch (Exception e) {
            LogUtils.w(DownloadConstants.TAG, "headersToJson failed: " + e.getMessage());
            return null;
        }
    }
}
