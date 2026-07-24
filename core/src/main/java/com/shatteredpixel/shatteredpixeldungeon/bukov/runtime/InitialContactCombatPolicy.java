package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

/** Fair but active opening-fire profile for the first visible raid contact. */
final class InitialContactCombatPolicy {

	static final float OPENING_WARNING_SECONDS = 2.25f;
	static final float AIM_SECONDS = 0.75f;
	static final int MAXIMUM_DAMAGE = 3;

	static boolean applies(boolean onboardingContact, boolean ranged) {
		return onboardingContact && ranged;
	}

	static float aimSeconds(float baseline, boolean applies) {
		if (baseline < 0f) {
			throw new IllegalArgumentException(
					"baseline aim must not be negative");
		}
		return applies ? Math.max(baseline, AIM_SECONDS) : baseline;
	}

	static int minimumDamage(int baseline, boolean applies) {
		if (baseline < 0) {
			throw new IllegalArgumentException(
					"baseline damage must not be negative");
		}
		return applies ? Math.min(baseline, MAXIMUM_DAMAGE) : baseline;
	}

	static int maximumDamage(
			int baselineMinimum,
			int baselineMaximum,
			boolean applies) {
		if (baselineMinimum < 0 || baselineMaximum < baselineMinimum) {
			throw new IllegalArgumentException(
					"invalid baseline damage range");
		}
		if (!applies) return baselineMaximum;
		return Math.max(
				minimumDamage(baselineMinimum, true),
				Math.min(baselineMaximum, MAXIMUM_DAMAGE));
	}

	static float openingWarningSeconds(boolean applies) {
		return applies ? OPENING_WARNING_SECONDS : 0f;
	}

	private InitialContactCombatPolicy() {
	}
}
