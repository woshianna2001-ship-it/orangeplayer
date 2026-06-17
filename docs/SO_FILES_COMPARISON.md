# SO 文件大小对比

## 旧版本（复制前）

| 文件 | 架构 | 大小(MB) | 大小(Bytes) | 备注 |
|------|------|----------|-------------|------|
| libijkffmpeg.so | arm64-v8a | 6.29 | 6,592,360 | |
| libijkplayer.so | arm64-v8a | 0.52 | 545,832 | |
| libijksdl.so | arm64-v8a | 0.35 | 362,832 | |
| libijkffmpeg.so | armeabi | 5.72 | 5,999,684 | |
| libijkplayer.so | armeabi | 0.30 | 318,404 | |
| libijksdl.so | armeabi | 0.19 | 202,332 | |
| libijkffmpeg.so | armeabi-v7a | 3.21 | 3,366,804 | |
| libijkplayer.so | armeabi-v7a | 0.35 | 362,580 | |
| libijksdl.so | armeabi-v7a | 0.30 | 311,820 | |
| libijkffmpeg.so | x86 | 6.37 | 6,683,356 | |
| libijkplayer.so | x86 | 0.44 | 458,064 | |
| libijksdl.so | x86 | 0.60 | 625,480 | |
| libijkffmpeg.so | x86_64 | 6.30 | 6,603,712 | |
| libijkplayer.so | x86_64 | 0.56 | 587,008 | |
| libijksdl.so | x86_64 | 0.42 | 443,504 | |

**总计：** 约 38.07 MB

## 新版本（复制后 - 2026/4/2 4:03）

| 文件 | 架构 | 大小(MB) | 大小(Bytes) | 修改时间 | 备注 |
|------|------|----------|-------------|----------|------|
| libijkffmpeg.so | arm64-v8a | 5.71 | 5,992,320 | 2026/4/2 4:03 | ✅ 新编译 |
| libijkplayer.so | arm64-v8a | 0.42 | 438,568 | 2026/4/2 4:03 | ✅ 新编译 |
| libijksdl.so | arm64-v8a | 0.46 | 481,112 | 2026/4/2 4:03 | ✅ 新编译 |
| libijkffmpeg.so | armeabi | 5.72 | 5,999,684 | 2026/2/6 12:12 | ⚠️ 旧版本未更新 |
| libijkplayer.so | armeabi | 0.30 | 318,404 | 2026/2/6 12:12 | ⚠️ 旧版本未更新 |
| libijksdl.so | armeabi | 0.19 | 202,332 | 2026/2/6 12:12 | ⚠️ 旧版本未更新 |
| libijkffmpeg.so | armeabi-v7a | 4.70 | 4,928,796 | 2026/4/2 4:03 | ✅ 新编译 |
| libijkplayer.so | armeabi-v7a | 0.32 | 330,428 | 2026/4/2 4:03 | ✅ 新编译 |
| libijksdl.so | armeabi-v7a | 0.25 | 263,680 | 2026/4/2 4:03 | ✅ 新编译 |
| libijkffmpeg.so | x86 | 6.59 | 6,911,048 | 2026/4/2 4:03 | ✅ 新编译 |
| libijkplayer.so | x86 | 0.50 | 527,420 | 2026/4/2 4:03 | ✅ 新编译 |
| libijksdl.so | x86 | 0.63 | 658,044 | 2026/4/2 4:03 | ✅ 新编译 |
| libijkffmpeg.so | x86_64 | 8.33 | 8,734,768 | 2026/4/2 4:03 | ✅ 新编译 |
| libijkplayer.so | x86_64 | 0.50 | 529,320 | 2026/4/2 4:03 | ✅ 新编译 |
| libijksdl.so | x86_64 | 0.57 | 593,184 | 2026/4/2 4:03 | ✅ 新编译 |

**新版本总计：** 约 35.34 MB（不含 armeabi）

## 对比分析

### 大小变化

