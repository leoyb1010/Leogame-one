# Leo 素材权属台账

`ASSET_PROVENANCE.csv` 逐文件记录 `artwork/inbox/` 中的 Leo 定制源素材。这批素材由项目所有者 Leo Yuan 提供并授权用于本仓库及其构建产物。

- `artwork/processed/` 为 `scripts/process_leo_artwork.py` 产生的中间派生文件。
- `core/`、`desktop/` 和 `ios/` 中的 Leo 运行时素材由同一脚本生成或缩放。
- 精确生成工具、seed 与原始 Prompt 未随早期素材一同保存，台账不伪造这部分信息。后续素材必须在接入前补齐这些字段。
- 2026-07-23 起，应用图标源文件更新为已确认的“逃离布科夫 / ESCAPE FROM BUKOV”版本；其哈希和跨平台派生范围记录在 `docs/bukov/ART_ASSET_MANIFEST.md`。
- 上游美术、音乐与音效仍遵循 Shattered Pixel Dungeon 的原始署名和许可，不在本表中重复声明。

当前台账同时覆盖 Bukov 运行时工业标题、环境、行动员、6 张首批敌人图集、
72 帧物品/交互图集和 28 个项目原创 WAV。以下命令会检查运行时资产是否逐
文件登记、生成脚本是否存在对应产物，以及来源/权利字段是否完整：

```bash
python3 scripts/validate_release.py
python3 scripts/bukov_release_manifest_check.py
```

静态台账通过不等于最终发行包已经完成权利复核；包内文件清单、源码提供
方式与发布页署名仍须绑定最终 Release commit，记录到
`docs/bukov/FINAL_QA_REPORT_TEMPLATE.md`。
