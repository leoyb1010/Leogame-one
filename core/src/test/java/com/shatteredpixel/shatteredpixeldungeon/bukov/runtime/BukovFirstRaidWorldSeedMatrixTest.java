package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovAnchorPlanner;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovLevel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovContainerDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCoordinator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidWorldDefinitions;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.ExtractionState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidBalanceTelemetry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidOutcome;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidResult;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.SettlementReceipt;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovHubController;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.watabou.noosa.Game;
import com.watabou.utils.GameSettings;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Ten deterministic, isolated production-World first-raid samples.
 *
 * These are automated simulation facts, not human playtest duration claims.
 * The generated JSON preserves that distinction for downstream evidence gates.
 */
public class BukovFirstRaidWorldSeedMatrixTest {

	private static final int REQUIRED_SEEDS = 10;
	private static final String SAMPLE_TYPE =
			"AUTOMATED_PRODUCTION_WORLD_ROUTE";
	private static final String THEME_ID = "fog_depot";

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
	private BukovPlayerJourneyAcceptanceTest.HeadlessStatisticsState
			previousStatistics;

	@Before
	public void captureGlobals() {
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
		GameSettings.set(
				new BukovPlayerJourneyAcceptanceTest.MemoryPreferences());
		if (Game.version == null) Game.version = "test";
		previousBadgesState =
				BukovPlayerJourneyAcceptanceTest
						.replaceBadgesForHeadlessTest();
		previousStatistics =
				BukovPlayerJourneyAcceptanceTest
						.replaceStatisticsForHeadlessTest();
		Dungeon.droppedItems = new SparseArray<>();
	}

	@After
	public void restoreGlobals() {
		Dungeon.depth = previousDepth;
		Dungeon.branch = previousBranch;
		Dungeon.seed = previousSeed;
		Dungeon.level = previousLevel;
		Dungeon.hero = previousHero;
		Dungeon.droppedItems = previousDroppedItems;
		BukovPlayerJourneyAcceptanceTest.restoreBadgesAfterHeadlessTest(
				previousBadgesState);
		BukovPlayerJourneyAcceptanceTest
				.restoreStatisticsAfterHeadlessTest(previousStatistics);
		Dungeon.quickslot.reset();
		Game.version = previousVersion;
		BukovMode.prepareRaidMode(previousMode);
		BukovMode.prepareUnlockedMaps(previousMaps);
		BukovMode.prepareSelectedMap(previousSelectedMap);
		GameSettings.set(null);
	}

	@Test
	public void tenUniqueSeedsCompleteProductionWorldFirstRaid()
			throws Exception {
		Set<Long> uniqueSeeds = new HashSet<>();
		List<SeedEvidence> evidence = new ArrayList<>();
		List<String> failures = new ArrayList<>();

		for (int index = 0; index < REQUIRED_SEEDS; index++) {
			long seed = seed(index);
			assertTrue("duplicate configured seed=" + seed,
					uniqueSeeds.add(seed));
			SeedEvidence sample = new SeedEvidence(index, seed);
			evidence.add(sample);
			Random.pushGenerator(seed ^ 0x42554B4F56524C44L);
			try {
				Actor.clear();
				Dungeon.droppedItems = new SparseArray<>();
				BukovPlayerJourneyAcceptanceTest
						.replaceBadgesForHeadlessTest();
				BukovPlayerJourneyAcceptanceTest
						.resetStatisticsForHeadlessSample();
				exerciseSeed(sample);
			} catch (Exception | AssertionError failure) {
				sample.failure = failureSummary(failure);
			} finally {
				Random.popGenerator();
				Actor.clear();
				Dungeon.level = null;
				Dungeon.hero = null;
				Dungeon.quickslot.reset();
			}
			if (!sample.passes()) {
				failures.add("seed=" + seed + " " + sample.failureReason());
			}
		}

		File summary = writeSummary(evidence, failures);
		assertEquals(REQUIRED_SEEDS, uniqueSeeds.size());
		assertTrue(
				"automated first-raid seed matrix failed; evidence="
						+ summary.getAbsolutePath() + "; " + failures,
				failures.isEmpty());
	}

