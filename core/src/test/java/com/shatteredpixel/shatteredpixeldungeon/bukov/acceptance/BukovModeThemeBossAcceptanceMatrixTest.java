package com.shatteredpixel.shatteredpixeldungeon.bukov.acceptance;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.BukovHostMob;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyArchetypeDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyArchetypeRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.FirstRaidEnemySpawnDirector;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.WhiteLineBossStateMachine;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovAnchorPlanner;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovLevel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovRaidLayout;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovZonePlanner;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.ExtractionDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.RaidMapValidator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.ThemeDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.ThemeRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovExtractionStates;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCoordinator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.ExtractionState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.LootTransaction;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidOutcome;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidResult;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.SettlementReceipt;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.WhiteLineSpawnPolicy;
import com.watabou.noosa.Game;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Deterministic production host matrix: four economic modes x six themes,
 * plus the authored fixed cold-storage training ground.
 *
 * Every row executes production map generation/validation, the authored boss
 * spawn contract and state machine when enabled, plus both success and death
 * settlement paths through the resumable raid coordinator. The structured
 * stdout row is consumed by scripts/bukov_mode_theme_boss_matrix_gate.sh.
 */
@RunWith(Parameterized.class)
public class BukovModeThemeBossAcceptanceMatrixTest {

	private static final int EXPECTED_HOST_COMBINATION_COUNT = 25;
	private static final int EXPECTED_THEME_COUNT = 6;

	@Parameterized.Parameters(name = "{0} x {1}")
	public static Collection<Object[]> matrix() throws IOException {
		ThemeRegistry themes = loadThemes();
		List<Object[]> rows = new ArrayList<>();
		for (BukovRaidMode mode : BukovRaidMode.values()) {
			int themeIndex = 0;
			for (ThemeDefinition theme : themes.all()) {
				if (mode.trainingGround()
						&& !"cold_storage".equals(theme.id)) {
					themeIndex++;
					continue;
				}
				rows.add(new Object[]{mode, theme, themeIndex++});
			}
		}
		assertEquals(EXPECTED_HOST_COMBINATION_COUNT, rows.size());
		return rows;
	}

	private final BukovRaidMode mode;
	private final ThemeDefinition theme;
	private final int themeIndex;
	private int previousDepth;
	private int previousBranch;
	private long previousSeed;
	private String previousVersion;
	private BukovRaidMode previousMode;
	private List<String> previousMaps;
	private String previousSelectedMap;

	public BukovModeThemeBossAcceptanceMatrixTest(
			BukovRaidMode mode,
			ThemeDefinition theme,
			int themeIndex) {
		this.mode = mode;
		this.theme = theme;
		this.themeIndex = themeIndex;
	}

	@Before
	public void captureGlobals() {
		previousDepth = Dungeon.depth;
		previousBranch = Dungeon.branch;
		previousSeed = Dungeon.seed;
		previousVersion = Game.version;
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
		Game.version = previousVersion;
		BukovMode.prepareRaidMode(previousMode);
		BukovMode.prepareUnlockedMaps(previousMaps);
		BukovMode.prepareSelectedMap(previousSelectedMap);
	}

