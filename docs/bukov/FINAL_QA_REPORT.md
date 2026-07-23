# 《逃离布科夫》v2.0 最终 QA 报告

结论：**PASS（个人单机版交付范围）**。macOS 打包应用和 iOS 模拟器均完成实际启动与交互验收；自动化、内容、存档、地图种子、30 分钟等价模拟压力门禁全部通过。真机、三人盲测和仪器化 GPU 百分位采样未执行，不能把本报告解释为 App Store 商业发行签字。

## 1. 构建身份

| 字段 | 值 |
|---|---|
| 版本 | `v2.0.0` |
| Release source commit | `327d69d6484f80faf73365636b391a410bbaccaa` |
| 分支 | `codex/bukov-realtime` |
| 测试日期与时区 | `2026-07-24 / Asia/Shanghai` |
| 测试方式 | Codex 自动门禁 + macOS/iOS 模拟器实际交互 |
| macOS | `26.5.2 (25F84)` |
| Xcode | `26.6 (17F113)` |
| iOS Runtime | `iOS 26.5` |
| iOS 模拟器 | `Codex Test iPhone 17 Pro` / `50D8337F-7AFD-4AAE-AB44-318BCDC02AF6` |
| macOS 主程序 SHA-256 | `019ca17a15b4588f8e33afe02c33efc4cce1b8cad6dbdad2c37f1dd07a91f3b7` |
| iOS AOT 主程序 SHA-256 | `56124e7d6bdfcbfb0d0e1a3388c3267d176e3c605a1c49ed7b0f625d34081738` |

构建产物：

- macOS：`/Users/leoyuan/Documents/日常/output/逃离布科夫-v2.0/逃离布科夫.app`
- macOS 压缩包：`/Users/leoyuan/Documents/日常/output/逃离布科夫-v2.0/逃离布科夫-v2.0-macOS.zip`
- iOS Simulator：`/Users/leoyuan/Documents/日常/output/逃离布科夫-v2.0/逃离布科夫-iOS-Simulator.app`
- 原始日志与截图：`build/evidence/final-qa/`

## 2. 自动门禁

| 门禁 | 结果 | 证据 |
|---|---|---|
| Core / Desktop / iOS 测试 | PASS：Core 520、Desktop 2、iOS 6，0 failures | `release-commit-gradle-suite.log` |
| 发行静态校验 | PASS | `static-gates-precommit.log` |
| 内容规模 | PASS：6 主题、18 枪械、13 敌人、4 模式 | `static-gates-precommit.log` |
| 原创视觉与敌人图集 | PASS | `static-gates-precommit.log` |
| 72 帧交互图标 | PASS | `static-gates-precommit.log` |
| 19 个原创 SFX 与空间音频模型 | PASS | `static-gates-precommit.log` |
| UI tokens | PASS；同时修复了脚本在 Bash 下的兼容性 | `ui-tokens-portable-final.log` |
| 10,000 地图种子 | PASS | `seed-sweep-10000.log` |
| 100 次存档压力 | PASS | `save-stress-100.log` |
| 1,800 秒性能 smoke | PASS | `performance-smoke-1800.log` |
| 1,800 秒端到端等价模拟 | PASS | `performance-e2e-1800.log` |
| RoboVM API 兼容性 | PASS | `static-gates-precommit.log` |

## 3. 内容与玩法验收

已实现并由内容清单、自动测试和玩家路径共同覆盖：

- 四种实时搜打撤模式：远征、快速清扫、布衣行动、Boss 合同。
- 六套地图主题，模式间地图规模、路线风险、Boss 支路和撤离策略不同。
- 18 把枪、8 类弹药、装甲、背包、医疗、任务档案、战利品和商店经济。
- 9 个普通敌人、3 个精英、1 个三阶段 Boss“白线”。
- 实时移动、八方向瞄准、射击、换弹、受击、医疗、负面状态、敌人警戒/弹药/冷却。
- 搜索容器、地面拾取、丢弃、任务门禁、撤离、结算、死亡损失、结算幂等。
- 基地、仓库、配装、买卖、四模式选择、教程、背包、暂停、设置和四通道音量。
- 检查点 v6：玩家状态、敌人状态、行动物品、容器、地图、任务、治疗和战斗状态恢复。

## 4. macOS 实际玩家路径

