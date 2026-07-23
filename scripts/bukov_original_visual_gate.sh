#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
OPERATOR="$REPO_ROOT/core/src/main/assets/sprites/bukov_operator.png"
OPERATOR_MANIFEST="$REPO_ROOT/core/src/main/assets/sprites/bukov_operator_manifest.json"
HERO_SPRITE="$REPO_ROOT/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/HeroSprite.java"
REALTIME_WORLD="$REPO_ROOT/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java"
LANDMARKS="$REPO_ROOT/core/src/main/assets/environment/bukov/first_raid_landmarks.png"
ROGUE="$REPO_ROOT/core/src/main/assets/sprites/rogue.png"
TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/bukov-original-visuals.XXXXXX")"
trap 'rm -rf "$TMP_DIR"' EXIT

node "$SCRIPT_DIR/generate_bukov_operator_sprite.mjs" \
  "$TMP_DIR/operator.png" "$TMP_DIR/operator_manifest.json"
node "$SCRIPT_DIR/generate_bukov_landmarks.mjs" "$TMP_DIR/landmarks.png"

cmp "$OPERATOR" "$TMP_DIR/operator.png"
cmp "$OPERATOR_MANIFEST" "$TMP_DIR/operator_manifest.json"
cmp "$LANDMARKS" "$TMP_DIR/landmarks.png"

operator_probe="$(ffprobe -v error -select_streams v:0 \
  -show_entries stream=width,height,pix_fmt -of csv=p=0 "$OPERATOR")"
landmark_probe="$(ffprobe -v error -select_streams v:0 \
  -show_entries stream=width,height,pix_fmt -of csv=p=0 "$LANDMARKS")"
[[ "$operator_probe" == "384,128,rgba" ]]
[[ "$landmark_probe" == "320,32,rgba" ]]

ffmpeg -hide_banner -loglevel error -i "$OPERATOR" \
  -f rawvideo -pix_fmt rgba "$TMP_DIR/operator.rgba"
ffmpeg -hide_banner -loglevel error -i "$LANDMARKS" \
  -f rawvideo -pix_fmt rgba "$TMP_DIR/landmarks.rgba"
ffmpeg -hide_banner -loglevel error -i "$ROGUE" \
  -f rawvideo -pix_fmt rgba "$TMP_DIR/rogue.rgba"

node - "$TMP_DIR/operator.rgba" "$TMP_DIR/landmarks.rgba" "$TMP_DIR/rogue.rgba" <<'NODE'
import { readFileSync } from "node:fs";

const operator = readFileSync(process.argv[2]);
const landmarks = readFileSync(process.argv[3]);
const rogue = readFileSync(process.argv[4]);
if (operator.length === rogue.length) {
  throw new Error("operator atlas unexpectedly retains the host atlas dimensions");
}

function opaqueInFrame(buffer, canvasWidth, frameX, frameY, frameWidth, frameHeight) {
  let count = 0;
  for (let y = frameY; y < frameY + frameHeight; y += 1) {
    for (let x = frameX; x < frameX + frameWidth; x += 1) {
      if (buffer[(y * canvasWidth + x) * 4 + 3] > 0) count += 1;
    }
  }
  return count;
}

for (let direction = 0; direction < 8; direction += 1) {
  for (let frame = 0; frame < 32; frame += 1) {
    const count = opaqueInFrame(operator, 384, frame * 12, direction * 15, 12, 15);
    if (count < 8) throw new Error(`operator direction ${direction} frame ${frame} is empty or unreadable: ${count}`);
  }
}
for (let frame = 0; frame < 10; frame += 1) {
  const count = opaqueInFrame(landmarks, 320, frame * 32, 0, 32, 32);
  if (count < 40) throw new Error(`landmark frame ${frame} is empty or unreadable: ${count}`);
}
NODE

node - "$OPERATOR_MANIFEST" <<'NODE'
import { readFileSync } from "node:fs";
const manifest = JSON.parse(readFileSync(process.argv[2], "utf8"));
if (manifest.frameCount !== 32 || manifest.directions.length !== 8) {
  throw new Error("operator manifest must define 32 frames in eight directions");
}
const expectedStates = [
  "idle", "move", "aim", "fire", "reload",
  "hit", "medical", "down", "extract",
];
if (manifest.states.map((state) => state.name).join(",") !== expectedStates.join(",")) {
  throw new Error("operator action-state contract is incomplete");
}
for (const direction of manifest.directions) {
  if (direction.footAnchor[0] !== 6 || direction.footAnchor[1] !== 14) {
    throw new Error(`unstable foot anchor for ${direction.name}`);
  }
  if (!Array.isArray(direction.muzzleAnchor)
      || direction.muzzleAnchor.length !== 2) {
    throw new Error(`missing muzzle anchor for ${direction.name}`);
  }
}
NODE

if rg -q "createReadStream|rogue\\.png|tiles_city\\.png" \
  "$SCRIPT_DIR/generate_bukov_operator_sprite.mjs" \
  "$SCRIPT_DIR/generate_bukov_landmarks.mjs"; then
  if rg -q "createReadStream|rogue\\.png|tiles_city\\.png" \
    "$SCRIPT_DIR/generate_bukov_operator_sprite.mjs" \
    "$SCRIPT_DIR/generate_bukov_landmarks.mjs"; then
    echo "original visual generators must not read source images" >&2
    exit 1
  fi
fi

for contract in \
  'idle.frames( film, 0, 1 )' \
  'run.frames( film, 2, 3, 4, 5, 6, 7 )' \
  'aim.frames( film, 8, 9 )' \
  'fire.frames( film, 10, 11, 12, 8 )' \
  'reload.frames( film, 13, 14, 15, 16, 8 )' \
  'hit.frames( film, 17, 18, 19, 8 )' \
  'medical.frames( film, 20, 21, 22, 23, 8 )' \
  'die.frames( film, 24, 25, 26, 27, 27 )' \
  'extract.frames( film, 28, 29, 30, 31, 28 )'; do
  rg -Fq "$contract" "$HERO_SPRITE"
done
rg -Fq 'setBukovRealtimeOrientation(' "$HERO_SPRITE"
rg -Fq 'setBukovRealtimeOrientation(' "$REALTIME_WORLD"
rg -Fq 'BukovFacing8.resolve(' "$HERO_SPRITE"

echo "Bukov original visual gate passed."
