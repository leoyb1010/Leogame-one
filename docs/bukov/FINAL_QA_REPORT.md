# 《逃离布科夫》Alpha 6 QA 报告

结论：**阶段通过，不是最终完成签字。**

本报告绑定代码提交 `cd1068b6642d72681aa4ffd662a4a6343f19d1f3`。它证明
Alpha 6 的自动回归、macOS 打包运行和 iOS Simulator AOT 启动通过；不能证明
v2.0 计划书已经 100% 完成。物理 iOS 设备、实体手柄、三人盲测、真实 GPU
帧时间百分位和墙钟 30 分钟热稳定性仍未执行（`NOT RUN`）。

## 构建身份

| 字段 | 值 |
|---|---|
| 产品 | 逃离布科夫 / Escape from Bukov |
| 版本 | `2.0.0-alpha6` |
| Source commit | `cd1068b6642d72681aa4ffd662a4a6343f19d1f3` |
| 分支 | `codex/bukov-realtime` |
| 日期 | `2026-07-24 / Asia/Shanghai` |
| macOS 主程序 SHA-256 | `cc3734c89cf5e6ce081615109094b5a7e1216be3b61de91a8c9eaf5dec57a7b7` |
| iOS Simulator 主程序 SHA-256 | `37ccae9b61b110977787cc593156cc0165c098647cb15b544b0e37132ee6464b` |

产物：

- macOS：`/Users/leoyuan/Documents/日常/output/逃离布科夫-alpha6-cd1068b66/逃离布科夫.app`
- iOS Simulator：`/Users/leoyuan/Documents/日常/output/逃离布科夫-alpha6-cd1068b66/逃离布科夫-iOS-Simulator.app`
- 自动回归日志：`build/evidence/alpha6-release-suite.log`

## 自动验收

| 门禁 | 结果 |
|---|---|
| Core 测试 | PASS：656 tests，0 failures/errors |
| Desktop 测试 | PASS：4 tests，0 failures/errors |
| iOS 测试 | PASS：6 tests，0 failures/errors |
| 内容规模 | PASS：6 主题、18 枪械、9 普通敌人、3 精英、1 Boss、5 模式 |
| 原创音频 | PASS：28 个 mono 48 kHz PCM16 WAV |
| 法律文件 | PASS：macOS JAR 与 iOS 包均含 GPL 与第三方 notices |
| 真实首关地图 | PASS：默认 500 个真实 `BukovLevel` seed |
| 射击生产链 | PASS：鼠标、触控、手柄逻辑均覆盖弹药、曳光、墙阻挡和伤害 |
| 相机 | PASS：滚轮不缩放；鼠标 3.5 格、手柄/触控 2.5 格前视；整数像素对齐 |
| 存档迁移 | PASS：旧 active raid 无枪时一次性发放 12+24 应急弹药，不进入经济 |

## 实际交互验收

### macOS

- 当前提交打包应用可以冷启动、继续行动。
- 行动中滚轮不再改变世界缩放。
- `Tab` 可打开行动背包。
- 先前同生产链包已实际验证鼠标射击使弹匣 `6 → 5`；Alpha 6 的相同链路由
  新增生产集成测试覆盖。当前保存中的弹药已耗尽，因此没有伪造新的实机扣弹截图。
- 地图保持整数缩放和固定跟随；相机前视已接入生产路径。

### iOS Simulator

- `iOS 26.5 / Codex Test iPhone 17 Pro` AOT 构建、签名、启动通过。
- 旧行动原先显示“未装备枪械 / --|--”，迁移后实际显示 Needle-9 `12 | 24`。
- 竖屏安全区、HUD、双摇杆和行动按钮可见。
- 右侧区域是持续式瞄准射击摇杆：需越过 18% 死区并保持至少一个 120 Hz
  fixed step；快速自动化 drag 不能替代真人持续触控，生产集成测试已覆盖该时序。

## 本轮新增的玩家可见能力

- 基地仓库新增六类筛选、四级稀有度色边和同类价值对比。
- 结算新增 600 ms 逐行揭示、金额滚动和撤离/损失印章，可安全跳过。
- 真实首关门禁验证档案、任务门、高价值点、E01、五件地面物资和维修柜互不冲突。
- 鼠标、触控、手柄射击输入汇入同一生产链。
- 六主题环境规则、敌人寻路/局部避障、保险、长期合同、枪械附件、维修钥匙锁柜、
  分枪族枪声、三段换弹声和弹壳表现均已接入。

## 尚未满足的最终要求

- 六主题仍需要更强的独立图集、地表材质和可交互剪影差异。
- VFX 渲染对象仍需池化，并区分墙体火花、护甲反馈、肉体/布料命中。
- 枪声音频仍需更多采样变体、机械/body/tail 分层和按空间切换尾韵。
- 仓库已提升信息层级，但还不是计划书要求的完整图标网格、搜索和多列排序。
- 需要完成同一最终 SHA 的“新档→配装→搜索→档案→开门→交火→撤离→结算→
  回仓→再开→死亡损失”双平台完整录像。
- 需要物理设备、实体手柄、真实 GPU P50/P95/P99、墙钟 30 分钟热稳定性和
  三人盲测，才能把报告升级为最终签字。

## 当前签字

- [x] `PASS — ALPHA 6 AUTOMATION + macOS/iOS SIMULATOR STARTUP`
- [ ] `PASS — COMPLETE PERSONAL SINGLE-PLAYER BUILD`
- [ ] `PASS — COMMERCIAL / APP STORE SCOPE`

证据生成日期：2026-07-24
