package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

/**
 * Deterministic three-layer gunshot mix. Variation is derived from an
 * audio-only sequence, never gameplay RNG.
 */
public final class GunshotAudioResolver {

	private static final float MAX_PAN = 0.82f;

	public static void resolve(
			boolean localPlayerSound,
			int sequence,
			float deltaX,
			float deltaY,
			SpatialAudioModel.Result spatial,
			GunshotAudioPlan out) {
		if (spatial == null || out == null) {
			throw new IllegalArgumentException("spatial and out are required");
		}
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(deltaX)
				|| !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
						deltaY)) {
			throw new IllegalArgumentException("sound delta must be finite");
		}

		float base = spatial.outputGain();
		float distance = (float)Math.sqrt(deltaX * deltaX + deltaY * deltaY);
		float pan = localPlayerSound || distance <= 0.0001f
				? 0f
				: clamp(deltaX / distance, -1f, 1f) * MAX_PAN;
		float leftScale = pan > 0f ? 1f - pan : 1f;
		float rightScale = pan < 0f ? 1f + pan : 1f;
		float pitch = variationPitch(sequence);
		// Sample has no per-instance low-pass filter. Shape the authored
		// layers with the same cutoff so walls and long distance still remove
		// high-frequency mechanical detail in actual playback.
		float highFrequencyScale = clamp(
				spatial.lowPassHz() / 20_000f,
				0.1f,
				1f);
		float bodyFilterScale = 0.72f + highFrequencyScale * 0.28f;

		out.set(
				base * 0.18f * highFrequencyScale * leftScale,
				base * 0.18f * highFrequencyScale * rightScale,
				clamp(pitch * 1.08f, 0.5f, 2f),
				base * 0.82f * bodyFilterScale * leftScale,
				base * 0.82f * bodyFilterScale * rightScale,
				pitch,
				base * 0.24f * leftScale,
				base * 0.24f * rightScale,
				clamp(pitch * 0.88f, 0.5f, 2f),
				spatial.lowPassHz(),
				spatial.audible());
	}

	public static float variationPitch(int sequence) {
		switch (com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.floorMod(
				sequence, 3)) {
			case 0:
				return 0.96f;
			case 1:
				return 1f;
			default:
				return 1.04f;
		}
	}

	private static float clamp(float value, float minimum, float maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}

	private GunshotAudioResolver() {
	}
}
