package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

/**
 * Final presentation guard for the realtime raid camera.
 *
 * <p>The smooth follower remains authoritative. This class only corrects
 * impossible/outdated scroll values, keeps the operator in a central safe
 * band, clamps the view to the authored world and aligns the result to a
 * physical-pixel boundary.</p>
 */
public final class BukovViewport {

	private static final float SAFE_BAND_START = 0.32f;
	private static final float SAFE_BAND_END = 0.68f;

	private BukovViewport() {
	}

	public static float resolveScroll(
			float currentScroll,
			float focus,
			float viewportSize,
			float worldSize,
			float zoom) {
		if (!finite(currentScroll)
				|| !finite(focus)
				|| !finite(viewportSize)
				|| !finite(worldSize)
				|| !finite(zoom)
				|| viewportSize <= 0f
				|| worldSize <= 0f
				|| zoom <= 0f) {
			return currentScroll;
		}

		float maximumScroll = Math.max(0f, worldSize - viewportSize);
		if (maximumScroll == 0f) {
			return pixelAlign((worldSize - viewportSize) * 0.5f, zoom);
		}

		float resolved = clamp(currentScroll, 0f, maximumScroll);
		float safeStart = resolved + viewportSize * SAFE_BAND_START;
		float safeEnd = resolved + viewportSize * SAFE_BAND_END;
		if (focus < safeStart) {
			resolved = focus - viewportSize * SAFE_BAND_START;
		} else if (focus > safeEnd) {
			resolved = focus - viewportSize * SAFE_BAND_END;
		}
		resolved = clamp(resolved, 0f, maximumScroll);
		return pixelAlign(resolved, zoom);
	}

	private static float pixelAlign(float value, float zoom) {
		return Math.round(value * zoom) / zoom;
	}

	private static float clamp(float value, float minimum, float maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}

	private static boolean finite(float value) {
		return value == value
				&& value > -Float.MAX_VALUE
				&& value < Float.MAX_VALUE;
	}
}
