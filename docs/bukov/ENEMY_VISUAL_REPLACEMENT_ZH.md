# 布科夫敌人视觉替换说明

## 结论

实时搜打撤主路径不再使用 `RatSprite`、`GooSprite`、
`GnollTricksterSprite`、`GuardSprite`、`BruteSprite` 或 `DM100Sprite`。
六个首局敌人定义都映射到布科夫自有 Sprite 类和独立 RGBA 图集。

| 敌人定义 | 屏幕识别特征 | 专属 Sprite |
| --- | --- | --- |
| `melee_rusher` | 土黄色夹克、软帽、临时背包 | `BukovScavengerSprite` |
| `scavenger_gunner` | 橄榄色战术背心、青色瞄具、长枪 | `BukovGunnerSprite` |
| `iron_clasp_guard` | 宽肩钢甲、琥珀色面罩 | `BukovArmoredSprite` |
| `iron_clasp_captain` | 黑红指挥甲、红色光学瞄具 | `BukovCaptainSprite` |
| `sensor_doll` | 双旋翼低空无人机、青色传感器 | `BukovDroneSprite` |
| `boss_white_line` | 高瘦白雨衣、黑脸、单只红眼 | `BukovWhiteLineSprite` |

## 图集契约

- 单帧：`16 × 18` RGBA。
- 每张图集：`256 × 18`，共 16 个槽位。
- 动画：待机 `0-1`、攻击 `2-3`、奔跑 `4-7`、死亡 `8-10`。
- 像素由脚本的矩形、线段等几何原语直接绘制，不复制旧地牢敌人图集。
- 敌人定义 ID 是首选映射键；旧存档只在缺失/未知 ID 时用
  `hostClassHint` 兼容回退，但回退目标仍是布科夫 Sprite。

## 可重复生成与验收

```sh
node scripts/generate_bukov_enemy_sprites.mjs
./scripts/bukov_enemy_sprite_gate.sh
```

验收脚本会检查六张图集都存在、尺寸和 RGBA 格式正确、哈希互不相同，
且 `BukovHostMob` 与布科夫 Sprite 包中没有六个旧地牢 Sprite 引用。

## 下一批物品与战术标记图集设计

后续图集应继续使用冷灰、橄榄绿、琥珀任务色、青色科技光四组色族，
避免复用药水、卷轴、法杖的外形语言。建议保持 `16 × 16` 单格并预留：

| 槽位 | 内容 | 关键轮廓 |
| --- | --- | --- |
| `0-3` | 手枪、冲锋枪、突击步枪、精确步枪 | 枪管长度和枪托区分 |
| `4-7` | 9mm、5.56、7.62、特殊弹药 | 弹匣形状与底色条区分 |
| `8-11` | 医疗包、止血带、护甲板、工具组 | 红十字仅用于医疗，护甲用钢灰 |
| `12-15` | 维修档案、军牌、电子元件、贵重品 | 任务档案固定琥珀描边 |
| `16-19` | 玩家、敌情、噪声、战利品标记 | 实心箭头、红菱形、声波、白箱 |
| `20-23` | 锁定通道、已开放、撤离点、倒计时撤离 | 锁、断锁、青门、环形进度 |

战术标记在小地图与世界空间必须共用同一轮廓，但世界空间版本可增加
两帧呼吸动画；颜色不得作为唯一信息，轮廓和内部符号必须独立可读。