状态：**PASS**

实机运行打包应用后已验证：

1. 冷启动标题、继续行动、进入基地、商店、模式切换和部署。
2. 地图超出单屏后摄像机持续跟随，探索区域按走过位置点亮。
3. 地面战利品可见、靠近有提示、成功拾取 `Needle-9`。
4. 射击弹匣从 `12` 降至 `11`；换弹从 `11/24` 恢复为 `12/23`。
5. `Tab` 打开专用行动背包；背包、暂停、设置均为不透底的战术窗口。
6. 保存返回、标题继续入口、检查点恢复正常。
7. 基地模式说明和商店最后一行无重叠。

关键截图：

- `mac-title.jpeg`
- `mac-hub.jpeg`
- `mac-vendor.jpeg`
- `mac-mode-quick.jpeg`
- `mac-raid-start.jpeg`
- `mac-camera-follow.jpeg`
- `mac-pickup.jpeg`
- `mac-fire.jpeg`
- `mac-reload.jpeg`
- `mac-backpack-opaque-final.jpeg`
- `mac-pause-opaque-final.jpeg`
- `mac-settings-opaque-final.jpeg`

## 5. iOS 模拟器实际玩家路径

状态：**PASS**

RoboVM AOT 构建并安装到 iPhone 17 Pro 模拟器后已验证：

1. 冷启动、标题、活动行动入口和基地。
2. 竖屏行动、双摇杆、交互、背包、暂停和四通道设置。
3. 动态岛、安全区和底部触控区域未遮挡主要操作。
4. 竖屏旋转到横屏后，HUD、设置、摇杆和按钮重新布局正常。
5. 背包、暂停、设置均为不透底窗口，文字对比度可读。
6. 横屏行动持续运行，无崩溃或异常停止。

关键截图：

- `ios-title.jpeg`
- `ios-hub-active.jpeg`
- `ios-raid.jpeg`
- `ios-backpack-opaque.jpeg`
- `ios-pause-opaque.jpeg`
- `ios-settings-opaque.jpeg`
- `ios-landscape-settings.jpeg`
- `ios-landscape-raid-clean.jpeg`

## 6. 性能与稳定性

| 项目 | 结论 |
|---|---|
| 固定模拟 | PASS：120 Hz；表现层 hitstop 不改变战斗模拟 |
| 高刷新配置 | PASS：桌面不限帧 / iOS 120 fps 配置与测试覆盖 |
| 1,800 秒 CPU smoke | PASS |
| 1,800 秒端到端等价模拟 | PASS |
| 10,000 seed 地图与任务门禁 | PASS |
| 100 次存档压力 | PASS |
| macOS/iOS 实际运行 | 未复现崩溃、透明窗口、输入中断或旋转异常 |

没有使用 Instruments/Metal System Trace 记录 P50/P95/P99 GPU 帧时间，也没有执行打包应用的墙钟 30 分钟热稳定性测试。因此本报告能证明高刷新配置、确定性模拟和自动压力门禁通过，不能替代商业发行级性能实验室报告。

## 7. 许可证与来源

| 检查 | 结果 |
|---|---|
| GPLv3 许可证与上游署名 | PASS |
| Leogame-one / Shattered Pixel Dungeon 来源记录 | PASS |
| 新增运行时素材进入来源台账 | PASS |
| 参考项目借鉴边界 | PASS：记录于 `docs/THIRD_PARTY_BORROWING.md` |
| 原创生成脚本和资产清单 | PASS |

## 8. 已知限制

- 未连接物理 iPhone/iPad，真机 QA 状态为 `NOT RUN`。
- 未进行三名真人玩家的盲测，不虚构通过率。
- 未连接实体手柄做完整玩家路径；手柄映射、焦点和重复输入由自动测试覆盖。
- 未采集打包应用的 GPU 帧时间百分位。

这些项目不阻断本次“自己玩的单机版 + macOS 与 iOS 模拟器”交付；若未来改为真机长期游玩或商业发行，应重新打开为发布门禁。

## 9. 最终签字

- [x] `PASS — PERSONAL SINGLE-PLAYER SCOPE`
- [ ] `PASS — COMMERCIAL / APP STORE SCOPE`
- [ ] `FAIL`

签字：Codex 自动验收与实际交互检查
证据生成日期：2026-07-24
