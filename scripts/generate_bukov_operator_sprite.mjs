#!/usr/bin/env node

/**
 * Generates the original eight-direction Bukov operator animation sheet.
 * Every visible pixel is built from the geometric primitives below.
 */

import { createHash } from "node:crypto";
import {
  mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync,
} from "node:fs";
import { dirname, join } from "node:path";
import { tmpdir } from "node:os";
import { spawnSync } from "node:child_process";

const WIDTH = 384;
const HEIGHT = 128;
const FRAME_W = 12;
const FRAME_H = 15;
const FRAME_COUNT = 32;
const DIRECTION_COUNT = 8;
const output = process.argv[2] ?? "core/src/main/assets/sprites/bukov_operator.png";
const manifestOutput = process.argv[3]
  ?? "core/src/main/assets/sprites/bukov_operator_manifest.json";
const pixels = Buffer.alloc(WIDTH * HEIGHT * 4);

const directions = [
  { name: "N", x: 0, y: -1 },
  { name: "NE", x: 1, y: -1 },
  { name: "E", x: 1, y: 0 },
  { name: "SE", x: 1, y: 1 },
  { name: "S", x: 0, y: 1 },
  { name: "SW", x: -1, y: 1 },
  { name: "W", x: -1, y: 0 },
  { name: "NW", x: -1, y: -1 },
];
const states = [
  { name: "idle", from: 0, to: 1 },
  { name: "move", from: 2, to: 7 },
  { name: "aim", from: 8, to: 9 },
  { name: "fire", from: 10, to: 12 },
  { name: "reload", from: 13, to: 16 },
  { name: "hit", from: 17, to: 19 },
  { name: "medical", from: 20, to: 23 },
  { name: "down", from: 24, to: 27 },
  { name: "extract", from: 28, to: 31 },
];

const C = {
  ink: [5, 9, 11, 255],
  shadow: [7, 10, 11, 120],
  boot: [14, 20, 22, 255],
  cloth: [35, 47, 50, 255],
  clothHi: [68, 84, 84, 255],
  armor: [65, 75, 51, 255],
  armorHi: [112, 121, 73, 255],
  helmet: [27, 35, 36, 255],
  helmetHi: [63, 76, 75, 255],
  visor: [70, 178, 176, 255],
  visorHi: [148, 239, 224, 255],
  gun: [24, 31, 34, 255],
  gunHi: [93, 109, 108, 255],
  sling: [133, 104, 56, 255],
  warning: [222, 126, 40, 255],
  red: [205, 53, 46, 255],
  medical: [220, 226, 216, 255],
  flash: [255, 230, 142, 255],
};

function put(frame, direction, x, y, color) {
  if (frame < 0 || frame >= FRAME_COUNT
      || direction < 0 || direction >= DIRECTION_COUNT
      || x < 0 || x >= FRAME_W || y < 0 || y >= FRAME_H) return;
  const px = frame * FRAME_W + x;
  const py = direction * FRAME_H + y;
  pixels.set(color, (py * WIDTH + px) * 4);
}

function rect(frame, direction, x, y, width, height, color) {
  for (let yy = y; yy < y + height; yy += 1) {
    for (let xx = x; xx < x + width; xx += 1) {
      put(frame, direction, xx, yy, color);
    }
  }
}

function line(frame, direction, x0, y0, x1, y1, color) {
  let dx = Math.abs(x1 - x0);
  const sx = x0 < x1 ? 1 : -1;
  let dy = -Math.abs(y1 - y0);
  const sy = y0 < y1 ? 1 : -1;
  let error = dx + dy;
  while (true) {
    put(frame, direction, x0, y0, color);
    if (x0 === x1 && y0 === y1) break;
    const twice = error * 2;
    if (twice >= dy) { error += dy; x0 += sx; }
    if (twice <= dx) { error += dx; y0 += sy; }
  }
}

function muzzle(direction) {
  return {
    x: 6 + direction.x * 5,
    y: 8 + direction.y * 4,
  };
}

