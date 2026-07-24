package com.shatteredpixel.shatteredpixeldungeon.sprites;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HeroSpriteRealtimeActionTimingTest {

	@Test
	public void reloadFilmSpansTheAuthoritativeCombatDuration() {
		assertEquals(
				0.4f,
				HeroSprite.reloadFrameDelay(2.4f, 6),
				0.0001f);
	}

	@Test
	public void missingDurationKeepsTheAuthoredFallbackRate() {
		assertEquals(
				1f / 12f,
				HeroSprite.reloadFrameDelay(0f, 6),
				0.0001f);
		assertEquals(
				1f / 12f,
				HeroSprite.reloadFrameDelay(Float.POSITIVE_INFINITY, 6),
				0.0001f);
	}

	@Test
	public void automaticFireCanRestartOnlyItsOwnPriority() {
		assertTrue(CharSprite.acceptsRealtimeAction(true, 1, 1, true));
		assertFalse(CharSprite.acceptsRealtimeAction(true, 1, 1, false));
		assertFalse(CharSprite.acceptsRealtimeAction(true, 2, 1, true));
		assertTrue(CharSprite.acceptsRealtimeAction(true, 1, 2, false));
	}
}
