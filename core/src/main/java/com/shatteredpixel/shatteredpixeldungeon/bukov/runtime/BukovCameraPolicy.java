package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

/**
 * Chooses the integer world-camera zoom for a realtime raid.
 *
 * <p>The UI camera keeps the platform-selected scale. The world camera is
 * independent so a wide desktop window cannot turn a compact raid into a
 * static whole-map overview, while portrait phones retain useful awareness.</p>
 */
public final class BukovCameraPolicy {

	private static final float LANDSCAPE_VISIBLE_TILES = 24f;
	private static final float MOBILE_LANDSCAPE_VISIBLE_TILES = 20f;
	private static final float PORTRAIT_VISIBLE_TILES = 14f;

	private BukovCameraPolicy() {
	}

	public static float resolveWorldZoom(
			float screenWidth,
			float tileSize,
			boolean landscape,
			boolean desktop,
			float minimumZoom,
			float maximumZoom) {
		if (!finite(screenWidth)
				|| !finite(tileSize)
				|| !finite(minimumZoom)
				|| !finite(maximumZoom)
				|| screenWidth <= 0f
				|| tileSize <= 0f
				|| minimumZoom <= 0f
				|| maximumZoom < minimumZoom) {
			throw new IllegalArgumentException(
					"screen, tile and zoom bounds must be finite and positive");
		}

		int minimumIntegerZoom = Math.max(1, (int)Math.ceil(minimumZoom));
		int maximumIntegerZoom = Math.max(
				minimumIntegerZoom,
				(int)Math.floor(maximumZoom));
		float visibleTiles = landscape
				? desktop
						? LANDSCAPE_VISIBLE_TILES
						: MOBILE_LANDSCAPE_VISIBLE_TILES
				: PORTRAIT_VISIBLE_TILES;
		int desiredZoom = Math.max(
				1,
				Math.round(screenWidth / (tileSize * visibleTiles)));
		return Math.max(
				minimumIntegerZoom,
				Math.min(maximumIntegerZoom, desiredZoom));
	}

	/**
	 * Compatibility overload for non-platform callers. Historically every
	 * landscape viewport used the desktop action scale.
	 */
	public static float resolveWorldZoom(
			float screenWidth,
			float tileSize,
			boolean landscape,
			float minimumZoom,
			float maximumZoom) {
		return resolveWorldZoom(
				screenWidth,
				tileSize,
				landscape,
				true,
				minimumZoom,
				maximumZoom);
	}

	private static boolean finite(float value) {
		return value == value
				&& value > -Float.MAX_VALUE
				&& value < Float.MAX_VALUE;
	}
}
