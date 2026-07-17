package com.shatteredpixel.shatteredpixeldungeon.actors;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CombatMathTest {

	@Test
	public void levelOneHeroKeepsExpectedBaseAccuracy() {
		assertEquals(0.90f, CombatMath.baseHitChance(10f, 2f), 0.0001f);
		assertEquals(0.20f, CombatMath.baseHitChance(10f, 25f), 0.0001f);
	}

	@Test
	public void rollComparisonMatchesOriginalRule() {
		assertTrue(CombatMath.rollHits(4f, 4f));
		assertTrue(CombatMath.rollHits(5f, 4f));
		assertFalse(CombatMath.rollHits(3.99f, 4f));
	}
}
