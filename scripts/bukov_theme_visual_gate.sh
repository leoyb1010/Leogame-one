#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ASSET_DIR="$REPO_ROOT/core/src/main/assets/environment/bukov"
THEMES_JSON="$REPO_ROOT/core/src/main/assets/bukov/content/themes.json"
LEVEL="$REPO_ROOT/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/levels/BukovLevel.java"
LANDMARK_TILEMAP="$REPO_ROOT/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/levels/BukovLandmarkTilemap.java"
SEMANTIC_LAYER="$REPO_ROOT/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/levels/BukovSemanticVisualLayer.java"
GATE_OVERLAY="$REPO_ROOT/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/bukov/BukovFirstRaidLandmarks.java"
TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/bukov-theme-visual-gate.XXXXXX")"
trap 'rm -rf "$TMP_DIR"' EXIT

node "$SCRIPT_DIR/generate_bukov_landmarks.mjs" \
  "$TMP_DIR/first_raid_landmarks.png"
node "$SCRIPT_DIR/generate_bukov_theme_visuals.mjs" \
  "$REPO_ROOT" "$TMP_DIR/assets" "$TMP_DIR/first_raid_landmarks.png"

sha256_file() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}

decode_rgba() {
  ffmpeg -hide_banner -loglevel error -i "$1" \
    -f rawvideo -pix_fmt rgba "$2"
}

assert_png_rgba_equal() {
  local committed="$1"
  local generated="$2"
  local label="$3"
  decode_rgba "$committed" "$TMP_DIR/${label}-committed.rgba"
  decode_rgba "$generated" "$TMP_DIR/${label}-generated.rgba"
  cmp \
    "$TMP_DIR/${label}-committed.rgba" \
    "$TMP_DIR/${label}-generated.rgba"
}

assert_manifest_hash() {
  local manifest="$1"
  local expression="$2"
  local png="$3"
  local label="$4"
  local expected actual
  expected="$(jq -er "$expression" "$manifest")"
  actual="$(sha256_file "$png")"
  if [[ "$expected" != "$actual" ]]; then
    echo "$label manifest SHA does not match PNG" >&2
    exit 1
  fi
}

asset_ids=(
  fog_depot
  rust_works
  flooded_bunker
  container_yard
  cold_storage
  underground_lab
)

for asset_id in "${asset_ids[@]}"; do
  for prefix in tiles water landmarks; do
    # ponytail: decoded pixels are the portable contract across PNG encoders.
    assert_png_rgba_equal \
      "$ASSET_DIR/${prefix}_${asset_id}.png" \
      "$TMP_DIR/assets/${prefix}_${asset_id}.png" \
      "${prefix}-${asset_id}"
  done
  [[ "$(ffprobe -v error -select_streams v:0 \
    -show_entries stream=width,height,pix_fmt -of csv=p=0 \
    "$ASSET_DIR/tiles_${asset_id}.png")" == "256,256,rgba" ]]
  [[ "$(ffprobe -v error -select_streams v:0 \
    -show_entries stream=width,height,pix_fmt -of csv=p=0 \
    "$ASSET_DIR/water_${asset_id}.png")" == "32,32,rgba" ]]
  [[ "$(ffprobe -v error -select_streams v:0 \
    -show_entries stream=width,height,pix_fmt -of csv=p=0 \
    "$ASSET_DIR/landmarks_${asset_id}.png")" == "320,32,rgba" ]]
done

assert_png_rgba_equal \
  "$ASSET_DIR/theme_visual_contact_sheet.png" \
  "$TMP_DIR/assets/theme_visual_contact_sheet.png" \
  "theme-contact-sheet"

jq -S 'del(.themes[].sha256, .contactSheet.sha256)' \
  "$ASSET_DIR/theme_visual_manifest.json" \
  >"$TMP_DIR/theme_visual_manifest-committed.logical.json"
jq -S 'del(.themes[].sha256, .contactSheet.sha256)' \
  "$TMP_DIR/assets/theme_visual_manifest.json" \
  >"$TMP_DIR/theme_visual_manifest-generated.logical.json"
cmp \
  "$TMP_DIR/theme_visual_manifest-committed.logical.json" \
  "$TMP_DIR/theme_visual_manifest-generated.logical.json"

for manifest_kind in committed generated; do
  if [[ "$manifest_kind" == "committed" ]]; then
    manifest="$ASSET_DIR/theme_visual_manifest.json"
    assets="$ASSET_DIR"
  else
    manifest="$TMP_DIR/assets/theme_visual_manifest.json"
    assets="$TMP_DIR/assets"
  fi
  for asset_id in "${asset_ids[@]}"; do
    for channel in tiles water landmarks; do
      assert_manifest_hash "$manifest" \
        ".themes[] | select(.assetId == \"$asset_id\") | .sha256.$channel" \
        "$assets/${channel}_${asset_id}.png" \
        "$manifest_kind $asset_id $channel"
    done
  done
  assert_manifest_hash "$manifest" '.contactSheet.sha256' \
    "$assets/theme_visual_contact_sheet.png" \
    "$manifest_kind contact sheet"
done

[[ "$(ffprobe -v error -select_streams v:0 \
  -show_entries stream=width,height,pix_fmt -of csv=p=0 \
  "$ASSET_DIR/theme_visual_contact_sheet.png")" == "576,96,rgba" ]]

