# IJKPlayer 编译快速指南

## 新脚本：build_ijk_official.sh

基于官方文档和 16K 编译教程创建的完整编译脚本。

### 主要改进

1. **使用 NDK r10e**（官方推荐）
   - 使用 65536 (64K) 对齐支持 16K Page Size
   - 自动下载和配置

2. **应用你的 FFmpeg 配置**
   - 自动复制 `GSYVideoPlayer-source/module-lite-more-fixed.sh`
   - 包含所有你配置的协议和编解码器

3. **16K Page Size 支持**
   - 自动修改 Android.mk 文件
   - 自动修改 FFmpeg 和 OpenSSL 编译脚本
   - 使用 `-Wl,-z,max-page-size=65536` 链接标志

4. **CarGuo FFmpeg（包含 HLS 修复）**
   - 使用 `ijk-n4.3-20260301-007` 版本
   - 包含 HLS Discontinuity 修复

### 运行方式

```bash
cd /mnt/d/android/projecet_iade/orangeplayer
bash scripts/build_ijk_official.sh
```

### 编译流程

1. 检查依赖（git, yasm, nasm, gcc, make, wget, unzip, curl）
2. 下载 NDK r10e（约 400MB）
3. 克隆 IJKPlayer
4. 配置 FFmpeg 源（CarGuo/FFmpeg）
5. 初始化 OpenSSL 和 FFmpeg
6. 应用 16K Page Size 补丁
7. 配置编译选项（使用你的 module-lite-more-fixed.sh）
8. 选择编译架构（推荐 arm64）
9. 开始编译

### 架构选择

- **arm64** (推荐) - 覆盖 95%+ 现代设备
- **armv7a** - 兼容老设备
- **x86/x86_64** - 模拟器测试
- **all** - 编译所有架构（耗时最长）

### 编译时间估算

- arm64 单架构：约 1-2 小时
- 所有架构：约 3-4 小时

### 验证 16K 对齐

编译完成后，验证 SO 文件是否正确对齐：

```bash
readelf -l libijkffmpeg.so | grep LOAD
```

应该看到：`Align = 0x10000` (65536)

### 下一步

1. 验证 SO 文件对齐（可选）
2. 运行 `scripts/copy_ijk_so.sh` 复制 SO 文件到项目
3. 在 Windows 中重新编译项目
4. 测试视频播放
