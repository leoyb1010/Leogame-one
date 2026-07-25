package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.FirstRaidEnemySpawnDirector;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovFirstRaidLootTables;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovLevel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovRaidLayout;
import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovContainerDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovHeapLootAdapter;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCoordinator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidWorldDefinitions;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovSearchableContainer;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.ExtractionState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.LootTransaction;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidOutcome;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidResult;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovHubController;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovHubViewModel;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.badlogic.gdx.Preferences;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameSettings;
import com.watabou.utils.SparseArray;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Headless production-component acceptance for the complete player journey.
 *
 * Unlike wiring guards, this drives real generated levels, authored content,
 * inventory UIDs, ballistics, mission state, extraction and settlement APIs.
 */
public class BukovPlayerJourneyAcceptanceTest {

	private int previousDepth;
	private int previousBranch;
	private long previousSeed;
	private String previousVersion;
	private Level previousLevel;
	private Hero previousHero;
	private BukovRaidMode previousMode;
	private List<String> previousMaps;
	private String previousSelectedMap;
	private SparseArray<ArrayList<Item>> previousDroppedItems;
	private Object[] previousBadgesState;
	private HeadlessStatisticsState previousStatistics;

	@Before
	public void captureGlobals() {
		Actor.clear();
		previousDepth = Dungeon.depth;
		previousBranch = Dungeon.branch;
		previousSeed = Dungeon.seed;
		previousVersion = Game.version;
		previousLevel = Dungeon.level;
		previousHero = Dungeon.hero;
		previousDroppedItems = Dungeon.droppedItems;
		previousMode = BukovMode.raidMode();
		previousMaps = new ArrayList<>(BukovMode.unlockedRaidThemes());
		previousSelectedMap = BukovMode.selectedRaidTheme();
		GameSettings.set(new MemoryPreferences());
		if (Game.version == null) Game.version = "test";
		previousBadgesState = replaceBadgesForHeadlessTest();
		previousStatistics = replaceStatisticsForHeadlessTest();
		Dungeon.droppedItems = new SparseArray<>();
	}

	@After
	public void restoreGlobals() {
		Actor.clear();
		Dungeon.depth = previousDepth;
		Dungeon.branch = previousBranch;
		Dungeon.seed = previousSeed;
		Dungeon.level = previousLevel;
		Dungeon.hero = previousHero;
		Dungeon.droppedItems = previousDroppedItems;
		restoreBadgesAfterHeadlessTest(previousBadgesState);
		restoreStatisticsAfterHeadlessTest(previousStatistics);
		Dungeon.quickslot.reset();
		Game.version = previousVersion;
		BukovMode.prepareRaidMode(previousMode);
		BukovMode.prepareUnlockedMaps(previousMaps);
		BukovMode.prepareSelectedMap(previousSelectedMap);
		GameSettings.set(null);
	}

	static Object[] replaceBadgesForHeadlessTest() {
		try {
			Field global = Badges.class.getDeclaredField("global");
			Field local = Badges.class.getDeclaredField("local");
			Field saveNeeded = Badges.class.getDeclaredField("saveNeeded");
			global.setAccessible(true);
			local.setAccessible(true);
			saveNeeded.setAccessible(true);
			Object[] previous = {
					global.get(null),
					local.get(null),
					saveNeeded.get(null)
			};
			global.set(null, new HashSet<Badges.Badge>());
			local.set(null, new HashSet<Badges.Badge>());
			saveNeeded.set(null, false);
			return previous;
		} catch (ReflectiveOperationException exception) {
			throw new AssertionError(
					"could not initialize headless badge state",
					exception);
		}
	}

	static void restoreBadgesAfterHeadlessTest(Object[] previous) {
		try {
			Field global = Badges.class.getDeclaredField("global");
			Field local = Badges.class.getDeclaredField("local");
			Field saveNeeded = Badges.class.getDeclaredField("saveNeeded");
			global.setAccessible(true);
			local.setAccessible(true);
			saveNeeded.setAccessible(true);
			global.set(null, previous[0]);
			local.set(null, previous[1]);
			saveNeeded.set(null, previous[2]);
		} catch (ReflectiveOperationException exception) {
			throw new AssertionError(
					"could not restore headless badge state",
					exception);
		}
	}

