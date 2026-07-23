package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;
import com.watabou.utils.PointF;

/**
 * Allocation-free controller response math. This class deliberately has no
 * libGDX input dependency, so dead-zone and curve behaviour can be regression
 * tested without a controller or graphics context.
 */
public final class BukovInputTuning {

	public static void sampleStick(
			float x,
			float y,
			float innerDeadZone,
			float outerDeadZone,
			boolean classicCurve,
			PointF output) {
		if (output == null) {
			throw new IllegalArgumentException("output is required");
		}
		if (!BukovNumbers.isFinite(x)
				|| !BukovNumbers.isFinite(y)
				|| !BukovNumbers.isFinite(innerDeadZone)
				|| !BukovNumbers.isFinite(outerDeadZone)
				|| innerDeadZone < 0f
				|| outerDeadZone > 1f
				|| outerDeadZone <= innerDeadZone) {
			throw new IllegalArgumentException("invalid stick tuning");
		}
		float magnitudeSquared = x * x + y * y;
		if (magnitudeSquared <= innerDeadZone * innerDeadZone) {
			output.set(0f, 0f);
			return;
		}
		float magnitude = (float)Math.sqrt(magnitudeSquared);
		float clampedMagnitude = Math.min(magnitude, outerDeadZone);
		float response =
				(clampedMagnitude - innerDeadZone)
						/ (outerDeadZone - innerDeadZone);
		if (classicCurve) {
			response = response * response * (3f - 2f * response);
		}
		float directionScale = response / magnitude;
		output.set(x * directionScale, y * directionScale);
	}

	public static float aimAssistScale(int level) {
		switch (level) {
			case 0:
				return 0f;
			case 1:
				return 0.15f;
			case 2:
				return 0.30f;
			default:
				throw new IllegalArgumentException(
						"aim assist level must be 0, 1 or 2");
		}
	}

	private BukovInputTuning() {
	}
}
