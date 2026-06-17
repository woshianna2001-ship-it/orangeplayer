# IJK HLS Discontinuity 修复方案 - TimestampAdjuster 实现

## ExoPlayer TimestampAdjuster 工作原理

### 核心机制

ExoPlayer 的 `TimestampAdjuster` 通过以下方式解决 HLS Discontinuity 问题：

1. **时间戳偏移计算**
   ```java
   timestampOffsetUs = desiredSampleTimestampUs - actualPtsUs;
   adjustedTimestamp = actualPtsUs + timestampOffsetUs;
   ```

2. **Rollover 处理**（MPEG-2 TS 33位时间戳会回绕）
   ```java
   // 检测时间戳回绕，选择最接近上一个时间戳的值
   long closestWrapCount = (lastPts + (MAX_PTS_PLUS_ONE / 2)) / MAX_PTS_PLUS_ONE;
   long ptsWrapBelow = pts90Khz + (MAX_PTS_PLUS_ONE * (closestWrapCount - 1));
   long ptsWrapAbove = pts90Khz + (MAX_PTS_PLUS_ONE * closestWrapCount);
   pts90Khz = Math.abs(ptsWrapBelow - lastPts) < Math.abs(ptsWrapAbove - lastPts)
       ? ptsWrapBelow : ptsWrapAbove;
   ```

3. **Discontinuity 处理**
   - 每个 discontinuity 段使用独立的 `TimestampAdjuster`
   - 基于 m3u8 的 `#EXTINF` 计算期望时间
   - 强制将 TS 内部 PTS 映射到期望时间

### HLS 播放流程

```
1. 解析 m3u8 → 构建 Timeline（基于 #EXTINF）
2. Seek 到目标时间 → 计算应该下载哪个 TS 切片
3. 下载 TS 切片 → 读取第一帧的 PTS
4. TimestampAdjuster 计算偏移 → offset = 期望时间 - 实际PTS
5. 后续所有帧 → adjustedPTS = actualPTS + offset
```

## IJK/FFmpeg 的问题

### 当前行为

1. **FFmpeg 信任 TS 内部 PTS**
   ```c
   // libavformat/mpegts.c
   // FFmpeg 直接使用 TS 内部的 PTS/DTS
   pkt->pts = pes->pts;
   pkt->dts = pes->dts;
   ```

2. **Seek 基于全局时间线**
   ```c
   // libavformat/utils.c - av_seek_frame()
   // 在整个流的时间维度上二分查找
   // 如果 PTS 不连续，会找到错误的位置
   ```

3. **没有 Discontinuity 感知**
   - FFmpeg 不知道 m3u8 的 `#EXT-X-DISCONTINUITY`
   - 不会为每个段创建独立的时间映射

## 修复方案

### 方案 A：在 IJK Java 层实现 TimestampAdjuster（推荐 ⭐⭐⭐⭐）

**优点：**
- 不需要修改 FFmpeg C 代码
- 可以复用 ExoPlayer 的逻辑
- 维护成本低

**实现步骤：**

1. **创建 HlsTimestampAdjuster.java**
   ```java
   public class HlsTimestampAdjuster {
       private long timestampOffsetUs = C.TIME_UNSET;
       private long lastAdjustedTimestampUs = C.TIME_UNSET;
       
       // 基于 m3u8 的 EXTINF 计算期望时间
       public void initialize(long expectedTimestampUs, long actualPtsUs) {
           timestampOffsetUs = expectedTimestampUs - actualPtsUs;
       }
       
       // 调整时间戳
       public long adjustTimestamp(long ptsUs) {
           if (timestampOffsetUs == C.TIME_UNSET) {
               return ptsUs;
           }
           long adjusted = ptsUs + timestampOffsetUs;
           lastAdjustedTimestampUs = adjusted;
           return adjusted;
       }
   }
   ```

2. **修改 IjkMediaPlayer.java**
   ```java
   // 在 getCurrentPosition() 中应用调整
   @Override
   public long getCurrentPosition() {
       long rawPosition = _getCurrentPosition();
       if (mTimestampAdjuster != null) {
           return mTimestampAdjuster.adjustTimestamp(rawPosition);
       }
       return rawPosition;
   }
   ```