# The generated terrain/water atlases retain source alpha exactly, proving
# that terrain slice geometry and transparent boundaries did not move.
ffmpeg -hide_banner -loglevel error -i \
  "$REPO_ROOT/core/src/main/assets/environment/tiles_city.png" \
  -vf format=rgba,alphaextract -f rawvideo -pix_fmt gray \
  "$TMP_DIR/tiles-source-alpha.raw"
ffmpeg -hide_banner -loglevel error -i \
  "$REPO_ROOT/core/src/main/assets/environment/water3.png" \
  -vf format=rgba,alphaextract -f rawvideo -pix_fmt gray \
  "$TMP_DIR/water-source-alpha.raw"
for asset_id in "${asset_ids[@]}"; do
  ffmpeg -hide_banner -loglevel error -i \
    "$ASSET_DIR/tiles_${asset_id}.png" \
    -vf format=rgba,alphaextract -f rawvideo -pix_fmt gray \
    "$TMP_DIR/tiles-${asset_id}-alpha.raw"
  ffmpeg -hide_banner -loglevel error -i \
    "$ASSET_DIR/water_${asset_id}.png" \
    -vf format=rgba,alphaextract -f rawvideo -pix_fmt gray \
    "$TMP_DIR/water-${asset_id}-alpha.raw"
  cmp \
    "$TMP_DIR/tiles-source-alpha.raw" \
    "$TMP_DIR/tiles-${asset_id}-alpha.raw"
  cmp \
    "$TMP_DIR/water-source-alpha.raw" \
    "$TMP_DIR/water-${asset_id}-alpha.raw"
done

node - "$ASSET_DIR" <<'NODE'
import { createHash } from "node:crypto";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import { spawnSync } from "node:child_process";

const directory = process.argv[2];
const manifest = JSON.parse(readFileSync(
  join(directory, "theme_visual_manifest.json"), "utf8"));
if (manifest.themes.length !== 6) {
  throw new Error("theme visual manifest must contain six themes");
}

function decode(path, width, height) {
  const result = spawnSync("ffmpeg", [
    "-hide_banner", "-loglevel", "error",
    "-i", path, "-f", "rawvideo", "-pix_fmt", "rgba", "-",
  ]);
  if (result.status !== 0) throw new Error(`cannot decode ${path}`);
  if (result.stdout.length !== width * height * 4) {
    throw new Error(`unexpected decoded size for ${path}`);
  }
  return result.stdout;
}

function colorCount(buffer, color) {
  let count = 0;
  for (let offset = 0; offset < buffer.length; offset += 4) {
    if (buffer[offset] === color[0]
        && buffer[offset + 1] === color[1]
        && buffer[offset + 2] === color[2]
        && buffer[offset + 3] === color[3]) {
      count += 1;
    }
  }
  return count;
}

const semantic = manifest.interactionColors;
const rgbaHashes = {
  tiles: new Set(),
  water: new Set(),
  landmarks: new Set(),
};
const dimensions = {
  tiles: [256, 256],
  water: [32, 32],
  landmarks: [320, 32],
};
for (const theme of manifest.themes) {
  const decoded = {};
  for (const channel of Object.keys(rgbaHashes)) {
    decoded[channel] = decode(
      join(directory, `${channel}_${theme.assetId}.png`),
      ...dimensions[channel]);
    rgbaHashes[channel].add(
      createHash("sha256").update(decoded[channel]).digest("hex"));
  }
  const landmarks = decoded.landmarks;
  for (const role of [
    "archive", "gate", "extraction", "conditional", "cache",
  ]) {
    if (colorCount(landmarks, semantic[role]) < 6) {
      throw new Error(
        `${theme.assetId} lacks readable ${role} interaction marker`);
    }
  }
  if (colorCount(landmarks, [...theme.palette.accent, 255]) < 8) {
    throw new Error(`${theme.assetId} lacks its landmark silhouette motif`);
  }
}
for (const [channel, hashes] of Object.entries(rgbaHashes)) {
  if (hashes.size !== 6) {
    throw new Error(`${channel} assets are not visually unique`);
  }
}
NODE

for required in \
  '"visualAssetId": "fog_depot"' \
  '"visualAssetId": "rust_works"' \
  '"visualAssetId": "flooded_bunker"' \
  '"visualAssetId": "container_yard"' \
  '"visualAssetId": "cold_storage"' \
  '"visualAssetId": "underground_lab"'; do
  rg -Fq "$required" "$THEMES_JSON"
done
rg -Fq 'return visualTheme().tilesTexture();' "$LEVEL"
rg -Fq 'return visualTheme().waterTexture();' "$LEVEL"
rg -Fq 'return visualTheme().landmarkTexture();' "$LEVEL"
rg -Fq 'visualAssetId(theme)' "$SEMANTIC_LAYER"
rg -Fq 'level.landmarkTex()' "$GATE_OVERLAY"
rg -Fq 'VISUAL_ASSET_ID' "$LANDMARK_TILEMAP"

if rg -q 'https?://|fetch\(|createReadStream' \
  "$SCRIPT_DIR/generate_bukov_theme_visuals.mjs"; then
  echo "theme visual generator must remain offline and source-local" >&2
  exit 1
fi

echo "Bukov six-theme visual gate passed."
