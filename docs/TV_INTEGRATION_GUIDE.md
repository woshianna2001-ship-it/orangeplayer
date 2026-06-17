# TV 模式集成指南

## 概述

本文档说明如何在 OrangePlayer 中集成 TV 模式支持，使播放器能够自动适配 TV 设备。

## 已添加的组件

### 1. TvUtils - TV 检测工具类

位置：`palyerlibrary/src/main/java/com/orange/playerlibrary/utils/TvUtils.java`

**功能**：
- 检测当前设备是否为 TV 设备
- 检测设备是否有触摸屏
- 提供 TV 设备推荐的 UI 尺寸
- 判断是否应该启用手势控制

**使用示例**：
```java
// 检测是否为 TV 设备
boolean isTv = TvUtils.isTvDevice(context);

// 获取推荐按钮大小
int buttonSize = TvUtils.getRecommendedButtonSize(context);

// 判断是否应该启用手势
boolean enableGesture = TvUtils.shouldEnableGesture(context);
```

### 2. TvControlView - TV 专属控制栏

位置：`palyerlibrary/src/main/java/com/orange/playerlibrary/component/TvControlView.java`

**特点**：
- 大按钮设计（80dp-96dp）
- 支持遥控器导航和焦点管理
- 自动隐藏控制栏（5秒后）
- 焦点动画效果
- 大字体显示（24sp-32sp）

**使用示例**：
```java
TvControlView tvControl = new TvControlView(context);
tvControl.bindVideoPlayer(videoPlayer);
tvControl.setTitle("视频标题");
tvControl.showControlBar();
```

## 集成方案

### 方案 1: 在 OrangevideoView 中自动切换

修改 `OrangevideoView` 在初始化时检测设备类型，自动选择合适的控制栏：

```java
private void initComponents() {
    if (TvUtils.isTvDevice(getContext())) {
        // 使用 TV 控制栏
        mTvControlView = new TvControlView(getContext());
        mTvControlView.bindVideoPlayer(this);
        addView(mTvControlView);
        
        // 禁用手势控制
        setIsTouchWiget(false);
    } else {
        // 使用标准控制栏
        mVodControlView = new VodControlView(getContext());
        addView(mVodControlView);
        
        // 启用手势控制
        setIsTouchWiget(true);
    }
}
```

### 方案 2: 手动指定 TV 模式

提供 API 让开发者手动指定是否使用 TV 模式：

```java
// 在 OrangevideoView 中添加
public void setTvMode(boolean tvMode) {
    mIsTvMode = tvMode;
    if (tvMode) {
        switchToTvControl();
    } else {
        switchToStandardControl();
    }
}

// 使用
videoView.setTvMode(true);
```

### 方案 3: 通过配置文件

在 Application 中全局配置：

```java
// 在 TvApplication 中
@Override
public void onCreate() {
    super.onCreate();
    
    // 启用 TV 模式
    OrangePlayerConfig.setTvMode(true);
    
    // 或自动检测
    OrangePlayerConfig.setTvMode(TvUtils.isTvDevice(this));
}
```

## 需要隐藏的 UI 组件（TV 模式）

### 1. 手势控制
```java
if (TvUtils.isTvDevice(context)) {
    // 禁用手势
    setIsTouchWiget(false);
}
```

### 2. 小屏幕控件
- 音量/亮度调节提示（TV 通常用遥控器调节）
- 双击暂停（TV 没有触摸屏）
- 滑动快进/快退（TV 用遥控器按键）

### 3. 不适合 TV 的功能
- 截图按钮（可选保留）
- 分享按钮（TV 通常不需要）
- 画中画按钮（TV 不支持）
- 小窗播放（TV 不支持）

## 需要调整的 UI 尺寸

### 按钮大小
```java
// 手机：48dp
// TV：80dp-96dp
int buttonSize = TvUtils.getRecommendedButtonSize(context);
```

### 文字大小
```java
// 手机：14sp
// TV：24sp-32sp
int textSize = TvUtils.getRecommendedTextSize(context);
```

### 间距
```java
// 手机：8dp-16dp
// TV：24dp-48dp
int padding = isTv ? 48 : 16;
```

## 遥控器按键处理

在 TV 模式下，需要处理遥控器按键：

```java
@Override
public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (!TvUtils.isTvDevice(getContext())) {
        return super.onKeyDown(keyCode, event);
    }
    
    switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_ENTER:
            // 显示/隐藏控制栏
            toggleControlBar();
            return true;
            
        case KeyEvent.KEYCODE_DPAD_LEFT:
            // 快退
            seekBackward(10000);
            return true;
            
        case KeyEvent.KEYCODE_DPAD_RIGHT:
            // 快进
            seekForward(10000);
            return true;
            
        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            // 播放/暂停
            togglePlayPause();
            return true;
    }
    
    return super.onKeyDown(keyCode, event);
}
```

