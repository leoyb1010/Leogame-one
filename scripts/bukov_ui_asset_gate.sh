#!/bin/bash
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
atlas="$root/core/src/main/assets/interfaces/bukov_ui.png"
manifest="$root/core/src/main/assets/interfaces/bukov_ui_manifest.json"
provenance="$root/artwork/licenses/ASSET_PROVENANCE.csv"
temporary="$(mktemp -d "${TMPDIR:-/tmp}/bukov-ui-assets.XXXXXX")"
trap 'rm -rf "$temporary"' EXIT

node "$root/scripts/generate_bukov_ui_atlas.mjs" \
  "$temporary/bukov_ui.png" \
  "$temporary/bukov_ui_manifest.json" \
  "$root/core/src/main/assets/bukov/content/ui_tokens.json" \
  >/dev/null

# ponytail: PNG containers may differ across zlib builds; decoded pixels are the contract.
ffmpeg -y -hide_banner -loglevel error \
  -i "$atlas" -f rawvideo -pix_fmt rgba "$temporary/bukov_ui.committed.rgba"
ffmpeg -y -hide_banner -loglevel error \
  -i "$temporary/bukov_ui.png" -f rawvideo -pix_fmt rgba \
  "$temporary/bukov_ui.generated.rgba"
cmp "$temporary/bukov_ui.committed.rgba" \
  "$temporary/bukov_ui.generated.rgba"

jq -S 'del(.sha256)' "$manifest" \
  >"$temporary/bukov_ui.committed.logical.json"
jq -S 'del(.sha256)' "$temporary/bukov_ui_manifest.json" \
  >"$temporary/bukov_ui.generated.logical.json"
cmp "$temporary/bukov_ui.committed.logical.json" \
  "$temporary/bukov_ui.generated.logical.json"

node - "$manifest" "$atlas" "$provenance" \
  "$temporary/bukov_ui.committed.rgba" \
  "$temporary/bukov_ui_manifest.json" "$temporary/bukov_ui.png" <<'NODE'
const { createHash } = require("node:crypto");
const { readFileSync } = require("node:fs");

const manifest = JSON.parse(readFileSync(process.argv[2], "utf8"));
const png = readFileSync(process.argv[3]);
const provenance = readFileSync(process.argv[4], "utf8");
const rgba = readFileSync(process.argv[5]);
const generatedManifest = JSON.parse(readFileSync(process.argv[6], "utf8"));
const generatedPng = readFileSync(process.argv[7]);
const expected = [
  "PANEL",
  "PANEL_RAISED",
  "BUTTON",
  "BUTTON_PRESSED",
  "BUTTON_FOCUSED",
  "BUTTON_DISABLED",
  "ROW_FOCUSED",
  "RARITY_COMMON",
  "RARITY_UNCOMMON",
  "RARITY_RARE",
  "RARITY_LEGENDARY",
  "HUD_HEALTH",
  "HUD_ARMOR",
  "HUD_AMMO",
  "HUD_INTERACT",
  "HUD_OBJECTIVE",
  "HUD_TIMER",
  "HUD_SOUND",
  "HUD_HIT",
  "TOUCH_MOVEMENT",
  "TOUCH_AIM_FIRE",
  "TOUCH_INTERACT",
  "TOUCH_RELOAD",
  "TOUCH_MEDICAL",
  "TOUCH_DROP",
  "TOUCH_BACKPACK",
  "TOUCH_PAUSE",
  "HUB_MODE",
  "HUB_VENDOR",
  "HUB_FILTER",
  "HUB_SORT",
  "HUB_SEARCH",
  "HUB_RECOMMEND",
  "HUB_DEPLOY",
  "HUB_BACK",
  "HUB_SETTINGS",
  "HUB_DOCUMENT",
  "HUB_RESUME",
  "STATUS_ACTION",
  "STATUS_LOOT",
  "STATUS_EXTRACT",
  "STATUS_DANGER",
  "STATUS_BLEEDING",
  "STATUS_FRACTURE",
  "STATUS_CONCUSSION",
  "STAMP_EXTRACTED",
  "STAMP_LOST",
  "TOUCH_DISABLED_STRIKE",
];
const actual = manifest.entries.map((entry) => entry.apiName);
if (manifest.schemaVersion !== 2
    || manifest.width !== 256
    || manifest.height !== 80
    || manifest.pixelSampling !== "nearest"
    || actual.join(",") !== expected.join(",")) {
  throw new Error("Bukov UI atlas manifest contract drift");
}
const hash = createHash("sha256").update(png).digest("hex");
const generatedHash = createHash("sha256").update(generatedPng).digest("hex");
if (hash !== manifest.sha256
    || generatedHash !== generatedManifest.sha256
    || !provenance.includes(`core/src/main/assets/interfaces/bukov_ui.png,`)
    || !provenance.includes(hash)) {
  throw new Error("Bukov UI atlas provenance/hash closure is incomplete");
}

