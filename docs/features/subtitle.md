# 外挂字幕功能设计

## 功能概述
支持加载外挂字幕文件（SRT、ASS、VTT 等格式），在视频播放时显示字幕。

## GSY 官方实现参考

### 关键代码
```java
// 设置外挂字幕 URL
binding.detailPlayer.setSubTitle("http://example.com/subtitle.srt");

// 切换字幕轨道
IjkExo2MediaPlayer player = ((IjkExo2MediaPlayer) videoPlayer.getGSYVideoManager().getPlayer().getMediaPlayer());
MappingTrackSelector.MappedTrackInfo mappedTrackInfo = player.getTrackSelector().getCurrentMappedTrackInfo();
// 遍历 C.TRACK_TYPE_TEXT 类型的轨道
```

### 依赖
- 需要使用 ExoPlayer 播放核心
- 字幕渲染使用 ExoPlayer 内置的 SubtitleView

## 实现方案

### 1. 字幕管理器 (SubtitleManager)
```java
public class SubtitleManager {
    // 加载字幕文件
    public void loadSubtitle(String url);
    public void loadSubtitle(File file);
    
    // 字幕控制
    public void show();
    public void hide();
    public void setTextSize(float size);
    public void setTextColor(int color);
    
    // 字幕轨道切换
    public List<SubtitleTrack> getAvailableTracks();
    public void selectTrack(int index);
}
```

### 2. 字幕视图 (SubtitleView)
- 使用 ExoPlayer 的 SubtitleView
- 或自定义 TextView 渲染字幕

### 3. 字幕解析器
- SRT 解析器
- ASS/SSA 解析器
- VTT 解析器

## UI 设计

### 字幕按钮
- 位置：全屏控制栏
- 图标：字幕图标
- 点击：显示字幕选择菜单

### 字幕选择菜单
- 关闭字幕
- 内嵌字幕（如果有）
- 外挂字幕列表
- 字幕设置（字体大小、颜色、位置）

## 文件结构
```
palyerlibrary/src/main/java/com/orange/playerlibrary/
├── subtitle/
│   ├── SubtitleManager.java      # 字幕管理器
│   ├── SubtitleParser.java       # 字幕解析器
│   ├── SubtitleTrack.java        # 字幕轨道模型
│   └── SubtitleView.java         # 字幕显示视图
```

## 使用示例
```java
// 设置字幕
videoView.setSubtitle("http://example.com/movie.srt");

// 或从本地文件
videoView.setSubtitle(new File("/sdcard/movie.srt"));

// 字幕设置
videoView.getSubtitleManager().setTextSize(24);
videoView.getSubtitleManager().setTextColor(Color.WHITE);
```
