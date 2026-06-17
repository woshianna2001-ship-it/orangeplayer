# Maven Central 发布总结

## 问题与解决方案

### 问题 1：Javadoc 生成卡住

**现象**：
- 发布时卡在 78% 不动
- 下载 Dokka 依赖（88.7MB）非常慢

**根本原因**：
- vanniktech 插件默认使用 Dokka 生成 Javadoc
- Dokka 依赖下载慢且会阻塞发布流程

**解决方案**：
```gradle
// 禁用 Dokka javadoc 生成，创建空的 javadoc jar
afterEvaluate {
    // 禁用 Dokka 相关任务
    tasks.findByName('javaDocReleaseGeneration')?.enabled = false
    
    // 重新配置 javaDocReleaseJar 任务，使其生成空的 jar 到正确位置
    def javadocTask = tasks.findByName('javaDocReleaseJar')
    if (javadocTask != null) {
        javadocTask.enabled = true
        javadocTask.archiveBaseName.set('release')
        javadocTask.archiveClassifier.set('javadoc')
        javadocTask.destinationDirectory.set(file("${buildDir}/intermediates/java_doc_jar/release"))
        
        javadocTask.doFirst {
            destinationDirectory.get().asFile.mkdirs()
        }
    }
}
```

### 问题 2：组件验证失败

**现象**：
- Maven Central 显示 "1 个组件验证失败"
- 错误信息：`pkg:maven/io.github.706412584/orangeplayer@1.0.6?type=aar`

**根本原因**：
- POM 文件中包含 Maven Central 上不存在的依赖：
  - `com.github.bilibili:DanmakuFlameMaster` (JitPack 依赖)
  - `com.aliyun.sdk.android:AliyunPlayer` (阿里云私有仓库)
  - `io.github.706412584:gsyVideoPlayer-aliplay` (依赖上述两个库)

**解决方案**：
将这些依赖改为 `compileOnly`，不传递给使用者：

```gradle
dependencies {
    // 核心依赖（会传递）
    api 'io.github.706412584:gsyVideoPlayer-java:1.1.0'
    implementation 'io.github.706412584:gsyVideoPlayer-exo_player2:1.1.0'
    
    // 可选依赖（不传递，让使用者自己添加）
    compileOnly ('io.github.706412584:gsyVideoPlayer-aliplay:1.1.0') {
        exclude group: 'com.aliyun.sdk.android', module: 'AliyunPlayer'
        exclude group: 'com.alivc.conan', module: 'AlivcConan'
    }
    compileOnly 'com.aliyun.sdk.android:AliyunPlayer:5.4.7.1-full'
    compileOnly 'com.github.bilibili:DanmakuFlameMaster:0.9.25'
}
```

## 版本历史

### v1.0.6 ❌
- 首次尝试发布
- 问题：包含 JitPack 和阿里云依赖，验证失败

### v1.0.7 ❌
- 移除了 `DanmakuFlameMaster` 和 `AliyunPlayer`
- 问题：仍包含 `gsyvideoplayer-aliplay`，验证失败

### v1.0.8 ✅
- 移除所有 Maven Central 上不存在的依赖
- 所有依赖验证通过
- 成功发布

## 最终配置

### build.gradle (palyerlibrary)

