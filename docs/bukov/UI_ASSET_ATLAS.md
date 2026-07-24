# Bukov UI asset atlas

`core/src/main/assets/interfaces/bukov_ui.png` is the shared Bukov tactical UI
skin for macOS and iOS. It is deterministic project-original pixel art, not a
recolor or extraction of the upstream dungeon/class UI.

The 256×64 RGBA atlas contains seven 16×16 nine-patch surfaces, four rarity
frames, eight HUD glyphs, seven status icons and two 48×16 settlement stamps:

- `PANEL`, `PANEL_RAISED`;
- `BUTTON`, `BUTTON_PRESSED`, `BUTTON_FOCUSED`, `BUTTON_DISABLED`,
  `ROW_FOCUSED`;
- `RARITY_COMMON`, `RARITY_UNCOMMON`, `RARITY_RARE`, `RARITY_LEGENDARY`;
- `HUD_HEALTH`, `HUD_ARMOR`, `HUD_AMMO`, `HUD_INTERACT`, `HUD_OBJECTIVE`,
  `HUD_TIMER`, `HUD_SOUND`, `HUD_HIT`;
- `STATUS_ACTION`, `STATUS_LOOT`, `STATUS_EXTRACT`, `STATUS_DANGER`;
- `STATUS_BLEEDING`, `STATUS_FRACTURE`, `STATUS_CONCUSSION`;
- `STAMP_EXTRACTED`, `STAMP_LOST`.

The three injury icons use independent silhouettes (droplet, broken bone and
shock ring) plus independent token colors, so the HUD remains distinguishable
without relying on color alone.

Focus and disabled buttons also differ by pixel pattern rather than color
alone. Rarity frames keep independent corner and edge silhouettes. The two
settlement stamps use open-route and slashed-loss marks, so success and death
remain distinct in monochrome captures.

Exact coordinates, margins, SHA-256, palette source, generator and rights
status are recorded in
`core/src/main/assets/interfaces/bukov_ui_manifest.json`. The runtime entry
point is `BukovUiAssets`; it forces nearest-neighbour sampling and falls back
to a caller-provided named UI color when the optional atlas cannot be loaded.

The title screen, first-run briefing, deployment/loading boundary, hideout
loadout rows, deployment actions, raid HUD and success/death settlement all use
this shared factory. No caller copies texture coordinates, and no legacy
Leo/original dungeon chrome is reachable through these player-critical paths.

Regenerate and verify without Gradle:

```sh
scripts/bukov_ui_asset_gate.sh
```

The gate regenerates both atlas and manifest into a temporary directory,
compares them byte-for-byte, checks manifest coverage and provenance, then
verifies title/deployment, rarity, focus/disabled, HUD and settlement wiring
plus the safe solid-color fallback path.
