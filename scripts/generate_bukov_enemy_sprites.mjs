#!/usr/bin/env node

/**
 * Generates the thirteen original Bukov enemy sprite sheets used by realtime
 * raid. The drawings are deliberately authored from geometric primitives
 * rather than copied from the host dungeon atlases.
 *
 * Sheet contract:
 *   16x18 px frames, 21 frames across
 *   idle 0-1, attack 2-3, walk 4-7, death 8-10, hit 11-12,
 *   archetype action 13-15, White Line encounter phases 16-20
 *
 * No npm packages are required. ffmpeg is only used to encode RGBA into PNG.
 */

import { createHash } from "node:crypto";
import {
  mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync,
} from "node:fs";
import { join } from "node:path";
import { tmpdir } from "node:os";
import { spawnSync } from "node:child_process";

const FRAME_W = 16;
const FRAME_H = 18;
const FRAMES = 21;
const WIDTH = FRAME_W * FRAMES;
const HEIGHT = FRAME_H;
const outputDir = process.argv[2] ?? "core/src/main/assets/sprites/bukov";
const manifestOutput = process.argv[3]
  ?? join(outputDir, "enemy_animation_manifest.json");

const palette = {
  outline: [9, 13, 15, 255],
  shadow: [18, 24, 26, 255],
  steel: [59, 72, 74, 255],
  steelHi: [116, 132, 128, 255],
  cloth: [37, 47, 46, 255],
  olive: [73, 82, 56, 255],
  oliveHi: [123, 130, 79, 255],
  tan: [126, 104, 73, 255],
  skin: [167, 125, 91, 255],
  visor: [83, 179, 178, 255],
  cyan: [63, 207, 212, 255],
  amber: [238, 158, 54, 255],
  red: [191, 47, 42, 255],
  white: [211, 218, 208, 255],
  boss: [215, 211, 190, 255],
  bossShadow: [88, 88, 84, 255],
};

function image() {
  return Buffer.alloc(WIDTH * HEIGHT * 4);
}

function put(buffer, frame, x, y, color) {
  if (x < 0 || x >= FRAME_W || y < 0 || y >= FRAME_H) return;
  const offset = ((y * WIDTH) + frame * FRAME_W + x) * 4;
  buffer[offset] = color[0];
  buffer[offset + 1] = color[1];
  buffer[offset + 2] = color[2];
  buffer[offset + 3] = color[3];
}

function rect(buffer, frame, x, y, w, h, color) {
  for (let yy = y; yy < y + h; yy += 1) {
    for (let xx = x; xx < x + w; xx += 1) put(buffer, frame, xx, yy, color);
  }
}

function line(buffer, frame, x0, y0, x1, y1, color) {
  let dx = Math.abs(x1 - x0);
  let sx = x0 < x1 ? 1 : -1;
  let dy = -Math.abs(y1 - y0);
  let sy = y0 < y1 ? 1 : -1;
  let error = dx + dy;
  while (true) {
    put(buffer, frame, x0, y0, color);
    if (x0 === x1 && y0 === y1) break;
    const twice = 2 * error;
    if (twice >= dy) {
      error += dy;
      x0 += sx;
    }
    if (twice <= dx) {
      error += dx;
      y0 += sy;
    }
  }
}

function humanPose(frame) {
  if (frame >= 8 && frame <= 10) return { dead: frame - 8 };
  const walk = frame >= 4 && frame <= 7 ? frame - 4 : -1;
  const attack = frame === 2 || frame === 3;
  const hit = frame === 11 || frame === 12;
  const special = frame >= 13 && frame <= 15 ? frame - 13 : -1;
  return {
    dead: -1,
    bob: frame === 1 || walk === 1 || walk === 3
      || (special >= 0 && special !== 1) ? 1 : 0,
    leftLeg: walk === 0 || walk === 2 ? -1 : 0,
    rightLeg: walk === 1 || walk === 3 ? 1 : 0,
    attack,
    hit,
    special,
    recoil: frame === 3 ? 1 : 0,
  };
}

