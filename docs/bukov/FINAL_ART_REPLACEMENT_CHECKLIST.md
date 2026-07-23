# 《逃离布科夫》最终美术替换清单

更新日期：2026-07-23

这份清单只记录玩家可见、仍带有 Shattered Pixel Dungeon 或旧
Leogame-one 视觉特征的图片与精灵。它不把“代码已可玩”误报成“最终美术已完成”。
替换时必须保持透明通道、图集帧尺寸、帧顺序和像素锚点不变；若要改变规格，应同步
修改对应 Sprite/Tilemap 类并补截图验收。

## P0：首轮验收前必须替换

| 玩家路径 | 当前文件 | 最终目标 | 验收 |
|---|---|---|---|
| 标题横屏背景 | `core/src/main/assets/splashes/title/leo_landscape.jpg` | 布科夫封锁区横屏主视觉，无旧城堡/地牢符号 | 16:9 与 iPad 横屏不裁掉主体；无烘焙按钮文字 |
| 标题竖屏背景 | `core/src/main/assets/splashes/title/leo_portrait.jpg` | 与横屏同一视觉体系的竖屏构图 | iPhone 刘海与底部安全区不遮挡主体 |
| 标题徽记 | `core/src/main/assets/interfaces/leo_title_emblem.png` | “逃离布科夫 / ESCAPE FROM BUKOV”透明徽记 | 1x/2x 显示清晰，无旧 Leo/狮印字样 |
| 标题菜单底板 | `core/src/main/assets/interfaces/leo_menu_panel.png` | 黑金翡翠战术终端面板 | 九宫格边缘无拉伸裂缝 |
| 首关地表 | `core/src/main/assets/environment/tiles_sewers.png` | 废弃工业区、仓库、道路与围挡像素图集 | 保持原 tile 单元和索引；无下水道砖墙观感 |
| 首关水面 | `core/src/main/assets/environment/water0.png` | 工业积水/污染水动画 | 全动画帧无跳边、无透明黑边 |
| 地形覆盖 | `core/src/main/assets/environment/terrain_features.png` | 路障、草丛、碎石、箱体等封锁区覆盖物 | 遮挡层级正确，不挡住拾取/撤离反馈 |
| 玩家行动员 | `core/src/main/assets/sprites/warrior.png` | 第一名持枪行动员完整运行时图集 | `idle/walk/aim/fire/reload/hit/death`方向和锚点一致 |
| 首关常规敌人 | `sprites/rat.png`、`snake.png`、`crab.png`、`slime.png` | 匪徒步枪手、冲锋手、游荡者、重装敌人 | 轮廓一眼可分；命中与死亡帧完整 |
| 首关精英/Boss | `sprites/guard.png`、`brute.png`、`goo.png` | 精英守卫、盾兵、白线 Boss | Boss 阶段变化、弱点与攻击前摇可读 |
| 战利品图集 | `core/src/main/assets/sprites/items.png` | 枪械、弹药、护甲、医疗品、任务物资 | 仓库、地面掉落、结算页使用同一物品语义 |
| 战利品图标 | `core/src/main/assets/sprites/item_icons.png` | 与战利品图集一致的高辨识图标 | 小尺寸不糊，不出现法杖/卷轴等地牢语义 |
| 行动员头像 | `core/src/main/assets/sprites/avatars.png` | 第一批行动员头像 | HUD、结算与档案页身份一致 |
| HUD 主图集 | `interfaces/status_pane.png`、`toolbar.png`、`icons.png`、`hero_icons.png` | 实时射击 HUD、弹匣/护甲/互动/撤离图标 | 手机安全区内可读；不出现回合制快捷栏语义 |
| 交互窗体 | `interfaces/chrome.png`、`menu_button.png`、`menu_pane.png` | 战术终端式窗体与菜单 | 文字对比度达标，触控热区不小于现有值 |

## P1：行动员与区域扩展时替换

### 行动员选择

