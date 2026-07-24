package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;

/**
 * Allocation-free realtime sprint meter.
 *
 * <p>Weight affects both the useful speed bonus and stamina drain. The meter
 * drains only after movement actually succeeds, so holding sprint against a
 * wall does not waste stamina.</p>
 */
public final class BukovSprintState {

	static final float RECOVERY_DELAY_SECONDS = 0.8f;
	static final float RECOVERY_PER_SECOND = 0.22f;
	static final float BASE_DRAIN_PER_SECOND = 0.18f;
	static final float LOAD_DRAIN_PER_SECOND = 0.14f;
	static final float LIGHT_SPEED_MULTIPLIER = 1.55f;
	static final float HEAVY_SPEED_MULTIPLIER = 1.28f;

	private float stamina = 1f;
	private float recoveryDelay;
	private boolean sprinting;

	public float speedMultiplier(
			boolean sprintHeld,
			boolean movementIntent,
			float loadFraction) {
		requireFraction(loadFraction);
		if (!sprintHeld || !movementIntent || stamina <= 0.0001f) {
			return 1f;
		}
		return LIGHT_SPEED_MULTIPLIER
				+ (HEAVY_SPEED_MULTIPLIER - LIGHT_SPEED_MULTIPLIER)
						* loadFraction;
	}

	public void fixedStep(
			float deltaSeconds,
			boolean sprintHeld,
			boolean moved,
			float loadFraction) {
		if (!BukovNumbers.isFinite(deltaSeconds) || deltaSeconds < 0f) {
			throw new IllegalArgumentException(
					"deltaSeconds must be finite and non-negative");
		}
		requireFraction(loadFraction);
		sprinting = sprintHeld && moved && stamina > 0.0001f;
		if (sprinting) {
			float drain = BASE_DRAIN_PER_SECOND
					+ LOAD_DRAIN_PER_SECOND * loadFraction;
			stamina = Math.max(0f, stamina - drain * deltaSeconds);
			recoveryDelay = RECOVERY_DELAY_SECONDS;
			if (stamina <= 0.0001f) {
				stamina = 0f;
				sprinting = false;
			}
			return;
		}
		float recoverySeconds =
				Math.max(0f, deltaSeconds - recoveryDelay);
		recoveryDelay = Math.max(0f, recoveryDelay - deltaSeconds);
		if (recoverySeconds > 0f) {
			stamina = Math.min(
					1f,
					stamina + RECOVERY_PER_SECOND * recoverySeconds);
		}
	}

	public float staminaFraction() {
		return stamina;
	}

	public boolean sprinting() {
		return sprinting;
	}

	private static void requireFraction(float value) {
		if (!BukovNumbers.isFinite(value) || value < 0f || value > 1f) {
			throw new IllegalArgumentException(
					"loadFraction must be finite and in [0, 1]");
		}
	}
}
