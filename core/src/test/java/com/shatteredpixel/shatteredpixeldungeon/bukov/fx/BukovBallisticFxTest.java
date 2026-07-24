package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import com.watabou.utils.PointF;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovBallisticFxTest {

	@Test
	public void tracerGeometrySpansExactMuzzleToImpactVector() {
		BukovTracerFx.TraceGeometry geometry = BukovTracerFx.plan(
				new PointF(3f, 4f),
				new PointF(15f, 9f),
				1f);

		assertTrue(geometry.visible());
		assertEquals(3f, geometry.fromX(), 0f);
		assertEquals(4f, geometry.fromY(), 0f);
		assertEquals(15f, geometry.toX(), 0f);
		assertEquals(9f, geometry.toY(), 0f);
		assertEquals(13f, geometry.length(), 0.0001f);
		assertEquals(
				(float) Math.toDegrees(Math.atan2(5f, 12f)),
				geometry.angleDegrees(),
				0.0001f);
		assertTrue(geometry.glowThickness() > geometry.coreThickness());
	}

	@Test
	public void zeroLengthOrInvalidTraceIsNeverVisible() {
		assertFalse(BukovTracerFx.plan(
				new PointF(6f, 6f),
				new PointF(6f, 6f),
				1f).visible());
		assertFalse(BukovTracerFx.plan(
				new PointF(Float.NaN, 2f),
				new PointF(3f, 4f),
				1f).visible());
		assertFalse(BukovTracerFx.plan(null, new PointF(3f, 4f), 1f).visible());
	}

	@Test
	public void tracerHasBriefMonotonicFadeAndHardExpiry() {
		float duration = BukovTracerFx.DURATION_SECONDS;

		assertTrue(duration >= 0.14f && duration <= 0.20f);
		assertEquals(1f, BukovTracerFx.alphaAt(0f, duration), 0f);
		assertEquals(0.5f, BukovTracerFx.alphaAt(duration * 0.5f, duration), 0.0001f);
		assertEquals(0f, BukovTracerFx.alphaAt(duration, duration), 0f);
		assertFalse(BukovTracerFx.expiredAt(duration - 0.001f, duration));
		assertTrue(BukovTracerFx.expiredAt(duration, duration));
		assertTrue(BukovTracerFx.expiredAt(Float.NaN, duration));
	}

	@Test
	public void brightProjectileHeadTraversesTheWholeHitscanTrace() {
		float duration = BukovTracerFx.DURATION_SECONDS;

		assertEquals(0f, BukovTracerFx.travelProgressAt(0f, duration), 0f);
		assertEquals(
				0.5f,
				BukovTracerFx.travelProgressAt(duration * 0.5f, duration),
				0.0001f);
		assertEquals(1f, BukovTracerFx.travelProgressAt(duration, duration), 0f);
		assertEquals(1f, BukovTracerFx.travelProgressAt(duration * 2f, duration), 0f);
		assertEquals(0f, BukovTracerFx.travelProgressAt(Float.NaN, duration), 0f);
	}

	@Test
	public void supportingFxStayWithinReadableCombatWindow() {
		assertTrue(BukovMuzzleFx.DURATION_SECONDS >= 0.10f);
		assertTrue(BukovMuzzleFx.DURATION_SECONDS <= 0.16f);
		assertTrue(BukovImpactFx.DURATION_SECONDS >= 0.12f);
		assertTrue(BukovImpactFx.DURATION_SECONDS <= 0.18f);
		assertFalse(BukovTracerFx.FRIENDLY_COLOR == BukovTracerFx.HOSTILE_COLOR);
	}
}
