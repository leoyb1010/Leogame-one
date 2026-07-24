/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyArchetypeDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;

/**
 * Shared production contract for the White Line boss spawn.
 *
 * Keeping eligibility and point safety outside the scene mutation lets the
 * acceptance matrix exercise exactly the policy used by the live world.
 */
public final class WhiteLineSpawnPolicy {

	public static boolean eligible(
			BukovRaidMode mode,
			EnemyArchetypeDefinition boss,
			int raidOrdinal,
			float elapsedSeconds,
			boolean firstRaidProtectionActive,
			boolean resolved,
			int activeBossCount) {
		if (mode == null || boss == null) {
			throw new IllegalArgumentException("mode and boss are required");
		}
		if (raidOrdinal <= 0) {
			throw new IllegalArgumentException("raidOrdinal must be positive");
		}
		if (!BukovNumbers.isFinite(elapsedSeconds)
				|| elapsedSeconds < 0f
				|| activeBossCount < 0) {
			throw new IllegalArgumentException(
					"elapsedSeconds and activeBossCount are invalid");
		}
		if (!mode.bossEnabled
				|| resolved
				|| firstRaidProtectionActive
				|| activeBossCount > 0) {
			return false;
		}
		return elapsedSeconds >= earliestSeconds(mode, boss, raidOrdinal);
	}

	public static float earliestSeconds(
			BukovRaidMode mode,
			EnemyArchetypeDefinition boss,
			int raidOrdinal) {
		if (mode == null || boss == null || raidOrdinal <= 0) {
			throw new IllegalArgumentException(
					"mode, boss, and positive raidOrdinal are required");
		}
		float earliest = mode.bossEarliestSeconds;
		if (raidOrdinal == 1 && mode != BukovRaidMode.BOSS_CONTRACT) {
			earliest = Math.max(
					earliest,
					boss.firstRaidMinimumSeconds);
		}
		return earliest;
	}

	public static boolean acceptsSpawnPoint(
			boolean bossArena,
			int cell,
			int levelLength,
			boolean insidePlayerFieldOfView,
			boolean occupied,
			boolean tooCloseToHero) {
		return bossArena
				&& cell >= 0
				&& cell < levelLength
				&& !insidePlayerFieldOfView
				&& !occupied
				&& !tooCloseToHero;
	}

	private WhiteLineSpawnPolicy() {
	}
}
