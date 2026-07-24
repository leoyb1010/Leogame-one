package com.shatteredpixel.shatteredpixeldungeon.bukov.content;

import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyTier;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BukovEnemyFirearmDropPolicyTest {

	@Test
	public void eliteAndBossFirearmsAlwaysRemainRecoverable() {
		assertTrue(BukovEnemyFirearmDropPolicy.shouldDrop(
				EnemyTier.ELITE, "iron_clasp_marksman", 17));
		assertTrue(BukovEnemyFirearmDropPolicy.shouldDrop(
				EnemyTier.BOSS, "boss_white_line", 23));
	}

	@Test
	public void commonDropRollIsStableAcrossCheckpointResume() {
		boolean first = BukovEnemyFirearmDropPolicy.shouldDrop(
				EnemyTier.COMMON, "scavenger_gunner", 42);
		boolean resumed = BukovEnemyFirearmDropPolicy.shouldDrop(
				EnemyTier.COMMON, "scavenger_gunner", 42);
		assertEquals(first, resumed);
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidIdentityCannotCreateARerollableDrop() {
		BukovEnemyFirearmDropPolicy.shouldDrop(
				EnemyTier.COMMON, "", 1);
	}
}
