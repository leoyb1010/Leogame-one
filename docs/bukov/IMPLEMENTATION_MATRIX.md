# 逃离布科夫 v2.0 实现与验收矩阵

更新时间：2026-07-25

计划基准：`逃离布科夫_Leogame-one实时搜打撤_完整开发执行计划书_v2.0.md`

宿主基线：`99a084d72fc4b399117be0369ccc2bdd41151879`

当前分支：`codex/bukov-realtime`

## Alpha 32 开发候选（未封存）

当前工作树已经补入真实命中分区、玩家/敌人统一护甲链、换弹取消、Boss
六帧阶段过渡及 200 ms 表现慢动作、枪械热量/污损/耐久运行态、按键绑定
入口、移动端 HUD/镜头/触控反馈修复、玩家射击空间桶索引，以及硬地/水面/
金属三类脚步声路由和声学差异门禁；同时补入敌人巡逻/搜索闭环、UI 音效
并发预算、World 三层枪声与脚步调度、进程级崩溃恢复，以及真实路线/交火/
承伤/结算的本地平衡采集和仓库窗口的令牌化非阻塞入场动效。完整变更和未完成边界见
`docs/bukov/ALPHA32_CHANGELOG_ZH.md`。

本节在生成干净提交、同 SHA Final Gate、双端包和人工玩家路线证据前，不覆盖
下方 Alpha 31 的封存身份，也不宣称 Alpha 32 已发布。

## Alpha 31 封存发行快照

| 身份 | SHA / 版本 | 已有证据 | 边界 |
|---|---|---|---|
| 封存发行源码 | `a91461c5ba57aedb76f07acd741b749100425fb2` | clean source seal、33/33 本地 final gate、远端 Apple/Linux CI | 本文档提交可晚于封存源码，不改变包身份 |
| Apple 安装候选 | `2.0.0-alpha31-e2e-ci` | macOS/iOS archive、主程序哈希、安装回执、原生音频、双端 30 分钟 | 物理 iPhone、实体手柄、硬件 GPU 未执行 |

Alpha 31 已经把此前“开发 HEAD”和“安装候选”收敛到同一源码 SHA。机器清单以
`RELEASE_MANIFEST.json` 为准，完整自动门禁以
`/Users/leoyuan/Documents/日常/output/evidence/a91461c5b-alpha31-final-gate/summary.json`
为准。

## 当前内容与平台事实

| 项目 | 当前代码/资产事实 | 当前证据与剩余边界 |
|---|---|---|
| 地图主题 | 6 个注册主题；首关 Figure-eight 语义图、三路线、三撤离 | 10,000 seed 同 SHA 通过；最终主题美术和十局人工体验待签字 |
| 枪械与弹药 | 18 枪、8 弹药；统一射击/换弹/伤害链 | 自动玩家旅程通过；实体手柄逐枪验收待补 |
| 敌人 | 13 个：9 普通、3 精英、1 Boss；能力标签有运行策略 | AI/交火自动门禁通过；完整 Boss 人工录像待补 |
| Raid 模式 | 5 个；远征、快速清扫、拾荒者、Boss 合同、演练场 | 生命周期通过；25 个有效模式/主题/Boss 组合通过 |
| 局外循环 | 仓库、配装、商店买卖/解锁、保险、合同和幂等结算已接入 | 玩家旅程 E2E 同 SHA 通过；双平台人工路线录像待补 |
| 原创资产 | 72 帧物品/交互图标，79 个 PCM WAV | 静态资产、跨平台生成一致性、来源与包内法律门禁通过 |
| iOS UI/音频 | 图标主识别、小字辅助、按压/禁用态、首次三项校准；原生 CoreAudio 无 fallback | Simulator 同 SHA 实际运行通过；物理 iPhone `NOT RUN` |
| 存档/性能 | checkpoint、幂等结算、10k seed、100 save、四进程强制崩溃恢复、双端墙钟 30 分钟 | JVM 崩溃只证明进程级恢复，不替代物理断电/fsync；硬件 GPU/Instruments 和真机热分析待补 |
| 本地平衡 | checkpoint/结算保存真实模式、主题、唯一完整路线、搜索、交火、击杀、HP 承伤、时长、价值与结束类型；10 seed 报告门禁 | 尚无 10 局真实玩家样本，当前不能宣称节奏与经济平衡通过 |
| CI/交付 | 本地 final gate 33/33；远端 `apple-java-build` 与 `java-build` 成功 | 人工/物理门禁不由 CI 自动签字 |

## 状态口径

