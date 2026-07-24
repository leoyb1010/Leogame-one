package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FixedStepClockTest {

	@Test
	public void advancesAtOneHundredTwentyHertz() {
		FixedStepClock clock = new FixedStepClock(120f, 0.1f, 8);
		AtomicInteger steps = new AtomicInteger();

		float alpha = clock.advance(1f / 60f, delta -> {
			assertEquals(1f / 120f, delta, 0.000001f);
			steps.incrementAndGet();
		});

		assertEquals(2, steps.get());
		assertEquals(0f, alpha, 0.00001f);
	}

	@Test
	public void clampsLongFramesAndDropsExcessBacklog() {
		FixedStepClock clock = new FixedStepClock(120f, 0.1f, 8);
		AtomicInteger steps = new AtomicInteger();

		float alpha = clock.advance(1f, ignored -> steps.incrementAndGet());

		assertEquals(8, steps.get());
		assertTrue(alpha >= 0f && alpha < 1f);
	}

	@Test
	public void conditionalAdvanceStopsAndDropsRemainingFrameBacklog() {
		FixedStepClock clock = new FixedStepClock(120f, 0.1f, 8);
		AtomicInteger steps = new AtomicInteger();

		float alpha = clock.advanceWhile(1f / 60f, ignored ->
				steps.incrementAndGet() < 1);

		assertEquals(1, steps.get());
		assertEquals(0f, alpha, 0f);
		clock.advance(0f, ignored -> steps.incrementAndGet());
		assertEquals(1, steps.get());
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsInvalidRate() {
		new FixedStepClock(0f, 0.1f, 8);
	}
}
