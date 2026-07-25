package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.FireControl;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.HitscanResolver;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.RealtimeDamage;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.Firearm;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovFirstRaidLootTables;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFxEvent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFxEventPool;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovLevel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovRaidLayout;
import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovContainerDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovHeapLootAdapter;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCoordinator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidWorldDefinitions;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRuntimeLoadoutAdapter;
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
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.watabou.noosa.Game;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

	@Before
	public void captureGlobals() {
		previousDepth = Dungeon.depth;
		previousBranch = Dungeon.branch;
		previousSeed = Dungeon.seed;
		previousVersion = Game.version;
		previousLevel = Dungeon.level;
		previousHero = Dungeon.hero;
		previousMode = BukovMode.raidMode();
		previousMaps = new ArrayList<>(BukovMode.unlockedRaidThemes());
		previousSelectedMap = BukovMode.selectedRaidTheme();
		if (Game.version == null) Game.version = "test";
	}

	@After
	public void restoreGlobals() {
		Dungeon.depth = previousDepth;
		Dungeon.branch = previousBranch;
		Dungeon.seed = previousSeed;
		Dungeon.level = previousLevel;
		Dungeon.hero = previousHero;
		Dungeon.quickslot.reset();
		Game.version = previousVersion;
		BukovMode.prepareRaidMode(previousMode);
		BukovMode.prepareUnlockedMaps(previousMaps);
		BukovMode.prepareSelectedMap(previousSelectedMap);
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

		raid.setExtractionCondition(
				FirstRaidMission.CONDITIONAL_EXTRACTION_ID,
				true);
		completeExtraction(
				raid,
				FirstRaidMission.CONDITIONAL_EXTRACTION_ID);
		RaidResult result = raid.settleSuccess();

		assertEquals(RaidOutcome.SUCCESS, result.outcome());
		assertTrue(result.missionCompleted());
		assertEquals(1, result.kills());
		assertTrue(result.transferredUids().contains(archiveUid));
		assertNull(saves.loadRaidCheckpoint());
		assertTrue(saves.loadProfile().completedContracts().contains(
				FirstRaidMission.EVENT_ID));

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
		BukovMode.prepareRaidMode(BukovRaidMode.EXPEDITION);
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
		FirearmRegistry firearms = new FirearmRegistry();
		firearms.loadJson(readContent("firearms.json"));
		AmmoRegistry ammunition = new AmmoRegistry();
		ammunition.loadJson(readContent("ammunition.json"));
		BukovRuntimeLoadoutAdapter.RuntimeLoadout runtime =
				new BukovRuntimeLoadoutAdapter(
						firearms,
						ammunition).materialize(raid);
		Firearm firearm = runtime.primaryWeapon();
		assertNotNull(firearm);
		FirearmDefinition definition = firearm.definition(firearms);
		int initialMagazine = firearm.magazineAmmo();
		assertTrue(initialMagazine > 0);

		int[] adjacent = adjacentPassableCells(level);
		RealtimeBody shooter = new RealtimeBody(
				adjacent[0], level.width(), 0.25f);
		RealtimeBody target = new RealtimeBody(
				adjacent[1], level.width(), 0.25f);
		CombatSink sink = new CombatSink(
				new LevelCollisionMap(level),
				shooter,
				target);
		FireControl control = new FireControl();
		while (sink.targetHealth > 0 && firearm.magazineAmmo() > 0) {
			control.update(
					definition.secondsPerShot(),
					false,
					false,
					false,
					firearm,
					definition,
					sink);
			control.update(
					0f,
					true,
					true,
					false,
					firearm,
					definition,
					sink);
		}

		assertTrue(sink.targetHealth < CombatSink.INITIAL_HEALTH);
		assertTrue(sink.targetHealth <= 0);
		assertTrue(sink.tracerEvents > 0);
		assertTrue(sink.nonZeroTracer);
		assertTrue(firearm.magazineAmmo() < initialMagazine);
		raid.session().recordKill();
		runtime.writeBack(raid.loot());
	}

	private static int[] adjacentPassableCells(BukovLevel level) {
		for (int cell = 0; cell < level.length(); cell++) {
			if (!level.passable[cell] || level.solid[cell]) continue;
			int x = cell % level.width();
			int y = cell / level.width();
			int right = cell + 1;
			if (x + 1 < level.width()
					&& right < level.length()
					&& level.passable[right]
					&& !level.solid[right]) {
				return new int[]{cell, right};
			}
			int down = cell + level.width();
			if (y + 1 < level.height()
					&& down < level.length()
					&& level.passable[down]
					&& !level.solid[down]) {
				return new int[]{cell, down};
			}
		}
		throw new AssertionError("Generated Bukov map has no adjacent open cells");
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

	private static String readContent(String fileName)
			throws IOException {
		return new String(
				Files.readAllBytes(Paths.get(
						"src/main/assets/bukov/content/" + fileName)),
				StandardCharsets.UTF_8);
	}

	private static final class CombatSink
			implements FireControl.Sink {

		private static final int INITIAL_HEALTH = 80;

		private final CollisionMap collision;
		private final RealtimeBody shooter;
		private final RealtimeBody target;
		private final HitscanResolver.Hit hit =
				new HitscanResolver.Hit();
		private final CombatFxEventPool fx =
				new CombatFxEventPool(16);
		private int targetHealth = INITIAL_HEALTH;
		private int sequence;
		private int tracerEvents;
		private boolean nonZeroTracer;

		private CombatSink(
				CollisionMap collision,
				RealtimeBody shooter,
				RealtimeBody target) {
			this.collision = collision;
			this.shooter = shooter;
			this.target = target;
		}

		@Override
		public void fire(
				Firearm firearm,
				FirearmDefinition definition) {
			float directionX = target.x - shooter.x;
			float directionY = target.y - shooter.y;
			HitscanResolver.cast(
					shooter.x,
					shooter.y,
					directionX,
					directionY,
					definition.effectiveRangeTiles,
					collision,
					(minX, minY, maxX, maxY) ->
							Collections.singletonList(target),
					shooter,
					hit);
			fx.tracer(
					1,
					++sequence,
					false,
					shooter.x,
					shooter.y,
					hit.x,
					hit.y,
					definition.tracerIntensity);
			fx.drain(event -> {
				if (event.type() != CombatFxEvent.Type.TRACER) {
					return;
				}
				tracerEvents++;
				float dx = event.toX() - event.fromX();
				float dy = event.toY() - event.fromY();
				nonZeroTracer |= event.intensity() > 0f
						&& dx * dx + dy * dy > 0.0001f;
			});
			if (hit.body == target) {
				float damage = RealtimeDamage.resolve(
						definition.damage,
						1f,
						hit.distance,
						definition.effectiveRangeTiles,
						definition.penetration,
						RealtimeDamage.HitZone.CORE,
						null);
				targetHealth -= Math.max(1, Math.round(damage));
				if (targetHealth <= 0) target.active = false;
			}
		}

		@Override
		public FireControl.AmmoSelection requestAmmo(
				String caliber,
				String preferredDefinitionId,
				int maximum,
				boolean allowAlternative) {
			return FireControl.AmmoSelection.none();
		}

		@Override
		public void dryFire() {
		}

		@Override
		public void reloadStarted(float seconds) {
		}

		@Override
		public void reloadAudioCues(
				FirearmDefinition definition,
				int cueMask) {
		}

		@Override
		public void reloadFinished() {
		}
	}
}
