#!/usr/bin/env node

/**
 * Generates the original first-raid industrial landmark atlas.
 *
 * Atlas contract: 320x32 RGBA, ten transparent 32x32 frames:
 * 0 archive cabinet; 1/2/3 repair-gate left/middle/right; 4 pump station;
 * 5 fixed extraction; 6 conditional extraction; 7 industrial crate;
 * 8 concrete cover; 9 sandbag cover.
 *
 * All pixels are project-original geometric drawing. No source images are read.
 */

import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { tmpdir } from "node:os";
import { spawnSync } from "node:child_process";

const FRAME = 32;
const COUNT = 10;
const WIDTH = FRAME * COUNT;
const HEIGHT = FRAME;
const output = process.argv[2]
  ?? "core/src/main/assets/environment/bukov/first_raid_landmarks.png";
const pixels = Buffer.alloc(WIDTH * HEIGHT * 4);

const C = {
  ink: [6, 10, 12, 255],
  deep: [15, 23, 26, 255],
  steel: [43, 58, 61, 255],
  steel2: [74, 91, 91, 255],
  steelHi: [130, 150, 145, 255],
  white: [219, 225, 214, 255],
  olive: [69, 78, 49, 255],
  oliveHi: [120, 126, 72, 255],
  rust: [117, 58, 35, 255],
  rustHi: [177, 83, 41, 255],
  amber: [226, 145, 40, 255],
  amberHi: [255, 204, 88, 255],
  cyan: [37, 181, 184, 255],
  cyanHi: [115, 239, 225, 255],
  cyanGlow: [37, 181, 184, 100],
  amberGlow: [226, 145, 40, 100],
  red: [183, 45, 42, 255],
  concrete: [91, 96, 91, 255],
  concreteHi: [148, 148, 134, 255],
  sand: [106, 91, 61, 255],
  sandHi: [159, 136, 86, 255],
};

function put(frame, x, y, color) {
  if (frame < 0 || frame >= COUNT || x < 0 || x >= FRAME || y < 0 || y >= FRAME) return;
  const offset = (y * WIDTH + frame * FRAME + x) * 4;
  pixels[offset] = color[0];
  pixels[offset + 1] = color[1];
  pixels[offset + 2] = color[2];
  pixels[offset + 3] = color[3];
}

function rect(frame, x, y, w, h, color) {
  for (let yy = y; yy < y + h; yy += 1) {
    for (let xx = x; xx < x + w; xx += 1) put(frame, xx, yy, color);
  }
}

function border(frame, x, y, w, h, edge, fill) {
  rect(frame, x, y, w, h, edge);
  rect(frame, x + 1, y + 1, w - 2, h - 2, fill);
}

function line(frame, x0, y0, x1, y1, color) {
  let dx = Math.abs(x1 - x0);
  let sx = x0 < x1 ? 1 : -1;
  let dy = -Math.abs(y1 - y0);
  let sy = y0 < y1 ? 1 : -1;
  let error = dx + dy;
  while (true) {
    put(frame, x0, y0, color);
    if (x0 === x1 && y0 === y1) break;
    const e2 = error * 2;
    if (e2 >= dy) {
      error += dy;
      x0 += sx;
    }
    if (e2 <= dx) {
      error += dx;
      y0 += sy;
    }
  }
}

function ring(frame, cx, cy, radius, color) {
  for (let angle = 0; angle < 360; angle += 6) {
    const r = angle * Math.PI / 180;
    put(frame, Math.round(cx + Math.cos(r) * radius), Math.round(cy + Math.sin(r) * radius), color);
  }
}

// 0: archive cabinet, two drawers, indexed binder visible behind cracked door.
border(0, 6, 2, 20, 29, C.ink, C.steel);
rect(0, 8, 4, 16, 3, C.steelHi);
rect(0, 8, 9, 16, 8, C.deep);
rect(0, 9, 10, 14, 6, C.olive);
rect(0, 11, 11, 3, 5, C.amber);
rect(0, 15, 11, 6, 1, C.white);
rect(0, 15, 13, 5, 1, C.steelHi);
rect(0, 8, 19, 16, 9, C.steel2);
border(0, 13, 21, 6, 3, C.ink, C.steelHi);
put(0, 23, 15, C.cyanHi);
line(0, 6, 29, 3, 31, C.rust);
line(0, 26, 29, 29, 31, C.rust);

// 1..3: a continuous 3-cell maintenance gate (96x32 when composed L/M/R).
for (let f = 1; f <= 3; f += 1) {
  rect(f, 0, 1, 32, 3, C.ink);
  rect(f, 0, 4, 32, 2, C.steelHi);
  rect(f, 0, 27, 32, 4, C.ink);
  for (let x = -8; x < 40; x += 12) {
    line(f, x, 7, x + 20, 27, C.rust);
    line(f, x + 3, 7, x + 23, 27, C.rustHi);
  }
  for (let y = 8; y < 27; y += 5) rect(f, 0, y, 32, 2, C.steel);
  rect(f, 0, 12, 32, 3, C.deep);
  rect(f, 0, 21, 32, 3, C.deep);
}
rect(1, 0, 0, 5, 32, C.ink);
rect(1, 3, 5, 4, 22, C.steel2);
rect(3, 27, 0, 5, 32, C.ink);
rect(3, 25, 5, 4, 22, C.steel2);
border(3, 20, 9, 8, 12, C.ink, C.deep);
rect(3, 22, 11, 4, 3, C.red);
rect(3, 22, 16, 4, 2, C.cyan);
put(3, 24, 17, C.cyanHi);
for (const f of [1, 2, 3]) {
  for (let x = 5; x < 31; x += 12) {
    line(f, x, 5, x + 5, 10, C.amber);
    line(f, x + 6, 5, x + 11, 10, C.ink);
  }
}

