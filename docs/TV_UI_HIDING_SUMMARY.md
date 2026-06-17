# TV 模式 UI 隐藏功能总结

## 概述

在 TV 模式下，自动隐藏不适合电视使用的 UI 元素，同时保留标准的播放控制功能。

## 实现方案

### 方案选择

**最终方案**: 使用标准的 app 控制组件（VodControlView），在 TV 模式下自动隐藏不适合的 UI

**优势**:
- 代码复用，无需维护两套控制组件
- 自动适配，通过 `OrangePlayerConfig.isTvMode()` 检测
- 简化维护，只需在一处修改即可影响所有 TV 应用

## 隐藏的 UI 元素

### 1. TitleView（标题栏）

**隐藏元素**:
- 投屏按钮 (`mCast`)
- 小窗按钮 (`mWindow`)

**实现位置**: `palyerlibrary/src/main/java/com/orange/playerlibrary/component/TitleView.java`

```java
// TV 模式下隐藏投屏和小窗按钮
if (mIsTvMode) {
    if (mCast != null) {
        mCast.setVisibility(GONE);
    }
    if (mWindow != null) {
        mWindow.setVisibility(GONE);
    }
}
```

### 2. VodControlView（播放控制栏）

**隐藏元素**:
- 全屏弹幕区 (`mFullScreenDanmu`) - 仅在全屏模式下
- 弹幕容器 (`mDanmuContainer`) - 仅在全屏模式下

**实现位置**: `palyerlibrary/src/main/java/com/orange/playerlibrary/component/VodControlView.java`

```java
@Override
public void onPlayerStateChanged(int playerState) {
    if (playerState == PlayerConstants.PLAYER_FULL_SCREEN) {
        // TV 模式下隐藏弹幕区
        if (mDanmuContainer != null) {
            mDanmuContainer.setVisibility(mIsTvMode ? GONE : VISIBLE);
        }
        // TV 模式下隐藏全屏弹幕按钮
        if (mFullScreenDanmu != null) {
            mFullScreenDanmu.setVisibility(mIsTvMode ? GONE : VISIBLE);
            if (!mIsTvMode) {
                mFullScreenDanmu.setSelected(true);
            }
        }
        // ... 其他控制元素保持正常显示
    }
}
```

### 3. GestureView（手势组件）

**TV 模式行为**: ✅ **正常启用**

手势组件在 TV 模式下**不会被禁用**，用户可以正常使用触摸手势控制音量、亮度和进度。

## 保留的 UI 元素

以下控制元素在 TV 模式下**正常显示和工作**:

### 播放控制
- 播放/暂停按钮
- 进度条
- 时间显示（当前时间/总时长）
- 全屏按钮

### 高级功能
- 倍速控制
- 选集按钮
- 跳过片头片尾
- 下一集按钮
- 锁定按钮（全屏时）

### 标题栏
- 返回按钮
- 标题文本
- 系统时间
- 电池状态
- 设置按钮

### 手势控制
- ✅ 音量调节手势
- ✅ 亮度调节手势
- ✅ 进度调节手势
- ✅ 手势提示显示

## TV 应用集成

### app-tv 模块

**布局文件**: `app-tv/src/main/res/layout/activity_tv_player.xml`

```xml
<FrameLayout>
    <!-- 只需要一个 OrangevideoView，自动适配 TV 模式 -->
    <com.orange.playerlibrary.OrangevideoView
        android:id="@+id/video_player"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
</FrameLayout>
```

**Activity**: `app-tv/src/main/java/com/orange/player/tv/ui/TvPlayerActivity.java`

```java
public class TvPlayerActivity extends AppCompatActivity {
    private OrangevideoView videoPlayer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tv_player);
        
        videoPlayer = findViewById(R.id.video_player);
        videoPlayer.setUp(videoUrl, true, videoTitle);
        videoPlayer.startPlayLogic();
    }
}
```

## TV 模式检测

TV 模式通过 `OrangePlayerConfig` 全局配置类检测:

```java
// 自动检测（推荐）
OrangePlayerConfig.setTvMode(context, null);  // 自动检测

// 手动设置
OrangePlayerConfig.setTvMode(context, true);   // 强制启用
OrangePlayerConfig.setTvMode(context, false);  // 强制禁用

// 检查状态
boolean isTvMode = OrangePlayerConfig.isTvMode(context);
```

## 代码变更

### 修改的文件

1. **VodControlView.java**
   - 添加 `mIsTvMode` 字段
   - 在 `init()` 中检测 TV 模式
   - 在 `onPlayerStateChanged()` 中根据 TV 模式隐藏弹幕区

2. **TitleView.java**
   - 添加 `mIsTvMode` 字段
   - 在 `initView()` 中检测 TV 模式并隐藏投屏/小窗按钮

3. **TvPlayerActivity.java**
   - 移除 TvControlView 相关代码
   - 简化为只使用 OrangevideoView

4. **activity_tv_player.xml**
   - 移除 TvControlView
   - 只保留 OrangevideoView

## 测试验证

### 测试环境
- Android TV 模拟器 (API 31)
- 设备: emulator-5554

### 测试结果
✅ 投屏按钮已隐藏  
✅ 小窗按钮已隐藏  
✅ 全屏弹幕区已隐藏  
✅ 播放控制正常  
✅ 进度条正常  
✅ 倍速控制正常  
✅ 手势控制正常（音量、亮度、进度）  
✅ 遥控器导航正常  

## 提交记录

```
commit cfdd338
feat(tv): TV模式下隐藏不适合的UI元素

- VodControlView: TV模式下隐藏全屏弹幕区(mFullScreenDanmu)
- TitleView: TV模式下隐藏投屏和小窗按钮(已完成)
- TvPlayerActivity: 使用标准OrangevideoView，移除TvControlView
- activity_tv_player.xml: 简化布局，只保留OrangevideoView

TV模式下自动隐藏的UI:
- 投屏按钮(TitleView)
- 小窗按钮(TitleView)
- 全屏弹幕区(VodControlView)

其他控制组件(播放、进度、倍速等)保持正常显示

commit 00e1358
docs(tv): 添加TV模式UI隐藏功能总结文档

commit ffec4a7
fix(tv): 恢复TV模式下的手势组件和控制器显示

- GestureView: 移除TV模式禁用逻辑，手势在TV模式下也可正常使用
- 修复控制器组件不显示的问题
- TV模式下保留所有播放控制功能
```

## 相关文档

- [TV 适配指南](TV_ADAPTATION_GUIDE.md)
- [TV 快速开始](TV_QUICK_START.md)
- [TV 代码共享方案](TV_SHARED_CODE_SUMMARY.md)

## 未来改进

可能的改进方向:

1. **更多 UI 定制**
   - 根据 TV 屏幕尺寸调整控制栏大小
   - 优化焦点高亮效果
   - 添加更多遥控器快捷键

2. **性能优化**
   - TV 模式下禁用不必要的动画
   - 优化内存使用

3. **功能增强**
   - 添加 TV 专属功能（如语音控制）
   - 支持多屏显示
