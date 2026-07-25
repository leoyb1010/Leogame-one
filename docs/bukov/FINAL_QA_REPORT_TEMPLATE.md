# 《逃离布科夫》最终 QA 报告模板

> 本文件是发行证据模板，不是已通过报告。未附原始日志、截图或录像的项目必须保持 `PENDING EVIDENCE`。

## 1. 构建身份

| 字段 | 值 |
|---|---|
| Release commit SHA | `PENDING EVIDENCE` |
| 分支 / Tag | `PENDING EVIDENCE` |
| 测试日期与时区 | `PENDING EVIDENCE` |
| 测试负责人 | `PENDING EVIDENCE` |
| macOS / Xcode / iOS Runtime | `PENDING EVIDENCE` |
| macOS 包 SHA-256 | `PENDING EVIDENCE` |
| iOS 包或安装产物 SHA-256 | `PENDING EVIDENCE` |

任何证据必须能回溯到同一个 Release commit SHA。工作树发生变化后，旧证据不得用于最终签字。

## 2. 自动门禁

| 门禁 | 命令 | 结果 | 原始日志路径 |
|---|---|---|---|
| 发行静态校验 | `python3 scripts/validate_release.py` | `PENDING EVIDENCE` | `PENDING EVIDENCE` |
| 内容规模 | `python3 scripts/bukov_content_scale_gate.py` | `PENDING EVIDENCE` | `PENDING EVIDENCE` |
| 音频资产 | `bash scripts/bukov_audio_gate.sh` | `PENDING EVIDENCE` | `PENDING EVIDENCE` |
| 72 帧图标图集 | `bash scripts/bukov_item_atlas_gate.sh` | `PENDING EVIDENCE` | `PENDING EVIDENCE` |
| UI tokens | `bash scripts/bukov_ui_tokens_check.sh` | `PENDING EVIDENCE` | `PENDING EVIDENCE` |
| Core / Desktop / iOS Java | `./scripts/apple-gradle core:clean desktop:clean core:test desktop:test desktop:build ios:compileJava --no-daemon` | `PENDING EVIDENCE` | `PENDING EVIDENCE` |
| 10k seeds | `./scripts/bukov_seed_sweep.sh 10000` | `PENDING EVIDENCE` | `PENDING EVIDENCE` |
| 存档压力 | `./scripts/bukov_save_stress.sh 100` | `PENDING EVIDENCE` | `PENDING EVIDENCE` |
| 108,000 帧 CPU 集成回归 | `./scripts/bukov_performance_e2e.sh 1800` | `PENDING EVIDENCE` | `PENDING EVIDENCE` |
| 双端真实 30 分钟活跃游玩 | `python3 scripts/bukov_render_frame_gate.py ...` | `PENDING EVIDENCE` | `PENDING EVIDENCE` |
| RoboVM API | `python3 scripts/bukov_robovm_api_gate.py` | `PENDING EVIDENCE` | `PENDING EVIDENCE` |

## 3. 内容抽检

| 项目 | 预期 | 结果 | 证据 |
|---|---:|---|---|
| 地图主题 | 6 | `PENDING EVIDENCE` | 清单与抽样截图 |
| 枪械 | 18 | `PENDING EVIDENCE` | 注册表、购买/拾取/配装抽样 |
| 敌人 | 13（9 普通、3 精英、1 Boss） | `PENDING EVIDENCE` | 生成/遭遇/死亡抽样 |
| Raid 模式 | 5 | `PENDING EVIDENCE` | 每模式一局 |
| 商店 | 买入、出售、余额、幂等、行动中锁定 | `PENDING EVIDENCE` | 操作录像与存档快照 |
| 图标 | 72 帧 | `PENDING EVIDENCE` | 图集门禁与游戏内抽样 |
| SFX | 83 个 WAV；必含击杀确认、Boss 阶段击破、砸地与过载 cue | `PENDING EVIDENCE` | 音频门禁、战斗事件路由与四通道混音抽样 |

## 4. macOS 玩家路径

状态：`PENDING EVIDENCE`

必须录制：