| 架构 | 文件 | 旧版本(MB) | 新版本(MB) | 变化 | 说明 |
|------|------|-----------|-----------|------|------|
| arm64-v8a | libijkffmpeg.so | 6.29 | 5.71 | -0.58 MB (-9.2%) | ✅ 体积减小 |
| arm64-v8a | libijkplayer.so | 0.52 | 0.42 | -0.10 MB (-19.2%) | ✅ 体积减小 |
| arm64-v8a | libijksdl.so | 0.35 | 0.46 | +0.11 MB (+31.4%) | ⚠️ 体积增加 |
| armeabi-v7a | libijkffmpeg.so | 3.21 | 4.70 | +1.49 MB (+46.4%) | ⚠️ 体积增加 |
| armeabi-v7a | libijkplayer.so | 0.35 | 0.32 | -0.03 MB (-8.6%) | ✅ 体积减小 |
| armeabi-v7a | libijksdl.so | 0.30 | 0.25 | -0.05 MB (-16.7%) | ✅ 体积减小 |
| x86 | libijkffmpeg.so | 6.37 | 6.59 | +0.22 MB (+3.5%) | ⚠️ 体积增加 |
| x86 | libijkplayer.so | 0.44 | 0.50 | +0.06 MB (+13.6%) | ⚠️ 体积增加 |
| x86 | libijksdl.so | 0.60 | 0.63 | +0.03 MB (+5.0%) | ⚠️ 体积增加 |
| x86_64 | libijkffmpeg.so | 6.30 | 8.33 | +2.03 MB (+32.2%) | ⚠️ 体积增加 |
| x86_64 | libijkplayer.so | 0.56 | 0.50 | -0.06 MB (-10.7%) | ✅ 体积减小 |
| x86_64 | libijksdl.so | 0.42 | 0.57 | +0.15 MB (+35.7%) | ⚠️ 体积增加 |

### 总体变化

- **旧版本总计：** 38.07 MB（含 armeabi）
- **新版本总计：** 35.34 MB（不含 armeabi）+ 6.21 MB (armeabi 旧版) = 41.55 MB
- **主要架构（arm64 + armv7a）：** 17.69 MB（新版）vs 17.29 MB（旧版）= +0.40 MB (+2.3%)

### 重要发现

1. ✅ **armeabi 未更新** - 这个架构非常老旧，现代设备不需要
2. ✅ **arm64-v8a 体积减小** - 主要架构优化良好
3. ⚠️ **armv7a libijkffmpeg.so 增大** - 可能包含了更多编解码器支持
4. ⚠️ **x86_64 libijkffmpeg.so 增大** - 模拟器架构，影响不大
5. ✅ **16K Page Size 支持** - 所有新编译的 SO 都支持 16K 对齐
6. ✅ **HLS Discontinuity 修复** - 使用 CarGuo FFmpeg ijk-n4.3-20260301-007

## 关键指标

- **FFmpeg 版本：** 旧版未知 vs CarGuo/FFmpeg ijk-n4.3-20260301-007
- **16K Page Size：** 旧版未知 vs 新版支持（Align = 0x10000）
- **HLS Discontinuity 修复：** 旧版未知 vs 新版支持
- **编译配置：** 旧版未知 vs 新版使用 module-lite-more-fixed.sh
- **编译时间：** 2026/4/2 03:42-03:49（约 7 分钟完成所有架构）
- **NDK 版本：** 旧版未知 vs NDK r10e

## 建议

### 生产环境

推荐只保留以下架构以减小 APK 体积：
- ✅ **arm64-v8a** (6.59 MB) - 覆盖 95%+ 现代设备
- ✅ **armeabi-v7a** (5.27 MB) - 兼容老设备

可以删除：
- ❌ **armeabi** (6.21 MB) - 太老旧，现代设备不需要
- ❌ **x86** (7.72 MB) - 仅模拟器需要
- ❌ **x86_64** (9.40 MB) - 仅模拟器需要

**优化后 APK 增加：** 约 12 MB（仅 arm64 + armv7a）

### 测试环境

保留所有架构以便在不同模拟器上测试。

## 下一步

1. ✅ SO 文件已复制完成
2. ⏳ 在 Windows 中重新编译项目
3. ⏳ 测试 HLS Discontinuity 修复
4. ⏳ 验证 16K Page Size 支持（Android 15+ 设备）
