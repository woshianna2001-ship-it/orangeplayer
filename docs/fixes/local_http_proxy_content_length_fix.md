# 本地 HTTP 代理服务器 Content-Length 问题修复

## 问题描述

使用本地 HTTP 代理服务器（如 AndroidVideoCache）播放视频时失败，日志显示：

```
curl: (8) Invalid Content-Length: value
E IJKMEDIA: Failed to open file 'http://127.0.0.1:6474/storage/...' or configure filtergraph
E OrangevideoView: onPlayError: url=http://127.0.0.1:6474/storage/...
```

## 根本原因

本地 HTTP 代理服务器（通常是 AndroidVideoCache 或类似库）返回的 HTTP 响应头中 **Content-Length 值无效或缺失**，导致：

1. IJK 播放器无法正确解析 HTTP 响应
2. ExoPlayer 也会遇到类似问题
3. 系统播放器可能也无法正常工作

## 诊断步骤

### 1. 检查本地代理服务器

```bash
# 查看监听端口
adb shell netstat -an | findstr "6474"

# 测试 HTTP 响应
adb shell "curl -I http://127.0.0.1:6474/path/to/file.m3u8"
```

### 2. 查看错误信息

如果看到以下错误，说明是 Content-Length 问题：
```
curl: (8) Invalid Content-Length: value
HTTP/1.1 200 OK
Content-Type: application/x-mpegURL
Connection: keep-alive
```

### 3. 检查文件是否存在

```bash
adb shell "ls -lh /storage/emulated/0/Android/data/com.jsg/m3u8_cache/..."
```

## 解决方案

### 方案一：修复视频缓存库（推荐）

如果应用使用的是 AndroidVideoCache，需要升级或修复该库。

#### AndroidVideoCache 已知问题

旧版本的 AndroidVideoCache 在某些情况下会返回错误的 Content-Length：

```java
// 问题代码示例
response.setHeader("Content-Length", String.valueOf(fileLength));
// 但 fileLength 可能是 -1 或其他无效值
```

#### 修复方法

1. **升级到最新版本**：

```gradle
dependencies {
    // 使用修复版本
    implementation 'com.danikula:videocache:2.7.1'
}
```

2. **或使用 GSYVideoPlayer 内置的缓存**：

```gradle
dependencies {
    implementation 'io.github.706412584:gsyVideoPlayer-proxy_cache:1.1.0'
}
```

3. **配置缓存代理**：

```java
// 在 Application 中初始化
HttpProxyCacheServer proxy = new HttpProxyCacheServer.Builder(context)
    .maxCacheSize(1024 * 1024 * 1024)  // 1GB
    .build();

// 使用代理 URL
String proxyUrl = proxy.getProxyUrl(originalUrl);
videoView.setUp(proxyUrl, true, title);
```

### 方案二：直接播放原始 URL

如果本地代理有问题，可以直接播放原始 URL：

```java
// 不使用本地代理
String originalUrl = "https://vodcnd01.3rut0.com/.../stream.m3u8";
videoView.setUp(originalUrl, true, title);
```

### 方案三：使用文件路径（如果是本地文件）

如果文件已经完全下载到本地，直接使用文件路径：

```java
String localPath = "/storage/emulated/0/Android/data/com.jsg/m3u8_cache/.../stream.m3u8";
videoView.setUp(localPath, true, title);
```

### 方案四：修复 HTTP 响应头

如果你有权限修改代理服务器代码，确保正确设置 Content-Length：

```java
public class FixedHttpProxyCache extends HttpProxyCache {
    @Override
    protected void sendHeaders(Response response, Socket socket) throws IOException {
        // 确保 Content-Length 有效
        long contentLength = getContentLength();
        if (contentLength > 0) {
            response.setHeader("Content-Length", String.valueOf(contentLength));
        } else {
            // 如果无法确定长度，使用 chunked 传输
            response.setHeader("Transfer-Encoding", "chunked");
            response.removeHeader("Content-Length");
        }
        super.sendHeaders(response, socket);
    }
}
```

## 验证修复

### 1. 测试 HTTP 响应

```bash
adb shell "curl -I http://127.0.0.1:6474/path/to/file.m3u8"
```

应该看到有效的 Content-Length：
```
HTTP/1.1 200 OK
Content-Type: application/x-mpegURL
Content-Length: 123456
Connection: keep-alive
```

### 2. 测试播放

```bash
# 清除日志
adb logcat -c

# 播放视频后查看日志
adb logcat -d | findstr "OrangevideoView IJKMEDIA"
```

应该不再看到 "Failed to open file" 错误。

## 常见问题

### Q: 为什么 IJK 和 ExoPlayer 都失败？

A: 因为问题在于 HTTP 代理服务器，不是播放器本身。所有播放器都依赖正确的 HTTP 响应头。

### Q: 为什么有时候能播放，有时候不能？

A: 可能是：
1. 文件还在下载中，长度未知
2. 代理服务器状态不稳定
3. 网络连接问题

### Q: 如何禁用视频缓存？

A: 如果缓存导致问题，可以禁用：

```java
// GSYVideoPlayer 禁用缓存
GSYVideoManager.instance().setCache(null);

// 或者不使用代理 URL
videoView.setUp(originalUrl, true, title);  // 直接使用原始 URL
```

## 相关日志

### 正常的 HTTP 响应

```
D IJKMEDIA: Opening 'http://127.0.0.1:6474/...' for reading
D IJKMEDIA: HTTP/1.1 200 OK
D IJKMEDIA: Content-Length: 123456
D IJKMEDIA: Content-Type: application/x-mpegURL
```

### 异常的 HTTP 响应

```
D IJKMEDIA: Opening 'http://127.0.0.1:6474/...' for reading
D IJKMEDIA: HTTP/1.1 200 OK
D IJKMEDIA: Content-Length: -1  ← 无效值
F IJKMEDIA: Failed to open file or configure filtergraph
```

## 技术细节

### AndroidVideoCache 工作原理

1. 应用请求播放 URL
2. AndroidVideoCache 启动本地 HTTP 服务器（如 127.0.0.1:6474）
3. 返回代理 URL 给播放器
4. 播放器请求代理 URL
5. 代理服务器从原始 URL 下载并缓存
6. 同时将数据流式传输给播放器

### Content-Length 的重要性

- 播放器需要知道文件大小来：
  - 显示进度条
  - 支持 seek 操作
  - 预分配缓冲区
  - 判断是否支持断点续传

### 为什么会出现无效的 Content-Length

1. **文件还在下载**：长度未知
2. **动态内容**：m3u8 文件可能动态生成
3. **库的 bug**：旧版本的缓存库有此问题
4. **网络错误**：原始服务器未返回 Content-Length

## 推荐方案

对于 com.jsg 应用，建议：

1. **短期方案**：直接播放原始 URL，不使用本地代理
2. **中期方案**：升级 AndroidVideoCache 到最新版本
3. **长期方案**：使用 GSYVideoPlayer 内置的缓存机制，更稳定可靠

## 相关链接

- [AndroidVideoCache GitHub](https://github.com/danikula/AndroidVideoCache)
- [GSYVideoPlayer 缓存文档](https://github.com/CarGuo/GSYVideoPlayer/blob/master/doc/CACHE.md)
- [HTTP Content-Length 规范](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Length)

---

**最后更新**: 2026-01-31  
**问题类型**: HTTP 代理服务器配置问题  
**影响范围**: 所有播放器引擎（IJK、ExoPlayer、系统播放器）
