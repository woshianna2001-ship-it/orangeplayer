# OrangePlayer 下一步行动清单（Spec模式）

## 1. 目标与范围
基于当前进展（`orange-ffmpeg` 已独立、`orange-downloader` 已按需依赖、文档已初步同步），本清单聚焦：
- 补齐 JNI 封装能力
- 打通 ts 合并调用链
- 完成可控开关与验证

> 周期建议：5 个工作日（可并行执行）

---

## 2. 高优先级任务（按优先级排序）

| 优先级 | 任务 | 可量化目标 | 预计完成时间 |
|---|---|---|---|
| P0 | 完成 `orange-ffmpeg` JNI 最小可用封装 | 提供 `init/execute/cancel/getVersion` 4 个可调用接口；至少通过 2 条命令测试（`-version`、本地 ts 合并） | 1.5 天 |
| P0 | 打通 `orange-downloader` 到 `orange-ffmpeg` 的调用链 | 新增 1 个明确入口（如 `TsMergeService`）；在下载完成后可触发合并；完成 3 个关键日志点（开始/成功/失败） | 1 天 |
| P1 | 落地 FFmpeg 功能开关配置 | 提供 1 个总开关 + 1 个“失败回退”开关；在 `app` 与 `app-legacy` 均可读取；开关变更无需改代码即可生效 | 0.5 天 |
| P1 | 建立端到端验证用例 | 覆盖 3 类样本：普通 m3u8、AES-128 m3u8、异常流；成功标准：3/3 可生成可播放文件，失败场景有错误码与日志 | 1 天 |
| P2 | 产物与体积治理 | `orange-ffmpeg` 仅保留 `arm64-v8a` 必要产物；新增构建后检查：目标产物存在且单模块增量体积控制在预期阈值内（阈值由你当前发布标准填写） | 0.5 天 |

---

## 3. 进度追踪（Spec）

### 状态定义
- `TODO`：未开始
- `DOING`：进行中
- `DONE`：已完成
- `BLOCKED`：阻塞中

### 当前状态
- [x] P0 JNI 最小可用封装（DONE）
- [x] P0 下载到合并调用链打通（DONE）

- [x] P1 功能开关配置（DONE）

- [x] P1 端到端验证用例（DONE）
- [x] P2 产物与体积治理（DONE）

### 进展记录
- 2026-03-24：已创建 `orange-ffmpeg` 最小 API 骨架（`init/execute/cancel/getVersion`）与异步回调接口；当前为可运行的 stub 模式，待接入真实 JNI 执行链路。
- 2026-03-24：已新增 `TsMergeService` 作为下载后合并入口，打通 `orange-downloader -> orange-ffmpeg(可选)`，并保留 Java 合并回退路径。
- 2026-03-24：已落地功能开关：`FFmpeg 合并总开关` + `Java 回退开关`，支持 `Manifest meta-data` 默认值与运行时持久化覆盖；`app`/`app-legacy` 启动时均可读取并输出当前开关状态。
- 2026-03-24：已新增 `TsMergeServiceE2ETest` 覆盖 3 类样本（普通 m3u8、AES-128 标记 m3u8、异常流），并补齐 `TsMergeService` 合并错误码（`7001~7005/7099`）与失败状态回传；`orange-downloader` 编译通过。
- 2026-03-24：已在 `app/build.gradle` 的 `androidTest` 源集中临时排除历史用例 `PlayerStateManagementIntegrationTest.java`（该用例与当前主代码接口不一致），` :app:compileDebugAndroidTestSources ` 已恢复通过，`TsMergeServiceE2ETest` 可正常参与编译。
- 2026-03-24：已尝试执行 `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.orange.player.TsMergeServiceE2ETest`，当前环境无已连接设备（`No connected devices!`），实机/模拟器执行待补。
- 2026-03-24：已完成历史 `PlayerStateManagementIntegrationTest` 修复并移除 `app/build.gradle` 中的临时排除；` :app:compileDebugAndroidTestSources ` 通过，设备 `PJA110 - 16` 执行 `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.orange.player.PlayerStateManagementIntegrationTest` 结果 `3/3` 通过。

- 2026-03-24：设备 `PJA110 - 16` 已连接并完成实测，执行 `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.orange.player.TsMergeServiceE2ETest`，结果 `3/3` 通过，P1 端到端验证完成。
- 2026-03-24：已在 `orange-ffmpeg/build.gradle` 新增 `verifyFfmpegArtifact` 构建校验任务：检查仅允许 `arm64-v8a` ABI、检查 release AAR 产物存在、检查体积阈值（默认 `25MB`，可通过 `-PorangeFfmpegMaxAarSizeMb` 覆盖）；执行 `:orange-ffmpeg:verifyFfmpegArtifact` 通过。
- 2026-03-24：已接入 `orange-ffmpeg` 原生构建链路（`CMakeLists.txt` + `orangeffmpegkit` so），`FFmpegKit` 可走真实 JNI 路径（非 Java stub）；新增 `FFmpegKitJniIntegrationTest` 并在设备 `PJA110 - 16` 上通过 `2/2`（`init/getVersion`、`execute -version`）。
- 2026-03-24：JNI 已补齐本地 m3u8->ts 合并执行链（兼容 `-i ... -c copy output` 命令参数）；`FFmpegKitJniIntegrationTest` 新增本地合并用例并在设备 `PJA110 - 16` 通过 `3/3`，P0 JNI 最小可用封装完成。
- 2026-03-24：`orange-ffmpeg` 已支持按参数选择 ABI：`-PorangeFfmpegAbis=arm64-v8a,x86_64`；`verifyFfmpegArtifact` 改为校验 AAR 实际 JNI ABI 与请求集合一致。`app` 新增 ABI 对齐策略：`debug` 默认全 ABI、`release` 默认 `arm64-v8a`，并支持 `-PorangeAppDebugAbis/-PorangeAppReleaseAbis` 覆盖；同时显式引入 `project(':orange-ffmpeg')` 以启用 FFmpeg merge 运行时。已验证 `:orange-ffmpeg:assembleDebug :app:assembleDebug :app:compileDebugAndroidTestSources` 在双 ABI 参数下通过。







### 更新规则
- 每完成一个任务，勾选对应项并记录：完成日期、提交号、验证结果（通过/失败原因）。
- 若任务阻塞，标记 `BLOCKED` 并补充阻塞原因与预计解除时间。

---

## 4. 验收口径（完成定义）
- 核心能力：可稳定完成 ts 合并并输出可播放文件。
- 可控性：功能可通过开关启停，失败可回退。
- 可维护性：日志、错误码、文档保持一致，便于后续复用到其他项目。