	private static void exerciseSeed(SeedEvidence sample)
			throws Exception {
		InMemoryBukovSaveService saves =
				new InMemoryBukovSaveService();
		BukovHubController hub = new BukovHubController(saves);
		hub.prepareAndConfirmDeployment();

		BukovLevel level = buildLevel(sample.seed);
		BukovAnchorPlanner.Result traversal =
				BukovAnchorPlanner.validateLockedMissionTraversal(
						level.width(),
						level.height(),
						level.map,
						level.raidLayout(),
						level.entrance());
		sample.mapReachable = traversal.valid;
		sample.mapReachabilityReason = traversal.reason;

		String raidId = "world-seed-matrix-" + sample.index;
		BukovRaidCoordinator raid = startRaid(
				saves,
				level,
				sample.seed,
				raidId);
		BukovRealtimeCombatHarness.Result live =
				BukovRealtimeCombatHarness
						.completeFirstRaidThroughWorld(raid, level);
		captureLiveEvidence(sample, live);
		sample.duplicateRaidUid = hasDuplicateUids(raid.loot().items());
		sample.duplicateAcrossRaidAndStash = hasUidOverlap(
				raid.loot().items(),
				saves.loadProfile().stash().items());

		String archiveUid = raid.loot().firstItemUidForDefinition(
				FirstRaidMission.ARCHIVE_DEFINITION_ID);
		sample.archiveUidPresent = archiveUid != null;
		RaidResult settlement = raid.settleSuccess();
		sample.settlementSuccess =
				settlement.outcome() == RaidOutcome.SUCCESS;
		sample.settlementMissionCompleted =
				settlement.missionCompleted();
		sample.settlementKills = settlement.kills();
		sample.transferredUidCount =
				settlement.transferredUids().size();
		sample.transferredValue = settlement.transferredValue();
		sample.duplicateSettlementUid =
				hasDuplicateStrings(settlement.transferredUids());
		sample.archiveTransferred =
				archiveUid != null
						&& settlement.transferredUids().contains(archiveUid);

		BukovProfile profile = saves.loadProfile();
		sample.duplicateStashUid =
				hasDuplicateUids(profile.stash().items());
		SettlementReceipt receipt = profile.settlement(raidId);
		if (receipt == null) {
			sample.failure = "missing durable settlement receipt";
			return;
		}
		captureBalance(sample, receipt.balanceTelemetry());
	}

	private static void captureLiveEvidence(
			SeedEvidence sample,
			BukovRealtimeCombatHarness.Result live) {
		sample.generatedEnemyCount = live.generatedEnemyCount;
		sample.realCombat =
				live.generatedEnemyCount > 0
						&& live.finalTargetHealth <= 0
						&& live.friendlyTracers > 0
						&& live.nonZeroFriendlyTracer
						&& live.hostileTracers > 0
						&& live.nonZeroHostileTracer
						&& live.damageTaken > 0
						&& live.firefights > 0
						&& live.killCount == 1
						&& !live.targetBodyActive
						&& !live.targetStillInLevel;
		sample.liveKills = live.killCount;
		sample.liveFirefights = live.firefights;
		sample.liveDamageTaken = live.damageTaken;
		sample.friendlyTracers = live.friendlyTracers;
		sample.hostileTracers = live.hostileTracers;

		BukovRealtimeCombatHarness.FirstRaidJourneyEvidence journey =
				live.firstRaidJourney;
		if (journey == null) {
			sample.failure = "missing first-raid World journey evidence";
			return;
		}
		sample.gateInitiallyBlocked = journey.gateInitiallyBlocked;
		sample.archiveSearchPrompted = journey.archiveSearchPrompted;
		sample.archiveCarried = journey.archiveCarried;
		sample.gateUnlockedByArchive = journey.gateUnlockedByArchive;
		sample.crossedUnlockedGate = journey.crossedUnlockedGate;
		sample.highValueSearchPrompted =
				journey.highValueSearchPrompted;
		sample.highValueLootCollected =
				journey.highValueLootCollected;
		sample.missionCompleted = journey.missionCompleted;
		sample.extractionPrompted = journey.extractionPrompted;
		sample.extractionCompleted = journey.extractionCompleted;
	}