	@Test
	public void mapBossAndBothSettlementOutcomesCloseTheLoop()
			throws Exception {
		assertEquals(5, BukovRaidMode.values().length);
		assertEquals(EXPECTED_THEME_COUNT, loadThemes().all().size());

		long seed = 700_001L
				+ (long)mode.ordinal() * 10_007L
				+ (long)themeIndex * 1_009L;
		// The engine-independent themed prototype proves every selected theme
		// retains the same validated extraction topology.
		BukovRaidLayout prototype =
				BukovZonePlanner.generateFirstRaid(seed, theme);
		BukovRaidLayout prototypeReplay =
				BukovZonePlanner.generateFirstRaid(seed, theme);
		RaidMapValidator.Result validation =
				RaidMapValidator.validate(prototype);
		assertTrue(
				mode + " x " + theme.id + " map: "
						+ validation.failure + " " + validation.reason,
				validation.valid);
		assertEquals(theme.id, prototype.themeId);
		assertEquals(
				layoutFingerprint(prototype),
				layoutFingerprint(prototypeReplay));

		// This is the actual host level path used by gameplay. It consumes both
		// selected mode geometry and selected theme, then paints real terrain.
		BukovLevel firstLevel = buildRealLevel(seed);
		BukovRaidLayout first = firstLevel.raidLayout();
		BukovLevel secondLevel = buildRealLevel(seed);
		BukovRaidLayout second = secondLevel.raidLayout();
		assertEquals(mode, firstLevel.raidMode());
		String effectiveTheme = theme.id;
		assertEquals(effectiveTheme, first.themeId);
		RaidMapValidator.Result hostValidation =
				RaidMapValidator.validate(first, mode);
		assertTrue(
				mode + " x " + theme.id + " host map: "
						+ hostValidation.failure + " "
						+ hostValidation.reason,
				hostValidation.valid);
		assertTrue(
				mode + " room count=" + first.playableRoomCount(),
				mode.acceptsContentRoomCount(first.playableRoomCount()));
		assertTrue(firstLevel.generationDiagnostics().isEmpty());
		BukovAnchorPlanner.Result traversal =
				BukovAnchorPlanner.validateLockedMissionTraversal(
						firstLevel.width(),
						firstLevel.height(),
						firstLevel.map,
						first,
						firstLevel.entrance());
		assertTrue(traversal.reason, traversal.valid);
		assertEquals(EXPECTED_THEME_COUNT, loadThemes().all().size());
		assertEquals(layoutFingerprint(first), layoutFingerprint(second));
		assertEquals(3, first.routes.size());
		assertEquals(3, first.extractions.size());
		assertNotNull(first.missionGate());

		BukovRaidLayout.BossMechanism bossMechanism =
				first.bossMechanism();
		assertNotNull("authored boss arena must exist", bossMechanism);
		assertTrue(bossMechanism.bodyTraceCells.length >= 3);
		assertTrue(bossMechanism.fogLampCell >= 0);
		assertFalse(bossMechanism.bossRoomId.isEmpty());

		EnemyArchetypeDefinition bossDefinition = loadEnemies().require(
				FirstRaidEnemySpawnDirector.FIRST_BOSS);
		float productionBossEarliest =
				WhiteLineSpawnPolicy.earliestSeconds(
						mode,
						bossDefinition,
						1);
		float bossEligibleAt = mode.bossEnabled
				? Math.max(90f, productionBossEarliest)
				: 90f;
		boolean bossEligible = WhiteLineSpawnPolicy.eligible(
				mode,
				bossDefinition,
				1,
				bossEligibleAt,
				false,
				false,
				0);
		WhiteLineBossStateMachine.Phase bossVictory =
				WhiteLineBossStateMachine.Phase.DORMANT;
		if (bossEligible) {
			assertTrue(mode.bossEnabled);
			assertFalse(WhiteLineSpawnPolicy.eligible(
					mode,
					bossDefinition,
					1,
					Math.max(0f, productionBossEarliest - 1f),
					false,
					false,
					0));
			assertFalse(WhiteLineSpawnPolicy.eligible(
					mode,
					bossDefinition,
					1,
					bossEligibleAt,
					true,
					false,
					0));
			assertFalse(WhiteLineSpawnPolicy.eligible(
					mode,
					bossDefinition,
					1,
					bossEligibleAt,
					false,
					false,
					1));
			assertTrue(WhiteLineSpawnPolicy.acceptsSpawnPoint(
					true,
					bossMechanism.bodyTraceCells[0],
					firstLevel.length(),
					false,
					false,
					false));
			assertFalse(WhiteLineSpawnPolicy.acceptsSpawnPoint(
					true,
					bossMechanism.bodyTraceCells[0],
					firstLevel.length(),
					true,
					false,
					false));
			BukovHostMob spawnedBoss =
					new BukovHostMob().configure(bossDefinition);
			spawnedBoss.pos = bossMechanism.bodyTraceCells[0];
			assertEquals(
					FirstRaidEnemySpawnDirector.FIRST_BOSS,
					spawnedBoss.definitionId());
			assertEquals(bossDefinition.health, spawnedBoss.HP);
			assertEquals(
					bossMechanism.bodyTraceCells[0],
					spawnedBoss.pos);
			WhiteLineBossStateMachine boss =
					new WhiteLineBossStateMachine(
							bossDefinition.health,
							seed);
			defeatBoss(boss, bossMechanism);
			bossVictory = boss.phase();
			assertEquals(
					WhiteLineBossStateMachine.Phase.DEFEATED,
					bossVictory);
		} else {
			assertFalse(mode.bossEnabled);
		}

		RaidResult success = settle(
				seed,
				"success",
				first,
				RaidOutcome.SUCCESS,
				bossVictory == WhiteLineBossStateMachine.Phase.DEFEATED);
		RaidResult death = settle(
				seed,
				"death",
				first,
				RaidOutcome.DEATH,
				false);

		assertEquals(RaidOutcome.SUCCESS, success.outcome());
		assertEquals(RaidOutcome.DEATH, death.outcome());
		assertTrue(success.debriefAvailable());
		assertTrue(death.debriefAvailable());
		assertFalse(success.replayed());
		assertFalse(death.replayed());
		if (mode.trainingGround()) {
			assertEquals(0L, success.transferredQuantity());
			assertEquals(0L, death.lostQuantity());
		} else {
			assertEquals(1L, success.transferredQuantity());
			assertEquals(1L, death.lostQuantity());
		}
		assertEquals(
				mode == BukovRaidMode.BOSS_CONTRACT && bossEligible,
				success.missionCompleted());
		assertFalse(death.missionCompleted());

		System.out.println(matrixEvidence(
				seed,
				effectiveTheme,
				layoutFingerprint(first),
				first.playableRoomCount(),
				bossEligible,
				bossVictory,
				success,
				death));
	}

