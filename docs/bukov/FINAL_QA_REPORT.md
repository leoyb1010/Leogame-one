# 《逃离布科夫》Alpha 31 最终门禁前 QA 状态

当前状态仍为 **`PENDING EVIDENCE`，不是个人单机版 100% 最终签字**。

本报告封存的发行源码 SHA 是
`a91461c5ba57aedb76f07acd741b749100425fb2`，版本
`2.0.0-alpha31-e2e-ci`。macOS/iOS Simulator 包、安装回执、双端墙钟性能、
33 项本地 final gate 和远端 CI 均绑定这一 SHA。

本文件属于证据整理，文档提交可以晚于封存发行源码；不得把文档提交 SHA 当作
Alpha 31 二进制身份。物理 iPhone、实体手柄、硬件 GPU/Instruments、三人盲测
和完整双平台人工玩家路线录像仍为 `NOT RUN` 或 `PENDING EVIDENCE`。

## 构建身份

| 字段 | 值 |
|---|---|
| 产品 | 逃离布科夫 / Escape from Bukov |
| 封存候选 | `2.0.0-alpha31-e2e-ci` |
| 封存发行 Source commit | `a91461c5ba57aedb76f07acd741b749100425fb2` |
| 分支 | `codex/bukov-realtime` |
| 日期 | `2026-07-25 / Asia/Shanghai` |
| macOS archive SHA-256 | `c8d92774e6703092073ba2322c93cff04b9d2738a54c3a5af42f851405ccf2bf` |
| macOS 主程序 SHA-256 | `3e5a29cfdce0423bac62b658964c8d574b41b316de357cc1ad688cf6699e8358` |
| iOS Simulator archive SHA-256 | `cc178c6fd7372a49c7ffc51b4e1170a04c304b475ecece60cb97b722cb935529` |
| iOS Simulator 主程序 SHA-256 | `92181b4af77a6e3a7be8ddfbd41a69fb490cfd19ced65b5d107e0e1a09be8079` |

产物与证据：

- 包目录：
  `/Users/leoyuan/Documents/日常/output/逃离布科夫-alpha31-e2e-ci-a91461c5b`
- macOS archive：
  `/Users/leoyuan/Documents/日常/output/逃离布科夫-alpha31-e2e-ci-a91461c5b/逃离布科夫-macOS-alpha31-e2e-ci-a91461c5b.zip`
- iOS Simulator archive：
  `/Users/leoyuan/Documents/日常/output/逃离布科夫-alpha31-e2e-ci-a91461c5b/逃离布科夫-iOS-Simulator-alpha31-e2e-ci-a91461c5b.zip`
- 安装回执：
  `/Users/leoyuan/Library/Logs/EscapeFromBukov/install-receipts/20260725T015200Z-a91461c5ba57-99831.txt`
- 双端性能与原生音频：
  `/Users/leoyuan/Documents/日常/output/evidence/a91461c5b-alpha31-performance`
- 完整自动 final gate：
  `/Users/leoyuan/Documents/日常/output/evidence/a91461c5b-alpha31-final-gate/summary.json`

## 自动验收