function drawOperator(frame, directionIndex, state, phase) {
  const facing = directions[directionIndex];
  const bob = state === "move" && phase % 2 === 1 ? 1 : 0;
  const gait = state === "move" ? (phase % 3) - 1 : 0;
  if (state === "down") {
    drawDown(frame, directionIndex, phase);
    return;
  }

  rect(frame, directionIndex, 3, 14, 7, 1, C.shadow);

  // Boots remain anchored to y=14 for every standing state and direction.
  line(frame, directionIndex, 5, 10 + bob, 4 + gait, 13, C.ink);
  line(frame, directionIndex, 7, 10 + bob, 8 - gait, 13, C.ink);
  rect(frame, directionIndex, 3 + gait, 14, 3, 1, C.boot);
  rect(frame, directionIndex, 7 - gait, 14, 3, 1, C.boot);

  // Backpack, plate carrier and helmet establish one stable silhouette.
  rect(frame, directionIndex, 2, 5 + bob, 2, 6, C.ink);
  rect(frame, directionIndex, 3, 6 + bob, 7, 6, C.ink);
  rect(frame, directionIndex, 4, 6 + bob, 5, 5, C.armor);
  rect(frame, directionIndex, 5, 7 + bob, 3, 1, C.armorHi);
  rect(frame, directionIndex, 4, 9 + bob, 5, 1, C.cloth);
  rect(frame, directionIndex, 4, 1 + bob, 5, 1, C.ink);
  rect(frame, directionIndex, 3, 2 + bob, 7, 4, C.ink);
  rect(frame, directionIndex, 4, 2 + bob, 5, 3, C.helmet);
  put(frame, directionIndex,
    6 + facing.x * 2, 4 + bob + facing.y, C.visorHi);
  put(frame, directionIndex,
    6 + facing.x, 4 + bob + facing.y, C.visor);

  const raised = state === "aim" || state === "fire"
    || state === "reload" || state === "hit";
  const target = muzzle(facing);
  const startX = 6;
  const startY = raised ? 8 + bob : 10 + bob;
  if (state === "reload") {
    line(frame, directionIndex, 4, 7 + bob, 6, 9 + bob, C.clothHi);
    line(frame, directionIndex, 9, 7 + bob, 7, 9 + bob, C.clothHi);
    rect(frame, directionIndex, 5, 8 + bob, 5, 2, C.gun);
    rect(frame, directionIndex, 6 + phase % 2, 10 + bob, 2, 3, C.sling);
  } else if (state === "medical") {
    line(frame, directionIndex, 4, 7 + bob, 6, 9 + bob, C.clothHi);
    line(frame, directionIndex, 9, 7 + bob, 7, 9 + bob, C.clothHi);
    rect(frame, directionIndex, 5, 8 + bob, 5, 4, C.red);
    rect(frame, directionIndex, 7, 8 + bob, 1, 4, C.medical);
    rect(frame, directionIndex, 6, 9 + bob, 3, 2, C.medical);
    if (phase >= 2) put(frame, directionIndex, 10, 7 + bob, C.visorHi);
  } else if (state === "extract") {
    line(frame, directionIndex, 4, 7 + bob, 2, 4 + bob, C.clothHi);
    rect(frame, directionIndex, 1, 2 + bob, 3, 3, C.ink);
    put(frame, directionIndex, 2, 3 + bob,
      phase % 2 === 0 ? C.visor : C.visorHi);
    line(frame, directionIndex, 9, 7 + bob, 10, 10 + bob, C.clothHi);
    if (phase >= 2) {
      put(frame, directionIndex, 1, 1 + bob, C.flash);
      put(frame, directionIndex, 10, 5 + bob, C.warning);
    }
  } else {
    line(frame, directionIndex, startX, startY,
      target.x, target.y, C.ink);
    line(frame, directionIndex, startX, startY - 1,
      target.x - facing.x, target.y - facing.y, C.gunHi);
    put(frame, directionIndex, startX, startY, C.gun);
    if (state === "fire" && phase <= 1) {
      put(frame, directionIndex, target.x, target.y, C.flash);
      put(frame, directionIndex,
        target.x - facing.y, target.y + facing.x, C.warning);
    }
  }

  if (state === "hit") {
    put(frame, directionIndex, 3 - Math.min(1, facing.x), 6 + bob, C.warning);
    if (phase === 1) put(frame, directionIndex, 2, 7 + bob, C.red);
  }
}

function drawDown(frame, direction, phase) {
  const y = 9 + Math.min(3, phase);
  rect(frame, direction, 2, y, 9, 3, C.ink);
  rect(frame, direction, 3, y, 6, 2, C.armor);
  rect(frame, direction, 8, y - 1, 3, 2, C.helmet);
  line(frame, direction, 2, Math.min(14, y + 3),
    10, Math.min(14, y + 3), C.gunHi);
  rect(frame, direction, 3, 14, 7, 1, C.shadow);
  if (phase < 2) put(frame, direction, 9, y - 1, C.visor);
}

for (let direction = 0; direction < directions.length; direction += 1) {
  for (let frame = 0; frame < FRAME_COUNT; frame += 1) {
    const state = states.find(
      (candidate) => frame >= candidate.from && frame <= candidate.to,
    );
    drawOperator(frame, direction, state.name, frame - state.from);
  }
}

mkdirSync(dirname(output), { recursive: true });
mkdirSync(dirname(manifestOutput), { recursive: true });
const temp = mkdtempSync(join(tmpdir(), "bukov-operator-original-"));
const raw = join(temp, "bukov_operator.rgba");
try {
  writeFileSync(raw, pixels);
  const encoded = spawnSync("ffmpeg", [
    "-y", "-hide_banner", "-loglevel", "error",
    "-f", "rawvideo", "-pixel_format", "rgba",
    "-video_size", `${WIDTH}x${HEIGHT}`, "-framerate", "1",
    "-i", raw, "-frames:v", "1", "-compression_level", "9", output,
  ], { stdio: "inherit" });
  if (encoded.status !== 0) process.exit(encoded.status ?? 1);
  const sha256 = createHash("sha256")
    .update(readFileSync(output)).digest("hex");
  const manifest = {
    schemaVersion: 1,
    atlas: "sprites/bukov_operator.png",
    width: WIDTH,
    height: HEIGHT,
    frameWidth: FRAME_W,
    frameHeight: FRAME_H,
    frameCount: FRAME_COUNT,
    directions: directions.map((direction, row) => ({
      name: direction.name,
      row,
      vector: [direction.x, direction.y],
      footAnchor: [6, 14],
      muzzleAnchor: [muzzle(direction).x, muzzle(direction).y],
    })),
    states,
    sha256,
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
