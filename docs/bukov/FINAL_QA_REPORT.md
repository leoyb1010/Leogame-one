# 《逃离布科夫》最终门禁前 QA 状态

当前状态仍为 **`PENDING EVIDENCE`，不是最终发布签字**。

本报告刻意区分两个源码身份：

- 当前已安装、完成双端墙钟 30 分钟验证的候选是
  `248b811a7c0575c1ffed7bc073da0179e1538c4c`，版本
  `2.0.0-alpha30-ios-ui-audio`。
- 最新已完成自动回归的开发代码 SHA 是
  `fdbfbc06b3963f161732c9dd7b59a5dc3388ca4b`，新增完整玩家旅程
  E2E/CI 门禁，但尚未重新打包。

因此，现有证据可以证明 Alpha 30 安装候选的双端启动、原生音频和连续帧 pacing，
也可以证明最新已测开发代码的全量 Java 自动回归；它们还不能合并成“同一最终 SHA
全部通过”。物理 iOS 设备、实体手柄、硬件 GPU、三人盲测仍为 `NOT RUN`。

## 构建身份

| 字段 | 值 |
|---|---|
| 产品 | 逃离布科夫 / Escape from Bukov |
| 当前安装候选 | `2.0.0-alpha30-ios-ui-audio` |
| 安装候选 Source commit | `248b811a7c0575c1ffed7bc073da0179e1538c4c` |
| 最新已测开发代码 SHA | `fdbfbc06b3963f161732c9dd7b59a5dc3388ca4b` |
| 分支 | `codex/bukov-realtime` |
| 日期 | `2026-07-25 / Asia/Shanghai` |
| macOS archive SHA-256 | `a95ecf784acbca2a87460ee9646b5475a1ee37a0a3011178fb118e639f4c215c` |
| macOS 主程序 SHA-256 | `707f424b7c6cd63c75a369ccf40e39b2f81e011868b25b4d35c4fcf4664b8dec` |
| iOS Simulator archive SHA-256 | `b3193f8767903098a7b6923322890e17af5d3ec68652e2c4fda73b9908513ac4` |
| iOS Simulator 主程序 SHA-256 | `ea76394fe0ab85afb5f91513a0d7d39cbf788bee208b150a94bb7ccfa6308926` |

产物与安装回执：

- macOS：
  `/Users/leoyuan/Documents/日常/output/逃离布科夫-alpha30-ios-ui-audio-248b811a7/逃离布科夫-macOS-alpha30-ios-ui-audio-248b811a7.zip`
- iOS Simulator：
  `/Users/leoyuan/Documents/日常/output/逃离布科夫-alpha30-ios-ui-audio-248b811a7/逃离布科夫-iOS-Simulator-alpha30-ios-ui-audio-248b811a7.zip`
- 安装回执：
  `/Users/leoyuan/Library/Logs/EscapeFromBukov/install-receipts/20260724T234007Z-248b811a7c05-41185.txt`
- 双端性能与音频证据：
  `/Users/leoyuan/Documents/日常/output/evidence/248b811a7-performance`

## 自动验收

| 门禁 | 结果 | 证据身份 |
|---|---|---|
| Core 测试 | PASS：1005 tests，0 failures/errors | 已测开发代码 `fdbfbc06b` |
| Desktop 测试 | PASS：10 tests，0 failures/errors | 已测开发代码 `fdbfbc06b` |
| iOS 测试 | PASS：11 tests，0 failures/errors | 已测开发代码 `fdbfbc06b` |
| 玩家旅程 E2E/CI | PASS：配装、搜刮、任务门、交火、撤离、结算、死亡损失等生产合同进入自动门禁 | 已测开发代码 `fdbfbc06b` |
| 内容规模 | PASS：6 主题、18 枪械、9 普通敌人、3 精英、1 Boss、5 模式 | 当前仓库 |
| 原创图标与音频 | PASS：72 帧图标、79 个 mono 48 kHz PCM16 WAV | 当前仓库 |
| 10k 地图 seed | PASS：10,000；exit 0 | 安装候选 `248b811a7`，`build/reports/bukov-seed-sweep.log` |
| 存档压力 | PASS：100 次；exit 0 | 安装候选 `248b811a7`，`build/reports/bukov-save-stress.log` |
| 双端 30 分钟 | PASS：macOS 1820.854 秒；iOS 1831.635 秒 | 安装候选 `248b811a7` |
| iOS 原生音频 | PASS：CoreAudio 建立 48 kHz mono spatial mixer 输入，并有运行期 AudioConverter 活动 | 安装候选 `248b811a7` |
| 法律文件 | PASS：GPL 与第三方 notices 已进入 Apple 包 | 安装候选 `248b811a7` |

