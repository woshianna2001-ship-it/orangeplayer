# 发布指南

本项目同时支持发布到 **JitPack** 和 **Maven Central**。

**✅ 最新发布状态**：
- 版本：1.0.8
- Maven Central：已配置并测试成功
- Group ID：io.github.706412584
- Artifact ID：orangeplayer
- 说明：弹幕和阿里云播放器功能需要额外依赖，详见 [可选依赖说明](OPTIONAL_DEPENDENCIES.md)

---

## 发布到 JitPack

JitPack 是最简单的发布方式，无需额外配置。

### 步骤

1. **推送代码到 GitHub**
   ```bash
   git add .
   git commit -m "Release v1.0.6"
   git push origin main
   ```

2. **创建 Release Tag**
   ```bash
   git tag v1.0.6
   git push origin v1.0.6
   ```

3. **访问 JitPack**
   - 打开 https://jitpack.io/#706412584/orangeplayer
   - JitPack 会自动构建你的库
   - 等待构建完成（绿色勾号）

4. **使用**
   ```gradle
   repositories {
       maven { url 'https://jitpack.io' }
   }
   
   dependencies {
       implementation 'com.github.706412584:orangeplayer:v1.0.6'
   }
   ```

---

## 发布到 Maven Central

Maven Central 是官方推荐的 Android 库发布平台，需要更多配置。

### 前置准备

#### 1. 注册 Sonatype 账号

访问 https://central.sonatype.com/ 注册账号。

#### 2. 验证命名空间

- 如果使用 `io.github.yourusername`，需要验证 GitHub 账号
- 在 Sonatype Central Portal 中添加命名空间
- 按照提示在 GitHub 创建验证仓库或添加 TXT 记录

#### 3. 生成 GPG 密钥

Maven Central 要求所有发布的包必须签名。

**Windows:**
```bash
# 安装 GPG (使用 Gpg4win)
# 下载: https://www.gpg4win.org/

# 生成密钥
gpg --gen-key

# 查看密钥 ID（取最后 8 位）
gpg --list-keys

# 导出密钥到文件
gpg --export-secret-keys -o secring.gpg

# 上传公钥到服务器
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

**Linux/Mac:**
```bash
# 生成密钥
gpg --gen-key

# 查看密钥 ID
gpg --list-keys

# 导出密钥
gpg --export-secret-keys -o ~/.gnupg/secring.gpg

# 上传公钥
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

### 配置项目

#### 1. 创建 gradle.properties

复制 `gradle.properties.example` 为 `gradle.properties`：

```properties
# Sonatype 凭证
ossrhUsername=your-username
ossrhPassword=your-password

# GPG 签名
signing.keyId=12345678  # GPG 密钥 ID 的最后 8 位
signing.password=your-gpg-password
signing.secretKeyRingFile=C:/Users/YourName/.gnupg/secring.gpg
```

> ⚠️ **重要**: `gradle.properties` 包含敏感信息，已在 `.gitignore` 中，不要提交到 Git！

#### 2. 更新版本号

在 `scripts/publish-root.gradle` 中修改版本号：

```gradle
ext {
    PUBLISH_GROUP_ID = 'io.github.706412584'
    PUBLISH_VERSION = '1.0.6'  // 修改这里
    PUBLISH_ARTIFACT_ID = 'orangeplayer'
}
```

### 发布步骤

#### 1. 构建并发布到 Staging

```bash
# Windows
gradlew clean build
gradlew publishToSonatype

# Linux/Mac
./gradlew clean build
./gradlew publishToSonatype
```

#### 2. 关闭并发布 Staging Repository

```bash
# 关闭 staging repository（会自动验证）
gradlew closeSonatypeStagingRepository

# 发布到 Maven Central（验证通过后）
gradlew releaseSonatypeStagingRepository
```

或者一步完成：

```bash
gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository
```

#### 3. 等待同步

- 发布后需要等待 10-30 分钟同步到 Maven Central
- 访问 https://central.sonatype.com/artifact/io.github.706412584/orangeplayer 查看状态
- 同步到 Maven Central 后可在 https://repo1.maven.org/maven2/io/github/706412584/orangeplayer/ 查看

### 使用发布的库

```gradle
repositories {
    mavenCentral()
}

dependencies {
    implementation 'io.github.706412584:orangeplayer:1.0.6'
}
```

---

## CI/CD 自动发布

### GitHub Actions

创建 `.github/workflows/publish.yml`：

```yaml
name: Publish to Maven Central

on:
  release:
    types: [created]

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 11
        uses: actions/setup-java@v3
        with:
          java-version: '11'
          distribution: 'temurin'
      
      - name: Decode GPG key
        run: |
          echo "${{ secrets.GPG_SECRET_KEY }}" | base64 --decode > secring.gpg
      
      - name: Publish to Maven Central
        env:
          OSSRH_USERNAME: ${{ secrets.OSSRH_USERNAME }}
          OSSRH_PASSWORD: ${{ secrets.OSSRH_PASSWORD }}
          SIGNING_KEY_ID: ${{ secrets.SIGNING_KEY_ID }}
          SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
          SIGNING_SECRET_KEY_RING_FILE: secring.gpg
        run: |
          ./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository
```

在 GitHub 仓库的 Settings > Secrets 中添加：
- `OSSRH_USERNAME`
- `OSSRH_PASSWORD`
- `SIGNING_KEY_ID`
- `SIGNING_PASSWORD`
- `GPG_SECRET_KEY` (base64 编码的 secring.gpg)

---

## 常见问题

### Q1: 签名失败

**错误**: `gpg: signing failed: No secret key`

**解决**:
- 检查 `signing.keyId` 是否正确（GPG 密钥 ID 的最后 8 位）
- 检查 `signing.secretKeyRingFile` 路径是否正确
- 确保 GPG 密钥已导出到文件

### Q2: 命名空间验证失败

**错误**: `Namespace not verified`

**解决**:
- 在 Sonatype Central Portal 中完成命名空间验证
- 对于 `io.github.yourusername`，需要在 GitHub 创建验证仓库

### Q3: POM 验证失败

**错误**: `POM validation failed`

**解决**:
- 确保 POM 包含必需字段：name, description, url, licenses, developers, scm
- 检查 `scripts/publish-module.gradle` 中的 POM 配置

### Q4: 上传超时

**解决**:
- 检查网络连接
- 使用代理：在 `gradle.properties` 中添加
  ```properties
  systemProp.http.proxyHost=127.0.0.1
  systemProp.http.proxyPort=7890
  systemProp.https.proxyHost=127.0.0.1
  systemProp.https.proxyPort=7890
  ```

### Q5: 版本已存在

**错误**: `Version already exists`

**解决**:
- Maven Central 不允许覆盖已发布的版本
- 修改版本号后重新发布

---

## 版本管理建议

- **开发版本**: `1.0.0-SNAPSHOT`
- **正式版本**: `1.0.0`
- **修复版本**: `1.0.1`
- **功能版本**: `1.1.0`
- **重大版本**: `2.0.0`

---

## 参考资料

- [Maven Central 发布指南](https://central.sonatype.org/publish/publish-guide/)
- [Gradle Nexus Publish Plugin](https://github.com/gradle-nexus/publish-plugin)
- [GPG 签名指南](https://central.sonatype.org/publish/requirements/gpg/)
