package com.shatteredpixel.shatteredpixeldungeon.bukov.combat;

import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.RealtimeBody;

/**
 * Small authored colliders layered inside a realtime body's broad-phase
 * circle. The broad-phase remains allocation-free; only the selected target
 * is classified.
 */
public final class HitZoneGeometry {

	public static final float CORE_RADIUS_RATIO = 0.55f;
	public static final float BOSS_WEAKPOINT_RADIUS_RATIO = 0.30f;
	public static final float BOSS_WEAKPOINT_OFFSET_Y_RATIO = -0.18f;

	public static RealtimeDamage.HitZone resolve(
			RealtimeBody body,
			float originX,
			float originY,
			float directionX,
			float directionY,
			boolean boss,
			boolean bossVulnerable) {
		if (body == null || !(body.radius > 0f)) {
			throw new IllegalArgumentException(
					"body with a positive radius is required");
		}
		float length = (float)Math.sqrt(
				directionX * directionX + directionY * directionY);
		if (!(length > 0.00001f)) {
			throw new IllegalArgumentException(
					"non-zero ray direction is required");
		}
		float dx = directionX / length;
		float dy = directionY / length;

		if (boss && bossVulnerable && intersects(
				originX,
				originY,
				dx,
				dy,
				body.x,
				body.y + body.radius
						* BOSS_WEAKPOINT_OFFSET_Y_RATIO,
				body.radius * BOSS_WEAKPOINT_RADIUS_RATIO)) {
			return RealtimeDamage.HitZone.BOSS_WEAKPOINT;
		}
		if (intersects(
				originX,
				originY,
				dx,
				dy,
				body.x,
				body.y,
				body.radius * CORE_RADIUS_RATIO)) {
			return RealtimeDamage.HitZone.CORE;
		}
		return RealtimeDamage.HitZone.LIMB;
	}

	private static boolean intersects(
			float originX,
			float originY,
			float directionX,
			float directionY,
			float centerX,
			float centerY,
			float radius) {
		return HitscanResolver.rayCircle(
				originX,
				originY,
				directionX,
				directionY,
				centerX,
				centerY,
				radius) >= 0f;
	}

	private HitZoneGeometry() {
	}
}
