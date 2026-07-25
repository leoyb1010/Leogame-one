# 发行来源与许可证审计

审计日期：2026-07-25
状态：静态台账通过；最终发行包复核 `PENDING EVIDENCE`

## 许可证边界

- 宿主 `Leogame-one` 与上游 Shattered Pixel Dungeon / Pixel Dungeon 继续按 GPLv3 分发。已有源码版权头、作者署名和 `LICENSE.txt` 不得删除。
- `gdx-ai`、Delver Engine、Mindustry 只作为机制或架构参考，没有引入依赖、复制源码或复制游戏数据。
- Seventh 因 GPL-2.0 兼容风险仅保留研究记录，不进入源码、素材和发行包。
- 本轮 Bukov Java、JSON、验证脚本以及程序化像素/音频生成脚本属于项目新增工作；随本 GPL 项目发布时按 GPL-3.0-or-later 提供对应源码。
- OpenAI 图像生成结果和 Leo 提供的素材以 `ASSET_PROVENANCE.csv` 中的逐文件权利状态为准。早期缺失的模型、seed 或完整 prompt 不得补造。

## 新增资产覆盖

`python3 scripts/validate_release.py` 会扫描：

- `artwork/inbox/`；
- `core/src/main/assets/splashes/bukov/`；
- `core/src/main/assets/sprites/bukov/` 与 `bukov_operator.png`；
- `core/src/main/assets/environment/bukov/`；
- `core/src/main/assets/sounds/bukov/`。

以上运行时资产必须逐文件出现在 `artwork/licenses/ASSET_PROVENANCE.csv`，且 provider、来源/Prompt 引用、权利状态不得为空。

当前可机器核验的原创生成链：

| 生成脚本 | 产物 |
|---|---|
| `generate_bukov_enemy_sprites.mjs` | 6 张首批敌人动画图集 |
| `generate_bukov_item_visuals.mjs` | 72 帧物品/交互图集与 manifest |
| `generate_bukov_landmarks.mjs` | 首关工业地标图集 |
| `generate_bukov_operator_sprite.mjs` | 行动员动画图集 |
| `generate_bukov_sfx.mjs` | 83 个单声道 48 kHz PCM16 WAV，含击杀确认与三个 Boss 专属 cue |

## 最终发行前仍需完成

1. 对最终 macOS `.app` 与 iOS 安装产物生成文件清单和 SHA-256。
2. 确认包内没有 `artwork/inbox/` 原稿、临时生成文件、调试新闻或未登记素材。
3. 确认发布页同时提供对应源码、GPLv3 许可证、上游署名与第三方说明。
4. 将上述产物路径填写到 `FINAL_QA_REPORT_TEMPLATE.md`，状态由 `PENDING EVIDENCE` 改为实际结果。
