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
const COLUMNS = 7;
const ROWS = 2;
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
  shadow: rgba("ink.shadow", 220),
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
    apiName: "STATUS_ACTION",
    kind: "icon",
    x: 0,
    y: TILE,
    width: TILE,
    height: TILE,
  },
  {
    apiName: "STATUS_LOOT",
    kind: "icon",
    x: TILE,
    y: TILE,
    width: TILE,
    height: TILE,
  },
  {
    apiName: "STATUS_EXTRACT",
    kind: "icon",
    x: TILE * 2,
    y: TILE,
    width: TILE,
    height: TILE,
  },
  {
    apiName: "STATUS_DANGER",
    kind: "icon",
    x: TILE * 3,
    y: TILE,
    width: TILE,
    height: TILE,
  },
  {
    apiName: "STATUS_BLEEDING",
    kind: "icon",
    x: TILE * 4,
    y: TILE,
    width: TILE,
    height: TILE,
  },
  {
    apiName: "STATUS_FRACTURE",
    kind: "icon",
    x: TILE * 5,
    y: TILE,
    width: TILE,
    height: TILE,
  },
  {
    apiName: "STATUS_CONCUSSION",
    kind: "icon",
    x: TILE * 6,
    y: TILE,
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

function panel(column, fill, edge, raised) {
  localRect(column, 0, 1, 1, 14, 14, colors.shadow);
  localRect(column, 0, 1, 1, 14, 1, edge);
  localRect(column, 0, 1, 2, 1, 12, edge);
  localRect(column, 0, 14, 2, 1, 12, colors.shadow);
  localRect(column, 0, 2, 14, 12, 1, colors.shadow);
  localRect(column, 0, 2, 2, 12, 12, fill);
  localRect(column, 0, 3, 3, 10, 1, raised ? colors.border : colors.deep);
  localRect(column, 0, 3, 12, 10, 1, colors.shadow);
  for (let x = 4; x <= 12; x += 4) {
    localPixel(column, 0, x, 4, raised ? colors.muted : colors.border);
  }
  localPixel(column, 0, 1, 1, colors.transparent);
  localPixel(column, 0, 14, 1, colors.transparent);
  localPixel(column, 0, 1, 14, colors.transparent);
  localPixel(column, 0, 14, 14, colors.transparent);
}

panel(0, colors.ink, colors.border, false);
panel(1, colors.surface, colors.valuable, true);
panel(2, colors.surface, colors.interact, true);
panel(3, colors.deep, colors.extract, false);

// Crosshair/action state.
localRect(0, 1, 7, 2, 2, 12, colors.interact);
localRect(0, 1, 2, 7, 12, 2, colors.interact);
localRect(0, 1, 6, 6, 4, 4, colors.deep);
localRect(0, 1, 7, 7, 2, 2, colors.text);

// Compact inventory crate.
localRect(1, 1, 3, 5, 10, 8, colors.valuable);
localRect(1, 1, 4, 6, 8, 6, colors.deep);
localRect(1, 1, 5, 3, 6, 2, colors.valuable);
localRect(1, 1, 7, 7, 2, 5, colors.border);
localRect(1, 1, 4, 8, 8, 1, colors.valuable);

// Extraction route and open threshold.
localRect(2, 1, 3, 3, 2, 10, colors.extract);
localRect(2, 1, 4, 3, 7, 2, colors.extract);
localRect(2, 1, 4, 11, 7, 2, colors.extract);
localRect(2, 1, 10, 5, 2, 3, colors.extract);
localRect(2, 1, 8, 6, 5, 2, colors.text);
localRect(2, 1, 11, 5, 2, 4, colors.text);

// Warning chevron, kept iconographic rather than text-like.
for (let y = 2; y <= 11; y += 1) {
  const inset = Math.abs(7 - y);
  localRect(3, 1, 7 - Math.floor((7 - inset) / 2), y, 7 - inset, 1,
    colors.danger);
}
localRect(3, 1, 7, 5, 2, 5, colors.deep);
localRect(3, 1, 7, 11, 2, 2, colors.deep);

// Bleeding: asymmetric droplet silhouette with a hollow highlight.
for (let y = 2; y <= 12; y += 1) {
  const halfWidth = y < 8
    ? Math.max(1, Math.floor((y - 1) / 2))
    : Math.max(2, 6 - Math.floor((y - 8) / 2));
  localRect(4, 1, 8 - halfWidth, y, halfWidth * 2, 1, colors.danger);
}
localRect(4, 1, 5, 9, 2, 2, colors.text);
localPixel(4, 1, 6, 8, colors.text);
localRect(4, 1, 7, 12, 3, 1, colors.shadow);

// Fracture: two bone ends split by an unmistakable lightning-shaped break.
localRect(5, 1, 3, 3, 4, 3, colors.valuable);
localRect(5, 1, 9, 10, 4, 3, colors.valuable);
for (let offset = 0; offset < 7; offset += 1) {
  localPixel(5, 1, 5 + offset, 5 + offset, colors.valuable);
  if (offset < 5) {
    localPixel(5, 1, 6 + offset, 5 + offset, colors.valuable);
  }
}
localRect(5, 1, 7, 7, 2, 2, colors.deep);
localPixel(5, 1, 8, 6, colors.deep);
localPixel(5, 1, 7, 9, colors.deep);

// Concussion: head ring with offset shock arcs; never shares the droplet or
// broken-bone outline, so colorblind mode retains a shape distinction.
localRect(6, 1, 5, 4, 6, 1, colors.interact);
localRect(6, 1, 4, 5, 1, 5, colors.interact);
localRect(6, 1, 11, 5, 1, 5, colors.interact);
localRect(6, 1, 5, 10, 6, 1, colors.interact);
localRect(6, 1, 6, 6, 4, 4, colors.deep);
localRect(6, 1, 7, 6, 2, 1, colors.text);
localPixel(6, 1, 2, 4, colors.interact);
localPixel(6, 1, 13, 4, colors.interact);
localRect(6, 1, 1, 7, 2, 1, colors.interact);
localRect(6, 1, 13, 7, 2, 1, colors.interact);
localPixel(6, 1, 3, 12, colors.interact);
localPixel(6, 1, 12, 12, colors.interact);

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
    schemaVersion: 1,
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
