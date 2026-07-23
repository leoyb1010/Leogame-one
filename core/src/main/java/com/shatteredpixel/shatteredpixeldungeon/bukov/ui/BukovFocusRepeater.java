package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;

/**
 * Converts a held left stick into focus steps with the 300ms repeat delay from
 * the UI contract. Direction changes step immediately; releasing resets it.
 */
public final class BukovFocusRepeater {

	public static final float THRESHOLD = 0.55f;
	public static final float INITIAL_REPEAT_DELAY = 0.30f;
	public static final float REPEAT_INTERVAL = 0.12f;

	private int heldDirection;
	private float repeatRemaining;

	public int update(float x, float y, float elapsed) {
		if (!BukovNumbers.isFinite(x)
				|| !BukovNumbers.isFinite(y)
				|| !BukovNumbers.isFinite(elapsed)
				|| elapsed < 0f) {
			throw new IllegalArgumentException("invalid focus input");
		}
		int direction = direction(x, y);
		if (direction == 0) {
			heldDirection = 0;
			repeatRemaining = 0f;
			return 0;
		}
		if (direction != heldDirection) {
			heldDirection = direction;
			repeatRemaining = INITIAL_REPEAT_DELAY;
			return direction;
		}
		repeatRemaining -= elapsed;
		if (repeatRemaining <= 0.000001f) {
			repeatRemaining += REPEAT_INTERVAL;
			return direction;
		}
		return 0;
	}

	private static int direction(float x, float y) {
		float absoluteX = Math.abs(x);
		float absoluteY = Math.abs(y);
		if (Math.max(absoluteX, absoluteY) < THRESHOLD) {
			return 0;
		}
		float dominant = absoluteY >= absoluteX ? y : x;
		return dominant < 0f ? -1 : 1;
	}
}
