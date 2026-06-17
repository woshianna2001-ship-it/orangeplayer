package com.orange.playerlibrary.download;

/**
 * 下载监听器接口
 */
public interface DownloadListener {
    
    /**
     * 下载开始
     * @param task 下载任务
     */
    void onDownloadStart(DownloadTask task);
    
    /**
     * 下载进度更新
     * @param task 下载任务
     * @param progress 进度（0-100）
     * @param speed 下载速度（字节/秒）
     */
    void onDownloadProgress(DownloadTask task, int progress, long speed);
    
    /**
     * 下载暂停
     * @param task 下载任务
     */
    void onDownloadPaused(DownloadTask task);
    
    /**
     * 下载完成
     * @param task 下载任务
     */
    void onDownloadCompleted(DownloadTask task);
    
    /**
     * 下载失败
     * @param task 下载任务
     * @param errorMessage 错误信息
     */
    void onDownloadFailed(DownloadTask task, String errorMessage);
    
    /**
     * 下载取消
     * @param task 下载任务
     */
    void onDownloadCancelled(DownloadTask task);
}
