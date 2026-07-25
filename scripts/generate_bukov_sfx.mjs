#!/usr/bin/env node

/**
 * Deterministic, original Bukov tactical SFX generator.
 *
 * The output is synthesized from oscillators and seeded noise. It does not
 * sample, transform, or embed any third-party recording.
 */

import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const SAMPLE_RATE = 48_000;
const PEAK = 0.9;
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const outputDir = path.join(root, "core/src/main/assets/sounds/bukov");

function seededRandom(seed) {
  let state = seed >>> 0;
  return () => {
    state += 0x6d2b79f5;
    let value = state;
    value = Math.imul(value ^ (value >>> 15), value | 1);
    value ^= value + Math.imul(value ^ (value >>> 7), value | 61);
    return ((value ^ (value >>> 14)) >>> 0) / 0x100000000;
  };
}

function buffer(seconds) {
  return new Float64Array(Math.ceil(seconds * SAMPLE_RATE));
}

function clamp01(value) {
  return Math.max(0, Math.min(1, value));
}

function smoothEnvelope(t, attack, release, duration) {
  const attackGain = attack <= 0 ? 1 : clamp01(t / attack);
  const releaseGain = release <= 0
    ? 1
    : clamp01((duration - t) / release);
  return attackGain * releaseGain;
}

function addChirp(out, start, duration, fromHz, toHz, gain, decay = 1.8) {
  const first = Math.max(0, Math.floor(start * SAMPLE_RATE));
  const count = Math.min(
    out.length - first,
    Math.ceil(duration * SAMPLE_RATE),
  );
  let phase = 0;
  for (let i = 0; i < count; i++) {
    const t = i / SAMPLE_RATE;
    const position = t / duration;
    const frequency = fromHz + (toHz - fromHz) * position;
    phase += (Math.PI * 2 * frequency) / SAMPLE_RATE;
    const envelope = Math.exp(-decay * position)
      * smoothEnvelope(t, 0.0015, Math.min(0.025, duration), duration);
    out[first + i] += Math.sin(phase) * gain * envelope;
  }
}

function addNoise(
  out,
  start,
  duration,
  gain,
  seed,
  lowPass = 1,
  highPass = 0,
  decay = 3,
) {
  const first = Math.max(0, Math.floor(start * SAMPLE_RATE));
  const count = Math.min(
    out.length - first,
    Math.ceil(duration * SAMPLE_RATE),
  );
  const random = seededRandom(seed);
  let low = 0;
  let previousLow = 0;
  for (let i = 0; i < count; i++) {
    const t = i / SAMPLE_RATE;
    const position = t / duration;
    const white = random() * 2 - 1;
    low += lowPass * (white - low);
    const high = low - previousLow;
    previousLow = low;
    const filtered = low * (1 - highPass) + high * highPass * 5;
    const envelope = Math.exp(-decay * position)
      * smoothEnvelope(t, 0.0005, Math.min(0.02, duration), duration);
    out[first + i] += filtered * gain * envelope;
  }
}

function addClick(out, at, gain, color = 0.65) {
  const first = Math.floor(at * SAMPLE_RATE);
  const random = seededRandom(Math.floor(at * 1e6) ^ 0xb00c0f);
  let filtered = 0;
  const count = Math.min(out.length - first, Math.floor(0.012 * SAMPLE_RATE));
  for (let i = 0; i < count; i++) {
    const white = random() * 2 - 1;
    filtered += color * (white - filtered);
    const envelope = Math.exp(-i / (SAMPLE_RATE * 0.0023));
    out[first + i] += filtered * gain * envelope;
  }
}

function addServo(out, start, duration, fromHz, toHz, gain) {
  const first = Math.floor(start * SAMPLE_RATE);
  const count = Math.min(out.length - first, Math.ceil(duration * SAMPLE_RATE));
  let phase = 0;
  for (let i = 0; i < count; i++) {
    const t = i / SAMPLE_RATE;
    const position = t / duration;
    const frequency = fromHz + (toHz - fromHz) * position
      + Math.sin(position * Math.PI * 8) * 14;
    phase += (Math.PI * 2 * frequency) / SAMPLE_RATE;
    const envelope = Math.sin(Math.PI * position);
    const motor = Math.sin(phase) + Math.sin(phase * 2.01) * 0.24;
    out[first + i] += motor * gain * envelope;
  }
}

