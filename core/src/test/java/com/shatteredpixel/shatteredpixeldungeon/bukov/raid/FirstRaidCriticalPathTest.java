package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FirstRaidCriticalPathTest {

	@Test
	public void extractionRequiresArchiveThenHighValueSearchAndSurvivesResume()
			throws IOException {
		InMemoryBukovSaveService saves = new InMemoryBukovSaveService();
		BukovRaidCoordinator raid = start(saves, "critical-path");

		assertTrue(raid.firstRaidMissionActive());
		assertEquals(
				FirstRaidMission.Stage.RECOVER_ARCHIVE,
				raid.firstRaidStage());
		assertEquals(
				FirstRaidMission.LOCKED_OBJECTIVE,
				raid.firstRaidObjective());
		assertTrue(raid.beginExtraction("E01"));
		raid.cancelExtraction();
		assertFalse(raid.beginExtraction("E02"));
		assertEquals(
				BukovSearchableContainer.State.LOCKED,
				raid.container("L01").state);

		assertTrue(raid.completeEvent(FirstRaidMission.EVENT_ID));
		assertEquals(
				FirstRaidMission.Stage.SECURE_HIGH_VALUE_CACHE,
				raid.firstRaidStage());
		assertEquals(
				FirstRaidMission.HIGH_VALUE_OBJECTIVE,
				raid.firstRaidObjective());
		assertFalse(raid.beginExtraction("E02"));
		assertEquals(
				BukovSearchableContainer.State.UNSEARCHED,
				raid.container("L01").state);
		raid.setExtractionCondition("E02", true);
		assertFalse(raid.beginExtraction("E02"));

		assertTrue(raid.beginContainerSearch("L01"));
		assertEquals(
				BukovSearchableContainer.UpdateResult.COMPLETED,
				raid.updateContainerSearch(
						"L01",
						2f,
						true,
						false,
						false,
						highValueTable()));
		assertEquals(
				FirstRaidMission.Stage.EXTRACT,
				raid.firstRaidStage());
		assertEquals(
				FirstRaidMission.UNLOCKED_OBJECTIVE,
				raid.firstRaidObjective());
		assertTrue(raid.firstRaidConditionalExtractionUnlocked());
		raid.saveCheckpoint();

		BukovRaidCoordinator resumed =
				BukovRaidCoordinator.resume(saves);
		assertNotNull(resumed);
		assertTrue(resumed.eventCompleted(FirstRaidMission.EVENT_ID));
		assertTrue(resumed.eventCompleted(
				FirstRaidMission.HIGH_VALUE_EVENT_ID));
		assertEquals(
				FirstRaidMission.Stage.EXTRACT,
				resumed.firstRaidStage());
		assertTrue(resumed.extraction("E02").conditionMet());
		assertTrue(resumed.beginExtraction("E02"));
	}

	@Test
	public void highValueCacheCannotBeSearchedBeforeArchive()
			throws IOException {
		BukovRaidCoordinator raid = start(
				new InMemoryBukovSaveService(),
				"early-cache");

		assertFalse(raid.beginContainerSearch("L01"));
		assertEquals(
				FirstRaidMission.Stage.RECOVER_ARCHIVE,
				raid.firstRaidStage());
		assertFalse(raid.beginExtraction("E02"));

		assertTrue(raid.completeEvent(FirstRaidMission.EVENT_ID));
		assertTrue(raid.beginContainerSearch("L01"));
	}

	@Test
	public void legacyAndTrainingSessionsRemainExtractable()
			throws IOException {
		BukovRaidCoordinator legacy = BukovRaidCoordinator.start(
				new InMemoryBukovSaveService(),
				7L,
				"legacy",
				20f,
				Collections.singletonList(ExtractionState.basic()));
		assertFalse(legacy.firstRaidMissionActive());
		assertTrue(legacy.beginExtraction("E01"));

		InMemoryBukovSaveService trainingSaves =
				new InMemoryBukovSaveService();
		BukovProfile profile = trainingSaves.loadProfile();
		profile.selectRaidMode(BukovRaidMode.TRAINING_GROUND);
		trainingSaves.saveProfile(profile);
		BukovRaidCoordinator training = start(
				trainingSaves,
				"training");
		assertFalse(training.firstRaidMissionActive());
		assertTrue(training.beginExtraction("E01"));
	}

	private static BukovRaidCoordinator start(
			InMemoryBukovSaveService saves,
			String raidId) throws IOException {
		return BukovRaidCoordinator.start(
				saves,
				441199L,
				raidId,
				40f,
				Arrays.asList(
						ExtractionState.basic(),
						ExtractionState.conditional()),
				Arrays.asList(
						new BukovContainerDefinition(
								FirstRaidMission.ARCHIVE_CONTAINER_ID,
								31,
								FirstRaidMission.ARCHIVE_LOOT_TABLE_ID,
								1,
								1.4f,
								false),
						new BukovContainerDefinition(
								"L01",
								87,
								FirstRaidMission.HIGH_VALUE_LOOT_TABLE_ID,
								2,
								2f,
								false)));
	}

	private static BukovLootTable highValueTable() {
		return new BukovLootTable(
				FirstRaidMission.HIGH_VALUE_LOOT_TABLE_ID,
				Collections.singletonList(
						new BukovLootTable.Entry(
								"critical-cache",
								1,
								1,
								1,
								TestItem::new)));
	}

	public static class TestItem extends Item {
	}
}