function drawHuman(buffer, frame, style) {
  const pose = humanPose(frame);
  if (pose.dead >= 0) {
    const y = 14 + pose.dead;
    rect(buffer, frame, 2, y, 11, 2, palette.outline);
    rect(buffer, frame, 4, y - 1, 7, 2, style.coat);
    rect(buffer, frame, 11, y - 1, 3, 1, style.accent);
    if (pose.dead === 0) rect(buffer, frame, 1, 11, 3, 3, style.helmet);
    return;
  }

  const y = pose.bob;
  // Ground shadow and legs.
  rect(buffer, frame, 4, 16, 8, 1, [7, 10, 11, 125]);
  line(buffer, frame, 6, 12 + y, 5 + pose.leftLeg, 16, palette.outline);
  line(buffer, frame, 10, 12 + y, 11 + pose.rightLeg, 16, palette.outline);
  put(buffer, frame, 4 + pose.leftLeg, 16, palette.steel);
  put(buffer, frame, 12 + pose.rightLeg, 16, palette.steel);

  // Torso, pack, shoulder blocks.
  rect(buffer, frame, 4, 7 + y, 8, 6, palette.outline);
  rect(buffer, frame, 5, 8 + y, 6, 5, style.coat);
  rect(buffer, frame, 3, 8 + y, 2, 4, style.pack);
  put(buffer, frame, 11, 8 + y, style.accent);
  rect(buffer, frame, 6, 10 + y, 4, 1, style.vest);

  // Helmet/head silhouette.
  rect(buffer, frame, 5, 3 + y, 6, 5, palette.outline);
  rect(buffer, frame, 6, 3 + y, 5, 4, style.helmet);
  rect(buffer, frame, 7, 6 + y, 4, 1, style.visor);
  put(buffer, frame, 5, 4 + y, style.helmetHi);
  if (style.cap) {
    rect(buffer, frame, 4, 3 + y, 7, 1, style.helmet);
    rect(buffer, frame, 10, 4 + y, 3, 1, style.helmet);
  }
  if (style.hood) {
    put(buffer, frame, 4, 5 + y, style.helmet);
    put(buffer, frame, 11, 5 + y, style.helmet);
    put(buffer, frame, 5, 2 + y, style.helmet);
  }
  if (style.shoulder) {
    rect(buffer, frame, 2, 7 + y, 3, 2, style.shoulder);
    rect(buffer, frame, 11, 7 + y, 3, 2, style.shoulder);
  }
  if (style.antenna) {
    line(buffer, frame, 10, 3 + y, 12, 0 + y, style.antenna);
    put(buffer, frame, 12, 0 + y,
      frame % 2 === 0 ? palette.cyan : palette.amber);
  }

  const reloading = pose.special >= 0 && style.specialAction === "reload";
  const rushing = pose.special >= 0 && style.specialAction === "rush";
  const gunY = (pose.attack ? 8 : reloading ? 12 : rushing ? 8 : 10) + y;
  const gunStart = pose.attack || rushing ? 7 : 6;
  const gunEnd = pose.attack
    ? 15 - pose.recoil : reloading ? 11 : rushing ? 15 : 13;
  line(buffer, frame, gunStart, gunY, gunEnd, gunY, palette.outline);
  line(buffer, frame, gunStart + 1, gunY - 1, gunEnd - 2, gunY - 1, style.gun);
  put(buffer, frame, gunStart + 3, gunY + 1, style.grip);
  if (style.scope) rect(buffer, frame, 9, gunY - 2, 3, 1, style.scope);
  if (pose.attack && frame === 2) {
    put(buffer, frame, 15, gunY, palette.white);
    put(buffer, frame, 14, gunY - 1, palette.amber);
  }
  if (pose.hit) {
    // Independent impact poses: a lateral body jolt plus a two-frame spark.
    const impactX = frame === 11 ? 3 : 12;
    put(buffer, frame, impactX, 6 + y, palette.white);
    put(buffer, frame, impactX + (frame === 11 ? -1 : 1), 5 + y, palette.red);
    line(buffer, frame, impactX, 7 + y,
      impactX + (frame === 11 ? 2 : -2), 9 + y, palette.red);
  } else if (reloading) {
    // Gun low, magazine removed, then seated: readable even at 1x.
    const magazineY = 11 + pose.special + y;
    rect(buffer, frame, 8, magazineY, 2, 3, style.grip);
    put(buffer, frame, 10, magazineY + 1,
      pose.special === 2 ? palette.white : style.accent);
  } else if (rushing) {
    // Forward weapon silhouette and dust trail distinguish rush from walk.
    line(buffer, frame, 11, 8 + y, 15, 6 + y, style.accent);
    put(buffer, frame, Math.max(0, 3 - pose.special), 16, palette.tan);
    put(buffer, frame, Math.max(0, 1 - pose.special), 15, palette.steelHi);
  } else if (pose.special >= 0) {
    // Non-firearm human signature/command pulse.
    put(buffer, frame, 2, 5 + y,
      pose.special === 1 ? palette.white : style.accent);
    put(buffer, frame, 13, 5 + y,
      pose.special === 1 ? palette.white : palette.red);
    rect(buffer, frame, 6, 8 + y,
      2 + pose.special, 1, style.accent);
  }
}

