# 崩溃提示功能说明

OrangePlayer Legacy 现在支持在应用崩溃时显示 Toast 提示。

## 功能特性

### 1. 崩溃时自动提示

当应用崩溃时，会自动显示 Toast 提示：

```
应用崩溃：NullPointerException
日志已保存
```

### 2. 自动保存日志

崩溃日志会自动保存到：
```
/sdcard/Android/data/com.orange.player.legacy/files/crash_logs/crash_YYYY-MM-DD_HH-mm-ss.txt
```

### 3. 延迟关闭

Toast 显示后会延迟 2 秒再关闭应用，确保用户能看到提示。

## 工作原理

### 1. 全局异常捕获

```java
Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        // 1. 保存崩溃日志
        String logPath = saveCrashLog(throwable);
        
        // 2. 显示 Toast 提示
        showCrashToast(throwable, logPath);
        
        // 3. 延迟 2 秒
        Thread.sleep(2000);
        
        // 4. 调用系统默认处理器（关闭应用）
        defaultHandler.uncaughtException(thread, throwable);
    }
});
```

### 2. Toast 显示

在新线程中创建 Looper 来显示 Toast：

```java
new Thread(new Runnable() {
    @Override
    public void run() {
        Looper.prepare();
        
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        
        Looper.loop();
    }
}).start();
```

### 3. 日志保存

保存完整的崩溃信息：
- 时间戳
- 应用版本
- 设备信息
- 异常堆栈

## 使用场景

### 场景 1：电视机上调试

**问题：** 电视机上应用闪退，看不到错误信息

**解决：**
1. 安装带崩溃提示的版本
2. 复现崩溃
3. 看到 Toast 提示："应用崩溃：XXX"
4. 通过 ADB 拉取日志文件

### 场景 2：用户反馈

**问题：** 用户报告应用崩溃，但不知道原因

**解决：**
1. 用户看到崩溃提示
2. 告知开发者错误类型（如 NullPointerException）
3. 开发者可以快速定位问题

### 场景 3：现场演示

**问题：** 演示时应用崩溃，很尴尬

**解决：**
1. Toast 提示让用户知道发生了什么
2. "日志已保存"让用户知道问题会被记录
3. 提升专业度

## Toast 显示内容

### 基本格式

```
应用崩溃：<异常类型>
日志已保存
```

### 常见异常类型

| 异常类型 | Toast 显示 | 含义 |
|---------|-----------|------|
| NullPointerException | 应用崩溃：NullPointerException | 空指针异常 |
| ClassNotFoundException | 应用崩溃：ClassNotFoundException | 类未找到 |
| UnsatisfiedLinkError | 应用崩溃：UnsatisfiedLinkError | 缺少 native 库 |
| OutOfMemoryError | 应用崩溃：OutOfMemoryError | 内存不足 |
| RuntimeException | 应用崩溃：RuntimeException | 运行时异常 |

## 测试崩溃提示

### 方法 1：手动触发崩溃

在代码中添加测试代码：

```java
// 在 MainActivity 的某个按钮点击事件中
throw new RuntimeException("测试崩溃");
```

### 方法 2：触发空指针

```java
String test = null;
test.length();  // 会抛出 NullPointerException
```

### 方法 3：触发内存溢出

```java
List<byte[]> list = new ArrayList<>();
while (true) {
    list.add(new byte[1024 * 1024]);  // 会抛出 OutOfMemoryError
}
```

## 查看崩溃日志

### 方法 1：通过 ADB

```bash
# 拉取所有崩溃日志
adb pull /sdcard/Android/data/com.orange.player.legacy/files/crash_logs/ ./

# 查看最新的日志
adb shell "ls -lt /sdcard/Android/data/com.orange.player.legacy/files/crash_logs/ | head -n 2"
adb shell "cat /sdcard/Android/data/com.orange.player.legacy/files/crash_logs/crash_*.txt"
```

### 方法 2：使用诊断脚本

```bash
scripts\diagnose_tv_crash.bat
```

会自动拉取所有崩溃日志到 `crash_logs/` 目录。

### 方法 3：在设备上查看

使用文件管理器访问：
```
/sdcard/Android/data/com.orange.player.legacy/files/crash_logs/
```

## 日志文件格式

```
=== 崩溃信息 ===
时间: 2026-02-06_15-30-45
应用包名: com.orange.player.legacy
应用版本: 1.0-legacy
版本号: 1

=== 设备信息 ===
Android 版本: 4.4.2
API Level: 19
设备型号: MI TV 4A
设备厂商: Xiaomi
设备品牌: Xiaomi
CPU ABI: armeabi-v7a

=== 异常堆栈 ===
java.lang.NullPointerException
    at com.orange.player.MainActivity.onCreate(MainActivity.java:45)
    at android.app.Activity.performCreate(Activity.java:5008)
    ...
```

## 注意事项

### 1. Toast 可能不显示

**原因：**
- 应用崩溃太快
- 系统资源不足
- Toast 队列已满

**解决：**
- 延迟时间已设置为 2 秒
- 日志文件仍会保存
- 可以通过 ADB 查看日志

### 2. 权限问题

**需要权限：**
```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

**Android 6.0+：**
- 需要运行时权限
- 但 `getExternalFilesDir()` 不需要权限

### 3. 存储空间

**日志文件大小：**
- 每个日志文件约 5-10 KB
- 建议定期清理旧日志

**清理方法：**
```bash
# 清理所有崩溃日志
adb shell "rm -rf /sdcard/Android/data/com.orange.player.legacy/files/crash_logs/*"
```

## 优化建议

### 1. 添加日志上传

可以集成崩溃上报 SDK：
- Bugly
- Firebase Crashlytics
- Sentry

### 2. 添加重启选项

在 Toast 中添加"重启应用"按钮：

```java
// 显示对话框而不是 Toast
AlertDialog.Builder builder = new AlertDialog.Builder(context);
builder.setTitle("应用崩溃")
       .setMessage("应用遇到错误，是否重启？")
       .setPositiveButton("重启", new DialogInterface.OnClickListener() {
           @Override
           public void onClick(DialogInterface dialog, int which) {
               // 重启应用
               restartApp();
           }
       })
       .setNegativeButton("退出", null)
       .show();
```

### 3. 添加日志清理

定期清理旧日志：

```java
// 只保留最近 10 个日志文件
File[] logs = logDir.listFiles();
if (logs != null && logs.length > 10) {
    Arrays.sort(logs, new Comparator<File>() {
        @Override
        public int compare(File f1, File f2) {
            return Long.compare(f2.lastModified(), f1.lastModified());
        }
    });
    
    for (int i = 10; i < logs.length; i++) {
        logs[i].delete();
    }
}
```

## 相关文档

- [电视崩溃诊断指南](TV_CRASH_DIAGNOSIS.md)
- [调试工具使用说明](../scripts/README_TV_DEBUG.md)
- [FAQ](FAQ.md)

---

**最后更新**: 2026-02-06  
**适用版本**: OrangePlayer Legacy v1.0+
