# 逃离布科夫行动员分层动画

## 运行时结论

行动员不再使用“整张人物跟随鼠标换方向”的旧接法。实时渲染由两个同尺寸透明图层组成：

- `bukov_operator_lower.png`：髋部以下、脚步和接地阴影，仅跟随移动向量。
- `bukov_operator_upper.png`：躯干、头盔、手臂、武器、枪口焰和动作道具，优先跟随瞄准向量。

因此角色可以向东移动、同时向北瞄准或开火；停止移动后腿部保留最后一个稳定朝向，武器仍可独立跟随鼠标。死亡动作只绘制完整的上层倒地帧，不会残留站立的腿。

## 图集契约

- 三张图集均为 `384 × 128`、8-bit RGBA、透明背景。
- 每帧 `12 × 15`，每个方向 32 帧，共 8 行。
- 方向顺序：N、NE、E、SE、S、SW、W、NW。
- 0–1 idle，2–7 move，8–9 aim，10–12 fire，13–16 reload，
  17–19 hit，20–23 medical，24–27 down，28–31 extract。
- 完整图 `bukov_operator.png` 继续作为头像和动画时序源；分层图只改变实时世界中的绘制组合。

`BukovOperatorPose` 是唯一的方向状态入口。`HeroSprite` 的下层拥有独立动画时钟，所以上层播放开火、换弹或治疗时不会冻结脚步。运行时按 v2.0 状态机映射为 4 帧 8fps idle、8 帧 12fps walk、八向单帧 aim、4 帧 30fps fire、6 帧 reload、2 帧短 hit 和 6 帧 12fps death；现有原创帧通过确定性复用补齐状态机要求的帧数。

## 确定性重建

```bash
node scripts/generate_bukov_operator_sprite.mjs
```

生成器从同一组原创像素图元一次性输出完整图、下半身层、上半身/武器层和带三份 SHA-256 的 manifest。完整图的像素输出保持不变，分层资产不读取或复制任何外部角色图片。
