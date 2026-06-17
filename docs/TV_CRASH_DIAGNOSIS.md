# 电视机崩溃诊断指南

当 OrangePlayer Legacy 在电视机上闪退时，使用本指南进行诊断。

## 快速诊断（推荐）

### 方法 1：使用诊断脚本

1. **连接电视**
   ```bash
   # 运行连接脚本
   scripts\connect_tv.bat
   
   # 输入电视 IP 地址（在电视网络设置中查看）
   # 例如：192.168.1.100
   ```

2. **运行诊断**
   ```bash
   scripts\diagnose_tv_crash.bat
   ```

3. **查看结果**
   - 控制台会显示最近的崩溃信息
   - 完整日志保存在 `tv_crash_log_*.txt`
   - 应用崩溃日志在 `crash_logs/` 目录

### 方法 2：实时监控

```bash
# 1. 连接电视
scripts\connect_tv.bat

# 2. 清除旧日志并开始监控
scripts\watch_tv_log.bat

# 3. 在电视上启动应用
# 4. 观察控制台输出的错误信息
```

## 详细步骤

### 1. 启用电视 ADB 调试

**小米电视/盒子：**
1. 进入 **设置 → 关于**
2. 连续点击 **Android 版本** 7 次，开启开发者模式
3. 返回设置，进入 **开发者选项**
4. 开启 **USB 调试** 和 **网络调试**

**其他品牌电视：**
- 通常在 **设置 → 系统 → 开发者选项** 中
- 如果找不到，尝试在"关于"页面连续点击版本号

### 2. 查看电视 IP 地址

**方法 1：电视设置**
- 进入 **设置 → 网络 → 网络状态**
- 查看 IP 地址（例如：192.168.1.100）

**方法 2：路由器管理页面**
- 登录路由器管理页面
- 查看已连接设备列表

### 3. 连接电视

**无线连接（推荐）：**
```bash
# 连接到电视
adb connect 192.168.1.100:5555

# 验证连接
adb devices
```

**USB 连接：**
```bash
# 使用 USB 线连接电视和电脑
# 某些电视支持 USB 调试

adb devices
```

### 4. 查看崩溃日志

**查看实时日志：**
```bash
# 清除旧日志
adb logcat -c

# 启动应用，然后查看日志
adb logcat *:E
```

**查看历史崩溃：**
```bash
# 查看最近的 FATAL 错误
adb logcat -d | findstr "FATAL"

# 查看应用相关错误
adb logcat -d | findstr "com.orange.player.legacy"
```

**导出日志到文件：**
```bash
# 导出完整日志
adb logcat -d > tv_crash_log.txt

# 只导出错误日志
adb logcat -d *:E > tv_error_log.txt
```

### 5. 查看应用崩溃日志文件

应用会自动保存崩溃日志到：
```
/sdcard/Android/data/com.orange.player.legacy/files/crash_logs/
```

**查看日志列表：**
```bash
adb shell "ls -la /sdcard/Android/data/com.orange.player.legacy/files/crash_logs/"
```

**拉取日志到电脑：**
```bash
# 拉取所有崩溃日志
adb pull /sdcard/Android/data/com.orange.player.legacy/files/crash_logs/ ./crash_logs/

# 查看最新的日志
adb shell "cat /sdcard/Android/data/com.orange.player.legacy/files/crash_logs/crash_*.txt"
```

## 常见崩溃原因

### 1. ClassNotFoundException / NoClassDefFoundError

**错误示例：**
```
java.lang.NoClassDefFoundError: Failed resolution of: Lcom/shuyu/gsyvideoplayer/...
```

**原因：** 缺少必需的依赖库

**解决方案：**
- 检查 `app-legacy/build.gradle` 中的依赖配置
- 确保所有 GSYVideoPlayer 模块都已正确引入

### 2. UnsatisfiedLinkError

**错误示例：**
```
java.lang.UnsatisfiedLinkError: dlopen failed: library "libijkffmpeg.so" not found
```

**原因：** 缺少 native 库（.so 文件）

**解决方案：**
- 检查 APK 中是否包含对应架构的 .so 文件
- 确认电视的 CPU 架构（armeabi-v7a / arm64-v8a）

### 3. RuntimeException: Unable to start activity

**错误示例：**
```
java.lang.RuntimeException: Unable to start activity ComponentInfo{...}
```

