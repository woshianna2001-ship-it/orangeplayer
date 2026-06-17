# OrangePlayer SDK 集成指南（支持 Android 4.0+）

## 📦 已发布的模块

所有模块已发布到本地 Maven 仓库，版本号：`1.1.0-api14`

| 模块 | 大小 | 说明 |
|------|------|------|
| **orangeplayer** | 962 KB | 核心库（必需） |
| **gsyVideoPlayer-base** | 5 KB | 基础模块（必需） |
| **gsyVideoPlayer-proxy_cache** | - | 缓存代理（必需） |
| **gsyVideoPlayer-java** | 338 KB | Java 层实现（必需） |
| **gsyVideoPlayer-armv7a** | 2.4 MB | ARM 32位 so（至少选一个） |
| **gsyVideoPlayer-armv64** | 3.2 MB | ARM 64位 so（至少选一个） |
| **gsyVideoPlayer-x86** | 2.9 MB | x86 32位 so（可选） |
| **gsyVideoPlayer-x86_64** | 3.5 MB | x86 64位 so（可选） |

---

## 🚀 快速集成

### 方式 1: 使用本地 Maven 仓库（测试用）

```gradle
repositories {
    mavenLocal()  // 使用本地 .m2 仓库
}

dependencies {
    // 核心库
    implementation 'io.github.706412584:orangeplayer:1.1.0-api14'
    
    // 选择需要的 CPU 架构（至少一个）
    implementation 'io.github.706412584:gsyVideoPlayer-armv7a:1.1.0-api14'
    implementation 'io.github.706412584:gsyVideoPlayer-armv64:1.1.0-api14'
}
```

### 方式 2: 使用 JitPack（推荐）

1. 推送代码到 GitHub 并创建 tag：
```bash
git add .
git commit -m "Release v1.1.0-api14"
git tag v1.1.0-api14
git push origin main
git push origin v1.1.0-api14
```

2. 在项目中使用：
```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.706412584:orangeplayer:v1.1.0-api14'
}
```

### 方式 3: 发布到 Maven Central

```bash
# 配置 gradle.properties 或环境变量后执行
./gradlew publishAllPublicationsToSonatypeRepository
```

---

## 📱 集成示例

### 最小配置（仅 ARM 设备）

```gradle
android {
    defaultConfig {
        minSdk 14  // 支持 Android 4.0+
        
        // 如果方法数超过 64K，启用 MultiDex
        multiDexEnabled true
    }
}

dependencies {
    // 核心库（包含 base、proxy_cache、java 模块）
    implementation 'io.github.706412584:orangeplayer:1.1.0-api14'
    
    // ARM 32位设备（大部分老设备）
    implementation 'io.github.706412584:gsyVideoPlayer-armv7a:1.1.0-api14'
    
    // MultiDex 支持（Android 4.x 必需）
    implementation 'androidx.multidex:multidex:2.0.1'
}
```

### 完整配置（所有架构 + 可选功能）

```gradle
dependencies {
    // 核心库
    implementation 'io.github.706412584:orangeplayer:1.1.0-api14'
    
    // 所有 CPU 架构
    implementation 'io.github.706412584:gsyVideoPlayer-armv7a:1.1.0-api14'
    implementation 'io.github.706412584:gsyVideoPlayer-armv64:1.1.0-api14'
    implementation 'io.github.706412584:gsyVideoPlayer-x86:1.1.0-api14'
    implementation 'io.github.706412584:gsyVideoPlayer-x86_64:1.1.0-api14'
    
    // 可选功能
    implementation 'com.github.bilibili:DanmakuFlameMaster:0.9.25'  // 弹幕
    implementation 'com.github.uaoan:UaoanDLNA:1.0.1'  // DLNA（需要 API 19+）
    implementation 'com.squareup.okhttp3:okhttp:3.12.13'  // DLNA 依赖
    
    // MultiDex
    implementation 'androidx.multidex:multidex:2.0.1'
}
```

### 按需加载架构（推荐）

```gradle
android {
    defaultConfig {
        // 根据构建类型选择架构
        ndk {
            // Debug: 只包含 ARM 32位（加快编译）
            // Release: 包含所有架构
            abiFilters 'armeabi-v7a', 'arm64-v8a'
        }
    }
    
    splits {
        abi {
            enable true
            reset()
            include 'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64'
            universalApk true  // 生成包含所有架构的通用 APK
        }
    }
}

dependencies {
    implementation 'io.github.706412584:orangeplayer:1.1.0-api14'
    
    // 所有架构（构建时会根据 splits 配置自动分包）
    implementation 'io.github.706412584:gsyVideoPlayer-armv7a:1.1.0-api14'
    implementation 'io.github.706412584:gsyVideoPlayer-armv64:1.1.0-api14'
    implementation 'io.github.706412584:gsyVideoPlayer-x86:1.1.0-api14'
    implementation 'io.github.706412584:gsyVideoPlayer-x86_64:1.1.0-api14'
}
```