	private RaidResult settle(
			long seed,
			String suffix,
			BukovRaidLayout layout,
			RaidOutcome outcome,
			boolean bossDefeated) throws IOException {
		String raidId = "matrix-"
				+ mode.name().toLowerCase() + '-'
				+ theme.id + '-' + suffix;
		InMemoryBukovSaveService saves =
				new InMemoryBukovSaveService();
		BukovProfile profile = saves.loadProfile();
		profile.selectRaidMode(mode);
		saves.saveProfile(profile);

		List<ExtractionState> extractions =
				BukovExtractionStates.fromLayout(layout);
		assertEquals(3, extractions.size());
		assertEquals("E01", extractions.get(0).extractionId());
		assertEquals(ExtractionState.Type.BASIC, extractions.get(0).type());
		assertEquals(ExtractionState.Type.CONDITIONAL, extractions.get(1).type());
		assertEquals(ExtractionState.Type.TEMPORARY, extractions.get(2).type());
		BukovRaidCoordinator raid = BukovRaidCoordinator.start(
				saves,
				seed,
				raidId,
				100f,
				extractions);
		assertEquals(
				LootTransaction.PickupResult.ADDED,
				raid.pickup(evidenceItem(raidId)));
		if (mode == BukovRaidMode.BOSS_CONTRACT && bossDefeated) {
			assertTrue(raid.markBossContractCompleted());
		}

		RaidResult result;
		if (outcome == RaidOutcome.SUCCESS) {
			assertTrue(raid.beginExtraction("E01"));
			raid.tick(
					raid.extraction("E01").interactionSeconds(),
					ExtractionState.Interaction.ACTIVE);
			result = raid.settleSuccess();
		} else {
			result = raid.settleDeath();
		}

		BukovProfile settled = saves.loadProfile();
		SettlementReceipt receipt = settled.settlement(raidId);
		assertNotNull(receipt);
		assertEquals(outcome, receipt.outcome());
		assertEquals(outcome, result.outcome());
		assertEquals(result.missionCompleted(), receipt.missionCompleted());
		assertTrue(settled.isSettled(raidId));
		assertNull(saves.loadRaidCheckpoint());
		return result;
	}

	private static void defeatBoss(
			WhiteLineBossStateMachine boss,
			BukovRaidLayout.BossMechanism mechanism) {
		assertEquals(
				WhiteLineBossStateMachine.Result.ENGAGED,
				boss.engage());
		assertEquals(
				WhiteLineBossStateMachine.Result.OBJECTIVE_COMPLETED,
				boss.flankUmbrella(1f, 0f, -1f, 0f));
		assertEquals(
				WhiteLineBossStateMachine.Result.PHASE_CHANGED,
				boss.applyDamage(boss.maximumHealth()));
		assertTrue(boss.synchronizedTrace(boss.trueBodyIndex()));
		assertTrue(boss.trueBodyIndex() < mechanism.bodyTraceCells.length);
		assertEquals(
				WhiteLineBossStateMachine.Result.OBJECTIVE_COMPLETED,
				boss.identifyTrueBody(boss.trueBodyIndex()));
		assertEquals(
				WhiteLineBossStateMachine.Result.PHASE_CHANGED,
				boss.applyDamage(boss.maximumHealth()));
		assertEquals(
				boss.fogLampAnchor(),
				mechanism.fogLampAnchorId);
		assertEquals(
				WhiteLineBossStateMachine.Result.OBJECTIVE_COMPLETED,
				boss.disableFogLamp(mechanism.fogLampAnchorId));
		assertEquals(
				WhiteLineBossStateMachine.Result.DEFEATED,
				boss.applyDamage(boss.maximumHealth()));
	}

