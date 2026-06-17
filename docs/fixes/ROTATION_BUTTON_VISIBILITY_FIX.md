# 横竖屏切换按钮显示修复

## 问题描述

用户报告：**横竖屏切换按钮不显示**，应该和锁屏按钮同级，在播放器右侧，进入全屏（包括横屏全屏和竖屏全屏）时都应该显示这个按钮。

## 问题分析

### 原始代码逻辑

在 `VodControlView.java` 中，横竖屏切换按钮的可见性控制如下：

```java
// 原始代码 - 只在竖屏全屏模式且未锁屏时显示按钮
boolean shouldShow = helper.isPortraitFullscreen() && !mIsLocked;
mRotationButton.setVisibility(shouldShow ? VISIBLE : GONE);
```

这导致：
- ✅ **竖屏全屏**模式下按钮会显示
- ❌ **横屏全屏**模式下按钮**不会显示**（因为 `isPortraitFullscreen()` 返回 false）

### 布局文件配置

在 `orange_layout_vod_control_view.xml` 中：

```xml
<!-- 锁定按钮 (全屏时显示在左侧) -->
<ImageView
    android:id="@+id/iv_lock"
    android:layout_width="48dp"
    android:layout_height="48dp"
    android:layout_gravity="start|center_vertical"
    android:layout_marginStart="16dp"
    android:visibility="gone" />

<!-- 横竖屏切换按钮 (竖屏全屏时显示在锁定按钮右侧) -->
<ImageView
    android:id="@+id/rotation_button"
    android:layout_width="48dp"
    android:layout_height="48dp"
    android:layout_gravity="start|center_vertical"
    android:layout_marginStart="80dp"
    android:visibility="gone" />
```

两个按钮默认都是 `gone`，需要在运行时根据状态控制显示。

## 修复方案

### 1. 修改按钮可见性逻辑

**文件**: `palyerlibrary/src/main/java/com/orange/playerlibrary/component/VodControlView.java`

**修改内容**:

```java
/**
 * 更新横竖屏切换按钮可见性
 */
public void updateRotationButtonVisibility() {
    if (mRotationButton == null) {
        return;
    }
    
    if (mOrangeController == null) {
        mRotationButton.setVisibility(GONE);
        return;
    }
    
    OrangevideoView videoView = mOrangeController.getVideoView();
    if (videoView == null) {
        mRotationButton.setVisibility(GONE);
        return;
    }
    
    com.orange.playerlibrary.CustomFullscreenHelper helper = videoView.getFullscreenHelper();
    if (helper == null) {
        mRotationButton.setVisibility(GONE);
        return;
    }
    
    // ✅ 修复：在全屏模式下显示按钮（横屏全屏或竖屏全屏），且未锁屏
    boolean shouldShow = helper.isFullscreen() && !mIsLocked;
    mRotationButton.setVisibility(shouldShow ? VISIBLE : GONE);
    
    android.util.Log.d(TAG, "updateRotationButtonVisibility: shouldShow=" + shouldShow 
        + ", isFullscreen=" + helper.isFullscreen()
        + ", isPortraitFullscreen=" + helper.isPortraitFullscreen()
        + ", isLocked=" + mIsLocked);
}
```

**关键变化**:
- ❌ 旧逻辑：`helper.isPortraitFullscreen() && !mIsLocked`
- ✅ 新逻辑：`helper.isFullscreen() && !mIsLocked`

现在只要进入全屏（无论横屏还是竖屏），按钮都会显示。

### 2. 增强点击切换功能

**文件**: `palyerlibrary/src/main/java/com/orange/playerlibrary/component/VodControlView.java`

**修改内容**:

