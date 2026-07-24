package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovSettlementRevealModelTest {

	@Test
	public void ritualClockRevealsRowsAndExactTotal() {
		BukovSettlementRevealModel reveal =
				new BukovSettlementRevealModel(
						3, 900L, 900, 70, false);

		assertEquals(0, reveal.visibleRows());
		assertEquals(0L, reveal.displayedValue());
		assertFalse(reveal.stampVisible());

		reveal.advance(0.3f);
		assertEquals(1, reveal.visibleRows());
		assertEquals(300L, reveal.displayedValue());
		reveal.advance(0.3f);
		assertEquals(2, reveal.visibleRows());
		assertEquals(600L, reveal.displayedValue());
		reveal.advance(0.3f);

		assertTrue(reveal.complete());
		assertEquals(3, reveal.visibleRows());
		assertEquals(900L, reveal.displayedValue());
		assertTrue(reveal.stampVisible());
	}

	@Test
	public void skipPublishesEveryFinalStateWithoutChangingValue() {
		BukovSettlementRevealModel reveal =
				new BukovSettlementRevealModel(
						7, 12_345L, 900, 70, false);
		reveal.advance(0.05f);
		reveal.skip();
		reveal.skip();
		reveal.advance(1f);

		assertTrue(reveal.complete());
		assertEquals(7, reveal.visibleRows());
		assertEquals(12_345L, reveal.displayedValue());
		assertTrue(reveal.stampVisible());
	}

	@Test
	public void reducedMotionInstantClockPublishesAllInformationQuickly() {
		BukovSettlementRevealModel reveal =
				new BukovSettlementRevealModel(
						12, 98_765L, 900, 70, true);

		reveal.advance(0.069f);
		assertFalse(reveal.complete());
		assertTrue(reveal.visibleRows() < 12);
		assertTrue(reveal.displayedValue() < 98_765L);

		reveal.advance(0.0011f);

		assertTrue(reveal.complete());
		assertEquals(12, reveal.visibleRows());
		assertEquals(98_765L, reveal.displayedValue());
		assertTrue(reveal.stampVisible());
	}

	@Test
	public void invalidDurationCannotCreateAStuckReveal() {
		try {
			new BukovSettlementRevealModel(
					1, 1L, 900, 0, true);
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage().contains("durations"));
			return;
		}
		throw new AssertionError("zero-duration reveal must be rejected");
	}
}
