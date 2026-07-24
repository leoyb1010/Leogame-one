package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

/**
 * Shared logical-pixel contract for the title, hideout, deployment boundary
 * and raid HUD. PixelScene already converts physical Retina pixels to logical
 * camera units, so these values must never be multiplied by device density.
 */
public final class BukovVisualContract {

	public static final float OUTER_MARGIN = 8f;
	public static final float GAP = 4f;
	public static final float CARD_PADDING = 6f;
	public static final float MAX_CONTENT_WIDTH = 420f;
	public static final String FONT_CAPTION = "hud";
	public static final String FONT_BODY = "body";
	public static final String FONT_SECTION = "section";
	public static final String FONT_TITLE = "title";
	public static final String FONT_DISPLAY = "display";

	public static float controlHeight(boolean touch) {
		return touch ? 22f : 18f;
	}

	public static float controlHeight(boolean touch, int scaleLevel) {
		return BukovUiScale.controlHeight(
				controlHeight(touch),
				touch,
				scaleLevel);
	}

	public static float compactControlHeight(boolean touch) {
		return touch ? 22f : 16f;
	}

	public static float contentWidth(float usableWidth, boolean wide) {
		float bounded = Math.max(0f, usableWidth - OUTER_MARGIN * 2f);
		return wide ? Math.min(MAX_CONTENT_WIDTH, bounded) : bounded;
	}

	public static float contentWidth(
			float usableWidth,
			boolean wide,
			int scaleLevel) {
		float margin = BukovUiScale.value(OUTER_MARGIN, scaleLevel);
		float bounded = Math.max(0f, usableWidth - margin * 2f);
		float maximum = BukovUiScale.value(
				MAX_CONTENT_WIDTH,
				scaleLevel);
		return wide ? Math.min(maximum, bounded) : bounded;
	}

	public static float centeredLeft(
			float usableLeft,
			float usableWidth,
			float contentWidth) {
		return usableLeft + Math.max(
				0f, (usableWidth - contentWidth) * 0.5f);
	}

	public static float panelWidth(float usableWidth, boolean wide) {
		float maximum = wide ? 132f : 150f;
		return Math.max(
				96f,
				Math.min(maximum, usableWidth - OUTER_MARGIN * 2f));
	}

	public static float safeTop(float insetTop) {
		return Math.max(0f, insetTop) + OUTER_MARGIN;
	}

	public static float safeBottom(
			float screenHeight, float insetBottom) {
		return screenHeight
				- Math.max(0f, insetBottom)
				- OUTER_MARGIN;
	}

	private BukovVisualContract() {
	}
}
