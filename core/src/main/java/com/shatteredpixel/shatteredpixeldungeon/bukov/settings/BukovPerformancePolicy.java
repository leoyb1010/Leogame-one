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
		int safeSequence = Math.max(0, sequence);
		switch (profile) {
			case HIGH_QUALITY:
				return true;
			case BALANCED:
				return type != CombatFxEvent.Type.IMPACT
						|| safeSequence % 2 == 0;
			case HIGH_FRAME_RATE:
				if (type == CombatFxEvent.Type.MUZZLE_FLASH) {
					return safeSequence % 2 == 0;
				}
				if (type == CombatFxEvent.Type.IMPACT) {
					return safeSequence % 4 == 0;
				}
				return safeSequence % 2 == 0;
			default:
				throw new IllegalArgumentException(
						"unknown Bukov performance profile: " + profile);
		}
	}

	private BukovPerformancePolicy() {
	}
}
