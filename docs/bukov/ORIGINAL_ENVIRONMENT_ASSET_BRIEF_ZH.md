# 《逃离布科夫》首批原创环境素材生成规范

更新日期：2026-07-25

## 目标

首批交付覆盖六个主题的地面/墙体、门、掩体、地标和水面。地图拓扑、碰撞和
任务逻辑保持现有实现，只替换玩家看到的环境语言。

生成图不能直接覆盖运行时 atlas。每张原图必须先保存 prompt、模型、种子和授权
记录，再由人工或处理脚本完成像素对齐、切片映射和透明通道检查。

## 统一美术规则（稳定上下文）

- 类型：俯视 3/4 视角工业废土像素画，16×16 世界网格，最近邻采样。
- 画面：写实比例经过像素化提炼，轮廓干净，材质明确，不做卡通大头。
- 光源：左上方冷环境光；危险/任务设备可有局部暖光。
- 色值：暗部不能压成纯黑，角色和敌人站在地表上必须保留清楚轮廓。
- 地面：至少包含基础面、边缘、转角、破损、污渍、积水、警戒标线和通道接缝。
- 墙体：至少包含正面、顶部、内外角、门洞、破损和设备附着边。
- 交互色：任务档案琥珀、危险橙红、撤离青绿、条件撤离金黄；不得只靠颜色表达，
  必须有不同形状。
- 禁止：文字、品牌 Logo、人物、武器 UI、透视背景、等距 45° 棋盘、平滑抗锯齿、
  JPEG 噪点、柔焦、法术、符文、中世纪石墙、地牢火把。

## 动态输入边界

下面各主题的“主题变量”是每次生成唯一允许替换的部分。统一规则、尺寸、相机、
光源、透明背景和输出契约保持不变。

## 母版生成 Prompt 模板

```text
你是一名资深像素游戏环境美术师。为俯视角实时搜打撤游戏《逃离布科夫》
设计一套原创工业环境素材母版。

<固定规则>
top-down three-quarter pixel art, industrial extraction shooter environment,
16x16 world grid, crisp nearest-neighbor pixels, clean readable silhouettes,
top-left cool ambient light, controlled local warm practical lights,
dark but readable values, no baked text, no characters, no UI,
no antialiasing, transparent background for isolated props
</固定规则>

<主题变量>
主题：{{THEME_NAME}}
核心材质：{{MATERIALS}}
空间结构：{{STRUCTURE_LANGUAGE}}
地面特征：{{FLOOR_LANGUAGE}}
墙与门：{{WALL_DOOR_LANGUAGE}}
掩体：{{COVER_LANGUAGE}}
大型地标：{{LANDMARK_LANGUAGE}}
环境危险：{{HAZARD_LANGUAGE}}
主色与强调色：{{PALETTE}}
</主题变量>

<输出要求>
同一张清晰的 sprite design board，分区展示：
1. 地面、墙体、门和转角；
2. 两类一格掩体和两类两格掩体；
3. 档案柜、维修门、撤离信标、工业缓存；
4. 三个主题专属大型地标；
5. 水面或主题动态表面的四帧变化。
所有物件保持统一像素密度、脚底接触面和左上光源。
背景纯透明或单色，不添加标题、标签和展示阴影。
</输出要求>
```

统一负面 Prompt：

```text
no fantasy dungeon, no medieval castle, no magic, no rune, no torch,
no character, no creature, no weapon HUD, no logo, no readable text,
no isometric camera, no photorealistic render, no 3D mockup,
no smooth vector edges, no antialiasing, no blur, no depth of field,
no JPEG artifacts, no baked lighting background, no palette-only variants
```

## 六主题变量

### 1. 雾港仓库 / `fog_depot`

```text
THEME_NAME: fogbound coastal freight depot
MATERIALS: wet concrete, dark galvanized steel, old teal paint, salt corrosion
STRUCTURE_LANGUAGE: long warehouse lanes, loading bays, mast and cable silhouettes
FLOOR_LANGUAGE: irregular wet patches, drain channels, faded loading numbers without text
WALL_DOOR_LANGUAGE: corrugated warehouse walls, rolling shutters, maintenance doors
COVER_LANGUAGE: wrapped pallets, concrete barriers, low cargo cages
LANDMARK_LANGUAGE: signal mast, fog lamp controller, flooded loading dock pump
HAZARD_LANGUAGE: dense ground fog, leaking electrical cabinet, slippery standing water
PALETTE: blue-black, desaturated teal, cold grey, restrained amber lamps
```

### 2. 锈蚀工坊 / `rust_works`

```text
THEME_NAME: abandoned rusted metal workshop
MATERIALS: oxidized steel, soot-black brick, furnace plate, copper pipe
STRUCTURE_LANGUAGE: diagonal furnace lanes, welding bays, exhaust stacks
FLOOR_LANGUAGE: scorched plates, diagonal safety bands, slag cracks and oil burns
WALL_DOOR_LANGUAGE: riveted furnace walls, heat shields, heavy sliding blast doors
COVER_LANGUAGE: steel worktables, scrap bins, engine blocks, plate stacks
LANDMARK_LANGUAGE: dormant furnace, exhaust stack manifold, broken overhead crane
HAZARD_LANGUAGE: residual embers, steam vents, unstable hot floor seams
PALETTE: charcoal, iron brown, burnt orange, small pale-yellow heat accents
```

### 3. 淹水通道 / `flooded_bunker`