// 4: pump station with pipes, motor, pressure gauge, service hatch.
rect(4, 1, 9, 7, 14, C.ink);
rect(4, 2, 10, 6, 12, C.steel2);
rect(4, 24, 9, 7, 14, C.ink);
rect(4, 24, 10, 6, 12, C.steel2);
border(4, 7, 5, 18, 23, C.ink, C.steel);
rect(4, 9, 7, 14, 4, C.steelHi);
ring(4, 16, 17, 7, C.ink);
ring(4, 16, 17, 5, C.cyan);
rect(4, 15, 11, 3, 12, C.deep);
rect(4, 10, 16, 12, 3, C.deep);
put(4, 16, 17, C.cyanHi);
border(4, 11, 1, 10, 6, C.ink, C.deep);
line(4, 16, 5, 19, 2, C.amberHi);
put(4, 20, 6, C.red);
rect(4, 9, 26, 4, 4, C.ink);
rect(4, 20, 26, 4, 4, C.ink);

// 5: fixed extraction beacon, cyan landing ring and upward guidance mast.
ring(5, 16, 18, 13, C.cyanGlow);
ring(5, 16, 18, 10, C.cyan);
ring(5, 16, 18, 7, C.cyanHi);
rect(5, 14, 5, 5, 16, C.ink);
rect(5, 16, 3, 1, 15, C.cyanHi);
rect(5, 12, 18, 9, 5, C.steel2);
rect(5, 13, 19, 7, 2, C.steelHi);
line(5, 7, 18, 3, 14, C.cyanHi);
line(5, 7, 18, 3, 22, C.cyanHi);
line(5, 25, 18, 29, 14, C.cyanHi);
line(5, 25, 18, 29, 22, C.cyanHi);
put(5, 16, 1, C.white);

// 6: conditional extraction, amber checkpoint barrier and credential reader.
ring(6, 16, 18, 13, C.amberGlow);
border(6, 3, 6, 6, 22, C.ink, C.steel2);
border(6, 24, 6, 6, 22, C.ink, C.steel2);
line(6, 8, 12, 25, 19, C.ink);
line(6, 8, 10, 25, 17, C.amber);
for (let x = 10; x < 24; x += 5) put(6, x, 12 + Math.floor((x - 8) * 7 / 17), C.deep);
border(6, 21, 1, 9, 9, C.ink, C.deep);
rect(6, 23, 3, 5, 3, C.red);
rect(6, 23, 6, 5, 2, C.amber);
put(6, 27, 7, C.white);
rect(6, 4, 28, 4, 3, C.ink);
rect(6, 25, 28, 4, 3, C.ink);

// 7: rugged industrial loot crate with latches and hazard geometry.
border(7, 3, 7, 26, 20, C.ink, C.rust);
rect(7, 5, 9, 22, 5, C.rustHi);
rect(7, 5, 16, 22, 8, C.deep);
line(7, 8, 23, 15, 16, C.amber);
line(7, 17, 23, 25, 15, C.amber);
border(7, 6, 4, 6, 5, C.ink, C.steel2);
border(7, 21, 4, 6, 5, C.ink, C.steel2);
border(7, 13, 23, 7, 6, C.ink, C.steel);
rect(7, 15, 24, 3, 2, C.cyan);
rect(7, 6, 28, 5, 3, C.ink);
rect(7, 22, 28, 5, 3, C.ink);

// 8: chipped concrete road barrier with exposed reinforcing steel.
line(8, 3, 28, 7, 9, C.ink);
line(8, 28, 28, 24, 9, C.ink);
rect(8, 7, 7, 18, 19, C.ink);
rect(8, 8, 8, 16, 17, C.concrete);
rect(8, 9, 9, 14, 3, C.concreteHi);
line(8, 10, 22, 16, 15, C.deep);
line(8, 16, 15, 20, 19, C.deep);
put(8, 8, 18, [0, 0, 0, 0]);
put(8, 23, 13, [0, 0, 0, 0]);
line(8, 4, 30, 28, 30, C.ink);
line(8, 6, 7, 5, 3, C.rustHi);
line(8, 25, 7, 27, 3, C.rustHi);

// 9: two-layer sandbag firing position with a central observation notch.
const bags = [
  [3, 19], [9, 19], [15, 19], [21, 19], [26, 19],
  [6, 14], [12, 14], [20, 14],
  [2, 24], [8, 24], [14, 24], [20, 24], [26, 24],
];
for (const [x, y] of bags) {
  border(9, x, y, 6, 5, C.ink, C.sand);
  line(9, x + 1, y + 1, x + 4, y + 1, C.sandHi);
  put(9, x + 5, y + 2, C.sling ?? C.rust);
}
rect(9, 14, 12, 6, 5, C.deep);
rect(9, 15, 13, 4, 2, C.steelHi);
line(9, 1, 30, 31, 30, C.ink);

mkdirSync(dirname(output), { recursive: true });
const temp = mkdtempSync(join(tmpdir(), "bukov-landmarks-original-"));
const raw = join(temp, "first_raid_landmarks.rgba");
try {
  writeFileSync(raw, pixels);
  const encoded = spawnSync("ffmpeg", [
    "-y", "-hide_banner", "-loglevel", "error",
    "-f", "rawvideo", "-pixel_format", "rgba",
    "-video_size", `${WIDTH}x${HEIGHT}`, "-framerate", "1",
    "-i", raw, "-frames:v", "1", "-compression_level", "9", output,
  ], { stdio: "inherit" });
  if (encoded.status !== 0) process.exit(encoded.status ?? 1);
} finally {
  rmSync(temp, { recursive: true, force: true });
}
