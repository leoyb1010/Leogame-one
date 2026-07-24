package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovSettlementRevealModelTest {

	@Test
	public void sixHundredMillisecondClockRevealsRowsAndExactTotal() {
		BukovSettlementRevealModel reveal =
				new BukovSettlementRevealModel(3, 900L);

		assertEquals(0, reveal.visibleRows());
		assertEquals(0L, reveal.displayedValue());
		assertFalse(reveal.stampVisible());

		reveal.advance(0.2f);
		assertEquals(1, reveal.visibleRows());
		assertEquals(300L, reveal.displayedValue());
		reveal.advance(0.2f);
		assertEquals(2, reveal.visibleRows());
		assertEquals(600L, reveal.displayedValue());
		reveal.advance(0.2f);

		assertTrue(reveal.complete());
		assertEquals(3, reveal.visibleRows());
		assertEquals(900L, reveal.displayedValue());
		assertTrue(reveal.stampVisible());
	}

	@Test
	public void skipPublishesEveryFinalStateWithoutChangingValue() {
		BukovSettlementRevealModel reveal =
				new BukovSettlementRevealModel(7, 12_345L);
		reveal.advance(0.05f);
		reveal.skip();

		assertTrue(reveal.complete());
		assertEquals(7, reveal.visibleRows());
		assertEquals(12_345L, reveal.displayedValue());
		assertTrue(reveal.stampVisible());
	}
}
