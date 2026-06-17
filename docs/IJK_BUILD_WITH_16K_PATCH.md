# IJK 编译指南（包含 16K 补丁 + HLS 修复）

## 🎯 本次编译包含的特性

### 1. GSYVideoPlayer 16K 补丁
- ✅ NDK r22 兼容性修复
- ✅ 16K Page Size 支持（arm64/x86_64）
- ✅ Stack Canary 保护
- ✅ LLVM libc++ (c++_static)
- ✅ 修复 `ijkav_register_async_protocol` 崩溃

### 2. FFmpeg n4.3 + OpenSSL
- ✅ 使用 CarGuo/FFmpeg tag `ijk-n4.3-20260301-007`
- ✅ 包含 ijk 协议/demuxer 兼容
- ✅ OpenSSL 1.1.1w 支持（加密 m3u8）
- ✅ crypto 协议支持

### 3. HLS Discontinuity 修复
- ✅ 修复 m3u8 seek 跳跃问题
- ✅ 支持广告插入的 m3u8
- ✅ 支持 `#EXT-X-DISCONTINUITY` 标记

## 📋 编译步骤

### 方式 1：一键编译（推荐）

```bash
# 设置代理（如果需要）
export http_proxy=http://172.26.80.1:7887
export https_proxy=http://172.26.80.1:7887

# 运行编译脚本
cd /mnt/d/android/projecet_iade/orangeplayer
bash scripts/build_ijk_with_hls_fix.sh
```

### 方式 2：手动执行

```bash
# 1. 创建工作目录
mkdir -p ~/ijkplayer-build-hls-fix
cd ~/ijkplayer-build-hls-fix

# 2. 下载 IJKPlayer
git clone https://github.com/bilibili/ijkplayer.git
cd ijkplayer
git checkout -B k0.8.8 k0.8.8

# 3. 应用 GSYVideoPlayer 16K 补丁
PATCH_DIR="/mnt/d/android/projecet_iade/orangeplayer/GSYVideoPlayer-source/16kpatch"

git apply "$PATCH_DIR/ndk_r22_16k_commit.patch"

cd ijkmedia/ijksoundtouch
git apply "$PATCH_DIR/ndk_r22_soundtouch.patch"
cd ../..

cd ijkmedia/ijkyuv
git apply "$PATCH_DIR/ndk_r22_ijkyuv.patch"
cd ../..

# 4. 修改 FFmpeg 源为 CarGuo/FFmpeg
sed -i 's|IJK_FFMPEG_UPSTREAM=.*|IJK_FFMPEG_UPSTREAM=https://github.com/CarGuo/FFmpeg.git|g' init-android.sh
sed -i 's|IJK_FFMPEG_FORK=.*|IJK_FFMPEG_FORK=https://github.com/CarGuo/FFmpeg.git|g' init-android.sh
sed -i 's|IJK_FFMPEG_COMMIT=.*|IJK_FFMPEG_COMMIT=ijk-n4.3-20260301-007|g' init-android.sh

# 5. 初始化
./init-android-openssl.sh
./init-android.sh

# 6. 应用 FFmpeg 补丁（可选，CarGuo 的 FFmpeg 可能已包含）
cd android/ffmpeg
git apply "$PATCH_DIR/ndk_r22_ffmpeg_n4.3_ijk.patch"
cd ../..

# 7. 配置编译选项
cd config
rm -f module.sh
ln -s module-lite-hevc.sh module.sh
cd ..

# 8. 编译
cd android/contrib
./compile-openssl.sh clean
./compile-openssl.sh all
./compile-ffmpeg.sh clean
./compile-ffmpeg.sh all
cd ..
./compile-ijk.sh all
```

## ⏱️ 编译时间估算

| 步骤 | 预计时间 |
|------|---------|
| 下载 IJKPlayer | 1-3 分钟 |
| 应用补丁 | 1 分钟 |
| 初始化 OpenSSL | 2-5 分钟 |
| 初始化 FFmpeg | 10-20 分钟 |
| 编译 OpenSSL | 10-30 分钟 |
| 编译 FFmpeg | 2-4 小时 |
| 编译 IJK | 30-60 分钟 |
| **总计** | **4-6 小时** |

## 📦 编译产物

### SO 文件位置
```
~/ijkplayer-build-hls-fix/ijkplayer/android/ijkplayer/
├── ijkplayer-armv7a/src/main/libs/armeabi-v7a/
│   ├── libijkffmpeg.so (4K Page Size)
│   ├── libijkplayer.so
│   └── libijksdl.so
├── ijkplayer-arm64/src/main/libs/arm64-v8a/
│   ├── libijkffmpeg.so (16K Page Size)
│   ├── libijkplayer.so
│   └── libijksdl.so
├── ijkplayer-x86/src/main/libs/x86/
│   ├── libijkffmpeg.so (4K Page Size)
│   ├── libijkplayer.so
│   └── libijksdl.so
└── ijkplayer-x86_64/src/main/libs/x86_64/
    ├── libijkffmpeg.so (16K Page Size)
    ├── libijkplayer.so
    └── libijksdl.so
```

