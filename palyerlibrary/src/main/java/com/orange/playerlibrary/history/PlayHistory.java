package com.orange.playerlibrary.history;

/**
 * 播放历史数据模型
 */
public class PlayHistory {
    private long id;
    private String videoUrl;      // 视频 URL（唯一标识）
    private String videoTitle;    // 视频标题
    private String thumbnailUrl;  // 缩略图 URL
    private String thumbnailBase64; // 缩略图 Base64
    private long duration;        // 视频总时长（毫秒）
    private long position;        // 播放位置（毫秒）
    private long lastPlayTime;    // 最后播放时间戳
    private int playCount;        // 播放次数

    public PlayHistory() {}

    public PlayHistory(String videoUrl, String videoTitle, long duration, long position) {
        this.videoUrl = videoUrl;
        this.videoTitle = videoTitle;
        this.duration = duration;
        this.position = position;
        this.lastPlayTime = System.currentTimeMillis();
        this.playCount = 1;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getVideoTitle() { return videoTitle; }
    public void setVideoTitle(String videoTitle) { this.videoTitle = videoTitle; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getThumbnailBase64() { return thumbnailBase64; }
    public void setThumbnailBase64(String thumbnailBase64) { this.thumbnailBase64 = thumbnailBase64; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public long getPosition() { return position; }
    public void setPosition(long position) { this.position = position; }

    public long getLastPlayTime() { return lastPlayTime; }
    public void setLastPlayTime(long lastPlayTime) { this.lastPlayTime = lastPlayTime; }

    public int getPlayCount() { return playCount; }
    public void setPlayCount(int playCount) { this.playCount = playCount; }

    /**
     * 获取播放进度百分比
     */
    public int getProgressPercent() {
        if (duration <= 0) return 0;
        return (int) (position * 100 / duration);
    }

    /**
     * 格式化播放位置为时间字符串
     */
    public String getFormattedPosition() {
        return formatTime(position);
    }

    /**
     * 格式化总时长为时间字符串
     */
    public String getFormattedDuration() {
        return formatTime(duration);
    }

    private String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }
}
