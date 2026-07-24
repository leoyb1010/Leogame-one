package com.shatteredpixel.shatteredpixeldungeon.bukov.content;

import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyTier;

/**
 * Deterministic weapon recovery policy for armed first-raid enemies.
 *
 * Elite firearms are always recoverable. Common enemies use a stable roll so
 * checkpoint resume cannot reroll their weapon into existence.
 */
public final class BukovEnemyFirearmDropPolicy {

	private static final int COMMON_DROP_PERCENT = 35;

	public static boolean shouldDrop(
			EnemyTier tier,
			String archetypeId,
			int actorId) {
		if (tier == null
				|| archetypeId == null
				|| archetypeId.trim().isEmpty()
				|| actorId < 0) {
			throw new IllegalArgumentException(
					"tier, archetypeId and actorId are required");
		}
		if (tier == EnemyTier.BOSS || tier == EnemyTier.ELITE) {
			return true;
		}
		int hash = 17;
		hash = 31 * hash + archetypeId.hashCode();
		hash = 31 * hash + actorId;
		return com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
				.floorMod(hash, 100) < COMMON_DROP_PERCENT;
	}

	private BukovEnemyFirearmDropPolicy() {
	}
}