	static HeadlessStatisticsState replaceStatisticsForHeadlessTest() {
		Bundle previous = new Bundle();
		Statistics.storeInBundle(previous);
		HeadlessStatisticsState state = new HeadlessStatisticsState(
				previous,
				Statistics.completedWithNoKilling);
		resetStatisticsForHeadlessSample();
		return state;
	}

	static void resetStatisticsForHeadlessSample() {
		Statistics.reset();
		Statistics.completedWithNoKilling = false;
	}

	static void restoreStatisticsAfterHeadlessTest(
			HeadlessStatisticsState previous) {
		Statistics.restoreFromBundle(previous.bundle);
		Statistics.completedWithNoKilling =
				previous.completedWithNoKilling;
	}

	static final class HeadlessStatisticsState {
		final Bundle bundle;
		final boolean completedWithNoKilling;

		HeadlessStatisticsState(
				Bundle bundle,
				boolean completedWithNoKilling) {
			this.bundle = bundle;
			this.completedWithNoKilling = completedWithNoKilling;
		}
	}

	@Test
	public void freshProfileCompletesArchiveGateCombatE02AndReturnsToHub()
			throws Exception {
		InMemoryBukovSaveService saves =
				new InMemoryBukovSaveService();
		BukovHubController hub = new BukovHubController(saves);

		assertTrue(hub.viewModel().canDeploy);
		hub.clearLoadout();
		assertFalse(hub.viewModel().canDeploy);
		hub.prepareAndConfirmDeployment();
		assertTrue(hub.viewModel().canDeploy);
		Set<String> deployedUids = new HashSet<>(
				saves.loadProfile().loadout().selectedUids());
		assertFalse(deployedUids.isEmpty());

		long seed = 0xB0C0F001L;
		BukovLevel level = buildLevel(seed);
		BukovRaidCoordinator raid = startRaid(
				saves, level, seed, "journey-e02");
		assertTrue(raid.firstRaidMissionActive());
		assertEquals(
				FirstRaidMission.Stage.RECOVER_ARCHIVE,
				raid.firstRaidStage());
		assertEquals(0, saves.loadProfile().loadout().distinctItemCount());
		assertTrue(hasDefinitionPrefix(raid, "firearm:"));
		assertTrue(hasDefinitionPrefix(raid, "ammo:"));
		for (String deployedUid : deployedUids) {
			assertFalse(saves.loadProfile().stash().contains(deployedUid));
			assertTrue(raid.loot().contains(deployedUid));
		}
		raid.setExtractionCondition(
				FirstRaidMission.CONDITIONAL_EXTRACTION_ID,
				true);
		assertFalse(raid.beginExtraction(
				FirstRaidMission.CONDITIONAL_EXTRACTION_ID));

		assertPlayerShotKillsOnGeneratedMap(raid, level);
		assertEquals(1, raid.session().killCount());

		BukovRaidLayout.MissionGate gate = level.missionGate();
		assertNotNull(gate);
		MissionGateTerrain.apply(level, gate.gateCells, false, null);
		LevelCollisionMap collision = new LevelCollisionMap(level);
		assertGateBlocked(collision, level.width(), gate.gateCells, true);

		BukovHeapLootAdapter loot = new BukovHeapLootAdapter(raid);
		BukovRaidCoordinator.ContainerSnapshot archive =
				raid.container(FirstRaidMission.ARCHIVE_CONTAINER_ID);
		assertNotNull(archive);
		searchAndRelease(raid, archive, loot);
		String archiveUid = raid.loot().firstItemUidForDefinition(
				FirstRaidMission.ARCHIVE_DEFINITION_ID);
		assertNotNull(archiveUid);
		assertEquals(
				BukovHeapLootAdapter.DropResult.PROTECTED_ITEM,
				loot.drop(archiveUid, new Heap()));

		assertTrue(raid.completeEvent(FirstRaidMission.EVENT_ID));
		assertEquals(
				FirstRaidMission.Stage.SECURE_HIGH_VALUE_CACHE,
				raid.firstRaidStage());
		MissionGateTerrain.apply(level, gate.gateCells, true, null);
		assertGateBlocked(collision, level.width(), gate.gateCells, false);

		raid.saveCheckpoint();
		raid = BukovRaidCoordinator.resume(saves);
		assertNotNull(raid);
		assertTrue(raid.eventCompleted(FirstRaidMission.EVENT_ID));
		assertEquals(
				FirstRaidMission.Stage.SECURE_HIGH_VALUE_CACHE,
				raid.firstRaidStage());
		assertTrue(raid.loot().contains(archiveUid));
		assertTrue(raid.extraction(
				FirstRaidMission.CONDITIONAL_EXTRACTION_ID)
				.conditionMet());
		for (String deployedUid : deployedUids) {
			assertTrue(raid.loot().contains(deployedUid));
		}
		loot = new BukovHeapLootAdapter(raid);

		BukovRaidCoordinator.ContainerSnapshot highValue =
				containerForTable(
						raid,
						FirstRaidMission.HIGH_VALUE_LOOT_TABLE_ID);
		assertNotNull(highValue);
		assertEquals(
				BukovSearchableContainer.State.UNSEARCHED,
				highValue.state);
		searchAndRelease(raid, highValue, loot);
		assertTrue(raid.firstRaidMissionCompleted());
		assertEquals(
				FirstRaidMission.Stage.EXTRACT,
				raid.firstRaidStage());

		completeExtraction(
				raid,
				FirstRaidMission.CONDITIONAL_EXTRACTION_ID);
		RaidResult result = raid.settleSuccess();

		assertEquals(RaidOutcome.SUCCESS, result.outcome());
		assertTrue(result.missionCompleted());
		assertEquals(1, result.kills());
		assertTrue(result.transferredUids().contains(archiveUid));
		assertTrue(result.transferredUids().containsAll(deployedUids));
		assertNull(saves.loadRaidCheckpoint());
		assertTrue(saves.loadProfile().completedContracts().contains(
				FirstRaidMission.EVENT_ID));
		for (String deployedUid : deployedUids) {
			assertTrue(saves.loadProfile().stash().contains(deployedUid));
		}

		BukovHubViewModel returned =
				new BukovHubController(saves).viewModel();
		assertFalse(returned.activeRaid);
		assertNotNull(returned.latestSettlement);
		assertEquals(
				RaidOutcome.SUCCESS,
				returned.latestSettlement.outcome);
		assertTrue(returned.latestSettlement.missionCompleted);
		assertEquals(1, returned.latestSettlement.kills);
	}

