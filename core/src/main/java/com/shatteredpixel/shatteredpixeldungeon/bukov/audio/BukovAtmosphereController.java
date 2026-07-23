package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

/**
 * Three-state, allocation-free atmosphere crossfade.
 */
public final class BukovAtmosphereController {

	public enum State {
		CALM,
		TENSE,
		COMBAT
	}

	public static final float CROSSFADE_SECONDS = 1.5f;
	public static final float COMBAT_RELEASE_SECONDS = 8f;

	private State target = State.CALM;
	private float calmGain = 1f;
	private float tenseGain;
	private float combatGain;
	private float combatRelease;

	public void update(
			float deltaSeconds,
			BukovAtmosphereSignal signal) {
		if (signal == null) {
			throw new IllegalArgumentException("signal is required");
		}
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
						deltaSeconds)
				|| deltaSeconds < 0f) {
			throw new IllegalArgumentException("invalid deltaSeconds");
		}
		if (signal.combat()) {
			combatRelease = COMBAT_RELEASE_SECONDS;
		} else {
			combatRelease = Math.max(0f, combatRelease - deltaSeconds);
		}
		target = combatRelease > 0f
				? State.COMBAT
				: signal.tense() ? State.TENSE : State.CALM;

		float step = Math.min(1f, deltaSeconds / CROSSFADE_SECONDS);
		calmGain = approach(
				calmGain, target == State.CALM ? 1f : 0f, step);
		tenseGain = approach(
				tenseGain, target == State.TENSE ? 1f : 0f, step);
		combatGain = approach(
				combatGain, target == State.COMBAT ? 1f : 0f, step);
		float total = calmGain + tenseGain + combatGain;
		if (total > 0f) {
			calmGain /= total;
			tenseGain /= total;
			combatGain /= total;
		}
	}

	public State target() {
		return target;
	}

	public float gain(State state) {
		switch (state) {
			case CALM:
				return calmGain;
			case TENSE:
				return tenseGain;
			case COMBAT:
				return combatGain;
			default:
				throw new IllegalStateException("unknown state: " + state);
		}
	}

	public float combatBlend() {
		return combatGain;
	}

	private static float approach(
			float value,
			float target,
			float amount) {
		if (value < target) return Math.min(target, value + amount);
		if (value > target) return Math.max(target, value - amount);
		return value;
	}
}
