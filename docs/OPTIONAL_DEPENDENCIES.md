# 可选依赖说明

OrangePlayer 的某些功能需要额外的依赖，这些依赖在 Maven Central 上不可用，需要使用者手动添加。

## 弹幕功能

如果需要使用弹幕功能，需要添加 JitPack 仓库和 DanmakuFlameMaster 依赖：

### 1. 添加 JitPack 仓库

在 `settings.gradle` 中：

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }  // 添加 JitPack
    }
}
```

### 2. 添加弹幕库依赖

在 `app/build.gradle` 中：

```gradle
dependencies {
    implementation 'io.github.706412584:orangeplayer:1.0.7'
    
    // 弹幕功能（可选）
    implementation 'com.github.bilibili:DanmakuFlameMaster:0.9.25'
}
```

## 阿里云播放器

如果需要使用阿里云播放器内核，需要添加阿里云仓库和 SDK：

### 1. 添加阿里云仓库

在 `settings.gradle` 中：

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://maven.aliyun.com/repository/releases' }  // 阿里云仓库
    }
}
```

### 2. 添加阿里云播放器依赖

在 `app/build.gradle` 中：

```gradle
dependencies {
    implementation 'io.github.706412584:orangeplayer:1.0.7'
    
    // 阿里云播放器（可选）
    implementation 'com.aliyun.sdk.android:AliyunPlayer:5.4.7.1-full'
}
```

> ⚠️ **注意**：阿里云播放器 5.4.x 及以上版本需要 License 授权。如果不想申请授权，可以使用 5.3.x 版本。

## 完整配置示例

如果需要所有功能，完整配置如下：

**settings.gradle:**

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }  // 弹幕库
        maven { url 'https://maven.aliyun.com/repository/releases' }  // 阿里云播放器
    }
}
```

**app/build.gradle:**

```gradle
dependencies {
    // OrangePlayer 核心库
    implementation 'io.github.706412584:orangeplayer:1.0.7'
    
    // 可选：弹幕功能
    implementation 'com.github.bilibili:DanmakuFlameMaster:0.9.25'
    
    // 可选：阿里云播放器
    implementation 'com.aliyun.sdk.android:AliyunPlayer:5.4.7.1-full'
}
```

## 功能对照表

| 功能 | 是否需要额外依赖 | 依赖库 |
|------|----------------|--------|
| 基础播放 | ❌ 否 | - |
| ExoPlayer 内核 | ❌ 否 | 已包含 |
| 系统播放器内核 | ❌ 否 | 已包含 |
| 弹幕 | ✅ 是 | DanmakuFlameMaster |
| 阿里云播放器 | ✅ 是 | AliyunPlayer |
| OCR 字幕识别 | ❌ 否 | 已包含 |
| 语音识别 | ❌ 否 | 已包含 |
| 字幕翻译 | ❌ 否 | 已包含 |

## 为什么这样设计？

Maven Central 要求所有依赖都必须在 Maven Central 上可用。但是：

- `DanmakuFlameMaster` 只在 JitPack 上发布
- `AliyunPlayer` 只在阿里云私有仓库发布

为了让 OrangePlayer 能够成功发布到 Maven Central，我们将这些依赖设置为 `compileOnly`（编译时依赖），不会传递给使用者。使用者可以根据需要自行添加。

这样做的好处：
1. ✅ 可以发布到 Maven Central
2. ✅ 不需要弹幕/阿里云播放器的用户不会引入额外依赖
3. ✅ 需要这些功能的用户可以灵活选择版本