function addLoopTone(out, frequency, gain, phaseOffset = 0) {
  for (let i = 0; i < out.length; i++) {
    const phase = (Math.PI * 2 * frequency * i) / SAMPLE_RATE + phaseOffset;
    out[i] += Math.sin(phase) * gain;
  }
}

function addLoopNoise(out, gain, seed, smoothing) {
  const random = seededRandom(seed);
  let filtered = 0;
  for (let i = 0; i < out.length; i++) {
    const white = random() * 2 - 1;
    filtered += smoothing * (white - filtered);
    out[i] += filtered * gain;
  }
}

function finish(out) {
  // Remove DC, apply a transparent soft limiter, then peak-normalize.
  let mean = 0;
  for (const sample of out) mean += sample;
  mean /= Math.max(1, out.length);
  let peak = 0;
  for (let i = 0; i < out.length; i++) {
    out[i] = Math.tanh((out[i] - mean) * 1.18) / Math.tanh(1.18);
    peak = Math.max(peak, Math.abs(out[i]));
  }
  const scale = peak > 0 ? PEAK / peak : 1;
  const fade = Math.min(Math.floor(0.012 * SAMPLE_RATE), out.length);
  for (let i = 0; i < out.length; i++) {
    let edge = 1;
    if (i < 24) edge *= i / 24;
    if (i >= out.length - fade) edge *= (out.length - 1 - i) / fade;
    out[i] *= scale * Math.max(0, edge);
  }
  return out;
}

function wavPcm16(samples) {
  const dataBytes = samples.length * 2;
  const wav = Buffer.alloc(44 + dataBytes);
  wav.write("RIFF", 0);
  wav.writeUInt32LE(36 + dataBytes, 4);
  wav.write("WAVE", 8);
  wav.write("fmt ", 12);
  wav.writeUInt32LE(16, 16);
  wav.writeUInt16LE(1, 20);
  wav.writeUInt16LE(1, 22);
  wav.writeUInt32LE(SAMPLE_RATE, 24);
  wav.writeUInt32LE(SAMPLE_RATE * 2, 28);
  wav.writeUInt16LE(2, 32);
  wav.writeUInt16LE(16, 34);
  wav.write("data", 36);
  wav.writeUInt32LE(dataBytes, 40);
  for (let i = 0; i < samples.length; i++) {
    const sample = Math.max(-1, Math.min(1, samples[i]));
    wav.writeInt16LE(Math.round(sample * 32767), 44 + i * 2);
  }
  return wav;
}

const gunshotFamilies = [
  {
    name: "pistol",
    seed: 0x1200,
    mechanicalHz: 1560,
    bodyHz: 176,
    bodySeconds: 0.15,
    bodyGain: 0.72,
  },
  {
    name: "smg",
    seed: 0x2200,
    mechanicalHz: 1880,
    bodyHz: 214,
    bodySeconds: 0.12,
    bodyGain: 0.62,
  },
  {
    name: "carbine",
    seed: 0x3200,
    mechanicalHz: 1320,
    bodyHz: 144,
    bodySeconds: 0.19,
    bodyGain: 0.82,
  },
  {
    name: "rifle",
    seed: 0x4200,
    mechanicalHz: 1060,
    bodyHz: 108,
    bodySeconds: 0.25,
    bodyGain: 0.96,
  },
  {
    name: "shotgun",
    seed: 0x5200,
    mechanicalHz: 820,
    bodyHz: 82,
    bodySeconds: 0.31,
    bodyGain: 1.12,
  },
  {
    name: "heavy",
    seed: 0x6200,
    mechanicalHz: 640,
    bodyHz: 64,
    bodySeconds: 0.38,
    bodyGain: 1.24,
  },
];

function synthesizeGunshotMechanical(family, variant) {
  const duration = 0.082 + variant * 0.006;
  const out = buffer(duration);
  const color = 0.62 + variant * 0.07;
  const frequencyScale = 0.96 + variant * 0.04;
  addClick(out, 0.002, 0.8 - variant * 0.04, color);
  addChirp(
    out,
    0.003,
    0.038 + variant * 0.003,
    family.mechanicalHz * frequencyScale,
    family.mechanicalHz * 0.34 * frequencyScale,
    0.38,
    6.4,
  );
  addNoise(
    out,
    0.008,
    0.052,
    0.21,
    family.seed + 0x10 + variant,
    0.28,
    0.62,
    7.8,
  );
  addClick(out, 0.044 + variant * 0.003, 0.24, 0.5 + variant * 0.08);
  return finish(out);
}

