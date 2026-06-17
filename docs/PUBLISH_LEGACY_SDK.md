# 发布 OrangePlayer Legacy SDK 指南

## 概述

OrangePlayer Legacy SDK 是支持 Android 4.0+ (API 14+) 的视频播放器库，基于 GSYVideoPlayer 和 IJKPlayer。

---

## 📦 SDK 信息

- **Group ID**: `io.github.706412584`
- **Artifact ID**: `orangeplayer`
- **Version**: `1.1.0-api14`
- **最低支持**: Android 4.0 (API 14)
- **目标版本**: Android 14 (API 36)

---

## 🔧 发布前准备

### 1. 确保 GSYVideoPlayer 模块已包含

在 `settings.gradle` 中应该已经包含：

```gradle
include ':gsyVideoPlayer-base'
include ':gsyVideoPlayer-proxy_cache'
include ':gsyVideoPlayer-java'
include ':gsyVideoPlayer-armv7a'
include ':gsyVideoPlayer-armv64'
include ':gsyVideoPlayer-x86'
include ':gsyVideoPlayer-x86_64'
```

### 2. 配置发布凭证

在项目根目录创建 `gradle.properties` 文件（不要提交到 Git）：

```properties
# Maven Central 凭证
ossrhUsername=your_username
ossrhPassword=your_password

# GPG 签名配置
signing.keyId=your_key_id
signing.password=your_key_password
signing.secretKeyRingFile=/path/to/secring.gpg
```

或者使用环境变量：

```bash
export OSSRH_USERNAME=your_username
export OSSRH_PASSWORD=your_password
export SIGNING_KEY_ID=your_key_id
export SIGNING_PASSWORD=your_key_password
export SIGNING_SECRET_KEY_RING_FILE=/path/to/secring.gpg
```

---

## 📤 发布步骤

### 方式 1: 发布到本地 Maven 仓库（测试用）

```bash
# 发布到项目的 build/repo 目录
./gradlew :palyerlibrary:publishToMavenLocal

# 或发布到用户主目录的 .m2 仓库
./gradlew :palyerlibrary:publishMavenPublicationToLocalRepository
```

发布后的位置：
- 项目本地：`build/repo/io/github/706412584/orangeplayer/1.1.0-api14/`
- 用户 .m2：`~/.m2/repository/io/github/706412584/orangeplayer/1.1.0-api14/`

### 方式 2: 发布到 Maven Central

```bash
# 1. 清理并构建
./gradlew clean :palyerlibrary:build

# 2. 发布到 Maven Central Staging
./gradlew :palyerlibrary:publishMavenPublicationToSonatypeRepository

# 3. 登录 Maven Central 后台完成发布
# https://s01.oss.sonatype.org/
```

### 方式 3: 使用 JitPack（最简单）

1. 确保代码已推送到 GitHub
2. 创建一个 Git tag：

```bash
git tag v1.1.0-api14
git push origin v1.1.0-api14
```

3. 访问 JitPack 构建：
   - https://jitpack.io/#706412584/orangeplayer

4. 用户可以直接使用：

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.706412584:orangeplayer:v1.1.0-api14'
}
```

---

## 📚 使用文档

### 基础集成

```gradle
// 1. 添加仓库
repositories {
    mavenCentral()
    // 或使用 JitPack
    maven { url 'https://jitpack.io' }
}

