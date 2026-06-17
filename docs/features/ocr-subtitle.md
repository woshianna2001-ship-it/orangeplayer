# OCR 翻译字幕功能

## 功能概述
通过 OCR 识别视频画面中的硬字幕，翻译成目标语言后显示。

## 技术方案

### 依赖库（可选安装）
- **Tesseract4Android** - OCR 文字识别（离线）
- **ML Kit Translation** - 文字翻译（离线）

### 核心流程
```
视频帧截取 → 字幕区域裁剪 → OCR识别 → 文字翻译 → 字幕显示
     ↓              ↓            ↓          ↓          ↓
   1-2fps      底部20%区域   Tesseract   ML Kit    SubtitleView
```

## 模块设计

### 1. OcrSubtitleManager
- 检测依赖库是否可用
- 管理 OCR 识别和翻译流程
- 控制截帧频率和字幕去重

### 2. OcrEngine（接口）
- `boolean isAvailable()` - 检测是否可用
- `String recognize(Bitmap image)` - 识别文字

### 3. TranslationEngine（接口）
- `boolean isAvailable()` - 检测是否可用
- `void translate(String text, String sourceLang, String targetLang, Callback callback)`

## 用户界面

### 字幕设置弹窗新增选项
- OCR 翻译字幕（开关）
- 源语言选择（自动检测/中文/英文/日文/韩文）
- 目标语言选择（中文/英文）
- 字幕区域调整（底部百分比）

### 未安装依赖时
显示提示："OCR 翻译功能需要安装额外依赖，请在 build.gradle 中添加：
```gradle
implementation 'cz.adaptech.tesseract4android:tesseract4android:4.7.0'
implementation 'com.google.mlkit:translate:17.0.2'
```
"

## 依赖配置

### build.gradle (可选依赖)
```gradle
// OCR 翻译字幕功能（可选）
// implementation 'cz.adaptech.tesseract4android:tesseract4android:4.7.0'
// implementation 'com.google.mlkit:translate:17.0.2'
```

## 性能优化
- 截帧频率：默认 1fps，可调整
- 字幕去重：相同文字不重复显示
- 区域裁剪：只识别字幕区域，减少计算量
- 异步处理：OCR 和翻译在后台线程执行
