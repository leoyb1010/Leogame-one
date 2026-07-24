package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.BukovEnemySpawnPlanner;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoStack;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovEconomicItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovLootItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.watabou.noosa.Game;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BukovLooseLootPlannerTest {

	private static final long[] SEEDS = {
			1L, 7L, 42L, 256L, 4096L, 94823742L, 987654321L, -42L
	};

	private int previousDepth;
	private int previousBranch;
	private long previousSeed;
	private String previousVersion;
	private BukovRaidMode previousRaidMode;
	private boolean previousGroundStarterKitRequired;

	@Before
	public void captureGlobals() {
		previousDepth = Dungeon.depth;
		previousBranch = Dungeon.branch;
		previousSeed = Dungeon.seed;
		previousVersion = Game.version;
		previousRaidMode = BukovMode.raidMode();
		previousGroundStarterKitRequired =
				BukovMode.groundStarterKitRequired();
		BukovMode.prepareGroundStarterKit(true);
		if (Game.version == null) Game.version = "test";
	}

	@After
	public void restoreGlobals() {
		Dungeon.depth = previousDepth;
		Dungeon.branch = previousBranch;
		Dungeon.seed = previousSeed;
		Game.version = previousVersion;
		BukovMode.prepareRaidMode(previousRaidMode);
		BukovMode.prepareGroundStarterKit(
				previousGroundStarterKitRequired);
	}

	@Test
	public void unarmedFirstRaidAuthorsVisibleCombatPairInsideSpawnRoom() {
		BukovMode.prepareRaidMode(BukovRaidMode.EXPEDITION);
		for (long seed : SEEDS) {
			Dungeon.depth = 1;
			Dungeon.branch = 0;
			Dungeon.seed = seed;

			BukovLevel level = new BukovLevel();
			level.create();
			List<BukovLooseLootPlanner.Placement> placements =
					BukovLooseLootPlanner.plan(
							level.width(),
							level.height(),
							level.passable,
							level.raidLayout(),
							level.entrance(),
							BukovRaidMode.EXPEDITION,
							level.semanticCell("scrap_compactor"));
			List<BukovLooseLootPlanner.Placement> repeated =
					BukovLooseLootPlanner.plan(
							level.width(),
							level.height(),
							level.passable,
							level.raidLayout(),
							level.entrance(),
							BukovRaidMode.EXPEDITION,
							level.semanticCell("scrap_compactor"));

			assertEquals("seed=" + seed,
					BukovLooseLootPlanner.REQUIRED_PLACEMENT_COUNT,
					placements.size());
			assertSamePlacements(placements, repeated);
			Set<Integer> cells = new HashSet<>();
			boolean foundWeapon = false;
			boolean foundAmmo = false;
			boolean foundReserveAmmo = false;
			boolean foundMedical = false;
			boolean foundSalvage = false;
			String deploymentRoomId = deploymentRoomId(level);
			for (BukovLooseLootPlanner.Placement placement : placements) {
				assertTrue("seed=" + seed,
						cells.add(placement.cell));
				BukovRaidLayout.Mark mark =
						level.raidLayout().mark(placement.roomId);
				assertNotNull(mark);
				boolean starter = placement.kind
						== BukovLooseLootPlanner.Kind.WEAPON
						|| placement.kind
								== BukovLooseLootPlanner.Kind.AMMUNITION
						|| placement.kind
								== BukovLooseLootPlanner.Kind
										.RESERVE_AMMUNITION;
				if (starter) {
					assertEquals(
							BukovRaidLayout.Zone.SPAWN,
							mark.zone);
					assertEquals(deploymentRoomId, placement.roomId);
					assertTrue(placement.distanceFromDeployment > 0);
					assertTrue(
							placement.distanceFromDeployment
									<= BukovLooseLootPlanner
											.GROUND_STARTER_RADIUS);
				} else {
					assertTrue(
							placement.distanceFromDeployment
									>= BukovLooseLootPlanner
											.MINIMUM_DISTANCE_FROM_DEPLOYMENT);
					assertFalse(
							mark.zone == BukovRaidLayout.Zone.SPAWN);
				}
				if (placement.kind == BukovLooseLootPlanner.Kind.MEDICAL
						|| placement.kind
								== BukovLooseLootPlanner.Kind.SALVAGE) {
					assertFalse(mark.structuralTransit);
					assertFalse(
							mark.zone == BukovRaidLayout.Zone.TRANSIT);
				}

				Heap heap = level.heaps.get(placement.cell);
				assertNotNull("seed=" + seed, heap);
				assertEquals(Heap.Type.HEAP, heap.type);
				assertTrue("seed=" + seed, heap.seen);
				assertFalse("seed=" + seed, heap.hidden);
				assertNotNull("seed=" + seed, heap.peek());
				switch (placement.kind) {
					case WEAPON:
						foundWeapon = heap.peek() instanceof BukovEconomicItem
								&& "firearm:needle_9".equals(
										((BukovEconomicItem) heap.peek())
												.bukovDefinitionId());
						break;
					case AMMUNITION:
						foundAmmo = heap.peek() instanceof AmmoStack
								&& "ammo_9_training".equals(
										((AmmoStack) heap.peek())
												.definitionId())
								&& heap.peek().quantity() == 18;
						break;
					case RESERVE_AMMUNITION:
						foundReserveAmmo = heap.peek() instanceof AmmoStack
								&& "ammo_9_standard".equals(
										((AmmoStack) heap.peek())
												.definitionId())
								&& heap.peek().quantity() == 18;
						break;
					case MEDICAL:
						foundMedical = heap.peek() instanceof BukovLootItem
								&& ((BukovLootItem) heap.peek()).category()
										== BukovLootItem.Category.MEDICAL;
						break;
					case SALVAGE:
						foundSalvage = heap.peek() instanceof BukovLootItem
								&& ((BukovLootItem) heap.peek()).category()
										== BukovLootItem.Category.TOOL;
						break;
				}
			}
			assertTrue("seed=" + seed, foundWeapon);
			assertTrue("seed=" + seed, foundAmmo);
			assertTrue("seed=" + seed, foundReserveAmmo);
			assertTrue("seed=" + seed, foundMedical);
			assertTrue("seed=" + seed, foundSalvage);
		}
	}

	@Test
	public void completeCombatLoadoutDoesNotReceiveDuplicateGroundPair() {
		BukovMode.prepareRaidMode(BukovRaidMode.EXPEDITION);
		Dungeon.depth = 1;
		Dungeon.branch = 0;
		Dungeon.seed = 94823742L;

		BukovLevel level = new BukovLevel();
		level.create();
		List<BukovLooseLootPlanner.Placement> placements =
				BukovLooseLootPlanner.plan(
						level.width(),
						level.height(),
						level.passable,
						level.raidLayout(),
						level.entrance(),
						BukovRaidMode.EXPEDITION,
						level.semanticCell("scrap_compactor"),
						false);

		assertEquals(
				BukovLooseLootPlanner.BASE_PLACEMENT_COUNT,
				placements.size());
		for (BukovLooseLootPlanner.Placement placement : placements) {
			assertTrue(placement.kind
					== BukovLooseLootPlanner.Kind.MEDICAL
					|| placement.kind
							== BukovLooseLootPlanner.Kind.SALVAGE);
		}
	}

	@Test
	public void trainingGroundKeepsAllSuppliesAndLiveTargetsNearDeployment() {
		BukovMode.prepareRaidMode(BukovRaidMode.TRAINING_GROUND);
		Dungeon.depth = 1;
		Dungeon.branch = 0;
		Dungeon.seed = 94823742L;

		BukovLevel level = new BukovLevel();
		level.create();
		List<BukovLooseLootPlanner.Placement> placements =
				BukovLooseLootPlanner.plan(
						level.width(),
						level.height(),
						level.passable,
						level.raidLayout(),
						level.entrance(),
						BukovRaidMode.TRAINING_GROUND,
						level.semanticCell("scrap_compactor"));

		assertEquals(18, level.raidMode().standardRoomBudget);
		assertEquals("cold_storage", level.raidLayout().themeId);
		assertEquals(5, placements.size());
		for (BukovLooseLootPlanner.Placement placement : placements) {
			assertTrue(placement.distanceFromDeployment
					<= BukovLooseLootPlanner.TRAINING_INTRODUCTION_RADIUS);
		}

		int eligibleTargetCells = 0;
		for (BukovEnemySpawnPlanner.SpawnPoint point :
				level.enemySpawnPoints()) {
			if (!point.bossArena
					&& point.distanceFromSpawnRooms >= 3) {
				eligibleTargetCells++;
			}
		}
		assertTrue(eligibleTargetCells
				>= BukovRaidMode.TRAINING_GROUND.initialEnemyCount);
	}

	private static String deploymentRoomId(BukovLevel level) {
		int entrance = level.entrance();
		int x = entrance % level.width();
		int y = entrance / level.width();
		for (BukovRaidLayout.Mark mark : level.raidLayout().marks) {
			if (x >= mark.left && x <= mark.right
					&& y >= mark.top && y <= mark.bottom) {
				return mark.roomId();
			}
		}
		throw new AssertionError("deployment room missing");
	}

	private static void assertSamePlacements(
			List<BukovLooseLootPlanner.Placement> first,
			List<BukovLooseLootPlanner.Placement> second) {
		assertEquals(first.size(), second.size());
		for (int index = 0; index < first.size(); index++) {
			BukovLooseLootPlanner.Placement expected = first.get(index);
			BukovLooseLootPlanner.Placement actual = second.get(index);
			assertEquals(expected.kind, actual.kind);
			assertEquals(expected.cell, actual.cell);
			assertEquals(expected.distanceFromDeployment,
					actual.distanceFromDeployment);
			assertEquals(expected.roomId, actual.roomId);
		}
	}
}
