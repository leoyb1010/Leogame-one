package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.watabou.noosa.Game;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Real-level P0 seed gate for the complete first-raid objective surface.
 *
 * Local CI defaults to 500 seeds. Nightly invokes the same structure with
 * -Dbukov.firstRaidSeedCount=10000; no alternate fast-path or synthetic room
 * graph is used.
 */
public class BukovFirstRaidCriticalPathSeedGateTest {

	private static final int LOCAL_SEEDS = 500;
	private static final int MAX_NIGHTLY_SEEDS = 10_000;

	private int previousDepth;
	private int previousBranch;
	private long previousSeed;
	private String previousVersion;
	private BukovRaidMode previousMode;

	@Before
	public void captureGlobals() {
		previousDepth = Dungeon.depth;
		previousBranch = Dungeon.branch;
		previousSeed = Dungeon.seed;
		previousVersion = Game.version;
		previousMode = BukovMode.raidMode();
		if (Game.version == null) Game.version = "test";
		BukovMode.prepareRaidMode(BukovRaidMode.EXPEDITION);
	}

	@After
	public void restoreGlobals() {
		Dungeon.depth = previousDepth;
		Dungeon.branch = previousBranch;
		Dungeon.seed = previousSeed;
		Game.version = previousVersion;
		BukovMode.prepareRaidMode(previousMode);
	}

	@Test
	public void realFirstRaidSeedsPreserveObjectiveAndPickupContract() {
		int seedCount = seedCount();
		for (int index = 0; index < seedCount; index++) {
			long seed = seed(index);
			Dungeon.depth = 1;
			Dungeon.branch = 0;
			Dungeon.seed = seed;

			BukovLevel level = new BukovLevel();
			level.create();
			BukovRaidLayout layout = level.raidLayout();
			BukovRaidLayout.MissionGate gate = layout.missionGate();
			assertNotNull("seed=" + seed + " mission gate", gate);

			// This production validator performs terrain flood fills twice:
			// archive reachable with every G01 cell closed, then >=8 genuinely
			// new non-gate cells reachable after those exact cells open.
			BukovAnchorPlanner.Result traversal =
					BukovAnchorPlanner.validateLockedMissionTraversal(
							level.width(),
							level.height(),
							level.map,
							layout,
							level.entrance());
			assertTrue(
					"seed=" + seed + " " + traversal.reason,
					traversal.valid);

			BukovRaidLayout.LootAnchor highValue =
					level.missionHighValueAnchor();
			assertNotNull("seed=" + seed + " high-value point", highValue);
			ExtractionDefinition baseline = layout.extraction("E01");
			assertNotNull("seed=" + seed + " E01", baseline);
			int maintenance = level.semanticCell("scrap_compactor");
			assertTrue("seed=" + seed + " maintenance cache", maintenance >= 0);

			List<BukovLooseLootPlanner.Placement> loose =
					BukovLooseLootPlanner.plan(
							level.width(),
							level.height(),
							level.passable,
							layout,
							level.entrance(),
							BukovRaidMode.EXPEDITION,
							maintenance);
			assertEquals(
					"seed=" + seed + " ground pickup count",
					BukovLooseLootPlanner.REQUIRED_PLACEMENT_COUNT,
					loose.size());

			Set<Integer> occupied = new HashSet<>();
			assertUnique(occupied, gate.archiveCell, seed, "archive");
			for (int gateCell : gate.gateCells) {
				assertUnique(occupied, gateCell, seed, "gate");
			}
			for (BukovRaidLayout.LootAnchor anchor : layout.lootAnchors) {
				assertUnique(
						occupied,
						anchor.cell,
						seed,
						"loot " + anchor.id);
			}
			for (ExtractionDefinition extraction : layout.extractions) {
				assertUnique(
						occupied,
						extraction.interactionCell,
						seed,
						"extraction " + extraction.id);
			}
			for (BukovLooseLootPlanner.Placement placement : loose) {
				assertUnique(
						occupied,
						placement.cell,
						seed,
						"ground " + placement.kind);
			}
			assertUnique(
					occupied,
					maintenance,
					seed,
					"maintenance cache");
			assertTrue(
					"seed=" + seed + " high-value anchor contract",
					occupied.contains(highValue.cell));
			assertTrue(
					"seed=" + seed + " E01 contract",
					occupied.contains(baseline.interactionCell));
		}
	}

	private static int seedCount() {
		String environment = System.getenv(
				"BUKOV_FIRST_RAID_SEED_COUNT");
		int requested = environment == null || environment.trim().isEmpty()
				? Integer.getInteger(
						"bukov.firstRaidSeedCount",
						LOCAL_SEEDS)
				: Integer.parseInt(environment.trim());
		if (requested <= 0 || requested > MAX_NIGHTLY_SEEDS) {
			throw new IllegalArgumentException(
					"bukov.firstRaidSeedCount must be 1..10000");
		}
		return requested;
	}

	private static long seed(int index) {
		long value = 0x9E3779B97F4A7C15L * (index + 1L);
		value ^= value >>> 30;
		value *= 0xBF58476D1CE4E5B9L;
		value ^= value >>> 27;
		value *= 0x94D049BB133111EBL;
		return value ^ value >>> 31;
	}

	private static void assertUnique(
			Set<Integer> occupied,
			int cell,
			long seed,
			String label) {
		assertTrue(
				"seed=" + seed + " conflicting " + label
						+ " cell=" + cell,
				cell >= 0 && occupied.add(cell));
	}
}
