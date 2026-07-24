package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;

/**
 * Distance-driven player footstep cadence for the 120 Hz realtime loop.
 *
 * <p>Cadence follows distance actually accepted by collision rather than input
 * or render frames. The minimum interval is a guard against a teleport or
 * unusually large fixed step producing an audio burst.</p>
 */
public final class FootstepCadence {

	public static final float FIRST_STEP_DISTANCE_TILES = 0.36f;
	public static final float STRIDE_DISTANCE_TILES = 1.02f;
	public static final float MINIMUM_INTERVAL_SECONDS = 0.18f;

	private float distanceUntilStep = FIRST_STEP_DISTANCE_TILES;
	private float intervalRemaining;

	public boolean advance(
			float acceptedDeltaX,
			float acceptedDeltaY,
			float deltaSeconds) {
		requireFinite(acceptedDeltaX, "acceptedDeltaX");
		requireFinite(acceptedDeltaY, "acceptedDeltaY");
		if (!BukovNumbers.isFinite(deltaSeconds) || deltaSeconds < 0f) {
			throw new IllegalArgumentException(
					"deltaSeconds must be finite and non-negative");
		}
		intervalRemaining = Math.max(
				0f,
				intervalRemaining - deltaSeconds);
		float distance = (float)Math.sqrt(
				acceptedDeltaX * acceptedDeltaX
						+ acceptedDeltaY * acceptedDeltaY);
		if (distance <= 0.00001f) {
			return false;
		}
		distanceUntilStep -= distance;
		if (distanceUntilStep > 0f || intervalRemaining > 0f) {
			return false;
		}
		do {
			distanceUntilStep += STRIDE_DISTANCE_TILES;
		} while (distanceUntilStep <= 0f);
		intervalRemaining = MINIMUM_INTERVAL_SECONDS;
		return true;
	}

	private static void requireFinite(float value, String label) {
		if (!BukovNumbers.isFinite(value)) {
			throw new IllegalArgumentException(label + " must be finite");
		}
	}
}
