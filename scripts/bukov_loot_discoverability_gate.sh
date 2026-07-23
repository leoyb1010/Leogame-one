#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "$0")/.." && pwd)"
level="$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/levels/BukovLevel.java"
planner="$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/levels/BukovLooseLootPlanner.java"
visual="$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/bukov/BukovItemSprite.java"
runtime="$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java"
test_source="$repo_dir/core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/bukov/levels/BukovLooseLootPlannerTest.java"

grep -Fq 'BukovLooseLootPlanner.place(this);' "$level"
grep -Fq 'REQUIRED_PLACEMENT_COUNT = 5' "$planner"
grep -Fq 'INTRODUCTION_RADIUS = 12' "$planner"
grep -Fq 'heap.seen = true;' "$planner"
grep -Fq 'case AMMUNITION:' "$planner"
grep -Fq 'case MEDICAL:' "$planner"
grep -Fq 'case SALVAGE:' "$planner"
grep -Fq 'public static final int FRAME_COUNT = 72;' "$visual"
grep -Fq 'case "ammo:ammo_9_standard":' "$visual"
grep -Fq 'return Frame.AMMO_9_STANDARD;' "$visual"
grep -Fq 'case "bandage":' "$visual"
grep -Fq 'return Frame.MEDICAL_BANDAGE;' "$visual"
grep -Fq 'return Frame.SALVAGE;' "$visual"
grep -Fq 'selectVisibleLootHeap(' "$runtime"
grep -Fq 'everyFirstRaidAuthorsFiveVisibleGroundPickupsOutsideSpawn' "$test_source"

echo "Bukov loot discoverability gate passed"