	@Test
	public void realWorldBackpackPausesAndResumesWithoutCatchUp()
			throws Exception {
		InMemoryBukovSaveService saves =
				new InMemoryBukovSaveService();
		BukovHubController hub = new BukovHubController(saves);
		hub.prepareAndConfirmDeployment();

		long seed = 0xBACCAC01L;
		BukovLevel level = buildLevel(seed);
		BukovRaidCoordinator raid = startRaid(
				saves,
				level,
				seed,
				"journey-backpack-pause");
		BukovRealtimeCombatHarness.Result result =
				BukovRealtimeCombatHarness
						.verifyBackpackPauseAgainstGeneratedEnemy(
								raid,
								level);
		BukovRealtimeCombatHarness.BackpackPauseEvidence pause =
				result.backpackPause;

		assertNotNull(result.toString(), pause);
		assertTrue(
				"pause probe must begin after production enemy AI is active",
				pause.damageBeforePause > 0);
		assertEquals(
				"the paused second must not advance raid time",
				0f,
				pause.pausedElapsedDelta,
				0f);
		assertEquals(
				"enemy attacks must not damage the operator behind the backpack",
				0,
				pause.pausedHealthDelta);
		assertEquals(
				"held fire must not consume ammunition behind the backpack",
				0,
				pause.pausedMagazineDelta);
		assertEquals(
				"enemy position must not advance while the real World is paused",
				0f,
				pause.pausedEnemyMovementSquared,
				0f);
		assertEquals(
				"enemy AI must not resolve hidden damage while paused",
				0,
				pause.pausedDamageDelta);

		assertEquals(
				"resume must simulate only the new render frame, not the paused second",
				1f / 60f,
				pause.resumedElapsedDelta,
				0.00001f);
		assertEquals(
				"held fire across backpack close must not leak a deferred shot",
				0,
				pause.resumedMagazineDelta);
		assertTrue(
				"release plus a fresh press must resume the production fire loop",
				pause.rearmedMagazineDelta < 0);
		assertTrue(
				"enemy AI must resume after the backpack closes",
				pause.resumedDamageDelta > 0);
	}

