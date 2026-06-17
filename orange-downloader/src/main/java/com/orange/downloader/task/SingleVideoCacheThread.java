package com.orange.downloader.task;

import android.os.Handler;

import com.orange.downloader.common.DownloadConstants;
import com.orange.downloader.listener.IVideoCacheListener;
import com.orange.downloader.model.VideoRange;
import com.orange.downloader.utils.HttpUtils;
import com.orange.downloader.utils.LogUtils;
import com.orange.downloader.utils.VideoDownloadUtils;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

public class SingleVideoCacheThread implements Runnable {

    private final VideoRange mRange;
    private final String mUrl;
    private final Map<String, String> mHeaders;
    private final long mTotalSize;
    private final File mSaveDir;
    private final String mMd5;
    private IVideoCacheListener mListener;
    private boolean mIsRunning = true;
    private Handler mMsgHandler;
    private int mId;

    public SingleVideoCacheThread(String url, Map<String, String> headers, VideoRange range, long totalSize, String saveDir, String saveName) {
        mUrl = url;
        if (headers == null) {
            headers = new HashMap<>();
        }
        mHeaders = headers;
        mRange = range;
        mTotalSize = totalSize;
        mMd5 = saveName;  // 使用传入的saveName，保持与父类一致
        mSaveDir = new File(saveDir);
        if (!mSaveDir.exists()) {
            mSaveDir.mkdirs();  // 使用 mkdirs() 创建所有必需的父目录
        }
    }

    public void setId(int id) {
        mId = id;
    }

    public void setHandler(Handler handler) {
        mMsgHandler = handler;
    }

    public void setCacheListener(IVideoCacheListener listener) {
        mListener = listener;
    }

    public boolean isRunning() {
        return mIsRunning;
    }

    public void pause() {
        LogUtils.i(DownloadConstants.TAG, "Pause task");
        mIsRunning = false;
    }

    @Override
    public void run() {
        if (!mIsRunning) {
            return;
        }
        downloadVideo();
    }

    private void downloadVideo() {
        LogUtils.i(DownloadConstants.TAG, "SingleVideoCacheThread.downloadVideo() start, ID=" + mId);
        LogUtils.i(DownloadConstants.TAG, "  SaveDir: " + mSaveDir.getAbsolutePath());
        LogUtils.i(DownloadConstants.TAG, "  SaveDir exists: " + mSaveDir.exists());
        LogUtils.i(DownloadConstants.TAG, "  SaveDir canWrite: " + mSaveDir.canWrite());
        
        File videoFile;
        try {
            videoFile = new File(mSaveDir, mMd5 + VideoDownloadUtils.VIDEO_SUFFIX);
            LogUtils.i(DownloadConstants.TAG, "  Video file path: " + videoFile.getAbsolutePath());
            LogUtils.i(DownloadConstants.TAG, "  Video file exists: " + videoFile.exists());
            LogUtils.i(DownloadConstants.TAG, "  SaveDir exists: " + mSaveDir.exists());
            LogUtils.i(DownloadConstants.TAG, "  SaveDir canWrite: " + mSaveDir.canWrite());
            LogUtils.i(DownloadConstants.TAG, "  SaveDir path: " + mSaveDir.getAbsolutePath());
            
            if (!videoFile.exists()) {
                boolean createResult = videoFile.createNewFile();
                LogUtils.i(DownloadConstants.TAG, "  createNewFile() result: " + createResult);
                LogUtils.i(DownloadConstants.TAG, "  Video file exists after create: " + videoFile.exists());
                
                if (!createResult) {
                    // 文件创建失败，记录详细错误信息
                    LogUtils.e(DownloadConstants.TAG, "  Failed to create file!");
                    LogUtils.e(DownloadConstants.TAG, "  Parent dir exists: " + videoFile.getParentFile().exists());
                    LogUtils.e(DownloadConstants.TAG, "  Parent dir canWrite: " + videoFile.getParentFile().canWrite());
                    throw new java.io.IOException("Failed to create file: " + videoFile.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            LogUtils.e(DownloadConstants.TAG, "Error creating video file: " + e.getMessage() + "\n" + android.util.Log.getStackTraceString(e));
            notifyOnFailed(e);
            return;
        }

        long requestStart = mRange.getStart();
        long requestEnd = mRange.getEnd();
        if (requestStart - 10 > 0) {
            requestStart = requestStart - 10;
        }
        if (requestEnd + 10 < mTotalSize) {
            requestEnd = requestEnd + 10;
        }
        long rangeGap = requestEnd - requestStart;
        mHeaders.put("Range", "bytes=" + requestStart + "-" + requestEnd);
        LogUtils.i(DownloadConstants.TAG, "  Request range: " + requestStart + "-" + requestEnd + " (gap=" + rangeGap + ")");
        
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        RandomAccessFile randomAccessFile = null;

        try {
            LogUtils.i(DownloadConstants.TAG, "  Opening RandomAccessFile");
            randomAccessFile = new RandomAccessFile(videoFile.getAbsoluteFile(), "rw");
            randomAccessFile.seek(requestStart);
            LogUtils.i(DownloadConstants.TAG, "  RandomAccessFile opened, seeking to " + requestStart);
            
            long cachedSize = 0;
            LogUtils.i(DownloadConstants.TAG, "Request range = " + mRange);
            LogUtils.i(DownloadConstants.TAG, "  Creating HTTP connection to: " + mUrl);
            connection = HttpUtils.getConnection(mUrl, mHeaders, VideoDownloadUtils.getDownloadConfig().shouldIgnoreCertErrors());
            LogUtils.i(DownloadConstants.TAG, "  HTTP connection created");
            
            inputStream = connection.getInputStream();
            LogUtils.i(DownloadConstants.TAG, "Receive response");

            byte[] buffer = new byte[VideoDownloadUtils.DEFAULT_BUFFER_SIZE];
            int readLength;
            while (mIsRunning && (readLength = inputStream.read(buffer)) != -1) {
                if (cachedSize + readLength > rangeGap) {
                    long read = rangeGap - cachedSize;
                    randomAccessFile.write(buffer, 0, (int)read);
                    cachedSize = rangeGap;
                } else {
                    randomAccessFile.write(buffer, 0, readLength);
                    cachedSize += readLength;
                }

                notifyOnProgress(cachedSize);

                if (cachedSize >= rangeGap) {
                    LogUtils.i(DownloadConstants.TAG, "Exceed cachedSize=" + cachedSize +", Range[start=" + requestStart +", end="+requestEnd+"]");
                    notifyOnRangeCompleted();
                    break;
                }
            }

            mIsRunning = false;
        } catch (Exception e) {
            LogUtils.e(DownloadConstants.TAG, "Error in downloadVideo: " + e.getMessage() + "\n" + android.util.Log.getStackTraceString(e));
            notifyOnFailed(e);
        } finally {
            mIsRunning = false;
            VideoDownloadUtils.close(inputStream);
            VideoDownloadUtils.close(randomAccessFile);
            HttpUtils.closeConnection(connection);
        }
    }

    private void notifyOnFailed(Exception e) {
        mMsgHandler.post(() -> mListener.onFailed(mRange, mId, e));
    }

    private void notifyOnProgress(long cachedSize) {
        mMsgHandler.post(() -> mListener.onProgress(mRange, mId, cachedSize));
    }

    private void notifyOnRangeCompleted() {
        mMsgHandler.post(() -> mListener.onRangeCompleted(mRange, mId));
    }
}
