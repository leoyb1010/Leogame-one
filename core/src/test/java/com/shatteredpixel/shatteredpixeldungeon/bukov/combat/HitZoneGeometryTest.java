package com.shatteredpixel.shatteredpixeldungeon.bukov.combat;

import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.RealtimeBody;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HitZoneGeometryTest {

	@Test
	public void ordinaryTargetSeparatesCoreFromLimb() {
		RealtimeBody body = body(4.5f, 2.5f, 0.3f);

		assertEquals(
				RealtimeDamage.HitZone.CORE,
				HitZoneGeometry.resolve(
						body,
						1.5f,
						2.5f,
						1f,
						0f,
						false,
						false));
		assertEquals(
				RealtimeDamage.HitZone.LIMB,
				HitZoneGeometry.resolve(
						body,
						1.5f,
						2.74f,
						1f,
						0f,
						false,
						false));
	}

	@Test
	public void bossWeakpointOnlyExistsDuringVulnerableWindow() {
		RealtimeBody boss = body(4.5f, 2.5f, 0.36f);

		assertEquals(
				RealtimeDamage.HitZone.CORE,
				HitZoneGeometry.resolve(
						boss,
						1.5f,
						2.5f,
						1f,
						0f,
						true,
						false));
		assertEquals(
				RealtimeDamage.HitZone.BOSS_WEAKPOINT,
				HitZoneGeometry.resolve(
						boss,
						1.5f,
						2.5f,
						1f,
						0f,
						true,
						true));
	}

	@Test
	public void vulnerableBossStillReportsLimbOutsideWeakpointAndCore() {
		RealtimeBody boss = body(4.5f, 2.5f, 0.36f);

		assertEquals(
				RealtimeDamage.HitZone.LIMB,
				HitZoneGeometry.resolve(
						boss,
						1.5f,
						2.79f,
						1f,
						0f,
						true,
						true));
	}

	@Test
	public void hitZoneMultiplierChangesResolvedDamage() {
		float core = damage(RealtimeDamage.HitZone.CORE);
		float limb = damage(RealtimeDamage.HitZone.LIMB);
		float weakpoint = damage(
				RealtimeDamage.HitZone.BOSS_WEAKPOINT);

		assertEquals(72f, limb, 0.0001f);
		assertEquals(100f, core, 0.0001f);
		assertEquals(150f, weakpoint, 0.0001f);
	}

	private static float damage(RealtimeDamage.HitZone zone) {
		return RealtimeDamage.resolve(
				100f,
				1f,
				1f,
				10f,
				0f,
				zone,
				null);
	}

	private static RealtimeBody body(
			float x, float y, float radius) {
		RealtimeBody body = new RealtimeBody();
		body.x = x;
		body.y = y;
		body.previousX = x;
		body.previousY = y;
		body.radius = radius;
		return body;
	}
}
