package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

/**
 * Render-rate camera follow math, kept independent from Noosa so its
 * dead-zone and convergence behaviour can be tested without launching a game.
 */
public final class RealtimeCameraFollow {

	private final float halfDeadZoneX;
	private final float halfDeadZoneY;
	private final float responsiveness;
	private float centerX;
	private float centerY;
	private boolean initialized;

	public RealtimeCameraFollow(
			float halfDeadZoneX,
			float halfDeadZoneY,
			float responsiveness) {
		if (halfDeadZoneX < 0f || halfDeadZoneY < 0f
				|| responsiveness <= 0f) {
			throw new IllegalArgumentException(
					"dead zone must be non-negative and responsiveness positive");
		}
		this.halfDeadZoneX = halfDeadZoneX;
		this.halfDeadZoneY = halfDeadZoneY;
		this.responsiveness = responsiveness;
	}

	public void reset(float centerX, float centerY) {
		if (!finite(centerX) || !finite(centerY)) {
			throw new IllegalArgumentException("camera center must be finite");
		}
		this.centerX = centerX;
		this.centerY = centerY;
		initialized = true;
	}

	public void update(float targetX, float targetY, float deltaSeconds) {
		if (!finite(targetX) || !finite(targetY)
				|| !finite(deltaSeconds) || deltaSeconds <= 0f) {
			return;
		}
		if (!initialized) {
			reset(targetX, targetY);
			return;
		}

		float desiredX = centerX;
		float desiredY = centerY;
		if (targetX > centerX + halfDeadZoneX) {
			desiredX = targetX - halfDeadZoneX;
		} else if (targetX < centerX - halfDeadZoneX) {
			desiredX = targetX + halfDeadZoneX;
		}
		if (targetY > centerY + halfDeadZoneY) {
			desiredY = targetY - halfDeadZoneY;
		} else if (targetY < centerY - halfDeadZoneY) {
			desiredY = targetY + halfDeadZoneY;
		}

		float safeDelta = Math.min(deltaSeconds, 0.25f);
		float blend = (float)(1d - Math.exp(-responsiveness * safeDelta));
		centerX += (desiredX - centerX) * blend;
		centerY += (desiredY - centerY) * blend;
	}

	public float centerX() {
		return centerX;
	}

	public float centerY() {
		return centerY;
	}

	public boolean initialized() {
		return initialized;
	}

	private static boolean finite(float value) {
		return value == value
				&& value > -Float.MAX_VALUE
				&& value < Float.MAX_VALUE;
	}
}
