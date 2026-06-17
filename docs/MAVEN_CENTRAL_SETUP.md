# Maven Central 发布配置说明

本文档说明项目中与 Maven Central 发布相关的所有配置文件。

---

## 配置文件概览

```
orangeplayer/
├── build.gradle                              # 根项目配置，引入发布插件
├── palyerlibrary/build.gradle                # 库模块配置，应用发布脚本
├── scripts/
│   ├── publish-root.gradle                   # Maven Central 根配置
│   └── publish-module.gradle                 # 模块发布配置（POM、签名等）
├── gradle.properties.example                 # 凭证配置示例
├── gradle.properties                         # 实际凭证（不提交到 Git）
├── publish.bat                               # Windows 发布脚本
├── publish.sh                                # Linux/Mac 发布脚本
├── .github/workflows/
│   └── publish-maven-central.yml             # GitHub Actions 自动发布
└── docs/
    ├── PUBLISH_GUIDE.md                      # 完整发布指南
    ├── GPG_SETUP_GUIDE.md                    # GPG 密钥设置
    ├── GITHUB_SECRETS_SETUP.md               # GitHub Secrets 配置
    └── MAVEN_CENTRAL_SETUP.md                # 本文档
```

---

## 1. build.gradle (根项目)

**位置**: `build.gradle`

**作用**: 
- 添加 Nexus Publish Plugin
- 引入根发布配置

**关键内容**:
```gradle
plugins {
    id 'io.github.gradle-nexus.publish-plugin' version '1.3.0'
}

apply from: "${rootDir}/scripts/publish-root.gradle"
```

---

## 2. scripts/publish-root.gradle

**位置**: `scripts/publish-root.gradle`

**作用**:
- 配置 Maven Central 仓库地址
- 设置项目坐标（groupId, artifactId, version）
- 配置 Sonatype 凭证

**关键配置**:
```gradle
ext {
    PUBLISH_GROUP_ID = 'io.github.706412584'
    PUBLISH_VERSION = '1.0.6'
    PUBLISH_ARTIFACT_ID = 'orangeplayer'
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://central.sonatype.com/api/v1/publisher/"))
            username = project.findProperty("ossrhUsername")
            password = project.findProperty("ossrhPassword")
        }
    }
}
```

**修改版本号**: 在这里修改 `PUBLISH_VERSION`

---

## 3. scripts/publish-module.gradle

**位置**: `scripts/publish-module.gradle`

**作用**:
- 配置 Maven 发布
- 生成 POM 文件
- 配置 GPG 签名
- 打包源码和文档

**关键功能**:
- 自动生成 `-sources.jar`
- 自动生成 `-javadoc.jar`
- 配置完整的 POM 信息（name, description, licenses, developers, scm）
- GPG 签名所有发布的文件

---

## 4. palyerlibrary/build.gradle

**位置**: `palyerlibrary/build.gradle`

**作用**: 应用发布配置到库模块

**关键内容**:
```gradle
plugins {
    id 'com.android.library'
}

apply from: "${rootDir}/scripts/publish-module.gradle"
```

---

## 5. gradle.properties

**位置**: `gradle.properties` (不提交到 Git)

**作用**: 存储敏感凭证信息

**内容**:
```properties
# Sonatype 凭证
ossrhUsername=your-username
ossrhPassword=your-password

# GPG 签名
signing.keyId=12345678
signing.password=your-gpg-password
signing.secretKeyRingFile=C:/path/to/secring.gpg
```

**重要**: 
- 此文件包含敏感信息，已在 `.gitignore` 中排除
- 使用 `gradle.properties.example` 作为模板

---

## 6. 发布脚本

### publish.bat (Windows)

**作用**: Windows 系统的发布脚本

**用法**:
```bash
publish.bat build      # 仅构建
publish.bat staging    # 发布到 Staging
publish.bat release    # 关闭并发布
publish.bat all        # 完整流程（默认）
```

### publish.sh (Linux/Mac)

**作用**: Linux/Mac 系统的发布脚本

**用法**:
```bash
chmod +x publish.sh
./publish.sh build
./publish.sh staging
./publish.sh release
./publish.sh all
```

---

## 7. GitHub Actions 工作流

**位置**: `.github/workflows/publish-maven-central.yml`

**作用**: 自动化发布到 Maven Central

**触发条件**:
1. 创建 GitHub Release 时自动触发
2. 手动触发（workflow_dispatch）

**所需 Secrets**:
- `OSSRH_USERNAME`
- `OSSRH_PASSWORD`
- `SIGNING_KEY_ID`
- `SIGNING_PASSWORD`
- `GPG_SECRET_KEY`

