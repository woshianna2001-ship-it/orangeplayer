# OrangePlayer 新功能开发文档

本目录包含 OrangePlayer 播放器新功能的设计和实现文档。

## 功能列表

1. [外挂字幕支持](./subtitle.md) - 支持 SRT、ASS 等格式的外挂字幕
2. [视频截图](./screenshot.md) - 截取当前画面保存到相册
3. [播放历史](./play-history.md) - 记录播放进度，支持续播
4. [广告加载](./advertisement.md) - 视频广告加载和展示

## GSY 官方参考

根据 GSY 官方库的分析：

### 字幕功能
- GSY 通过 ExoPlayer 支持字幕
- 示例代码：`gsy/GSYVideoPlayer-master/app/src/main/java/com/example/gsyvideoplayer/exosubtitle/`
- 关键方法：`setSubTitle(url)` 设置外挂字幕
- 支持 SRT 格式字幕文件

### 截图功能
- GSY 内置截图支持
- 关键方法：`taskShotPic(GSYVideoShotListener)` 获取截图
- 支持高清截图：`taskShotPic(listener, true)`
- 支持保存到文件：`saveFrame(file, listener)`

### 播放历史
- GSY 提供 `playPosition` 用于记录播放位置
- 需要自行实现持久化存储（SharedPreferences 或数据库）

### 广告功能
- GSY 没有内置广告功能
- 需要自行实现广告加载和展示逻辑