function drawDrone(buffer, frame, style = {}) {
  const shell = style.shell ?? palette.steel;
  const shellHi = style.shellHi ?? palette.steelHi;
  const eye = style.eye ?? palette.cyan;
  const leg = style.leg ?? palette.steel;
  if (frame >= 8 && frame <= 10) {
    const d = frame - 8;
    rect(buffer, frame, 5 - d * 2, 12 + d, 6, 2, shell);
    rect(buffer, frame, 11 + d, 9 + d, 2, 2, palette.red);
    return;
  }
  const run = frame >= 4 && frame <= 7 ? frame - 4 : frame;
  const bob = run % 2;
  rect(buffer, frame, 3, 7 + bob, 10, 5, palette.outline);
  rect(buffer, frame, 4, 7 + bob, 8, 4, shell);
  rect(buffer, frame, 6, 8 + bob, 4, 2, palette.shadow);
  rect(buffer, frame, 7, 8 + bob, 2, 1, frame === 2 ? palette.amber : eye);
  line(buffer, frame, 2, 6 + bob, 5, 8 + bob, shellHi);
  line(buffer, frame, 13, 6 + bob, 10, 8 + bob, shellHi);
  rect(buffer, frame, 1, 5 + bob, 3, 1, palette.outline);
  rect(buffer, frame, 12, 5 + bob, 3, 1, palette.outline);
  line(buffer, frame, 5, 12 + bob, 4, 15, leg);
  line(buffer, frame, 10, 12 + bob, 11, 15, leg);
  put(buffer, frame, 4, 15, eye);
  put(buffer, frame, 11, 15, eye);
  if (frame === 2) line(buffer, frame, 8, 10 + bob, 15, 10 + bob, palette.amber);
  if (frame === 11 || frame === 12) {
    // Dedicated hit: shell sparks on alternating sides.
    const sparkX = frame === 11 ? 2 : 13;
    put(buffer, frame, sparkX, 8 + bob, palette.white);
    put(buffer, frame, sparkX + (frame === 11 ? -1 : 1), 7 + bob, palette.red);
    put(buffer, frame, sparkX, 6 + bob, palette.amber);
  } else if (frame >= 13 && frame <= 15) {
    // Sensor/broadcast sweep, not a recycled attack pose.
    const radius = frame - 12;
    put(buffer, frame, 8 - radius, 4 + bob, eye);
    put(buffer, frame, 8 + radius, 4 + bob, eye);
    put(buffer, frame, 8 - radius, 12 + bob, eye);
    put(buffer, frame, 8 + radius, 12 + bob, eye);
  }
  if (style.antenna) {
    line(buffer, frame, 8, 7 + bob, 9, 2 + bob, shellHi);
    put(buffer, frame, 9, 1 + bob, frame % 2 ? palette.amber : eye);
  }
}

