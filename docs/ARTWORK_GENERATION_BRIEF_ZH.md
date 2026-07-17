# 《Leo的地牢围攻》高清素材生成与投放规范

## 1. 统一视觉方向

核心识别物：地下堡垒、青绿色灵火、旧金与深铁、狮首印记、孤独冒险者。整体采用高完成度暗黑奇幻插画，不照搬任何现有游戏 UI、角色或 Logo。

统一母提示词：

```text
Premium dark-fantasy dungeon crawler key art for an original game called "Leo's Dungeon Assault". An immense ancient underground fortress, weathered black stone, oxidized bronze, subtle lion-sigil architecture, emerald ghost fire as the signature light, cinematic volumetric lighting, deep spatial layers, tactile materials, strong silhouette readability, sophisticated composition, restrained dark green and antique gold palette, sharp focal details, clean edges, high contrast but preserved shadow detail, production-ready game art, original worldbuilding, no existing franchise references, no UI, no logo, no text, no watermark, no signature, crisp and high-definition.
```

统一负面提示词：

```text
blurry, soft focus, low resolution, muddy details, excessive bloom, fog covering the subject, flat lighting, oversaturated neon, generic mobile game ad, chibi, anime face, photorealistic modern clothing, sci-fi technology, copied game character, existing franchise logo, letters, Chinese characters, English text, watermark, signature, frame, UI button
```

注意：生成模型容易把中文标题画错。背景和徽记必须要求 `no text`；“Leo的地牢围攻”文字由程序或后期排版添加，不要烘焙进背景图。

## 2. 需要补充的素材

### A. App Icon

输出：PNG，1024×1024，sRGB，不透明背景，不要圆角，不要文字。

专用提示词：

```text
Square app icon for an original premium dark-fantasy dungeon game. A bold ancient lion-head crest forged from dark iron and antique gold, a single emerald ghost flame glowing inside the crest, subtle dungeon arch behind it, centered iconic silhouette, readable at 32 pixels, strong foreground-background separation, minimal small details, dramatic rim light, polished game icon, no text, no letters, no border, no rounded corners, no transparency.
```

投放路径：

`artwork/inbox/app-icon/app-icon-1024.png`

### B. 标题页背景

输出两张无文字 PNG/JPG：

- 竖屏：2048×2732，主体避开中间标题区和下方按钮区。
- 横屏：2732×2048，主体避开画面中央 45% 区域。

专用提示词：

```text
Ancient underground citadel entrance seen from inside a ruined dungeon, monumental arches, broken stairs descending into darkness, two distant emerald braziers, subtle lion emblems carved into stone, antique gold metalwork, cinematic dark fantasy, layered depth, crisp masonry and debris, clear negative space in the upper-middle area for a Chinese game title, clear dark space in the lower area for menu buttons, no characters close to camera, no text, no logo, no UI, high-definition production art.
```

投放路径：

- `artwork/inbox/title/title-background-portrait.png`
- `artwork/inbox/title/title-background-landscape.png`

### C. 狮首标题徽记

输出：2048×1024 透明 PNG，只生成徽记和装饰，不生成文字。

专用提示词：

```text
Transparent ornamental title emblem for a premium dark-fantasy dungeon game, symmetrical antique-gold lion crest, dark iron edges, small emerald flame accents, weathered but elegant, designed to frame a six-character Chinese title added later, wide horizontal composition, crisp alpha edges, no background, no text, no letters, no watermark.
```

投放路径：`artwork/inbox/title/title-emblem-transparent.png`

### D. 章节与英雄插画

每张先生成 2560×1440 主图，保留原始无损文件；接入游戏时统一优化为 1600×900 JPG。所有图保持角色脸部、武器方向和主光源清晰。

文件与场景变量：

| 文件 | 场景变量 |
|---|---|
| `warrior.png` | scarred armored warrior with a worn sword, sewer gate and emerald torchlight |
| `mage.png` | robed mage holding a charged staff, arcane library buried under the fortress |
| `rogue.png` | hooded rogue crossing a prison rooftop, moonlike shaft of underground light |
| `huntress.png` | huntress with a spirit bow, overgrown cavern garden and luminous roots |
| `duelist.png` | agile duelist in a ruined dwarven hall, poised before a mechanical guardian |
| `cleric.png` | battle cleric with a holy book and mace, ancient crypt with pale gold light |
| `sewers.png` | vast sewer cistern, stone bridges, toxic water and distant creature silhouette |
| `prison.png` | abandoned subterranean prison, iron cells, chains and one open gate |
| `caves.png` | crystalline mining cavern, broken lifts, emerald mineral glow |
| `city.png` | lost dwarven city beneath the earth, monumental avenues and bronze machinery |
| `halls.png` | infernal final halls, black stone cathedral, red abyss below and emerald sigil above |

把对应“场景变量”追加到统一母提示词末尾，一图只表现一个明确焦点。

投放目录：`artwork/inbox/splashes/`

## 3. UI 高清化素材

当前战斗角色、地砖和物品仍是有意设计的像素画，不应使用普通 AI 放大后直接覆盖，否则会破坏碰撞格和动画帧。下一轮优先补充独立 UI 面板，而不是整包替换 sprite sheet。

第一批 UI 可投放：

- `artwork/inbox/ui/menu-panel-9slice.png`：512×512 透明 PNG，四角装饰保持 48px 安全区。
- `artwork/inbox/ui/button-normal.png`：1024×256 透明 PNG。
- `artwork/inbox/ui/button-pressed.png`：1024×256 透明 PNG。
- `artwork/inbox/ui/dialog-frame-9slice.png`：1024×1024 透明 PNG，边缘清晰、中心可拉伸。

UI 提示词：

```text
Modular dark-fantasy game UI panel, blackened iron and worn antique-gold trim, restrained emerald rune accents, subtle lion motif, orthographic front view, perfectly symmetrical, clean sharp alpha edges, empty readable center, designed for 9-slice scaling, premium but not ornate, no text, no icons, no background, transparent PNG.
```

## 4. 交付检查

- 不要把文字、按钮或水印画进场景图。
- 不要自行裁圆角或添加 Apple 图标遮罩。
- PNG 使用 sRGB；JPG 品质不低于 92。
- 同系列保持同一角色服装、狮首徽记、青绿火焰色值和金属材质。
- 原始大图只放 `artwork/inbox/`，由项目脚本生成游戏内优化版本，避免反复压缩。
