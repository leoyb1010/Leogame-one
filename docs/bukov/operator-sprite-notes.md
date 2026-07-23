# 逃离布科夫原创行动员动画图集

## 结论

`core/src/main/assets/sprites/bukov_operator.png` 已经不是旧 Rogue 图的换色版本。当前图集由 `scripts/generate_bukov_operator_sprite.mjs` 从透明画布开始，用像素几何图元独立绘制：全覆式头盔、窄观察窗、呼吸面罩、板甲、战术背包、无线电天线、步枪、弹匣和腰包均为新的轮廓。

生成脚本不读取任何源图片，不接受角色模板路径，也不复制旧图的 Alpha 平面。它只将自行绘制的 RGBA 缓冲区交给 FFmpeg 编码成 PNG。

## 运行时契约

- 图集：`256 × 128`、8-bit RGBA、透明背景。
- 有效帧：每帧 `12 × 15`，每行 21 帧，8 行装备层级。
- 有效区域：`252 × 120`；右侧 4 像素和底部 8 像素是现有 `HeroSprite` 图集契约要求的透明留白。
- 0–1：idle，端枪警戒有轻微姿态差。
- 2–7：run，六帧非对称步态，包含身体起伏。
- 8–12：death，从受击、屈膝到倒地静止。
- 13–15：fire，举枪、枪口焰、后坐复位。
- 16–17：reload / operate，卸下弹匣和插入弹匣。
- 18：hit / airborne brace，当前可兼容主工程的 `fly` 帧调用。
- 19–20：extract / radio，抬手操作撤离无线电。

现有 `HeroSprite` 不需要改变帧宽、帧高或动画索引即可接入前六类动画；第 16–17 帧复用 `operate`，19–20 帧复用 `read`。后续如果为受击和撤离增加专属动画状态，可直接使用 18 和 19–20，不需要再次改图集。

## 确定性重建

```bash
node scripts/generate_bukov_operator_sprite.mjs
```

脚本仅依赖 Node.js 标准库与 FFmpeg。相同脚本会生成与仓库资产逐字节一致的 PNG：

- 当前 PNG SHA-256：`ecd792ae22de2f2e6a2f0ad854f8a8968fada2613da58c02f511e7b9b2617c18`
- 旧 `rogue.png` SHA-256：`84f66ab86adc47ee06440aa08931dadeca9325b127e30c559ca363b11acaf7aa`
- 两者轮廓和 Alpha 均不相同，生成脚本也没有源图读取入口。

统一静态验收：

```bash
./scripts/bukov_original_visual_gate.sh
```
