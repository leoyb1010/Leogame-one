#!/usr/bin/env node

/**
 * Generates the original Bukov tactical UI atlas.
 *
 * Every pixel is authored by the primitives below from the project's named UI
 * palette. No upstream Pixel Dungeon UI, external image, tracing, recoloring,
 * font, logo, or third-party game asset is read or sampled.
 */

import { createHash } from "node:crypto";
import {
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from "node:fs";
import { dirname, join } from "node:path";
import { tmpdir } from "node:os";
import { spawnSync } from "node:child_process";

const TILE = 16;
const COLUMNS = 16;
const ROWS = 4;
const WIDTH = TILE * COLUMNS;
const HEIGHT = TILE * ROWS;
const output = process.argv[2]
  ?? "core/src/main/assets/interfaces/bukov_ui.png";
const manifestOutput = process.argv[3]
  ?? "core/src/main/assets/interfaces/bukov_ui_manifest.json";
const tokensPath = process.argv[4]
  ?? "core/src/main/assets/bukov/content/ui_tokens.json";

const tokens = JSON.parse(readFileSync(tokensPath, "utf8")).colors;
const rgba = (token, alpha = 255) => {
  const value = tokens[token];
  if (!/^#[0-9A-Fa-f]{6}$/.test(value ?? "")) {
    throw new Error(`missing UI color token: ${token}`);
  }
  return [
    Number.parseInt(value.slice(1, 3), 16),
    Number.parseInt(value.slice(3, 5), 16),
    Number.parseInt(value.slice(5, 7), 16),
    alpha,
  ];
};

const colors = {
  transparent: [0, 0, 0, 0],
  ink: rgba("ink.background", 244),
  deep: rgba("panel.deep", 252),
  surface: rgba("panel.surface", 238),
  border: rgba("panel.border"),
  interact: rgba("accent.interact"),
  valuable: rgba("accent.valuable"),
  danger: rgba("accent.danger"),
  extract: rgba("accent.extract"),
  text: rgba("text.primary"),
  muted: rgba("text.secondary"),
  disabled: rgba("text.disabled"),
  shadow: rgba("ink.shadow", 220),
  common: rgba("rarity.common"),
  uncommon: rgba("rarity.uncommon"),
  rare: rgba("rarity.rare"),
  legendary: rgba("rarity.legendary"),
};

const entries = [
  {
    apiName: "PANEL",
    kind: "ninePatch",
    x: 0,
    y: 0,
    width: TILE,
    height: TILE,
    margins: [4, 4, 4, 4],
  },
  {
    apiName: "PANEL_RAISED",
    kind: "ninePatch",
    x: TILE,
    y: 0,
    width: TILE,
    height: TILE,
    margins: [4, 4, 4, 4],
  },
  {
    apiName: "BUTTON",
    kind: "ninePatch",
    x: TILE * 2,
    y: 0,
    width: TILE,
    height: TILE,
    margins: [4, 4, 4, 4],
  },
  {
    apiName: "BUTTON_PRESSED",
    kind: "ninePatch",
    x: TILE * 3,
    y: 0,
    width: TILE,
    height: TILE,
    margins: [4, 4, 4, 4],
  },
  {
    apiName: "BUTTON_FOCUSED",
    kind: "ninePatch",
    x: TILE * 4,
    y: 0,
    width: TILE,
    height: TILE,
    margins: [4, 4, 4, 4],
  },
  {
    apiName: "BUTTON_DISABLED",
    kind: "ninePatch",
    x: TILE * 5,
    y: 0,
    width: TILE,
    height: TILE,
    margins: [4, 4, 4, 4],
  },
  {
    apiName: "ROW_FOCUSED",
    kind: "ninePatch",
    x: TILE * 6,
    y: 0,
    width: TILE,
    height: TILE,
    margins: [4, 4, 4, 4],
  },
  ...[
    ["RARITY_COMMON", 0],
    ["RARITY_UNCOMMON", 1],
    ["RARITY_RARE", 2],
    ["RARITY_LEGENDARY", 3],
  ].map(([apiName, column]) => ({
    apiName,
    kind: "ninePatch",
    x: TILE * column,
    y: TILE,
    width: TILE,
    height: TILE,
    margins: [4, 4, 4, 4],
  })),
  ...[
    ["HUD_HEALTH", 0],
    ["HUD_ARMOR", 1],
    ["HUD_AMMO", 2],
    ["HUD_INTERACT", 3],
    ["HUD_OBJECTIVE", 4],
    ["HUD_TIMER", 5],
    ["HUD_SOUND", 6],
    ["HUD_HIT", 7],
  ].map(([apiName, column]) => ({
    apiName,
    kind: "icon",
    x: TILE * column,
    y: TILE * 2,
    width: TILE,
    height: TILE,
  })),
  ...[
    ["TOUCH_MOVEMENT", 8],
    ["TOUCH_AIM_FIRE", 9],
    ["TOUCH_INTERACT", 10],
    ["TOUCH_RELOAD", 11],
    ["TOUCH_MEDICAL", 12],
    ["TOUCH_DROP", 13],
    ["TOUCH_BACKPACK", 14],
    ["TOUCH_PAUSE", 15],
  ].map(([apiName, column]) => ({
    apiName,
    kind: "icon",
    x: TILE * column,
    y: TILE * 2,
    width: TILE,
    height: TILE,
  })),
  {
    apiName: "STATUS_ACTION",
    kind: "icon",
    x: 0,
    y: TILE * 3,
    width: TILE,
    height: TILE,
  },
  {
    apiName: "STATUS_LOOT",
    kind: "icon",
    x: TILE,
    y: TILE * 3,
    width: TILE,
    height: TILE,
  },
  {
    apiName: "STATUS_EXTRACT",
    kind: "icon",
    x: TILE * 2,
    y: TILE * 3,
    width: TILE,
    height: TILE,
  },
  {
    apiName: "STATUS_DANGER",
    kind: "icon",
    x: TILE * 3,
    y: TILE * 3,
    width: TILE,
    height: TILE,
  },
  {
    apiName: "STATUS_BLEEDING",
    kind: "icon",
    x: TILE * 4,
    y: TILE * 3,
    width: TILE,
    height: TILE,
  },
  {
    apiName: "STATUS_FRACTURE",
    kind: "icon",
    x: TILE * 5,
    y: TILE * 3,
    width: TILE,
    height: TILE,
  },
  {
    apiName: "STATUS_CONCUSSION",
    kind: "icon",
    x: TILE * 6,
    y: TILE * 3,
    width: TILE,
    height: TILE,
  },
  {
    apiName: "STAMP_EXTRACTED",
    kind: "image",
    x: TILE * 8,
    y: TILE * 3,
    width: TILE * 3,
    height: TILE,
  },
  {
    apiName: "STAMP_LOST",
    kind: "image",
    x: TILE * 11,
    y: TILE * 3,
    width: TILE * 3,
    height: TILE,
  },
  {
    apiName: "TOUCH_DISABLED_STRIKE",
    kind: "icon",
    x: TILE * 14,
    y: TILE * 3,
    width: TILE,
    height: TILE,
  },
];

const pixels = Buffer.alloc(WIDTH * HEIGHT * 4);

function pixel(x, y, color) {
  if (x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT) return;
  const offset = (y * WIDTH + x) * 4;
  pixels.set(color, offset);
}

function rect(x, y, width, height, color) {
  for (let yy = y; yy < y + height; yy += 1) {
    for (let xx = x; xx < x + width; xx += 1) pixel(xx, yy, color);
  }
}

function localRect(column, row, x, y, width, height, color) {
  rect(column * TILE + x, row * TILE + y, width, height, color);
}

function localPixel(column, row, x, y, color) {
  pixel(column * TILE + x, row * TILE + y, color);
}

function panel(column, row, fill, edge, raised) {
  localRect(column, row, 1, 1, 14, 14, colors.shadow);
  localRect(column, row, 1, 1, 14, 1, edge);
  localRect(column, row, 1, 2, 1, 12, edge);
  localRect(column, row, 14, 2, 1, 12, colors.shadow);
  localRect(column, row, 2, 14, 12, 1, colors.shadow);
  localRect(column, row, 2, 2, 12, 12, fill);
  localRect(column, row, 3, 3, 10, 1, raised ? colors.border : colors.deep);
  localRect(column, row, 3, 12, 10, 1, colors.shadow);
  for (let x = 4; x <= 12; x += 4) {
    localPixel(column, row, x, 4, raised ? colors.muted : colors.border);
  }
  localPixel(column, row, 1, 1, colors.transparent);
  localPixel(column, row, 14, 1, colors.transparent);
  localPixel(column, row, 1, 14, colors.transparent);
  localPixel(column, row, 14, 14, colors.transparent);
}

panel(0, 0, colors.ink, colors.border, false);
panel(1, 0, colors.surface, colors.valuable, true);
panel(2, 0, colors.surface, colors.interact, true);
panel(3, 0, colors.deep, colors.extract, false);
panel(4, 0, colors.surface, colors.valuable, true);
panel(5, 0, colors.deep, colors.disabled, false);
panel(6, 0, colors.surface, colors.interact, false);

// Focus and disabled states are shape-distinct, not tint-only.
for (let x = 3; x <= 12; x += 3) {
  localPixel(4, 0, x, 2, colors.text);
  localPixel(4, 0, x, 13, colors.interact);
}
for (let offset = 3; offset <= 12; offset += 3) {
  localPixel(5, 0, offset, offset, colors.disabled);
  localPixel(5, 0, 15 - offset, offset, colors.disabled);
}
localRect(6, 0, 2, 2, 2, 12, colors.interact);
localRect(6, 0, 4, 3, 1, 10, colors.valuable);

function rarityFrame(column, edge) {
  panel(column, 1, colors.deep, edge, false);
  localPixel(column, 1, 3, 3, edge);
  localPixel(column, 1, 12, 3, edge);
  localPixel(column, 1, 3, 12, edge);
  localPixel(column, 1, 12, 12, edge);
}

rarityFrame(0, colors.common);
rarityFrame(1, colors.uncommon);
rarityFrame(2, colors.rare);
rarityFrame(3, colors.legendary);

const HUD_ROW = 2;
const STATUS_ROW = 3;

// Health: split field dressing / ECG silhouette.
localRect(0, HUD_ROW, 3, 6, 10, 5, colors.extract);
localRect(0, HUD_ROW, 6, 3, 4, 11, colors.extract);
localRect(0, HUD_ROW, 5, 7, 2, 2, colors.deep);
localPixel(0, HUD_ROW, 7, 6, colors.text);
localPixel(0, HUD_ROW, 8, 9, colors.text);
localRect(0, HUD_ROW, 9, 7, 2, 2, colors.deep);

// Armor plate with a split lower edge.
localRect(1, HUD_ROW, 4, 2, 8, 2, colors.valuable);
localRect(1, HUD_ROW, 3, 4, 10, 5, colors.valuable);
localRect(1, HUD_ROW, 4, 9, 8, 2, colors.valuable);
localRect(1, HUD_ROW, 6, 11, 4, 2, colors.valuable);
localRect(1, HUD_ROW, 6, 5, 4, 4, colors.deep);
localRect(1, HUD_ROW, 7, 5, 2, 2, colors.text);

// Magazine and three visible rounds.
localRect(2, HUD_ROW, 4, 2, 8, 2, colors.valuable);
localRect(2, HUD_ROW, 5, 4, 7, 8, colors.valuable);
localRect(2, HUD_ROW, 6, 12, 4, 2, colors.valuable);
for (let y = 5; y <= 9; y += 2) {
  localRect(2, HUD_ROW, 7, y, 3, 1, colors.deep);
}

// Interact hand / switch glyph.
localRect(3, HUD_ROW, 3, 8, 8, 4, colors.interact);
localRect(3, HUD_ROW, 6, 3, 2, 7, colors.interact);
localRect(3, HUD_ROW, 9, 5, 2, 5, colors.interact);
localRect(3, HUD_ROW, 11, 7, 2, 4, colors.interact);
localRect(3, HUD_ROW, 5, 12, 6, 2, colors.interact);

// Objective dossier.
localRect(4, HUD_ROW, 3, 2, 10, 12, colors.common);
localRect(4, HUD_ROW, 5, 4, 6, 1, colors.deep);
localRect(4, HUD_ROW, 5, 7, 6, 1, colors.deep);
localRect(4, HUD_ROW, 5, 10, 4, 1, colors.interact);

// Timer face, directional sound and incoming-hit wedge.
localRect(5, HUD_ROW, 5, 2, 6, 1, colors.muted);
localRect(5, HUD_ROW, 3, 5, 1, 6, colors.muted);
localRect(5, HUD_ROW, 12, 5, 1, 6, colors.muted);
localRect(5, HUD_ROW, 5, 13, 6, 1, colors.muted);
localRect(5, HUD_ROW, 7, 5, 2, 4, colors.text);
localRect(5, HUD_ROW, 8, 8, 3, 2, colors.interact);
localRect(6, HUD_ROW, 6, 6, 4, 5, colors.valuable);
localRect(6, HUD_ROW, 4, 5, 1, 7, colors.valuable);
localRect(6, HUD_ROW, 2, 3, 1, 11, colors.valuable);
localRect(6, HUD_ROW, 11, 5, 1, 7, colors.valuable);
localRect(6, HUD_ROW, 13, 3, 1, 11, colors.valuable);
for (let y = 2; y <= 13; y += 1) {
  const width = Math.max(1, 8 - Math.abs(8 - y));
  localRect(7, HUD_ROW, 8 - Math.floor(width / 2), y, width, 1,
    colors.danger);
}
localRect(7, HUD_ROW, 7, 6, 2, 5, colors.deep);
localRect(7, HUD_ROW, 7, 12, 2, 1, colors.text);

const touchBlueprints = [
  // Movement d-pad.
  [[7, 1, 2, 10], [5, 3, 2, 2], [9, 3, 2, 2],
    [7, 5, 2, 10], [5, 11, 2, 2], [9, 11, 2, 2],
    [1, 7, 10, 2], [3, 5, 2, 2], [3, 9, 2, 2],
    [5, 7, 10, 2], [11, 5, 2, 2], [11, 9, 2, 2]],
  // Aim/fire reticle with a centre point and broken corners.
  [[7, 1, 2, 4], [7, 11, 2, 4], [1, 7, 4, 2],
    [11, 7, 4, 2], [7, 7, 2, 2], [4, 4, 3, 1],
    [4, 5, 1, 2], [9, 4, 3, 1], [11, 5, 1, 2],
    [4, 11, 3, 1], [4, 9, 1, 2], [9, 11, 3, 1],
    [11, 9, 1, 2]],
  // Interaction tap target with response marks.
  [[6, 6, 4, 4], [7, 2, 2, 3], [2, 7, 3, 2],
    [11, 7, 3, 2], [4, 4, 2, 1], [3, 3, 1, 1],
    [10, 4, 2, 1], [12, 3, 1, 1], [7, 11, 2, 3]],
  // Reload arrow wrapped around a magazine.
  [[4, 2, 7, 2], [2, 4, 2, 6], [4, 10, 3, 2],
    [10, 4, 2, 3], [9, 2, 4, 2], [11, 1, 2, 5],
    [12, 4, 2, 2], [7, 7, 3, 6], [8, 8, 1, 3],
    [7, 12, 3, 2]],
  // Medical first-aid cross.
  [[6, 2, 4, 12], [2, 6, 12, 4], [4, 4, 2, 2],
    [10, 4, 2, 2], [4, 10, 2, 2], [10, 10, 2, 2]],
  // Drop arrow entering an open container.
  [[7, 1, 2, 7], [4, 6, 3, 2], [9, 6, 3, 2],
    [6, 8, 4, 2], [2, 10, 2, 4], [12, 10, 2, 4],
    [2, 13, 12, 2], [4, 10, 2, 1], [10, 10, 2, 1]],
  // Backpack with lid, pocket and shoulder straps.
  [[5, 1, 6, 2], [3, 3, 10, 2], [2, 5, 2, 9],
    [12, 5, 2, 9], [3, 13, 10, 2], [5, 7, 6, 1],
    [5, 10, 6, 3], [6, 11, 4, 1], [1, 6, 1, 5],
    [14, 6, 1, 5]],
  // Pause bars inside a persistent bracket.
  [[4, 3, 3, 10], [9, 3, 3, 10], [2, 1, 12, 1],
    [2, 14, 12, 1], [1, 2, 1, 3], [14, 2, 1, 3]],
];

for (let glyph = 0; glyph < touchBlueprints.length; glyph += 1) {
  const column = 8 + glyph;
  for (const [x, y, width, height] of touchBlueprints[glyph]) {
    localRect(column, HUD_ROW, x, y, width, height, colors.text);
  }
}

// Crosshair/action state.
localRect(0, STATUS_ROW, 7, 2, 2, 12, colors.interact);
localRect(0, STATUS_ROW, 2, 7, 12, 2, colors.interact);
localRect(0, STATUS_ROW, 6, 6, 4, 4, colors.deep);
localRect(0, STATUS_ROW, 7, 7, 2, 2, colors.text);

// Compact inventory crate.
localRect(1, STATUS_ROW, 3, 5, 10, 8, colors.valuable);
localRect(1, STATUS_ROW, 4, 6, 8, 6, colors.deep);
localRect(1, STATUS_ROW, 5, 3, 6, 2, colors.valuable);
localRect(1, STATUS_ROW, 7, 7, 2, 5, colors.border);
localRect(1, STATUS_ROW, 4, 8, 8, 1, colors.valuable);

// Extraction route and open threshold.
localRect(2, STATUS_ROW, 3, 3, 2, 10, colors.extract);
localRect(2, STATUS_ROW, 4, 3, 7, 2, colors.extract);
localRect(2, STATUS_ROW, 4, 11, 7, 2, colors.extract);
localRect(2, STATUS_ROW, 10, 5, 2, 3, colors.extract);
localRect(2, STATUS_ROW, 8, 6, 5, 2, colors.text);
localRect(2, STATUS_ROW, 11, 5, 2, 4, colors.text);

// Warning chevron, kept iconographic rather than text-like.
for (let y = 2; y <= 11; y += 1) {
  const inset = Math.abs(7 - y);
  localRect(3, STATUS_ROW, 7 - Math.floor((7 - inset) / 2), y, 7 - inset, 1,
    colors.danger);
}
localRect(3, STATUS_ROW, 7, 5, 2, 5, colors.deep);
localRect(3, STATUS_ROW, 7, 11, 2, 2, colors.deep);

// Bleeding: asymmetric droplet silhouette with a hollow highlight.
for (let y = 2; y <= 12; y += 1) {
  const halfWidth = y < 8
    ? Math.max(1, Math.floor((y - 1) / 2))
    : Math.max(2, 6 - Math.floor((y - 8) / 2));
  localRect(4, STATUS_ROW, 8 - halfWidth, y, halfWidth * 2, 1, colors.danger);
}
localRect(4, STATUS_ROW, 5, 9, 2, 2, colors.text);
localPixel(4, STATUS_ROW, 6, 8, colors.text);
localRect(4, STATUS_ROW, 7, 12, 3, 1, colors.shadow);

// Fracture: two bone ends split by an unmistakable lightning-shaped break.
localRect(5, STATUS_ROW, 3, 3, 4, 3, colors.valuable);
localRect(5, STATUS_ROW, 9, 10, 4, 3, colors.valuable);
for (let offset = 0; offset < 7; offset += 1) {
  localPixel(5, STATUS_ROW, 5 + offset, 5 + offset, colors.valuable);
  if (offset < 5) {
    localPixel(5, STATUS_ROW, 6 + offset, 5 + offset, colors.valuable);
  }
}
localRect(5, STATUS_ROW, 7, 7, 2, 2, colors.deep);
localPixel(5, STATUS_ROW, 8, 6, colors.deep);
localPixel(5, STATUS_ROW, 7, 9, colors.deep);

// Concussion: head ring with offset shock arcs; never shares the droplet or
// broken-bone outline, so colorblind mode retains a shape distinction.
localRect(6, STATUS_ROW, 5, 4, 6, 1, colors.interact);
localRect(6, STATUS_ROW, 4, 5, 1, 5, colors.interact);
localRect(6, STATUS_ROW, 11, 5, 1, 5, colors.interact);
localRect(6, STATUS_ROW, 5, 10, 6, 1, colors.interact);
localRect(6, STATUS_ROW, 6, 6, 4, 4, colors.deep);
localRect(6, STATUS_ROW, 7, 6, 2, 1, colors.text);
localPixel(6, STATUS_ROW, 2, 4, colors.interact);
localPixel(6, STATUS_ROW, 13, 4, colors.interact);
localRect(6, STATUS_ROW, 1, 7, 2, 1, colors.interact);
localRect(6, STATUS_ROW, 13, 7, 2, 1, colors.interact);
localPixel(6, STATUS_ROW, 3, 12, colors.interact);
localPixel(6, STATUS_ROW, 12, 12, colors.interact);

function stamp(startColumn, edge, slash) {
  const startX = startColumn * TILE;
  rect(startX + 1, STATUS_ROW * TILE + 2, 46, 12, colors.shadow);
  rect(startX + 2, STATUS_ROW * TILE + 1, 44, 1, edge);
  rect(startX + 2, STATUS_ROW * TILE + 14, 44, 1, edge);
  rect(startX + 1, STATUS_ROW * TILE + 2, 1, 12, edge);
  rect(startX + 46, STATUS_ROW * TILE + 2, 1, 12, edge);
  for (let x = 5; x < 43; x += 6) {
    pixel(startX + x, STATUS_ROW * TILE + 4, edge);
    pixel(startX + x + 2, STATUS_ROW * TILE + 11, edge);
  }
  if (slash) {
    for (let offset = 0; offset < 10; offset += 1) {
      rect(startX + 18 + offset, STATUS_ROW * TILE + 3 + offset, 3, 1, edge);
    }
    rect(startX + 11, STATUS_ROW * TILE + 6, 25, 2, edge);
  } else {
    rect(startX + 10, STATUS_ROW * TILE + 7, 7, 3, edge);
    rect(startX + 16, STATUS_ROW * TILE + 8, 3, 4, edge);
    rect(startX + 18, STATUS_ROW * TILE + 6, 17, 3, edge);
    rect(startX + 32, STATUS_ROW * TILE + 4, 3, 4, edge);
  }
}

stamp(8, colors.extract, false);
stamp(11, colors.danger, true);

for (let offset = 2; offset <= 12; offset += 2) {
  localRect(14, STATUS_ROW, offset, offset, 2, 2, colors.text);
}

const temp = mkdtempSync(join(tmpdir(), "bukov-ui-atlas-"));
try {
  mkdirSync(dirname(output), { recursive: true });
  mkdirSync(dirname(manifestOutput), { recursive: true });
  const raw = join(temp, "bukov_ui.rgba");
  writeFileSync(raw, pixels);
  const encoded = spawnSync("ffmpeg", [
    "-y", "-hide_banner", "-loglevel", "error",
    "-f", "rawvideo",
    "-pixel_format", "rgba",
    "-video_size", `${WIDTH}x${HEIGHT}`,
    "-framerate", "1",
    "-i", raw,
    "-frames:v", "1",
    "-compression_level", "9",
    "-pred", "none",
    output,
  ], { stdio: "inherit" });
  if (encoded.status !== 0) process.exit(encoded.status ?? 1);

  const png = readFileSync(output);
  const uniqueRgbaColors = new Set();
  let transparentPixels = 0;
  for (let offset = 0; offset < pixels.length; offset += 4) {
    uniqueRgbaColors.add(pixels.subarray(offset, offset + 4).toString("hex"));
    if (pixels[offset + 3] === 0) transparentPixels += 1;
  }
  const manifest = {
    schemaVersion: 2,
    atlas: "interfaces/bukov_ui.png",
    generator: "scripts/generate_bukov_ui_atlas.mjs",
    palette: "bukov/content/ui_tokens.json",
    license: "project original cleared for this repository and personal builds",
    width: WIDTH,
    height: HEIGHT,
    pixelSampling: "nearest",
    uniqueRgbaColors: uniqueRgbaColors.size,
    transparentPixels,
    sha256: createHash("sha256").update(png).digest("hex"),
    entries,
  };
  writeFileSync(
    manifestOutput,
    `${JSON.stringify(manifest, null, 2)}\n`,
    "utf8",
  );
  process.stdout.write(`${JSON.stringify(manifest, null, 2)}\n`);
} finally {
  rmSync(temp, { recursive: true, force: true });
}