function drawBoss(buffer, frame) {
  if (frame >= 8 && frame <= 10) {
    const d = frame - 8;
    rect(buffer, frame, 1, 13 + d, 14, 3 - Math.min(d, 2), palette.outline);
    rect(buffer, frame, 3, 12 + d, 10, 2, palette.bossShadow);
    return;
  }
  const bob = frame === 1 || frame === 5 || frame === 7 ? 1 : 0;
  // Wide white raincoat silhouette with an unnaturally thin black face.
  rect(buffer, frame, 3, 4 + bob, 10, 11, palette.outline);
  rect(buffer, frame, 4, 5 + bob, 8, 9, palette.boss);
  rect(buffer, frame, 6, 3 + bob, 5, 5, palette.outline);
  rect(buffer, frame, 7, 4 + bob, 3, 4, palette.shadow);
  put(buffer, frame, 9, 5 + bob, palette.red);
  line(buffer, frame, 3, 8 + bob, 1, 15, palette.bossShadow);
  line(buffer, frame, 12, 8 + bob, 14, 15, palette.bossShadow);
  // Ground hem is the anchor; upper-body bob must never move the feet.
  rect(buffer, frame, 2, 14, 12, 2, palette.outline);
  if (frame === 2 || frame === 3) {
    line(buffer, frame, 8, 7 + bob, 15, 5 + bob, palette.white);
    put(buffer, frame, 15, 5 + bob, palette.cyan);
  }
  if (frame === 11 || frame === 12) {
    // Independent impact poses, separate from encounter phase films.
    const impactX = frame === 11 ? 3 : 12;
    put(buffer, frame, impactX, 7 + bob, palette.white);
    put(buffer, frame, impactX + (frame === 11 ? -1 : 1), 6 + bob, palette.red);
    line(buffer, frame, impactX, 8 + bob, 8, 10 + bob, palette.red);
  } else if (frame >= 13 && frame <= 15) {
    // Boss signature cast: core charges before the phase attack.
    const charge = frame - 12;
    rect(buffer, frame, 7 - Math.floor(charge / 2), 8 + bob,
      2 + charge, 2 + charge, frame === 14 ? palette.cyan : palette.red);
  } else if (frame === 16 || frame === 17) {
    // Phase one: umbrella-like shield arc.
    line(buffer, frame, 1, 6, 8, 1, palette.amber);
    line(buffer, frame, 8, 1, 15, 6, palette.amber);
    line(buffer, frame, 8, 1, 8, 15, palette.bossShadow);
    if (frame === 17) {
      put(buffer, frame, 1, 7, palette.cyan);
      put(buffer, frame, 15, 7, palette.cyan);
    }
  } else if (frame === 18) {
    // Phase two: decoy eyes flank the original silhouette.
    put(buffer, frame, 2, 6, palette.red);
    put(buffer, frame, 13, 6, palette.red);
    put(buffer, frame, 1, 10, palette.red);
    put(buffer, frame, 14, 10, palette.red);
  } else if (frame === 19) {
    // Phase three: fog-overload lamps.
    rect(buffer, frame, 1, 3, 2, 8, palette.cyan);
    rect(buffer, frame, 13, 3, 2, 8, palette.cyan);
  } else if (frame === 20) {
    // Vulnerability window: exposed red core.
    rect(buffer, frame, 6, 8, 4, 4, palette.red);
    rect(buffer, frame, 7, 9, 2, 2, palette.white);
  }
}

