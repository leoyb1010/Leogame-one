package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;

/**
 * Presentation-only clock for the delay between player death and settlement.
 *
 * <p>This model has no simulation or persistence dependency. The host advances
 * it with render delta and owns every side effect after consuming completion.</p>
 */
public final class BukovDeathTransitionModel {

	public static final int DURATION_MILLISECONDS = 1000;
	private static final float DURATION_SECONDS =
			DURATION_MILLISECONDS / 1000f;
	private static final float MAX_VEIL_ALPHA = 0.50f;
	private static final float REDUCED_FLASHES_MAX_VEIL_ALPHA = 0.25f;

	private float elapsedSeconds;
	private boolean completionConsumed;

	public void advance(float deltaSeconds) {
		if (!BukovNumbers.isFinite(deltaSeconds)
				|| deltaSeconds < 0f) {
			throw new IllegalArgumentException(
					"deltaSeconds must be finite and non-negative");
		}
		float boundedDelta = Math.min(
				DURATION_SECONDS,
				deltaSeconds);
		elapsedSeconds = Math.min(
				DURATION_SECONDS,
				elapsedSeconds + boundedDelta);
	}

	public float progress() {
		return elapsedSeconds / DURATION_SECONDS;
	}

	public boolean complete() {
		return elapsedSeconds >= DURATION_SECONDS;
	}

	/**
	 * Returns true exactly once after the transition reaches completion.
	 */
	public boolean consumeCompletion() {
		if (!complete() || completionConsumed) {
			return false;
		}
		completionConsumed = true;
		return true;
	}

	/**
	 * Reduced motion removes interpolation, not the one-second settlement
	 * delay. Reduced flashes caps the neutral veil at half strength.
	 */
	public float veilAlpha(
			boolean reduceMotion,
			boolean reduceFlashes) {
		float maximum = reduceFlashes
				? REDUCED_FLASHES_MAX_VEIL_ALPHA
				: MAX_VEIL_ALPHA;
		return maximum * (reduceMotion ? 1f : progress());
	}
}
