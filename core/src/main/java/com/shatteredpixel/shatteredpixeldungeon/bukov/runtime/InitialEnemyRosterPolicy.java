package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;

/**
 * Reconciles the persisted one-bit initial-spawn latch with the actual host
 * roster. Old checkpoints could record the latch after spawning zero mobs.
 */
final class InitialEnemyRosterPolicy {

	static boolean shouldPopulate(
			boolean completed,
			int liveNonBossEnemies,
			int killCount,
			int targetCount) {
		validate(liveNonBossEnemies, killCount, targetCount);
		if (targetCount == 0 || liveNonBossEnemies >= targetCount) {
			return false;
		}
		if (!completed) return true;
		// A completed latch plus an empty, untouched roster is the signature of
		// the legacy failed-spawn checkpoint. Never repopulate after real kills.
		return liveNonBossEnemies == 0 && killCount == 0;
	}

	static boolean completed(int liveNonBossEnemies, int targetCount) {
		if (liveNonBossEnemies < 0 || targetCount < 0) {
			throw new IllegalArgumentException(
					"enemy counts must not be negative");
		}
		return liveNonBossEnemies >= targetCount;
	}

	static boolean needsVisibleContact(
			BukovRaidMode mode,
			int raidOrdinal,
			int liveNonBossEnemies) {
		if (mode == null || raidOrdinal <= 0 || liveNonBossEnemies < 0) {
			throw new IllegalArgumentException(
					"mode, raid ordinal, and live count are required");
		}
		return liveNonBossEnemies == 0
				&& (mode.trainingGround() || raidOrdinal == 1);
	}

	private static void validate(
			int liveNonBossEnemies,
			int killCount,
			int targetCount) {
		if (liveNonBossEnemies < 0 || killCount < 0 || targetCount < 0) {
			throw new IllegalArgumentException(
					"enemy counts must not be negative");
		}
	}

	private InitialEnemyRosterPolicy() {
	}
}
