package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

/**
 * Allocation-free geometry for the mobile off-screen navigation cue.
 *
 * <p>The cue is visual only and never captures input. Its compact 20x18
 * footprint stays inside the platform safe area while its position and arrow
 * redundantly encode all eight directions.</p>
 */
public final class BukovMobileNavigationLayout {

	public static final float WIDTH = 20f;
	public static final float HEIGHT = 18f;
	private static final float EDGE_MARGIN = 4f;

	public static final class Rect {
		public float x;
		public float y;
		public float width;
		public float height;

		public Rect() {
		}

		private Rect set(float x, float y, float width, float height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			return this;
		}

		public float right() {
			return x + width;
		}

		public float bottom() {
			return y + height;
		}
	}

	public static Rect calculate(
			float viewportWidth,
			float viewportHeight,
			float safeLeft,
			float safeTop,
			float safeRight,
			float safeBottom,
			BukovRaidHudState.Direction direction,
			Rect result) {
		if (viewportWidth <= 0f || viewportHeight <= 0f
				|| direction == null
				|| result == null) {
			throw new IllegalArgumentException(
					"viewport, direction and result are required");
		}
		float left = clamp(
				safeLeft + EDGE_MARGIN,
				0f,
				Math.max(0f, viewportWidth - WIDTH));
		float top = clamp(
				safeTop + EDGE_MARGIN,
				0f,
				Math.max(0f, viewportHeight - HEIGHT));
		float right = clamp(
				viewportWidth - safeRight - EDGE_MARGIN - WIDTH,
				left,
				Math.max(left, viewportWidth - WIDTH));
		float bottom = clamp(
				viewportHeight - safeBottom - EDGE_MARGIN - HEIGHT,
				top,
				Math.max(top, viewportHeight - HEIGHT));
		float centerX = (left + right) * 0.5f;
		float centerY = (top + bottom) * 0.5f;
		switch (direction) {
			case N:
				return result.set(centerX, top, WIDTH, HEIGHT);
			case NE:
				return result.set(right, top, WIDTH, HEIGHT);
			case E:
				return result.set(right, centerY, WIDTH, HEIGHT);
			case SE:
				return result.set(right, bottom, WIDTH, HEIGHT);
			case S:
				return result.set(centerX, bottom, WIDTH, HEIGHT);
			case SW:
				return result.set(left, bottom, WIDTH, HEIGHT);
			case W:
				return result.set(left, centerY, WIDTH, HEIGHT);
			case NW:
			default:
				return result.set(left, top, WIDTH, HEIGHT);
		}
	}

	/**
	 * Tests the exact world target, not merely its direction band. The delta is
	 * expressed in dungeon tiles and the remaining values in world pixels.
	 */
	public static boolean targetInsideWorldViewport(
			float heroWorldX,
			float heroWorldY,
			float targetDeltaXTiles,
			float targetDeltaYTiles,
			float tileSize,
			float scrollX,
			float scrollY,
			float viewportWorldWidth,
			float viewportWorldHeight,
			float edgeMargin) {
		if (!finite(heroWorldX)
				|| !finite(heroWorldY)
				|| !finite(targetDeltaXTiles)
				|| !finite(targetDeltaYTiles)
				|| !finite(tileSize)
				|| !finite(scrollX)
				|| !finite(scrollY)
				|| !finite(viewportWorldWidth)
				|| !finite(viewportWorldHeight)
				|| !finite(edgeMargin)
				|| tileSize <= 0f
				|| viewportWorldWidth <= 0f
				|| viewportWorldHeight <= 0f
				|| edgeMargin < 0f) {
			return false;
		}
		float targetX = heroWorldX + targetDeltaXTiles * tileSize;
		float targetY = heroWorldY + targetDeltaYTiles * tileSize;
		return targetX >= scrollX + edgeMargin
				&& targetX <= scrollX + viewportWorldWidth - edgeMargin
				&& targetY >= scrollY + edgeMargin
				&& targetY <= scrollY + viewportWorldHeight - edgeMargin;
	}

	private static float clamp(
			float value, float minimum, float maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}

	private static boolean finite(float value) {
		return value == value
				&& value > -Float.MAX_VALUE
				&& value < Float.MAX_VALUE;
	}

	private BukovMobileNavigationLayout() {
		throw new AssertionError();
	}
}