	private static void captureBalance(
			SeedEvidence sample,
			RaidBalanceTelemetry balance) {
		sample.balanceAvailable = balance.available();
		sample.balanceSeed = balance.seed();
		sample.balanceMode = balance.mode().name();
		sample.balanceThemeId = balance.themeId();
		sample.balanceRouteId = balance.routeId();
		sample.simulatedDurationSeconds = balance.durationSeconds();
		sample.balanceContainerSearches = balance.containerSearches();
		sample.balanceFirefights = balance.firefights();
		sample.balanceKills = balance.kills();
		sample.balanceDamageTaken = balance.damageTaken();
		sample.balanceExtractedValue = balance.extractedValue();
		sample.balanceEnd = balance.end().name();
		sample.balanceSettled = balance.settled();
	}

	private static BukovLevel buildLevel(long seed) {
		BukovMode.prepareRaidMode(BukovRaidMode.EXPEDITION);
		BukovMode.prepareUnlockedMaps(
				Collections.singletonList(THEME_ID));
		BukovMode.prepareSelectedMap(THEME_ID);
		Dungeon.depth = 1;
		Dungeon.branch = 0;
		Dungeon.seed = seed;
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
		if (extractions.size() != 3) {
			throw new AssertionError(
					"seed=" + seed + " expected three extractions");
		}
		return BukovRaidCoordinator.start(
				saves,
				seed,
				raidId,
				BukovHubController.FIRST_RAID_WEIGHT_LIMIT,
				extractions,
				containers);
	}

	private static long seed(int index) {
		long value = 0x9E3779B97F4A7C15L * (index + 1L);
		value ^= value >>> 30;
		value *= 0xBF58476D1CE4E5B9L;
		value ^= value >>> 27;
		value *= 0x94D049BB133111EBL;
		return value ^ value >>> 31;
	}

	private static boolean hasDuplicateUids(List<RaidItem> items) {
		Set<String> uids = new HashSet<>();
		for (RaidItem item : items) {
			if (!uids.add(item.itemUid())) return true;
		}
		return false;
	}

	private static boolean hasDuplicateStrings(List<String> values) {
		return new HashSet<>(values).size() != values.size();
	}

	private static boolean hasUidOverlap(
			List<RaidItem> first,
			List<RaidItem> second) {
		Set<String> uids = new HashSet<>();
		for (RaidItem item : first) uids.add(item.itemUid());
		for (RaidItem item : second) {
			if (uids.contains(item.itemUid())) return true;
		}
		return false;
	}

	private static String failureSummary(Throwable failure) {
		String message = failure.getMessage();
		return failure.getClass().getSimpleName()
				+ (message == null || message.isEmpty()
						? "" : ": " + message);
	}

	private static File writeSummary(
			List<SeedEvidence> evidence,
			List<String> failures) throws IOException {
		File directory = evidenceDirectory();
		File target = new File(directory, "summary.json");
		StringBuilder json = new StringBuilder();
		json.append("{\n")
				.append("  \"schemaVersion\": 1,\n")
				.append("  \"gate\": \"bukov_first_raid_world_seed_matrix\",\n")
				.append("  \"status\": \"")
				.append(failures.isEmpty() ? "passed" : "failed")
				.append("\",\n")
				.append("  \"sampleType\": \"")
				.append(SAMPLE_TYPE)
				.append("\",\n")
				.append("  \"humanPlaytest\": false,\n")
				.append("  \"durationMeaning\": \"fixed_step_simulation_seconds\",\n")
				.append("  \"uidUniquenessScope\": \"per_isolated_profile\",\n")
				.append("  \"requiredSeedCount\": ")
				.append(REQUIRED_SEEDS)
				.append(",\n")
				.append("  \"uniqueSeedCount\": ")
				.append(uniqueSeedCount(evidence))
				.append(",\n")
				.append("  \"completedSeedCount\": ")
				.append(passedCount(evidence))
				.append(",\n")
				.append("  \"seeds\": [\n");
		for (int index = 0; index < evidence.size(); index++) {
			if (index > 0) json.append(",\n");
			json.append(evidence.get(index).toJson());
		}
		json.append("\n  ],\n")
				.append("  \"failures\": [");
		for (int index = 0; index < failures.size(); index++) {
			if (index > 0) json.append(',');
			json.append("\n    \"")
					.append(jsonEscape(failures.get(index)))
					.append('"');
		}
		if (!failures.isEmpty()) json.append('\n');
		json.append("  ]\n}\n");
		try (FileOutputStream output =
				new FileOutputStream(target, false)) {
			output.write(json.toString().getBytes(StandardCharsets.UTF_8));
			output.flush();
			output.getFD().sync();
		}
		return target;
	}

