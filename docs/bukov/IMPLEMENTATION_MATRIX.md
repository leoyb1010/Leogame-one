# 逃离布科夫 v2.0 实现与验收矩阵

更新时间：2026-07-24

计划基准：`逃离布科夫_Leogame-one实时搜打撤_完整开发执行计划书_v2.0.md`

宿主基线：`99a084d72fc4b399117be0369ccc2bdd41151879`

当前分支：`codex/bukov-realtime`

## 当前发行候选快照

以下事实由 `scripts/bukov_release_manifest_check.py` 直接对照仓库内容校验，
优先于本文件下方保留的 2026-07-23 历史测试数字：

| 项目 | 当前代码/资产事实 | 最终平台证据 |
|---|---|---|
| 地图主题 | 6 个注册主题 | `PENDING EVIDENCE` |
| 枪械与弹药 | 18 枪、8 弹药，按注册表口径配装 | `PENDING EVIDENCE` |
| 敌人 | 13 个：9 普通、3 精英、1 Boss | `PENDING EVIDENCE` |
| Raid 模式 | 4 个：远征、快速清扫、拾荒者、Boss 合同 | `PENDING EVIDENCE` |
| 局外循环 | 仓库、配装、商店买卖与幂等交易已接入 | `PENDING EVIDENCE` |
| 原创资产 | 72 帧物品/交互图标，19 个 PCM WAV | 台账静态通过；最终包复核待证据 |
| 平台 | macOS 与 iOS 源码/构建入口存在；120 Hz 固定模拟与高刷新配置存在 | 最终 SHA 的 macOS/iOS 实机结果待证据 |
| 存档/性能 | checkpoint、幂等结算、seed/save/performance 门禁存在 | 10k/100 次/30 分钟最终运行待证据 |

机器状态以 `RELEASE_MANIFEST.json` 为准；人工结果使用
`FINAL_QA_REPORT_TEMPLATE.md`。本次文档整理没有运行 Gradle，也没有把历史
macOS 截图或旧 iOS 编译记录升级成最终验收结论。

## 状态口径

| 标记 | 含义 |
|---|---|
| ✅ | 代码已接入宿主，且当前工作树留有对应自动测试结果 |
| 🟡 | 已有实现，但缺少计划要求的内容或人工/平台验收 |
| ❌ | 计划要求的能力尚未实现 |
| ⚪ | 只缺可复核证据，不能据此宣称验收通过 |

本表区分“代码存在”“自动测试通过”和“玩家实际验收”。当前所有 Bukov
改动仍在未提交工作树中（44 个已修改、12 个未跟踪路径）；在形成唯一提交
SHA 前，任何截图和报告都只能绑定到本地快照，不能视为最终交付证据。

## Gate 总表

