package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EnemyRangedRoleSelectorTest {

	@Test
	public void hostRangedTypesBeatFloorOneFallback() {
		assertTrue(EnemyRangedRoleSelector.shouldReplace(
				4,
				"Snake",
				9,
				"GnollTrickster"
		));
		assertTrue(EnemyRangedRoleSelector.shouldReplace(
				4,
				"Snake",
				10,
				"DM100"
		));
	}

	@Test
	public void snakeBeatsOrdinaryFloorOneEnemy() {
		assertTrue(EnemyRangedRoleSelector.shouldReplace(
				1,
				"Rat",
				8,
				"Snake"
		));
	}

	@Test
	public void equalPriorityUsesLowestStableId() {
		assertTrue(EnemyRangedRoleSelector.shouldReplace(
				9,
				"Rat",
				3,
				"Rat"
		));
		assertFalse(EnemyRangedRoleSelector.shouldReplace(
				3,
				"Rat",
				9,
				"Rat"
		));
	}
}