```text
THEME_NAME: partially flooded underground service bunker
MATERIALS: damp concrete, painted pipework, algae stain, submerged steel grating
STRUCTURE_LANGUAGE: horizontal water channels, raised service walks, overhead pipes
FLOOR_LANGUAGE: shallow reflective water, broken drainage grid, sediment lines
WALL_DOOR_LANGUAGE: bunker concrete, pressure doors, pipe penetrations, flood marks
COVER_LANGUAGE: pump housings, raised cable trays, sealed equipment cases
LANDMARK_LANGUAGE: main pump, pressure valve station, collapsed water-control gate
HAZARD_LANGUAGE: electrified puddles, leaking pipe, deep-water drop
PALETTE: near-black navy, petrol blue, oxidized cyan, warning yellow
```

### 4. 荒生集装箱场 / `container_yard`

```text
THEME_NAME: overgrown abandoned container freight yard
MATERIALS: painted container steel, cracked asphalt, weeds, chain-link fence
STRUCTURE_LANGUAGE: modular container blocks, open traffic lanes, gantry silhouettes
FLOOR_LANGUAGE: asphalt seams, tire marks, weed breaks, cargo lane blocks
WALL_DOOR_LANGUAGE: stacked container sides, fence gates, rolling cargo doors
COVER_LANGUAGE: container corners, cable reels, pallet stacks, portable barriers
LANDMARK_LANGUAGE: gantry crane hook, collapsed container bridge, weighbridge cabin
HAZARD_LANGUAGE: unstable stacked cargo, exposed cable trench, thorn growth
PALETTE: dark olive, faded cargo red and blue, asphalt grey, amber work light
```

### 5. 低温冷库 / `cold_storage`

```text
THEME_NAME: failing industrial cold-storage complex
MATERIALS: insulated white panel, frost, stainless steel, rubber strip curtain
STRUCTURE_LANGUAGE: strict maintenance grid, cold rooms, refrigeration corridors
FLOOR_LANGUAGE: frosted service grid, ice cracks, drainage slots, anti-slip panels
WALL_DOOR_LANGUAGE: insulated panels, freezer doors, fan housings, frozen seals
COVER_LANGUAGE: cold crates, compressor units, stainless preparation tables
LANDMARK_LANGUAGE: giant cooling fan, ammonia compressor bank, emergency thaw station
HAZARD_LANGUAGE: ice patch, refrigerant leak, freezing mist
PALETTE: deep blue-grey, cold white, desaturated steel, cyan status light
```

### 6. 封闭地下实验室 / `underground_lab`

```text
THEME_NAME: sealed underground sensor research laboratory
MATERIALS: dark composite panel, brushed metal, reinforced glass, cable conduit
STRUCTURE_LANGUAGE: circuit-like paths, clean-room locks, sensor arrays
FLOOR_LANGUAGE: embedded cable traces, access panels, diagnostic light nodes
WALL_DOOR_LANGUAGE: sealed segmented walls, decontamination doors, observation windows
COVER_LANGUAGE: instrument racks, specimen cabinets, mobile shield partitions
LANDMARK_LANGUAGE: sensor antenna, containment chamber, central diagnostic core
HAZARD_LANGUAGE: failed containment field, arcing conduit, opaque chemical spill
PALETTE: blue-black, graphite, sterile grey, restrained violet and cyan signals
```

## 首批交付矩阵

每个主题先交以下三张母版：

| 文件 | 生成画布 | 内容 | 运行时目标 |
|---|---:|---|---|
| `tiles_master.png` | 1024×1024 | 地面、墙、门、转角、破损与边缘 | `tiles_<asset_id>.png`，256×256 RGBA |
| `water_master.png` | 1024×1024 | 四帧水面/动态表面与边缘 | `water_<asset_id>.png`，32×32 RGBA |
| `landmarks_master.png` | 1536×512 | 掩体、任务物、撤离与大型地标 | `landmarks_<asset_id>.png`，320×32 RGBA |
| `overlays_master.png` | 1024×512 | 两帧雾、蒸汽、漏水、杂草、霜气或扫描环境叠层 | `overlays_<asset_id>.png`，64×32 RGBA |

生成母版可以较大，便于挑选和修整；运行时目标必须由人工重新像素化并按既有索引
切片，禁止直接缩小整张 AI 图片。

## 保存路径

原始生成物：

```text
artwork/inbox/environment/<asset_id>/tiles_master.png
artwork/inbox/environment/<asset_id>/water_master.png
artwork/inbox/environment/<asset_id>/landmarks_master.png
artwork/inbox/environment/<asset_id>/overlays_master.png
artwork/inbox/environment/<asset_id>/PROMPT.md
artwork/inbox/environment/<asset_id>/generation.json
```

`generation.json` 至少保存：

```json
{
  "model": "",
  "promptVersion": "bukov-environment-v1",
  "seed": "",
  "generatedAt": "",
  "sourcePath": "",
  "usageRights": "personal project cleared"
}
```

处理后的运行时路径：

```text
core/src/main/assets/environment/bukov/tiles_<asset_id>.png
core/src/main/assets/environment/bukov/water_<asset_id>.png
core/src/main/assets/environment/bukov/landmarks_<asset_id>.png
core/src/main/assets/environment/bukov/overlays_<asset_id>.png
```

六个 `<asset_id>`：

```text
fog_depot
rust_works
flooded_bunker
container_yard
cold_storage
underground_lab
```

## 导入验收

1. 更新 `artwork/licenses/ASSET_PROVENANCE.csv`，记录原图、prompt、模型、种子、
   路径、授权和最终 SHA-256。
2. 运行 `bash scripts/bukov_theme_visual_gate.sh`。
3. 六主题分别截取普通房间、任务门、交火、撤离四张实局图。
4. 把截图全部转灰度；不看颜色仍能正确判断至少五个主题，才进入最终人工签字。
5. 任一主题仍明显像地牢、城市地砖换色，或只能靠 HUD 文案辨认，退回重画。