---

## 🎯 版本选择建议

### 根据目标用户选择

| 目标用户 | minSdk | 依赖配置 |
|---------|--------|---------|
| **现代设备** | 21+ | 使用原版 GSYVideoPlayer（支持 ExoPlayer） |
| **兼容老设备** | 14+ | 使用 OrangePlayer Legacy（本 SDK） |
| **仅 Android 4.4+** | 19+ | 使用本 SDK + DLNA 功能 |

### 根据 APK 大小选择

| 配置 | APK 增加大小 | 支持设备 |
|------|------------|---------|
| 仅 armv7a | ~3 MB | 大部分老设备 |
| armv7a + armv64 | ~6 MB | 所有 ARM 设备 |
| 全架构 | ~12 MB | 所有设备 |

---

## 📝 使用示例

### 基础播放

```java
OrangevideoView videoPlayer = findViewById(R.id.video_player);

// 播放网络视频
videoPlayer.setUp("https://example.com/video.mp4", true, "视频标题");
videoPlayer.startPlayLogic();
```

### 版本兼容处理

```java
// 检查 Android 版本并启用相应功能
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
    // Android 4.4+ 支持 DLNA
    videoPlayer.setDLNAEnabled(true);
} else {
    // Android 4.0-4.3 不支持 DLNA
    dlnaButton.setVisibility(View.GONE);
}
```

---

## 🔧 发布到 Maven Central

### 1. 配置凭证

在 `gradle.properties` 中添加：

```properties
ossrhUsername=your_username
ossrhPassword=your_password
signing.keyId=your_key_id
signing.password=your_key_password
signing.secretKeyRingFile=/path/to/secring.gpg
```

### 2. 发布所有模块

```bash
# Windows
publish-all-modules.bat

# 或手动发布
./gradlew publishAllPublicationsToSonatypeRepository
```

### 3. 登录 Maven Central 完成发布

访问：https://s01.oss.sonatype.org/

---

## 📊 模块依赖关系

```
orangeplayer (核心库)
├── gsyVideoPlayer-base (基础模块)
├── gsyVideoPlayer-proxy_cache (缓存代理)
├── gsyVideoPlayer-java (Java 实现)
│   ├── gsyVideoPlayer-base
│   └── gsyVideoPlayer-proxy_cache
└── [用户选择] so 文件模块
    ├── gsyVideoPlayer-armv7a
    ├── gsyVideoPlayer-armv64
    ├── gsyVideoPlayer-x86
    └── gsyVideoPlayer-x86_64
```

**注意**：
- `orangeplayer` 已经包含了 `base`、`proxy_cache`、`java` 模块的依赖
- 用户只需要额外添加 so 文件模块（至少一个架构）

---

## 🐛 常见问题

### 1. 找不到 so 文件

**错误**: `UnsatisfiedLinkError: dlopen failed`

**解决**: 确保添加了至少一个架构的依赖
```gradle
implementation 'io.github.706412584:gsyVideoPlayer-armv7a:1.1.0-api14'
```

### 2. 方法数超过 64K

**错误**: `Cannot fit requested classes in a single dex file`

**解决**: 启用 MultiDex
```gradle
android {
    defaultConfig {
        multiDexEnabled true
    }
}
dependencies {
    implementation 'androidx.multidex:multidex:2.0.1'
}
```

### 3. 依赖冲突

**错误**: `Duplicate class found`

**解决**: 排除冲突的依赖
```gradle
implementation('io.github.706412584:orangeplayer:1.1.0-api14') {
    exclude group: 'androidx.appcompat', module: 'appcompat'
}
```

---

## 📚 相关文档

- [发布指南](PUBLISH_LEGACY_SDK.md)
- [使用示例](SDK_USAGE_EXAMPLE.md)
- [Android 4.0+ 兼容说明](ANDROID_4.4_FINAL_CONCLUSION.md)
- [API 文档](API.md)

---

## 🎉 总结

通过将 GSYVideoPlayer 的预编译 so 文件打包为独立模块，用户现在可以：

✅ **简单集成** - 只需添加依赖，无需手动编译  
✅ **按需选择** - 根据目标设备选择架构  
✅ **版本管理** - 统一的版本号管理  
✅ **兼容性好** - 支持 Android 4.0+  
✅ **体积可控** - 按需选择架构，控制 APK 大小

用户不再需要克隆 GSYVideoPlayer 源码或手动编译 IJKPlayer！