	@Test
	public void productionWorldCompletesFirstRaidAndRedeploys()
			throws Exception {
		InMemoryBukovSaveService saves =
				new InMemoryBukovSaveService();
		BukovHubController hub = new BukovHubController(saves);
		hub.prepareAndConfirmDeployment();
		Set<String> deployedUids = new HashSet<>(
				saves.loadProfile().loadout().selectedUids());
		assertFalse(deployedUids.isEmpty());

		long seed = 0xB0C0F101L;
		BukovLevel level = buildLevel(seed);
		BukovRaidCoordinator raid = startRaid(
				saves,
				level,
				seed,
				"journey-production-world");
		BukovRealtimeCombatHarness.Result live =
				BukovRealtimeCombatHarness
						.completeFirstRaidThroughWorld(raid, level);
		assertRealCombatResult(live);

		BukovRealtimeCombatHarness.FirstRaidJourneyEvidence journey =
				live.firstRaidJourney;
		assertNotNull(live.toString(), journey);
		assertTrue("mission gate must start physically closed",
				journey.gateInitiallyBlocked);
		assertTrue("archive cabinet must expose the production search prompt",
				journey.archiveSearchPrompted);
		assertTrue("archive must enter the real carried-loot ledger",
				journey.archiveCarried);
		assertTrue("carrying the archive must unlock the production gate",
				journey.gateUnlockedByArchive);
		assertTrue("operator must cross the unlocked gate through real movement",
				journey.crossedUnlockedGate);
		assertTrue("high-value cache must expose the production search prompt",
				journey.highValueSearchPrompted);
		assertTrue("searched high-value loot must be picked up through World",
				journey.highValueLootCollected);
		assertTrue("real search and pickup must complete the first mission",
				journey.missionCompleted);
		assertTrue("E01 must expose the production extraction prompt",
				journey.extractionPrompted);
		assertTrue("holding production interact must complete E01",
				journey.extractionCompleted);

		String archiveUid = raid.loot().firstItemUidForDefinition(
				FirstRaidMission.ARCHIVE_DEFINITION_ID);
		assertNotNull(archiveUid);
		RaidResult settlement = raid.settleSuccess();
		assertEquals(RaidOutcome.SUCCESS, settlement.outcome());
		assertTrue(settlement.missionCompleted());
		assertEquals(1, settlement.kills());
		assertTrue(settlement.transferredUids().contains(archiveUid));
		assertTrue(settlement.transferredUids().containsAll(deployedUids));
		assertNull(saves.loadRaidCheckpoint());

		BukovHubController returned = new BukovHubController(saves);
		BukovHubViewModel returnedView = returned.viewModel();
		assertFalse(returnedView.activeRaid);
		assertNotNull(returnedView.latestSettlement);
		assertEquals(
				RaidOutcome.SUCCESS,
				returnedView.latestSettlement.outcome);
		assertTrue(returnedView.latestSettlement.missionCompleted);
		assertEquals(1, returnedView.latestSettlement.kills);
		assertTrue(saves.loadProfile().stash().contains(archiveUid));

		returned.repeatLastLoadout();
		if (!returned.viewModel().canDeploy) {
			returned.prepareAndConfirmDeployment();
		}
		assertTrue("settlement must leave a valid second loadout",
				returned.viewModel().canDeploy);
		returned.confirmDeployment();

		// Reuse the established visible-contact seed so this assertion tests
		// redeployment and production combat, not an unrelated wall-separated
		// spawn that the headless harness cannot autonomously path toward.
		long secondSeed = 0xB0C0F001L;
		BukovLevel secondLevel = buildLevel(secondSeed);
		BukovRaidCoordinator secondRaid = startRaid(
				saves,
				secondLevel,
				secondSeed,
				"journey-production-world-second");
		BukovRealtimeCombatHarness.Result secondContact =
				BukovRealtimeCombatHarness.killOneGeneratedEnemy(
						secondRaid,
						secondLevel);
		assertRealCombatResult(secondContact);
	}

