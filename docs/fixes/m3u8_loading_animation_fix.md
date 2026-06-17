# m3u8 视频加载动画问题修复

## 问题描述

用户报告在播放 m3u8 视频时遇到两个问题：

1. **加载动画异常消失**：视频加载时，加载动画显示几秒后就隐藏了，即使视频还在加载中
2. **播放出错没显示错误页面**：当系统播放器不支持 m3u8 格式时（错误码 -38），播放器卡在黑屏状态，没有显示错误页面

## 问题分析

### 问题 1：加载动画异常消失

通过日志分析发现：
- `hideAllWidget()` 方法每 2.5 秒被调用一次
- 该方法会隐藏所有组件，包括加载动画
- 即使播放器处于 `PREPARING` 或 `BUFFERING` 状态，加载动画也会被隐藏

**根本原因**：GSY 基类的 `hideAllWidget()` 方法被控制器的自动隐藏机制频繁调用，没有考虑播放器当前状态。

### 问题 2：播放出错没显示错误页面

通过日志分析发现：
- MediaPlayer 报错：`error (-38, 0)` = `MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK`
- 这个错误表示系统播放器不支持 HLS 流媒体（m3u8）
- 但播放器没有进入 `ERROR` 状态，一直卡在 `PREPARING (1)` 状态

**根本原因**：
1. 系统播放器的某些错误不会触发 `onPlayError` 回调
2. 播放器会卡在 `PREPARING` 状态，没有超时检测机制

## 解决方案

### 修复 1：在 PREPARING/BUFFERING 状态下禁止隐藏加载动画

修改 `OrangevideoView.hideAllWidget()` 方法：

```java
@Override
protected void hideAllWidget() {
    android.util.Log.d(TAG, "hideAllWidget: 隐藏所有组件包括加载动画, state=" + mCurrentState);
    
    // 在 PREPARING 和 BUFFERING 状态下，不隐藏加载动画
    // 这些状态下用户需要看到加载进度
    if (mCurrentState == CURRENT_STATE_PREPAREING || 
        mCurrentState == CURRENT_STATE_PLAYING_BUFFERING_START) {
        android.util.Log.d(TAG, "hideAllWidget: 跳过隐藏加载动画（正在加载/缓冲）");
        return;
    }
    
    setViewShowState(mLoadingProgressBar, INVISIBLE);
    stopSpeedUpdate();
}
```

**效果**：
- 在 `PREPARING` 状态下，加载动画会持续显示
- 在 `BUFFERING` 状态下，缓冲动画会持续显示
- 只有在播放、暂停、完成等稳定状态下才会隐藏加载动画

### 修复 2：添加 PREPARING 状态超时检测

添加错误检测定时器，检测播放器是否卡在 `PREPARING` 状态超过 30 秒：

```java
/**
 * 启动错误检测定时器
 * 检测播放器是否卡在 PREPARING 状态超过 30 秒
 */
private void startErrorDetectionTimer() {
    if (mInnerHandler != null) {
        mInnerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 如果卡在 PREPARING 状态超过 30 秒，认为出错
                if (mCurrentState == CURRENT_STATE_PREPAREING) {
                    long preparingTime = System.currentTimeMillis() - mPreparingStartTime;
                    if (preparingTime > 30000) {
                        android.util.Log.e(TAG, "错误检测: 播放器卡在 PREPARING 状态超过 30 秒，触发错误状态");
                        setOrangePlayState(PlayerConstants.STATE_ERROR);
                        setStateAndUi(CURRENT_STATE_ERROR);
                    } else {
                        // 继续检测
                        startErrorDetectionTimer();
                    }
                }
            }
        }, 5000); // 每 5 秒检测一次
    }
}
```

在 `setStateAndUi()` 中记录进入 `PREPARING` 状态的时间：

```java
@Override
protected void setStateAndUi(int state) {
    // ... 日志代码 ...
    
    // 记录进入 PREPARING 状态的时间
    if (state == CURRENT_STATE_PREPAREING && mCurrentState != CURRENT_STATE_PREPAREING) {
        mPreparingStartTime = System.currentTimeMillis();
    }
    
    super.setStateAndUi(state);
}
```

**效果**：
- 如果播放器卡在 `PREPARING` 状态超过 30 秒，自动触发错误状态
- 显示错误页面，用户可以点击重试或返回

## 测试验证

### 测试场景 1：正常 m3u8 视频加载

**预期行为**：
1. 点击播放后，显示加载动画
2. 加载动画持续显示，直到视频开始播放
3. 视频开始播放后，加载动画自动隐藏

### 测试场景 2：系统播放器不支持的 m3u8 视频

**预期行为**：
1. 点击播放后，显示加载动画
2. 加载动画持续显示 30 秒
3. 30 秒后，显示错误页面
4. 用户可以点击重试或返回

### 测试场景 3：网络缓慢导致缓冲

**预期行为**：
1. 视频播放过程中，网络缓慢导致缓冲
2. 显示缓冲动画和网速
3. 缓冲动画持续显示，直到缓冲完成
4. 缓冲完成后，继续播放，缓冲动画自动隐藏

## 相关文件

- `palyerlibrary/src/main/java/com/orange/playerlibrary/OrangevideoView.java`
  - `hideAllWidget()` - 修复加载动画异常消失
  - `startErrorDetectionTimer()` - 添加超时检测
  - `setStateAndUi()` - 记录 PREPARING 状态时间

## 注意事项

1. **30 秒超时时间**：可以根据实际网络情况调整，但不建议设置太短（< 20 秒）
2. **系统播放器限制**：系统播放器对 HLS 流的支持有限，建议在设置中提示用户切换到 ExoPlayer 或 IJK
3. **错误重试**：用户点击重试后，可以尝试切换播放器内核

## 后续优化建议

1. **自动切换播放器**：检测到 m3u8 URL 且当前是系统播放器时，提示用户切换到 ExoPlayer
2. **更详细的错误信息**：在错误页面显示具体的错误原因和解决建议
3. **播放器兼容性检测**：在播放前检测当前播放器是否支持该视频格式
