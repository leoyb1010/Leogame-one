package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;

/**
 * Allocation-free presentation state for the segmented reload ring.
 *
 * <p>The simulation remains authoritative. This model only preserves the last
 * interrupted progress long enough to play the 120 ms reverse specified by the
 * HUD contract. A completed reload is detected by the magazine count increase
 * and disappears immediately.</p>
 */
final class BukovReloadRingModel {

	static final int SEGMENT_COUNT = 8;

	private boolean previousReloading;
	private int previousMagazine;
	private float previousProgress;
	private float displayedProgress;
	private float reverseStartProgress;
	private float reverseRemainingSeconds;

	void update(
			float elapsedSeconds,
			boolean reloading,
			float progress,
			int magazine,
			boolean reduceMotion,
			float reverseDurationSeconds) {
		float safeElapsed = finiteNonNegative(elapsedSeconds);
		float safeProgress = clamp01(progress);
		float safeReverseDuration =
				Math.max(0.001f, finiteNonNegative(reverseDurationSeconds));

		if (reloading) {
			displayedProgress = safeProgress;
			reverseRemainingSeconds = 0f;
		} else if (previousReloading) {
			boolean completed = magazine > previousMagazine;
			if (!completed && !reduceMotion && previousProgress > 0f) {
				reverseStartProgress = previousProgress;
				reverseRemainingSeconds = Math.max(
						0f, safeReverseDuration - safeElapsed);
				displayedProgress = reverseStartProgress
						* reverseRemainingSeconds / safeReverseDuration;
			} else {
				clearReverse();
			}
		} else if (reverseRemainingSeconds > 0f) {
			reverseRemainingSeconds = Math.max(
					0f, reverseRemainingSeconds - safeElapsed);
			displayedProgress = reverseStartProgress
					* reverseRemainingSeconds / safeReverseDuration;
			if (reverseRemainingSeconds <= 0f) {
				clearReverse();
			}
		} else {
			displayedProgress = 0f;
		}

		previousReloading = reloading;
		previousMagazine = Math.max(0, magazine);
		previousProgress = safeProgress;
	}

	boolean visible(boolean reloading) {
		return reloading || reverseRemainingSeconds > 0f;
	}

	float displayedProgress() {
		return displayedProgress;
	}

	int filledSegmentCount() {
		if (displayedProgress <= 0f) return 0;
		return Math.min(
				SEGMENT_COUNT,
				(int)Math.ceil(displayedProgress * SEGMENT_COUNT));
	}

	private void clearReverse() {
		displayedProgress = 0f;
		reverseStartProgress = 0f;
		reverseRemainingSeconds = 0f;
	}

	private static float finiteNonNegative(float value) {
		return BukovNumbers.isFinite(value) ? Math.max(0f, value) : 0f;
	}

	private static float clamp01(float value) {
		return Math.min(1f, finiteNonNegative(value));
	}
}