// 2. 添加依赖
dependencies {
    // 核心库（必需）
    implementation 'io.github.706412584:orangeplayer:1.1.0-api14'
    
    // 选择至少一个 CPU 架构的 so 文件（必需）
    implementation 'com.shuyu.gsyvideoplayer:gsyVideoPlayer-armv7a:1.0.0'  // ARM 32位
    implementation 'com.shuyu.gsyvideoplayer:gsyVideoPlayer-armv64:1.0.0'  // ARM 64位
    
    // 可选功能
    implementation 'com.github.bilibili:DanmakuFlameMaster:0.9.25'  // 弹幕
    implementation 'com.github.uaoan:UaoanDLNA:1.0.1'  // DLNA 投屏（需要 API 19+）
}
```

### 最小配置（仅支持 ARM 设备）

```gradle
dependencies {
    implementation 'io.github.706412584:orangeplayer:1.1.0-api14'
    implementation 'com.shuyu.gsyvideoplayer:gsyVideoPlayer-armv7a:1.0.0'
}
```

### 完整配置（支持所有架构 + 可选功能）

```gradle
dependencies {
    // 核心库
    implementation 'io.github.706412584:orangeplayer:1.1.0-api14'
    
    // 所有架构的 so 文件
    implementation 'com.shuyu.gsyvideoplayer:gsyVideoPlayer-armv7a:1.0.0'
    implementation 'com.shuyu.gsyvideoplayer:gsyVideoPlayer-armv64:1.0.0'
    implementation 'com.shuyu.gsyvideoplayer:gsyVideoPlayer-x86:1.0.0'
    implementation 'com.shuyu.gsyvideoplayer:gsyVideoPlayer-x86_64:1.0.0'
    
    // 可选功能
    implementation 'com.github.bilibili:DanmakuFlameMaster:0.9.25'  // 弹幕
    implementation 'com.github.uaoan:UaoanDLNA:1.0.1'  // DLNA（API 19+）
    implementation 'com.squareup.okhttp3:okhttp:3.12.13'  // DLNA 依赖
}
```

---

## 🎯 版本兼容性

| Android 版本 | API Level | 支持状态 | 功能限制 |
|-------------|-----------|---------|---------|
| Android 4.0-4.3 | 14-18 | ✅ 完全支持 | DLNA 不可用 |
| Android 4.4+ | 19+ | ✅ 完全支持 | 所有功能可用 |
| Android 5.0+ | 21+ | ✅ 完全支持 | 所有功能可用 |

---

## 📋 发布检查清单

发布前请确认：

- [ ] 版本号已更新（`maven-publish.gradle` 中的 `pomVersion`）
- [ ] minSdk 设置为 14（`palyerlibrary/build.gradle`）
- [ ] GSYVideoPlayer 模块的 minSdk 设置为 14（`gradle/base.gradle`）
- [ ] 依赖版本都兼容 API 14
- [ ] 代码已编译通过：`./gradlew :palyerlibrary:build`
- [ ] 已生成 AAR 文件：`palyerlibrary/build/outputs/aar/palyerlibrary-release.aar`
- [ ] 已测试本地发布：`./gradlew :palyerlibrary:publishToMavenLocal`
- [ ] 更新了 CHANGELOG.md
- [ ] 更新了 README.md 中的版本号
- [ ] 创建了 Git tag（如果使用 JitPack）

---

## 🔍 验证发布

### 验证本地发布

```bash
# 1. 发布到本地
./gradlew :palyerlibrary:publishToMavenLocal

# 2. 在测试项目中使用
repositories {
    mavenLocal()
}

dependencies {
    implementation 'io.github.706412584:orangeplayer:1.1.0-api14'
}

# 3. 同步并构建测试项目
./gradlew clean build
```

### 验证 JitPack 发布

1. 访问：https://jitpack.io/#706412584/orangeplayer/v1.1.0-api14
2. 查看构建日志
3. 确认构建成功（绿色勾）

---

## 🐛 常见问题

### 1. 签名失败

**问题**: `signing.keyId` 未配置

**解决**: 
- 确保 GPG 密钥已生成
- 配置 `gradle.properties` 或环境变量
- 或在 JitPack 环境中禁用签名（已自动处理）

### 2. 依赖冲突

**问题**: GSYVideoPlayer 模块找不到

**解决**:
- 确保 `settings.gradle` 中已包含所有 GSYVideoPlayer 模块
- 确保 `gradle/` 目录下的配置文件已复制

### 3. minSdk 不匹配

**问题**: 依赖库需要更高的 API 版本

**解决**:
- 检查所有依赖的 minSdk 要求
- 使用兼容 API 14 的版本
- 将高版本依赖改为 `compileOnly`

### 4. AAR 文件过大

**问题**: AAR 包含了所有架构的 so 文件

**解决**:
- 将 so 文件模块改为 `compileOnly`
- 让用户按需选择架构
- 当前配置已优化，AAR 不包含 so 文件

---

## 📖 相关文档

- [Android 4.0+ 兼容版本结论](ANDROID_4.4_FINAL_CONCLUSION.md)
- [Maven Central 发布指南](MAVEN_CENTRAL_PUBLISH_SUMMARY.md)
- [项目 README](../README.md)

---

## 🎉 发布后

发布成功后，记得：

1. 更新 README.md 中的安装说明
2. 在 GitHub 创建 Release
3. 通知用户新版本可用
4. 更新示例项目
5. 撰写发布公告