	@Test
	public void e01E02AndE03AllSettleThroughRealMapDefinitions()
			throws Exception {
		String[] extractionIds = {"E01", "E02", "E03"};
		for (int index = 0; index < extractionIds.length; index++) {
			String extractionId = extractionIds[index];
			InMemoryBukovSaveService saves =
					completedFirstContractProfile();
			long seed = 0xE0100000L + index * 977L;
			BukovLevel level = buildLevel(seed);
			String raidId = "journey-" + extractionId.toLowerCase();
			BukovRaidCoordinator raid =
					startRaid(saves, level, seed, raidId);
			RaidItem found = foundItem(
					raidId + "-found",
					"loot:route_evidence");
			assertEquals(
					LootTransaction.PickupResult.ADDED,
					raid.pickup(found));

			ExtractionState extraction = raid.extraction(extractionId);
			assertNotNull(extraction);
			if ("E02".equals(extractionId)) {
				assertFalse(extraction.availableAt(
						raid.session().elapsedSeconds));
				raid.setExtractionCondition(extractionId, true);
			} else if ("E03".equals(extractionId)) {
				assertFalse(extraction.availableAt(
						Math.max(0f, extraction.opensAtSeconds() - 0.01f)));
				raid.tick(
						extraction.opensAtSeconds(),
						ExtractionState.Interaction.NONE);
			}
			assertTrue(extraction.availableAt(
					raid.session().elapsedSeconds));

			completeExtraction(raid, extractionId);
			RaidResult result = raid.settleSuccess();
			assertEquals(RaidOutcome.SUCCESS, result.outcome());
			assertTrue(result.transferredUids().contains(
					found.itemUid()));
			assertNull(saves.loadRaidCheckpoint());
			assertTrue(saves.loadProfile().stash().contains(
					found.itemUid()));

			BukovHubViewModel returned =
					new BukovHubController(saves).viewModel();
			assertNotNull(returned.latestSettlement);
			assertEquals(
					RaidOutcome.SUCCESS,
					returned.latestSettlement.outcome);
			assertEquals(raidId, returned.latestSettlement.raidId);
		}
	}

	@Test
	public void productionWorldDefeatsWhiteLineDropsLootAndCompletesContract()
			throws Exception {
		InMemoryBukovSaveService saves =
				new InMemoryBukovSaveService();
		BukovHubController hub = new BukovHubController(saves);
		hub.selectRaidMode(BukovRaidMode.BOSS_CONTRACT);
		hub.prepareAndConfirmDeployment();

		long seed = 0xB055C0DEL;
		BukovLevel level = buildLevel(
				seed, BukovRaidMode.BOSS_CONTRACT);
		BukovRaidCoordinator raid = startRaid(
				saves,
				level,
				seed,
				"journey-white-line-production");
		assertTrue(raid.bossContractRequired());
		assertFalse(raid.bossContractCompleted());

		BukovWhiteLineProductionHarness.Evidence evidence =
				BukovWhiteLineProductionHarness
						.defeatThroughProductionWorld(raid, level);

		assertTrue(evidence.toString(),
				evidence.initialHealth > 0);
		assertEquals(evidence.toString(), 0, evidence.finalHealth);
		assertTrue(evidence.toString(),
				evidence.playerFireEvents > 0);
		assertEquals(evidence.toString(),
				2, evidence.phaseBreakEvents);
		assertTrue(evidence.toString(), evidence.slamEvents > 0);
		assertTrue(evidence.toString(), evidence.overloadEvents > 0);
		assertEquals(evidence.toString(),
				1, evidence.weakpointKillEvents);
		assertTrue(evidence.toString(),
				evidence.bossLootCount >= 3);
		assertEquals(evidence.toString(), 1, evidence.killCount);
		assertTrue(evidence.toString(), evidence.bodyInactive);
		assertTrue(evidence.toString(), evidence.removedFromLevel);
		assertTrue(evidence.toString(), evidence.levelResolved);
		assertTrue(evidence.toString(), evidence.contractCompleted);
		assertTrue(evidence.toString(), evidence.extractionAvailable);
		assertTrue(evidence.toString(), evidence.extractionPrompted);
		assertTrue(evidence.toString(), evidence.extractionCompleted);

		RaidResult result = raid.settleSuccess();
		assertEquals(RaidOutcome.SUCCESS, result.outcome());
		assertTrue(result.missionCompleted());
		assertEquals(1, result.kills());
		assertTrue(saves.loadProfile()
				.settlement("journey-white-line-production")
				.missionCompleted());
	}