	private String matrixEvidence(
			long seed,
			String effectiveTheme,
			String mapFingerprint,
			int roomCount,
			boolean bossEligible,
			WhiteLineBossStateMachine.Phase bossVictory,
			RaidResult success,
			RaidResult death) {
		return "BUKOV_MATRIX"
				+ "\tmode=" + mode.name()
				+ "\ttheme=" + theme.id
				+ "\tmap_theme=" + effectiveTheme
				+ "\tseed=" + seed
				+ "\tmap=VALID"
				+ "\trooms=" + roomCount
				+ "\tfingerprint=" + mapFingerprint
				+ "\textractions=3"
				+ "\tboss_policy="
				+ (bossEligible ? "ELIGIBLE" : "SUPPRESSED")
				+ "\tboss_model="
				+ (bossEligible ? bossVictory.name() : "NOT_APPLICABLE")
				+ "\twin=" + success.outcome().name()
				+ "\twin_qty=" + success.transferredQuantity()
				+ "\tloss=" + death.outcome().name()
				+ "\tloss_qty=" + death.lostQuantity()
				+ "\treceipts=2";
	}

	private BukovLevel buildRealLevel(long seed) throws IOException {
		ThemeRegistry registry = loadThemes();
		List<String> mapIds = new ArrayList<>();
		for (ThemeDefinition definition : registry.all()) {
			mapIds.add(definition.id);
		}
		BukovMode.prepareUnlockedMaps(mapIds);
		BukovMode.prepareSelectedMap(theme.id);
		BukovMode.prepareRaidMode(mode);
		Dungeon.depth = 1;
		Dungeon.branch = 0;
		Dungeon.seed = seed;
		BukovLevel level = new BukovLevel();
		level.create();
		return level;
	}

	private static RaidItem evidenceItem(String raidId) {
		return new RaidItem(
				raidId + "-loot",
				"loot:matrix_evidence",
				1,
				0.1f,
				1_000,
				true,
				false,
				1f);
	}

	private static String layoutFingerprint(BukovRaidLayout layout)
			throws Exception {
		StringBuilder value = new StringBuilder();
		value.append(layout.seed).append('|')
				.append(layout.themeId).append('|');
		for (BukovRaidLayout.Mark mark : layout.marks) {
			value.append(mark.roomId()).append(':')
					.append(mark.zone).append(':')
					.append(mark.semanticId).append(':')
					.append(mark.minimumPassageWidthTiles).append(':')
					.append(mark.eliteSpawnAllowed).append(':')
					.append(mark.structuralTransit).append('|');
		}
		for (BukovRaidLayout.Link link : layout.links) {
			value.append(link.firstRoomId).append('>')
					.append(link.secondRoomId).append(':')
					.append(link.requiredEvent).append(':')
					.append(link.traversalSeconds).append('|');
		}
		for (ExtractionDefinition extraction : layout.extractions) {
			value.append(extraction.id).append(':')
					.append(extraction.roomId).append(':')
					.append(extraction.interactionCell).append(':')
					.append(extraction.availableFromSeconds).append('|');
		}
		for (BukovRaidLayout.LootAnchor anchor : layout.lootAnchors) {
			value.append(anchor.id).append(':')
					.append(anchor.roomId).append(':')
					.append(anchor.cell).append(':')
					.append(anchor.lootTableId).append('|');
		}
		for (BukovRaidLayout.Route route : layout.routes) {
			value.append(route.routeId).append(':')
					.append(route.risk).append(':')
					.append(route.roomIds).append('|');
		}
		BukovRaidLayout.MissionGate gate = layout.missionGate();
		value.append(gate.archiveCell).append(':')
				.append(gate.gateCell).append(':')
				.append(Arrays.toString(gate.gateCells)).append('|');
		BukovRaidLayout.BossMechanism boss = layout.bossMechanism();
		value.append(boss.bossRoomId).append(':')
				.append(Arrays.toString(boss.bodyTraceCells)).append(':')
				.append(boss.fogLampAnchorId).append(':')
				.append(boss.fogLampCell);

		byte[] digest = MessageDigest.getInstance("SHA-256").digest(
				value.toString().getBytes(StandardCharsets.UTF_8));
		StringBuilder hex = new StringBuilder();
		for (int index = 0; index < 12; index++) {
			hex.append(String.format("%02x", digest[index] & 0xff));
		}
		return hex.toString();
	}

	private static ThemeRegistry loadThemes() throws IOException {
		ThemeRegistry registry = new ThemeRegistry();
		registry.loadJson(readAsset("themes.json"));
		return registry;
	}

	private static EnemyArchetypeRegistry loadEnemies() throws IOException {
		EnemyArchetypeRegistry registry =
				new EnemyArchetypeRegistry();
		registry.loadJson(readAsset("enemies.json"));
		return registry;
	}

	private static String readAsset(String filename) throws IOException {
		return new String(
				Files.readAllBytes(Paths.get(
						"src/main/assets/bukov/content/" + filename)),
				StandardCharsets.UTF_8);
	}
}
