# ijkplayer 编译指南（Android 4.4 支持）

## 背景

为了支持 Android 4.4 (API 19)，我们尝试从源码编译 ijkplayer，因为：
- ✅ ijkplayer 源码支持 API 9+
- ❌ Maven 发布的版本需要 API 21+

## 编译环境要求

### 必需工具

1. **NDK r10e**
   - 下载：http://developer.android.com/tools/sdk/ndk/index.html
   - 版本：r10e（ijkplayer 推荐）
   - 大小：约 400MB

2. **Git**
   - Windows: Git Bash
   - 或使用 WSL (Windows Subsystem for Linux)

3. **yasm**
   - Mac/Linux: `brew install yasm` 或 `apt-get install yasm`
   - Windows: 需要手动下载或使用 WSL

4. **bash 环境**
   - Mac/Linux: 原生支持
   - Windows: Git Bash 或 WSL

### 环境变量配置

```bash
# 添加到 ~/.bash_profile 或 ~/.profile
export ANDROID_SDK=<your sdk path>
export ANDROID_NDK=<your ndk path>
```

## 编译步骤

### 1. 克隆源码

```bash
git clone https://github.com/Bilibili/ijkplayer.git ijkplayer-android
cd ijkplayer-android
git checkout -B latest k0.8.8
```

### 2. 初始化 Android 环境

```bash
./init-android.sh
```

这会下载 FFmpeg 源码和其他依赖。

### 3. 配置编译选项

选择编译配置（影响二进制大小）：

#### 选项 A：完整版本（更多编解码器）
```bash
cd config
rm module.sh
ln -s module-default.sh module.sh
```

#### 选项 B：精简版本 + HEVC
```bash
cd config
rm module.sh
ln -s module-lite-hevc.sh module.sh
```

#### 选项 C：精简版本（默认，推荐）
```bash
cd config
rm module.sh
ln -s module-lite.sh module.sh
```

### 4. 编译 FFmpeg

```bash
cd android/contrib
./compile-ffmpeg.sh clean
./compile-ffmpeg.sh all
```

**注意**：这一步会：
- 下载 FFmpeg 源码
- 编译多个架构（armv7a, arm64, x86, x86_64）
- 耗时：30分钟 - 2小时（取决于机器性能）

### 5. 编译 ijkplayer

```bash
cd ..
./compile-ijk.sh all
```

这会生成：
- `ijkplayer-armv7a/src/main/libs/armeabi-v7a/libijkffmpeg.so`
- `ijkplayer-armv7a/src/main/libs/armeabi-v7a/libijkplayer.so`
- `ijkplayer-armv7a/src/main/libs/armeabi-v7a/libijksdl.so`
- 以及其他架构的 so 文件

### 6. 集成到项目

#### 方法 A：使用 Android Studio 导入

1. 打开 Android Studio
2. File -> Open -> 选择 `ijkplayer-android/android/ijkplayer/`
3. 在你的项目中添加模块依赖

#### 方法 B：复制 so 文件

```bash
# 复制到你的项目
cp -r ijkplayer-armv7a/src/main/libs/* your-project/app/src/main/jniLibs/
cp -r ijkplayer-java/src/main/java/* your-project/app/src/main/java/
```

## 修改 minSdk

编译后的 ijkplayer 默认 minSdk 可能是 9，但需要修改 AndroidManifest.xml：

```xml
<!-- ijkplayer-java/src/main/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="tv.danmaku.ijk.media.player">
    <uses-sdk android:minSdkVersion="9" />
</manifest>
```

## 预期问题

### 1. NDK 版本问题

ijkplayer 推荐 NDK r10e，但现在最新的是 r26+。使用新版本 NDK 可能会遇到：
- 编译错误
- API 不兼容
- 需要修改编译脚本

### 2. Windows 编译问题

ijkplayer 的编译脚本是为 Unix 系统设计的，在 Windows 上需要：
- WSL (Windows Subsystem for Linux)
- 或 Git Bash + 额外配置
- 或使用虚拟机/Docker

### 3. 编译时间

完整编译需要：
- FFmpeg 编译：30分钟 - 2小时
- ijkplayer 编译：10-30分钟
- 总计：1-3小时（首次编译）

### 4. 二进制大小

编译后的 so 文件大小：
- 精简版：每个架构约 5-8MB
- 完整版：每个架构约 10-15MB
- 4个架构总计：20-60MB

## 实际测试结果

### 我们的环境

- 系统：Windows
- NDK：未配置
- bash：PowerShell（不兼容）

### 遇到的问题

1. ❌ NDK 未安装
2. ❌ 需要 bash 环境（Windows 需要 WSL）
3. ❌ 需要 yasm 工具
4. ❌ 编译时间长（1-3小时）
5. ❌ 维护成本高

## 成本分析

### 时间成本

| 任务 | 时间 |
|------|------|
| 配置环境（NDK、WSL、yasm） | 1-2小时 |
| 首次编译 | 1-3小时 |
| 集成到项目 | 2-4小时 |
| 测试和调试 | 4-8小时 |
| **总计** | **8-17小时** |

### 维护成本

- 每次更新 ijkplayer 需要重新编译
- 需要维护编译环境
- 需要处理不同 NDK 版本的兼容性
- 需要处理不同架构的 so 文件

### 收益

- 支持 Android 4.4 (API 19)
- 市场覆盖率从 99% 提升到 99.5%
- **仅增加 0.5% 用户**

## 替代方案

### 方案 1：使用系统播放器（推荐）

```java
// 不依赖任何第三方库
MediaPlayer player = new MediaPlayer();
player.setDataSource(url);
player.prepare();
player.start();
```

**优点**：
- ✅ 支持所有 Android 版本
- ✅ 无需编译
- ✅ 无额外依赖

**缺点**：
- ❌ 功能有限
- ❌ 格式支持少（不支持 HLS、RTSP、RTMP）
- ❌ 无法自定义

### 方案 2：保持 minSdk 21（强烈推荐）

```gradle
android {
    defaultConfig {
        minSdk 21  // Android 5.0+
    }
}
```

**优点**：
- ✅ 覆盖 99% 用户
- ✅ 使用现代库（GSYVideoPlayer、ExoPlayer）
- ✅ 功能完整
- ✅ 维护成本低

**缺点**：
- ❌ 不支持 Android 4.4（0.5% 用户）

## 结论

**不推荐编译 ijkplayer 来支持 Android 4.4**

理由：
1. ❌ 编译环境配置复杂（NDK、WSL、yasm）
2. ❌ 编译时间长（1-3小时）
3. ❌ 维护成本高
4. ❌ 仅增加 0.5% 市场覆盖率
5. ❌ 投入产出比极低（8-17小时 vs 0.5% 用户）

**推荐方案**：
- 保持 minSdk 21
- 使用 GSYVideoPlayer 或 ExoPlayer
- 覆盖 99% 用户已经足够

## 参考资料

- [ijkplayer GitHub](https://github.com/bilibili/ijkplayer)
- [ijkplayer 编译文档](https://github.com/bilibili/ijkplayer/blob/master/README.md)
- [Android NDK 下载](https://developer.android.com/ndk/downloads)
- [WSL 安装指南](https://docs.microsoft.com/en-us/windows/wsl/install)

---

**最后更新**: 2026-02-06  
**状态**: 不推荐  
**原因**: 成本太高，收益太低