| 门禁 | 结果 | 证据 |
|---|---|---|
| 完整本地 final gate | PASS：33/33，exit 0 | `a91461c5b-alpha31-final-gate/summary.json` |
| Core 测试 | PASS：1005 tests，0 failures/errors | final gate `19-test-core.log` |
| Desktop 测试 | PASS：10 tests，0 failures/errors | final gate `22-test-desktop.log` |
| iOS 测试 | PASS：11 tests，0 failures/errors | final gate `23-test-ios.log` |
| 远端 CI | PASS：`apple-java-build`、`java-build` | [GitHub Actions run 30137122827](https://github.com/leoyb1010/Leogame-one/actions/runs/30137122827) |
| 玩家旅程 E2E/CI | PASS：配装、搜刮、任务门、交火、撤离、结算、死亡损失进入自动门禁 | `a91461c5` |
| 五模式生命周期 | PASS | final gate step 20 |
| 模式/主题/Boss 矩阵 | PASS：25 个有效组合；25 成功结算、25 死亡结算 | final gate step 21 |
| 内容规模 | PASS：6 主题、18 枪械、9 普通敌人、3 精英、1 Boss、5 模式 | final gate/static gates |
| 原创图标与音频 | PASS：72 帧图标、79 个 mono 48 kHz PCM16 WAV | final gate steps 5、8、10、14、15 |
| 10k 地图 seed | PASS：10,000；exit 0 | final gate step 25 |
| 存档压力 | PASS：100 次；exit 0 | final gate step 26 |
| Apple 构建与包来源 | PASS：macOS、iOS Simulator、法律文件、来源完整性 | final gate steps 29–33 |
| 双端 30 分钟 | PASS：macOS 1850.970 秒；iOS 2072.118 秒 | Alpha 31 performance summary |
| iOS 原生音频 | PASS：原生 CoreAudio mixer/converter 活动，未进入 fallback | `iOS-audio.log` |

33 项自动 final gate 已全部通过，但它的 `PASS` 只覆盖自动化、Apple 构建、
包来源和现有 Simulator/macOS 性能证据，不自动勾选物理设备和人工验收。

## 双端墙钟 30 分钟结果

`render-frame-summary.json` 状态为 `passed`。阈值为每端至少 1800 秒、60 Hz
P95 不高于 18.4 ms、P99 不高于 33.3 ms、刷新预算超限率不高于 5%。

| 平台 | 活跃秒数 | 帧数 | Delivered FPS | P50 | P95 | P99 | 超刷新预算 | >33.3 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| macOS | 1850.970 | 216158 | 116.781 | 8.9 ms | 12.5 ms | 16.8 ms | 4 | 1 |
| iOS Simulator | 2072.118 | 124310 | 59.992 | 16.7 ms | 17.6 ms | 18.1 ms | 366 | 3 |

合计 340468 帧、3923.088 秒，最差单端 P95 为 17.6 ms。两端记录均绑定
`a91461c5`，且为连续活跃行动、无暂停/挂起/session discontinuity。

该指标是 `Gdx.graphics.getDeltaTime()` 的
`cpu-render-callback-frame-pacing`，不是硬件 GPU 计时。
`thermal-process-snapshot.txt` 只能证明抓取时没有系统 thermal/performance
warning，不能替代 Instruments/Metal GPU、温度或热降频报告。

## iOS 原生音频证据

Alpha 31 的 `iOS-audio.log` 记录了 `AUSpatialMixerV2` 构建、多个 input element、
48 kHz mono stream format 以及运行期 `AudioConverter` 活动。当前候选保持原生
音频链运行，未记录 Simulator audio fallback。该证据不替代物理 iPhone
扬声器、耳机、静音键和后台恢复听感验收。

## 当前玩家可见能力

- iOS 行动按钮采用图标主识别、小字辅助，并具有按压/禁用态和至少 22
  逻辑像素命中区。
- 首次启动包含 UI 缩放、震动和瞄准辅助三项校准。
- HUD 包含脱战 8 秒淡化、最多三路 500ms 受击方向弧和同源去重。
- 枪口、弹壳、曳光、火花、血雾、弹孔和爆炸使用固定容量场景对象池。
- 仓库具备六类筛选、搜索、多种排序、四级稀有度边框和价值比较。
- 18 枪、8 弹药、五模式、六主题、商店分层、首局枪弹兜底、13 敌人和能力标签
  已接入生产运行时。
- 玩家旅程、五模式生命周期和 25 个模式/主题/Boss 组合进入串行 final gate 与远端 CI。

## 仍未满足的最终要求

- 物理 iPhone 的安装、音频、旋转、后台恢复、温度和完整搜打撤路线。
- 实体手柄的移动、瞄准、射击、菜单焦点和完整玩家路线。
- Instruments/Metal 的硬件 GPU、draw call、显存、温度与热降频证据，以及
  60/120/144 Hz 对照。
- 双平台完整“新档→配装→搜索→档案→开门→交火→撤离→结算→回仓→
  再开→死亡损失”人工玩家路线录像。
- 原 Leogame-one 模式的创建、存档、继续人工回归。
- 三人盲测及六主题最终美术/整局体验签字。

## 当前签字

- [x] `PASS — ALPHA 31 SEALED SOURCE / PACKAGE / INSTALL`
- [x] `PASS — 33/33 AUTOMATED FINAL GATE`
- [x] `PASS — APPLE + LINUX REMOTE CI`
- [x] `PASS — 30-MIN macOS + iOS SIMULATOR RENDER-CALLBACK PACING`
- [ ] `PASS — COMPLETE PERSONAL SINGLE-PLAYER BUILD`
- [ ] `PASS — COMMERCIAL / APP STORE SCOPE`（不在当前个人单机交付范围）

证据生成日期：2026-07-25
