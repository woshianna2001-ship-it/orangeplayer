# GSYVideoPlayer-source 子模块修改清单

## 📋 概述

GSYVideoPlayer-source 子模块包含了以下修改，这些修改在更新上游时需要特别注意。

---

## 🔧 重要修改

### 1. Maven 发布配置修改

**目的**：发布到你自己的 Maven 仓库（io.github.706412584）

**修改文件**：
- `gsyVideoPlayer-base/build.gradle`
- `gsyVideoPlayer-java/build.gradle`
- `gsyVideoPlayer-proxy_cache/build.gradle`
- `gsyVideoPlayer-exo_player2/build.gradle`
- `gsyVideoPlayer-aliplay/build.gradle`
- 所有 SO 模块的 build.gradle

**修改内容**：
```gradle
ext {
    PROJ_GROUP = 'io.github.706412584'  // 改为你的 group ID
    PROJ_ARTIFACTID = 'gsyVideoPlayer-base'
    PROJ_VERSION = rootProject.ext.pomVersion
}
```

**原始值**：
```gradle
// 官方使用 com.shuyu 或 io.github.carguo
```

---

### 2. Namespace 修改

**目的**：统一包名，避免冲突

**修改文件**：
- `gsyVideoPlayer-base/build.gradle`
- `gsyVideoPlayer-java/build.gradle`
- `gsyVideoPlayer-proxy_cache/build.gradle`
- 所有模块的 build.gradle

**修改内容**：
```gradle
android {
    namespace 'com.shuyu.gsyvideoplayer.base'  // 统一使用 gsyvideoplayer
}
```

**原始值**：
```gradle
android {
    namespace 'com.shuyu.gsy.base'  // 官方使用 gsy 缩写
}
```

---

### 3. 版本号修改

**文件**：`gradle.properties`

**修改内容**：
```properties
PROJ_VERSION=12.0.0  // 固定在 12.0.0
```

**原始值**：
```properties
PROJ_VERSION=12.1.0  // 上游最新版本
```

**原因**：你的 fork 基于 12.0.0，添加了自定义修改

---

### 4. IJK SO 文件更新

**目的**：支持 16K Page Size 和加密视频

**修改文件**：
- `gsyVideoPlayer-armv64/src/main/jniLibs/arm64-v8a/*.so`
- `gsyVideoPlayer-ex_so/src/main/jniLibs/**/*.so`

**修改内容**：
- 使用自己编译的 IJK SO 文件
- 包含 16K Page Size 补丁
- 包含加密视频支持（ex_so）

**原始值**：
- 官方预编译的 SO 文件
- 不支持 16K Page Size
- ex_so 可能不支持某些加密格式

---

### 5. 新增文件

**16K 补丁**：
- `16kpatch/README.md`
- `16kpatch/ndk_r22_16k_commit.patch`
- `16kpatch/ndk_r22_ffmpeg_n4.3_ijk.patch`
- `16kpatch/ndk_r22_ijkyuv.patch`
- `16kpatch/ndk_r22_soundtouch.patch`

**FFmpeg 配置**：
- `module-lite-more-fixed.sh` - 修复版配置（添加了缺失的协议）
- `module-lite-more-with-tools.sh` - 带命令行工具的配置

---

## 📊 修改文件完整列表

### 构建配置文件

| 文件 | 修改类型 | 重要性 |
|------|---------|--------|
| `gradle.properties` | 版本号 | ⭐⭐⭐ |
| `gradle/base.gradle` | 构建配置 | ⭐⭐ |
| `gradle/dependencies.gradle` | 依赖版本 | ⭐⭐ |

### 模块 build.gradle

