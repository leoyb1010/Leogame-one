package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RealtimeCameraLookAheadTest {

	@Test
	public void pointerAndDirectAimReachAuthoredTileDistances() {
		assertLookAhead(3.5f * 16f);
		assertLookAhead(2.5f * 16f);
	}

	@Test
	public void releasedAimSmoothlyReturnsToTrackedOperator() {
		RealtimeCameraFollow follow = follow();
		simulate(follow, 60, 1f, 56f, 0f);
		float offset = follow.centerX();

		follow.update(0f, 0f, 0f, 0f, 1f / 60f);
		assertTrue(follow.centerX() < offset);
		assertTrue(follow.centerX() > 0f);

		simulate(follow, 60, 2f, 0f, 0f);
		assertEquals(0f, follow.centerX(), 0.01f);
		assertEquals(0f, follow.centerY(), 0.01f);
	}

	@Test
	public void lookAheadConvergenceIsRenderFrameRateIndependent() {
		RealtimeCameraFollow at30 = follow();
		RealtimeCameraFollow at144 = follow();
		simulate(at30, 30, 1f, 56f, -24f);
		simulate(at144, 144, 1f, 56f, -24f);

		assertEquals(at30.centerX(), at144.centerX(), 0.02f);
		assertEquals(at30.centerY(), at144.centerY(), 0.02f);
	}

	private static void assertLookAhead(float pixels) {
		RealtimeCameraFollow follow = follow();
		simulate(follow, 60, 2f, pixels, 0f);
		assertEquals(pixels, follow.centerX(), 0.01f);
		assertEquals(0f, follow.centerY(), 0.01f);
	}

	private static RealtimeCameraFollow follow() {
		RealtimeCameraFollow follow =
				new RealtimeCameraFollow(12f, 8f, 8f);
		follow.reset(0f, 0f);
		return follow;
	}

	private static void simulate(
			RealtimeCameraFollow follow,
			int framesPerSecond,
			float seconds,
			float lookAheadX,
			float lookAheadY) {
		int frames = Math.round(framesPerSecond * seconds);
		for (int frame = 0; frame < frames; frame++) {
			follow.update(
					0f,
					0f,
					lookAheadX,
					lookAheadY,
					1f / framesPerSecond);
		}
	}
}
