# 六主题视觉结构门禁

更新日期：2026-07-25

## 结论

`bukov_theme_visual_gate.sh` 现在同时验证文件一致性、来源闭包和去色后的
结构差异。六张只改色相、饱和度、亮度或 PNG 压缩结果的图片，即使 SHA-256
全部不同，也不能再作为六套主题通过。

这仍是工程门禁，不是最终美术签字。它证明主题具有分布式结构差异，不证明构图、
材质、光照、动画和整局观感已经达到最终品质；最终美术仍必须完成双平台截图册和
人工验收。

## 检查方法

`scripts/bukov_theme_structure_gate.mjs` 对 `tiles`、`water` 和 `landmarks`
分别执行：

1. 通过 FFmpeg 解码为 RGBA，排除 PNG 压缩器差异。
2. 将 RGB 转为 Rec.709 灰度亮度。
3. 对每个非透明像素记录右、下和两个对角方向的局部明暗顺序。
4. 比较六主题的 15 个两两组合。
5. 同时检查全图结构距离与分区覆盖，避免只在角落加一个标记就通过。

局部明暗顺序不依赖主题颜色。单调调色会保留顺序，结构码距离接近零；新增接缝、
管线、警戒带、积水通道、网格或地标轮廓才会改变结果。

## 当前阈值

| 通道 | 分区 | 最小两两结构距离 | 最少变化分区 |
|---|---:|---:|---:|
| 地表 `tiles` | 16×16 px | 0.055 | 30 |
| 水面 `water` | 8×8 px | 0.045 | 3 |
| 地标 `landmarks` | 每个 32×32 帧 | 0.018 | 3 |

`overlays` 另按 alpha 平面比较六套 64×32 图集，要求两帧各有有效像素且六套
alpha 指纹全部不同。这样即使颜色不同，复用同一雾片/裂缝轮廓也会失败。

门禁会逐对输出最弱主题组合。失败信息包含通道、主题名、实际结构距离、要求值和
变化分区数量，便于直接定位“哪两套仍只是换色”。

## 运行命令

完整六主题门禁：

```bash
bash scripts/bukov_theme_visual_gate.sh
```

只运行结构检查并保留 JSON 报告：

```bash
node scripts/bukov_theme_structure_gate.mjs \
  core/src/main/assets/environment/bukov \
  /tmp/bukov-theme-structure-report.json
```

验证算法确实拒绝纯调色：

```bash
node scripts/bukov_theme_structure_gate.mjs --self-test
```

## 失败处理

- `structure distance` 不足：重画地面接缝、墙边、通道、积水或设备轮廓；不要继续
  提高色彩差异。
- `changed regions` 不足：差异集中在少数格子，应把主题语言分布到整套 atlas。
- `landmarks` 不足：至少让三个 32×32 地标帧具有主题独立剪影。
- 不得降低阈值来接纳待替换素材。阈值调整必须附六主题实局截图、误报说明和新的
  自测样本。

## 边界

当前 terrain/water atlas 仍保留宿主切片和 alpha 契约，以维持地图索引与存档
兼容；静态环境叠层是项目原创几何，但仍是可替换的工程底稿。首批原创
环境素材的交付和目标路径见
`docs/bukov/ORIGINAL_ENVIRONMENT_ASSET_BRIEF_ZH.md`。当原创 atlas 完成后，
应将来源清单从宿主 `tiles_city.png` / `water3.png` 迁移到项目原创母版，同时
保留本门禁。
