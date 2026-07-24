#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HOST="$ROOT/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ai/BukovHostMob.java"
SPRITES="$ROOT/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/bukov"
ASSETS="$ROOT/core/src/main/assets/sprites/bukov"
MANIFEST="$ASSETS/enemy_animation_manifest.json"
WORLD="$ROOT/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java"
PROVENANCE="$ROOT/artwork/licenses/ASSET_PROVENANCE.csv"
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
  [[ "$probe" == "336,18,rgba" ]] || {
    echo "FAIL: $path does not satisfy 16x18 x 21-frame RGBA: $probe" >&2
    exit 1
  }
  cmp "$path" "$generated"
  ffmpeg -hide_banner -loglevel error -i "$path" \
    -f rawvideo -pix_fmt rgba "$TMP/$name.rgba"
  node - "$TMP/$name.rgba" "$name" <<'NODE'
import { readFileSync } from "node:fs";
const bytes = readFileSync(process.argv[2]);
const name = process.argv[3];
const { createHash } = await import("node:crypto");
const width = 336;
const frameHashes = [];
const uprightBottomRows = [];
for (let frame = 0; frame < 21; frame += 1) {
  let opaque = 0;
  let bottomRow = -1;
  const frameBytes = Buffer.alloc(16 * 18 * 4);
  for (let y = 0; y < 18; y += 1) {
    for (let x = frame * 16; x < frame * 16 + 16; x += 1) {
      const source = (y * width + x) * 4;
      const target = (y * 16 + x - frame * 16) * 4;
      bytes.copy(frameBytes, target, source, source + 4);
      if (bytes[source + 3] > 0) {
        opaque += 1;
        bottomRow = Math.max(bottomRow, y);
      }
    }
  }
  if (opaque < 6) {
    throw new Error(`${name} frame ${frame} unreadable: ${opaque}`);
  }
  frameHashes.push(createHash("sha256").update(frameBytes).digest("hex"));
  if (frame < 8 || frame >= 11) uprightBottomRows.push(bottomRow);
}
if (new Set(uprightBottomRows).size !== 1) {
  throw new Error(`${name} upright foot anchor drifts: ${uprightBottomRows}`);
}
for (const [action, frames] of Object.entries({
  hit: [11, 12],
  special: [13, 14, 15],
})) {
  const hashes = frames.map((frame) => frameHashes[frame]);
  if (new Set(hashes).size !== frames.length) {
    throw new Error(`${name} ${action} frames are not independently visible`);
  }
  if (hashes.some((hash) => frameHashes.slice(0, 8).includes(hash))) {
    throw new Error(`${name} ${action} reuses idle/walk/attack art`);
  }
}
NODE
  asset_hash="$(shasum -a 256 "$path" | awk '{print $1}')"
  rg -Fq "$asset_hash" "$PROVENANCE" || {
    echo "FAIL: $path hash is missing from asset provenance" >&2
    exit 1
  }
  asset_hashes+=("$asset_hash")
done

unique_asset_hashes="$(printf '%s\n' "${asset_hashes[@]}" | sort -u | wc -l | tr -d ' ')"
[[ "$unique_asset_hashes" -eq "${#expected[@]}" ]] || {
  echo "FAIL: one or more enemy sheets are byte-identical placeholders" >&2
  exit 1
}

cmp "$MANIFEST" "$TMP/enemy_animation_manifest.json"
[[ "$(jq '.schemaVersion' "$MANIFEST")" -eq 2 ]]
[[ "$(jq '.frameCount' "$MANIFEST")" -eq 21 ]]
[[ "$(jq '.sheets | length' "$MANIFEST")" -eq 13 ]]
[[ "$(jq -r '.sheets[].enemyIds[]' "$MANIFEST" | sort -u | wc -l | tr -d ' ')" -eq 13 ]]
[[ "$(jq '[.sheets[] | select(.specialAction == "reload")] | length' "$MANIFEST")" -eq 8 ]]
[[ "$(jq '[.sheets[] | select(.specialAction == "rush")] | length' "$MANIFEST")" -eq 2 ]]
[[ "$(jq '[.sheets[] | select(.specialAction == "scan")] | length' "$MANIFEST")" -eq 2 ]]
[[ "$(jq '[.sheets[] | select(.specialAction == "phase_cast")] | length' "$MANIFEST")" -eq 1 ]]
jq -e '.actions == {
  "idle": [0, 1],
  "attack": [2, 3],
  "walk": [4, 7],
  "death": [8, 10],
  "hit": [11, 12],
  "special": [13, 15],
  "bossEncounterPhase": [16, 20]
}' "$MANIFEST" >/dev/null
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
for (let frame = 16; frame <= 20; frame += 1) {
  const frameBytes = Buffer.alloc(16 * 18 * 4);
  for (let y = 0; y < 18; y += 1) {
    bytes.copy(frameBytes, y * 16 * 4, (y * 336 + frame * 16) * 4,
      (y * 336 + frame * 16 + 16) * 4);
  }
  hashes.push(createHash("sha256").update(frameBytes).digest("hex"));
}
if (new Set(hashes).size !== 5) {
  throw new Error("White Line phase frames 16-20 are not independently visible");
}
NODE
rg -Fq 'setEncounterVisual(' "$SPRITES/BukovWhiteLineSprite.java"
rg -Fq 'setEncounterVisual(' "$WORLD"
rg -q 'bossPhase\(phase\)' "$WORLD"
rg -q 'enemy\.bossState\.vulnerable\(\)' "$WORLD"

rg -Fq 'realtimeHitReaction()' "$SPRITES/BukovEnemySprite.java"
rg -Fq 'hit.frames(frames, 11, 12)' "$SPRITES/BukovEnemySprite.java"
rg -Fq 'special.frames(frames, 13, 14, 15)' "$SPRITES/BukovEnemySprite.java"
rg -Fq 'playEnemyRush(enemy);' "$WORLD"
rg -Fq 'playEnemyReload(enemy);' "$WORLD"
rg -Fq 'playEnemyScan(source);' "$WORLD"
rg -Fq 'realtimePhaseCast(hero.pos)' "$WORLD"

echo "PASS: 13 reproducible 21-frame enemy sheets with stable anchors, idle/walk/rush-or-special/attack/hit/death runtime wiring, and White Line phase visuals"
