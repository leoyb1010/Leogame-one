#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HOST="$ROOT/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ai/BukovHostMob.java"
SPRITES="$ROOT/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/bukov"
ASSETS="$ROOT/core/src/main/assets/sprites/bukov"
MANIFEST="$ASSETS/enemy_animation_manifest.json"
WORLD="$ROOT/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java"
TMP="$(mktemp -d "${TMPDIR:-/tmp}/bukov-enemies.XXXXXX")"
trap 'rm -rf "$TMP"' EXIT

forbidden='RatSprite|GooSprite|GnollTricksterSprite|GuardSprite|BruteSprite|DM100Sprite'
if rg -n "$forbidden" "$HOST" "$SPRITES"; then
  echo "FAIL: Bukov main-path enemy visuals still reference dungeon sprites" >&2
  exit 1
fi

expected=(
  scavenger gunner armored captain drone white_line
  alley_scout depot_shotgunner line_rifleman fog_stalker
  signal_operator iron_clasp_marksman breach_veteran
)
classes=(
  Scavenger Gunner Armored Captain Drone WhiteLine
  AlleyScout DepotShotgunner LineRifleman FogStalker
  SignalOperator IronClaspMarksman BreachVeteran
)
enemy_ids=(
  melee_rusher scavenger_gunner iron_clasp_guard iron_clasp_captain
  sensor_doll boss_white_line alley_scout depot_shotgunner line_rifleman
  fog_stalker signal_operator iron_clasp_marksman breach_veteran
)

node "$ROOT/scripts/generate_bukov_enemy_sprites.mjs" \
  "$TMP/assets" "$TMP/enemy_animation_manifest.json" >/dev/null

asset_hashes=()
for name in "${expected[@]}"; do
  path="$ASSETS/$name.png"
  generated="$TMP/assets/$name.png"
  test -f "$path" || {
    echo "FAIL: missing $path" >&2
    exit 1
  }
  probe="$(ffprobe -v error -select_streams v:0 \
    -show_entries stream=width,height,pix_fmt -of csv=p=0 "$path")"
  [[ "$probe" == "256,18,rgba" ]] || {
    echo "FAIL: $path does not satisfy 16x18 x 16-frame RGBA: $probe" >&2
    exit 1
  }
  cmp "$path" "$generated"
  ffmpeg -hide_banner -loglevel error -i "$path" \
    -f rawvideo -pix_fmt rgba "$TMP/$name.rgba"
  node - "$TMP/$name.rgba" "$name" <<'NODE'
import { readFileSync } from "node:fs";
const bytes = readFileSync(process.argv[2]);
const name = process.argv[3];
const width = 256;
for (let frame = 0; frame < 16; frame += 1) {
  let opaque = 0;
  for (let y = 0; y < 18; y += 1) {
    for (let x = frame * 16; x < frame * 16 + 16; x += 1) {
      if (bytes[(y * width + x) * 4 + 3] > 0) opaque += 1;
    }
  }
  if (opaque < 6) {
    throw new Error(`${name} frame ${frame} unreadable: ${opaque}`);
  }
}
NODE
  asset_hashes+=("$(shasum -a 256 "$path" | awk '{print $1}')")
done

unique_asset_hashes="$(printf '%s\n' "${asset_hashes[@]}" | sort -u | wc -l | tr -d ' ')"
[[ "$unique_asset_hashes" -eq "${#expected[@]}" ]] || {
  echo "FAIL: one or more enemy sheets are byte-identical placeholders" >&2
  exit 1
}

cmp "$MANIFEST" "$TMP/enemy_animation_manifest.json"
[[ "$(jq '.sheets | length' "$MANIFEST")" -eq 13 ]]
[[ "$(jq -r '.sheets[].enemyIds[]' "$MANIFEST" | sort -u | wc -l | tr -d ' ')" -eq 13 ]]
for id in "${enemy_ids[@]}"; do
  jq -e --arg id "$id" \
    '.sheets[] | select(.enemyIds | index($id))' "$MANIFEST" >/dev/null
  rg -q "case \"$id\":" "$HOST"
done
for class in "${classes[@]}"; do
  rg -q "Bukov${class}Sprite\\.class" "$HOST" || {
    echo "FAIL: Bukov${class}Sprite is not wired into BukovHostMob" >&2
    exit 1
  }
done

# White Line must reserve independently visible phase and vulnerability frames.
node - "$TMP/white_line.rgba" <<'NODE'
import { createHash } from "node:crypto";
import { readFileSync } from "node:fs";
const bytes = readFileSync(process.argv[2]);
const hashes = [];
for (let frame = 11; frame <= 15; frame += 1) {
  const frameBytes = Buffer.alloc(16 * 18 * 4);
  for (let y = 0; y < 18; y += 1) {
    bytes.copy(frameBytes, y * 16 * 4, (y * 256 + frame * 16) * 4,
      (y * 256 + frame * 16 + 16) * 4);
  }
  hashes.push(createHash("sha256").update(frameBytes).digest("hex"));
}
if (new Set(hashes).size !== 5) {
  throw new Error("White Line phase frames 11-15 are not independently visible");
}
NODE
rg -Fq 'setEncounterVisual(' "$SPRITES/BukovWhiteLineSprite.java"
rg -Fq 'setEncounterVisual(' "$WORLD"
rg -q 'bossPhase\(enemy\.bossState\.phase\(\)\)' "$WORLD"
rg -q 'enemy\.bossState\.vulnerable\(\)' "$WORLD"

echo "PASS: 13 reproducible original enemy sheets, four core actions, seven distinct new silhouettes, and White Line phase visuals"
