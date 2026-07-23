#!/usr/bin/env node

/**
 * Generates Bukov's original 16x16 item/interaction atlas and manifest.
 *
 * All visible pixels are constructed from the primitives below. The generator
 * does not read, sample, trace, recolor or derive from any source image.
 * ffmpeg is used only to encode the generated RGBA buffer as PNG.
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

const FRAME = 16;
const output = process.argv[2]
  ?? "core/src/main/assets/sprites/bukov/items_interactions.png";
const manifestOutput = process.argv[3]
  ?? "core/src/main/assets/sprites/bukov/items_interactions_manifest.json";

const firearms = [
  ["firearm:needle_9", "FIREARM_NEEDLE_9", "pistol"],
  ["firearm:shuttle_9", "FIREARM_SHUTTLE_9", "smg"],
  ["firearm:ward_556", "FIREARM_WARD_556", "rifle"],
  ["firearm:mountain_762", "FIREARM_MOUNTAIN_762", "rifle"],
  ["firearm:bolt_12", "FIREARM_BOLT_12", "shotgun"],
  ["firearm:longstreet_762", "FIREARM_LONGSTREET_762", "marksman"],
  ["firearm:sentinel_9", "FIREARM_SENTINEL_9", "pistol"],
  ["firearm:sparrow_9", "FIREARM_SPARROW_9", "pistol"],
  ["firearm:hive_9", "FIREARM_HIVE_9", "smg"],
  ["firearm:whisper_9", "FIREARM_WHISPER_9", "smg"],
  ["firearm:jackal_9", "FIREARM_JACKAL_9", "smg"],
  ["firearm:river_556", "FIREARM_RIVER_556", "rifle"],
  ["firearm:foundry_762", "FIREARM_FOUNDRY_762", "rifle"],
  ["firearm:carbine_556", "FIREARM_CARBINE_556", "rifle"],
  ["firearm:breaker_12", "FIREARM_BREAKER_12", "shotgun"],
  ["firearm:rainstorm_12", "FIREARM_RAINSTORM_12", "shotgun"],
  ["firearm:watchtower_556", "FIREARM_WATCHTOWER_556", "marksman"],
  ["firearm:frontier_762", "FIREARM_FRONTIER_762", "marksman"],
];
const ammunition = [
  ["ammo:ammo_9_training", "AMMO_9_TRAINING"],
  ["ammo:ammo_9_standard", "AMMO_9_STANDARD"],
  ["ammo:ammo_9_subsonic", "AMMO_9_SUBSONIC"],
  ["ammo:ammo_556_standard", "AMMO_556_STANDARD"],
  ["ammo:ammo_556_armor_piercing", "AMMO_556_ARMOR_PIERCING"],
  ["ammo:ammo_762_standard", "AMMO_762_STANDARD"],
  ["ammo:ammo_762_expanding", "AMMO_762_EXPANDING"],
  ["ammo:ammo_12g_buckshot", "AMMO_12G_BUCKSHOT"],
];
const armor = [
  ["armor:soft_vest", "ARMOR_SOFT_VEST"],
  ["armor:patrol_vest", "ARMOR_PATROL_VEST"],
  ["armor:ceramic_rig", "ARMOR_CERAMIC_RIG"],
];
const backpacks = [
  ["backpack:scout_pack", "BACKPACK_SCOUT_PACK"],
  ["backpack:field_pack", "BACKPACK_FIELD_PACK"],
];
const medicalTools = [
  ["bandage", "MEDICAL_BANDAGE"],
  ["painkiller", "MEDICAL_PAINKILLER"],
  ["first_aid", "MEDICAL_FIRST_AID"],
  ["tourniquet", "MEDICAL_TOURNIQUET"],
  ["antiseptic", "MEDICAL_ANTISEPTIC"],
  ["splint", "MEDICAL_SPLINT"],
  ["stim", "MEDICAL_STIM"],
  ["tool_set", "TOOL_SET"],
];
const lootMission = [
  ["canned_food", "LOOT_CANNED_FOOD"],
  ["water_filter", "LOOT_WATER_FILTER"],
  ["bolts", "LOOT_BOLTS"],
  ["scrap_metal", "LOOT_SCRAP_METAL"],
  ["cloth_roll", "LOOT_CLOTH_ROLL"],
  ["battery", "LOOT_BATTERY"],
  ["ceramic_shard", "LOOT_CERAMIC_SHARD"],
  ["rubber_hose", "LOOT_RUBBER_HOSE"],
  ["sealed_coffee", "LOOT_SEALED_COFFEE"],
  ["copper_wire", "LOOT_COPPER_WIRE"],
  ["electric_motor", "LOOT_ELECTRIC_MOTOR"],
  ["bearing", "LOOT_BEARING"],
  ["circuit_board", "LOOT_CIRCUIT_BOARD"],
  ["fuel_can", "LOOT_FUEL_CAN"],
  ["welding_rod", "LOOT_WELDING_ROD"],
  ["pressure_gauge", "LOOT_PRESSURE_GAUGE"],
  ["relay_module", "LOOT_RELAY_MODULE"],
  ["copper_coil", "LOOT_COPPER_COIL"],
  ["machine_oil", "LOOT_MACHINE_OIL"],
  ["gold_watch", "LOOT_GOLD_WATCH"],
  ["encrypted_drive", "LOOT_ENCRYPTED_DRIVE"],
  ["antique_coin", "LOOT_ANTIQUE_COIN"],
  ["camera_lens", "LOOT_CAMERA_LENS"],
  ["military_chip", "LOOT_MILITARY_CHIP"],
  ["radio_crystal", "LOOT_RADIO_CRYSTAL"],
  ["optical_sensor", "LOOT_OPTICAL_SENSOR"],
  ["officer_badge", "LOOT_OFFICER_BADGE"],
  ["command_key", "LOOT_COMMAND_KEY"],
  ["prototype_core", "LOOT_PROTOTYPE_CORE"],
  ["mission:maintenance_archive", "MISSION_ARCHIVE"],
];
const interactions = [
  ["interaction:fixed_extraction", "FIXED_EXTRACTION"],
  ["interaction:conditional_extraction", "CONDITIONAL_EXTRACTION"],
  ["interaction:pump_station", "PUMP_STATION"],
];

const entries = [
  ...firearms.map(([definitionId, apiName, variant]) => ({
    definitionId, apiName, category: "firearm", variant,
  })),
  ...ammunition.map(([definitionId, apiName]) => ({
    definitionId, apiName, category: "ammunition",
  })),
  ...armor.map(([definitionId, apiName]) => ({
    definitionId, apiName, category: "armor",
  })),
  ...backpacks.map(([definitionId, apiName]) => ({
    definitionId, apiName, category: "backpack",
  })),
  ...medicalTools.map(([definitionId, apiName]) => ({
    definitionId, apiName, category: "medical_tool",
  })),
  ...lootMission.map(([definitionId, apiName]) => ({
    definitionId, apiName, category: "loot_mission",
  })),
  ...interactions.map(([definitionId, apiName]) => ({
    definitionId, apiName, category: "interaction",
  })),
].map((entry, index) => ({ ...entry, index }));

const FRAME_COUNT = entries.length;
const WIDTH = FRAME * FRAME_COUNT;
const HEIGHT = FRAME;

if (FRAME_COUNT !== 72) {
  throw new Error(`atlas contract drift: expected 72 frames, got ${FRAME_COUNT}`);
}

const colors = {
  outline: [7, 11, 13, 255],
  deep: [16, 24, 27, 255],
  shadow: [28, 39, 42, 255],
  steel: [61, 78, 80, 255],
  steelHi: [121, 143, 139, 255],
  white: [220, 226, 216, 255],
  olive: [73, 83, 56, 255],
  oliveHi: [128, 137, 82, 255],
  tan: [143, 113, 72, 255],
  brass: [196, 137, 55, 255],
  brassHi: [244, 191, 77, 255],
  orange: [199, 83, 35, 255],
  orangeHi: [240, 132, 52, 255],
  red: [163, 42, 39, 255],
  redHi: [224, 72, 57, 255],
  cyan: [42, 190, 190, 255],
  cyanHi: [105, 239, 223, 255],
  amber: [238, 157, 47, 255],
  glowCyan: [42, 190, 190, 112],
  glowAmber: [238, 157, 47, 112],
};
const accents = [
  colors.cyan, colors.brass, colors.orangeHi, colors.redHi,
  colors.oliveHi, colors.steelHi, colors.amber, colors.cyanHi,
];

const pixels = Buffer.alloc(WIDTH * HEIGHT * 4);

function put(frame, x, y, color) {
  if (frame < 0 || frame >= FRAME_COUNT
      || x < 0 || x >= FRAME || y < 0 || y >= FRAME) return;
  const offset = ((y * WIDTH) + frame * FRAME + x) * 4;
  pixels.set(color, offset);
}

function rect(frame, x, y, width, height, color) {
  for (let yy = y; yy < y + height; yy += 1) {
    for (let xx = x; xx < x + width; xx += 1) put(frame, xx, yy, color);
  }
}

function borderRect(frame, x, y, width, height, border, fill) {
  rect(frame, x, y, width, height, border);
  rect(frame, x + 1, y + 1, width - 2, height - 2, fill);
}

function line(frame, x0, y0, x1, y1, color) {
  let dx = Math.abs(x1 - x0);
  const sx = x0 < x1 ? 1 : -1;
  let dy = -Math.abs(y1 - y0);
  const sy = y0 < y1 ? 1 : -1;
  let error = dx + dy;
  while (true) {
    put(frame, x0, y0, color);
    if (x0 === x1 && y0 === y1) break;
    const twice = error * 2;
    if (twice >= dy) { error += dy; x0 += sx; }
    if (twice <= dx) { error += dx; y0 += sy; }
  }
}

function ring(frame, cx, cy, radius, color) {
  for (let offset = -radius + 1; offset < radius; offset += 1) {
    put(frame, cx + offset, cy - radius, color);
    put(frame, cx + offset, cy + radius, color);
    put(frame, cx - radius, cy + offset, color);
    put(frame, cx + radius, cy + offset, color);
  }
  put(frame, cx - radius + 1, cy - radius + 1, color);
  put(frame, cx + radius - 1, cy - radius + 1, color);
  put(frame, cx - radius + 1, cy + radius - 1, color);
  put(frame, cx + radius - 1, cy + radius - 1, color);
}

function accent(frame) {
  return accents[frame % accents.length];
}

function drawFirearm(frame, variant, familyIndex) {
  const a = accent(frame);
  const long = variant === "rifle" || variant === "marksman"
    || variant === "shotgun";
  const x0 = long ? 1 : 3;
  const x1 = variant === "pistol" ? 12 : 15;
  const y0 = 11 + (familyIndex % 2);
  const y1 = variant === "marksman" ? 4 : 6;
  line(frame, x0, y0, x1, y1, colors.outline);
  line(frame, x0 + 1, y0, x1, y1 + 1, colors.steelHi);
  rect(frame, 6, 7, variant === "pistol" ? 5 : 7, 4, colors.outline);
  rect(frame, 7, 8, variant === "pistol" ? 4 : 5, 2, colors.deep);
  rect(frame, 8 + (familyIndex % 3), 8, 2, 1, a);
  if (variant === "marksman") {
    rect(frame, 7, 4, 6, 2, colors.outline);
    rect(frame, 8, 4, 4, 1, a);
  } else if (variant === "shotgun") {
    line(frame, 9, 10, 15, 7, colors.tan);
    rect(frame, 10, 7, 3, 3, a);
  } else if (variant === "smg") {
    rect(frame, 8, 10, 2, 5, colors.outline);
    put(frame, 9, 12, a);
  } else if (variant === "pistol") {
    line(frame, 8, 10, 7, 14, colors.outline);
    line(frame, 9, 10, 8, 14, colors.olive);
  } else {
    rect(frame, 8, 10, 3, 4, colors.outline);
    put(frame, 9, 11, a);
  }
  if (familyIndex % 3 === 0) {
    rect(frame, 2, 10, 4, 3, colors.olive);
    put(frame, 3, 10, a);
  } else if (familyIndex % 3 === 1) {
    line(frame, 5, 10, 2, 14, colors.outline);
    line(frame, 6, 10, 3, 14, colors.tan);
  } else {
    rect(frame, 5, 11, 3, 2, colors.deep);
  }
}

function drawAmmunition(frame, variant) {
  const a = accent(frame);
  const width = 10 + (variant % 2) * 2;
  borderRect(frame, 8 - width / 2, 6, width, 8, colors.outline,
    variant % 3 === 0 ? colors.olive : colors.steel);
  rect(frame, 9 - width / 2, 7, width - 2, 2, a);
  const rounds = 2 + (variant % 4);
  for (let index = 0; index < rounds; index += 1) {
    const x = 3 + index * 3;
    rect(frame, x, 1 + (index % 2), 2, 5, colors.brass);
    put(frame, x, index % 2, variant === 4 ? colors.outline : colors.brassHi);
  }
  rect(frame, 6, 10, 4, 2, colors.deep);
}

function drawArmor(frame, variant) {
  const a = accent(frame);
  line(frame, 5, 3, 2, 7, colors.outline);
  line(frame, 10, 3, 13, 7, colors.outline);
  borderRect(frame, 3, 4, 10, 11, colors.outline,
    variant === 2 ? colors.steel : colors.olive);
  rect(frame, 5, 5, 6, 3, a);
  borderRect(frame, 5, 9, 6, 4, colors.deep, colors.steel);
  if (variant > 0) {
    rect(frame, 2, 8, 2, 5, colors.outline);
    rect(frame, 12, 8, 2, 5, colors.outline);
  }
}

function drawBackpack(frame, variant) {
  const a = accent(frame);
  rect(frame, 5, 2, 6, 3, colors.outline);
  borderRect(frame, 3 - variant, 4, 10 + variant * 2, 11,
    colors.outline, colors.olive);
  rect(frame, 4 - variant, 5, 8 + variant * 2, 2, a);
  borderRect(frame, 5, 9, 6, 4, colors.deep, colors.steel);
  line(frame, 3 - variant, 6, 1, 13, colors.outline);
  line(frame, 12 + variant, 6, 14, 13, colors.outline);
}

function drawMedicalTool(frame, variant) {
  const a = accent(frame);
  switch (variant) {
    case 0:
      ring(frame, 8, 8, 5, colors.outline);
      ring(frame, 8, 8, 4, colors.white);
      rect(frame, 6, 7, 4, 3, colors.redHi);
      break;
    case 1:
      borderRect(frame, 5, 3, 6, 11, colors.outline, colors.white);
      rect(frame, 6, 1, 4, 3, colors.outline);
      rect(frame, 6, 7, 4, 3, a);
      break;
    case 2:
      borderRect(frame, 2, 4, 12, 10, colors.outline, colors.red);
      rect(frame, 7, 6, 2, 6, colors.white);
      rect(frame, 5, 8, 6, 2, colors.white);
      break;
    case 3:
      ring(frame, 8, 8, 5, colors.outline);
      line(frame, 4, 12, 12, 4, a);
      line(frame, 5, 12, 13, 4, colors.steelHi);
      break;
    case 4:
      borderRect(frame, 5, 2, 7, 13, colors.outline, colors.steel);
      rect(frame, 6, 4, 5, 3, a);
      rect(frame, 7, 0, 3, 3, colors.outline);
      break;
    case 5:
      for (const x of [3, 10]) {
        rect(frame, x, 1, 3, 14, colors.outline);
        rect(frame, x + 1, 2, 1, 12, colors.tan);
      }
      for (const y of [4, 8, 12]) rect(frame, 5, y, 6, 2, a);
      break;
    case 6:
      line(frame, 3, 13, 13, 3, colors.outline);
      line(frame, 4, 13, 14, 3, colors.steelHi);
      rect(frame, 6, 8, 5, 4, a);
      line(frame, 13, 3, 15, 1, colors.white);
      break;
    default:
      borderRect(frame, 2, 5, 12, 9, colors.outline, colors.orange);
      rect(frame, 5, 3, 6, 3, colors.outline);
      line(frame, 4, 12, 12, 6, colors.brassHi);
      line(frame, 4, 6, 12, 12, colors.steelHi);
      break;
  }
}

function drawLoot(frame, variant, definitionId) {
  const a = accent(frame);
  switch (variant % 10) {
    case 0:
      borderRect(frame, 4, 2, 8, 13, colors.outline, colors.steel);
      rect(frame, 5, 4, 6, 3, a);
      rect(frame, 6, 0, 4, 3, colors.outline);
      break;
    case 1:
      ring(frame, 8, 8, 6, colors.outline);
      ring(frame, 8, 8, 4, a);
      rect(frame, 7, 4, 2, 8, colors.deep);
      break;
    case 2:
      borderRect(frame, 2, 5, 12, 9, colors.outline, colors.olive);
      rect(frame, 3, 6, 10, 2, a);
      for (const x of [4, 7, 10]) rect(frame, x, 9, 2, 4, colors.steelHi);
      break;
    case 3:
      line(frame, 2, 13, 13, 2, colors.outline);
      line(frame, 3, 13, 14, 2, a);
      line(frame, 2, 8, 8, 14, colors.steel);
      break;
    case 4:
      borderRect(frame, 3, 3, 10, 11, colors.outline, colors.deep);
      rect(frame, 4, 4, 8, 3, colors.steel);
      rect(frame, 5, 5, 5, 1, a);
      for (const x of [5, 7, 9, 11]) put(frame, x, 13, colors.brass);
      break;
    case 5:
      ring(frame, 8, 8, 5, colors.steel);
      ring(frame, 8, 8, 3, colors.outline);
      put(frame, 8, 8, a);
      rect(frame, 7, 1, 3, 4, colors.outline);
      break;
    case 6:
      borderRect(frame, 2, 3, 12, 11, colors.outline, colors.tan);
      line(frame, 3, 12, 12, 4, a);
      line(frame, 3, 5, 12, 12, colors.deep);
      break;
    case 7:
      rect(frame, 3, 3, 10, 12, colors.outline);
      rect(frame, 4, 4, 8, 10, colors.olive);
      rect(frame, 5, 5, 6, 2, a);
      rect(frame, 6, 1, 4, 3, colors.outline);
      break;
    case 8:
      ring(frame, 8, 8, 6, a);
      ring(frame, 8, 8, 4, colors.outline);
      line(frame, 5, 11, 11, 5, colors.white);
      put(frame, 8, 8, colors.deep);
      break;
    default:
      borderRect(frame, 3, 2, 10, 13, colors.outline, colors.olive);
      rect(frame, 4, 3, 2, 11, a);
      rect(frame, 7, 5, 4, 1, colors.white);
      line(frame, 7, 11, 11, 7, colors.cyanHi);
      break;
  }
  const signature = hash8(definitionId);
  for (let bit = 0; bit < 4; bit += 1) {
    if ((signature & (1 << bit)) !== 0) put(frame, 11 + bit, 14, a);
  }
}

function drawInteraction(frame, variant) {
  if (variant === 0) {
    ring(frame, 8, 8, 6, colors.glowCyan);
    ring(frame, 8, 8, 5, colors.cyan);
    rect(frame, 7, 4, 3, 7, colors.deep);
    put(frame, 8, 3, colors.white);
  } else if (variant === 1) {
    ring(frame, 8, 8, 6, colors.glowAmber);
    rect(frame, 2, 4, 3, 10, colors.outline);
    rect(frame, 11, 4, 3, 10, colors.outline);
    line(frame, 4, 6, 12, 10, colors.orangeHi);
  } else {
    rect(frame, 1, 6, 14, 5, colors.outline);
    borderRect(frame, 4, 3, 8, 11, colors.outline, colors.steel);
    ring(frame, 8, 8, 3, colors.cyan);
    put(frame, 8, 8, colors.cyanHi);
  }
}

function hash8(value) {
  let hash = 0;
  for (let index = 0; index < value.length; index += 1) {
    hash = ((hash * 33) ^ value.charCodeAt(index)) & 0xff;
  }
  return hash;
}

function tagFrame(frame) {
  const a = accent(frame);
  for (let bit = 0; bit < 7; bit += 1) {
    put(frame, 1 + bit, 15, (frame & (1 << bit)) !== 0 ? a : colors.outline);
  }
}

let categoryIndex = {
  firearm: 0,
  ammunition: 0,
  armor: 0,
  backpack: 0,
  medical_tool: 0,
  loot_mission: 0,
  interaction: 0,
};
for (const entry of entries) {
  const variant = categoryIndex[entry.category]++;
  switch (entry.category) {
    case "firearm":
      drawFirearm(entry.index, entry.variant, variant);
      break;
    case "ammunition":
      drawAmmunition(entry.index, variant);
      break;
    case "armor":
      drawArmor(entry.index, variant);
      break;
    case "backpack":
      drawBackpack(entry.index, variant);
      break;
    case "medical_tool":
      drawMedicalTool(entry.index, variant);
      break;
    case "loot_mission":
      drawLoot(entry.index, variant, entry.definitionId);
      break;
    case "interaction":
      drawInteraction(entry.index, variant);
      break;
    default:
      throw new Error(`unknown category: ${entry.category}`);
  }
  tagFrame(entry.index);
}

const rawFrameHashes = new Set();
for (let frame = 0; frame < FRAME_COUNT; frame += 1) {
  const bytes = Buffer.alloc(FRAME * FRAME * 4);
  for (let y = 0; y < FRAME; y += 1) {
    const sourceStart = (y * WIDTH + frame * FRAME) * 4;
    pixels.copy(bytes, y * FRAME * 4, sourceStart, sourceStart + FRAME * 4);
  }
  const hash = createHash("sha256").update(bytes).digest("hex");
  if (rawFrameHashes.has(hash)) {
    throw new Error(`duplicate raw frame: ${frame}`);
  }
  rawFrameHashes.add(hash);
}

mkdirSync(dirname(output), { recursive: true });
mkdirSync(dirname(manifestOutput), { recursive: true });
const temp = mkdtempSync(join(tmpdir(), "bukov-item-visuals-"));
const raw = join(temp, "items_interactions.rgba");

try {
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
    output,
  ], { stdio: "inherit" });
  if (encoded.status !== 0) process.exit(encoded.status ?? 1);

  const png = readFileSync(output);
  const alpha = [];
  for (let offset = 3; offset < pixels.length; offset += 4) {
    alpha.push(pixels[offset]);
  }
  const rgbaColors = new Set();
  for (let offset = 0; offset < pixels.length; offset += 4) {
    rgbaColors.add(pixels.subarray(offset, offset + 4).toString("hex"));
  }
  const sha256 = createHash("sha256").update(png).digest("hex");
  const manifest = {
    schemaVersion: 1,
    atlas: "sprites/bukov/items_interactions.png",
    width: WIDTH,
    height: HEIGHT,
    frameWidth: FRAME,
    frameHeight: FRAME,
    frameCount: FRAME_COUNT,
    colorType: "RGBA",
    uniqueRgbaColors: rgbaColors.size,
    opaquePixels: alpha.filter((value) => value === 255).length,
    translucentPixels: alpha.filter(
      (value) => value > 0 && value < 255,
    ).length,
    transparentPixels: alpha.filter((value) => value === 0).length,
    sha256,
    entries: entries.map(({
      definitionId, apiName, category, index,
    }) => ({ definitionId, apiName, category, index })),
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