	@Test
	public void secondRaidDeathLosesExactCarriedUidsAndReturnsToHub()
			throws Exception {
		InMemoryBukovSaveService saves =
				completedFirstContractProfile();
		long firstSeed = 0xD3A70001L;
		BukovRaidCoordinator first = startRaid(
				saves,
				buildLevel(firstSeed),
				firstSeed,
				"journey-before-death");
		RaidItem retained = foundItem(
				"journey-retained-loot",
				"loot:retained_evidence");
		assertEquals(
				LootTransaction.PickupResult.ADDED,
				first.pickup(retained));
		completeExtraction(first, "E01");
		assertEquals(
				RaidOutcome.SUCCESS,
				first.settleSuccess().outcome());
		assertTrue(saves.loadProfile().stash().contains(
				retained.itemUid()));

		BukovHubController returned = new BukovHubController(saves);
		returned.repeatLastLoadout();
		if (!returned.viewModel().canDeploy) {
			returned.prepareAndConfirmDeployment();
		}
		assertTrue(returned.viewModel().canDeploy);
		returned.confirmDeployment();

		long secondSeed = 0xD3A70002L;
		BukovRaidCoordinator second = startRaid(
				saves,
				buildLevel(secondSeed),
				secondSeed,
				"journey-death");
		RaidItem fatalRunLoot = foundItem(
				"journey-fatal-loot",
				"loot:fatal_evidence");
		assertEquals(
				LootTransaction.PickupResult.ADDED,
				second.pickup(fatalRunLoot));
		Set<String> carriedBeforeDeath = itemUids(second.loot().items());
		assertFalse(carriedBeforeDeath.isEmpty());
		assertTrue(carriedBeforeDeath.contains(
				fatalRunLoot.itemUid()));

		RaidResult death = second.settleDeath();
		assertEquals(RaidOutcome.DEATH, death.outcome());
		assertEquals(
				carriedBeforeDeath,
				new HashSet<>(death.lostUids()));
		assertNull(saves.loadRaidCheckpoint());
		BukovProfile settled = saves.loadProfile();
		for (String lostUid : carriedBeforeDeath) {
			assertFalse(settled.stash().contains(lostUid));
		}

		BukovHubViewModel afterDeath =
				new BukovHubController(saves).viewModel();
		assertNotNull(afterDeath.latestSettlement);
		assertEquals(
				RaidOutcome.DEATH,
				afterDeath.latestSettlement.outcome);
		assertEquals(
				carriedBeforeDeath,
				new HashSet<>(afterDeath.latestSettlement.itemUids));
		assertTrue(
				"death recovery must leave the next raid playable",
				afterDeath.canDeploy);
	}

	private static BukovLevel buildLevel(long seed) {
		return buildLevel(seed, BukovRaidMode.EXPEDITION);
	}

