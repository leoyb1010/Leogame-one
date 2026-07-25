# 逃离布科夫 v2.0 实现与验收矩阵

更新时间：2026-07-25

计划基准：`逃离布科夫_Leogame-one实时搜打撤_完整开发执行计划书_v2.0.md`

宿主基线：`99a084d72fc4b399117be0369ccc2bdd41151879`

当前分支：`codex/bukov-realtime`

## 当前双身份快照

当前不能把“已安装候选”和“最新已测开发代码”混写成同一个发布物：

| 身份 | SHA / 版本 | 已有证据 | 边界 |
|---|---|---|---|
| Apple 安装候选 | `248b811a7c0575c1ffed7bc073da0179e1538c4c` / `2.0.0-alpha30-ios-ui-audio` | macOS/iOS archive 与主程序哈希、安装回执、双端连续 30 分钟、iOS 原生音频、10k seed、100 次存档 | 不包含后续 E2E/CI 提交 |
| 最新已测开发代码 | `fdbfbc06b3963f161732c9dd7b59a5dc3388ca4b` | core 1005、desktop 10、iOS 11，零失败；玩家旅程 E2E/CI | 尚未重新打包和平台复核 |

最终签字要求后续把两种身份收敛为同一个重打 SHA。机器清单以
`RELEASE_MANIFEST.json` 为准，人工结果以 `FINAL_QA_REPORT.md` 为准。

## 当前发行候选快照

| 项目 | 当前代码/资产事实 | 当前证据与剩余边界 |
|---|---|---|
| 地图主题 | 6 个注册主题；首关 Figure-eight 语义图、三路线、三撤离 | 10,000 seed 在 `248b811a7` 通过；最终主题美术和十局人工体验待签字 |
| 枪械与弹药 | 18 枪、8 弹药；城防-556 换弹 1.4 秒 | 射击生产合同自动覆盖；实体手柄逐枪验收待补 |
| 敌人 | 13 个：9 普通、3 精英、1 Boss；18 个能力标签有运行策略 | 生成、攻击、受击、死亡合同已有自动回归；完整 Boss 实机录像待补 |
| Raid 模式 | 5 个：远征、快速清扫、拾荒者、Boss 合同、演练场 | 模式生命周期和地图策略有自动测试；五模式逐局人工签字待补 |
| 局外循环 | 仓库、完整配装、商店买卖/解锁、保险、合同和幂等结算已接入 | 玩家旅程 E2E 已接入 `fdbfbc06b`；双平台同 SHA 完整录像待补 |
| 原创资产 | 72 帧物品/交互图标，79 个 PCM WAV | 发行清单与来源台账通过；最终主题美术品质待验收 |
| iOS UI/音频 | 图标主识别、小字辅助、按压/禁用态、首次三项校准；原生 CoreAudio 活动可见 | Alpha 30 Simulator 实际运行通过；物理 iOS 设备 `NOT RUN` |
| 存档/性能 | checkpoint、幂等结算、10k seed、100 次 save gate；双端墙钟 30 分钟 | Alpha 30 帧 pacing gate 通过；硬件 GPU/热分析及最新已测开发代码重打待补 |

## 状态口径

| 标记 | 含义 |
|---|---|
| ✅ | 实现已接入，且存在绑定明确 SHA 的自动或平台证据 |
| 🟡 | 已有实现或阶段证据，但缺少最终同 SHA 包、人工或设备签字 |
| ❌ | 计划要求的能力尚未实现 |
| ⚪ | 只缺可复核证据，不能据此宣称验收通过 |

## Gate 总表

