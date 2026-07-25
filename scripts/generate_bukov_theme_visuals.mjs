#!/usr/bin/env node

/**
 * Generates six deterministic Bukov environment skins from project-owned
 * atlas geometry. No network, external images or runtime dependencies.
 *
 * The host atlas alpha/coordinates remain unchanged, preserving every terrain
 * slice and collision contract. Palette ramps, per-tile pattern accents and
 * per-theme landmark silhouettes create screenshot-readable identity.
 */

import {
  createHash,
} from "node:crypto";
import {
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from "node:fs";
import { dirname, join, resolve } from "node:path";
import { tmpdir } from "node:os";
import { spawnSync } from "node:child_process";

const repoRoot = resolve(process.argv[2] ?? ".");
const outputDir = resolve(process.argv[3]
  ?? join(repoRoot, "core/src/main/assets/environment/bukov"));
const landmarkSource = resolve(process.argv[4]
  ?? join(outputDir, "first_raid_landmarks.png"));

const tileSource = join(
  repoRoot, "core/src/main/assets/environment/tiles_city.png");
const waterSource = join(
  repoRoot, "core/src/main/assets/environment/water3.png");

const THEMES = [
  {
    themeId: "fog_depot",
    assetId: "fog_depot",
    pattern: "fog patches",
    motif: "signal mast",
    shadow: [13, 27, 32],
    mid: [65, 91, 96],
    high: [156, 180, 174],
    accent: [88, 178, 173],
  },
  {
    themeId: "rust_workshop",
    assetId: "rust_works",
    pattern: "diagonal furnace stripes",
    motif: "exhaust stack",
    shadow: [31, 20, 17],
    mid: [112, 67, 44],
    high: [207, 154, 92],
    accent: [226, 104, 47],
  },
  {
    themeId: "flooded_passage",
    assetId: "flooded_bunker",
    pattern: "horizontal flood channels",
    motif: "overhead pipe",
    shadow: [9, 25, 34],
    mid: [40, 92, 108],
    high: [126, 191, 197],
    accent: [53, 199, 214],
  },
  {
    themeId: "overgrown_yard",
    assetId: "container_yard",
    pattern: "container block seams",
    motif: "gantry hook",
    shadow: [23, 29, 22],
    mid: [72, 91, 58],
    high: [177, 164, 97],
    accent: [218, 155, 50],
  },
  {
    themeId: "cold_storage",
    assetId: "cold_storage",
    pattern: "frost service grid",
    motif: "cooling fan",
    shadow: [19, 27, 38],
    mid: [78, 103, 127],
    high: [191, 219, 230],
    accent: [121, 213, 238],
  },
  {
    themeId: "sealed_lab",
    assetId: "underground_lab",
    pattern: "violet circuit traces",
    motif: "sensor antenna",
    shadow: [20, 20, 37],
    mid: [69, 66, 108],
    high: [194, 166, 210],
    accent: [204, 92, 206],
  },
];

const SEMANTIC = {
  archive: [239, 190, 67, 255],
  gate: [229, 100, 52, 255],
  extraction: [76, 224, 171, 255],
  conditional: [244, 169, 56, 255],
  cache: [201, 153, 244, 255],
};

const temp = mkdtempSync(join(tmpdir(), "bukov-theme-visuals-"));
mkdirSync(outputDir, { recursive: true });

function run(command, args) {
  const result = spawnSync(command, args, { stdio: "inherit" });
  if (result.status !== 0) process.exit(result.status ?? 1);
}

function decode(path, width, height, label) {
  const raw = join(temp, `${label}.rgba`);
  run("ffmpeg", [
    "-y", "-hide_banner", "-loglevel", "error",
    "-i", path,
    "-f", "rawvideo", "-pix_fmt", "rgba", raw,
  ]);
  const pixels = readFileSync(raw);
  const expected = width * height * 4;
  if (pixels.length !== expected) {
    throw new Error(`${label} decoded to ${pixels.length}, expected ${expected}`);
  }
  return pixels;
}

function encode(pixels, width, height, output, label) {
  const raw = join(temp, `${label}-out.rgba`);
  mkdirSync(dirname(output), { recursive: true });
  writeFileSync(raw, pixels);
  run("ffmpeg", [
    "-y", "-hide_banner", "-loglevel", "error",
    "-f", "rawvideo", "-pixel_format", "rgba",
    "-video_size", `${width}x${height}`, "-framerate", "1",
    "-i", raw,
    "-frames:v", "1", "-compression_level", "9", output,
  ]);
}

function clamp(value) {
  return Math.max(0, Math.min(255, Math.round(value)));
}

function mix(first, second, amount) {
  return [
    clamp(first[0] + (second[0] - first[0]) * amount),
    clamp(first[1] + (second[1] - first[1]) * amount),
    clamp(first[2] + (second[2] - first[2]) * amount),
  ];
}

function ramp(theme, luminance) {
  return luminance < 0.52
    ? mix(theme.shadow, theme.mid, luminance / 0.52)
    : mix(theme.mid, theme.high, (luminance - 0.52) / 0.48);
}

function patternAmount(index, x, y) {
  const localX = x & 15;
  const localY = y & 15;
  if (index === 0) {
    return localY === 5 || ((x * 17 + y * 31) % 53 === 0) ? 0.16 : 0;
  }
  if (index === 1) {
    return (localX + localY) % 8 <= 1 ? 0.27 : 0;
  }
  if (index === 2) {
    return localY === 7 || localY === 8 ? 0.26 : 0;
  }
  if (index === 3) {
    return localX === 0 || localX === 15
      || (localY === 4 && localX > 2 && localX < 13) ? 0.21 : 0;
  }
  if (index === 4) {
    return localX % 8 === 0 || localY % 8 === 0
      || ((x * 11 + y * 7) % 67 === 0) ? 0.20 : 0;
  }
  return (localX === 4 && localY >= 4 && localY <= 12)
    || (localY === 4 && localX >= 4 && localX <= 12)
    || (localX === 12 && localY === 12) ? 0.29 : 0;
}

function recolor(source, width, height, theme, themeIndex, strength = 1) {
  const output = Buffer.alloc(source.length);
  for (let y = 0; y < height; y += 1) {
    for (let x = 0; x < width; x += 1) {
      const offset = (y * width + x) * 4;
      const alpha = source[offset + 3];
      if (alpha === 0) continue;
      const luminance = (
        source[offset] * 0.2126
        + source[offset + 1] * 0.7152
        + source[offset + 2] * 0.0722
      ) / 255;
      let color = ramp(theme, luminance);
      const pattern = patternAmount(themeIndex, x, y) * strength;
      if (pattern > 0) color = mix(color, theme.accent, pattern);
      output[offset] = color[0];
      output[offset + 1] = color[1];
      output[offset + 2] = color[2];
      output[offset + 3] = alpha;
    }
  }
  return output;
}

function put(buffer, width, x, y, color) {
  if (x < 0 || x >= width || y < 0 || y >= 32) return;
  const offset = (y * width + x) * 4;
  buffer[offset] = color[0];
  buffer[offset + 1] = color[1];
  buffer[offset + 2] = color[2];
  buffer[offset + 3] = color[3] ?? 255;
}

function line(buffer, width, x0, y0, x1, y1, color) {
  let dx = Math.abs(x1 - x0);
  const sx = x0 < x1 ? 1 : -1;
  let dy = -Math.abs(y1 - y0);
  const sy = y0 < y1 ? 1 : -1;
  let error = dx + dy;
  while (true) {
    put(buffer, width, x0, y0, color);
    if (x0 === x1 && y0 === y1) break;
    const doubled = error * 2;
    if (doubled >= dy) {
      error += dy;
      x0 += sx;
    }
    if (doubled <= dx) {
      error += dx;
      y0 += sy;
    }
  }
}

function rect(buffer, width, x, y, w, h, color) {
  for (let yy = y; yy < y + h; yy += 1) {
    for (let xx = x; xx < x + w; xx += 1) {
      put(buffer, width, xx, yy, color);
    }
  }
}

function themeMotif(buffer, theme, themeIndex) {
  const width = 320;
  const accent = [...theme.accent, 255];
  const ink = [8, 12, 17, 255];
  for (let frame = 0; frame < 10; frame += 1) {
    const left = frame * 32;
    if (themeIndex === 0) {
      line(buffer, width, left + 27, 2, left + 27, 11, ink);
      line(buffer, width, left + 24, 5, left + 30, 5, accent);
      put(buffer, width, left + 27, 1, accent);
    } else if (themeIndex === 1) {
      rect(buffer, width, left + 25, 2, 5, 10, ink);
      rect(buffer, width, left + 26, 3, 3, 8, accent);
      put(buffer, width, left + 28, 0, [91, 72, 65, 180]);
      put(buffer, width, left + 30, 1, [91, 72, 65, 140]);
    } else if (themeIndex === 2) {
      line(buffer, width, left + 2, 3, left + 10, 3, ink);
      line(buffer, width, left + 3, 4, left + 3, 10, accent);
      line(buffer, width, left + 3, 10, left + 8, 10, accent);
      put(buffer, width, left + 9, 11, [183, 239, 244, 220]);
    } else if (themeIndex === 3) {
      line(buffer, width, left + 23, 2, left + 30, 2, ink);
      line(buffer, width, left + 28, 2, left + 28, 8, accent);
      line(buffer, width, left + 28, 8, left + 25, 11, accent);
      line(buffer, width, left + 25, 11, left + 23, 9, ink);
    } else if (themeIndex === 4) {
      const cx = left + 27;
      const cy = 6;
      line(buffer, width, cx - 4, cy, cx + 4, cy, accent);
      line(buffer, width, cx, cy - 4, cx, cy + 4, accent);
      line(buffer, width, cx - 3, cy - 3, cx + 3, cy + 3, ink);
      line(buffer, width, cx + 3, cy - 3, cx - 3, cy + 3, ink);
    } else {
      line(buffer, width, left + 27, 5, left + 27, 12, ink);
      line(buffer, width, left + 22, 3, left + 27, 5, accent);
      line(buffer, width, left + 27, 5, left + 31, 1, accent);
      rect(buffer, width, left + 25, 11, 5, 2, accent);
    }
  }
}

function semanticMarkers(buffer) {
  const width = 320;
  rect(buffer, width, 15, 11, 4, 3, SEMANTIC.archive);
  for (let frame = 1; frame <= 3; frame += 1) {
    rect(buffer, width, frame * 32 + 14, 5, 4, 2, SEMANTIC.gate);
  }
  rect(buffer, width, 5 * 32 + 14, 4, 5, 3, SEMANTIC.extraction);
  rect(buffer, width, 6 * 32 + 22, 4, 5, 3, SEMANTIC.conditional);
  rect(buffer, width, 7 * 32 + 14, 22, 5, 3, SEMANTIC.cache);
}

function environmentOverlays(theme, themeIndex) {
  const width = 64;
  const output = Buffer.alloc(width * 32 * 4);
  const accent = [...theme.accent, 220];
  const bright = [...mix(theme.accent, theme.high, 0.65), 205];
  const mid = [...theme.mid, 180];
  const soft = [...mix(theme.shadow, theme.mid, 0.65), 105];
  for (let frame = 0; frame < 2; frame += 1) {
    const left = frame * 32;
    if (themeIndex === 0) {
      // Low coastal fog banks with different broken silhouettes per frame.
      rect(output, width, left + 3, 22 - frame, 25, 3, soft);
      rect(output, width, left + 7, 19 - frame, 17, 3, soft);
      rect(output, width, left + 11, 17, 8, 2, mid);
      line(output, width, left + 5, 26, left + 25, 26, accent);
      put(output, width, left + 4 + frame * 20, 18, bright);
    } else if (themeIndex === 1) {
      // Furnace vent, rising heat teeth and loose welding sparks.
      rect(output, width, left + 5, 25, 22, 3, mid);
      for (let x = 7; x <= 25; x += 4) {
        line(output, width, left + x, 25, left + x + 2, 20, accent);
      }
      line(output, width, left + 10, 18, left + 14, 11 - frame, soft);
      line(output, width, left + 20, 18, left + 17, 8 + frame, soft);
      put(output, width, left + 6 + frame * 18, 10, bright);
      put(output, width, left + 25 - frame * 14, 6, bright);
    } else if (themeIndex === 2) {
      // Overhead pipe leak feeding concentric floor ripples.
      rect(output, width, left + 2, 3, 18, 3, mid);
      rect(output, width, left + 17, 4, 3, 7, mid);
      line(output, width, left + 18, 11, left + 18, 18 + frame, accent);
      put(output, width, left + 18, 20 + frame, bright);
      line(output, width, left + 7, 25, left + 27, 25, soft);
      line(output, width, left + 11, 22, left + 23, 22, accent);
      line(output, width, left + 14, 19, left + 20, 19, bright);
    } else if (themeIndex === 3) {
      // Asphalt crack with two distinct weed and vine clusters.
      line(output, width, left + 3, 28, left + 15, 21, mid);
      line(output, width, left + 15, 21, left + 28, 27, mid);
      const root = left + (frame === 0 ? 12 : 21);
      line(output, width, root, 25, root, 11, accent);
      line(output, width, root, 17, root - 7, 13, bright);
      line(output, width, root, 19, root + 6, 14, bright);
      line(output, width, root - 2, 25, root - 8, 20, soft);
    } else if (themeIndex === 4) {
      // Frost bloom, ice crystal spokes and low refrigeration mist.
      rect(output, width, left + 3, 24, 26, 3, soft);
      rect(output, width, left + 7, 21, 17, 2, soft);
      const cx = left + (frame === 0 ? 12 : 21);
      const cy = frame === 0 ? 13 : 11;
      line(output, width, cx - 7, cy, cx + 7, cy, accent);
      line(output, width, cx, cy - 7, cx, cy + 7, accent);
      line(output, width, cx - 5, cy - 5, cx + 5, cy + 5, bright);
      line(output, width, cx + 5, cy - 5, cx - 5, cy + 5, bright);
    } else {
      // Laboratory scan plane with circuit branches and diagnostic nodes.
      const scanY = frame === 0 ? 9 : 17;
      line(output, width, left + 3, scanY, left + 28, scanY, soft);
      line(output, width, left + 6, 27, left + 6, 14, mid);
      line(output, width, left + 6, 14, left + 17, 14, accent);
      line(output, width, left + 17, 14, left + 17, 5, accent);
      line(output, width, left + 17, 20, left + 27, 20, bright);
      rect(output, width, left + 4, 25, 5, 4, accent);
      rect(output, width, left + 15, 3, 5, 4, bright);
      rect(output, width, left + 25, 18, 4, 4, accent);
    }
  }
  return output;
}

function sha256(path) {
  return createHash("sha256").update(readFileSync(path)).digest("hex");
}

function copyRegion(
  source, sourceWidth, sourceX, sourceY, regionWidth, regionHeight,
  target, targetWidth, targetX, targetY,
) {
  for (let y = 0; y < regionHeight; y += 1) {
    for (let x = 0; x < regionWidth; x += 1) {
      const sourceOffset = (
        (sourceY + y) * sourceWidth + sourceX + x
      ) * 4;
      const targetOffset = (
        (targetY + y) * targetWidth + targetX + x
      ) * 4;
      source.copy(target, targetOffset, sourceOffset, sourceOffset + 4);
    }
  }
}

const tiles = decode(tileSource, 256, 256, "tiles-source");
const water = decode(waterSource, 32, 32, "water-source");
const landmarks = decode(landmarkSource, 320, 32, "landmarks-source");
const generated = [];

try {
  for (let index = 0; index < THEMES.length; index += 1) {
    const theme = THEMES[index];
    const themedTiles = recolor(tiles, 256, 256, theme, index, 1);
    const themedWater = recolor(water, 32, 32, theme, index, 1.25);
    const themedLandmarks = recolor(
      landmarks, 320, 32, theme, index, 0.45);
    const themedOverlays = environmentOverlays(theme, index);
    themeMotif(themedLandmarks, theme, index);
    semanticMarkers(themedLandmarks);

    const tileFile = join(outputDir, `tiles_${theme.assetId}.png`);
    const waterFile = join(outputDir, `water_${theme.assetId}.png`);
    const landmarkFile = join(
      outputDir, `landmarks_${theme.assetId}.png`);
    const overlayFile = join(
      outputDir, `overlays_${theme.assetId}.png`);
    encode(themedTiles, 256, 256, tileFile, `tiles-${theme.assetId}`);
    encode(themedWater, 32, 32, waterFile, `water-${theme.assetId}`);
    encode(
      themedLandmarks,
      320,
      32,
      landmarkFile,
      `landmarks-${theme.assetId}`,
    );
    encode(
      themedOverlays,
      64,
      32,
      overlayFile,
      `overlays-${theme.assetId}`,
    );
    generated.push({
      theme,
      tiles: themedTiles,
      water: themedWater,
      landmarks: themedLandmarks,
      overlays: themedOverlays,
      tileFile,
      waterFile,
      landmarkFile,
      overlayFile,
    });
  }

  const panelWidth = 96;
  const panelHeight = 128;
  const sheetWidth = panelWidth * THEMES.length;
  const sheet = Buffer.alloc(sheetWidth * panelHeight * 4);
  for (let index = 0; index < generated.length; index += 1) {
    const value = generated[index];
    const panelX = index * panelWidth;
    for (let y = 0; y < panelHeight; y += 1) {
      for (let x = 0; x < panelWidth; x += 1) {
        const offset = (y * sheetWidth + panelX + x) * 4;
        sheet[offset] = value.theme.shadow[0];
        sheet[offset + 1] = value.theme.shadow[1];
        sheet[offset + 2] = value.theme.shadow[2];
        sheet[offset + 3] = 255;
      }
    }
    copyRegion(
      value.tiles, 256, 0, 0, 64, 64,
      sheet, sheetWidth, panelX, 0,
    );
    copyRegion(
      value.water, 32, 0, 0, 32, 32,
      sheet, sheetWidth, panelX + 64, 0,
    );
    copyRegion(
      value.landmarks, 320, 5 * 32, 0, 32, 32,
      sheet, sheetWidth, panelX + 64, 32,
    );
    copyRegion(
      value.landmarks, 320, 0, 0, 32, 32,
      sheet, sheetWidth, panelX, 64,
    );
    copyRegion(
      value.landmarks, 320, 6 * 32, 0, 32, 32,
      sheet, sheetWidth, panelX + 32, 64,
    );
    copyRegion(
      value.landmarks, 320, 7 * 32, 0, 32, 32,
      sheet, sheetWidth, panelX + 64, 64,
    );
    copyRegion(
      value.overlays, 64, 0, 0, 64, 32,
      sheet, sheetWidth, panelX + 16, 96,
    );
  }
  const contactSheet = join(outputDir, "theme_visual_contact_sheet.png");
  encode(
    sheet,
    sheetWidth,
    panelHeight,
    contactSheet,
    "theme-contact-sheet",
  );

  const manifest = {
    schemaVersion: 2,
    generator: "scripts/generate_bukov_theme_visuals.mjs",
    internalSources: [
      "core/src/main/assets/environment/tiles_city.png",
      "core/src/main/assets/environment/water3.png",
      "core/src/main/assets/environment/bukov/first_raid_landmarks.png",
    ],
    dimensions: {
      tiles: [256, 256],
      water: [32, 32],
      landmarks: [320, 32],
      overlays: [64, 32],
      contactSheet: [sheetWidth, panelHeight],
    },
    interactionColors: SEMANTIC,
    themes: generated.map((value) => ({
      themeId: value.theme.themeId,
      assetId: value.theme.assetId,
      pattern: value.theme.pattern,
      landmarkMotif: value.theme.motif,
      palette: {
        shadow: value.theme.shadow,
        mid: value.theme.mid,
        high: value.theme.high,
        accent: value.theme.accent,
      },
      files: {
        tiles: `environment/bukov/tiles_${value.theme.assetId}.png`,
        water: `environment/bukov/water_${value.theme.assetId}.png`,
        landmarks:
          `environment/bukov/landmarks_${value.theme.assetId}.png`,
        overlays:
          `environment/bukov/overlays_${value.theme.assetId}.png`,
      },
      sha256: {
        tiles: sha256(value.tileFile),
        water: sha256(value.waterFile),
        landmarks: sha256(value.landmarkFile),
        overlays: sha256(value.overlayFile),
      },
    })),
    contactSheet: {
      file: "environment/bukov/theme_visual_contact_sheet.png",
      sha256: sha256(contactSheet),
    },
  };
  writeFileSync(
    join(outputDir, "theme_visual_manifest.json"),
    `${JSON.stringify(manifest, null, 2)}\n`,
  );
} finally {
  rmSync(temp, { recursive: true, force: true });
}