	private static BukovLevel buildLevel(
			long seed,
			BukovRaidMode mode) {
		BukovMode.prepareRaidMode(mode);
		BukovMode.prepareUnlockedMaps(
				Collections.singletonList("fog_depot"));
		BukovMode.prepareSelectedMap("fog_depot");
		Dungeon.depth = 1;
		Dungeon.branch = 0;
		Dungeon.seed = seed;
		// Level.drop() only creates scene sprites when the generated level is
		// already installed as Dungeon.level. Keep headless generation detached
		// until all deterministic loose loot has been placed.
		Dungeon.level = null;
		BukovLevel level = new BukovLevel();
		level.create();
		Dungeon.level = level;
		return level;
	}

	private static BukovRaidCoordinator startRaid(
			InMemoryBukovSaveService saves,
			BukovLevel level,
			long seed,
			String raidId) throws IOException {
		List<ExtractionState> extractions =
				BukovRaidWorldDefinitions.extractions(level);
		List<BukovContainerDefinition> containers =
				BukovRaidWorldDefinitions.containers(level);
		assertEquals(3, extractions.size());
		return BukovRaidCoordinator.start(
				saves,
				seed,
				raidId,
				BukovHubController.FIRST_RAID_WEIGHT_LIMIT,
				extractions,
				containers);
	}

	private static InMemoryBukovSaveService
			completedFirstContractProfile() throws IOException {
		InMemoryBukovSaveService saves =
				new InMemoryBukovSaveService();
		BukovHubController hub = new BukovHubController(saves);
		hub.prepareAndConfirmDeployment();
		BukovProfile profile = saves.loadProfile();
		profile.completeContract(FirstRaidMission.EVENT_ID);
		saves.saveProfile(profile);
		return saves;
	}

	private static void searchAndRelease(
			BukovRaidCoordinator raid,
			BukovRaidCoordinator.ContainerSnapshot container,
			BukovHeapLootAdapter loot) {
		assertTrue(raid.beginContainerSearch(container.containerId));
		assertEquals(
				BukovSearchableContainer.UpdateResult.COMPLETED,
				raid.updateContainerSearch(
						container.containerId,
						container.searchSeconds,
						true,
						false,
						false,
						BukovFirstRaidLootTables.require(
								container.lootTableId)));
		Heap released = new Heap();
		released.pos = container.cell;
		assertTrue(
				raid.releaseContainerContents(
						container.containerId,
						released) > 0);
		while (released.peek() != null) {
			assertEquals(
					LootTransaction.PickupResult.ADDED,
					loot.pickupTop(released));
		}
	}

	private static void completeExtraction(
			BukovRaidCoordinator raid,
			String extractionId) {
		ExtractionState extraction = raid.extraction(extractionId);
		assertNotNull(extraction);
		assertTrue(raid.beginExtraction(extractionId));
		raid.tick(
				extraction.interactionSeconds(),
				ExtractionState.Interaction.ACTIVE);
		assertTrue(extraction.completed());
	}

	private static BukovRaidCoordinator.ContainerSnapshot
			containerForTable(
					BukovRaidCoordinator raid,
					String lootTableId) {
		for (BukovRaidCoordinator.ContainerSnapshot container :
				raid.containers()) {
			if (lootTableId.equals(container.lootTableId)) {
				return container;
			}
		}
		return null;
	}

	private static void assertGateBlocked(
			LevelCollisionMap collision,
			int width,
			int[] cells,
			boolean expected) {
		assertTrue(cells.length > 0);
		for (int cell : cells) {
			assertEquals(
					"gate cell=" + cell,
					expected,
					collision.blocked(
							cell % width,
							cell / width));
		}
	}

	private static void assertPlayerShotKillsOnGeneratedMap(
			BukovRaidCoordinator raid,
			BukovLevel level) throws IOException {
		BukovRealtimeCombatHarness.Result result =
				BukovRealtimeCombatHarness.killOneGeneratedEnemy(
						raid,
						level);
		assertRealCombatResult(result);
	}