const sheets = [
  {
    name: "scavenger.png",
    enemyIds: ["melee_rusher"],
    family: "light-rusher",
    tier: "normal",
    specialAction: "rush",
    draw: (b, f) => drawHuman(b, f, {
      coat: palette.tan, pack: palette.olive, vest: palette.shadow,
      accent: palette.amber, helmet: palette.cloth, helmetHi: palette.tan,
      visor: palette.skin, gun: palette.steel, grip: palette.tan,
      scope: palette.outline, cap: true,
      specialAction: "rush",
    }),
  },
  {
    name: "gunner.png",
    enemyIds: ["scavenger_gunner"],
    family: "rifle-scavenger",
    tier: "normal",
    specialAction: "reload",
    draw: (b, f) => drawHuman(b, f, {
      coat: palette.olive, pack: palette.shadow, vest: palette.steel,
      accent: palette.red, helmet: palette.shadow, helmetHi: palette.steel,
      visor: palette.visor, gun: palette.steelHi, grip: palette.cloth,
      scope: palette.cyan, cap: false,
      specialAction: "reload",
    }),
  },
  {
    name: "armored.png",
    enemyIds: ["iron_clasp_guard"],
    family: "heavy-guard",
    tier: "elite",
    specialAction: "reload",
    draw: (b, f) => drawHuman(b, f, {
      coat: palette.steel, pack: palette.outline, vest: palette.steelHi,
      accent: palette.amber, helmet: palette.steel, helmetHi: palette.steelHi,
      visor: palette.amber, gun: palette.steelHi, grip: palette.shadow,
      scope: palette.amber, cap: false,
      specialAction: "reload",
    }),
  },
  {
    name: "captain.png",
    enemyIds: ["iron_clasp_captain"],
    family: "command-elite",
    tier: "elite",
    specialAction: "reload",
    draw: (b, f) => drawHuman(b, f, {
      coat: [62, 49, 45, 255], pack: palette.outline, vest: [120, 72, 53, 255],
      accent: palette.red, helmet: palette.outline, helmetHi: [94, 75, 65, 255],
      visor: palette.red, gun: palette.steelHi, grip: palette.shadow,
      scope: palette.red, cap: false,
      specialAction: "reload",
    }),
  },
  {
    name: "drone.png",
    enemyIds: ["sensor_doll"],
    family: "sensor-drone",
    tier: "normal",
    specialAction: "scan",
    draw: drawDrone,
  },
  {
    name: "white_line.png",
    enemyIds: ["boss_white_line"],
    family: "white-line-boss",
    tier: "boss",
    specialAction: "phase_cast",
    draw: drawBoss,
  },
  {
    name: "alley_scout.png",
    enemyIds: ["alley_scout"],
    family: "hooded-scout",
    tier: "normal",
    specialAction: "reload",
    draw: (b, f) => drawHuman(b, f, {
      coat: [31, 74, 75, 255], pack: palette.shadow, vest: palette.cyan,
      accent: palette.cyan, helmet: [20, 48, 50, 255],
      helmetHi: palette.visor, visor: palette.cyan, gun: palette.steelHi,
      grip: palette.cloth, scope: palette.cyan, hood: true,
      specialAction: "reload",
    }),
  },
  {
    name: "depot_shotgunner.png",
    enemyIds: ["depot_shotgunner"],
    family: "orange-heavy-shotgun",
    tier: "normal",
    specialAction: "reload",
    draw: (b, f) => drawHuman(b, f, {
      coat: [105, 61, 36, 255], pack: palette.outline,
      vest: [179, 92, 40, 255], accent: palette.amber,
      helmet: palette.steel, helmetHi: palette.amber, visor: palette.amber,
      gun: palette.steelHi, grip: palette.tan, scope: palette.outline,
      shoulder: [179, 92, 40, 255],
      specialAction: "reload",
    }),
  },
  {
    name: "line_rifleman.png",
    enemyIds: ["line_rifleman"],
    family: "blue-line-rifle",
    tier: "normal",
    specialAction: "reload",
    draw: (b, f) => drawHuman(b, f, {
      coat: [40, 58, 87, 255], pack: palette.outline,
      vest: [63, 101, 139, 255], accent: palette.white,
      helmet: [31, 45, 70, 255], helmetHi: [89, 123, 156, 255],
      visor: palette.cyan, gun: palette.steelHi, grip: palette.shadow,
      scope: palette.cyan,
      specialAction: "reload",
    }),
  },
  {
    name: "fog_stalker.png",
    enemyIds: ["fog_stalker"],
    family: "dark-fog-stalker",
    tier: "normal",
    specialAction: "rush",
    draw: (b, f) => drawHuman(b, f, {
      coat: [20, 42, 43, 255], pack: [12, 26, 27, 255],
      vest: [35, 78, 74, 255], accent: [95, 214, 179, 255],
      helmet: [10, 29, 31, 255], helmetHi: [44, 91, 84, 255],
      visor: [95, 214, 179, 255], gun: palette.steel,
      grip: palette.shadow, scope: [95, 214, 179, 255], hood: true,
      shoulder: [20, 61, 59, 255],
      specialAction: "rush",
    }),
  },
  {
    name: "signal_operator.png",
    enemyIds: ["signal_operator"],
    family: "signal-walker",
    tier: "normal",
    specialAction: "scan",
    draw: (b, f) => drawDrone(b, f, {
      shell: [73, 62, 91, 255], shellHi: [144, 115, 164, 255],
      eye: [224, 93, 231, 255], leg: [68, 84, 104, 255],
      antenna: true,
    }),
  },
  {
    name: "iron_clasp_marksman.png",
    enemyIds: ["iron_clasp_marksman"],
    family: "long-optic-elite",
    tier: "elite",
    specialAction: "reload",
    draw: (b, f) => drawHuman(b, f, {
      coat: [75, 81, 85, 255], pack: palette.outline,
      vest: [137, 146, 141, 255], accent: palette.cyan,
      helmet: palette.steel, helmetHi: palette.white, visor: palette.cyan,
      gun: palette.white, grip: palette.shadow, scope: palette.cyan,
      cap: true, antenna: palette.cyan,
      specialAction: "reload",
    }),
  },
  {
    name: "breach_veteran.png",
    enemyIds: ["breach_veteran"],
    family: "red-breach-elite",
    tier: "elite",
    specialAction: "reload",
    draw: (b, f) => drawHuman(b, f, {
      coat: [77, 35, 31, 255], pack: palette.outline,
      vest: [139, 52, 42, 255], accent: palette.red,
      helmet: [61, 27, 26, 255], helmetHi: [156, 62, 48, 255],
      visor: palette.amber, gun: palette.steelHi, grip: palette.shadow,
      scope: palette.red, shoulder: [156, 62, 48, 255],
      specialAction: "reload",
    }),
  },
];

