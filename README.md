# 逃离布科夫 · ESCAPE FROM BUKOV

> 搜到什么不算本事，能带回来才算。

《逃离布科夫》是一款离线单机、俯视角、实时战斗的像素搜打撤游戏。玩家从仓库完成配装，进入程序化地图搜索物资、处理敌人和路线风险，再选择继续深入或及时撤离。成功带出的物品进入长期仓库；行动中死亡则失去本局携带物。

项目以 Leogame-one 的 GPLv3 代码库为工程基础，继承 Shattered Pixel Dungeon 成熟的地图生成、渲染与跨平台管线，并将玩家主流程重构为实时移动、枪械战斗、搜刮、撤离、结算与长期仓库。兼容源码仍保留原作者版权头和许可证，但不会作为《逃离布科夫》的玩家入口或品牌身份。

[![CI](https://github.com/leoyb1010/Leogame-one/actions/workflows/ci.yml/badge.svg)](https://github.com/leoyb1010/Leogame-one/actions/workflows/ci.yml)
![Platforms](https://img.shields.io/badge/platforms-macOS%20%7C%20iOS-2A6F7B)
![Mode](https://img.shields.io/badge/combat-realtime-E05A3A)
![License](https://img.shields.io/badge/license-GPLv3-5C6BC0)

## 游戏内容

- 实时移动、鼠标/双摇杆瞄准、半自动与自动射击
- 6 个程序化地图主题、不同风险路线、搜刮容器与多种撤离条件
- 18 把枪械、8 种具体弹药，配装按注册表口径校验
- 13 种敌人：9 普通、3 精英、1 Boss
- 远征、快速清扫、拾荒者、Boss 合同 4 种 Raid 模式
- 基地商店买入/出售、长期仓库、配装与行动中交易锁定
- 72 帧物品/交互图标和 19 个项目原创 PCM 音效
- 物品 UID、Raid 检查点、成功/死亡幂等结算与长期仓库
- macOS 与 iOS 共用核心逻辑，固定模拟 120 Hz，并提供高刷新配置

## 当前开发状态

当前分支正在执行《完整开发执行计划书 v2.0》。标题、基地、商店、行动部署、实时战斗、任务物品、封锁门、搜刮、撤离、结算与长期仓库已进入同一条布科夫玩家路径；内容规模已达到 6 主题、18 枪、13 敌人与 4 模式。

这仍是“待最终平台证据”的候选状态，不等于已经发行验收：最终 SHA 的统一 Gradle、macOS 打包全流程、iOS AOT/模拟器/真机、60/120/144 Hz 帧 pacing、30 分钟稳定性和恢复测试必须按 [最终 QA 模板](docs/bukov/FINAL_QA_REPORT_TEMPLATE.md) 留证。机器可读状态见 [发行清单](docs/bukov/RELEASE_MANIFEST.json)。

## 快速构建

环境要求：

- macOS
- Homebrew OpenJDK 17
- 构建 iOS 时需要 Xcode 与对应 Simulator Runtime

统一编译与测试：

```bash
./scripts/apple-gradle core:clean desktop:clean core:test desktop:build ios:compileJava --no-daemon
```

打包并启动 macOS：

```bash
./scripts/apple-gradle desktop:jpackageImage --no-daemon
open "$(getconf DARWIN_USER_CACHE_DIR)/escape-from-bukov-gradle/desktop/jpackage/逃离布科夫.app"
```

启动 iPhone 模拟器版本：

```bash
./scripts/apple-gradle ios:launchIPhoneSimulator --no-daemon
```

专项门禁：

```bash
./scripts/bukov_ui_tokens_check.sh
python3 ./scripts/bukov_release_manifest_check.py
python3 ./scripts/bukov_content_scale_gate.py
./scripts/bukov_audio_gate.sh
./scripts/bukov_item_atlas_gate.sh
./scripts/bukov_seed_sweep.sh 10000
./scripts/bukov_save_stress.sh 100
./scripts/bukov_performance_smoke.sh 1800
```

## 目录

```text
core/src/main/java/.../bukov/   布科夫实时战斗、内容、地图、Raid、存档与 UI
core/src/main/assets/bukov/     枪械、弹药和 UI 令牌等数据
core/src/test/java/.../bukov/   单元、回归、种子、存档与性能门禁
desktop/                        macOS/桌面启动器与图标
ios/                            iPhone/iPad 启动器、Info.plist 与 AppIcon
artwork/                        原始素材、生成素材和权属记录
docs/bukov/                     执行基线、实现矩阵、素材清单与验收记录
scripts/                        Apple 构建包装器与布科夫专项检查
```

## 开发纪律

- 固定模拟步长为 120 Hz，渲染帧率不能改变战斗结果。
- 枪械、弹药、携带物和仓库使用同一物品实例/UID 体系。
- 撤离和死亡结算必须幂等；同一 Raid 不得重复领取。
- 地图种子必须满足可达性、替代路线、风险升级和撤离约束。
- VFX、音频、震屏与 UI 只消费表现事件，不反向修改模拟结果。
- 所有可见“完成”结论必须同时通过自动测试、打包和实机流程。

## 文档

- [布科夫实现矩阵](docs/bukov/IMPLEMENTATION_MATRIX.md)
- [当前里程碑验收](docs/bukov/MILESTONE_ACCEPTANCE_2026-07-23.md)
- [机器可读发行清单](docs/bukov/RELEASE_MANIFEST.json)
- [最终 QA 报告模板](docs/bukov/FINAL_QA_REPORT_TEMPLATE.md)
- [发行来源与许可证审计](docs/bukov/RELEASE_PROVENANCE_AUDIT.md)
- [美术素材清单](docs/bukov/ART_ASSET_MANIFEST.md)
- [第三方借鉴边界](docs/THIRD_PARTY_BORROWING.md)
- [来源台账](docs/SOURCE_PROVENANCE.csv)
- [Apple 开发与构建](APPLE_DEVELOPMENT.md)

## 开源与权属

本项目基于 [Shattered Pixel Dungeon](https://github.com/00-Evan/shattered-pixel-dungeon) 与 [Pixel Dungeon](https://github.com/00-Evan/pixel-dungeon-gradle)，继续遵循 [GPLv3](LICENSE.txt)。原项目作者、翻译、音乐、美术和音效署名必须保留。

新增源码与素材的来源、借鉴方式和许可证记录在 `docs/SOURCE_PROVENANCE.csv`、`docs/THIRD_PARTY_BORROWING.md` 与 `artwork/licenses/`。对外分发修改版时，必须同步提供对应源码、GPLv3 许可证及所有必要声明。
