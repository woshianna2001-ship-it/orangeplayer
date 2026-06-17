package com.orange.playerlibrary.subtitle;

/**
 * 字幕条目
 */
public class SubtitleEntry {
    private long startTime;  // 开始时间（毫秒）
    private long endTime;    // 结束时间（毫秒）
    private String text;     // 字幕文本

    public SubtitleEntry(long startTime, long endTime, String text) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.text = text;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getDuration() {
        return endTime - startTime;
    }

    @Override
    public String toString() {
        return "SubtitleEntry{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", text='" + text + '\'' +
                '}';
    }
}