```java
private void onRotationButtonClick() {
    if (mOrangeController == null) {
        android.util.Log.w(TAG, "onRotationButtonClick: mOrangeController is null");
        return;
    }
    
    OrangevideoView videoView = mOrangeController.getVideoView();
    if (videoView == null) {
        android.util.Log.w(TAG, "onRotationButtonClick: videoView is null");
        return;
    }
    
    com.orange.playerlibrary.CustomFullscreenHelper helper = videoView.getFullscreenHelper();
    if (helper == null) {
        android.util.Log.w(TAG, "onRotationButtonClick: CustomFullscreenHelper is null");
        return;
    }
    
    // ✅ 修复：根据当前全屏状态决定切换方向
    if (helper.isPortraitFullscreen()) {
        // 从竖屏全屏切换到横屏全屏
        android.util.Log.d(TAG, "onRotationButtonClick: Switching from portrait to landscape fullscreen");
        helper.stopPortraitFullScreen();
        
        // 延迟 100ms 后进入横屏全屏，等待退出动画完成
        postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mOrangeController != null) {
                    OrangevideoView v = mOrangeController.getVideoView();
                    if (v != null) {
                        com.orange.playerlibrary.CustomFullscreenHelper h = v.getFullscreenHelper();
                        if (h != null) {
                            h.startFullScreen();
                        }
                    }
                }
            }
        }, 100);
    } else if (helper.isFullscreen()) {
        // 从横屏全屏切换到竖屏全屏
        android.util.Log.d(TAG, "onRotationButtonClick: Switching from landscape to portrait fullscreen");
        helper.stopFullScreen();
        
        // 延迟 100ms 后进入竖屏全屏，等待退出动画完成
        postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mOrangeController != null) {
                    OrangevideoView v = mOrangeController.getVideoView();
                    if (v != null) {
                        com.orange.playerlibrary.CustomFullscreenHelper h = v.getFullscreenHelper();
                        if (h != null) {
                            h.startPortraitFullScreen();
                        }
                    }
                }
            }
        }, 100);
    } else {
        android.util.Log.w(TAG, "onRotationButtonClick: Not in fullscreen mode");
    }
}
```

**关键变化**:
- ❌ 旧逻辑：只支持从竖屏全屏 → 横屏全屏
- ✅ 新逻辑：支持双向切换
  - 竖屏全屏 → 横屏全屏
  - 横屏全屏 → 竖屏全屏

## 使用效果

### 场景 1: 进入横屏全屏
1. 点击播放器的全屏按钮
2. 播放器进入横屏全屏模式
3. **锁屏按钮**和**横竖屏切换按钮**同时显示在屏幕左侧
4. 点击横竖屏切换按钮 → 切换到竖屏全屏

### 场景 2: 进入竖屏全屏
1. 点击播放器的竖屏全屏按钮（如果有）
2. 播放器进入竖屏全屏模式（不旋转屏幕）
3. **锁屏按钮**和**横竖屏切换按钮**同时显示在屏幕左侧
4. 点击横竖屏切换按钮 → 切换到横屏全屏

### 场景 3: 锁屏状态
1. 在全屏模式下点击锁屏按钮
2. 锁屏按钮变为锁定状态，其他按钮隐藏
3. **横竖屏切换按钮也被隐藏**（防止误触）
4. 再次点击锁屏按钮解锁 → 所有按钮恢复显示

## 技术细节

### 位置关系
- **锁屏按钮**: `layout_marginStart="16dp"`
- **横竖屏切换按钮**: `layout_marginStart="80dp"`（在锁屏按钮右侧）

### 显示条件
```java
boolean shouldShow = helper.isFullscreen() && !mIsLocked;
```
- `helper.isFullscreen()`: 处于全屏状态（横屏或竖屏）
- `!mIsLocked`: 未锁屏

### 切换延迟
- 使用 `postDelayed(..., 100)` 延迟 100ms
- 等待退出动画完成后再进入新的全屏模式
- 避免动画冲突

## 相关文件

- `palyerlibrary/src/main/java/com/orange/playerlibrary/component/VodControlView.java`
- `palyerlibrary/src/main/res/layout/orange_layout_vod_control_view.xml`
- `palyerlibrary/src/main/java/com/orange/playerlibrary/CustomFullscreenHelper.java`

## 测试验证

1. **横屏全屏测试**:
   ```
   - 播放视频
   - 点击全屏按钮
   - 检查：锁屏按钮和横竖屏切换按钮是否都显示在左侧？
   - 点击横竖屏切换按钮 → 应该切换到竖屏全屏
   ```

2. **竖屏全屏测试**:
   ```
   - 播放视频
   - 进入竖屏全屏（如果有入口）
   - 检查：锁屏按钮和横竖屏切换按钮是否都显示在左侧？
   - 点击横竖屏切换按钮 → 应该切换到横屏全屏
   ```

3. **锁屏测试**:
   ```
   - 全屏模式下点击锁屏按钮
   - 检查：横竖屏切换按钮是否被隐藏？
   - 解锁后 → 横竖屏切换按钮是否恢复显示？
   ```

## 版本

- **修复日期**: 2026-03-27
- **影响模块**: palyerlibrary
- **影响版本**: 1.3.2+