## 焦点管理

TV 模式下需要正确管理焦点：

```java
// 设置控件可获取焦点
button.setFocusable(true);
button.setFocusableInTouchMode(false); // TV 不需要触摸模式

// 焦点变化监听
button.setOnFocusChangeListener((v, hasFocus) -> {
    if (hasFocus) {
        // 放大动画
        v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start();
    } else {
        // 恢复
        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
    }
});

// 设置默认焦点
playButton.requestFocus();
```

## 实现步骤

### 第一步：修改 OrangevideoView

1. 添加 TV 模式检测
2. 根据设备类型选择控制栏
3. 禁用不适合 TV 的功能

### 第二步：修改现有组件

1. **GestureView**: 在 TV 模式下禁用
2. **VodControlView**: 调整按钮大小和文字大小
3. **PrepareView**: 调整封面和按钮大小

### 第三步：测试

1. 在 TV 模拟器上测试
2. 在真实 TV 设备上测试
3. 测试遥控器导航
4. 测试焦点管理

## 配置示例

### app-tv 模块（完全 TV 模式）

```java
public class TvApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 使用 ExoPlayer
        PlayerFactory.setPlayManager(Exo2PlayerManager.class);
        
        // 禁用缓存
        CacheFactory.setCacheManager(null);
        
        // 启用 TV 模式
        OrangePlayerConfig.setTvMode(true);
    }
}
```

### app 模块（自动检测）

```java
public class OrangeApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 自动检测设备类型
        boolean isTv = TvUtils.isTvDevice(this);
        OrangePlayerConfig.setTvMode(isTv);
        
        if (isTv) {
            // TV 设备配置
            PlayerFactory.setPlayManager(Exo2PlayerManager.class);
            CacheFactory.setCacheManager(null);
        } else {
            // 手机/平板配置
            PlayerFactory.setPlayManager(IjkPlayerManager.class);
        }
    }
}
```

## 优势

### 1. 代码复用
- app 和 app-tv 共享同一个播放器库
- 自动适配不同设备类型
- 减少重复代码

### 2. 统一维护
- 修复 bug 只需改一处
- 新功能自动支持两种模式
- 降低维护成本

### 3. 灵活配置
- 支持自动检测
- 支持手动指定
- 支持全局配置

## 后续工作

1. ✅ 创建 TvUtils 工具类
2. ✅ 创建 TvControlView 组件
3. ✅ 创建 OrangePlayerConfig 全局配置类
4. ✅ 修改 GestureView 支持 TV 模式（自动禁用）
5. ✅ 修改 TvApplication 使用全局配置
6. ✅ 修改 OrangeApplication 自动检测 TV 模式
7. ⏳ 修改 OrangevideoView 支持 TV 模式自动切换控制栏
8. ⏳ 修改其他组件适配 TV（VodControlView、PrepareView 等）
9. ⏳ 完善文档和示例

## 当前状态

### 已完成
- ✅ TV 设备检测（TvUtils）
- ✅ TV 专属控制栏（TvControlView）
- ✅ 全局配置系统（OrangePlayerConfig）
- ✅ 手势控制在 TV 模式下自动禁用（GestureView）
- ✅ app-tv 强制启用 TV 模式
- ✅ app 自动检测设备类型

### 工作原理

1. **app 模块**：
   - 启动时自动检测设备类型
   - 如果是 TV 设备，自动启用 TV 模式
   - 如果是手机/平板，使用标准模式

2. **app-tv 模块**：
   - 强制启用 TV 模式
   - 使用 ExoPlayer 内核
   - 禁用缓存

3. **palyerlibrary**：
   - GestureView 在 TV 模式下自动禁用
   - 其他组件可以通过 `OrangePlayerConfig.isTvMode()` 检测模式
   - 提供 TvControlView 作为 TV 专属控制栏

### 使用示例

```java
// 在 Application 中
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 方式 1: 自动检测
        boolean isTv = TvUtils.isTvDevice(this);
        OrangePlayerConfig.setTvMode(isTv);
        
        // 方式 2: 手动指定
        OrangePlayerConfig.setTvMode(true);
        
        // 方式 3: 禁用自动检测
        OrangePlayerConfig.setAutoDetectTvMode(false);
        OrangePlayerConfig.setTvMode(false);
    }
}

// 在组件中检测 TV 模式
if (OrangePlayerConfig.isTvMode(context)) {
    // TV 模式逻辑
} else {
    // 标准模式逻辑
}
```

## 相关文档

- [TV 快速开始](TV_QUICK_START.md)
- [TV 适配指南](TV_ADAPTATION_GUIDE.md)
- [TV 模块创建记录](TV_MODULE_CREATED.md)