	private static int passedCount(List<SeedEvidence> evidence) {
		int count = 0;
		for (SeedEvidence sample : evidence) {
			if (sample.passes()) count++;
		}
		return count;
	}

	private static int uniqueSeedCount(List<SeedEvidence> evidence) {
		Set<Long> seeds = new HashSet<>();
		for (SeedEvidence sample : evidence) seeds.add(sample.seed);
		return seeds.size();
	}

	private static File evidenceDirectory() throws IOException {
		String configured = System.getProperty(
				"bukov.firstRaidWorldEvidenceDir");
		if (configured == null || configured.trim().isEmpty()) {
			configured = System.getenv(
					"BUKOV_FIRST_RAID_WORLD_EVIDENCE_DIR");
		}
		if (configured == null || configured.trim().isEmpty()) {
			configured = "build/reports/"
					+ "bukov-first-raid-world-seed-matrix";
		}
		File directory = new File(configured).getCanonicalFile();
		if (!directory.mkdirs() && !directory.isDirectory()) {
			throw new IOException(
					"unable to create evidence directory " + directory);
		}
		return directory;
	}

	private static String jsonEscape(String value) {
		if (value == null) return "";
		StringBuilder escaped = new StringBuilder();
		for (int index = 0; index < value.length(); index++) {
			char character = value.charAt(index);
			switch (character) {
				case '\\':
					escaped.append("\\\\");
					break;
				case '"':
					escaped.append("\\\"");
					break;
				case '\n':
					escaped.append("\\n");
					break;
				case '\r':
					escaped.append("\\r");
					break;
				case '\t':
					escaped.append("\\t");
					break;
				default:
					if (character < 0x20) {
						escaped.append(String.format(
								java.util.Locale.ROOT,
								"\\u%04x",
								(int)character));
					} else {
						escaped.append(character);
					}
			}
		}
		return escaped.toString();
	}

	private static final class SeedEvidence {
		private final int index;
		private final long seed;
		private boolean mapReachable;
		private String mapReachabilityReason = "";
		private int generatedEnemyCount;
		private boolean realCombat;
		private int liveKills;
		private int liveFirefights;
		private int liveDamageTaken;
		private int friendlyTracers;
		private int hostileTracers;
		private boolean gateInitiallyBlocked;
		private boolean archiveSearchPrompted;
		private boolean archiveCarried;
		private boolean archiveUidPresent;
		private boolean gateUnlockedByArchive;
		private boolean crossedUnlockedGate;
		private boolean highValueSearchPrompted;
		private boolean highValueLootCollected;
		private boolean missionCompleted;
		private boolean extractionPrompted;
		private boolean extractionCompleted;
		private boolean settlementSuccess;
		private boolean settlementMissionCompleted;
		private int settlementKills;
		private int transferredUidCount;
		private long transferredValue;
		private boolean archiveTransferred;
		private boolean duplicateRaidUid;
		private boolean duplicateAcrossRaidAndStash;
		private boolean duplicateSettlementUid;
		private boolean duplicateStashUid;
		private boolean balanceAvailable;
		private long balanceSeed;
		private String balanceMode = "";
		private String balanceThemeId = "";
		private String balanceRouteId = "";
		private float simulatedDurationSeconds;
		private int balanceContainerSearches;
		private int balanceFirefights;
		private int balanceKills;
		private int balanceDamageTaken;
		private long balanceExtractedValue;
		private String balanceEnd = "";
		private boolean balanceSettled;
		private String failure = "";

		private SeedEvidence(int index, long seed) {
			this.index = index;
			this.seed = seed;
		}

