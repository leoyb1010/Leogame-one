# Third-party borrowing register

This register separates code actually present in the repository from projects
that were only studied as design references. A reference row does not authorize
copying its code, data, or assets.

## Leogame-one / Shattered Pixel Dungeon

- Source: Leogame-one, derived from Shattered Pixel Dungeon and Pixel Dungeon.
- Repository: `https://github.com/leoyb1010/Leogame-one`
- Upstream: `https://github.com/00-Evan/shattered-pixel-dungeon`
- Baseline: `99a084d72fc4b399117be0369ccc2bdd41151879`
- License: GPL-3.0; see `LICENSE.txt`.
- Usage: sole host project, directly modified.
- Local scope: existing host source, rendering, procedural rooms, Item/Heap,
  Bundle persistence, desktop and iOS launchers.
- Assets included: existing upstream assets remain under their existing
  notices and in-game credits.
- Reviewed: 2026-07-23.

## gdx-ai

- Repository: `https://github.com/libgdx/gdx-ai`
- Version recorded by the approved v2.0 plan: `1.8.2`.
- License: Apache-2.0.
- Usage status: design reference only in the current implementation.
- Current result: Bukov AI uses small local deterministic controllers; no
  gdx-ai dependency or copied source has been added.
- Assets included: no.
- Reviewed: 2026-07-23.

## Delver Engine

- Repository: `https://github.com/Interrupt/delverengine`
- Version recorded by the approved v2.0 plan: `v1.4.0`.
- License: zlib for the engine source; game data/assets are excluded.
- Files studied by the plan: `Gun.java`, `Projectile.java`, `CachePools.java`,
  `ItemManager.java`.
- Usage status: mechanics vocabulary and design reference only.
- Current result: FireControl, hitscan, projectile and registry code were
  independently implemented for this project; no Delver source or game data is
  included.
- Assets included: no.
- Reviewed: 2026-07-23.

## Mindustry

- Repository: `https://github.com/Anuken/Mindustry`
- Version recorded by the approved v2.0 plan: `v159.7`.
- License: GPL-3.0.
- Files studied by the plan: weapon definitions, bullet types, collisions,
  damage and pathfinding.
- Usage status: architecture reference only.
- Current result: no Arc dependency and no Mindustry source or assets included.
- Assets included: no.
- Reviewed: 2026-07-23.

## Seventh

- Repository: project recorded as `Seventh` in the approved v2.0 plan.
- Revision recorded by the plan: `bc0a258e...`.
- License: GPL-2.0.
- Usage status: research-only because direct combination with this GPL-3.0 host
  has unresolved compatibility risk.
- Current result: no source, data, dependency, or asset included.
- Assets included: no.
- Reviewed: 2026-07-23.

## User-provided Bukov artwork

- Source: local Unity prototype owned/provided by the user.
- Imported file:
  `Assets/Bukov/Branding/bukov_app_icon_master_v01.png`.
- Destination: `artwork/inbox/app-icon/app-icon-1024.png`, then mechanically
  resized into desktop and iOS icon sets by `scripts/process_leo_artwork.py`.
- Master SHA-256:
  `44061f43d5635a7f125a5fcd9d7e0926bced6c3e770a9f3c1f694e087d8da83e`.
- Usage: application icon only.
- External license: none asserted; user-provided/private project asset.
- Reviewed: 2026-07-23.