function synthesizeGunshotBody(family, variant) {
  const duration = family.bodySeconds + variant * 0.012;
  const out = buffer(duration);
  const frequencyScale = 0.965 + variant * 0.035;
  addNoise(
    out,
    0,
    Math.min(0.07, duration * 0.32),
    1.15 + family.bodyGain * 0.2,
    family.seed + 0x30 + variant,
    0.82 - family.bodyGain * 0.1,
    0.32 + variant * 0.07,
    8.8 - variant * 0.4,
  );
  addChirp(
    out,
    0,
    duration * 0.58,
    family.bodyHz * frequencyScale,
    family.bodyHz * 0.38 * frequencyScale,
    family.bodyGain,
    4.1,
  );
  addNoise(
    out,
    duration * 0.06,
    duration * 0.88,
    0.2 + family.bodyGain * 0.17,
    family.seed + 0x50 + variant,
    0.11,
    0.06,
    5.2,
  );
  return finish(out);
}

function synthesizeGunshotTail(space, variant) {
  const configurations = {
    indoor: {
      duration: 0.31,
      baseHz: 128,
      gain: 0.48,
      reflections: [0.055, 0.105, 0.176],
      seed: 0x7100,
    },
    corridor: {
      duration: 0.48,
      baseHz: 102,
      gain: 0.42,
      reflections: [0.085, 0.18, 0.295],
      seed: 0x7200,
    },
    open: {
      duration: 0.67,
      baseHz: 76,
      gain: 0.34,
      reflections: [0.14, 0.31, 0.49],
      seed: 0x7300,
    },
  };
  const config = configurations[space];
  const duration = config.duration + variant * 0.018;
  const out = buffer(duration);
  const frequencyScale = 0.97 + variant * 0.03;
  addNoise(
    out,
    0,
    duration * 0.96,
    config.gain,
    config.seed + variant,
    space === "indoor" ? 0.16 : 0.08,
    0.04,
    space === "open" ? 3.1 : 4.5,
  );
  addChirp(
    out,
    0.006,
    duration * 0.72,
    config.baseHz * frequencyScale,
    config.baseHz * 0.42 * frequencyScale,
    config.gain * 0.64,
    space === "open" ? 3.2 : 4.8,
  );
  config.reflections.forEach((at, reflection) => {
    addNoise(
      out,
      at + variant * 0.004,
      Math.min(0.085, duration - at),
      config.gain * (0.32 - reflection * 0.055),
      config.seed + 0x20 + variant * 5 + reflection,
      0.22,
      0.12,
      6.2,
    );
  });
  return finish(out);
}

function synthesizeFootstep(surface, variant) {
  const configurations = {
    hard: {
      duration: 0.19,
      seed: 0xd100,
      lowPass: 0.34,
      highPass: 0.18,
      bodyFromHz: 142,
      bodyToHz: 68,
      bodyGain: 0.46,
      contactGain: 0.63,
    },
    water: {
      duration: 0.27,
      seed: 0xd200,
      lowPass: 0.12,
      highPass: 0.03,
      bodyFromHz: 118,
      bodyToHz: 48,
      bodyGain: 0.35,
      contactGain: 0.52,
    },
    metal: {
      duration: 0.23,
      seed: 0xd300,
      lowPass: 0.26,
      highPass: 0.42,
      bodyFromHz: 520,
      bodyToHz: 164,
      bodyGain: 0.38,
      contactGain: 0.58,
    },
  };
  const config = configurations[surface];
  const duration = config.duration + variant * 0.018;
  const out = buffer(duration);
  const variantScale = 0.96 + variant * 0.07;

  addNoise(
    out,
    0,
    duration * (surface === "water" ? 0.9 : 0.48),
    config.contactGain,
    config.seed + variant,
    config.lowPass,
    config.highPass,
    surface === "water" ? 4.1 : 7.2,
  );
  addChirp(
    out,
    0.003,
    duration * 0.62,
    config.bodyFromHz * variantScale,
    config.bodyToHz * variantScale,
    config.bodyGain,
    surface === "metal" ? 5.2 : 4.2,
  );

  if (surface === "hard") {
    addClick(out, 0.008, 0.38 + variant * 0.04, 0.56);
    addNoise(
      out,
      0.032,
      duration * 0.52,
      0.21,
      config.seed + 0x20 + variant,
      0.08,
      0.07,
      5.8,
    );
  } else if (surface === "water") {
    addNoise(
      out,
      0.024,
      duration * 0.78,
      0.34,
      config.seed + 0x20 + variant,
      0.045,
      0.02,
      2.7,
    );
    addChirp(
      out,
      0.018,
      duration * 0.66,
      680 * variantScale,
      210 * variantScale,
      0.1,
      3.6,
    );
  } else {
    addClick(out, 0.006, 0.48 + variant * 0.04, 0.82);
    addChirp(
      out,
      0.03,
      duration * 0.72,
      1240 * variantScale,
      390 * variantScale,
      0.23,
      4.6,
    );
  }
  return finish(out);
}

