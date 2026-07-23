#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"
atlas="$repo_root/core/src/main/assets/sprites/bukov/items_interactions.png"
manifest="$repo_root/core/src/main/assets/sprites/bukov/items_interactions_manifest.json"
mapping="$repo_root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/bukov/BukovItemSprite.java"
provenance="$repo_root/artwork/licenses/ASSET_PROVENANCE.csv"
firearms_json="$repo_root/core/src/main/assets/bukov/content/firearms.json"
ammunition_json="$repo_root/core/src/main/assets/bukov/content/ammunition.json"
loot_source="$repo_root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/content/BukovFirstRaidLootTables.java"
temp_dir="$(mktemp -d "${TMPDIR:-/tmp}/bukov-item-atlas.XXXXXX")"
trap 'rm -rf "$temp_dir"' EXIT

node "$script_dir/generate_bukov_item_visuals.mjs" \
  "$temp_dir/items.png" "$temp_dir/manifest.json" >/dev/null
cmp "$atlas" "$temp_dir/items.png"
cmp "$manifest" "$temp_dir/manifest.json"

probe="$(ffprobe -v error -select_streams v:0 \
  -show_entries stream=width,height,pix_fmt -of csv=p=0 "$atlas")"
[[ "$probe" == "1152,16,rgba" ]]

ffmpeg -hide_banner -loglevel error -i "$atlas" \
  -f rawvideo -pix_fmt rgba "$temp_dir/items.rgba"

node --input-type=module - \
  "$temp_dir/items.rgba" "$manifest" "$mapping" "$provenance" \
  "$firearms_json" "$ammunition_json" "$loot_source" <<'NODE'
import { createHash } from "node:crypto";
import { readFileSync } from "node:fs";

const raw = readFileSync(process.argv[2]);
const manifest = JSON.parse(readFileSync(process.argv[3], "utf8"));
const mapping = readFileSync(process.argv[4], "utf8");
const provenance = readFileSync(process.argv[5], "utf8");
const firearms = JSON.parse(readFileSync(process.argv[6], "utf8")).firearms;
const ammunition = JSON.parse(readFileSync(process.argv[7], "utf8")).ammunition;
const lootSource = readFileSync(process.argv[8], "utf8");
const frameSize = 16;
const frameCount = 72;
const width = frameSize * frameCount;

if (raw.length !== width * frameSize * 4) {
  throw new Error(`unexpected raw byte count: ${raw.length}`);
}
if (manifest.schemaVersion !== 1
    || manifest.width !== width
    || manifest.height !== frameSize
    || manifest.frameWidth !== frameSize
    || manifest.frameHeight !== frameSize
    || manifest.frameCount !== frameCount
    || manifest.entries.length !== frameCount) {
  throw new Error("manifest dimensions or entry count drift");
}

const expectedCategories = {
  firearm: 18,
  ammunition: 8,
  armor: 3,
  backpack: 2,
  medical_tool: 8,
  loot_mission: 30,
  interaction: 3,
};
const categoryCounts = {};
const definitionIds = new Set();
const apiNames = new Set();
const indices = new Set();
for (const entry of manifest.entries) {
  categoryCounts[entry.category] = (categoryCounts[entry.category] ?? 0) + 1;
  if (!definitionIds.add(entry.definitionId)) {
    throw new Error(`duplicate definition mapping: ${entry.definitionId}`);
  }
  if (!apiNames.add(entry.apiName)) {
    throw new Error(`duplicate API name: ${entry.apiName}`);
  }
  if (!indices.add(entry.index)) {
    throw new Error(`duplicate frame index: ${entry.index}`);
  }
  if (!mapping.includes(`${entry.apiName}(${entry.index})`)) {
    throw new Error(`missing Frame enum mapping: ${entry.apiName}`);
  }
  if (entry.category !== "interaction"
      && !mapping.includes(`case "${entry.definitionId}":`)) {
    throw new Error(`missing definition mapping: ${entry.definitionId}`);
  }
}
if (JSON.stringify(categoryCounts) !== JSON.stringify(expectedCategories)) {
  throw new Error(`category coverage drift: ${JSON.stringify(categoryCounts)}`);
}
if (!mapping.includes("public static final int FRAME_COUNT = 72;")) {
  throw new Error("runtime frame count drift");
}

