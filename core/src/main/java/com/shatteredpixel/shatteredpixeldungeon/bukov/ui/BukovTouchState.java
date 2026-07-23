package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import java.util.Arrays;

/**
 * Renderer-independent multi-touch state used by {@link BukovTouchControls}.
 * It is intentionally allocation-free while a raid is running.
 */
public final class BukovTouchState {

	public enum Stick {
		MOVEMENT,
		AIM_FIRE
	}

	public enum Action {
		INTERACT,
		RELOAD,
		MEDICAL,
		DROP,
		BACKPACK,
		PAUSE
	}

	public static final float DEFAULT_DEAD_ZONE = 0.18f;

	private final float deadZone;
	private final StickState movement = new StickState();
	private final StickState aimFire = new StickState();
	private final int[] actionPointers = new int[Action.values().length];
	private final boolean[] actionPressed = new boolean[Action.values().length];

	public BukovTouchState() {
		this(DEFAULT_DEAD_ZONE);
	}

	public BukovTouchState(float deadZone) {
		if (deadZone < 0f || deadZone >= 1f) {
			throw new IllegalArgumentException("dead zone must be in [0, 1)");
		}
		this.deadZone = deadZone;
		Arrays.fill(actionPointers, -1);
	}

	public boolean beginStick(
			Stick stick,
			int pointerId,
			float centerX,
			float centerY,
			float radius,
			float pointerX,
			float pointerY) {
		if (radius <= 0f || pointerInUse(pointerId)) {
			return false;
		}
		StickState target = state(stick);
		if (target.pointerId != -1) {
			return false;
		}
		target.pointerId = pointerId;
		target.centerX = centerX;
		target.centerY = centerY;
		target.radius = radius;
		updateStick(target, pointerX, pointerY);
		return true;
	}

	public boolean beginAction(Action action, int pointerId) {
		if (pointerInUse(pointerId)) {
			return false;
		}
		int index = action.ordinal();
		if (actionPointers[index] != -1) {
			return false;
		}
		actionPointers[index] = pointerId;
		actionPressed[index] = true;
		return true;
	}

	public void movePointer(int pointerId, float x, float y) {
		if (movement.pointerId == pointerId) {
			updateStick(movement, x, y);
		}
		if (aimFire.pointerId == pointerId) {
			updateStick(aimFire, x, y);
		}
	}

	public void endPointer(int pointerId) {
		if (movement.pointerId == pointerId) {
			movement.reset();
		}
		if (aimFire.pointerId == pointerId) {
			aimFire.reset();
		}
		for (int i = 0; i < actionPointers.length; i++) {
			if (actionPointers[i] == pointerId) {
				actionPointers[i] = -1;
			}
		}
	}

	public float movementX() {
		return movement.outputX;
	}

	public float movementY() {
		return movement.outputY;
	}

	public float aimX() {
		return aimFire.outputX;
	}

	public float aimY() {
		return aimFire.outputY;
	}

	public boolean movementHeld() {
		return movement.pointerId != -1;
	}

	public boolean aimHeld() {
		return aimFire.pointerId != -1;
	}

	/**
	 * Fire begins only after the aim stick leaves its dead zone. This prevents a
	 * finger landing near the centre from wasting ammunition.
	 */
	public boolean fireHeld() {
		return aimHeld() && (aimFire.outputX != 0f || aimFire.outputY != 0f);
	}

	public boolean actionHeld(Action action) {
		return actionPointers[action.ordinal()] != -1;
	}

	public boolean consumePressed(Action action) {
		int index = action.ordinal();
		boolean result = actionPressed[index];
		actionPressed[index] = false;
		return result;
	}

	public void reset() {
		movement.reset();
		aimFire.reset();
		Arrays.fill(actionPointers, -1);
		Arrays.fill(actionPressed, false);
	}

	private void updateStick(StickState target, float x, float y) {
		float dx = (x - target.centerX) / target.radius;
		float dy = (y - target.centerY) / target.radius;
		float magnitude = (float)Math.sqrt(dx * dx + dy * dy);
		if (magnitude <= deadZone) {
			target.outputX = 0f;
			target.outputY = 0f;
			return;
		}
		float clampedMagnitude = Math.min(1f, magnitude);
		float scaledMagnitude = (clampedMagnitude - deadZone) / (1f - deadZone);
		float inverseMagnitude = 1f / magnitude;
		target.outputX = dx * inverseMagnitude * scaledMagnitude;
		target.outputY = dy * inverseMagnitude * scaledMagnitude;
	}

	private boolean pointerInUse(int pointerId) {
		if (pointerId < 0) {
			return true;
		}
		if (movement.pointerId == pointerId || aimFire.pointerId == pointerId) {
			return true;
		}
		for (int actionPointer : actionPointers) {
			if (actionPointer == pointerId) {
				return true;
			}
		}
		return false;
	}

	private StickState state(Stick stick) {
		if (stick == null) {
			throw new IllegalArgumentException("stick is required");
		}
		return stick == Stick.MOVEMENT ? movement : aimFire;
	}

	private static final class StickState {
		int pointerId = -1;
		float centerX;
		float centerY;
		float radius;
		float outputX;
		float outputY;

		void reset() {
			pointerId = -1;
			centerX = 0f;
			centerY = 0f;
			radius = 0f;
			outputX = 0f;
			outputY = 0f;
		}
	}
}