| Gate | 已完成并可核验 | 未完成 / 未签字 | 验收命令 | 当前判定 |
|---|---|---|---|---|
| 0 环境与保护 | 基线、分支、来源台账；`fdbfbc06b` 为 core 1005、desktop 10、iOS 11，零失败；玩家旅程 E2E/CI 已接入 | 原 Leogame-one 模式在最终改动后的创建、存档、继续人工录像；最新已测开发代码尚未重打 | `git status --short --branch`；`git diff --check`；完整测试套件 | 🟡 |
| 1 实时移动 | 120 Hz fixed-step、碰撞、键鼠/触控输入、镜头跟随、窗口化和整数像素对齐已接入；Alpha 30 双端活跃行动分别 1820.854 / 1831.635 秒 | 实体手柄 20 分钟；60/120/144 Hz 同 seed 对照；最新 SHA 平台复核 | runtime 测试；人工输入/刷新率矩阵 | 🟡 |
| 2 枪战 | 18 枪、8 弹药、hitscan、弹药守恒、换弹、伤害、护甲、医疗、自伤/友伤隔离、13 敌人与表现池已接入；城防-556 为 1.4 秒 | 实体手柄逐枪交火、所有精英/Boss 的双平台完整反馈录像 | combat/AI 测试；实机逐枪射速/换弹/墙阻/敌人闭环 | 🟡 |
| 3 搜打撤 | profile/raid 存档、临时文件+备份、UID、幂等结算、仓库/配装/商店、容器、任务档案、门、E01/E02/E03 和死亡损失已接入；100 次存档 gate 通过 | 双平台同一最终 SHA 的整局录像；手柄焦点和真实强退恢复签字 | `./scripts/bukov_save_stress.sh 100`；玩家旅程 E2E；人工整局 | 🟡 |
| 4 第一关 | 26–34 房、三路线、三撤离、首局枪弹兜底、地面物资、任务门、18 枪、8 弹药、3 护甲、2 背包、7 医疗、13 敌人和长期合同链均有实现；10k seed gate 通过 | 10 局留存、15–25 分钟整局节奏、六主题视觉差异人工验收 | `./scripts/bukov_seed_sweep.sh 10000`；固定回归种子；十局体验表 | 🟡 |
| 5 体验与表现 | 品牌/App 图标、8 向动作、UI tokens、脱战 8 秒淡化、三路 500ms 受击弧、七类 VFX 固定池、首次三项校准、仓库筛选/搜索/排序、79 SFX、iOS 图标按钮和原生音频均已接入 | 最终 UI/主题美术包、物理设备、三人盲测及计划第 82 节人工签字 | UI/资产/音频 gate；UX 检查单 | 🟡 |
| 6 性能与交付 | Alpha 30 双端真实打包应用连续 30 分钟 gate 通过：macOS P95 13.2ms，iOS P95 17.8ms，60 Hz 阈值 18.4ms；archive/binary SHA 和安装回执齐全 | 最新已测开发代码 `fdbfbc06b` 尚未重打；硬件 GPU、Metal、显存、真机温度/热降频和全新机器离线恢复未验收 | render-frame gate；Instruments/Metal；最终 SHA 重打和双端验收 | 🟡 |
| 1.0 全内容 | 6 主题、18 枪、13 敌人、5 模式、保险、合同、商店进度和长期经济均已实现 | 最终内容丰富度、主题美术、整局平衡和同 SHA 双平台完成签字 | 计划第 68/82 节最终定义 | 🟡 |

## 当前自动与平台证据

| 证据 | 可证明什么 | 不能证明什么 |
|---|---|---|
| Gradle 测试结果：core 1005、desktop 10、iOS 11，零失败 | `fdbfbc06b` 已测开发代码的 Java 自动回归和玩家旅程合同通过 | 不等于该代码已进入 Apple 包 |
| `build/reports/bukov-seed-sweep.log` | `248b811a7` 的 10,000 seed，clean worktree，exit 0 | 不替代十局人工节奏体验 |
| `build/reports/bukov-save-stress.log` | `248b811a7` 的 100 次 checkpoint/resume/settlement，clean worktree，exit 0 | 不是操作系统强杀进程 |
| `render-frame-summary.json` | `248b811a7` macOS 1820.854 秒、iOS 1831.635 秒连续活跃游戏，双端 gate passed | 是 render-callback pacing，不是硬件 GPU counter |
| `iOS-audio.log` | Alpha 30 iOS 进程实际创建 48 kHz mono spatial mixer 输入并有运行期 converter 活动 | 不证明物理扬声器、耳机或真机听感 |
| Alpha 30 archive、`PACKAGE_INFO.txt`、`SHA256SUMS.txt` 与安装 receipt | 包源码身份、archive/binary 哈希、双端安装和启动一致 | 不包含 `fdbfbc06b` 之后的 E2E/CI |
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
| HUD、触控与 UI 令牌 | `BukovRaidHud.java`、`BukovTouchControls.java`、`WndBukovHub.java`、`ui_tokens.json` | `bukov/ui/*Test.java` |
| 表现、音频与体验合同 | `bukov/fx/`、`bukov/audio/`、`experience_contract.json` | `bukov/fx/*Test.java`、`bukov/audio/*Test.java` |
| 玩家旅程门禁 | `BukovRaidSession`、`BukovRealtimeWorld`、部署/结算场景 | `bukov/e2e/*Test.java` 及 CI gate |

