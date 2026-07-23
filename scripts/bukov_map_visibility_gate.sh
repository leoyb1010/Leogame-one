#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
level="$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/levels/BukovLevel.java"
anchors="$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/levels/BukovAnchorPlanner.java"
scene="$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java"
fog="$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/tiles/FogOfWar.java"
world="$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java"

grep -Fq 'class BukovLevel extends RegularLevel' "$level"
grep -Fq 'STANDARD_ROOM_BUDGET = 31' "$level"
grep -Fq 'validateLockedMissionTraversal' "$level"
grep -Fq 'Q01 archive is unreachable while G01 is locked' "$anchors"
grep -Fq 'G01 traps deployment in a tiny area' "$anchors"
grep -Fq 'G01 does not guard a meaningful post-objective area' "$anchors"
grep -Fq 'rememberBukovVisibility();' "$scene"
grep -Fq 'Dungeon.level.visited[cell] = true;' "$scene"
grep -Fq 'BUKOV_FOG_COLORS' "$fog"
grep -Fq 'updateRealtimeCamera(Game.elapsed);' "$world"
grep -Fq 'private void updateRealtimeCamera(float renderDelta)' "$world"

echo "PASS: Bukov map scale, locked-gate traversal, exploration fog and camera follow source gates"
