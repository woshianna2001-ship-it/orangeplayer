# DLNA 投屏功能指南

OrangePlayer 支持通过 DLNA 协议将视频投屏到支持 DLNA 的设备（如电视、投影仪等）。

## 功能特性

- ✅ DLNA 设备发现
- ✅ 实时投屏控制
- ✅ 音量、进度调节
- ✅ 暂停/播放控制
- ✅ 自动断线重连

## 依赖配置

### 1. 添加 DLNA 库依赖

在 `app/build.gradle` 中添加：

```gradle
dependencies {
    // DLNA 投屏库
    implementation 'com.github.AnyListen:UaoanDLNA:1.0.1'
    
    // HTTP 客户端（DLNA 依赖）
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
}
```

### 2. 添加 JitPack 仓库

在 `settings.gradle` 中确保有 JitPack 仓库：

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }  // 添加 JitPack
    }
}
```

### 3. 配置 ProGuard 规则

在 `app/proguard-rules.pro` 中添加：

```proguard
# DLNA 投屏
-keep class com.uaoanlao.tv.** { *; }
-keep class com.cicada.player.** { *; }
```

## 使用方法

### 基础使用

```java
// 获取投屏控制器
CastController castController = orangeVideoView.getCastController();

// 搜索 DLNA 设备
castController.startSearch();

// 获取设备列表
List<DLNADevice> devices = castController.getDevices();

// 连接设备
castController.connectDevice(device);

// 投屏视频
castController.cast(videoUrl);

// 控制播放
castController.play();
castController.pause();
castController.stop();

// 调节音量
castController.setVolume(50);

// 调节进度
castController.seek(5000); // 5 秒
```

### 监听投屏事件

```java
castController.setOnCastListener(new OnCastListener() {
    @Override
    public void onDeviceFound(DLNADevice device) {
        // 发现设备
        Log.d("Cast", "Found device: " + device.getName());
    }
    
    @Override
    public void onConnected(DLNADevice device) {
        // 连接成功
        Log.d("Cast", "Connected to: " + device.getName());
    }
    
    @Override
    public void onDisconnected() {
        // 断开连接
        Log.d("Cast", "Disconnected");
    }
    
    @Override
    public void onError(String error) {
        // 错误
        Log.e("Cast", "Error: " + error);
    }
    
    @Override
    public void onPlayStateChanged(int state) {
        // 播放状态改变
        switch (state) {
            case PLAYING:
                Log.d("Cast", "Playing");
                break;
            case PAUSED:
                Log.d("Cast", "Paused");
                break;
            case STOPPED:
                Log.d("Cast", "Stopped");
                break;
        }
    }
});
```

## 完整示例

```java
public class CastActivity extends AppCompatActivity {
    
    private OrangevideoView videoView;
    private CastController castController;
    private ListView deviceListView;
    private ArrayAdapter<String> adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cast);
        
        videoView = findViewById(R.id.video_view);
        deviceListView = findViewById(R.id.device_list);
        
        // 初始化投屏控制器
        castController = videoView.getCastController();
        
        // 设置投屏监听
        castController.setOnCastListener(new OnCastListener() {
            @Override
            public void onDeviceFound(DLNADevice device) {
                addDeviceToList(device);
            }
            
            @Override
            public void onConnected(DLNADevice device) {
                Toast.makeText(CastActivity.this, 
                    "已连接到: " + device.getName(), 
                    Toast.LENGTH_SHORT).show();
                
                // 投屏当前视频
                castController.cast(videoView.getCurrentUrl());
            }
            
            @Override
            public void onDisconnected() {
                Toast.makeText(CastActivity.this, 
                    "已断开连接", 
                    Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(CastActivity.this, 
                    "投屏错误: " + error, 
                    Toast.LENGTH_SHORT).show();
            }
        });
        
        // 设备列表点击事件
        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            DLNADevice device = castController.getDevices().get(position);
            castController.connectDevice(device);
        });
        
        // 开始搜索设备
        findViewById(R.id.btn_search).setOnClickListener(v -> {
            castController.startSearch();
            Toast.makeText(this, "正在搜索设备...", Toast.LENGTH_SHORT).show();
        });
        
        // 停止投屏
        findViewById(R.id.btn_stop).setOnClickListener(v -> {
            castController.stop();
        });
    }
    
    private void addDeviceToList(DLNADevice device) {
        if (adapter == null) {
            adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_list_item_1);
            deviceListView.setAdapter(adapter);
        }
        adapter.add(device.getName());
        adapter.notifyDataSetChanged();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (castController != null) {
            castController.stop();
        }
    }
}
```

## 布局文件示例

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">
    
    <!-- 视频播放器 -->
    <com.orange.playerlibrary.OrangevideoView
        android:id="@+id/video_view"
        android:layout_width="match_parent"
        android:layout_height="300dp" />
    
    <!-- 搜索按钮 -->
    <Button
        android:id="@+id/btn_search"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="搜索设备" />
    
    <!-- 设备列表 -->
    <ListView
        android:id="@+id/device_list"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />
    
    <!-- 停止投屏按钮 -->
    <Button
        android:id="@+id/btn_stop"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="停止投屏" />
    
</LinearLayout>
```

## 常见问题

### Q1: 搜索不到设备？

**A:** 检查以下几点：
1. 确保手机和投屏设备在同一个 WiFi 网络
2. 投屏设备支持 DLNA 协议
3. 投屏设备已打开并处于待机状态
4. 检查防火墙设置

### Q2: 投屏后没有声音？

**A:** 可能原因：
1. 投屏设备的音量设置为静音
2. 检查投屏设备的音频输出设置
3. 尝试重新连接设备

### Q3: 投屏中途断开连接？

**A:** 解决方案：
1. 检查 WiFi 信号强度
2. 减少网络干扰
3. 重新连接设备
4. 更新投屏设备固件

### Q4: 支持哪些视频格式？

**A:** 支持的格式取决于投屏设备的支持情况，通常包括：
- MP4 (H.264/H.265)
- MKV
- AVI
- MOV
- FLV

## 权限配置

在 `AndroidManifest.xml` 中添加以下权限：

```xml
<!-- 网络权限 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />

<!-- WiFi 权限 -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />

<!-- 多播权限 -->
<uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
```

## 性能优化

1. **设备搜索超时**：默认 10 秒，可根据需要调整
2. **缓冲大小**：根据网络速度调整缓冲时间
3. **连接池**：复用连接以提高效率

## 故障排查

如果遇到问题，可以启用调试日志：

```java
// 启用调试
castController.setDebug(true);

// 查看日志
adb logcat | grep "Cast"
```

## 相关资源

- [UaoanDLNA GitHub](https://github.com/AnyListen/UaoanDLNA)
- [DLNA 协议文档](https://www.dlna.org/)
- [OrangePlayer API 文档](API.md)

## 更新日志

### v1.1.0
- ✅ 支持 DLNA 投屏
- ✅ 设备自动发现
- ✅ 实时播放控制

---

如有问题，请提交 [Issue](https://github.com/706412584/orangeplayer/issues)
