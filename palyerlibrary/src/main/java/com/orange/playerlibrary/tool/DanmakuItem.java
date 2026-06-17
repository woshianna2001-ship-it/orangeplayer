package com.orange.playerlibrary.tool;

/**
 * 弹幕数据模型类
 */
public class DanmakuItem {
    private String text;       // 弹幕文字内容
    private int color;         // 文字颜色
    private long timestamp;    // 弹幕应出现的视频时间（毫秒，与视频position单位一致）
    private boolean isSelf;    // 是否是自己发送的弹幕
    
    public DanmakuItem(String text, int color, long timestamp, boolean isSelf) {
        this.text = text;
        this.color = color;
        this.timestamp = timestamp;
        this.isSelf = isSelf;
    }
    
    // getter方法
    public String getText() { return text; }
    public int getColor() { return color; }
    public long getTimestamp() { return timestamp; }
    public boolean isSelf() { return isSelf; }
}