**配置方法**: 参考 `docs/GITHUB_SECRETS_SETUP.md`

---

## 发布流程

### 本地发布

1. **配置凭证**
   ```bash
   # 复制示例文件
   cp gradle.properties.example gradle.properties
   
   # 编辑并填入真实值
   notepad gradle.properties  # Windows
   # vim gradle.properties    # Linux/Mac
   ```

2. **更新版本号**
   - 编辑 `scripts/publish-root.gradle`
   - 修改 `PUBLISH_VERSION`

3. **执行发布**
   ```bash
   # Windows
   publish.bat all
   
   # Linux/Mac
   ./publish.sh all
   ```

### GitHub Actions 自动发布

1. **配置 Secrets**
   - 参考 `docs/GITHUB_SECRETS_SETUP.md`
   - 在 GitHub 仓库设置中添加所需 Secrets

2. **创建 Release**
   ```bash
   git tag v1.0.6
   git push origin v1.0.6
   ```
   
   或在 GitHub 网页上创建 Release

3. **等待构建**
   - 进入 Actions 标签页查看进度
   - 构建成功后自动发布到 Maven Central

---

## 验证发布

### 1. 检查 Sonatype Central

访问: https://central.sonatype.com/artifact/io.github.706412584/orangeplayer

### 2. 检查 Maven Central

等待 10-30 分钟后访问:
https://repo1.maven.org/maven2/io/github/706412584/orangeplayer/

### 3. 在项目中测试

```gradle
dependencies {
    implementation 'io.github.706412584:orangeplayer:1.0.6'
}
```

---

## 常见任务

### 更新版本号

1. 编辑 `scripts/publish-root.gradle`
2. 修改 `PUBLISH_VERSION = '1.0.7'`
3. 更新 `README.md` 中的版本号示例

### 修改 POM 信息

编辑 `scripts/publish-module.gradle` 中的 `pom` 块：

```gradle
pom {
    name = 'OrangePlayer'
    description = '新的描述'
    url = 'https://github.com/706412584/orangeplayer'
    // ...
}
```

### 添加新模块发布

如果有新的库模块需要发布：

1. 在模块的 `build.gradle` 中添加:
   ```gradle
   apply from: "${rootDir}/scripts/publish-module.gradle"
   ```

2. 在 `scripts/publish-root.gradle` 中添加模块配置

### 切换到 SNAPSHOT 版本

在 `scripts/publish-root.gradle` 中:
```gradle
PUBLISH_VERSION = '1.0.7-SNAPSHOT'
```

SNAPSHOT 版本会发布到 Snapshot 仓库，可以覆盖更新。

---

## 故障排查

### 构建失败

```bash
# 清理并重新构建
gradlew clean build --stacktrace
```

### 签名失败

1. 检查 GPG 密钥是否正确
2. 验证 `gradle.properties` 中的配置
3. 参考 `docs/GPG_SETUP_GUIDE.md`

### 上传失败

1. 检查网络连接
2. 验证 Sonatype 凭证
3. 检查命名空间是否已验证

### 查看详细日志

```bash
gradlew publishToSonatype --info
gradlew publishToSonatype --debug
```

---

## 最佳实践

### 1. 版本管理

- 使用语义化版本: `MAJOR.MINOR.PATCH`
- 开发版本使用 `-SNAPSHOT` 后缀
- 正式版本不要使用后缀

### 2. 发布前检查

- 运行所有测试
- 检查代码质量
- 更新文档
- 参考 `RELEASE_CHECKLIST.md`

### 3. 安全性

- 不要提交 `gradle.properties` 到 Git
- 定期轮换密钥和密码
- 使用 User Token 代替密码

### 4. 文档更新

每次发布后更新：
- `README.md` 中的版本号
- `CHANGELOG.md` 中的更新日志
- GitHub Release Notes

---

## 相关文档

- [完整发布指南](PUBLISH_GUIDE.md) - 详细的发布步骤
- [GPG 设置指南](GPG_SETUP_GUIDE.md) - GPG 密钥生成和配置
- [GitHub Secrets 配置](GITHUB_SECRETS_SETUP.md) - CI/CD 配置
- [发布检查清单](../RELEASE_CHECKLIST.md) - 发布前检查项

---

## 参考资料

- [Maven Central 发布指南](https://central.sonatype.org/publish/publish-guide/)
- [Gradle Nexus Publish Plugin](https://github.com/gradle-nexus/publish-plugin)
- [Maven POM 参考](https://maven.apache.org/pom.html)
- [语义化版本](https://semver.org/lang/zh-CN/)