## 最终统一验收命令

下列命令必须在准备发布的同一工作树、同一 SHA 上执行并保存日志。本次文档更新
不运行 Gradle，也不把已测开发代码的自动测试冒充 Alpha 30 二进制身份。

```bash
git diff --check
./scripts/apple-gradle core:test desktop:test ios:test --no-daemon
./scripts/bukov_ui_tokens_check.sh
./scripts/bukov_seed_sweep.sh 10000
./scripts/bukov_save_stress.sh 100
./scripts/bukov_performance_smoke.sh 1800
./scripts/bukov_performance_e2e.sh 1800
./scripts/bukov_robovm_api_gate.py

scripts/bukov_final_gate.sh --apply \
  --render-frame-log /absolute/path/macOS-render.log \
  --render-frame-log /absolute/path/iOS-render.log
```

## 必须补齐的人工验收矩阵

| 场景 | macOS | iOS 模拟器 | 手柄 | 通过标准 |
|---|---|---|---|---|
| 冷启动→仓库→配装→出击 | Alpha 30 部分通过 | Alpha 30 部分通过 | `NOT RUN` | 无崩溃、焦点不丢、配装持久化 |
| 移动→瞄准→射击→换弹→击杀 | Alpha 30 部分通过 | Alpha 30 部分通过 | `NOT RUN` | 无滑行/穿墙，射速不随帧率，敌人反馈完整 |
| 搜 3 个容器→拾取/丢弃 | 自动 E2E 通过，人工待签 | 自动 E2E 通过，人工待签 | `NOT RUN` | 搜索可打断，UID/重量/价值一致 |
| E01/E02/E03→结算→回仓库 | 自动 E2E 通过，人工待签 | 自动 E2E 通过，人工待签 | `NOT RUN` | 条件、倒计时、打断、幂等正确 |
| 中途退出→继续→异常退出恢复 | 历史部分通过，须最终 SHA 重录 | 待签字 | 不适用 | seed、弹药、计时、容器、撤离状态一致 |
| 原 Leogame-one 模式回归 | 待签字 | 待签字 | `NOT RUN` | 创建、存档、继续不受 Bukov 分支破坏 |
| 30 分钟活跃游戏 | Alpha 30 PASS | Alpha 30 PASS | `NOT RUN` | render-callback gate 达标，无崩溃/断点 |
| 硬件 GPU / 真机热稳定 | 待 Instruments | 物理设备 `NOT RUN` | 不适用 | GPU/显存/温度/热降频有原始报告 |

## 退出结论

当前不是“全部开发完”。已经有真实可安装的 Alpha 30、双端 30 分钟证据、
10k/100 长门禁以及最新玩家旅程 E2E；阻断最终签字的事项是：

1. 最新已测开发代码尚未重打，自动回归和平台包不是同一 SHA。
2. 双平台完整玩家录像、原模式回归、实体手柄和物理 iOS 设备未签字。
3. 硬件 GPU/Metal/显存/热稳定与 60/120/144 Hz 对照未完成。
4. 六主题最终美术差异、整局平衡和三人盲测未完成。