| 标记 | 含义 |
|---|---|
| ✅ | 实现已接入，且存在绑定封存 SHA 的自动或平台证据 |
| 🟡 | 已有实现和阶段证据，但缺少人工、物理设备或最终体验签字 |
| ❌ | 计划要求的能力尚未实现 |
| ⚪ | 只缺可复核证据，不能据此宣称验收通过 |

## Gate 总表

| Gate | 已完成并可核验 | 未完成 / 未签字 | 当前判定 |
|---|---|---|---|
| 0 环境与保护 | `a91461c5` clean source；本地 final gate 33/33；core 1005、desktop 10、iOS 11；远端 Apple/Linux CI 全绿 | 原 Leogame-one 模式创建/存档/继续人工录像 | 🟡 |
| 1 实时移动 | 120 Hz fixed-step、碰撞、键鼠/触控、镜头跟随、窗口化和像素对齐已接入；同 SHA 双端连续活跃行动 1850.970 / 2072.118 秒 | 实体手柄 20 分钟；60/120/144 Hz 同 seed 人工对照 | 🟡 |
| 2 枪战 | 18 枪、8 弹药、hitscan、弹药守恒、换弹、伤害、护甲、医疗、自伤/友伤隔离、13 敌人与表现池已接入 | 实体手柄逐枪交火、精英/Boss 双平台完整反馈录像 | 🟡 |
| 3 搜打撤 | profile/raid 存档、临时文件+备份、UID、幂等结算、仓库/配装/商店、容器、任务档案、门、三撤离和死亡损失已接入；100 次存档、玩家旅程及四进程强制崩溃 gate 通过 | 双平台完整人工路线、物理断电恢复和手柄焦点签字 | 🟡 |
| 4 第一关 | 26–34 房、三路线、三撤离、首局枪弹兜底、任务门、地面物资、装备/医疗/敌人/合同链均有实现；10k seed 通过 | 十局留存、15–25 分钟整局节奏、六主题视觉差异人工验收 | 🟡 |
| 5 体验与表现 | 品牌、App 图标、8 向动作、UI tokens、HUD 淡化/受击弧、七类 VFX 池、首次校准、仓库搜索排序、79 SFX、iOS 图标按钮及原生音频均接入 | 最终 UI/主题美术包、物理 iPhone、三人盲测及第 82 节人工签字 | 🟡 |
| 6 性能与交付 | 同 SHA Apple 包、安装回执、包来源；macOS P95 12.5ms，iOS P95 17.6ms，60 Hz 阈值 18.4ms；33/33 final gate 和远端 CI 通过 | 硬件 GPU、Metal、显存、真机温度/热降频、全新机器离线恢复 | 🟡 |
| 1.0 全内容 | 6 主题、18 枪、13 敌人、5 模式、保险、合同、商店进度和长期经济均已实现 | 最终内容丰富度、主题美术、整局平衡和人工完成签字 | 🟡 |

## Alpha 31 自动与平台证据

