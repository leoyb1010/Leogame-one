# Bukov UI asset atlas

`core/src/main/assets/interfaces/bukov_ui.png` is the shared Bukov tactical UI
skin for macOS and iOS. It is deterministic project-original pixel art, not a
recolor or extraction of the upstream dungeon/class UI.

The 112×32 RGBA atlas contains four 16×16 nine-patch surfaces and seven 16×16
status icons:

- `PANEL`, `PANEL_RAISED`;
- `BUTTON`, `BUTTON_PRESSED`;
- `STATUS_ACTION`, `STATUS_LOOT`, `STATUS_EXTRACT`, `STATUS_DANGER`;
- `STATUS_BLEEDING`, `STATUS_FRACTURE`, `STATUS_CONCUSSION`.

The three injury icons use independent silhouettes (droplet, broken bone and
shock ring) plus independent token colors, so the HUD remains distinguishable
without relying on color alone.

Exact coordinates, margins, SHA-256, palette source, generator and rights
status are recorded in
`core/src/main/assets/interfaces/bukov_ui_manifest.json`. The runtime entry
point is `BukovUiAssets`; it forces nearest-neighbour sampling and falls back
to a caller-provided named UI color when the optional atlas cannot be loaded.

The title screen, first-run briefing and deployment/loading boundary use this
shared factory. Other Bukov surfaces can adopt the same frames without copying
texture coordinates or reviving the legacy Leo/original dungeon chrome.

Regenerate and verify without Gradle:

```sh
scripts/bukov_ui_asset_gate.sh
```

The gate regenerates both atlas and manifest into a temporary directory,
compares them byte-for-byte, checks manifest coverage and provenance, then
verifies the three player-visible wiring points and the fallback path.