function sortedDefinitions(category) {
  return manifest.entries
    .filter((entry) => entry.category === category)
    .map((entry) => entry.definitionId)
    .sort();
}
const firearmDefinitions = firearms
  .map((entry) => `firearm:${entry.id}`).sort();
const ammoDefinitions = ammunition
  .map((entry) => `ammo:${entry.id}`).sort();
if (JSON.stringify(sortedDefinitions("firearm"))
    !== JSON.stringify(firearmDefinitions)) {
  throw new Error("manifest does not cover all authored firearms");
}
if (JSON.stringify(sortedDefinitions("ammunition"))
    !== JSON.stringify(ammoDefinitions)) {
  throw new Error("manifest does not cover all authored ammunition");
}
const expectedArmor = [
  "armor:soft_vest", "armor:patrol_vest", "armor:ceramic_rig",
].sort();
const expectedBackpacks = [
  "backpack:scout_pack", "backpack:field_pack",
].sort();
const expectedMedicalTools = [
  "bandage", "painkiller", "first_aid", "tourniquet",
  "antiseptic", "splint", "stim", "tool_set",
].sort();
if (JSON.stringify(sortedDefinitions("armor"))
    !== JSON.stringify(expectedArmor)
    || JSON.stringify(sortedDefinitions("backpack"))
    !== JSON.stringify(expectedBackpacks)
    || JSON.stringify(sortedDefinitions("medical_tool"))
    !== JSON.stringify(expectedMedicalTools)) {
  throw new Error("equipment/medical manifest coverage drift");
}
for (const definitionId of sortedDefinitions("loot_mission")) {
  if (definitionId.startsWith("mission:")) continue;
  if (!lootSource.includes(`loot("${definitionId}"`)) {
    throw new Error(`manifest loot is not authored content: ${definitionId}`);
  }
}

const frameHashes = new Set();
const colors = new Set();
let transparent = 0;
let translucent = 0;
let opaque = 0;
for (let frame = 0; frame < frameCount; frame += 1) {
  const frameBytes = Buffer.alloc(frameSize * frameSize * 4);
  let frameOpaque = 0;
  let frameTransparent = 0;
  for (let y = 0; y < frameSize; y += 1) {
    for (let x = 0; x < frameSize; x += 1) {
      const source = (y * width + frame * frameSize + x) * 4;
      const target = (y * frameSize + x) * 4;
      raw.copy(frameBytes, target, source, source + 4);
      const rgba = raw.subarray(source, source + 4);
      colors.add(rgba.toString("hex"));
      const alpha = rgba[3];
      if (alpha === 0) {
        transparent += 1;
        frameTransparent += 1;
      } else if (alpha === 255) {
        opaque += 1;
        frameOpaque += 1;
      } else {
        translucent += 1;
      }
    }
  }
  if (frameOpaque < 24 || frameTransparent < 24) {
    throw new Error(
      `frame ${frame} lacks readable silhouette/alpha: `
        + `${frameOpaque} opaque, ${frameTransparent} transparent`,
    );
  }
  const hash = createHash("sha256").update(frameBytes).digest("hex");
  if (!frameHashes.add(hash)) {
    throw new Error(`duplicate visual frame: ${frame}`);
  }
}
if (colors.size > 24 || colors.size < 12) {
  throw new Error(`shared palette drift: ${colors.size} RGBA colors`);
}
if (transparent === 0 || opaque === 0 || translucent === 0) {
  throw new Error("atlas must contain transparent, opaque and glow pixels");
}
if (transparent !== manifest.transparentPixels
    || translucent !== manifest.translucentPixels
    || opaque !== manifest.opaquePixels
    || colors.size !== manifest.uniqueRgbaColors) {
  throw new Error("manifest pixel statistics drift");
}

const pngHash = createHash("sha256")
  .update(readFileSync(process.argv[3].replace(
    "items_interactions_manifest.json", "items_interactions.png",
  )))
  .digest("hex");
if (pngHash !== manifest.sha256 || !provenance.includes(pngHash)) {
  throw new Error("atlas SHA-256 is missing from manifest/provenance");
}
NODE

if rg -q 'rogue\\.png|items\\.png|ItemSpriteSheet|tiles_city\\.png' \
  "$script_dir/generate_bukov_item_visuals.mjs"; then
  echo "item generator references a host/source atlas" >&2
  exit 1
fi

echo "Bukov item atlas gate: PASS (72 unique logical frames)."