| 文件 | 修改内容 | 重要性 |
|------|---------|--------|
| `gsyVideoPlayer-base/build.gradle` | namespace + Maven 配置 | ⭐⭐⭐⭐ |
| `gsyVideoPlayer-java/build.gradle` | namespace + Maven 配置 | ⭐⭐⭐⭐ |
| `gsyVideoPlayer-proxy_cache/build.gradle` | Maven 配置 | ⭐⭐⭐ |
| `gsyVideoPlayer-exo_player2/build.gradle` | Maven 配置 | ⭐⭐⭐ |
| `gsyVideoPlayer-aliplay/build.gradle` | Maven 配置 | ⭐⭐⭐ |
| `gsyVideoPlayer-armv7a/build.gradle` | Maven 配置 | ⭐⭐ |
| `gsyVideoPlayer-armv64/build.gradle` | Maven 配置 | ⭐⭐ |
| `gsyVideoPlayer-x86/build.gradle` | Maven 配置 | ⭐⭐ |
| `gsyVideoPlayer-x86_64/build.gradle` | Maven 配置 | ⭐⭐ |
| `gsyVideoPlayer-ex_so/build.gradle` | Maven 配置 | ⭐⭐ |

### SO 文件

| 文件 | 修改内容 | 重要性 |
|------|---------|--------|
| `gsyVideoPlayer-armv64/src/main/jniLibs/arm64-v8a/*.so` | 16K 补丁 | ⭐⭐⭐⭐⭐ |
| `gsyVideoPlayer-ex_so/src/main/jniLibs/**/*.so` | 16K 补丁 + 加密支持 | ⭐⭐⭐⭐⭐ |

### Java 源码

| 文件 | 修改内容 | 重要性 |
|------|---------|--------|
| `gsyVideoPlayer-java/src/main/java/com/shuyu/gsyvideoplayer/GSYVideoManager.java` | 换行符 | ⭐ |
| `gsyVideoPlayer-java/src/main/java/com/shuyu/gsyvideoplayer/placeholder/PlaceholderSurface.java` | 空行 | ⭐ |
| `gsyVideoPlayer-java/src/main/java/com/shuyu/gsyvideoplayer/utils/NetInfoModule.java` | 空行 | ⭐ |

**重要说明**：
- ✅ **GSYVideoPlayer 源码没有功能性修改**（只有格式化）
- ✅ **所有功能修改都通过继承实现**（在主项目的 palyerlibrary 模块中）
- ✅ 这是更好的做法，更新上游时不会有冲突

---

## ⚠️ 更新上游时的冲突处理

### 高风险冲突（必须手动处理）

1. **Maven 配置**（所有 build.gradle）
   - 冲突原因：你的 group ID 是 `io.github.706412584`，上游是 `io.github.carguo`
   - 处理方式：保留你的配置

2. **Namespace**（base 和 java 模块）
   - 冲突原因：你使用 `gsyvideoplayer`，上游使用 `gsy`
   - 处理方式：保留你的配置

3. **版本号**（gradle.properties）
   - 冲突原因：你固定在 12.0.0，上游会更新
   - 处理方式：根据需要决定是否更新

4. **SO 文件**（armv64 和 ex_so）
   - 冲突原因：你使用自己编译的 SO，上游使用官方 SO
   - 处理方式：保留你的 SO 文件

### 中风险冲突（可能需要合并）

1. **module-lite-more-fixed.sh**
   - 冲突原因：你添加了缺失的协议，上游可能更新 module-lite-more.sh
   - 处理方式：手动合并，保留你添加的协议

