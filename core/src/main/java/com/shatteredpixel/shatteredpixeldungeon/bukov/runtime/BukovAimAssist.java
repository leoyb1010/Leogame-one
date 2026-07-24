package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.watabou.utils.PointF;

/**
 * Small, deterministic aim-magnetism policy. World visibility remains the
 * authority: callers must only submit candidates which are alive, in the
 * player's FOV and have an unobstructed line of sight.
 */
public final class BukovAimAssist {

	static final float MINIMUM_DOT = 0.90f;

	public static boolean accepts(
			float inputX,
			float inputY,
			float targetX,
			float targetY,
			float maximumRange,
			boolean visible) {
		if (!visible || maximumRange <= 0f) return false;
		float targetLengthSquared = targetX * targetX + targetY * targetY;
		if (targetLengthSquared <= 0.000001f
				|| targetLengthSquared > maximumRange * maximumRange) {
			return false;
		}
		float inputLengthSquared = inputX * inputX + inputY * inputY;
		if (inputLengthSquared <= 0.000001f) return false;
		float dot = (inputX * targetX + inputY * targetY)
				/ (float)Math.sqrt(inputLengthSquared * targetLengthSquared);
		return dot >= MINIMUM_DOT;
	}

	public static float score(
			float inputX,
			float inputY,
			float targetX,
			float targetY,
			float maximumRange) {
		float distance = (float)Math.sqrt(
				targetX * targetX + targetY * targetY);
		float dot = (inputX * targetX + inputY * targetY)
				/ Math.max(0.000001f, distance
						* (float)Math.sqrt(inputX * inputX + inputY * inputY));
		float angularError = 1f - dot;
		float distanceFraction = distance / Math.max(0.000001f, maximumRange);
		return angularError * 4f + distanceFraction;
	}

	public static void blend(
			float inputX,
			float inputY,
			float targetX,
			float targetY,
			float strength,
			PointF output) {
		if (output == null) {
			throw new IllegalArgumentException("output is required");
		}
		float clamped = Math.max(0f, Math.min(1f, strength));
		float targetLength = (float)Math.sqrt(
				targetX * targetX + targetY * targetY);
		if (targetLength <= 0.000001f) {
			RealtimeInput.normalizeInto(inputX, inputY, output);
			return;
		}
		float normalizedTargetX = targetX / targetLength;
		float normalizedTargetY = targetY / targetLength;
		RealtimeInput.normalizeInto(
				inputX + (normalizedTargetX - inputX) * clamped,
				inputY + (normalizedTargetY - inputY) * clamped,
				output);
	}

	private BukovAimAssist() {
	}
}