3. **解析 m3u8 检测 Discontinuity**
   ```java
   public class M3U8Parser {
       public List<Segment> parse(String m3u8Content) {
           List<Segment> segments = new ArrayList<>();
           long cumulativeTime = 0;
           boolean hasDiscontinuity = false;
           
           for (String line : m3u8Content.split("\n")) {
               if (line.startsWith("#EXT-X-DISCONTINUITY")) {
                   hasDiscontinuity = true;
               } else if (line.startsWith("#EXTINF:")) {
                   float duration = parseExtinf(line);
                   segments.add(new Segment(
                       cumulativeTime,
                       duration,
                       hasDiscontinuity
                   ));
                   cumulativeTime += duration * 1000000; // 转换为微秒
                   hasDiscontinuity = false;
               }
           }
           return segments;
       }
   }
   ```

**缺点：**
- 只能修正 `getCurrentPosition()`
- Seek 操作仍然会有问题（因为 FFmpeg 层面的 seek 无法修正）

### 方案 B：修改 FFmpeg HLS Demuxer（复杂 ⭐⭐）

**需要修改的文件：**
- `libavformat/hls.c` - HLS demuxer
- `libavformat/mpegts.c` - MPEG-TS demuxer

**实现思路：**

1. **在 hls.c 中记录 Discontinuity 信息**
   ```c
   typedef struct HLSContext {
       // ... 现有字段
       int64_t *discontinuity_offsets;  // 每个 discontinuity 的时间偏移
       int nb_discontinuities;
   } HLSContext;
   ```

2. **解析 m3u8 时记录 Discontinuity**
   ```c
   // 在 parse_playlist() 中
   if (strstr(line, "#EXT-X-DISCONTINUITY")) {
       // 记录当前累计时间
       c->discontinuity_offsets[c->nb_discontinuities++] = cumulative_time;
   }
   ```

3. **Seek 时应用偏移**
   ```c
   // 在 hls_read_seek() 中
   int64_t adjusted_timestamp = timestamp;
   for (int i = 0; i < c->nb_discontinuities; i++) {
       if (timestamp >= c->discontinuity_offsets[i]) {
           // 应用偏移修正
       }
   }
   ```

**缺点：**
- 需要深入理解 FFmpeg 代码
- 修改 C 代码，调试困难
- 需要重新编译 FFmpeg
- 维护成本高

### 方案 C：使用 ExoPlayer（最简单 ⭐⭐⭐⭐⭐）

**实现：**
```java
// 在 PlayerEngineSelector.java 中
if (url.contains(".m3u8")) {
    // 检测是否包含 discontinuity
    if (M3U8DiscontinuityDetector.hasDiscontinuity(url)) {
        return PlayerConstants.ENGINE_EXO;
    }
}
```

**优点：**
- 不需要任何修改
- ExoPlayer 已经完美解决
- 立即可用

## 推荐方案

### 短期方案（立即可用）

使用**方案 C**：自动切换到 ExoPlayer

项目中已经有 `M3U8DiscontinuityDetector` 和 `autoSelectPlayerEngine`，只需要确保启用。

### 中期方案（如果必须用 IJK）

实现**方案 A**：Java 层 TimestampAdjuster

1. 创建 `HlsTimestampAdjuster.java`
2. 修改 `IjkMediaPlayer.getCurrentPosition()`
3. 解析 m3u8 检测 discontinuity

**注意：** 这只能修正显示的时间，Seek 操作仍然会有问题。

### 长期方案（不推荐）

实现**方案 B**：修改 FFmpeg

除非你有充足的时间和 FFmpeg 开发经验，否则不推荐。

## 结论

**HLS Discontinuity 是 FFmpeg 架构层面的限制，无法通过简单的补丁修复。**

最佳方案是：
1. 对于包含 discontinuity 的 m3u8，自动切换到 ExoPlayer
2. 对于其他视频，继续使用 IJK

这样既能解决问题，又不需要维护复杂的 FFmpeg 修改。

## 参考资料

- [ExoPlayer Issue #8312](https://github.com/google/ExoPlayer/issues/8312) - Discontinuity 处理
- [ExoPlayer TimestampAdjuster 源码](https://github.com/androidx/media/blob/release/libraries/common/src/main/java/androidx/media3/common/util/TimestampAdjuster.java)
- [FFmpeg HLS Demuxer](https://github.com/FFmpeg/FFmpeg/blob/master/libavformat/hls.c)
- [HLS RFC 8216](https://tools.ietf.org/html/rfc8216) - HLS 标准
