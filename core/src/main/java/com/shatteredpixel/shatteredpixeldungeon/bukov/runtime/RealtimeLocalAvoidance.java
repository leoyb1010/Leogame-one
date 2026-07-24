package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

/**
 * Allocation-free local separation for one realtime enemy.
 *
 * <p>This is intentionally local rather than a second path planner: the grid
 * navigator owns walls and rooms, while this resolver prevents nearby allies
 * from choosing the same point and creating doorway deadlocks.</p>
 */
public final class RealtimeLocalAvoidance {

	public static final float DEFAULT_CLEARANCE_TILES = 0.78f;
	private static final float REPULSION_BLEND = 0.82f;

	private final int stableKey;
	private float desiredX;
	private float desiredY;
	private float repulsionX;
	private float repulsionY;
	private float resolvedX;
	private float resolvedY;
	private int neighbourCount;
	private boolean resolved;

	public RealtimeLocalAvoidance(int stableKey) {
		this.stableKey = stableKey;
	}

	public void begin(float desiredX, float desiredY) {
		requireFinite(desiredX, "desiredX");
		requireFinite(desiredY, "desiredY");
		this.desiredX = desiredX;
		this.desiredY = desiredY;
		repulsionX = 0f;
		repulsionY = 0f;
		neighbourCount = 0;
		resolved = false;
	}

	public void avoid(
			float selfX,
			float selfY,
			float otherX,
			float otherY,
			int otherStableKey,
			float clearance) {
		requireFinite(selfX, "selfX");
		requireFinite(selfY, "selfY");
		requireFinite(otherX, "otherX");
		requireFinite(otherY, "otherY");
		if (clearance <= 0f || Float.isNaN(clearance)
				|| Float.isInfinite(clearance)) {
			throw new IllegalArgumentException(
					"clearance must be finite and positive");
		}
		float awayX = selfX - otherX;
		float awayY = selfY - otherY;
		float distanceSquared = awayX * awayX + awayY * awayY;
		if (distanceSquared >= clearance * clearance) {
			return;
		}

		float unitX;
		float unitY;
		float distance;
		if (distanceSquared <= 0.000001f) {
			int low = Math.min(stableKey, otherStableKey);
			int high = Math.max(stableKey, otherStableKey);
			int direction = mix(low, high) & 3;
			unitX = direction == 0 ? 1f : direction == 2 ? -1f : 0f;
			unitY = direction == 1 ? 1f : direction == 3 ? -1f : 0f;
			if (stableKey > otherStableKey) {
				unitX = -unitX;
				unitY = -unitY;
			}
			distance = 0f;
		} else {
			distance = (float)Math.sqrt(distanceSquared);
			float inverseDistance = 1f / distance;
			unitX = awayX * inverseDistance;
			unitY = awayY * inverseDistance;
		}
		float strength = 1f - distance / clearance;
		repulsionX += unitX * strength;
		repulsionY += unitY * strength;
		neighbourCount++;
		resolved = false;
	}

	public float desiredX() {
		resolve();
		return resolvedX;
	}

	public float desiredY() {
		resolve();
		return resolvedY;
	}

	private void resolve() {
		if (resolved) return;
		resolvedX = desiredX;
		resolvedY = desiredY;
		if (neighbourCount > 0) {
			float inverseCount = 1f / neighbourCount;
			resolvedX += repulsionX * inverseCount * REPULSION_BLEND;
			resolvedY += repulsionY * inverseCount * REPULSION_BLEND;
		}
		float lengthSquared = resolvedX * resolvedX + resolvedY * resolvedY;
		if (lengthSquared <= 0.000001f) {
			resolvedX = 0f;
			resolvedY = 0f;
			resolved = true;
			return;
		}
		float inverseLength = 1f / (float)Math.sqrt(lengthSquared);
		resolvedX *= inverseLength;
		resolvedY *= inverseLength;
		resolved = true;
	}

	private static int mix(int low, int high) {
		int value = low * 0x45d9f3b + high * 0x119de1f3;
		value ^= value >>> 16;
		return value;
	}

	private static void requireFinite(float value, String label) {
		if (Float.isNaN(value) || Float.isInfinite(value)) {
			throw new IllegalArgumentException(label + " must be finite");
		}
	}
}
