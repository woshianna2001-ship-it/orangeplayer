# 迁移到本地 GSYVideoPlayer 模块

## 概述

从版本 1.1.0 开始，项目已从使用远程 GSYVideoPlayer 依赖迁移到使用本地源码模块。这样做的好处：

1. **完全控制**：可以自定义和修改 GSYVideoPlayer 源码
2. **版本一致**：所有模块使用相同版本，避免冲突
3. **支持 Android 4.0+**：本地模块已配置为支持 API 14+
4. **独立发布**：可以将所有模块发布到 Maven Central
5. **完全本地化**：包括 ExoPlayer 和 AliPlayer 内核也使用本地模块

## 更新内容

### 1. palyerlibrary 模块

**依赖配置** (`palyerlibrary/build.gradle`):

```gradle
dependencies {
    // GSYVideoPlayer 核心依赖 - 使用本地源码模块（支持 API 14）
    api project(':gsyVideoPlayer-base')
    api project(':gsyVideoPlayer-proxy_cache')
    api project(':gsyVideoPlayer-java')
    
    // Native so 文件模块 - compileOnly（让用户按需选择架构）
    compileOnly project(':gsyVideoPlayer-armv7a')
    compileOnly project(':gsyVideoPlayer-armv64')
    compileOnly project(':gsyVideoPlayer-x86')
    compileOnly project(':gsyVideoPlayer-x86_64')
}
```

**说明**：
- 核心模块使用 `api` 暴露给使用者
- so 文件模块使用 `compileOnly`，让用户按需选择架构

### 2. app 模块

**依赖配置** (`app/build.gradle`):

```gradle
dependencies {
    // 自定义播放器模块
    implementation project(':palyerlibrary')
    
    // 播放器内核 - 使用本地 GSYVideoPlayer 模块
    implementation project(':gsyVideoPlayer-armv7a')  // ARM 32位架构
    implementation project(':gsyVideoPlayer-armv64')  // ARM 64位架构
    implementation project(':gsyVideoPlayer-x86')     // x86 32位架构
    implementation project(':gsyVideoPlayer-x86_64')  // x86 64位架构
    
    // ExoPlayer 内核（可选，需要 API 21+）- 使用本地模块
    implementation project(':gsyVideoPlayer-exo_player2')
    
    // 阿里云播放器内核（可选）- 使用本地模块
    implementation(project(':gsyVideoPlayer-aliplay')) {
        // 排除内置的阿里云播放器 SDK，使用 5.4.7.1 免授权版本
        exclude group: 'com.aliyun.sdk.android', module: 'AliyunPlayer'
        exclude group: 'com.alivc.conan', module: 'AlivcConan'
    }
    implementation 'com.aliyun.sdk.android:AliyunPlayer:5.4.7.1-full'
}
```

**重要**：
- 完全使用本地模块，不再依赖远程的 GSYVideoPlayer
- ExoPlayer 和 AliPlayer 内核也使用本地模块
- 只需排除阿里云播放器内核中的 SDK，使用免授权版本

## 本地 GSYVideoPlayer 模块列表

项目包含以下 GSYVideoPlayer 模块（位于 `GSYVideoPlayer-source/` 目录）：

| 模块名 | 说明 | 类型 |
|--------|------|------|
| gsyVideoPlayer-base | 核心基础库 | 必需 |
| gsyVideoPlayer-proxy_cache | 视频缓存代理 | 必需 |
| gsyVideoPlayer-java | Java 层实现 | 必需 |
| gsyVideoPlayer-exo_player2 | ExoPlayer 内核 | 可选 |
| gsyVideoPlayer-aliplay | 阿里云播放器内核 | 可选 |
| gsyVideoPlayer-armv7a | ARM 32位 so 文件 | 可选 |
| gsyVideoPlayer-armv64 | ARM 64位 so 文件 | 可选 |
| gsyVideoPlayer-x86 | x86 32位 so 文件 | 可选 |
| gsyVideoPlayer-x86_64 | x86 64位 so 文件 | 可选 |

## 构建验证

更新后的构建结果：

```bash
./gradlew :app:assembleDebug
# BUILD SUCCESSFUL in 1m 15s
# APK 大小: 190.98 MB
```

依赖树验证：

```bash
./gradlew :app:dependencies --configuration debugRuntimeClasspath
```

应该看到：
- ✅ `project :gsyVideoPlayer-base`
- ✅ `project :gsyVideoPlayer-proxy_cache`
- ✅ `project :gsyVideoPlayer-java`
- ✅ `project :gsyVideoPlayer-exo_player2`
- ✅ `project :gsyVideoPlayer-aliplay`
- ✅ `project :gsyVideoPlayer-armv7a`
- ✅ `project :gsyVideoPlayer-armv64`
- ✅ `project :gsyVideoPlayer-x86`
- ✅ `project :gsyVideoPlayer-x86_64`

## 常见问题

### Q: 为什么要使用本地模块？

**优势**：
1. 完全控制源码，可以自定义修改
2. 避免版本冲突，所有模块使用相同版本
3. 支持 Android 4.0+（API 14+）
4. 可以独立发布到 Maven Central
5. 不依赖远程仓库，构建更稳定

### Q: 如何选择需要的架构？

根据目标设备选择：

- **全架构支持**（推荐）：添加所有 4 个架构模块
- **仅 ARM 设备**：只添加 armv7a 和 armv64
- **减小 APK 体积**：使用 App Bundle 或 APK 分包

### Q: 本地模块支持哪些 Android 版本？

- **最低版本**：Android 4.0 (API 14)
- **目标版本**：Android 14 (API 36)
- **IJKPlayer so 文件**：支持 API 9+
- **ExoPlayer 内核**：需要 API 21+

### Q: 如何发布所有模块？

使用提供的脚本：

```bash
# 发布到本地 Maven 仓库
publish-all-modules.bat

# 测试发布并创建 bundle
test-publish-all.bat

# 上传到 Maven Central
cd maven-central
publish.bat
# 选择选项 3
```

## 相关文档

- [Android 4.0+ 支持说明](ANDROID_4.4_FINAL_CONCLUSION.md)
- [发布到 Maven Central](PUBLISH_LEGACY_SDK.md)
- [SDK 集成指南](SDK_INTEGRATION_GUIDE.md)
