# 广告加载功能设计

## 功能概述
在视频播放前、中、后插入广告，支持跳过功能。

## 广告类型

### 1. 前贴片广告 (Pre-roll)
- 视频播放前展示
- 通常 5-30 秒
- 支持跳过（如 5 秒后可跳过）

### 2. 中插广告 (Mid-roll)
- 视频播放中间插入
- 可设置多个插入点
- 适合长视频

### 3. 后贴片广告 (Post-roll)
- 视频播放完成后展示
- 用户可能直接关闭

### 4. 暂停广告 (Pause Ad)
- 用户暂停时显示
- 通常是图片广告
- 不影响播放体验

## 实现方案

### 1. 广告数据模型
```java
public class Advertisement {
    private String id;
    private AdType type;          // PRE_ROLL, MID_ROLL, POST_ROLL, PAUSE
    private String videoUrl;      // 视频广告 URL
    private String imageUrl;      // 图片广告 URL
    private String clickUrl;      // 点击跳转链接
    private int duration;         // 广告时长（秒）
    private int skipAfter;        // 几秒后可跳过（0 表示不可跳过）
    private long insertPosition;  // 中插广告的插入位置（毫秒）
}

public enum AdType {
    PRE_ROLL,
    MID_ROLL,
    POST_ROLL,
    PAUSE
}
```

### 2. 广告管理器
```java
public class AdvertisementManager {
    private List<Advertisement> mAdList;
    private AdCallback mCallback;
    
    // 加载广告配置
    public void loadAds(String videoId, AdLoadCallback callback);
    
    // 检查是否有前贴片广告
    public Advertisement getPreRollAd();
    
    // 检查当前位置是否有中插广告
    public Advertisement getMidRollAd(long position);
    
    // 获取后贴片广告
    public Advertisement getPostRollAd();
    
    // 获取暂停广告
    public Advertisement getPauseAd();
    
    // 广告播放完成
    public void onAdComplete(Advertisement ad);
    
    // 广告被跳过
    public void onAdSkipped(Advertisement ad);
    
    // 广告被点击
    public void onAdClicked(Advertisement ad);
}
```

### 3. 广告播放器视图
```java
public class AdPlayerView extends FrameLayout {
    private OrangevideoView mAdVideoView;
    private ImageView mAdImageView;
    private TextView mCountdownText;
    private TextView mSkipButton;
    
    // 播放视频广告
    public void playVideoAd(Advertisement ad);
    
    // 显示图片广告
    public void showImageAd(Advertisement ad);
    
    // 更新倒计时
    private void updateCountdown(int seconds);
    
    // 显示跳过按钮
    private void showSkipButton();
}
```

### 4. 广告播放流程
```
开始播放
    │
    ▼
检查前贴片广告 ──有──► 播放前贴片广告 ──完成/跳过──┐
    │                                              │
    │无                                            │
    ▼                                              ▼
播放正片 ◄─────────────────────────────────────────┘
    │
    ▼
检查中插广告位置 ──到达──► 暂停正片 ──► 播放中插广告 ──完成/跳过──► 继续正片
    │
    ▼
正片播放完成
    │
    ▼
检查后贴片广告 ──有──► 播放后贴片广告
    │
    │无
    ▼
播放结束
```

## UI 设计

### 广告播放界面
```
┌─────────────────────────────────────┐
│                                     │
│         [广告视频画面]               │
│                                     │
│  广告 15秒                [跳过广告] │
│                                     │
│  [了解更多]                          │
└─────────────────────────────────────┘
```

### 暂停广告
```
┌─────────────────────────────────────┐
│                                     │
│         [视频暂停画面]               │
│                                     │
│    ┌─────────────────┐              │
│    │   [广告图片]     │              │
│    │   点击了解更多   │              │
│    └─────────────────┘              │
│                                     │
└─────────────────────────────────────┘
```

## 文件结构
```
palyerlibrary/src/main/java/com/orange/playerlibrary/
├── ad/
│   ├── Advertisement.java           # 广告数据模型
│   ├── AdvertisementManager.java    # 广告管理器
│   ├── AdPlayerView.java            # 广告播放视图
│   ├── AdCallback.java              # 广告回调接口
│   └── AdConfig.java                # 广告配置
```

## 广告配置示例
```json
{
  "videoId": "video_123",
  "ads": [
    {
      "id": "ad_001",
      "type": "PRE_ROLL",
      "videoUrl": "http://example.com/ad1.mp4",
      "clickUrl": "http://example.com/landing",
      "duration": 15,
      "skipAfter": 5
    },
    {
      "id": "ad_002",
      "type": "MID_ROLL",
      "videoUrl": "http://example.com/ad2.mp4",
      "insertPosition": 300000,
      "duration": 10,
      "skipAfter": 0
    }
  ]
}
```

## 使用示例
```java
// 初始化广告管理器
AdvertisementManager adManager = new AdvertisementManager(context);
adManager.setCallback(new AdCallback() {
    @Override
    public void onAdStart(Advertisement ad) {
        // 广告开始
    }
    
    @Override
    public void onAdComplete(Advertisement ad) {
        // 广告完成，继续播放正片
        videoView.start();
    }
    
    @Override
    public void onAdSkipped(Advertisement ad) {
        // 广告被跳过
        videoView.start();
    }
    
    @Override
    public void onAdClicked(Advertisement ad) {
        // 打开广告链接
        openUrl(ad.getClickUrl());
    }
});

// 加载广告
adManager.loadAds(videoId, ads -> {
    Advertisement preRoll = adManager.getPreRollAd();
    if (preRoll != null) {
        adPlayerView.playVideoAd(preRoll);
    } else {
        videoView.start();
    }
});
```

## 注意事项
1. 广告加载应该异步进行，不阻塞视频播放
2. 广告播放失败时应该直接播放正片
3. 跳过按钮的倒计时要准确
4. 中插广告位置要记录，避免重复播放
5. 考虑用户体验，广告不宜过多过长
6. 需要统计广告曝光、点击、完成等数据