已测开发代码的完整测试结果不能替代 Apple 包；Alpha 30 的平台结果也不能证明
`fdbfbc06b` 已经打包。最终签字必须在后续同一 SHA 上重打并复核。

## 双端墙钟 30 分钟结果

`render-frame-summary.json` 的 gate 状态为 `passed`，阈值为每端至少 1800 秒、
60 Hz P95 不高于 18.4 ms、P99 不高于 33.3 ms、刷新预算超限率不高于 5%。

| 平台 | 活跃秒数 | Delivered FPS | P50 | P95 | P99 | 超刷新预算 | >33.3 ms |
|---|---:|---:|---:|---:|---:|---:|---:|
| macOS | 1820.854 | 115.629 | 9.7 ms | 13.2 ms | 16.8 ms | 3 / 210543 | 1 |
| iOS Simulator | 1831.635 | 59.995 | 16.7 ms | 17.8 ms | 18.1 ms | 322 / 109889 | 1 |

两端均为连续活跃行动、无暂停/挂起/尺寸切换或 session discontinuity。该指标是
`Gdx.graphics.getDeltaTime()` 的 `cpu-render-callback-frame-pacing`，不是硬件 GPU
计时。`thermal-process-snapshot.txt` 没有记录系统 thermal/performance warning，
但它不是 Instruments/Metal 热分析。

## 当前玩家可见能力

- iOS 行动按钮已改为图标主识别、小字辅助，并具有按压/禁用态和至少 22
  逻辑像素命中区；基地、商店、服务、背包、结算、暂停及首次校准入口沿用同一图标语言。
- 首次启动已有 UI 缩放、震动和瞄准辅助三项校准。
- HUD 已有脱战 8 秒淡化、最多三路 500ms 受击方向弧与同源去重。
- 枪口、弹壳、曳光、火花、血雾、弹孔和爆炸使用固定容量场景对象池。
- 仓库已有六类筛选、搜索、多种排序、四级稀有度边框和同类价值比较。
- 城防-556 换弹规格为 1.4 秒；18 枪、8 弹药进入同一射击、换弹与伤害链。
- 五模式、六主题、商店分层解锁、首局枪弹兜底、13 敌人及其能力标签均有运行时策略。
- iOS Simulator 的原生 CoreAudio mixer 和转换器在 30 分钟候选进程内有实际活动记录。

## 仍未满足的最终要求

- 将 `fdbfbc06b` 之后的最终交付代码重新打成 macOS/iOS 候选，使自动回归与平台
  证据绑定同一 SHA。
- 录制双平台完整“新档→配装→搜索→档案→开门→交火→撤离→结算→回仓→
  再开→死亡损失”玩家录像，并对照 E2E 合同人工签字。
- 完成六主题最终独立图集、地表材质、可交互剪影和整局视觉品质验收。
- 完成物理 iOS 真机、实体手柄、60/120/144 Hz 对照、Instruments/Metal
  硬件 GPU/显存/热降频与三人盲测。
- 完成原 Leogame-one 模式的创建、存档、继续人工回归。

## 当前签字

- [x] `PASS — ALPHA 30 PACKAGE / INSTALL / 30-MIN macOS+iOS SIMULATOR PACING`
- [x] `PASS — fdbfbc06b AUTOMATION AND PLAYER-JOURNEY E2E`
- [ ] `PASS — COMPLETE PERSONAL SINGLE-PLAYER BUILD`
- [ ] `PASS — COMMERCIAL / APP STORE SCOPE`（不在当前个人单机交付范围）

证据生成日期：2026-07-25
