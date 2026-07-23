# Bukov Item and Interaction Atlas

Project-original pixel artwork generated from geometric primitives. It does
not sample, trace, recolor, or derive visible pixels from the host item atlas.

Generation:

```sh
node scripts/generate_bukov_item_visuals.mjs
```

Contract:

- Atlas: `sprites/bukov/items_interactions.png`
- Manifest: `sprites/bukov/items_interactions_manifest.json`
- Image: 1152x16, 8-bit RGBA, non-interlaced
- Frames: seventy-two horizontal 16x16 frames
- Palette: 20 RGBA colors, shared outline and material palette
- Alpha: 7077 opaque, 84 translucent, 11271 transparent pixels
- SHA-256: `24889647037db162f70b99dd5fa9ae77d3c0b91cd24a438416ccb5d68b3499fe`

| Frames | Category | Logical icons |
|---:|---|---:|
| 0-17 | Firearms | 18 |
| 18-25 | Ammunition | 8 |
| 26-28 | Armor | 3 |
| 29-30 | Backpacks | 2 |
| 31-38 | Medical items and field tools | 8 |
| 39-68 | Loot and mission items | 30 |
| 69-71 | Extraction and mission interactions | 3 |

Every logical definition has a manifest row and an explicit
`BukovItemSprite.Frame` mapping. The atlas deliberately retains transparent
space around each silhouette so the icons remain readable on inventory,
ground-loot, controller-focus, and high-contrast backgrounds.
