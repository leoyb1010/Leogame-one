# 六主题场景视觉来源台账

更新日期：2026-07-24

## 生产映射

玩法主题 ID 保持不变，以兼容现有存档；视觉资源 ID 只负责选择 atlas。

| 玩法主题 ID | 视觉资源 ID | 地表图案 | 地标剪影 |
|---|---|---|---|
| `fog_depot` | `fog_depot` | 雾斑与潮湿补丁 | 信号桅杆 |
| `rust_workshop` | `rust_works` | 斜向炉区警戒纹 | 排气烟囱 |
| `flooded_passage` | `flooded_bunker` | 横向积水通道 | 架空管线 |
| `overgrown_yard` | `container_yard` | 集装箱分块接缝 | 龙门吊钩 |
| `cold_storage` | `cold_storage` | 冷库检修网格 | 制冷风扇 |
| `sealed_lab` | `underground_lab` | 地下实验室电路纹 | 传感天线 |

每个视觉资源 ID 都有三张独立运行时 atlas：

- `environment/bukov/tiles_<视觉资源 ID>.png`
- `environment/bukov/water_<视觉资源 ID>.png`
- `environment/bukov/landmarks_<视觉资源 ID>.png`

精确文件名、尺寸、调色板和 SHA-256 记录在
`core/src/main/assets/environment/bukov/theme_visual_manifest.json`。

## 来源与权属

生成器只读取仓库内已有资产：

| 内部源文件 | 用途 | 来源边界 |
|---|---|---|
| `core/src/main/assets/environment/tiles_city.png` | 保留宿主 terrain atlas 的切片与透明边界 | 上游工程内置资产，随本仓库许可使用 |
| `core/src/main/assets/environment/water3.png` | 保留宿主水面动画切片与透明边界 | 上游工程内置资产，随本仓库许可使用 |
| `core/src/main/assets/environment/bukov/first_raid_landmarks.png` | 交互物件的基础像素几何 | 本项目程序生成原创 |

六套 palette、像素图案、地标附加剪影、接触表与清单均由
`scripts/generate_bukov_theme_visuals.mjs` 确定性生成。生成过程不下载、
不调用网络、不引入第三方图片或新依赖。

## 可玩性与交互契约

- 地图房间、连接、碰撞、出生点、撤离点和任务门拓扑不由视觉生成器修改。
- 六套 `tiles` 和 `water` 的 alpha 平面必须逐像素等于对应宿主源图，
  因此 atlas 切片边界不会漂移。
- 档案柜、维修门、基础撤离、条件撤离和工业缓存使用跨主题固定的
  高对比语义色；主题 accent 只改变外围轮廓和剪影，不改变交互含义。
- 旧存档缺少视觉资源 ID 时回退 `fog_depot`；新存档会保存地标的
  `visualAssetId`，避免恢复后换回雾港贴图。

交互色值和每主题 accent 精确值均以生成清单为准。

## 复现与验收

```bash
node scripts/generate_bukov_theme_visuals.mjs
bash scripts/bukov_theme_visual_gate.sh
```

门禁会从源文件重新生成六套资产并逐字节比对，同时验证：

- 6 套 `tiles / water / landmarks` 各自哈希唯一；
- 尺寸和 RGBA 像素格式符合宿主 atlas；
- `tiles / water` alpha 与宿主源图完全一致；
- 每套地标均包含五类可交互语义色和独立主题剪影；
- 运行时 terrain、水面、静态地标与任务门都从当前主题取图；
- 生成器没有网络或外部文件读取路径。

人工快速复核图：
`core/src/main/assets/environment/bukov/theme_visual_contact_sheet.png`。
横向顺序与本页“生产映射”表一致。
