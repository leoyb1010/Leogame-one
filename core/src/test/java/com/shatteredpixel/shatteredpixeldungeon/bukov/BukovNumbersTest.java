package com.shatteredpixel.shatteredpixeldungeon.bukov;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovNumbersTest {

	@Test
	public void finiteCheckRejectsNaNAndInfinities() {
		assertTrue(BukovNumbers.isFinite(1.25f));
		assertFalse(BukovNumbers.isFinite(Float.NaN));
		assertFalse(BukovNumbers.isFinite(Float.POSITIVE_INFINITY));
		assertFalse(BukovNumbers.isFinite(Double.NEGATIVE_INFINITY));
	}

	@Test
	public void compatibilityArithmeticMatchesJavaContract() {
		assertEquals(2, BukovNumbers.floorMod(-8, 5));
		assertEquals(360L, BukovNumbers.floorMod(-1L, 361L));
		assertEquals(
				Long.remainderUnsigned(Long.MIN_VALUE, 7L),
				BukovNumbers.remainderUnsigned(Long.MIN_VALUE, 7L));
		assertEquals(
				Long.toUnsignedString(Long.MIN_VALUE),
				BukovNumbers.toUnsignedString(Long.MIN_VALUE));
		assertEquals(
				Integer.toUnsignedLong(-1),
				BukovNumbers.toUnsignedLong(-1));
		assertEquals(30, BukovNumbers.addExact(10, 20));
		assertEquals(30L, BukovNumbers.addExact(10L, 20L));
	}

	@Test(expected = ArithmeticException.class)
	public void checkedAdditionPreservesOverflowFailure() {
		BukovNumbers.addExact(Long.MAX_VALUE, 1L);
	}
}
