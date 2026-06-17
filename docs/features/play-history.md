# 播放历史功能设计

## 功能概述
记录用户的播放历史和播放进度，支持续播功能。

## 功能需求

### 1. 播放进度记录
- 自动保存当前播放进度
- 退出时保存进度
- 切换视频时保存进度

### 2. 续播功能
- 打开视频时检查是否有历史进度
- 提示用户是否从上次位置继续播放
- 或自动跳转到上次位置

### 3. 播放历史列表
- 显示最近播放的视频
- 显示播放进度百分比
- 支持删除历史记录

## 实现方案

### 1. 数据模型
```java
public class PlayHistory {
    private String videoUrl;      // 视频 URL（唯一标识）
    private String videoTitle;    // 视频标题
    private String thumbnailUrl;  // 缩略图
    private long duration;        // 视频总时长
    private long position;        // 播放位置
    private long lastPlayTime;    // 最后播放时间
    private int playCount;        // 播放次数
}
```

### 2. 存储方案

#### 方案 A: SharedPreferences（简单）
```java
// 适合少量数据
SharedPreferences prefs = context.getSharedPreferences("play_history", MODE_PRIVATE);
prefs.edit()
    .putLong("position_" + videoUrl.hashCode(), position)
    .apply();
```

#### 方案 B: SQLite 数据库（推荐）
```sql
CREATE TABLE play_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    video_url TEXT UNIQUE,
    video_title TEXT,
    thumbnail_url TEXT,
    duration INTEGER,
    position INTEGER,
    last_play_time INTEGER,
    play_count INTEGER DEFAULT 1
);
```

#### 方案 C: Room 数据库（现代方案）
```java
@Entity(tableName = "play_history")
public class PlayHistoryEntity {
    @PrimaryKey
    @NonNull
    public String videoUrl;
    public String videoTitle;
    public long duration;
    public long position;
    public long lastPlayTime;
}
```

### 3. 播放历史管理器
```java
public class PlayHistoryManager {
    private static PlayHistoryManager sInstance;
    private PlayHistoryDao mDao;
    
    // 保存播放进度
    public void saveProgress(String url, String title, long duration, long position);
    
    // 获取播放进度
    public long getProgress(String url);
    
    // 获取播放历史列表
    public List<PlayHistory> getHistoryList(int limit);
    
    // 删除历史记录
    public void deleteHistory(String url);
    
    // 清空所有历史
    public void clearAll();
}
```

### 4. 自动保存策略
```java
// 在 OrangevideoView 中
private static final long SAVE_INTERVAL = 10000; // 10秒保存一次
private Handler mSaveHandler = new Handler();
private Runnable mSaveRunnable = () -> {
    saveCurrentProgress();
    mSaveHandler.postDelayed(mSaveRunnable, SAVE_INTERVAL);
};

// 开始播放时启动
private void startAutoSave() {
    mSaveHandler.postDelayed(mSaveRunnable, SAVE_INTERVAL);
}

// 停止播放时保存并停止
private void stopAutoSave() {
    mSaveHandler.removeCallbacks(mSaveRunnable);
    saveCurrentProgress();
}
```

## UI 设计

### 续播提示弹窗
```
┌─────────────────────────────┐
│  上次播放到 12:34           │
│                             │
│  [从头播放]  [继续播放]      │
└─────────────────────────────┘
```

### 播放历史列表
```
┌─────────────────────────────┐
│ 播放历史                    │
├─────────────────────────────┤
│ [缩略图] 视频标题1          │
│          进度: 45%  昨天    │
├─────────────────────────────┤
│ [缩略图] 视频标题2          │
│          进度: 80%  3天前   │
└─────────────────────────────┘
```

## 文件结构
```
palyerlibrary/src/main/java/com/orange/playerlibrary/
├── history/
│   ├── PlayHistory.java          # 数据模型
│   ├── PlayHistoryManager.java   # 历史管理器
│   ├── PlayHistoryDao.java       # 数据访问对象
│   └── PlayHistoryDatabase.java  # 数据库
```

## 使用示例
```java
// 检查是否有历史进度
long savedPosition = PlayHistoryManager.getInstance(context).getProgress(videoUrl);
if (savedPosition > 0) {
    // 显示续播提示
    showResumeDialog(savedPosition, () -> {
        videoView.seekTo(savedPosition);
    });
}

// 保存进度（自动调用，也可手动调用）
PlayHistoryManager.getInstance(context).saveProgress(
    videoUrl, 
    videoTitle, 
    videoView.getDuration(), 
    videoView.getCurrentPosition()
);
```

## 注意事项
1. 进度保存频率不宜过高，避免频繁 IO
2. 历史记录数量应有上限（如最近 100 条）
3. 视频 URL 可能变化，考虑使用视频 ID 作为唯一标识
4. 短视频（如 < 1 分钟）可以不记录历史
