package com.orange.downloader;

import com.orange.downloader.common.DownloadConstants;
import com.orange.downloader.model.VideoTaskItem;
import com.orange.downloader.model.VideoTaskState;
import com.orange.downloader.utils.LogUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Custom Download Queue.
 */
public class VideoDownloadQueue {
    private CopyOnWriteArrayList<VideoTaskItem> mQueue;

    public VideoDownloadQueue() {
        mQueue = new CopyOnWriteArrayList<>();
    }

    public List<VideoTaskItem> getDownloadList() {
        return mQueue;
    }

    //put it into queue
    public void offer(VideoTaskItem taskItem) {
        mQueue.add(taskItem);
        LogUtils.i(DownloadConstants.TAG, "[QUEUE] offer() - Task added, url=" + (taskItem != null ? taskItem.getUrl() : "null") + ", queueSize=" + mQueue.size());
    }

    //Remove Queue head item,
    //Return Next Queue head.
    public VideoTaskItem poll() {
        try {
            if (mQueue.size() >= 2) {
                mQueue.remove(0);
                return mQueue.get(0);
            } else if (mQueue.size() == 1) {
                mQueue.remove(0);
            }
        } catch (Exception e) {
            LogUtils.w(DownloadConstants.TAG, "DownloadQueue remove failed.");
        }
        return null;
    }

    public VideoTaskItem peek() {
        try {
            if (mQueue.size() >= 1) {
                return mQueue.get(0);
            }
        } catch (Exception e) {
            LogUtils.w(DownloadConstants.TAG, "DownloadQueue get failed.");
        }
        return null;
    }

    public boolean remove(VideoTaskItem taskItem) {
        if (contains(taskItem)) {
            boolean removed = mQueue.remove(taskItem);
            LogUtils.i(DownloadConstants.TAG, "[QUEUE] remove() - Task removed, url=" + (taskItem != null ? taskItem.getUrl() : "null") + ", queueSize=" + mQueue.size());
            return removed;
        }
        LogUtils.w(DownloadConstants.TAG, "[QUEUE] remove() - Task not in queue, url=" + (taskItem != null ? taskItem.getUrl() : "null"));
        return false;
    }

    public boolean contains(VideoTaskItem taskItem) {
        return mQueue.contains(taskItem);
    }

    public VideoTaskItem getTaskItem(String url) {
        try {
            for (int index = 0; index < mQueue.size(); index++) {
                VideoTaskItem taskItem = mQueue.get(index);
                if (taskItem != null && taskItem.getUrl() != null &&
                        taskItem.getUrl().equals(url)) {
                    return taskItem;
                }
            }
        } catch (Exception e) {
            LogUtils.w(DownloadConstants.TAG, "DownloadQueue getTaskItem failed.");
        }
        return null;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public int size() {
        return mQueue.size();
    }

    public boolean isHead(VideoTaskItem taskItem) {
        if (taskItem == null)
            return false;
        return taskItem.equals(peek());
    }

    public int getDownloadingCount() {
        int count = 0;
        try {
            for (int index = 0; index < mQueue.size(); index++) {
                VideoTaskItem item = mQueue.get(index);
                if (isTaskRunnig(item)) {
                    count++;
                    LogUtils.i(DownloadConstants.TAG, "[QUEUE] getDownloadingCount() - Running task: " + item.getUrl() + ", state=" + item.getTaskState());
                }
            }
        } catch (Exception e) {
            LogUtils.w(DownloadConstants.TAG, "DownloadQueue getDownloadingCount failed.");
        }
        LogUtils.i(DownloadConstants.TAG, "[QUEUE] getDownloadingCount() result=" + count + ", queueSize=" + mQueue.size());
        return count;
    }

    public int getPendingCount() {
        int count = 0;
        try {
            for (int index = 0; index < mQueue.size(); index++) {
                if (isTaskPending(mQueue.get(index))) {
                    count++;
                }
            }
        } catch (Exception e) {
            LogUtils.w(DownloadConstants.TAG, "DownloadQueue getDownloadingCount failed.");
        }
        return count;
    }

    public VideoTaskItem peekPendingTask() {
        try {
            for (int index = 0; index < mQueue.size(); index++) {
                VideoTaskItem taskItem = mQueue.get(index);
                if (isTaskPending(taskItem)) {
                    LogUtils.i(DownloadConstants.TAG, "[QUEUE] peekPendingTask() - Found pending task: " + taskItem.getUrl() + ", state=" + taskItem.getTaskState());
                    return taskItem;
                }
            }
        } catch (Exception e) {
            LogUtils.w(DownloadConstants.TAG, "DownloadQueue peekPendingTask failed.");
        }
        LogUtils.i(DownloadConstants.TAG, "[QUEUE] peekPendingTask() - No pending task found, queueSize=" + mQueue.size());
        return null;
    }

    public boolean isTaskPending(VideoTaskItem taskItem) {
        if (taskItem == null)
            return false;
        int taskState = taskItem.getTaskState();
        return taskState == VideoTaskState.PENDING ||
                taskState == VideoTaskState.PREPARE;
    }

    public boolean isTaskRunnig(VideoTaskItem taskItem) {
        if (taskItem == null)
            return false;
        int taskState = taskItem.getTaskState();
        return taskState == VideoTaskState.START ||
                taskState == VideoTaskState.DOWNLOADING;
    }
}