| Gate | 已完成并可核验 | 未完成 / 未签字 | 验收命令 | 当前判定 |
|---|---|---|---|---|
| 0 环境与保护 | 基线、分支、来源台账；当前 XML 报告为 core 238/238、desktop 2/2；`git diff --check` 通过 | 当前改动未提交；原 Leogame-one 模式在最终改动后的创建、存档、继续回归未留证 | `git status --short --branch`；`git diff --check`；`./scripts/apple-gradle core:test desktop:test desktop:build ios:compileJava --no-daemon` | 🟡 |
| 1 实时移动 | `FixedStepClock`、`RealtimeBody`、`GridCollision`、`RealtimeInput`、浮点精灵定位和 `GameScene` 模式分支已接入；runtime 测试存在且当前通过 | 键鼠/主流手柄各 20 分钟；60/120/144 Hz 等距；背包暂停；15 分钟无死锁均未留当前证据 | 完整测试命令；再按“人工验收矩阵”逐项录制 | 🟡 |
| 2 枪战 | 6 枪、8 种弹药定义；FireControl、hitscan、伤害、换弹、弹种守恒、近战/远程敌人和宿主护甲路径已接入；枪口/曳光/命中事件接入 `GameScene` | 当前 VFX 仍复用火花占位；枪声复用宿主音效；城防-556 当前换弹为 2.35s，与计划 1.4s 不一致；完整手柄交火、声音/震动三层反馈未签字 | `./scripts/apple-gradle core:test --tests '*bukov.combat*' --tests '*bukov.ai*' --no-daemon`；实机逐枪射速/换弹/墙阻/敌人闭环 | 🟡 |
| 3 搜打撤 | 独立 profile/raid 存档、临时文件+备份、断点恢复、UID、幂等结算、Heap 适配、仓库/配装窗口、可搜索容器、E01/E02/E03 撤离和死亡/成功结算已接入宿主 | “配装→搜 3 箱→交火→撤离→结算→仓库→死亡损失”尚无一次完整录像；手柄焦点全流程未签字；当前完整测试只按默认值执行 10 次压力循环 | `./scripts/bukov_save_stress.sh 100`；完整局人工录制；重复结算/异常退出复核 | 🟡 |
| 4 第一关 | 程序化 Figure-eight 语义图与宿主 `BukovLevel`；26–34 个内容房、稳定语义锚点、三路线、三撤离；6 枪、8 弹药定义、4 普通+1 精英+1 Boss、至少 30 个战利品条目 | 3 套护甲、2 个背包、完整合同链未发现；敌人与 Boss 使用宿主占位精灵；当前完整测试只扫默认 20+固定 20 个种子，未保存本快照 10k 报告；10 局留存和 15–25 分钟整局未验收 | `./scripts/bukov_seed_sweep.sh 10000`；20 个固定回归种子；10 局人工体验表 | 🟡 |
| 5 体验与表现 | 中英文品牌与 App 图标；`ui_tokens.json`；紧凑 HUD；仓库/配装/最近结算窗口；焦点模型；池化战斗事件；空间音频、关键声音可视化和表现零侵入模型均有测试 | UI/场景仍大量复用宿主美术；角色没有计划要求的 8 向 aim/fire/reload/hit/death 完整帧集；专属分层枪声、震屏/手柄震动、结算仪式、设置界面与持久化、12 项 UX 检查单均未完成 | `./scripts/bukov_ui_tokens_check.sh`；第 82 节 UX 检查单逐项录制 | 🟡 |
| 6 性能与交付 | 60 秒 CPU 集成报告：30 敌人+200 弹道，P95 0.0250ms、P99 0.0335ms、74.70 B/frame、0 GC；RoboVM AOT 禁用 API 测试通过；来源登记存在 | CPU 测试不渲染 GPU；无 30 分钟 soak、真实帧时间/帧 pacing/显存/温度；当前 macOS `.app` 时间早于最新源码，必须重打；当前 iOS 可安装/可玩闭环仍未签字；全新机器离线与存档恢复未验收 | `./scripts/bukov_performance_e2e.sh 1800`；`./scripts/bukov_robovm_api_gate.py`；重打 macOS/iOS 包并做双端离线验收 | 🟡 |
| 1.0 全内容 | 无 | 6 个地图主题、18–24 枪、12+ 敌人和长期经济循环尚未实施 | 计划第 68 节最终完成定义 | ❌ |

## 当前自动证据

| 证据 | 可证明什么 | 不能证明什么 |
|---|---|---|
| `core/build/test-results/test/`：66 个 XML、238 tests、0 failure/error/skipped，时间 2026-07-23 21:16 | 当前编译产物对应的 core 测试通过 | 不等于桌面/iOS 实际可玩 |
| 其中 Bukov：62 个 XML、232 tests、0 failure/error/skipped | runtime、combat、AI、地图、raid、save、UI、表现合同的自动断言通过 | 不等于每项均已接入玩家可达流程 |
| `desktop/build/test-results/test/`：2/2 | 桌面测试通过 | 不等于最新 macOS 包已重打 |
| `BukovEndToEndPerformanceSmoke.xml` | 60 秒集成 CPU 模拟；30 AI、200 弹道、无 GC，P95 低于 4.5ms 预算 | 不包含 `GameScene`、GPU、音频、真实精灵批次和热负载 |
| `BukovPerformanceSmoke.xml` | 60 秒内环微基准 P95 0.0031ms | 不是整局性能 |
| `BukovSeedSweepTest.xml` | 20 固定种子 + 默认 20 个可选种子通过 | 没有保留 10,000 种子本快照报告 |
| `BukovSaveStressTest.xml` | 默认 10 次 checkpoint/resume/settlement 通过 | 没有保留 100 次本快照报告，也不是强杀真实进程 |
| `BukovIosAotCompatibilityTest.xml` | 源码级已知 RoboVM 幻影 API 禁用规则通过 | 不能替代实际 AOT 链接和模拟器启动 |
| `docs/SOURCE_PROVENANCE.csv`、`docs/THIRD_PARTY_BORROWING.md` | 当前代码/素材来源与“仅参考”边界已登记 | 最终发行包 notices 与完整资产授权复核仍需做 |

