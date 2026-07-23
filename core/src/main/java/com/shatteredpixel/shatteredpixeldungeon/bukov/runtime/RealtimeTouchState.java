package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.watabou.utils.PointF;

/**
 * Renderer-independent dual-zone touch state.
 *
 * The top action strip is reserved for explicit interact/reload taps. Below
 * it, the left half is a floating movement stick and the right half is a
 * floating aim/fire stick.
 */
public final class RealtimeTouchState {

	static final float ACTION_STRIP_FRACTION = 0.18f;
	private static final float AIM_DEAD_ZONE_SQUARED = 16f;

	private int movementPointer = -1;
	private int firePointer = -1;
	private int interactPointer = -1;
	private float movementStartX;
	private float movementStartY;
	private float movementX;
	private float movementY;
	private float fireStartX;
	private float fireStartY;
	private float fireX;
	private float fireY;
	private boolean interactPressed;
	private boolean reloadPressed;

	public void pointerDown(
			int pointerId,
			float x,
			float y,
			float screenWidth,
			float screenHeight) {
		if (screenWidth <= 0f || screenHeight <= 0f) {
			return;
		}
		boolean left = x < screenWidth * 0.5f;
		if (y <= screenHeight * ACTION_STRIP_FRACTION) {
			if (left) {
				interactPressed = true;
				interactPointer = pointerId;
			} else {
				reloadPressed = true;
			}
			return;
		}
		if (left && movementPointer < 0) {
			movementPointer = pointerId;
			movementStartX = movementX = x;
			movementStartY = movementY = y;
		} else if (!left && firePointer < 0) {
			firePointer = pointerId;
			fireStartX = fireX = x;
			fireStartY = fireY = y;
		}
	}

	public void pointerMoved(int pointerId, float x, float y) {
		if (pointerId == movementPointer) {
			movementX = x;
			movementY = y;
		}
		if (pointerId == firePointer) {
			fireX = x;
			fireY = y;
		}
	}

	public void pointerUp(int pointerId) {
		if (pointerId == movementPointer) {
			movementPointer = -1;
		}
		if (pointerId == firePointer) {
			firePointer = -1;
		}
		if (pointerId == interactPointer) {
			interactPointer = -1;
		}
	}

	public void sample(float stickRadius, PointF movement, PointF aim) {
		if (stickRadius <= 0f) {
			throw new IllegalArgumentException("stickRadius must be positive");
		}
		if (movementPointer >= 0) {
			clampInto(
					movementX - movementStartX,
					movementY - movementStartY,
					stickRadius,
					movement);
		}
		if (firePointer >= 0) {
			float deltaX = fireX - fireStartX;
			float deltaY = fireY - fireStartY;
			if (deltaX * deltaX + deltaY * deltaY > AIM_DEAD_ZONE_SQUARED) {
				RealtimeInput.normalizeInto(deltaX, deltaY, aim);
			}
		}
	}

	public boolean movementActive() {
		return movementPointer >= 0;
	}

	public boolean fireHeld() {
		return firePointer >= 0;
	}

	public boolean interactHeld() {
		return interactPointer >= 0;
	}

	public boolean consumeInteractPressed() {
		boolean result = interactPressed;
		interactPressed = false;
		return result;
	}

	public boolean consumeReloadPressed() {
		boolean result = reloadPressed;
		reloadPressed = false;
		return result;
	}

	public void reset() {
		movementPointer = -1;
		firePointer = -1;
		interactPointer = -1;
		interactPressed = false;
		reloadPressed = false;
	}

	private static void clampInto(
			float x,
			float y,
			float radius,
			PointF output) {
		float lengthSquared = x * x + y * y;
		if (lengthSquared <= 0.000001f) {
			output.set(0f, 0f);
			return;
		}
		float length = (float)Math.sqrt(lengthSquared);
		float scale = Math.min(1f, length / radius) / length;
		output.set(x * scale, y * scale);
	}
}