const sounds = {
  gunshot_player() {
    const out = buffer(0.34);
    addNoise(out, 0, 0.05, 1.18, 0x101, 0.82, 0.42, 8);
    addChirp(out, 0, 0.12, 112, 46, 0.9, 4.2);
    addNoise(out, 0.018, 0.29, 0.48, 0x102, 0.13, 0.08, 4.8);
    addChirp(out, 0.004, 0.055, 780, 260, 0.28, 4.5);
    addClick(out, 0.074, 0.22);
    return finish(out);
  },

  gunshot_enemy() {
    const out = buffer(0.29);
    addNoise(out, 0, 0.042, 1.05, 0x201, 0.86, 0.52, 8.5);
    addChirp(out, 0, 0.1, 92, 51, 0.68, 4.4);
    addNoise(out, 0.016, 0.24, 0.38, 0x202, 0.17, 0.12, 5.2);
    addChirp(out, 0.003, 0.044, 960, 340, 0.22, 5);
    return finish(out);
  },

  // Six authored weapon-body families. They intentionally differ in
  // transient length, low-frequency body, mechanical brightness and tail;
  // per-weapon pitch/gain still provides variation inside each family.
  gunshot_pistol() {
    const out = buffer(0.24);
    addNoise(out, 0, 0.032, 1.1, 0x211, 0.91, 0.62, 10.5);
    addChirp(out, 0, 0.085, 178, 72, 0.62, 5.4);
    addChirp(out, 0.002, 0.048, 1480, 430, 0.34, 6.2);
    addNoise(out, 0.014, 0.19, 0.23, 0x212, 0.2, 0.2, 6.4);
    addClick(out, 0.052, 0.24, 0.74);
    return finish(out);
  },

  gunshot_smg() {
    const out = buffer(0.19);
    addNoise(out, 0, 0.026, 0.98, 0x221, 0.94, 0.7, 12);
    addChirp(out, 0, 0.064, 216, 91, 0.48, 6.2);
    addChirp(out, 0.001, 0.034, 1860, 610, 0.38, 7.1);
    addNoise(out, 0.011, 0.14, 0.18, 0x222, 0.24, 0.28, 7.4);
    addClick(out, 0.038, 0.3, 0.8);
    return finish(out);
  },

  gunshot_carbine() {
    const out = buffer(0.30);
    addNoise(out, 0, 0.041, 1.14, 0x231, 0.88, 0.52, 9.4);
    addChirp(out, 0, 0.105, 142, 57, 0.75, 4.8);
    addChirp(out, 0.002, 0.05, 1260, 360, 0.31, 5.6);
    addNoise(out, 0.018, 0.25, 0.32, 0x232, 0.15, 0.14, 5.2);
    addClick(out, 0.061, 0.18, 0.68);
    return finish(out);
  },

  gunshot_rifle() {
    const out = buffer(0.39);
    addNoise(out, 0, 0.052, 1.22, 0x241, 0.84, 0.43, 8.4);
    addChirp(out, 0, 0.145, 108, 41, 0.94, 4.0);
    addChirp(out, 0.003, 0.062, 970, 245, 0.3, 4.8);
    addNoise(out, 0.021, 0.34, 0.43, 0x242, 0.11, 0.09, 4.5);
    addClick(out, 0.083, 0.2, 0.63);
    return finish(out);
  },

  gunshot_shotgun() {
    const out = buffer(0.49);
    addNoise(out, 0, 0.075, 1.38, 0x251, 0.74, 0.28, 6.9);
    addChirp(out, 0, 0.19, 82, 30, 1.08, 3.5);
    addNoise(out, 0.016, 0.43, 0.58, 0x252, 0.08, 0.05, 3.8);
    addChirp(out, 0.004, 0.075, 610, 138, 0.29, 4.2);
    addClick(out, 0.118, 0.22, 0.54);
    return finish(out);
  },

  gunshot_heavy() {
    const out = buffer(0.58);
    addNoise(out, 0, 0.09, 1.48, 0x261, 0.68, 0.22, 6.1);
    addChirp(out, 0, 0.23, 64, 24, 1.22, 3.0);
    addNoise(out, 0.018, 0.53, 0.67, 0x262, 0.065, 0.035, 3.25);
    addChirp(out, 0.002, 0.095, 470, 104, 0.33, 3.8);
    addClick(out, 0.145, 0.26, 0.48);
    return finish(out);
  },

  bullet_hit() {
		const out = buffer(0.19);
    addClick(out, 0.002, 1.05, 0.82);
    addNoise(out, 0.005, 0.13, 0.72, 0x301, 0.3, 0.56, 6);
    addChirp(out, 0.004, 0.12, 2280, 610, 0.34, 4.2);
    addChirp(out, 0.012, 0.14, 510, 170, 0.24, 4.8);
		return finish(out);
	},

  contact_hit() {
    const out = buffer(0.18);
    addNoise(out, 0, 0.075, 0.78, 0x351, 0.23, 0.12, 6.5);
    addChirp(out, 0, 0.13, 155, 62, 0.7, 4.5);
    addNoise(out, 0.025, 0.12, 0.24, 0x352, 0.09, 0.06, 4.8);
    return finish(out);
  },

  dry_fire() {
    const out = buffer(0.095);
    addClick(out, 0.006, 0.82, 0.78);
    addChirp(out, 0.007, 0.052, 880, 420, 0.32, 5);
    addClick(out, 0.042, 0.26, 0.55);
    return finish(out);
  },

  reload_start() {
    const out = buffer(0.24);
    addClick(out, 0.012, 0.72, 0.66);
    addChirp(out, 0.015, 0.085, 310, 145, 0.25, 3.2);
    addNoise(out, 0.052, 0.11, 0.22, 0x401, 0.08, 0.15, 2.4);
    addClick(out, 0.151, 0.48, 0.72);
    return finish(out);
  },

  reload_finish() {
    const out = buffer(0.25);
    addClick(out, 0.01, 0.72, 0.62);
    addChirp(out, 0.012, 0.075, 190, 325, 0.28, 3.2);
    addClick(out, 0.105, 0.8, 0.76);
    addChirp(out, 0.106, 0.085, 620, 255, 0.28, 4.5);
    addClick(out, 0.178, 0.34, 0.55);
    return finish(out);
  },

  reload_mag_out() {
    const out = buffer(0.23);
    addClick(out, 0.01, 0.74, 0.7);
    addChirp(out, 0.012, 0.065, 410, 175, 0.22, 4.4);
    addNoise(out, 0.045, 0.12, 0.19, 0x411, 0.09, 0.19, 3.5);
    addClick(out, 0.145, 0.52, 0.62);
    return finish(out);
  },

  reload_mag_in() {
    const out = buffer(0.28);
    addNoise(out, 0, 0.1, 0.2, 0x421, 0.07, 0.12, 3.2);
    addChirp(out, 0.025, 0.09, 172, 286, 0.25, 3.8);
    addClick(out, 0.104, 0.88, 0.76);
    addChirp(out, 0.11, 0.09, 690, 270, 0.18, 5.2);
    addClick(out, 0.205, 0.28, 0.55);
    return finish(out);
  },

  reload_charge() {
    const out = buffer(0.33);
    addClick(out, 0.008, 0.55, 0.72);
    addNoise(out, 0.015, 0.18, 0.24, 0x431, 0.11, 0.24, 2.7);
    addChirp(out, 0.02, 0.17, 520, 205, 0.27, 2.8);
    addClick(out, 0.205, 0.92, 0.82);
    addChirp(out, 0.208, 0.08, 880, 310, 0.23, 5.5);
    return finish(out);
  },

  loot_pickup() {
    const out = buffer(0.22);
    addNoise(out, 0, 0.16, 0.22, 0x501, 0.07, 0.03, 2.2);
    addClick(out, 0.028, 0.26, 0.58);
    addChirp(out, 0.06, 0.12, 820, 1240, 0.2, 2.3);
    addClick(out, 0.145, 0.22, 0.65);
    return finish(out);
  },

  search_complete() {
    const out = buffer(0.28);
    addNoise(out, 0, 0.15, 0.3, 0x601, 0.08, 0.02, 2.5);
    addClick(out, 0.035, 0.36, 0.63);
    addClick(out, 0.124, 0.28, 0.7);
    addChirp(out, 0.145, 0.105, 680, 920, 0.18, 2.8);
    return finish(out);
  },

  gate_unlock() {
    const out = buffer(0.54);
    addServo(out, 0, 0.31, 118, 246, 0.25);
    addNoise(out, 0.04, 0.28, 0.11, 0x701, 0.09, 0.18, 1.5);
    addClick(out, 0.31, 0.78, 0.72);
    addChirp(out, 0.34, 0.135, 920, 920, 0.2, 2.1);
    addChirp(out, 0.405, 0.11, 1260, 1260, 0.18, 2.5);
    return finish(out);
  },

  extraction_start() {
    const out = buffer(0.55);
    addNoise(out, 0, 0.42, 0.1, 0x801, 0.04, 0.38, 1.8);
    addChirp(out, 0.035, 0.13, 880, 880, 0.32, 2.4);
    addChirp(out, 0.21, 0.13, 1180, 1180, 0.32, 2.4);
    addClick(out, 0.39, 0.36, 0.72);
    addNoise(out, 0.405, 0.12, 0.14, 0x802, 0.08, 0.32, 3);
    return finish(out);
  },

  extraction_complete() {
    const out = buffer(0.68);
    addNoise(out, 0, 0.5, 0.07, 0x901, 0.05, 0.4, 1.5);
    addChirp(out, 0.025, 0.18, 660, 660, 0.27, 2);
    addChirp(out, 0.185, 0.18, 880, 880, 0.28, 2);
    addChirp(out, 0.345, 0.22, 1100, 1100, 0.3, 2.3);
    addClick(out, 0.545, 0.33, 0.66);
    return finish(out);
  },

  kill_confirm() {
    const out = buffer(0.076);
    // The authored fundamental stays inside the 200-300 Hz Gate 5 band.
    addChirp(out, 0.001, 0.069, 268, 224, 0.72, 2.8);
    addChirp(out, 0.006, 0.054, 242, 218, 0.24, 3.4);
    return finish(out);
  },

  boss_phase_break() {
    const out = buffer(0.34);
    addNoise(out, 0, 0.11, 0.42, 0x911, 0.24, 0.35, 5.8);
    addChirp(out, 0.008, 0.19, 132, 56, 0.58, 2.2);
    addClick(out, 0.096, 0.72, 0.52);
    addChirp(out, 0.105, 0.19, 540, 1180, 0.24, 2.5);
    addClick(out, 0.268, 0.28, 0.72);
    return finish(out);
  },

  boss_slam() {
    const out = buffer(0.29);
    addNoise(out, 0, 0.16, 0.66, 0x921, 0.1, 0.08, 4.5);
    addChirp(out, 0.002, 0.24, 108, 42, 0.82, 2.7);
    addClick(out, 0.012, 0.34, 0.38);
    addNoise(out, 0.09, 0.17, 0.18, 0x922, 0.035, 0.16, 2.9);
    return finish(out);
  },

  boss_overload() {
    const out = buffer(0.39);
    addNoise(out, 0, 0.28, 0.54, 0x931, 0.36, 0.58, 3.7);
    addChirp(out, 0.002, 0.22, 1640, 138, 0.48, 2.6);
    addChirp(out, 0.075, 0.25, 94, 38, 0.64, 2.2);
    addClick(out, 0.185, 0.48, 0.74);
    addNoise(out, 0.19, 0.18, 0.22, 0x932, 0.06, 0.32, 2.8);
    return finish(out);
  },

  ui_focus() {
    const out = buffer(0.028);
    addClick(out, 0.001, 0.34, 0.72);
    addChirp(out, 0.002, 0.023, 1260, 1480, 0.18, 2.4);
    return finish(out);
  },

  ui_confirm() {
    const out = buffer(0.078);
    addClick(out, 0.001, 0.3, 0.64);
    addChirp(out, 0.004, 0.036, 720, 960, 0.25, 2.2);
    addChirp(out, 0.033, 0.039, 1040, 1320, 0.2, 2.4);
    return finish(out);
  },

  ui_cancel() {
    const out = buffer(0.058);
    addClick(out, 0.002, 0.28, 0.58);
    addChirp(out, 0.004, 0.049, 760, 430, 0.25, 2.7);
    return finish(out);
  },

  ui_error() {
    const out = buffer(0.115);
    addClick(out, 0.002, 0.34, 0.5);
    addChirp(out, 0.004, 0.102, 210, 126, 0.35, 2.2);
    addNoise(out, 0.012, 0.08, 0.12, 0xa01, 0.08, 0.2, 2.5);
    return finish(out);
  },

  ambience_calm() {
    const out = buffer(4);
    addLoopNoise(out, 0.28, 0xb01, 0.008);
    addLoopTone(out, 50, 0.06);
    addLoopTone(out, 100, 0.025, Math.PI / 3);
    addChirp(out, 1.15, 0.34, 1320, 1140, 0.035, 1.5);
    addChirp(out, 3.05, 0.28, 940, 1120, 0.03, 1.6);
    return finish(out);
  },

  ambience_tense() {
    const out = buffer(4);
    addLoopNoise(out, 0.24, 0xb02, 0.012);
    addLoopTone(out, 45, 0.085);
    addLoopTone(out, 90, 0.04, Math.PI / 2);
    for (const at of [0.2, 1.2, 2.2, 3.2]) {
      addChirp(out, at, 0.34, 92, 54, 0.12, 2.3);
      addClick(out, at + 0.18, 0.045, 0.42);
    }
    return finish(out);
  },

  ambience_combat() {
    const out = buffer(4);
    addLoopNoise(out, 0.2, 0xb03, 0.018);
    addLoopTone(out, 55, 0.09);
    addLoopTone(out, 110, 0.035, Math.PI / 4);
    for (let beat = 0; beat < 8; beat++) {
      const at = beat * 0.5;
      addNoise(out, at, 0.11, 0.22, 0xc00 + beat, 0.07, 0.08, 5.5);
      addChirp(out, at, 0.16, beat % 4 === 0 ? 98 : 76, 45, 0.18, 3.4);
    }
    return finish(out);
  },
};

