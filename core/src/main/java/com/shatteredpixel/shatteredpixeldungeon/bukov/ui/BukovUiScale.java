package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

/**
 * Pure 100/125/150 percent UI scaling policy.
 *
 * <p>PixelScene already works in logical pixels, so this helper never applies
 * display density. Safe-area fitting remains the caller's responsibility.</p>
 */
public final class BukovUiScale {

	public static final int LEVEL_COUNT = 3;
	public static final float MINIMUM_TOUCH_TARGET = 18f;

	public static int clampLevel(int level) {
		return Math.max(0, Math.min(LEVEL_COUNT - 1, level));
	}

	public static int percent(int level) {
		return 100 + clampLevel(level) * 25;
	}

	public static float multiplier(int level) {
		return percent(level) / 100f;
	}

	public static float value(float authoredValue, int level) {
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
						.isFinite(authoredValue)
				|| authoredValue < 0f) {
			throw new IllegalArgumentException(
					"authoredValue must be finite and non-negative");
		}
		return authoredValue * multiplier(level);
	}

	public static int pixels(int authoredPixels, int level) {
		if (authoredPixels < 0) {
			throw new IllegalArgumentException(
					"authoredPixels must be non-negative");
		}
		return Math.round(authoredPixels * multiplier(level));
	}

	public static int fontPixels(int authoredPixels, int level) {
		return Math.max(1, pixels(authoredPixels, level));
	}

	public static float controlHeight(
			float authoredHeight,
			boolean touch,
			int level) {
		float scaled = value(authoredHeight, level);
		return touch ? Math.max(MINIMUM_TOUCH_TARGET, scaled) : scaled;
	}

	private BukovUiScale() {
	}
}