### 验证 Page Size

```bash
# 查看 arm64 SO 的对齐（应该是 0x4000 = 16K）
readelf -l libijkffmpeg.so | grep LOAD

# 查看 armv7a SO 的对齐（应该是 0x1000 = 4K）
readelf -l libijkffmpeg.so | grep LOAD
```

## 🔄 复制 SO 到项目

```bash
cd /mnt/d/android/projecet_iade/orangeplayer
bash scripts/copy_ijk_so.sh
```

## 🧪 测试验证

### 1. 重新编译项目

```powershell
# 在 Windows PowerShell 中
./gradlew clean
./gradlew :app:installDebug
```

### 2. 测试视频

播放包含 discontinuity 的 m3u8：
```
https://c1.rrcdnbf6.com/video/sanrenxingbiyouwomei/第01集/index.m3u8
```

### 3. 验证 Seek 功能

- 拖动进度条到 280 秒
- 应该准确跳转到 280 秒（不应该跳到 511 秒）

### 4. 验证加密 m3u8

测试播放加密的 HLS 视频（如果有的话）

### 5. 验证 16K Page Size

在 Android 15+ 设备上测试（如果有的话）

## 📊 对比表

| 特性 | 官方 IJK | GSYVideoPlayer ex_so | 本次编译 |
|------|---------|---------------------|---------|
| FFmpeg 版本 | n3.4 | n4.3 | n4.3 |
| OpenSSL | ❌ | ✅ 1.1.1w | ✅ 1.1.1w |
| 加密 m3u8 | ❌ | ✅ | ✅ |
| 16K Page Size | ❌ | ✅ | ✅ |
| HLS Discontinuity 修复 | ❌ | ❌ | ✅ |
| Stack Canary | ❌ | ✅ | ✅ |
| NDK 版本 | r10e | r22 | r22 |

## ❓ 常见问题

### Q1: 补丁应用失败

**A:** 补丁可能已经包含在 CarGuo 的 FFmpeg 中，继续编译即可。

### Q2: 编译 FFmpeg 时报错

**A:** 检查：
1. NDK 版本是否是 r22
2. 代理是否正常
3. 磁盘空间是否足够（至少 10GB）

### Q3: SO 文件很小

**A:** 检查编译日志，确保 FFmpeg 编译成功。正常的 `libijkffmpeg.so` 应该在 3-5MB 左右。

### Q4: 16K Page Size 验证失败

**A:** 使用 `readelf -l libijkffmpeg.so | grep LOAD` 查看 Align 值：
- arm64/x86_64: 应该是 `0x4000` (16384)
- armv7a/x86: 应该是 `0x1000` (4096)

### Q5: 运行时崩溃

**A:** 可能的原因：
1. SO 文件没有正确复制到项目
2. NDK 版本不匹配
3. 缺少某些依赖库

## 🔗 参考资料

- [GSYVideoPlayer 16K 补丁](https://github.com/CarGuo/GSYVideoPlayer/tree/master/16kpatch)
- [Android 15 16K Page Size 适配](https://juejin.cn/post/7396306532671094793)
- [IJKPlayer 官方文档](https://github.com/bilibili/ijkplayer)
- [FFmpeg HLS Discontinuity](https://github.com/jjustman/ffmpeg-hls-pts-discontinuity-reclock)

## 📝 注意事项

1. ⚠️ 编译时间很长（4-6 小时），建议晚上运行
2. ⚠️ 需要至少 10GB 磁盘空间
3. ⚠️ 需要稳定的网络连接（建议使用代理）
4. ⚠️ 编译成功后务必充分测试各种视频格式
5. ⚠️ 16K Page Size 主要针对 Android 15+，但向下兼容

## ✅ 成功标志

编译成功后，你应该看到：

```
[10/10] 编译 IJK...
  开始编译 IJK（这可能需要 30-60 分钟）...
  
========================================
  编译完成！
========================================

SO 文件位置：
  armv7a: ~/ijkplayer-build-hls-fix/ijkplayer/android/ijkplayer/ijkplayer-armv7a/src/main/libs/armeabi-v7a/
  arm64:  ~/ijkplayer-build-hls-fix/ijkplayer/android/ijkplayer/ijkplayer-arm64/src/main/libs/arm64-v8a/
  x86:    ~/ijkplayer-build-hls-fix/ijkplayer/android/ijkplayer/ijkplayer-x86/src/main/libs/x86/
  x86_64: ~/ijkplayer-build-hls-fix/ijkplayer/android/ijkplayer/ijkplayer-x86_64/src/main/libs/x86_64/
```

祝你编译顺利！🎉
