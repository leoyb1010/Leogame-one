package com.shatteredpixel.shatteredpixeldungeon.bukov.settings;

import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFxEvent;

/**
 * Presentation-only load shedding. It never changes fixed-step simulation,
 * combat events, hit resolution, AI, persistence, or input sampling.
 */
public final class BukovPerformancePolicy {

	public static final int HIGH_QUALITY = 0;
	public static final int BALANCED = 1;
	public static final int HIGH_FRAME_RATE = 2;

	public static boolean renderCombatFx(
			int profile,
			CombatFxEvent.Type type,
			int sequence) {
		if (type == null) {
			return false;
		}
		if (profile < HIGH_QUALITY || profile > HIGH_FRAME_RATE) {
			throw new IllegalArgumentException(
					"unknown Bukov performance profile: " + profile);
		}
		// Muzzle, tracer and endpoint impact form one small feedback packet for
		// a hitscan round. Sampling any member made valid shots appear broken
		// in the default high-frame-rate profile. These effects are pooled and
		// made from a handful of ColorBlocks, so every profile keeps the packet
		// intact; heavier atmosphere remains the correct load-shedding target.
		switch (profile) {
			case HIGH_QUALITY:
			case BALANCED:
			case HIGH_FRAME_RATE:
				return true;
			default:
				throw new AssertionError("validated performance profile");
		}
	}

	private BukovPerformancePolicy() {
	}
}