1. 从正式安装路径冷启动；默认窗口化、可切换其他 App、全屏往返后恢复合理窗口。
2. 标题、基地、商店、完整配装与五种模式选择。
3. 鼠标自由移动且不锁定；准星跟鼠标；滚轮不缩放世界；镜头跟随玩家跨越至少三个屏幕。
4. 移动、射击、换弹、医疗、背包、暂停和设置；中文输入法开启时游戏键不产生字母或候选窗。
5. 步枪、手枪、霰弹分别射墙和敌人：可见短弹道、命中反馈、弹药守恒，射墙不伤自己。
6. 普通门可穿；任务门拿档案前真实阻挡并提示原因，拿档案后视觉与碰撞同时开放。
7. 搜索容器、拾取/丢弃、交互提示、任务档案、通道解锁和撤离。
8. 至少三类普通敌人、精英和 Boss 可见且有移动、攻击、受击、死亡与掉落反馈。
9. 成功结算回仓库、死亡损失、重复结算保护。
10. 中途退出、继续行动、检查点恢复。
11. 键鼠与至少一种手柄完整流程。
12. 60/120/144 Hz 帧率对照；相同 seed 的模拟结果不得随渲染帧率变化。

产物路径：`PENDING EVIDENCE`

## 5. iOS 玩家路径

状态：`PENDING EVIDENCE`

必须记录：

1. RoboVM AOT、安装与冷启动。
2. iPhone 竖屏和至少一种横屏/平板布局。
3. 安全区、动态岛、Home Indicator 与旋转恢复。
4. 地图跟随玩家且不能被手势缩放成固定大地图；走过区域保持点亮。
5. 双摇杆、瞄准、射击、交互、换弹、医疗、背包、丢弃和暂停均为图标主识别、小字辅助，并具有按压/禁用态。
6. 控件不与动态岛/Home Indicator 重叠，旋转和后台恢复后无粘住移动或连射。
7. 完整搜打撤一局、后台恢复、强退后继续。
8. 模拟器结果与至少一台真机结果分开记录。

模拟器证据：`PENDING EVIDENCE`
真机证据：`PENDING EVIDENCE`

## 6. 性能与稳定性

| 场景 | 通过标准 | 结果 | 证据 |
|---|---|---|---|
| 固定模拟 | 120 Hz；渲染帧率不改变战斗结果 | `PENDING EVIDENCE` | 自动测试日志 |
| 模拟帧 CPU 集成 | 108,000 帧快速回归；明确不等于墙钟 30 分钟 | `PENDING EVIDENCE` | JSON/XML/终端日志 |
| 打包游戏帧 pacing | 60/120/144 Hz 分别记录 P50/P95/P99 | `PENDING EVIDENCE` | 帧时间报告 |
| 压力场景 | 30 敌人 + 200 表现对象 | `PENDING EVIDENCE` | 录像与性能采样 |
| 双端 30 分钟稳定性 | 每端活跃游玩 ≥ 1800 秒；无暂停/断点/尺寸切换冒充时长，无崩溃、死锁、存档损坏 | `PENDING EVIDENCE` | 同一 SHA 的日志、录像与包哈希 |

## 7. 许可证与来源

| 检查 | 结果 | 证据 |
|---|---|---|
| GPLv3 许可证和源码提供方式 | `PENDING EVIDENCE` | `LICENSE.txt`、发布页 |
| 上游作者与项目署名保留 | `PENDING EVIDENCE` | About、README、NOTICE |
| 运行时新增资产全部进入来源台账 | `PENDING EVIDENCE` | `ASSET_PROVENANCE.csv` |
| 参考项目未复制代码或资产 | `PENDING EVIDENCE` | `THIRD_PARTY_BORROWING.md` |
| 发行包不含未授权或来源不明素材 | `PENDING EVIDENCE` | 最终包文件清单 |

## 8. 缺陷与签字

发布阻断缺陷：`PENDING EVIDENCE`

允许遗留缺陷及理由：`PENDING EVIDENCE`

最终结论只能选择一项：

- [ ] `PASS`：所有阻断门禁通过，证据绑定同一 SHA。
- [ ] `FAIL`：存在阻断缺陷。
- [x] `PENDING EVIDENCE`：尚未完成最终双端验收。

签字：`PENDING EVIDENCE`
