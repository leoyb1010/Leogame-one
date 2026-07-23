/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.noosa.Game;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * Builds real BukovLevel terrain rather than testing a synthetic room graph.
 * Each accepted seed must leave Q01 and a useful raid area reachable while
 * G01 is closed, then expose additional terrain after the gate opens.
 */
public class BukovLevelTraversalSeedTest {

	private static final long[] SEEDS = {
			1L, 2L, 3L, 7L, 11L, 42L, 99L, 256L,
			1024L, 4096L, 94823742L, 117013337L,
			314159265L, 987654321L, 2147483647L,
			-1L, -42L, Long.MAX_VALUE
	};

	private int previousDepth;
	private int previousBranch;
	private long previousSeed;
	private String previousVersion;

	@Before
	public void captureGlobals() {
		previousDepth = Dungeon.depth;
		previousBranch = Dungeon.branch;
		previousSeed = Dungeon.seed;
		previousVersion = Game.version;
		if (Game.version == null) Game.version = "test";
	}

	@After
	public void restoreGlobals() {
		Dungeon.depth = previousDepth;
		Dungeon.branch = previousBranch;
		Dungeon.seed = previousSeed;
		Game.version = previousVersion;
	}

	@Test
	public void realBukovSeedsHaveLargeConnectedPreGatePlayArea() {
		for (long seed : SEEDS) {
			Dungeon.depth = 1;
			Dungeon.branch = 0;
			Dungeon.seed = seed;

			BukovLevel level = new BukovLevel();
			level.create();
			BukovRaidLayout layout = level.raidLayout();
			BukovRaidLayout.BossMechanism boss =
					layout.bossMechanism();
			assertNotNull("seed=" + seed + " boss anchors", boss);
			assertTrue("seed=" + seed + " fog lamp must stay walkable",
					passable(level.map[boss.fogLampCell]));
			for (int bodyCell : boss.bodyTraceCells) {
				assertTrue("seed=" + seed
								+ " body trace must stay walkable: "
								+ bodyCell,
						passable(level.map[bodyCell]));
			}

			assertTrue("seed=" + seed + " playable rooms="
							+ layout.playableRoomCount(),
					layout.playableRoomCount() >= 26
							&& layout.playableRoomCount() <= 34);
			BukovAnchorPlanner.Result traversal =
					BukovAnchorPlanner.validateLockedMissionTraversal(
							level.width(),
							level.height(),
							level.map,
							layout,
							level.entrance());
			assertTrue("seed=" + seed + " " + traversal.reason,
					traversal.valid);

			for (int gateCell : layout.missionGate().gateCells) {
				level.map[gateCell] = Terrain.LOCKED_DOOR;
			}
			BukovAnchorPlanner.Result restoredLockedTraversal =
					BukovAnchorPlanner.validateLockedMissionTraversal(
							level.width(),
							level.height(),
							level.map,
							layout,
							level.entrance());
			assertTrue("locked save seed=" + seed + " "
							+ restoredLockedTraversal.reason,
					restoredLockedTraversal.valid);
		}
	}

	private static boolean passable(int terrain) {
		return terrain >= 0
				&& terrain < Terrain.flags.length
				&& (Terrain.flags[terrain] & Terrain.PASSABLE) != 0;
	}
}
