# OrangePlayer Torrent 测试指南

## 测试 .torrent 文件播放

### 步骤 1: 获取测试 .torrent 文件

在电脑浏览器中下载以下任一文件：

1. **Big Buck Bunny** (推荐，文件小)
   - 直接下载链接: https://webtorrent.io/torrents/big-buck-bunny.torrent
   - 视频大小: ~276 MB
   - 格式: MP4

2. **Sintel**
   - 直接下载链接: https://webtorrent.io/torrents/sintel.torrent
   - 视频大小: ~129 MB
   - 格式: MP4

### 步骤 2: 将 .torrent 文件传输到手机

```bash
# 方法 1: 使用 adb push
adb push big-buck-bunny.torrent /sdcard/Download/

# 方法 2: 通过微信/QQ 发送到手机
```

### 步骤 3: 在 OrangePlayer 中测试

1. 打开 OrangePlayer Demo 应用
2. 在 URL 输入框中输入本地文件路径：
   ```
   file:///sdcard/Download/big-buck-bunny.torrent
   ```
3. 点击播放按钮

### 预期结果

- ✅ 应用应该能够解析 .torrent 文件
- ✅ 开始下载种子内容
- ✅ 下载进度条显示
- ✅ 达到缓冲阈值后开始播放

### 与磁力链接的区别

| 特性 | .torrent 文件 | 磁力链接 |
|------|--------------|---------|
| 元数据获取 | 直接从文件读取 | 需要通过 DHT 网络获取 |
| 速度 | 立即开始下载 | 需要等待元数据获取（可能超时）|
| 网络要求 | 只需连接 peer | 需要 DHT 网络连接 |
| 成功率 | 高 | 取决于网络环境 |

### 故障排查

如果 .torrent 文件也无法播放：
1. 检查日志中是否有 libtorrent4j 初始化成功的消息
2. 检查是否有权限错误
3. 检查存储空间是否足够

如果 .torrent 文件可以播放，说明：
- ✅ OrangePlayer 的 torrent 功能代码正常
- ✅ libtorrent4j 库工作正常
- ❌ 问题确实是 DHT 网络连接问题（磁力链接特有）

## 结论

如果 .torrent 文件能播放而磁力链接不能，这证明了：
1. OrangePlayer 代码没有问题
2. 问题出在网络环境的 DHT 连接上
3. 这是正常现象，很多移动应用都有这个问题

## 建议

对于生产环境的 OrangePlayer：
1. 优先支持 .torrent 文件
2. 对磁力链接添加更长的超时时间（如 2-5 分钟）
3. 提供友好的错误提示："磁力链接获取元数据中，请耐心等待..."
4. 考虑添加服务器端 DHT 代理服务（如果有服务器资源）
