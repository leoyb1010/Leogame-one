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

cmp "$atlas" "$temporary/bukov_ui.png"
cmp "$manifest" "$temporary/bukov_ui_manifest.json"

node - "$manifest" "$atlas" "$provenance" <<'NODE'
const { createHash } = require("node:crypto");
const { readFileSync } = require("node:fs");

const manifest = JSON.parse(readFileSync(process.argv[2], "utf8"));
const png = readFileSync(process.argv[3]);
const provenance = readFileSync(process.argv[4], "utf8");
const expected = [
  "PANEL",
  "PANEL_RAISED",
  "BUTTON",
  "BUTTON_PRESSED",
  "STATUS_ACTION",
  "STATUS_LOOT",
  "STATUS_EXTRACT",
  "STATUS_DANGER",
  "STATUS_BLEEDING",
  "STATUS_FRACTURE",
  "STATUS_CONCUSSION",
];
const actual = manifest.entries.map((entry) => entry.apiName);
if (manifest.schemaVersion !== 1
    || manifest.width !== 112
    || manifest.height !== 32
    || manifest.pixelSampling !== "nearest"
    || actual.join(",") !== expected.join(",")) {
  throw new Error("Bukov UI atlas manifest contract drift");
}
const hash = createHash("sha256").update(png).digest("hex");
if (hash !== manifest.sha256
    || !provenance.includes(`core/src/main/assets/interfaces/bukov_ui.png,`)
    || !provenance.includes(hash)) {
  throw new Error("Bukov UI atlas provenance/hash closure is incomplete");
}
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

echo "PASS: deterministic Bukov UI atlas, provenance, fallback, and wiring"
