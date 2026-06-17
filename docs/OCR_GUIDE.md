# OCR 字幕翻译指南

完整的 OCR 字幕识别与翻译功能配置和使用指南。

## 目录

- [功能介绍](#功能介绍)
- [系统要求](#系统要求)
- [添加依赖](#添加依赖)
- [语言包管理](#语言包管理)
- [使用方法](#使用方法)
- [功能特点](#功能特点)
- [注意事项](#注意事项)
- [故障排查](#故障排查)

---

## 功能介绍

OrangePlayer 支持识别视频画面中的硬字幕（嵌入在视频中的字幕），并使用 ML Kit 进行翻译。

**适用场景：**
- 视频中嵌入了硬字幕（无法关闭的字幕）
- 需要翻译外语硬字幕
- 学习外语时需要双语对照

---

## 系统要求

- **Android 5.0 (API 21) 或更高版本**
- 至少 100MB 可用存储空间（用于语言包）
- 建议 2GB 以上内存

---

## 添加依赖

在 `app/build.gradle` 中添加：

```gradle
dependencies {
    // OCR 文字识别
    implementation 'cz.adaptech.tesseract4android:tesseract4android:4.7.0'
    
    // 文字翻译
    implementation 'com.google.mlkit:translate:17.0.2'
}
```

---

## 语言包管理

### 支持的语言

Tesseract OCR 需要语言包文件才能工作。语言包文件需要放在应用的 `assets` 目录或外部存储。

| 语言 | 文件 | 大小 | 下载地址 |
|------|------|------|---------|
| 简体中文 | chi_sim.traineddata | 2.35 MB | [下载](https://github.com/tesseract-ocr/tessdata/raw/main/chi_sim.traineddata) |
| 繁体中文 | chi_tra.traineddata | 2.26 MB | [下载](https://github.com/tesseract-ocr/tessdata/raw/main/chi_tra.traineddata) |
| 英语 | eng.traineddata | 3.92 MB | [下载](https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata) |
| 日语 | jpn.traineddata | 2.36 MB | [下载](https://github.com/tesseract-ocr/tessdata/raw/main/jpn.traineddata) |
| 韩语 | kor.traineddata | 1.60 MB | [下载](https://github.com/tesseract-ocr/tessdata/raw/main/kor.traineddata) |
| 法语 | fra.traineddata | 2.19 MB | [下载](https://github.com/tesseract-ocr/tessdata/raw/main/fra.traineddata) |
| 德语 | deu.traineddata | 1.99 MB | [下载](https://github.com/tesseract-ocr/tessdata/raw/main/deu.traineddata) |
| 西班牙语 | spa.traineddata | 2.18 MB | [下载](https://github.com/tesseract-ocr/tessdata/raw/main/spa.traineddata) |
| 俄语 | rus.traineddata | 2.84 MB | [下载](https://github.com/tesseract-ocr/tessdata/raw/main/rus.traineddata) |
| 阿拉伯语 | ara.traineddata | 1.87 MB | [下载](https://github.com/tesseract-ocr/tessdata/raw/main/ara.traineddata) |

更多语言请访问：https://github.com/tesseract-ocr/tessdata

### 语言包放置位置

#### 方式一：放在 assets 目录（推荐）

```
app/src/main/assets/
└── tessdata/
    ├── chi_sim.traineddata
    ├── eng.traineddata
    └── jpn.traineddata
```

**优点：**
- 打包在 APK 中，无需下载
- 首次使用即可用

**缺点：**
- 增加 APK 体积
- 无法动态更新

#### 方式二：运行时下载到外部存储

```
/sdcard/Android/data/your.package/files/tessdata/
├── chi_sim.traineddata
├── eng.traineddata
└── jpn.traineddata
```

**优点：**
- 不增加 APK 体积
- 可按需下载
- 可动态更新

**缺点：**
- 首次使用需要下载
- 需要网络连接

### 使用 LanguagePackManager 管理语言包

```java
import com.orange.playerlibrary.ocr.LanguagePackManager;

// 创建管理器
LanguagePackManager manager = new LanguagePackManager(context);

// 检查语言包是否已安装
if (manager.isLanguageInstalled("chi_sim")) {
    // 已安装简体中文
    startOcr();
} else {
    // 下载语言包
    downloadLanguagePack();
}

// 下载语言包
private void downloadLanguagePack() {
    ProgressDialog dialog = new ProgressDialog(this);
    dialog.setTitle("下载语言包");
    dialog.setMessage("正在下载简体中文语言包...");
    dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    dialog.setMax(100);
    dialog.show();
    
    manager.downloadLanguage("chi_sim", new LanguagePackManager.DownloadCallback() {
        @Override
        public void onProgress(int progress, long downloaded, long total) {
            dialog.setProgress(progress);
            String text = String.format("下载中... %d%% (%s / %s)",
                progress,
                formatFileSize(downloaded),
                formatFileSize(total));
            dialog.setMessage(text);
        }
        
        @Override
        public void onSuccess() {
            dialog.dismiss();
            Toast.makeText(MainActivity.this, "下载成功", Toast.LENGTH_SHORT).show();
            startOcr();
        }
        
        @Override
        public void onError(String error) {
            dialog.dismiss();
            Toast.makeText(MainActivity.this, "下载失败：" + error, Toast.LENGTH_LONG).show();
        }
    });
}

// 格式化文件大小
private String formatFileSize(long bytes) {
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
    return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
}
```

### 获取已安装的语言包

```java
List<String> installed = manager.getInstalledLanguages();
for (String langCode : installed) {
    String displayName = LanguagePackManager.getLanguageDisplayName(langCode);
    Log.d(TAG, "已安装：" + displayName + " (" + langCode + ")");
}
```

### 删除语言包

```java
new AlertDialog.Builder(this)
    .setTitle("删除语言包")
    .setMessage("确定要删除简体中文语言包吗？")
    .setPositiveButton("删除", (dialog, which) -> {
        if (manager.deleteLanguage("chi_sim")) {
            Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
        }
    })
    .setNegativeButton("取消", null)
    .show();
```

---

## 使用方法

### 检查 OCR 功能是否可用

```java
import com.orange.playerlibrary.ocr.OcrAvailabilityChecker;

if (OcrAvailabilityChecker.isOcrTranslateAvailable()) {
    // OCR 功能可用
    startOcrTranslate();
} else {
    // 显示缺少的依赖
    String message = OcrAvailabilityChecker.getMissingDependenciesMessage();
    new AlertDialog.Builder(this)
        .setTitle("OCR 功能不可用")
        .setMessage(message)
        .setPositiveButton("查看文档", (dialog, which) -> {
            // 打开文档链接
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://github.com/706412584/orangeplayer/blob/main/docs/OCR_GUIDE.md"));
            startActivity(intent);
        })
        .show();
}
```

### 通过播放器 UI 启动 OCR

最简单的方式是通过播放器的 UI 启动 OCR：

```java
// 用户操作流程：
// 1. 点击字幕按钮
// 2. 选择 "OCR 翻译字幕"
// 3. 设置识别区域（拖动调整）
// 4. 选择源语言和目标语言
// 5. 点击开始识别
```

### 通过代码启动 OCR

```java
import com.orange.playerlibrary.VideoEventManager;

VideoEventManager eventManager = videoView.getVideoController().getVideoEventManager();
if (eventManager != null) {
    // 检查语言包
    LanguagePackManager manager = new LanguagePackManager(this);
    if (!manager.isLanguageInstalled("chi_sim")) {
        // 下载语言包
        downloadLanguagePack();
    } else {
        // 启动 OCR
        eventManager.startOcrTranslate();
    }
}
```

### 设置识别区域

OCR 支持自定义识别区域，只识别画面中的特定部分：

```java
// 通过 UI 设置（推荐）
// 用户可以拖动调整识别区域的位置和大小

// 或通过代码设置
// eventManager.setOcrRegion(left, top, right, bottom);
```

### 选择语言

```java
// 源语言（视频字幕的语言）
String[] sourceLangs = {"简体中文", "英语", "日语", "韩语"};
String[] sourceCodes = {"chi_sim", "eng", "jpn", "kor"};

// 目标语言（翻译成的语言）
String[] targetLangs = {"简体中文", "英语", "日语", "韩语"};
String[] targetCodes = {"zh", "en", "ja", "ko"};

// 通过 UI 选择（推荐）
// 用户可以在设置界面选择源语言和目标语言
```

---

## 功能特点

### 优点

- ✅ **硬字幕识别**：识别嵌入在视频中的字幕
- ✅ **实时翻译**：使用 ML Kit 翻译识别结果
- ✅ **区域选择**：可自定义识别区域，提高准确率
- ✅ **多语言支持**：支持中文、英语、日语、韩语等多种语言
- ✅ **离线识别**：Tesseract OCR 支持离线识别
- ✅ **在线翻译**：ML Kit 首次需要下载模型，之后可离线使用

### 限制

- ⚠️ **性能影响**：OCR 识别会占用较多 CPU 资源
- ⚠️ **识别准确率**：受字幕清晰度、字体、背景影响
- ⚠️ **识别延迟**：每次识别需要 0.5-2 秒
- ⚠️ **内存占用**：语言包会占用 50-100 MB 内存

---

## 注意事项

### 1. 识别准确率

影响识别准确率的因素：

- **字幕清晰度**：模糊的字幕识别率低
- **字体大小**：太小的字幕识别率低
- **背景干扰**：复杂背景会降低识别率
- **字体类型**：艺术字体识别率低

**提高识别率的方法：**

1. 调整识别区域，只包含字幕部分
2. 选择正确的源语言
3. 暂停视频进行识别
4. 使用高清视频源

### 2. 性能优化

OCR 识别会占用较多 CPU 资源，建议：

- 不要在低端设备上长时间使用
- 识别时降低视频分辨率
- 适当增加识别间隔
- 不需要时及时关闭 OCR

### 3. 翻译模型下载

ML Kit 翻译功能首次使用时需要下载语言模型：

- 每个语言模型约 30-50 MB
- 需要网络连接
- 下载后可离线使用
- 模型会自动更新

### 4. 存储空间

语言包会占用存储空间：

- 每个 OCR 语言包：1-4 MB
- 每个翻译模型：30-50 MB
- 建议预留至少 100 MB 空间

---

## 故障排查

### 问题 1：OCR 按钮显示"查看安装说明"

**原因：** 缺少 Tesseract 或 ML Kit 依赖

**解决方案：**

```gradle
dependencies {
    implementation 'cz.adaptech.tesseract4android:tesseract4android:4.7.0'
    implementation 'com.google.mlkit:translate:17.0.2'
}
```

### 问题 2：识别不出字幕

**可能原因：**
1. 语言包未安装或损坏
2. 识别区域设置不正确
3. 字幕太小或太模糊
4. 选择的源语言不正确

**解决方案：**
1. 重新下载语言包
2. 调整识别区域，只包含字幕
3. 使用高清视频源
4. 确认源语言设置正确

### 问题 3：识别速度很慢

**可能原因：**
1. 设备性能较低
2. 识别区域太大
3. 视频分辨率太高

**解决方案：**
1. 缩小识别区域
2. 降低视频分辨率
3. 增加识别间隔
4. 暂停视频进行识别

### 问题 4：翻译失败

**可能原因：**
1. 翻译模型未下载
2. 网络连接失败
3. 存储空间不足

**解决方案：**
1. 检查网络连接
2. 手动下载翻译模型
3. 清理存储空间
4. 查看 Logcat 日志

### 问题 5：识别结果乱码

**可能原因：**
1. 选择的语言包不正确
2. 字幕编码问题
3. 字体不支持

**解决方案：**
1. 确认源语言设置正确
2. 尝试其他语言包
3. 检查字幕字体

---

## 相关文档

- [安装指南](INSTALLATION.md)
- [语音识别指南](SPEECH_RECOGNITION.md)
- [API 文档](API.md)
- [常见问题](FAQ.md)

---

## 技术支持

如有问题或建议，欢迎联系：

- **QQ**: 706412584
- **GitHub Issues**: https://github.com/706412584/orangeplayer/issues