function encode(name, pixels) {
  mkdirSync(outputDir, { recursive: true });
  const temp = mkdtempSync(join(tmpdir(), "bukov-enemy-"));
  const rgba = join(temp, `${name}.rgba`);
  const target = join(outputDir, name);
  try {
    writeFileSync(rgba, pixels);
    const result = spawnSync("ffmpeg", [
      "-y", "-hide_banner", "-loglevel", "error",
      "-f", "rawvideo", "-pixel_format", "rgba",
      "-video_size", `${WIDTH}x${HEIGHT}`, "-framerate", "1",
      "-i", rgba, "-frames:v", "1", "-compression_level", "9", target,
    ], { stdio: "inherit" });
    if (result.status !== 0) process.exit(result.status ?? 1);
    return createHash("sha256")
      .update(readFileSync(target)).digest("hex");
  } finally {
    rmSync(temp, { recursive: true, force: true });
  }
}

const manifest = {
  schemaVersion: 2,
  frameWidth: FRAME_W,
  frameHeight: FRAME_H,
  frameCount: FRAMES,
  actions: {
    idle: [0, 1],
    attack: [2, 3],
    walk: [4, 7],
    death: [8, 10],
    hit: [11, 12],
    special: [13, 15],
    bossEncounterPhase: [16, 20],
  },
  sheets: [],
};
for (const sheet of sheets) {
  const pixels = image();
  for (let frame = 0; frame < FRAMES; frame += 1) sheet.draw(pixels, frame);
  const sha256 = encode(sheet.name, pixels);
  manifest.sheets.push({
    asset: `sprites/bukov/${sheet.name}`,
    enemyIds: sheet.enemyIds,
    family: sheet.family,
    tier: sheet.tier,
    specialAction: sheet.specialAction,
    sha256,
  });
}

mkdirSync(join(manifestOutput, ".."), { recursive: true });
writeFileSync(manifestOutput, `${JSON.stringify(manifest, null, 2)}\n`);
console.log(`Generated ${sheets.length} Bukov enemy sheets in ${outputDir}`);
