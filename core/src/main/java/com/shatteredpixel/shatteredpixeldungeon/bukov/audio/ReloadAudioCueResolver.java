package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

/**
 * Allocation-free crossing detector for the three reload cues. A mask is
 * returned so a long frame cannot silently skip more than one phase.
 */
public final class ReloadAudioCueResolver {

	public static int crossed(
			FirearmAudioProfile profile,
			float previousElapsed,
			float currentElapsed,
			float reloadSeconds) {
		if (profile == null) {
			throw new IllegalArgumentException("profile is required");
		}
		if (!finite(previousElapsed)
				|| !finite(currentElapsed)
				|| !finite(reloadSeconds)
				|| previousElapsed < 0f
				|| currentElapsed < previousElapsed
				|| reloadSeconds <= 0f) {
			throw new IllegalArgumentException(
					"reload timing must be finite, positive and monotonic");
		}
		int result = 0;
		for (ReloadAudioCue cue : ReloadAudioCue.values()) {
			float cueTime = profile.fraction(cue) * reloadSeconds;
			if (previousElapsed < cueTime && currentElapsed >= cueTime) {
				result |= cue.mask;
			}
		}
		return result;
	}

	public static boolean contains(int mask, ReloadAudioCue cue) {
		return cue != null && (mask & cue.mask) != 0;
	}

	private static boolean finite(float value) {
		return com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
				.isFinite(value);
	}

	private ReloadAudioCueResolver() {
	}
}