for (const family of gunshotFamilies) {
  for (let variant = 0; variant < 3; variant++) {
    const suffix = variant + 1;
    sounds[`gunshot_${family.name}_mechanical_${suffix}`] =
      () => synthesizeGunshotMechanical(family, variant);
    sounds[`gunshot_${family.name}_body_${suffix}`] =
      () => synthesizeGunshotBody(family, variant);
  }
}
for (const space of ["indoor", "corridor", "open"]) {
  for (let variant = 0; variant < 3; variant++) {
    sounds[`gunshot_tail_${space}_${variant + 1}`] =
      () => synthesizeGunshotTail(space, variant);
  }
}
for (const surface of ["hard", "water", "metal"]) {
  for (let variant = 0; variant < 2; variant++) {
    sounds[`footstep_${surface}_${variant + 1}`] =
      () => synthesizeFootstep(surface, variant);
  }
}

fs.mkdirSync(outputDir, { recursive: true });
for (const [name, synthesize] of Object.entries(sounds)) {
  const bytes = wavPcm16(synthesize());
  const output = path.join(outputDir, `${name}.wav`);
  fs.writeFileSync(output, bytes);
  const hash = crypto.createHash("sha256").update(bytes).digest("hex");
  process.stdout.write(`${path.relative(root, output)},${hash}\n`);
}
