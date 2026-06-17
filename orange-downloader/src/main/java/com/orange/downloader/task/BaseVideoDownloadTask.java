package com.orange.downloader.task;

import com.orange.downloader.common.DownloadConstants;
import com.orange.downloader.model.VideoTaskItem;
import com.orange.downloader.utils.HttpUtils;
import com.orange.downloader.utils.LogUtils;
import com.orange.downloader.utils.VideoDownloadUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BaseVideoDownloadTask extends VideoDownloadTask {

    private long mTotalLength;

    public BaseVideoDownloadTask(VideoTaskItem taskItem, Map<String, String> headers) {
        super(taskItem, headers);
        if (mHeaders == null) {
            mHeaders = new HashMap<>();
        }
        mCurrentCachedSize = taskItem.getDownloadSize();
        mTotalLength = taskItem.getTotalSize();
    }

    @Override
    public void startDownload() {
        mDownloadTaskListener.onTaskStart(mTaskItem.getUrl());
        startDownload(mCurrentCachedSize);
    }

    private void startDownload(long curLength) {
        if (mTaskItem.isCompleted()) {
            LogUtils.i(DownloadConstants.TAG, "BaseVideoDownloadTask local file.");
            notifyDownloadFinish();
            return;
        }
        mCurrentCachedSize = curLength;
        mDownloadExecutor = new ThreadPoolExecutor(
                THREAD_COUNT, THREAD_COUNT, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.DiscardOldestPolicy());
        mDownloadExecutor.execute(() -> {
            File videoFile;
            try {
                videoFile = new File(mSaveDir, mSaveName + VideoDownloadUtils.VIDEO_SUFFIX);
                if (!videoFile.exists()) {
                    videoFile.createNewFile();
                    mCurrentCachedSize = 0;
                } else {
                    mCurrentCachedSize = videoFile.length();
                }
            } catch (Exception e) {
                LogUtils.w(DownloadConstants.TAG, "BaseDownloadTask createNewFile failed, exception=" + e.getMessage());
                return;
            }

            InputStream inputStream = null;
            RandomAccessFile randomAccessFile = null;
            try {
                inputStream = getResponseBody(mFinalUrl, mCurrentCachedSize, mTotalLength);
                byte[] buf = new byte[BUFFER_SIZE];

                randomAccessFile = new RandomAccessFile(videoFile.getAbsolutePath(), "rw");
                randomAccessFile.seek(mCurrentCachedSize);
                int readLength;
                while ((readLength = inputStream.read(buf)) != -1) {
                    if (mCurrentCachedSize + readLength > mTotalLength) {
                        randomAccessFile.write(buf, 0, (int) (mTotalLength - mCurrentCachedSize));
                        mCurrentCachedSize = mTotalLength;
                    } else {
                        randomAccessFile.write(buf, 0, readLength);
                        mCurrentCachedSize += readLength;
                    }
                    notifyDownloadProgress();
                }
            } catch (Exception e) {
                LogUtils.w(DownloadConstants.TAG, "FAILED, exception=" + e.getMessage());
                e.printStackTrace();
                notifyDownloadError(e);
            } finally {
                VideoDownloadUtils.close(inputStream);
                VideoDownloadUtils.close(randomAccessFile);
            }
        });
    }

    @Override
    public void pauseDownload() {
        if (mDownloadExecutor != null && !mDownloadExecutor.isShutdown()) {
            mDownloadExecutor.shutdownNow();
            notifyOnTaskPaused();
        }
    }

    @Override
    public void resumeDownload() {
        startDownload(mCurrentCachedSize);
    }

    private void notifyDownloadProgress() {
        if (mCurrentCachedSize >= mTotalLength) {
            mDownloadTaskListener.onTaskProgress(100, mTotalLength, mTotalLength, mSpeed);
            mPercent = 100.0f;
            notifyDownloadFinish();
        } else {
            float percent = mCurrentCachedSize * 1.0f * 100 / mTotalLength;
            if (!VideoDownloadUtils.isFloatEqual(percent, mPercent)) {
                long nowTime = System.currentTimeMillis();
                if (mCurrentCachedSize > mLastCachedSize && nowTime > mLastInvokeTime) {
                    mSpeed = (mCurrentCachedSize - mLastCachedSize) * 1000 * 1.0f / (nowTime - mLastInvokeTime);
                }
                mDownloadTaskListener.onTaskProgress(percent, mCurrentCachedSize, mTotalLength, mSpeed);
                mPercent = percent;
                mLastInvokeTime = nowTime;
                mLastCachedSize = mCurrentCachedSize;
            }
        }
    }

    private void notifyDownloadError(Exception e) {
        notifyOnTaskFailed(e);
    }

    private void notifyDownloadFinish() {
        synchronized (mDownloadLock) {
            if (!mDownloadFinished) {
                // 单线程下载也需要重命名扩展名
                renameVideoFileToCorrectExtension();
                mDownloadTaskListener.onTaskFinished(mTotalLength);
                mDownloadFinished = true;
            }
        }
    }

    /**
     * 将临时 .video 文件重命名为正确的扩展名
     */
    private void renameVideoFileToCorrectExtension() {
        try {
            String tempFileName = mSaveName + VideoDownloadUtils.VIDEO_SUFFIX;
            File tempFile = new File(mSaveDir, tempFileName);
            
            if (!tempFile.exists()) {
                LogUtils.w(DownloadConstants.TAG, "[MP4_RENAME] Temp file not found: " + tempFile.getAbsolutePath());
                return;
            }
            
            String extension = getCorrectExtension();
            
            // 优先使用 title 作为文件名，如果没有 title 则使用 mSaveName (md5)
            String targetName = mTaskItem.getTitle();
            if (targetName != null && !targetName.isEmpty()) {
                targetName = targetName.replaceAll("[\\\\/:*?\"<>|]", "_"); // 移除非法字符
            } else {
                targetName = mSaveName;
            }
            
            String correctFileName = targetName + extension;
            File correctFile = new File(mSaveDir, correctFileName);
            
            // 处理同名文件冲突
            int counter = 1;
            while (correctFile.exists() && !correctFile.getAbsolutePath().equals(tempFile.getAbsolutePath())) {
                correctFileName = targetName + "_" + counter + extension;
                correctFile = new File(mSaveDir, correctFileName);
                counter++;
            }
            
            boolean renamed = tempFile.renameTo(correctFile);
            LogUtils.i(DownloadConstants.TAG, "[MP4_RENAME] Rename result: " + renamed + ", from=" + tempFileName + ", to=" + correctFileName);
            if (renamed) {
                mTaskItem.setFileName(correctFileName);
                mTaskItem.setFilePath(correctFile.getAbsolutePath());
            } else {
                mTaskItem.setFileName(tempFileName);
                mTaskItem.setFilePath(tempFile.getAbsolutePath());
            }
        } catch (Exception e) {
            LogUtils.e(DownloadConstants.TAG, "[MP4_RENAME] Error renaming file: " + e.getMessage());
        }
    }

    private String getCorrectExtension() {
        String mimeType = mTaskItem.getMimeType();
        if (mimeType != null) {
            if (mimeType.contains("mp4") || mimeType.contains("MP4")) return ".mp4";
            else if (mimeType.contains("webm")) return ".webm";
            else if (mimeType.contains("quicktime") || mimeType.contains("mov")) return ".mov";
            else if (mimeType.contains("3gp")) return ".3gp";
            else if (mimeType.contains("mkv")) return ".mkv";
        }
        String url = mTaskItem.getUrl();
        if (url != null) {
            String lowerUrl = url.toLowerCase();
            if (lowerUrl.contains(".mp4")) return ".mp4";
            if (lowerUrl.contains(".webm")) return ".webm";
            if (lowerUrl.contains(".mov")) return ".mov";
            if (lowerUrl.contains(".3gp")) return ".3gp";
            if (lowerUrl.contains(".mkv")) return ".mkv";
        }
        return ".mp4";
    }

    private InputStream getResponseBody(String url, long start, long end) throws IOException {
        if (end == mTotalLength) {
            mHeaders.put("Range", "bytes=" + start + "-");
        } else {
            mHeaders.put("Range", "bytes=" + start + "-" + end);
        }
        HttpURLConnection connection = HttpUtils.getConnection(url, mHeaders, VideoDownloadUtils.getDownloadConfig().shouldIgnoreCertErrors());
        return connection.getInputStream();
    }

}