## 关键实现证据索引

| 能力 | 实现入口 | 自动测试入口 |
|---|---|---|
| 120 Hz 实时层 | `bukov/runtime/`、`GameScene.java`、`Char.java`、`CharSprite.java` | `bukov/runtime/*Test.java` |
| 枪械与弹药 | `bukov/combat/`、`assets/bukov/content/firearms.json`、`ammunition.json` | `bukov/combat/**/*Test.java` |
| 敌人、精英、Boss | `bukov/ai/`、`assets/bukov/content/enemies.json`、`BukovRealtimeWorld.java` | `bukov/ai/*Test.java` |
| 地图与撤离 | `bukov/levels/`、`bukov/map/`、`Dungeon.java` | `bukov/levels/*Test.java`、`bukov/map/*Test.java` |
| 搜刮、仓库、结算 | `bukov/raid/`、`bukov/content/`、`WndBukovHub.java` | `bukov/raid/*Test.java`、`bukov/content/*Test.java` |
| 文件存档 | `bukov/save/` | `bukov/save/*Test.java` |
| HUD 与 UI 令牌 | `BukovRaidHud.java`、`WndBukovHub.java`、`ui_tokens.json` | `bukov/ui/*Test.java` |
| 表现事件与体验合同 | `bukov/fx/`、`bukov/audio/`、`experience_contract.json` | `bukov/fx/*Test.java`、`bukov/audio/*Test.java`、`bukov/settings/*Test.java` |

## 最终统一验收命令

下列命令必须在准备提交的同一工作树、同一 SHA 上执行并保存日志；本次矩阵
整理没有运行 Gradle。

```bash
git diff --check
./scripts/apple-gradle \
  core:clean desktop:clean \
  core:test desktop:test desktop:build ios:compileJava \
  --no-daemon

./scripts/bukov_ui_tokens_check.sh
./scripts/bukov_seed_sweep.sh 10000
./scripts/bukov_save_stress.sh 100
./scripts/bukov_performance_smoke.sh 1800
./scripts/bukov_performance_e2e.sh 1800
./scripts/bukov_robovm_api_gate.py
```

## 必须补齐的人工验收矩阵

| 场景 | macOS | iOS 模拟器 | 手柄 | 通过标准 |
|---|---|---|---|---|
| 冷启动→仓库→配装→出击 | 待签字 | 待签字 | 待签字 | 无崩溃、焦点不丢、配装持久化 |
| 移动→瞄准→射击→换弹→击杀 | 部分历史验证，须绑定最终 SHA | 待签字 | 待签字 | 无滑行/穿墙，射速不随帧率，敌人有完整反馈 |
| 搜 3 个容器→拾取/丢弃 | 待签字 | 待签字 | 待签字 | 搜索可打断，UID/重量/价值一致 |
| E01/E02/E03→结算→回仓库 | 待签字 | 待签字 | 待签字 | 条件、倒计时、打断、幂等均正确 |
| 中途退出→继续→异常退出恢复 | 部分历史验证，须重录 | 待签字 | 不适用 | seed、弹药、计时、容器、撤离状态一致 |
| 原 Leogame-one 模式回归 | 待签字 | 待签字 | 待签字 | 创建、存档、继续不受 Bukov 分支破坏 |
| 30 分钟高负载 | 待签字 | 待签字 | 待签字 | 真实 P95 达标，无崩溃、持续 GC 或热降频异常 |

## 退出结论

当前不是“全部开发完”。核心搜打撤系统已从模型推进到宿主接线，自动测试面
较完整；阻断最终签字的主要问题是：

1. iOS 与 macOS 最新源码包的完整玩家路径仍未形成同一 SHA 的证据。
2. 最终角色/敌人动画、专属音频、VFX、设置和全流程手柄品质未达到第 82 节。
3. 护甲、背包、合同链和多主题长期内容未完成。
4. 10k seed、100 次存档、30 分钟 CPU/GPU/设备稳定性报告需要在最终 SHA 重跑并归档。
