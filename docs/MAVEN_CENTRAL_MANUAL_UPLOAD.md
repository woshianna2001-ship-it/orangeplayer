# Maven Central 手动上传指南

由于新版 Sonatype Central Portal 的 API 还在变化，自动发布可能遇到问题。本指南介绍如何手动上传到 Maven Central。

---

## 方案一：使用 JitPack（推荐）

JitPack 是最简单的方式，无需复杂配置：

### 1. 推送代码到 GitHub

```bash
git add .
git commit -m "Release v1.0.6"
git push origin main
```

### 2. 创建 Release Tag

```bash
git tag v1.0.6
git push origin v1.0.6
```

### 3. 访问 JitPack

- 打开 https://jitpack.io/#706412584/orangeplayer
- 点击 "Get it" 触发构建
- 等待构建完成

### 4. 使用

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.706412584:orangeplayer:v1.0.6'
}
```

---

## 方案二：手动上传到 Maven Central

### 步骤 1: 生成发布文件

```bash
# 构建 AAR
.\gradlew :palyerlibrary:assembleRelease

# 生成源码 JAR
.\gradlew :palyerlibrary:androidSourcesJar

# 生成文档 JAR
.\gradlew :palyerlibrary:androidJavadocsJar
```

生成的文件位于：
- `palyerlibrary/build/outputs/aar/palyerlibrary-release.aar`
- `palyerlibrary/build/libs/palyerlibrary-1.0.6-sources.jar`
- `palyerlibrary/build/libs/palyerlibrary-1.0.6-javadoc.jar`

### 步骤 2: 创建 POM 文件

创建 `palyerlibrary-1.0.6.pom`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>io.github.706412584</groupId>
    <artifactId>orangeplayer</artifactId>
    <version>1.0.6</version>
    <packaging>aar</packaging>
    
    <name>OrangePlayer</name>
    <description>基于 GSYVideoPlayer 的增强 Android 视频播放器库，支持多播放内核、字幕、弹幕、OCR 识别、语音识别等功能</description>
    <url>https://github.com/706412584/orangeplayer</url>
    
    <licenses>
        <license>
            <name>The Apache License, Version 2.0</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        </license>
    </licenses>
    
    <developers>
        <developer>
            <id>706412584</id>
            <name>xcwl</name>
            <email>706412584@qq.com</email>
        </developer>
    </developers>
    
    <scm>
        <connection>scm:git:git://github.com/706412584/orangeplayer.git</connection>
        <developerConnection>scm:git:ssh://github.com/706412584/orangeplayer.git</developerConnection>
        <url>https://github.com/706412584/orangeplayer</url>
    </scm>
    
    <dependencies>
        <dependency>
            <groupId>androidx.appcompat</groupId>
            <artifactId>appcompat</artifactId>
            <version>1.7.1</version>
        </dependency>
        <dependency>
            <groupId>io.github.carguo</groupId>
            <artifactId>gsyvideoplayer</artifactId>
            <version>11.3.0</version>
        </dependency>
        <!-- 其他依赖... -->
    </dependencies>
</project>
```

### 步骤 3: 签名所有文件

```bash
# 签名 AAR
gpg -ab palyerlibrary-release.aar

# 签名源码
gpg -ab palyerlibrary-1.0.6-sources.jar

# 签名文档
gpg -ab palyerlibrary-1.0.6-javadoc.jar

# 签名 POM
gpg -ab palyerlibrary-1.0.6.pom
```

### 步骤 4: 创建 Bundle

将所有文件打包成一个 ZIP：

```
orangeplayer-1.0.6-bundle.zip
├── orangeplayer-1.0.6.aar
├── orangeplayer-1.0.6.aar.asc
├── orangeplayer-1.0.6-sources.jar
├── orangeplayer-1.0.6-sources.jar.asc
├── orangeplayer-1.0.6-javadoc.jar
├── orangeplayer-1.0.6-javadoc.jar.asc
├── orangeplayer-1.0.6.pom
└── orangeplayer-1.0.6.pom.asc
```

### 步骤 5: 上传到 Central Portal

1. 登录 https://central.sonatype.com/
2. 点击 **Publish** > **Upload**
3. 选择你的 Bundle ZIP 文件
4. 点击 **Publish**
5. 等待验证和发布（通常 10-30 分钟）

---

## 方案三：使用 Gradle 生成 Bundle

我为你创建了一个自动化脚本：

```bash
.\gradlew :palyerlibrary:createMavenCentralBundle
```

这会自动：
1. 构建 AAR
2. 生成源码和文档 JAR
3. 创建 POM 文件
4. 签名所有文件
5. 打包成 Bundle ZIP

生成的文件位于：`palyerlibrary/build/maven-central-bundle/`

---

## 验证发布

发布后等待 10-30 分钟，然后：

1. 访问 https://central.sonatype.com/artifact/io.github.706412584/orangeplayer
2. 检查版本是否显示
3. 在项目中测试：

```gradle
dependencies {
    implementation 'io.github.706412584:orangeplayer:1.0.6'
}
```

---

## 常见问题

### Q1: 签名失败

确保 GPG 密钥已正确配置：
```bash
gpg --list-keys
```

### Q2: POM 验证失败

确保 POM 包含所有必需字段：
- name
- description
- url
- licenses
- developers
- scm

### Q3: 上传失败

检查：
- 文件命名是否正确
- 所有文件都已签名
- ZIP 文件结构正确

---

## 推荐方案

对于大多数开源项目，我推荐：

1. **主要使用 JitPack**（方案一）
   - 配置简单
   - 自动构建
   - 无需手动操作

2. **同时发布到 Maven Central**（方案二/三）
   - 更官方
   - 更好的可见性
   - 但配置复杂

你可以先用 JitPack 快速发布，然后慢慢研究 Maven Central 的新 API。
