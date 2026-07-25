package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.SoundCategory;

/** Pure rules shared by the eight-direction sound ring and its unit tests. */
final class BukovSoundRingModel {

	static final int SEGMENT_COUNT = 8;

	static int segmentIndex(BukovRaidHudState.Direction direction) {
		return direction == null ? -1 : direction.ordinal();
	}

	static boolean longArc(SoundCategory category) {
		return category == SoundCategory.PLAYER_GUNSHOT
				|| category == SoundCategory.ENEMY_GUNSHOT
				|| category == SoundCategory.BOSS_CUE
				|| category == SoundCategory.EXTRACTION_CUE;
	}

	static float alpha(
			BukovRaidHudState state, float lifetimeSeconds) {
		if (state == null
				|| !state.soundVisible()
				|| !finitePositive(lifetimeSeconds)) {
			return 0f;
		}
		float distanceAlpha;
		switch (state.soundDistance()) {
			case FAR:
				distanceAlpha = 0.45f;
				break;
			case MID:
				distanceAlpha = 0.72f;
				break;
			case NEAR:
			default:
				distanceAlpha = 1f;
				break;
		}
		float lifetimeAlpha = clamp01(
				state.soundRemainingSeconds() / lifetimeSeconds);
		float strengthAlpha = 0.35f + 0.65f
				* clamp01(state.soundStrength());
		return clamp01(
				state.combatAwarenessAlpha()
						* distanceAlpha
						* lifetimeAlpha
						* strengthAlpha);
	}

	private static float clamp01(float value) {
		if (Float.isNaN(value) || Float.isInfinite(value)) return 0f;
		return Math.max(0f, Math.min(1f, value));
	}

	private static boolean finitePositive(float value) {
		return !Float.isNaN(value)
				&& !Float.isInfinite(value)
				&& value > 0f;
	}

	private BukovSoundRingModel() {
		throw new AssertionError();
	}
}