| 证据 | 可证明什么 | 不能证明什么 |
|---|---|---|
| `a91461c5b-alpha31-final-gate/summary.json` | 同 SHA 串行 33/33：静态门禁、全量测试、五模式、25 组合、10k、100 save、性能模拟、Apple 构建、法律/来源和 source integrity | 不自动证明物理设备或人工体验 |
| [GitHub Actions run 30137122827](https://github.com/leoyb1010/Leogame-one/actions/runs/30137122827) | 同 SHA `apple-java-build` 和 `java-build` 两个 job 成功 | 不包含本机 Simulator 人工路线 |
| `a91461c5b-alpha31-performance/render-frame-summary.json` | macOS 1850.970 秒、iOS 2072.118 秒连续活跃游戏，合计 340468 帧，gate passed | 是 CPU render-callback pacing，不是硬件 GPU counter |
| `a91461c5b-alpha31-performance/iOS-audio.log` | iOS 进程实际运行原生 CoreAudio mixer/converter，未进入 fallback | 不证明物理扬声器、耳机、静音键或后台听感 |
| Alpha 31 `PACKAGE_INFO.txt`、`SHA256SUMS.txt` 与安装 receipt | 封存 SHA、archive/binary 哈希、双端安装和启动一致 | 不替代完整人工玩家路线 |
| `mode-theme-boss-matrix/summary.json` | 25 个有效组合、25 成功结算、25 死亡结算、12 个 Boss-enabled rows | 不证明每种组合的人工乐趣与美术品质 |
| `docs/SOURCE_PROVENANCE.csv`、`docs/THIRD_PARTY_BORROWING.md` | 当前代码/素材来源与“仅参考”边界已登记 | 最终发布页源码提供方式仍需签字 |

## 关键实现证据索引

| 能力 | 实现入口 | 自动测试入口 |
|---|---|---|
| 120 Hz 实时层 | `bukov/runtime/`、`GameScene.java`、`Char.java`、`CharSprite.java` | `bukov/runtime/*Test.java` |
| 枪械与弹药 | `bukov/combat/`、`assets/bukov/content/firearms.json`、`ammunition.json` | `bukov/combat/**/*Test.java` |
| 敌人、精英、Boss | `bukov/ai/`、`assets/bukov/content/enemies.json`、`BukovRealtimeWorld.java` | `bukov/ai/*Test.java` |
| 地图与撤离 | `bukov/levels/`、`bukov/map/`、`Dungeon.java` | `bukov/levels/*Test.java`、`bukov/map/*Test.java` |
| 搜刮、仓库、结算 | `bukov/raid/`、`bukov/content/`、`WndBukovHub.java` | `bukov/raid/*Test.java`、`bukov/content/*Test.java` |
| 文件存档 | `bukov/save/` | `bukov/save/*Test.java` |
| 本地平衡与路线证据 | `RaidBalanceTelemetry.java`、`BukovBalanceReport.java`、`RaidSession.java`、`BukovRealtimeWorld.java` | `RaidBalanceTelemetryTest`、`BukovBalanceReportTest`、`BukovBalanceRuntimeWiringTest` |
| HUD、触控与 UI 令牌 | `BukovRaidHud.java`、`BukovTouchControls.java`、`WndBukovHub.java`、`ui_tokens.json` | `bukov/ui/*Test.java` |
| 表现、音频与体验合同 | `bukov/fx/`、`bukov/audio/`、`experience_contract.json` | `bukov/fx/*Test.java`、`bukov/audio/*Test.java` |
| 玩家旅程门禁 | `BukovRaidSession`、`BukovRealtimeWorld`、部署/结算场景 | `BukovPlayerJourneyAcceptanceTest`、CI |

## 最终统一验收命令

Alpha 31 已执行完整串行门禁并保存 33 个步骤的独立日志。本次文档更新不重跑
Gradle。下一候选仍需在准备发布的同一工作树、同一 SHA 上执行：

```bash
scripts/bukov_final_gate.sh --apply \
  --render-frame-log /absolute/path/macOS-render.log \
  --render-frame-log /absolute/path/iOS-render.log
```

## 必须补齐的人工验收矩阵

| 场景 | macOS | iOS Simulator | 实体手柄/真机 | 通过标准 |
|---|---|---|---|---|
| 冷启动→仓库→配装→出击 | Alpha 31 部分通过 | Alpha 31 部分通过 | `NOT RUN` | 无崩溃、焦点不丢、配装持久化 |
| 移动→瞄准→射击→换弹→击杀 | Alpha 31 部分通过 | Alpha 31 部分通过 | `NOT RUN` | 无滑行/穿墙，射速不随帧率，敌人反馈完整 |
| 搜 3 容器→任务门→三撤离→结算 | 自动 E2E 通过，人工待签 | 自动 E2E 通过，人工待签 | `NOT RUN` | 条件、打断、幂等和状态一致 |
| 中途退出→继续→异常退出恢复 | 历史部分通过，须录像 | 待签字 | 真机 `NOT RUN` | seed、弹药、计时、容器、撤离状态一致 |
| 原 Leogame-one 模式回归 | 待签字 | 待签字 | `NOT RUN` | 创建、存档、继续不受 Bukov 分支破坏 |
| 30 分钟活跃游戏 | Alpha 31 PASS | Alpha 31 PASS | 真机 `NOT RUN` | render-callback gate 达标，无崩溃/断点 |
| 硬件 GPU / 热稳定 | 待 Instruments | Simulator 非硬件证据 | 真机 `NOT RUN` | GPU/显存/温度/热降频有原始报告 |

## 退出结论

Alpha 31 已完成同一封存 SHA 的包、安装、双端 30 分钟、33/33 本地 final gate
和 Apple/Linux 远端 CI。它仍不是“个人单机版 100% 最终签字”，剩余阻断为：

1. 物理 iPhone 和实体手柄未验收。
2. 硬件 GPU/Instruments、温度/热降频及 60/120/144 Hz 对照未完成。
3. 双平台完整人工玩家路线、原模式回归和三人盲测未签字。
4. 六主题最终美术差异、整局平衡和体验品质仍需人工验收。
