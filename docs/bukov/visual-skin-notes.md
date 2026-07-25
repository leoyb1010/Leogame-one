# 布科夫六主题场景视觉包

原来的雾港单主题调色包已升级为六主题生产资产。运行时不再固定使用
`tiles_fog_depot.png` 和 `water_fog_depot.png`，而是根据当前玩法主题选择
独立的地表、水面、地标和静态环境叠层 atlas。

六主题的映射、地表图案、地标剪影、来源、权属、兼容边界与验收命令统一记录在：

- `docs/bukov/THEME_VISUAL_SOURCE_LEDGER.md`
- `core/src/main/assets/environment/bukov/theme_visual_manifest.json`

## 视觉与技术约束

- 地图拓扑和碰撞继续由宿主地图生成逻辑负责。
- 地表与水面派生图严格保留宿主 atlas 的尺寸、切片位置和 alpha。
- 所有 atlas 保持 RGBA 像素图，运行时使用像素采样，不做缩放重绘。
- 交互物件在六种 palette 中保持固定高对比语义色。
- 主题图案和地标剪影必须在灰度或低饱和屏幕上仍能区分。
- 每张地图最多增加三个静态 2×2 环境叠层，不改变 terrain、碰撞或任务路线，
  也不增加逐帧动画循环。
- 不接入外部素材、联网生成 API 或新运行时依赖。

## 复现

```bash
node scripts/generate_bukov_theme_visuals.mjs
bash scripts/bukov_theme_visual_gate.sh
```

门禁会重新生成并逐字节比对全部产物。人工复核可直接查看：
`core/src/main/assets/environment/bukov/theme_visual_contact_sheet.png`。
