package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;

/**
 * Shared final-render accessibility policy for short bright combat flashes.
 *
 * Simulation intensity remains unchanged; only the rendered alpha is scaled.
 */
public final class BukovAccessibilityPresentation {

	public static float flashScale(boolean reduceFlashes) {
		return reduceFlashes ? 0.5f : 1f;
	}

	public static float flashAlpha(
			float authoredAlpha, boolean reduceFlashes) {
		if (!BukovNumbers.isFinite(authoredAlpha)) {
			throw new IllegalArgumentException(
					"authoredAlpha must be finite");
		}
		return Math.max(
				0f,
				Math.min(1f, authoredAlpha))
						* flashScale(reduceFlashes);
	}

	private BukovAccessibilityPresentation() {
	}
}