**原因：** Activity 初始化失败

**解决方案：**
- 查看完整的堆栈信息
- 检查 AndroidManifest.xml 配置
- 检查 Activity 的 onCreate 方法

### 4. OutOfMemoryError

**错误示例：**
```
java.lang.OutOfMemoryError: Failed to allocate a ... byte allocation
```

**原因：** 内存不足

**解决方案：**
- 降低视频缓存大小
- 及时释放资源
- 使用更小的图片资源

### 5. SecurityException

**错误示例：**
```
java.lang.SecurityException: Permission denied
```

**原因：** 缺少必要的权限

**解决方案：**
- 检查 AndroidManifest.xml 中的权限声明
- Android 6.0+ 需要运行时权限

## 诊断检查清单

使用此清单逐项排查：

- [ ] 电视已开启 ADB 调试
- [ ] 电脑和电视在同一网络
- [ ] ADB 成功连接到电视
- [ ] 应用已安装（`adb shell pm list packages | findstr orange`）
- [ ] 查看了实时日志（`adb logcat`）
- [ ] 查看了崩溃日志文件
- [ ] 记录了完整的错误堆栈
- [ ] 确认了电视的 Android 版本和 CPU 架构

## 收集诊断信息

提交 Issue 时，请提供以下信息：

### 1. 设备信息
```bash
# 获取设备信息
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getprop ro.product.cpu.abi
```

### 2. 应用信息
```bash
# 检查应用是否安装
adb shell pm list packages | findstr orange

# 查看应用版本
adb shell dumpsys package com.orange.player.legacy | findstr version
```

### 3. 崩溃日志
- 完整的 logcat 输出
- 应用崩溃日志文件
- 崩溃时的操作步骤

### 4. APK 信息
```bash
# 查看 APK 信息
adb shell pm path com.orange.player.legacy
adb pull <apk路径> app-legacy.apk

# 分析 APK
aapt dump badging app-legacy.apk
```

## 远程调试

如果无法物理接触电视，可以：

### 1. 使用远程 ADB 工具
- **ADB Wireless** (需要 root)
- **WiFi ADB** 应用

### 2. 使用日志收集应用
在应用中集成日志收集 SDK：
- Bugly
- Firebase Crashlytics
- Sentry

### 3. 使用远程控制
- TeamViewer
- AnyDesk
- 向日葵远程控制

## 常用 ADB 命令

```bash
# 连接设备
adb connect <IP>:5555

# 断开连接
adb disconnect <IP>:5555

# 查看连接的设备
adb devices

# 安装应用
adb install app-legacy-debug.apk

# 卸载应用
adb uninstall com.orange.player.legacy

# 启动应用
adb shell am start -n com.orange.player.legacy/.MainActivity

# 强制停止应用
adb shell am force-stop com.orange.player.legacy

# 清除应用数据
adb shell pm clear com.orange.player.legacy

# 查看应用日志
adb logcat | findstr "com.orange.player.legacy"

# 截图
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png

# 录屏
adb shell screenrecord /sdcard/screen.mp4
adb pull /sdcard/screen.mp4
```

## 故障排除

### ADB 连接失败

**问题：** `unable to connect to <IP>:5555`

**解决方案：**
1. 确认电视和电脑在同一网络
2. 检查电视防火墙设置
3. 重启电视的 ADB 服务
4. 尝试使用 USB 连接

### 设备未授权

**问题：** `device unauthorized`

**解决方案：**
1. 在电视上会弹出授权对话框，点击"允许"
2. 如果没有弹出，重启 ADB：
   ```bash
   adb kill-server
   adb start-server
   ```

### 日志太多看不清

**解决方案：**
```bash
# 只看错误日志
adb logcat *:E

# 只看特定标签
adb logcat OrangeApplication:D *:S

# 过滤关键词
adb logcat | findstr "FATAL"
```

## 获取帮助

如果以上方法无法解决问题：

1. **提交 Issue**
   - GitHub: https://github.com/706412584/orangeplayer/issues
   - 附上完整的崩溃日志和设备信息

2. **联系作者**
   - QQ: 706412584

3. **查看文档**
   - [FAQ](FAQ.md)
   - [Android 4.4 支持指南](ANDROID_4.4_SUPPORT.md)

---

**最后更新**: 2026-02-06  
**适用版本**: OrangePlayer Legacy v1.0+
