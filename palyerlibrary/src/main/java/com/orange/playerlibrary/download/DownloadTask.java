package com.orange.playerlibrary.download;

import java.io.Serializable;

/**
 * 下载任务模型
 */
public class DownloadTask implements Serializable {
    
    // 任务状态
    public static final int STATE_IDLE = 0;        // 空闲
    public static final int STATE_WAITING = 1;     // 等待中
    public static final int STATE_DOWNLOADING = 2; // 下载中
    public static final int STATE_PAUSED = 3;      // 已暂停
    public static final int STATE_COMPLETED = 4;   // 已完成
    public static final int STATE_FAILED = 5;      // 失败
    public static final int STATE_CANCELLED = 6;   // 已取消
    
    private String taskId;              // 任务ID（唯一标识）
    private String url;                 // 下载地址
    private String title;               // 视频标题
    private String savePath;            // 保存路径
    private String fileName;            // 文件名
    private long totalSize;             // 总大小（字节）
    private long downloadedSize;        // 已下载大小（字节）
    private int state;                  // 当前状态
    private int progress;               // 下载进度（0-100）
    private long speed;                 // 下载速度（字节/秒）
    private long createTime;            // 创建时间
    private long updateTime;            // 更新时间
    private String errorMessage;        // 错误信息
    private boolean isM3u8;             // 是否是 M3U8 格式
    
    public DownloadTask() {
        this.taskId = generateTaskId();
        this.state = STATE_IDLE;
        this.createTime = System.currentTimeMillis();
        this.updateTime = this.createTime;
    }
    
    public DownloadTask(String url, String title, String savePath) {
        this();
        this.url = url;
        this.title = title;
        this.savePath = savePath;
        this.fileName = generateFileName(url, title);
        this.isM3u8 = url.toLowerCase().contains(".m3u8");
    }
    
    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return "task_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }
    
    /**
     * 生成文件名
     */
    private String generateFileName(String url, String title) {
        if (title != null && !title.isEmpty()) {
            // 移除非法字符
            String safeName = title.replaceAll("[\\\\/:*?\"<>|]", "_");
            return safeName + getFileExtension(url);
        }
        // 从 URL 提取文件名
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        if (fileName.contains("?")) {
            fileName = fileName.substring(0, fileName.indexOf('?'));
        }
        return fileName;
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String url) {
        if (url.toLowerCase().contains(".m3u8")) {
            return ".mp4"; // M3U8 转换为 MP4
        }
        if (url.contains(".")) {
            String ext = url.substring(url.lastIndexOf('.'));
            if (ext.contains("?")) {
                ext = ext.substring(0, ext.indexOf('?'));
            }
            return ext;
        }
        return ".mp4"; // 默认扩展名
    }
    
    /**
     * 更新进度
     */
    public void updateProgress(long downloadedSize, long totalSize) {
        this.downloadedSize = downloadedSize;
        this.totalSize = totalSize;
        if (totalSize > 0) {
            this.progress = (int) (downloadedSize * 100 / totalSize);
        }
        this.updateTime = System.currentTimeMillis();
    }
    
    /**
     * 计算下载速度
     */
    public void calculateSpeed(long downloadedBytes, long timeMillis) {
        if (timeMillis > 0) {
            this.speed = downloadedBytes * 1000 / timeMillis;
        }
    }
    
    /**
     * 获取格式化的文件大小
     */
    public String getFormattedTotalSize() {
        return formatFileSize(totalSize);
    }
    
    /**
     * 获取格式化的已下载大小
     */
    public String getFormattedDownloadedSize() {
        return formatFileSize(downloadedSize);
    }
    
    /**
     * 获取格式化的下载速度
     */
    public String getFormattedSpeed() {
        return formatFileSize(speed) + "/s";
    }
    
    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }
    
    /**
     * 获取状态描述
     */
    public String getStateDescription() {
        switch (state) {
            case STATE_IDLE:
                return "空闲";
            case STATE_WAITING:
                return "等待中";
            case STATE_DOWNLOADING:
                return "下载中";
            case STATE_PAUSED:
                return "已暂停";
            case STATE_COMPLETED:
                return "已完成";
            case STATE_FAILED:
                return "失败";
            case STATE_CANCELLED:
                return "已取消";
            default:
                return "未知";
        }
    }
    
    // Getters and Setters
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getSavePath() {
        return savePath;
    }
    
    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public long getTotalSize() {
        return totalSize;
    }
    
    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }
    
    public long getDownloadedSize() {
        return downloadedSize;
    }
    
    public void setDownloadedSize(long downloadedSize) {
        this.downloadedSize = downloadedSize;
    }
    
    public int getState() {
        return state;
    }
    
    public void setState(int state) {
        this.state = state;
        this.updateTime = System.currentTimeMillis();
    }
    
    public int getProgress() {
        return progress;
    }
    
    public void setProgress(int progress) {
        this.progress = progress;
    }
    
    public long getSpeed() {
        return speed;
    }
    
    public void setSpeed(long speed) {
        this.speed = speed;
    }
    
    public long getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
    
    public long getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public boolean isM3u8() {
        return isM3u8;
    }
    
    public void setM3u8(boolean m3u8) {
        isM3u8 = m3u8;
    }
    
    /**
     * 获取完整的文件路径
     */
    public String getFullPath() {
        return savePath + "/" + fileName;
    }
}