		private boolean passes() {
			return failure.isEmpty()
					&& mapReachable
					&& realCombat
					&& gateInitiallyBlocked
					&& archiveSearchPrompted
					&& archiveCarried
					&& archiveUidPresent
					&& gateUnlockedByArchive
					&& crossedUnlockedGate
					&& highValueSearchPrompted
					&& highValueLootCollected
					&& missionCompleted
					&& extractionPrompted
					&& extractionCompleted
					&& settlementSuccess
					&& settlementMissionCompleted
					&& settlementKills == 1
					&& transferredUidCount > 0
					&& transferredValue > 0L
					&& archiveTransferred
					&& !duplicateRaidUid
					&& !duplicateAcrossRaidAndStash
					&& !duplicateSettlementUid
					&& !duplicateStashUid
					&& balanceAvailable
					&& balanceSettled
					&& balanceSeed == seed
					&& BukovRaidMode.EXPEDITION.name()
							.equals(balanceMode)
					&& THEME_ID.equals(balanceThemeId)
					&& simulatedDurationSeconds > 0f
					&& balanceContainerSearches >= 2
					&& balanceFirefights >= 1
					&& balanceKills == 1
					&& balanceDamageTaken > 0
					&& balanceExtractedValue == transferredValue
					&& RaidBalanceTelemetry.End.BASIC_EXTRACTION.name()
							.equals(balanceEnd);
		}

		private String failureReason() {
			if (!failure.isEmpty()) return failure;
			return "evidence predicate failed";
		}

		private String toJson() {
			return "    {"
					+ "\"index\":" + index
					+ ",\"seed\":" + seed
					+ ",\"sampleType\":\"" + SAMPLE_TYPE + "\""
					+ ",\"humanPlaytest\":false"
					+ ",\"mapReachable\":" + mapReachable
					+ ",\"mapReachabilityReason\":\""
							+ jsonEscape(mapReachabilityReason) + "\""
					+ ",\"generatedEnemyCount\":" + generatedEnemyCount
					+ ",\"realCombat\":" + realCombat
					+ ",\"liveKills\":" + liveKills
					+ ",\"liveFirefights\":" + liveFirefights
					+ ",\"liveDamageTaken\":" + liveDamageTaken
					+ ",\"friendlyTracers\":" + friendlyTracers
					+ ",\"hostileTracers\":" + hostileTracers
					+ ",\"gateInitiallyBlocked\":" + gateInitiallyBlocked
					+ ",\"archiveSearchPrompted\":"
							+ archiveSearchPrompted
					+ ",\"archiveCarried\":" + archiveCarried
					+ ",\"archiveUidPresent\":" + archiveUidPresent
					+ ",\"gateUnlockedByArchive\":"
							+ gateUnlockedByArchive
					+ ",\"crossedUnlockedGate\":" + crossedUnlockedGate
					+ ",\"highValueSearchPrompted\":"
							+ highValueSearchPrompted
					+ ",\"highValueLootCollected\":"
							+ highValueLootCollected
					+ ",\"missionCompleted\":" + missionCompleted
					+ ",\"extractionPrompted\":" + extractionPrompted
					+ ",\"extractionCompleted\":" + extractionCompleted
					+ ",\"settlementSuccess\":" + settlementSuccess
					+ ",\"settlementMissionCompleted\":"
							+ settlementMissionCompleted
					+ ",\"settlementKills\":" + settlementKills
					+ ",\"transferredUidCount\":" + transferredUidCount
					+ ",\"transferredValue\":" + transferredValue
					+ ",\"archiveTransferred\":" + archiveTransferred
					+ ",\"duplicateRaidUid\":" + duplicateRaidUid
					+ ",\"duplicateAcrossRaidAndStash\":"
							+ duplicateAcrossRaidAndStash
					+ ",\"duplicateSettlementUid\":"
							+ duplicateSettlementUid
					+ ",\"duplicateStashUid\":" + duplicateStashUid
					+ ",\"balance\":{\"available\":" + balanceAvailable
					+ ",\"settled\":" + balanceSettled
					+ ",\"seed\":" + balanceSeed
					+ ",\"mode\":\"" + jsonEscape(balanceMode) + "\""
					+ ",\"themeId\":\""
							+ jsonEscape(balanceThemeId) + "\""
					+ ",\"routeId\":\""
							+ jsonEscape(balanceRouteId) + "\""
					+ ",\"simulatedDurationSeconds\":"
							+ simulatedDurationSeconds
					+ ",\"containerSearches\":"
							+ balanceContainerSearches
					+ ",\"firefights\":" + balanceFirefights
					+ ",\"kills\":" + balanceKills
					+ ",\"damageTaken\":" + balanceDamageTaken
					+ ",\"extractedValue\":"
							+ balanceExtractedValue
					+ ",\"end\":\"" + jsonEscape(balanceEnd) + "\"}"
					+ ",\"passed\":" + passes()
					+ ",\"failure\":\"" + jsonEscape(failure) + "\""
					+ '}';
		}
	}
}
