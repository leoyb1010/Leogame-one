package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BukovWhiteLinePresentationTimingTest {

	@Test
	public void weakPointWindowScalesOnlyItsCoveredPresentationTime() {
		assertEquals(
				0.06f,
				BukovWhiteLineSprite.scaledPresentationElapsed(0.2f, 0.2f),
				0.0001f);
		assertEquals(
				0.11f,
				BukovWhiteLineSprite.scaledPresentationElapsed(0.25f, 0.2f),
				0.0001f);
		assertEquals(6, BukovWhiteLineSprite.PHASE_TRANSITION_FRAME_COUNT);
		assertEquals(
				0.2f,
				BukovWhiteLineSprite.WEAK_POINT_SLOW_MOTION_SECONDS,
				0f);
		assertEquals(
				0.3f,
				BukovWhiteLineSprite.WEAK_POINT_SLOW_MOTION_SCALE,
				0f);
	}
}