```gradle
plugins {
    id 'com.android.library'
    id 'com.vanniktech.maven.publish' version '0.34.0'
    id 'signing'
}

// Maven Central 发布配置
mavenPublishing {
    coordinates("io.github.706412584", "orangeplayer", "1.0.8")

    pom {
        name = 'OrangePlayer'
        description = '基于 GSYVideoPlayer 的增强 Android 视频播放器库，支持多播放内核、字幕、弹幕、OCR 识别、语音识别等功能'
        url = 'https://github.com/706412584/orangeplayer'
        
        licenses {
            license {
                name = 'The Apache License, Version 2.0'
                url = 'http://www.apache.org/licenses/LICENSE-2.0.txt'
            }
        }

        developers {
            developer {
                id = '706412584'
                name = 'xcwl'
                email = '706412584@qq.com'
            }
        }

        scm {
            connection = 'scm:git:github.com/706412584/orangeplayer.git'
            developerConnection = 'scm:git:ssh://github.com/706412584/orangeplayer.git'
            url = 'https://github.com/706412584/orangeplayer'
        }
    }

    publishToMavenCentral()
    signAllPublications()
}

// 禁用 Dokka javadoc 生成
afterEvaluate {
    tasks.findByName('javaDocReleaseGeneration')?.enabled = false
    
    def javadocTask = tasks.findByName('javaDocReleaseJar')
    if (javadocTask != null) {
        javadocTask.enabled = true
        javadocTask.archiveBaseName.set('release')
        javadocTask.archiveClassifier.set('javadoc')
        javadocTask.destinationDirectory.set(file("${buildDir}/intermediates/java_doc_jar/release"))
        
        javadocTask.doFirst {
            destinationDirectory.get().asFile.mkdirs()
        }
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.7.1'
    
    // GSYVideoPlayer 核心依赖
    api 'io.github.706412584:gsyVideoPlayer-java:1.1.0'
    implementation 'io.github.706412584:gsyVideoPlayer-exo_player2:1.1.0'
    
    // 可选依赖（compileOnly）
    compileOnly ('io.github.706412584:gsyVideoPlayer-aliplay:1.1.0') {
        exclude group: 'com.aliyun.sdk.android', module: 'AliyunPlayer'
        exclude group: 'com.alivc.conan', module: 'AlivcConan'
    }
    compileOnly 'com.aliyun.sdk.android:AliyunPlayer:5.4.7.1-full'
    compileOnly 'com.github.bilibili:DanmakuFlameMaster:0.9.25'
    
    // 其他依赖
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    implementation 'cz.adaptech.tesseract4android:tesseract4android:4.7.0'
    implementation 'com.google.mlkit:translate:17.0.2'
    implementation 'com.alphacephei:vosk-android:0.3.47'
}
```

### gradle.properties

```properties
# Maven Central 发布配置
mavenCentralUsername=你的用户名
mavenCentralPassword=你的密码

# GPG 签名配置
signing.keyId=C60913C4
signing.password=你的GPG密码
signing.secretKeyRingFile=C:/Users/70641/secring.gpg
```

## 发布命令

```bash
# 清理并发布到 Maven Central
.\gradlew clean :palyerlibrary:publishToMavenCentral

# 或者先发布到本地测试
.\gradlew :palyerlibrary:publishToMavenLocal
```

## 使用方式

### 基础使用（Maven Central）

```gradle
dependencies {
    implementation 'io.github.706412584:orangeplayer:1.0.8'
}
```

### 完整功能（需要额外依赖）

```gradle
repositories {
    google()
    mavenCentral()
    maven { url 'https://jitpack.io' }  // 弹幕库
    maven { url 'https://maven.aliyun.com/repository/releases' }  // 阿里云播放器
}

dependencies {
    implementation 'io.github.706412584:orangeplayer:1.0.8'
    
    // 可选：弹幕功能
    implementation 'com.github.bilibili:DanmakuFlameMaster:0.9.25'
    
    // 可选：阿里云播放器
    implementation 'com.aliyun.sdk.android:AliyunPlayer:5.4.7.1-full'
}
```

详见 [可选依赖说明](OPTIONAL_DEPENDENCIES.md)

## 经验总结

1. **Maven Central 要求所有依赖都在 Maven Central 上可用**
   - 不能依赖 JitPack 或私有仓库的包
   - 使用 `compileOnly` 可以避免传递依赖

2. **Javadoc 是必需的，但可以是空的**
   - vanniktech 插件会检查 javadoc.jar 是否存在
   - 可以禁用 Dokka，生成空的 javadoc.jar

3. **GPG 签名是必需的**
   - 所有文件都需要 .asc 签名文件
   - 公钥必须上传到公共密钥服务器

4. **发布流程**
   - 本地测试：`publishToMavenLocal`
   - 发布到 staging：`publishToMavenCentral`
   - 在 Maven Central 网站上验证并发布

5. **版本号管理**
   - 每次发布都需要新的版本号
   - 已发布的版本不能修改或删除