const entryPixels = (entry) => {
  const out = [];
  for (let y = entry.y; y < entry.y + entry.height; y += 1) {
    const start = (y * manifest.width + entry.x) * 4;
    out.push(rgba.subarray(start, start + entry.width * 4));
  }
  return Buffer.concat(out);
};
const requireDistinct = (names) => {
  const hashes = names.map((name) => {
    const entry = manifest.entries.find((candidate) =>
      candidate.apiName === name);
    if (!entry) throw new Error(`missing UI entry ${name}`);
    const pixels = entryPixels(entry);
    const visible = Array.from(
      { length: pixels.length / 4 },
      (_, index) => pixels[index * 4 + 3],
    ).filter((alpha) => alpha > 0).length;
    if (visible < 12) throw new Error(`${name} is visually empty`);
    return createHash("sha256").update(pixels).digest("hex");
  });
  if (new Set(hashes).size !== names.length) {
    throw new Error(`UI entries are not shape-distinct: ${names.join(",")}`);
  }
};
requireDistinct([
  "BUTTON", "BUTTON_PRESSED", "BUTTON_FOCUSED", "BUTTON_DISABLED",
]);
requireDistinct([
  "RARITY_COMMON", "RARITY_UNCOMMON", "RARITY_RARE", "RARITY_LEGENDARY",
]);
requireDistinct([
  "HUD_HEALTH", "HUD_ARMOR", "HUD_AMMO", "HUD_INTERACT",
  "HUD_OBJECTIVE", "HUD_TIMER", "HUD_SOUND", "HUD_HIT",
]);
requireDistinct([
  "TOUCH_MOVEMENT", "TOUCH_AIM_FIRE", "TOUCH_INTERACT", "TOUCH_RELOAD",
  "TOUCH_MEDICAL", "TOUCH_DROP", "TOUCH_BACKPACK", "TOUCH_PAUSE",
]);
requireDistinct([
  "HUB_MODE", "HUB_VENDOR", "HUB_FILTER", "HUB_SORT",
  "HUB_SEARCH", "HUB_RECOMMEND", "HUB_DEPLOY", "HUB_BACK",
  "HUB_SETTINGS", "HUB_DOCUMENT", "HUB_RESUME",
]);
requireDistinct(["STAMP_EXTRACTED", "STAMP_LOST"]);
NODE

for scene in TitleScene.java WelcomeScene.java BukovDeploymentScene.java; do
  source="$root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/$scene"
  rg -q 'BukovUiAssets\.surface\(' "$source"
  if rg -q 'Assets\.Interfaces\.LEO_' "$source"; then
    echo "ERROR: legacy Leo UI skin is reachable from $scene" >&2
    exit 1
  fi
done

loader="$root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/BukovUiAssets.java"
rg -q 'TextureCache\.createSolid\(fallbackColor\)' "$loader"
rg -q 'Assets\.Interfaces\.BUKOV_UI' "$loader"
rg -q 'Surface\.BUTTON_DISABLED' \
  "$root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/WndBukovHub.java"
rg -q 'Surface\.BUTTON_DISABLED' \
  "$root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/BukovHubScene.java"
rg -q 'BukovUiAssets\.rarityFrame\(' \
  "$root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/WndBukovHub.java"
rg -q 'BukovUiAssets\.HudElement\.HEALTH' \
  "$root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/BukovRaidHud.java"
rg -q 'BukovUiAssets\.Stamp\.EXTRACTED' \
  "$root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/WndBukovSettlement.java"
rg -q 'BukovUiAssets\.Stamp\.LOST' \
  "$root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/WndBukovSettlement.java"
rg -q 'BukovUiAssets\.touchGlyph\(' \
  "$root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/BukovTouchIcon.java"
rg -q 'BukovUiAssets\.touchDisabledStrike\(' \
  "$root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/BukovTouchIcon.java"

echo "PASS: deterministic complete Bukov UI atlas, provenance, fallback, and wiring"
