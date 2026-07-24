package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BukovCombatFxViewPoolTest {

	@Test
	public void productionCapacitiesAreFiniteAndCoverBurstFire() {
		assertTrue(BukovCombatFxViewPool.MUZZLE_CAPACITY >= 12);
		assertTrue(BukovCombatFxViewPool.SHELL_CAPACITY >= 12);
		assertTrue(BukovCombatFxViewPool.TRACER_CAPACITY >= 16);
		assertTrue(BukovCombatFxViewPool.IMPACT_CAPACITY >= 16);
		assertTrue(BukovCombatFxViewPool.MUZZLE_CAPACITY <= 32);
		assertTrue(BukovCombatFxViewPool.SHELL_CAPACITY <= 32);
		assertTrue(BukovCombatFxViewPool.TRACER_CAPACITY <= 32);
		assertTrue(BukovCombatFxViewPool.IMPACT_CAPACITY <= 32);
	}

	@Test
	public void freeSlotWinsBeforeAnyLiveViewIsReused() {
		assertEquals(
				1,
				BukovCombatFxViewPool.oldestOrFree(
						new boolean[]{true, false, true},
						new long[]{1L, 0L, 2L}));
	}

	@Test
	public void saturationReusesTheOldestPresentationOnlySlot() {
		assertEquals(
				1,
				BukovCombatFxViewPool.oldestOrFree(
						new boolean[]{true, true, true},
						new long[]{8L, 3L, 12L}));
	}

	@Test(expected = IllegalArgumentException.class)
	public void slotPolicyRejectsMismatchedState() {
		BukovCombatFxViewPool.oldestOrFree(
				new boolean[]{true},
				new long[]{1L, 2L});
	}
}