2. **16kpatch/**
   - 冲突原因：你添加的新目录，上游不会有
   - 处理方式：保留你的补丁

### 低风险冲突（自动合并）

1. **Java 源码格式化**
   - 冲突原因：换行符和空行
   - 处理方式：接受上游的格式化

2. **文档文件**（README.md 等）
   - 冲突原因：你可能有自定义说明
   - 处理方式：根据需要合并

---

## 🔄 合并上游更新的推荐步骤

```bash
# 1. 进入子模块
cd GSYVideoPlayer-source

# 2. 拉取上游更新
git fetch upstream

# 3. 查看更新内容
git log HEAD..upstream/master --oneline
git diff HEAD..upstream/master --stat

# 4. 创建备份分支
git checkout -b backup-before-merge

# 5. 切回主分支
git checkout master

# 6. 合并更新（会有冲突）
git merge upstream/master

# 7. 解决冲突
# 重点检查：
# - 所有 build.gradle 的 PROJ_GROUP 和 namespace
# - gradle.properties 的版本号
# - SO 文件是否被覆盖
# - module-lite-more-fixed.sh 是否被删除

# 8. 验证修改
git diff backup-before-merge

# 9. 提交合并
git add .
git commit -m "合并上游更新并保留自定义修改"

# 10. 推送到你的 fork
git push origin master

# 11. 回到主项目，更新引用
cd ..
git add GSYVideoPlayer-source
git commit -m "更新 GSYVideoPlayer-source"
git push origin main
```

---

## 🛠️ 冲突解决示例

### 示例 1：build.gradle 冲突

```gradle
<<<<<<< HEAD
ext {
    PROJ_GROUP = 'io.github.706412584'
    PROJ_ARTIFACTID = 'gsyVideoPlayer-base'
    PROJ_VERSION = rootProject.ext.pomVersion
}
=======
// 上游可能没有这个配置，或者使用不同的 group
>>>>>>> upstream/master
```

**解决方式**：保留你的配置
```gradle
ext {
    PROJ_GROUP = 'io.github.706412584'
    PROJ_ARTIFACTID = 'gsyVideoPlayer-base'
    PROJ_VERSION = rootProject.ext.pomVersion
}
```

### 示例 2：namespace 冲突

```gradle
<<<<<<< HEAD
android {
    namespace 'com.shuyu.gsyvideoplayer.base'
}
=======
android {
    namespace 'com.shuyu.gsy.base'
}
>>>>>>> upstream/master
```

**解决方式**：保留你的 namespace
```gradle
android {
    namespace 'com.shuyu.gsyvideoplayer.base'
}
```

### 示例 3：SO 文件冲突

```bash
# 如果 SO 文件被覆盖，从备份恢复
git checkout backup-before-merge -- gsyVideoPlayer-armv64/src/main/jniLibs/
git checkout backup-before-merge -- gsyVideoPlayer-ex_so/src/main/jniLibs/
```

---

## 📝 验证清单

合并完成后，验证以下内容：

- [ ] 所有 build.gradle 的 `PROJ_GROUP` 是 `io.github.706412584`
- [ ] base 和 java 模块的 `namespace` 是 `com.shuyu.gsyvideoplayer.*`
- [ ] `gradle.properties` 的版本号符合预期
- [ ] `16kpatch/` 目录存在
- [ ] `module-lite-more-fixed.sh` 文件存在
- [ ] SO 文件没有被覆盖（检查文件大小和修改时间）
- [ ] 编译通过：`./gradlew :gsyVideoPlayer-base:build`

---

## 💡 总结

**GSYVideoPlayer-source 子模块的修改主要是**：

1. ⭐⭐⭐⭐⭐ **SO 文件** - 16K 补丁 + 加密支持（最重要）
2. ⭐⭐⭐⭐ **Maven 配置** - 发布到你自己的仓库
3. ⭐⭐⭐⭐ **Namespace** - 统一包名
4. ⭐⭐⭐⭐ **FFmpeg 配置** - module-lite-more-fixed.sh
5. ⭐⭐⭐ **版本号** - 固定在 12.0.0

**更新上游时最需要注意的是 SO 文件和 Maven 配置，这些是你的核心修改。**

**重要发现**：
- ✅ **GSYVideoPlayer 源码没有被直接修改**（只有格式化）
- ✅ **所有功能修改都通过继承实现**（在主项目中）
- ✅ 这意味着更新上游时**不会有 Java 源码冲突**
- ✅ 只需要注意 SO 文件、Maven 配置、namespace 等配置文件的冲突

---

## 📚 相关文档

- [GSYVideoPlayer 自定义修改说明](GSYVIDEOPLAYER_CUSTOM_MODIFICATIONS.md) - 主项目的修改
- [Git 子模块管理](GIT_SUBMODULE_MANAGEMENT.md) - 子模块管理指南
- [IJK 编译指南](IJK_BUILD_WITH_16K_PATCH.md) - 如何重新编译 SO 文件
