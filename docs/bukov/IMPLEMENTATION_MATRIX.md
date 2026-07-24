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
| 地图主题 | 6 个注册主题；首关 500 个真实 `BukovLevel` seed 回归 | Alpha 14 仍需完整正式行动实机路线 |
| 枪械与弹药 | 18 枪、8 弹药，按注册表口径配装 | Alpha 14 实机验证 Needle-9 弹匣 `12 → 11` |
| 敌人 | 13 个：9 普通、3 精英、1 Boss | Alpha 14 暗图实机确认敌人身体、轮廓和生命反馈可见 |
| Raid 模式 | 5 个：远征、快速清扫、拾荒者、Boss 合同、演练场 | macOS 演练场实机已进入 |
| 局外循环 | 仓库、配装、商店买卖与幂等交易已接入 | 完整正式行动录像仍待补 |
| 原创资产 | 72 帧物品/交互图标，73 个 PCM WAV | 发行清单与资产台账通过 |
| 平台 | Alpha 14 macOS 与 iOS Simulator 同一源码重打 | 双端签名、SHA 与启动通过；物理设备未执行 |
| 存档/性能 | checkpoint、幂等结算、seed/save/performance 门禁存在 | 10k/100 次/墙钟 30 分钟最终运行待证据 |

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

本表区分“代码存在”“自动测试通过”和“玩家实际验收”。Alpha 14 产物与截图
绑定源码提交 `ae7505d0776c85eec353dc7bda5c55462ba0e379`；后续仅文档证据
更新不改变该二进制身份。

## Gate 总表

| Gate | 已完成并可核验 | 未完成 / 未签字 | 验收命令 | 当前判定 |
|---|---|---|---|---|
| 0 环境与保护 | 基线、分支、来源台账；Alpha 14 为 core 703、desktop 7、iOS 6，0 failure/error；`git diff --check` 通过 | 原 Leogame-one 模式在最终改动后的创建、存档、继续录像未留证 | `git status --short --branch`；`git diff --check`；`./scripts/apple-gradle core:test desktop:test ios:test --no-daemon` | 🟡 |
| 1 实时移动 | `FixedStepClock`、`RealtimeBody`、`GridCollision`、`RealtimeInput`、浮点精灵定位和 `GameScene` 模式分支已接入；runtime 测试存在且当前通过 | 键鼠/主流手柄各 20 分钟；60/120/144 Hz 等距；背包暂停；15 分钟无死锁均未留当前证据 | 完整测试命令；再按“人工验收矩阵”逐项录制 | 🟡 |
| 2 枪战 | 18 枪、8 种弹药；FireControl、hitscan、伤害、换弹、弹种守恒、护甲路径和 13 类敌人已接入；Alpha 14 实机验证扣弹、敌人身体可见与短促曳光 | 城防-556 当前换弹为 2.35s，与 Gate 2 的 1.4s 规格不一致；实体手柄逐枪交火和所有 Boss 反馈未签字 | `./scripts/apple-gradle core:test --tests '*bukov.combat*' --tests '*bukov.ai*' --no-daemon`；实机逐枪射速/换弹/墙阻/敌人闭环 | 🟡 |
| 3 搜打撤 | 独立 profile/raid 存档、临时文件+备份、断点恢复、UID、幂等结算、Heap 适配、仓库/配装窗口、可搜索容器、E01/E02/E03 撤离和死亡/成功结算已接入宿主 | “配装→搜 3 箱→交火→撤离→结算→仓库→死亡损失”尚无一次完整录像；手柄焦点全流程未签字；当前完整测试只按默认值执行 10 次压力循环 | `./scripts/bukov_save_stress.sh 100`；完整局人工录制；重复结算/异常退出复核 | 🟡 |
| 4 第一关 | 程序化 Figure-eight 语义图与宿主 `BukovLevel`；26–34 房、三路线、三撤离；18 枪、8 弹药、3 护甲、2 背包、7 医疗、13 敌人和长期合同链均有实现 | 10k seed 报告、10 局留存和 15–25 分钟整局仍未验收 | `./scripts/bukov_seed_sweep.sh 10000`；20 个固定回归种子；10 局人工体验表 | 🟡 |
| 5 体验与表现 | 中英文品牌、App 图标、8 向玩家动作、`ui_tokens.json`、HUD、结算仪式、设置持久化、焦点模型、73 个分层/空间音频资源和反馈模型均已接入 | UI Token 尚未封闭；HUD 缺脱战淡化与三路受击方向；VFX 生产对象未完整池化；首次启动校准、完整 UI 美术包及第 82 节 12 项签字仍缺 | `./scripts/bukov_ui_tokens_check.sh`；第 82 节 UX 检查单逐项录制 | 🟡 |
| 6 性能与交付 | 60 秒 CPU 集成报告：30 敌人+200 弹道，P95 0.0250ms、P99 0.0335ms、74.70 B/frame、0 GC；RoboVM AOT 禁用 API 测试通过；来源登记存在 | CPU 测试不渲染 GPU；无 30 分钟 soak、真实帧时间/帧 pacing/显存/温度；当前 macOS `.app` 时间早于最新源码，必须重打；当前 iOS 可安装/可玩闭环仍未签字；全新机器离线与存档恢复未验收 | `./scripts/bukov_performance_e2e.sh 1800`；`./scripts/bukov_robovm_api_gate.py`；重打 macOS/iOS 包并做双端离线验收 | 🟡 |
| 1.0 全内容 | 无 | 6 个地图主题、18–24 枪、12+ 敌人和长期经济循环尚未实施 | 计划第 68 节最终完成定义 | ❌ |

## 当前自动证据

| 证据 | 可证明什么 | 不能证明什么 |
|---|---|---|
| `core/build/test-results/test/`：193 个 XML、703 tests、0 failure/error/skipped | Alpha 14 源码对应的 core 全量测试通过 | 不等于所有长时实机要求均通过 |
| `$(getconf DARWIN_USER_CACHE_DIR)/escape-from-bukov-gradle/desktop/test-results/test/`：7/7 | desktop 测试通过 | 不替代实体手柄与墙钟稳定性 |
| `$(getconf DARWIN_USER_CACHE_DIR)/escape-from-bukov-gradle/ios/test-results/test/`：6/6 | iOS 测试与 AOT 源码门禁通过 | 不替代物理 iOS 设备 |
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
