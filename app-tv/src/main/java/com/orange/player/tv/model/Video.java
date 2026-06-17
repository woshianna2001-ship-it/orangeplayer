package com.orange.player.tv.model;

/**
 * 视频数据模型
 */
public class Video {
    
    private String title;
    private String url;
    private String thumbnailUrl;
    private String duration;
    private String description;
    
    public Video(String title, String url, String thumbnailUrl, String duration) {
        this.title = title;
        this.url = url;
        this.thumbnailUrl = thumbnailUrl;
        this.duration = duration;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
    
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }
    
    public String getDuration() {
        return duration;
    }
    
    public void setDuration(String duration) {
        this.duration = duration;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}
