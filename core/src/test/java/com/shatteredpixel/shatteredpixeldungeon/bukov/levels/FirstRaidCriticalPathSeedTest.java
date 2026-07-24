package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.watabou.noosa.Game;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FirstRaidCriticalPathSeedTest {

	private static final long[] SEEDS = {
			1L, 7L, 42L, 256L, 4096L, 94823742L,
			987654321L, -42L
	};

	private int previousDepth;
	private int previousBranch;
	private long previousSeed;
	private String previousVersion;
	private BukovRaidMode previousRaidMode;

	@Before
	public void captureGlobals() {
		previousDepth = Dungeon.depth;
		previousBranch = Dungeon.branch;
		previousSeed = Dungeon.seed;
		previousVersion = Game.version;
		previousRaidMode = BukovMode.raidMode();
		if (Game.version == null) Game.version = "test";
		BukovMode.prepareRaidMode(BukovRaidMode.EXPEDITION);
	}

	@After
	public void restoreGlobals() {
		Dungeon.depth = previousDepth;
		Dungeon.branch = previousBranch;
		Dungeon.seed = previousSeed;
		Game.version = previousVersion;
		BukovMode.prepareRaidMode(previousRaidMode);
	}

	@Test
	public void everySeedHasSuppliesArchiveGateHighValueCacheAndExtraction() {
		for (long seed : SEEDS) {
			Dungeon.depth = 1;
			Dungeon.branch = 0;
			Dungeon.seed = seed;

			BukovLevel level = new BukovLevel();
			level.create();
			BukovRaidLayout layout = level.raidLayout();
			assertNotNull("seed=" + seed, layout);

			assertEquals(
					"seed=" + seed,
					BukovLooseLootPlanner.REQUIRED_PLACEMENT_COUNT,
					level.heaps.size);
			assertNotNull(
					"seed=" + seed,
					level.missionGate());
			assertTrue(
					"seed=" + seed,
					level.missionGate().archiveCell >= 0);
			assertTrue(
					"seed=" + seed,
					level.missionGate().gateCells.length > 0);

			BukovRaidLayout.LootAnchor critical =
					level.missionHighValueAnchor();
			assertNotNull("seed=" + seed, critical);
			assertEquals(
					"seed=" + seed,
					FirstRaidMission.HIGH_VALUE_LOOT_TABLE_ID,
					critical.lootTableId);
			assertEquals(
					"seed=" + seed,
					BukovRaidLayout.Zone.HIGH_VALUE,
					layout.mark(critical.roomId).zone);

			assertNotNull("seed=" + seed, layout.extraction("E01"));
			assertFalse(
					"seed=" + seed,
					containsDuplicateCriticalCell(level));
			assertTrue(
					"seed=" + seed,
					BukovAnchorPlanner.validateLockedMissionTraversal(
							level.width(),
							level.height(),
							level.map,
							layout,
							level.entrance()).valid);
		}
	}

	private static boolean containsDuplicateCriticalCell(BukovLevel level) {
		Set<Integer> cells = new HashSet<>();
		BukovRaidLayout layout = level.raidLayout();
		if (!cells.add(level.entrance())) return true;
		if (!cells.add(layout.missionGate().archiveCell)) return true;
		for (int gateCell : layout.missionGate().gateCells) {
			if (!cells.add(gateCell)) return true;
		}
		for (BukovRaidLayout.LootAnchor anchor : layout.lootAnchors) {
			if (!cells.add(anchor.cell)) return true;
		}
		for (ExtractionDefinition extraction : layout.extractions) {
			if (!cells.add(extraction.interactionCell)) return true;
		}
		return false;
	}
}
