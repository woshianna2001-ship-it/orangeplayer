# 电视调试工具使用指南

快速诊断 OrangePlayer Legacy 在电视上的崩溃问题。

## 快速开始

### 1. 连接电视

```bash
# 运行连接脚本
scripts\connect_tv.bat

# 输入电视 IP 地址（例如：192.168.1.100）
```

### 2. 诊断崩溃

```bash
# 运行诊断脚本（自动收集所有日志）
scripts\diagnose_tv_crash.bat
```

### 3. 实时监控

```bash
# 实时查看日志
scripts\watch_tv_log.bat

# 然后在电视上启动应用
```

## 工具说明

### connect_tv.bat
**功能：** 通过 WiFi 连接到电视

**使用场景：**
- 首次连接电视
- 电视重启后重新连接
- 验证 ADB 连接状态

**输出：**
- 连接状态
- 设备列表
- 常用命令提示

### diagnose_tv_crash.bat
**功能：** 全面诊断崩溃问题

**自动执行：**
1. 检查设备连接
2. 检查应用安装状态
3. 查看最近的崩溃日志
4. 导出完整系统日志
5. 拉取应用崩溃日志文件

**输出文件：**
- `tv_crash_log_*.txt` - 完整系统日志
- `crash_logs/` - 应用崩溃日志目录

### watch_tv_log.bat
**功能：** 实时监控应用日志

**使用场景：**
- 复现崩溃问题
- 调试新功能
- 查看运行时错误

**显示内容：**
- 错误级别日志（*:E）
- 应用相关日志
- 播放器日志

## 前置条件

### 1. 安装 ADB 工具

**检查是否已安装：**
```bash
adb version
```

**如果未安装：**
- 下载 Android SDK Platform-Tools
- 添加到系统 PATH 环境变量

### 2. 启用电视 ADB 调试

**小米电视/盒子：**
1. 设置 → 关于
2. 连续点击"Android 版本" 7 次
3. 返回设置 → 开发者选项
4. 开启"USB 调试"和"网络调试"

**其他品牌：**
- 通常在"设置 → 系统 → 开发者选项"

### 3. 查看电视 IP 地址

**方法 1：** 设置 → 网络 → 网络状态

**方法 2：** 路由器管理页面查看

## 常见问题

### Q: 连接失败怎么办？

**A:** 检查以下几点：
1. 电视和电脑在同一 WiFi 网络
2. 电视已开启 ADB 调试
3. 防火墙未阻止 5555 端口
4. 尝试重启电视的 ADB 服务

### Q: 设备显示 unauthorized？

**A:** 
1. 在电视上会弹出授权对话框
2. 点击"允许"或"始终允许"
3. 如果没有弹出，运行：
   ```bash
   adb kill-server
   adb start-server
   ```

### Q: 日志太多看不清？

**A:** 使用过滤：
```bash
# 只看错误
adb logcat *:E

# 只看应用
adb logcat | findstr "com.orange.player.legacy"

# 只看崩溃
adb logcat | findstr "FATAL"
```

### Q: 如何查看应用是否安装？

**A:**
```bash
adb shell pm list packages | findstr orange
```

### Q: 如何重新安装应用？

**A:**
```bash
# 卸载旧版本
adb uninstall com.orange.player.legacy

# 安装新版本
adb install app-legacy\build\outputs\apk\debug\app-legacy-debug.apk
```

## 手动命令

如果脚本无法使用，可以手动执行：

### 连接电视
```bash
adb connect 192.168.1.100:5555
adb devices
```

### 查看实时日志
```bash
adb logcat -c
adb logcat *:E
```

### 导出日志
```bash
adb logcat -d > crash_log.txt
```

### 拉取崩溃日志
```bash
adb pull /sdcard/Android/data/com.orange.player.legacy/files/crash_logs/ ./
```

### 查看应用信息
```bash
# 检查安装
adb shell pm list packages | findstr orange

# 查看版本
adb shell dumpsys package com.orange.player.legacy | findstr version

# 查看 APK 路径
adb shell pm path com.orange.player.legacy
```

### 应用操作
```bash
# 启动应用
adb shell am start -n com.orange.player.legacy/.MainActivity

# 停止应用
adb shell am force-stop com.orange.player.legacy

# 清除数据
adb shell pm clear com.orange.player.legacy
```

## 日志分析

### 查找关键错误

**FATAL EXCEPTION：**
```bash
adb logcat -d | findstr "FATAL EXCEPTION"
```

**ClassNotFoundException：**
```bash
adb logcat -d | findstr "ClassNotFoundException"
```

**UnsatisfiedLinkError：**
```bash
adb logcat -d | findstr "UnsatisfiedLinkError"
```

**OutOfMemoryError：**
```bash
adb logcat -d | findstr "OutOfMemoryError"
```

### 查看应用日志

```bash
# 应用启动日志
adb logcat -d | findstr "OrangeApplication"

# 播放器日志
adb logcat -d | findstr "GSYVideoPlayer"

# 崩溃堆栈
adb logcat -d | findstr "at com.orange.player"
```

## 提交 Issue

如果需要提交 Issue，请附上：

1. **设备信息**
   ```bash
   adb shell getprop ro.product.model
   adb shell getprop ro.build.version.release
   adb shell getprop ro.product.cpu.abi
   ```

2. **完整日志**
   - `tv_crash_log_*.txt`
   - `crash_logs/crash_*.txt`

3. **复现步骤**
   - 详细的操作步骤
   - 崩溃发生的时机

4. **APK 信息**
   - 版本号
   - 构建类型（Debug/Release）

## 相关文档

- [电视崩溃诊断指南](../docs/TV_CRASH_DIAGNOSIS.md) - 详细诊断步骤
- [FAQ](../docs/FAQ.md) - 常见问题解答
- [Android 4.4 支持指南](../docs/ANDROID_4.4_SUPPORT.md) - 兼容性说明

---

**提示：** 所有脚本都在 `scripts/` 目录下，可以直接双击运行。
