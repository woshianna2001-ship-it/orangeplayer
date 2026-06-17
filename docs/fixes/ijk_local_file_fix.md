# IJK 播放器本地文件播放修复

## 问题描述

使用 IJK 播放器播放本地视频文件时失败，日志显示错误：

```
E IJKMEDIA: Protocol 'file' not on whitelist 'http,https,tls,rtp,tcp,udp,crypto,httpproxy'!
E OrangevideoView: onPlayError: url=http://127.0.0.1:4845/storage/emulated/0/Android/data/com.jsg/m3u8_cache/...
```

## 根本原因

IJK 播放器默认的协议白名单不包含 `file` 协议，导致无法播放本地文件。即使通过本地 HTTP 服务器（如 127.0.0.1:4845）代理访问，底层仍然需要使用 `file` 协议读取实际文件。

## 解决方案

创建自定义的 `OrangeIjkPlayerManager` 类，在初始化 IJK 播放器时添加 `file` 协议到白名单：

```java
ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "protocol_whitelist", 
    "file,http,https,tls,rtp,tcp,udp,crypto,httpproxy,concat,subfile");
```

### 修改的文件

1. **新增文件**: `palyerlibrary/src/main/java/com/orange/playerlibrary/player/OrangeIjkPlayerManager.java`
   - 继承自 `IjkPlayerManager`
   - 重写 `getMediaPlayer()` 方法
   - 配置协议白名单，添加 `file` 和其他必要协议

2. **修改文件**: `palyerlibrary/src/main/java/com/orange/playerlibrary/OrangevideoView.java`
   - 将所有 `IjkPlayerManager.class` 替换为 `OrangeIjkPlayerManager.class`
   - 共修改 4 处引用

### 协议白名单说明

添加的协议包括：
- `file`: 本地文件访问
- `http/https`: HTTP(S) 流媒体
- `tls`: 安全传输层
- `rtp/tcp/udp`: 网络传输协议
- `crypto`: 加密流
- `httpproxy`: HTTP 代理
- `concat`: 文件拼接（用于 HLS 等）
- `subfile`: 子文件访问

## 测试方法

1. 编译并安装更新后的播放器库
2. 使用 IJK 播放器播放本地视频文件
3. 通过 adb logcat 确认不再出现协议白名单错误

```bash
# 清除日志
adb logcat -c

# 监控播放器日志
adb logcat | findstr /i "IJKMEDIA OrangevideoView"
```

## 影响范围

- 仅影响使用 IJK 播放器的场景
- ExoPlayer 和系统播放器不受影响
- 向后兼容，不影响现有的网络流播放功能

## 相关问题

- 应用包名: com.jsg
- 错误代码: -10000
- 播放器引擎: IJK (tv.danmaku.ijk.media.player.IjkMediaPlayer)