	private static void assertRealCombatResult(
			BukovRealtimeCombatHarness.Result result) {
		assertTrue(result.toString(), result.generatedEnemyCount > 0);
		assertEquals(
				FirstRaidEnemySpawnDirector.FIRST_GUNNER,
				result.targetDefinitionId);
		assertTrue(
				result.toString(),
				result.finalTargetHealth < result.initialTargetHealth);
		assertTrue(result.toString(), result.finalTargetHealth <= 0);
		assertTrue(result.toString(), result.friendlyTracers > 0);
		assertTrue(result.toString(), result.nonZeroFriendlyTracer);
		assertTrue(result.toString(), result.hostileTracers > 0);
		assertTrue(result.toString(), result.nonZeroHostileTracer);
		assertTrue(
				result.toString(),
				result.finalMagazine < result.initialMagazine);
		assertTrue(result.toString(), result.damageTaken > 0);
		assertTrue(result.toString(), result.firefights > 0);
		assertEquals(result.toString(), 1, result.killCount);
		assertFalse(result.toString(), result.targetBodyActive);
		assertFalse(result.toString(), result.targetStillInLevel);
	}

	private static boolean hasDefinitionPrefix(
			BukovRaidCoordinator raid,
			String prefix) {
		for (RaidItem item : raid.loot().items()) {
			if (item.definitionId().startsWith(prefix)) return true;
		}
		return false;
	}

	private static Set<String> itemUids(List<RaidItem> items) {
		Set<String> result = new HashSet<>();
		for (RaidItem item : items) result.add(item.itemUid());
		return result;
	}

	private static RaidItem foundItem(
			String uid,
			String definitionId) {
		return new RaidItem(
				uid,
				definitionId,
				1,
				0.1f,
				250,
				true,
				false,
				1f);
	}

	/** Minimal deterministic preference store for generated-level headless runs. */
	static final class MemoryPreferences implements Preferences {

		private final Map<String, Object> values = new HashMap<>();

		@Override
		public Preferences putBoolean(String key, boolean value) {
			values.put(key, value);
			return this;
		}

		@Override
		public Preferences putInteger(String key, int value) {
			values.put(key, value);
			return this;
		}

		@Override
		public Preferences putLong(String key, long value) {
			values.put(key, value);
			return this;
		}

		@Override
		public Preferences putFloat(String key, float value) {
			values.put(key, value);
			return this;
		}

		@Override
		public Preferences putString(String key, String value) {
			values.put(key, value);
			return this;
		}

		@Override
		public Preferences put(Map<String, ?> additions) {
			values.putAll(additions);
			return this;
		}

		@Override
		public boolean getBoolean(String key) {
			return getBoolean(key, false);
		}

		@Override
		public int getInteger(String key) {
			return getInteger(key, 0);
		}

		@Override
		public long getLong(String key) {
			return getLong(key, 0L);
		}

		@Override
		public float getFloat(String key) {
			return getFloat(key, 0f);
		}

		@Override
		public String getString(String key) {
			return getString(key, "");
		}

		@Override
		public boolean getBoolean(String key, boolean defaultValue) {
			Object value = values.get(key);
			return value instanceof Boolean
					? (Boolean) value
					: defaultValue;
		}

		@Override
		public int getInteger(String key, int defaultValue) {
			Object value = values.get(key);
			return value instanceof Number
					? ((Number) value).intValue()
					: defaultValue;
		}

		@Override
		public long getLong(String key, long defaultValue) {
			Object value = values.get(key);
			return value instanceof Number
					? ((Number) value).longValue()
					: defaultValue;
		}

		@Override
		public float getFloat(String key, float defaultValue) {
			Object value = values.get(key);
			return value instanceof Number
					? ((Number) value).floatValue()
					: defaultValue;
		}

		@Override
		public String getString(String key, String defaultValue) {
			Object value = values.get(key);
			return value instanceof String
					? (String) value
					: defaultValue;
		}

		@Override
		public Map<String, ?> get() {
			return Collections.unmodifiableMap(
					new HashMap<>(values));
		}

		@Override
		public boolean contains(String key) {
			return values.containsKey(key);
		}

		@Override
		public void clear() {
			values.clear();
		}

		@Override
		public void remove(String key) {
			values.remove(key);
		}

		@Override
		public void flush() {
		}
	}
}