- `splashes/warrior.jpg` → 先锋行动员档案背景
- `splashes/mage.jpg` → 技术员档案背景
- `splashes/rogue.jpg` → 侦察员档案背景
- `splashes/huntress.jpg` → 游骑兵档案背景
- `splashes/duelist.jpg` → 突击手档案背景
- `splashes/cleric.jpg` → 医疗员档案背景
- `sprites/warrior.png`、`mage.png`、`rogue.png`、`huntress.png`、
  `duelist.png`、`cleric.png` → 六名行动员统一规格像素动画图集

### 后续区域

- `environment/tiles_prison.png` + `water1.png` + `splashes/prison.jpg`
  → 检查站/拘留区
- `environment/tiles_caves.png`、`tiles_caves_crystal.png`、
  `tiles_caves_gnoll.png` + `water2.png` + `splashes/caves.jpg`
  → 地下矿区/隧道
- `environment/tiles_city.png` + `water3.png` + `splashes/city.jpg`
  → 废弃城区
- `environment/tiles_halls.png` + `water4.png` + `splashes/halls.jpg`
  → 军事核心区
- `environment/custom_tiles/*.png` → 各区域 Boss 房、任务点、撤离口与特殊地形；
  必须逐张维持原 tile 索引或同时迁移关卡引用。

### 敌人与 NPC

以下 `core/src/main/assets/sprites/` 图集在相应敌人进入主流程前必须替换：

- 人形敌人：`thief.png`、`bandit` 对应的现有复用图集、`guard.png`、
  `brute.png`、`shaman.png`、`monk.png`、`warlock.png`、`necromancer.png`
- 机械/固定火力：`dm100.png`、`dm200.png`、`dm300.png`、`pylon.png`、
  `red_sentry.png`、`crystal_sentry` 对应图集
- 特殊敌人/Boss：`tengu.png`、`king.png`、`demon.png`、`yog.png`、
  `yog_fists.png`、`spawner.png`
- 中立角色：`shopkeeper.png`、`blacksmith.png`、`wandmaker.png`、
  `ghost.png`、`ratking.png`
- 其他仍可能被经典生成器带入的怪物图集：`bat.png`、`bee.png`、
  `skeleton.png`、`ghoul.png`、`golem.png`、`eye.png`、`scorpio.png`、
  `spinner.png`、`swarm.png`、`wraith.png`、`succubus.png`、`ripper.png`、
  `piranha.png`、`mimic.png`、`elemental.png`。

## P2：统一风格与清除旧品牌

- 删除或替换旧 Leo 皮肤：`interfaces/leo_dialog_frame.png`、
  `leo_button_normal.png`、`leo_button_pressed.png`。经典次级入口若继续使用，
  也应改成不带个人旧品牌的中性皮肤。
- `interfaces/banners.png`、`badges.png`、`locked_badge.png`、
  `talent_button.png`、`talent_icons.png`：只有对应系统仍保留在布科夫模式时才重绘；
  若主流程已禁用，优先不加载而不是为了兼容旧界面继续扩充。
- `effects/effects.png`、`specks.png`、`spell_icons.png`、`text_icons.png`：
  枪口焰、曳光、命中、护甲破损、搜索和撤离反馈完成后替换；不应在主流程出现
  法术/符文图标。
- `splashes/title/archs.png`、`back_clusters.png`、`mid_mixed.png`、
  `front_small.png`：当前标题页不直接使用时可延后；若重新启用分层视差，必须先
  改成布科夫工业城市轮廓。

## 素材交付规格

- 图集：PNG、透明背景、最近邻采样、整数像素位置；禁止 JPEG 压缩和半像素锚点。
- 背景：无文字、无按钮、无 HUD 烘焙；横竖屏分别构图，保留标题与菜单安全区。
- 角色：统一左上光源、1px 轮廓、同一脚底锚点；每个动作必须有明确帧表。
- 命名：提交到 `artwork/inbox/<category>/`，同时附 prompt、模型、种子、
  原图保存路径与使用授权；处理后再进入 `core/src/main/assets/`。
- 验收：桌面、iPhone 与 iPad 各截一张标题、藏身处、首关战斗、搜索、撤离和结算图；
  任何一张仍明显像地牢、法术或中世纪职业，即视为未完成。
