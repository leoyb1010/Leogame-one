#!/usr/bin/env node

/**
 * Rejects six-theme packs whose only differences are palette or PNG bytes.
 *
 * The comparison converts each atlas to luminance, builds a local-contrast
 * structure code for every opaque pixel, and compares those codes by atlas
 * region. A monotonic recolour keeps the same structure codes and therefore
 * fails even when all six files have different hashes.
 */

import { readFileSync, writeFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { spawnSync } from "node:child_process";

const CHANNELS = {
  tiles: {
    width: 256,
    height: 256,
    regionWidth: 16,
    regionHeight: 16,
    minimumPairDistance: 0.055,
    minimumChangedRegions: 30,
    regionDistance: 0.035,
  },
  water: {
    width: 32,
    height: 32,
    regionWidth: 8,
    regionHeight: 8,
    minimumPairDistance: 0.045,
    minimumChangedRegions: 3,
    regionDistance: 0.030,
  },
  landmarks: {
    width: 320,
    height: 32,
    regionWidth: 32,
    regionHeight: 32,
    minimumPairDistance: 0.018,
    minimumChangedRegions: 3,
    regionDistance: 0.012,
  },
};

function fail(message) {
  throw new Error(`Bukov theme structure gate: ${message}`);
}

function decode(path, width, height) {
  const result = spawnSync("ffmpeg", [
    "-hide_banner", "-loglevel", "error",
    "-i", path,
    "-f", "rawvideo", "-pix_fmt", "rgba", "-",
  ]);
  if (result.status !== 0) {
    fail(`cannot decode ${path}: ${result.stderr.toString().trim()}`);
  }
  const expected = width * height * 4;
  if (result.stdout.length !== expected) {
    fail(`${path} decoded to ${result.stdout.length} bytes, expected ${expected}`);
  }
  return result.stdout;
}

function luminance(r, g, b) {
  return r * 0.2126 + g * 0.7152 + b * 0.0722;
}

function structure(buffer, width, height) {
  const luma = new Float32Array(width * height);
  const opaque = new Uint8Array(width * height);
  const samples = [];
  for (let index = 0; index < width * height; index += 1) {
    const offset = index * 4;
    if (buffer[offset + 3] < 32) continue;
    opaque[index] = 1;
    const value = luminance(
      buffer[offset], buffer[offset + 1], buffer[offset + 2]);
    luma[index] = value;
    samples.push(value);
  }
  if (samples.length < 32) fail("atlas has too few opaque pixels");
  /*
   * Local ordering, unlike absolute edge strength, survives a monotonic
   * palette ramp. A sub-one-luma epsilon only absorbs integer encoding ties;
   * it deliberately does not adapt to each theme's contrast.
   */
  const threshold = 0.75;
  const codes = new Uint8Array(width * height);

  const compare = (index, other, positiveBit, negativeBit) => {
    if (!opaque[other]) return 0;
    const delta = luma[other] - luma[index];
    if (delta > threshold) return positiveBit;
    if (delta < -threshold) return negativeBit;
    return 0;
  };

  for (let y = 0; y < height; y += 1) {
    for (let x = 0; x < width; x += 1) {
      const index = x + y * width;
      if (!opaque[index]) continue;
      let code = 0;
      if (x + 1 < width) code |= compare(index, index + 1, 1, 2);
      if (y + 1 < height) code |= compare(index, index + width, 4, 8);
      if (x + 1 < width && y + 1 < height) {
        code |= compare(index, index + width + 1, 16, 32);
      }
      if (x > 0 && y + 1 < height) {
        code |= compare(index, index + width - 1, 64, 128);
      }
      codes[index] = code;
    }
  }
  return { codes, opaque, threshold };
}

function compareStructures(first, second, config) {
  let common = 0;
  let changed = 0;
  const regionColumns = Math.ceil(config.width / config.regionWidth);
  const regionRows = Math.ceil(config.height / config.regionHeight);
  const regionCommon = new Uint32Array(regionColumns * regionRows);
  const regionChanged = new Uint32Array(regionColumns * regionRows);

  for (let y = 0; y < config.height; y += 1) {
    for (let x = 0; x < config.width; x += 1) {
      const index = x + y * config.width;
      if (!first.opaque[index] || !second.opaque[index]) continue;
      const region = Math.floor(x / config.regionWidth)
        + Math.floor(y / config.regionHeight) * regionColumns;
      common += 1;
      regionCommon[region] += 1;
      if (first.codes[index] !== second.codes[index]) {
        changed += 1;
        regionChanged[region] += 1;
      }
    }
  }
  if (common === 0) fail("theme pair has no common opaque pixels");
  let changedRegions = 0;
  const regionDistances = [];
  for (let index = 0; index < regionCommon.length; index += 1) {
    if (regionCommon[index] === 0) continue;
    const distance = regionChanged[index] / regionCommon[index];
    regionDistances.push(distance);
    if (distance >= config.regionDistance) changedRegions += 1;
  }
  return {
    distance: changed / common,
    changedPixels: changed,
    commonPixels: common,
    changedRegions,
    comparableRegions: regionDistances.length,
  };
}

function validateChannel(channel, themes, config) {
  const pairs = [];
  const failures = [];
  for (let first = 0; first < themes.length; first += 1) {
    for (let second = first + 1; second < themes.length; second += 1) {
      const metrics = compareStructures(
        themes[first].structure, themes[second].structure, config);
      const pair = {
        first: themes[first].assetId,
        second: themes[second].assetId,
        distance: Number(metrics.distance.toFixed(5)),
        changedRegions: metrics.changedRegions,
        comparableRegions: metrics.comparableRegions,
      };
      pairs.push(pair);
      if (metrics.distance < config.minimumPairDistance
          || metrics.changedRegions < config.minimumChangedRegions) {
        failures.push(
          `${channel} ${pair.first} vs ${pair.second}: `
          + `structure distance ${pair.distance} `
          + `(required >= ${config.minimumPairDistance}), `
          + `changed regions ${pair.changedRegions}/${pair.comparableRegions} `
          + `(required >= ${config.minimumChangedRegions})`);
      }
    }
  }
  return { pairs, failures };
}

function syntheticBuffer(width, height, palette, structuralStripe = false) {
  const buffer = Buffer.alloc(width * height * 4);
  for (let y = 0; y < height; y += 1) {
    for (let x = 0; x < width; x += 1) {
      const offset = (x + y * width) * 4;
      const bright = (x % 8 < 4) !== (y % 8 < 4);
      let color = bright ? palette[1] : palette[0];
      if (structuralStripe && (x + y) % 7 === 0) color = palette[2];
      buffer[offset] = color[0];
      buffer[offset + 1] = color[1];
      buffer[offset + 2] = color[2];
      buffer[offset + 3] = 255;
    }
  }
  return buffer;
}

function selfTest() {
  const width = 32;
  const height = 32;
  const first = structure(syntheticBuffer(width, height, [
    [20, 24, 28], [120, 130, 140], [230, 220, 180],
  ]), width, height);
  const recoloured = structure(syntheticBuffer(width, height, [
    [45, 12, 8], [205, 100, 30], [255, 220, 90],
  ]), width, height);
  const changed = structure(syntheticBuffer(width, height, [
    [45, 12, 8], [205, 100, 30], [255, 220, 90],
  ], true), width, height);
  const config = {
    width,
    height,
    regionWidth: 8,
    regionHeight: 8,
    regionDistance: 0.03,
  };
  const paletteOnly = compareStructures(first, recoloured, config);
  const structural = compareStructures(first, changed, config);
  if (paletteOnly.distance >= 0.01) {
    fail(`self-test accepted palette-only change: ${paletteOnly.distance}`);
  }
  if (structural.distance <= paletteOnly.distance + 0.02) {
    fail(`self-test missed structural change: ${structural.distance}`);
  }
  process.stdout.write(
    "PASS: structure self-test rejects palette-only uniqueness "
    + `(${paletteOnly.distance.toFixed(5)}) and detects geometry `
    + `(${structural.distance.toFixed(5)})\n`);
}

if (process.argv[2] === "--self-test") {
  selfTest();
  process.exit(0);
}

const assetDirectory = resolve(process.argv[2]
  ?? "core/src/main/assets/environment/bukov");
const reportPath = process.argv[3] ? resolve(process.argv[3]) : null;
const manifest = JSON.parse(readFileSync(
  join(assetDirectory, "theme_visual_manifest.json"), "utf8"));
if (!Array.isArray(manifest.themes) || manifest.themes.length !== 6) {
  fail("manifest must contain exactly six themes");
}

const report = {
  schemaVersion: 1,
  method: "adaptive grayscale local-contrast structure codes",
  assetDirectory,
  channels: {},
  status: "passed",
};
const allFailures = [];
for (const [channel, config] of Object.entries(CHANNELS)) {
  const themes = manifest.themes.map((theme) => {
    const path = join(assetDirectory, `${channel}_${theme.assetId}.png`);
    const pixels = decode(path, config.width, config.height);
    return {
      assetId: theme.assetId,
      structure: structure(pixels, config.width, config.height),
    };
  });
  const result = validateChannel(channel, themes, config);
  report.channels[channel] = {
    thresholds: {
      minimumPairDistance: config.minimumPairDistance,
      minimumChangedRegions: config.minimumChangedRegions,
      regionDistance: config.regionDistance,
    },
    pairs: result.pairs,
  };
  allFailures.push(...result.failures);
}
if (allFailures.length > 0) {
  report.status = "failed";
  report.failures = allFailures;
}
if (reportPath) {
  writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`);
}
if (allFailures.length > 0) {
  for (const failure of allFailures) {
    process.stderr.write(`FAIL: ${failure}\n`);
  }
  if (reportPath) process.stderr.write(`Structure report: ${reportPath}\n`);
  process.exit(1);
}
for (const [channel, value] of Object.entries(report.channels)) {
  const weakest = value.pairs.reduce(
    (current, pair) => pair.distance < current.distance ? pair : current);
  process.stdout.write(
    `PASS: ${channel} weakest pair ${weakest.first} vs ${weakest.second}, `
    + `distance ${weakest.distance}, regions `
    + `${weakest.changedRegions}/${weakest.comparableRegions}\n`);
}
if (reportPath) process.stdout.write(`Structure report: ${reportPath}\n`);
