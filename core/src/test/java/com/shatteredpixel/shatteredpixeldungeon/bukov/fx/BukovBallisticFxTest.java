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
		assertTrue(geometry.coreThickness() >= 0.8f);
		assertTrue(geometry.coreThickness() <= 1.2f);
		assertTrue(geometry.glowThickness() >= 1.8f);
		assertTrue(geometry.glowThickness() <= 2.5f);
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
	public void tracerHasBriefResidualWindowAndHardExpiry() {
		float duration = BukovTracerFx.DURATION_SECONDS;

		assertTrue(duration >= 0.30f && duration <= 0.42f);
		assertTrue(BukovTracerFx.TRAVEL_SECONDS >= 0.20f);
		assertTrue(BukovTracerFx.TRAVEL_SECONDS < duration);
		assertEquals(1f, BukovTracerFx.alphaAt(0f, duration), 0f);
		assertEquals(0.5f, BukovTracerFx.alphaAt(duration * 0.5f, duration), 0.0001f);
		assertEquals(0f, BukovTracerFx.alphaAt(duration, duration), 0f);
		assertFalse(BukovTracerFx.expiredAt(duration - 0.001f, duration));
		assertTrue(BukovTracerFx.expiredAt(duration, duration));
		assertTrue(BukovTracerFx.expiredAt(Float.NaN, duration));
	}

	@Test
	public void brightProjectileHeadTraversesTheWholeHitscanTrace() {
		float travelDuration = BukovTracerFx.TRAVEL_SECONDS;

		assertEquals(0f, BukovTracerFx.travelProgressAt(0f, travelDuration), 0f);
		assertEquals(
				0.5f,
				BukovTracerFx.travelProgressAt(travelDuration * 0.5f, travelDuration),
				0.0001f);
		assertEquals(1f, BukovTracerFx.travelProgressAt(
				travelDuration,
				travelDuration), 0f);
		assertEquals(1f, BukovTracerFx.travelProgressAt(
				travelDuration * 2f,
				travelDuration), 0f);
		assertEquals(0f, BukovTracerFx.travelProgressAt(
				Float.NaN,
				travelDuration), 0f);
	}

	@Test
	public void tracerTailFollowsTheHeadWithoutDrawingTheWholeShotVector() {
		BukovTracerFx.TraceGeometry geometry = BukovTracerFx.plan(
				new PointF(0f, 0f),
				new PointF(100f, 0f),
				1f);

		assertEquals(32f, BukovTracerFx.tailLengthFor(geometry.length()), 0f);
		assertFalse(BukovTracerFx.tailSegmentAt(geometry, 0f).visible());

		BukovTracerFx.TailSegment halfway = BukovTracerFx.tailSegmentAt(
				geometry,
				BukovTracerFx.TRAVEL_SECONDS * 0.5f);
		assertTrue(halfway.visible());
		assertEquals(18f, halfway.startX(), 0.0001f);
		assertEquals(50f, halfway.endX(), 0.0001f);
		assertEquals(32f, halfway.length(), 0.0001f);
		assertTrue(halfway.length() < geometry.length());

		BukovTracerFx.TailSegment endpoint = BukovTracerFx.tailSegmentAt(
				geometry,
				BukovTracerFx.TRAVEL_SECONDS);
		assertEquals(68f, endpoint.startX(), 0.0001f);
		assertEquals(100f, endpoint.endX(), 0.0001f);
		assertEquals(32f, endpoint.length(), 0.0001f);

		BukovTracerFx.TailSegment residual = BukovTracerFx.tailSegmentAt(
				geometry,
				0.32f);
		assertEquals(endpoint.startX(), residual.startX(), 0f);
		assertEquals(endpoint.endX(), residual.endX(), 0f);
		assertEquals(endpoint.length(), residual.length(), 0f);
	}

	@Test
	public void shortShotsUseAProportionalTailWithoutOvershootingTheMuzzle() {
		BukovTracerFx.TraceGeometry geometry = BukovTracerFx.plan(
				new PointF(10f, 20f),
				new PointF(30f, 20f),
				1f);

		assertEquals(7.6f, BukovTracerFx.tailLengthFor(geometry.length()), 0.0001f);
		BukovTracerFx.TailSegment early = BukovTracerFx.tailSegmentAt(
				geometry,
				BukovTracerFx.TRAVEL_SECONDS * 0.25f);
		assertEquals(10f, early.startX(), 0.0001f);
		assertEquals(15f, early.endX(), 0.0001f);
		assertEquals(5f, early.length(), 0.0001f);
	}

	@Test
	public void tracerDrawStateStaysVisibleAtSixtyAndOneHundredTwentyFps() {
		float frame120 = 1f / 120f;
		float frame60 = 1f / 60f;

		assertTrue(BukovTracerFx.travelProgressAt(
				frame120,
				BukovTracerFx.TRAVEL_SECONDS) > 0f);
		assertTrue(BukovTracerFx.travelProgressAt(
				frame60,
				BukovTracerFx.TRAVEL_SECONDS) > 0f);
		assertEquals(1f, BukovTracerFx.trailAlphaAt(frame120), 0f);
		assertEquals(1f, BukovTracerFx.trailAlphaAt(frame60), 0f);
		assertEquals(1f, BukovTracerFx.headAlphaAt(
				BukovTracerFx.TRAVEL_SECONDS), 0f);
		assertTrue(BukovTracerFx.trailAlphaAt(0.32f) > 0f);
		assertTrue(BukovTracerFx.headAlphaAt(0.32f) > 0f);
		assertEquals(0f, BukovTracerFx.trailAlphaAt(
				BukovTracerFx.DURATION_SECONDS), 0f);
		assertEquals(0f, BukovTracerFx.headAlphaAt(
				BukovTracerFx.DURATION_SECONDS), 0f);
	}

	@Test
	public void tracerColorsAndOutlinedHeadAreActuallyOpaqueAndReadable() {
		assertEquals(0xFF, BukovTracerFx.FRIENDLY_COLOR >>> 24);
		assertEquals(0xFF, BukovTracerFx.HOSTILE_COLOR >>> 24);
		assertEquals(0xFF, BukovTracerFx.HEAD_OUTLINE_COLOR >>> 24);

		float coreThickness = BukovTracerFx.plan(
				new PointF(0f, 0f),
				new PointF(16f, 0f),
				1f).coreThickness();
		assertTrue(BukovTracerFx.headWidthFor(coreThickness) >= 5.2f);
		assertTrue(BukovTracerFx.headWidthFor(coreThickness) < 8f);
		assertTrue(BukovTracerFx.headHeightFor(coreThickness) >= 2.2f);
		assertTrue(BukovTracerFx.headHeightFor(coreThickness) < 3.2f);
		assertTrue(BukovTracerFx.outlineWidthFor(coreThickness)
				> BukovTracerFx.headWidthFor(coreThickness));
		assertTrue(BukovTracerFx.outlineHeightFor(coreThickness)
				> BukovTracerFx.headHeightFor(coreThickness));
	}

	@Test
	public void firstFrameHitchStillPresentsTheTracerEndpointBeforeExpiry() {
		float duration = BukovTracerFx.DURATION_SECONDS;
		float hitchAge = 0.50f;

		assertEquals(1f, BukovTracerFx.travelProgressAt(
				hitchAge,
				BukovTracerFx.TRAVEL_SECONDS), 0f);
		assertFalse(BukovTracerFx.shouldExpireAfterUpdate(
				hitchAge,
				duration,
				false));
		assertTrue(BukovTracerFx.shouldExpireAfterUpdate(
				hitchAge + 1f / 60f,
				duration,
				true));
	}

	@Test
	public void normalFrameRateKeepsTheOriginalTracerLifetime() {
		float duration = BukovTracerFx.DURATION_SECONDS;
		float firstFrameAge = 1f / 120f;

		assertFalse(BukovTracerFx.shouldExpireAfterUpdate(
				firstFrameAge,
				duration,
				false));
		assertFalse(BukovTracerFx.shouldExpireAfterUpdate(
				duration - 0.001f,
				duration,
				true));
		assertTrue(BukovTracerFx.shouldExpireAfterUpdate(
				duration,
				duration,
				true));
		assertTrue(BukovTracerFx.shouldExpireAfterUpdate(
				Float.NaN,
				duration,
				false));
	}

	@Test
	public void supportingFxStayWithinReadableCombatWindow() {
		assertTrue(BukovMuzzleFx.DURATION_SECONDS >= 0.10f);
		assertTrue(BukovMuzzleFx.DURATION_SECONDS <= 0.16f);
		assertTrue(BukovImpactFx.DURATION_SECONDS >= 0.12f);
		assertTrue(BukovImpactFx.DURATION_SECONDS <= 0.18f);
		assertEquals(0.30f, BukovShellFx.DURATION_SECONDS, 0f);
		assertFalse(BukovTracerFx.FRIENDLY_COLOR == BukovTracerFx.HOSTILE_COLOR);
	}

	@Test
	public void shellCasingUsesAVisibleDeterministicParabola() {
		BukovShellFx.ShellTrajectory trajectory = BukovShellFx.plan(
				new PointF(10f, 20f),
				new PointF(1f, 0f),
				1f);

		assertTrue(trajectory.visible());
		assertEquals(10f, trajectory.xAt(0f), 0f);
		assertEquals(20f, trajectory.yAt(0f), 0f);
		assertTrue(trajectory.xAt(1f) > trajectory.xAt(0f));
		assertEquals(20f, trajectory.yAt(1f), 0.0001f);
		assertTrue(trajectory.yAt(0.5f) < trajectory.yAt(0f));
		assertTrue(trajectory.angleAt(1f) > trajectory.angleAt(0f));
		assertEquals(0f, BukovShellFx.progressAt(0f), 0f);
		assertEquals(0.5f, BukovShellFx.progressAt(0.15f), 0.0001f);
		assertEquals(1f, BukovShellFx.progressAt(0.30f), 0f);
	}

	@Test
	public void shellRejectsInvalidOrZeroLengthEjectionVectors() {
		assertFalse(BukovShellFx.plan(
				new PointF(2f, 3f),
				new PointF(0f, 0f),
				1f).visible());
		assertFalse(BukovShellFx.plan(
				new PointF(Float.NaN, 3f),
				new PointF(1f, 0f),
				1f).visible());
	}
}
