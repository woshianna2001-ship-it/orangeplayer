# 项目结构

```
orangeplayer/
├── app/                          # Demo 应用
├── palyerlibrary/                # 播放器库
│   └── src/main/java/com/orange/playerlibrary/
│       ├── OrangevideoView.java          # 主播放器视图
│       ├── OrangeVideoController.java    # 播放器控制器
│       ├── VideoEventManager.java        # 事件管理器
│       ├── PlayerSettingsManager.java    # 设置管理器
│       ├── PlayerConstants.java          # 常量定义
│       │
│       ├── component/                    # UI 组件
│       │   ├── VodControlView.java       # 点播控制组件
│       │   ├── LiveControlView.java      # 直播控制组件
│       │   ├── TitleView.java            # 标题栏组件
│       │   ├── GestureView.java          # 手势控制组件
│       │   ├── PrepareView.java          # 准备界面
│       │   ├── CompleteView.java         # 播放完成界面
│       │   ├── ErrorView.java            # 错误界面
│       │   └── DanmaView.java            # 弹幕视图
│       │
│       ├── subtitle/                     # 字幕模块
│       │   ├── SubtitleManager.java      # 字幕管理器
│       │   ├── SubtitleView.java         # 字幕显示视图
│       │   └── SubtitleEntry.java        # 字幕条目
│       │
│       ├── ocr/                          # OCR 模块
│       │   ├── OcrSubtitleManager.java   # OCR 字幕管理
│       │   ├── TesseractOcrEngine.java   # Tesseract 引擎
│       │   ├── MlKitTranslationEngine.java # ML Kit 翻译
│       │   ├── LanguagePackManager.java  # 语言包管理
│       │   └── LanguagePackDialog.java   # 语言包下载界面
│       │
│       ├── cast/                         # 投屏模块
│       │   └── DLNACastManager.java      # DLNA 投屏
│       │
│       ├── screenshot/                   # 截图模块
│       │   └── ScreenshotManager.java    # 截图管理
│       │
│       ├── history/                      # 历史记录模块
│       │   ├── PlayHistoryManager.java   # 历史管理
│       │   ├── PlayHistoryDatabase.java  # 数据库
│       │   └── PlayHistory.java          # 历史实体
│       │
│       ├── player/                       # 播放器内核
│       │   └── OrangeSystemPlayerManager.java
│       │
│       ├── exo/                          # ExoPlayer 扩展
│       │   └── OrangeExoPlayerManager.java
│       │
│       ├── interfaces/                   # 接口定义
│       │   ├── ControlWrapper.java       # 控制器包装
│       │   ├── IControlComponent.java    # 控制组件接口
│       │   ├── IDanmakuController.java   # 弹幕控制接口
│       │   └── OnStateChangeListener.java # 状态监听
│       │
│       ├── loading/                      # 加载动画
│       │   └── AVLoadingIndicatorView.java
│       │
│       └── tool/                         # 工具类
│           ├── DanmakuItem.java
│           └── DanmuexitDialog.java
│
├── tessdata_packs/               # OCR 语言包
│   ├── chi_sim.traineddata
│   ├── chi_tra.traineddata
│   ├── eng.traineddata
│   ├── jpn.traineddata
│   └── kor.traineddata
│
└── docs/                         # 文档
    ├── API.md                    # API 文档
    ├── STRUCTURE.md              # 项目结构
    └── features/                 # 功能文档
```

## 模块说明

### 核心模块

| 类 | 说明 |
|------|------|
| `OrangevideoView` | 主播放器，继承 GSYVideoPlayer |
| `OrangeVideoController` | 控制器，管理所有 UI 组件 |
| `VideoEventManager` | 处理所有用户交互事件 |
| `PlayerSettingsManager` | 持久化播放器设置 |

### UI 组件

| 组件 | 说明 |
|------|------|
| `VodControlView` | 点播控制栏（进度条、按钮等） |
| `TitleView` | 顶部标题栏（返回、设置等） |
| `GestureView` | 手势控制（亮度、音量、进度） |

### 功能模块

| 模块 | 说明 |
|------|------|
| `subtitle/` | 字幕加载、解析、显示 |
| `ocr/` | OCR 识别和翻译 |
| `cast/` | DLNA 投屏 |
| `screenshot/` | 视频截图 |
| `history/` | 播放历史记录 |
