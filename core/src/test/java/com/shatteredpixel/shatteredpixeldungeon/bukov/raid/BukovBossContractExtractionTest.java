package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.WhiteLineBossStateMachine;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovHubController;

import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Proves Boss contracts stay optional without losing durable completion. */
public class BukovBossContractExtractionTest {

	@Test
	public void basicExtractionSucceedsWithoutBossAndReportsContractFailure()
			throws IOException {
		InMemoryBukovSaveService saves =
				new InMemoryBukovSaveService();
		BukovHubController hub = new BukovHubController(saves);
		hub.selectRaidMode(BukovRaidMode.BOSS_CONTRACT);
		hub.confirmDeployment();

		BukovRaidCoordinator raid = BukovRaidCoordinator.start(
				saves,
				91024L,
				"boss-contract-lock",
				BukovHubController.FIRST_RAID_WEIGHT_LIMIT,
				Collections.singletonList(ExtractionState.basic()));

		assertTrue(raid.bossContractRequired());
		assertFalse(raid.bossContractCompleted());
		assertTrue(raid.beginExtraction("E01"));
		raid.tick(5f, ExtractionState.Interaction.ACTIVE);
		raid.saveCheckpoint();
		BukovRaidCoordinator resumed =
				BukovRaidCoordinator.resume(saves);
		assertFalse(resumed.bossContractCompleted());
		assertTrue(resumed.extraction("E01").completed());
		RaidResult result = resumed.settleSuccess();
		assertEquals(RaidOutcome.SUCCESS, result.outcome());
		assertFalse(result.missionCompleted());
		assertFalse(
				saves.loadProfile()
						.settlement("boss-contract-lock")
						.missionCompleted());
	}

	@Test
	public void bossDefeatAwardsContractCompletionAndSurvivesResume()
			throws IOException {
		InMemoryBukovSaveService saves =
				new InMemoryBukovSaveService();
		BukovHubController hub = new BukovHubController(saves);
		hub.selectRaidMode(BukovRaidMode.BOSS_CONTRACT);
		hub.confirmDeployment();
		BukovRaidCoordinator raid = BukovRaidCoordinator.start(
				saves,
				91026L,
				"boss-contract-complete",
				BukovHubController.FIRST_RAID_WEIGHT_LIMIT,
				Collections.singletonList(ExtractionState.basic()));

		assertTrue(raid.markBossContractCompleted());
		raid.saveCheckpoint();
		BukovRaidCoordinator resumed =
				BukovRaidCoordinator.resume(saves);
		assertTrue(resumed.bossContractCompleted());
		assertTrue(resumed.beginExtraction("E01"));
		resumed.tick(5f, ExtractionState.Interaction.ACTIVE);
		RaidResult result = resumed.settleSuccess();
		assertEquals(RaidOutcome.SUCCESS, result.outcome());
		assertTrue(result.missionCompleted());
	}

	@Test
	public void legacyBypassedBossAllowsExtractionButFailsContract()
			throws IOException {
		InMemoryBukovSaveService saves =
				startBossContract("legacy-bypassed", 91027L);
		BukovRaidCoordinator raid = BukovRaidCoordinator.resume(saves);

		assertFalse(raid.reconcileLegacyBossContractPhase(
				WhiteLineBossStateMachine.Phase.BYPASSED));
		assertFalse(raid.bossContractCompleted());
		assertTrue(raid.beginExtraction("E01"));
		raid.tick(5f, ExtractionState.Interaction.ACTIVE);
		assertFalse(raid.settleSuccess().missionCompleted());
	}

	@Test
	public void legacyDefeatedBossMigratesContractCompletion()
			throws IOException {
		InMemoryBukovSaveService saves =
				startBossContract("legacy-defeated", 91028L);
		BukovRaidCoordinator raid = BukovRaidCoordinator.resume(saves);

		assertTrue(raid.reconcileLegacyBossContractPhase(
				WhiteLineBossStateMachine.Phase.DEFEATED));
		raid.saveCheckpoint();
		BukovRaidCoordinator resumed =
				BukovRaidCoordinator.resume(saves);
		assertTrue(resumed.bossContractCompleted());
		assertTrue(resumed.beginExtraction("E01"));
		resumed.tick(5f, ExtractionState.Interaction.ACTIVE);
		assertTrue(resumed.settleSuccess().missionCompleted());
	}

	@Test
	public void ordinaryModesCannotForgeBossCompletion()
			throws IOException {
		InMemoryBukovSaveService saves =
				new InMemoryBukovSaveService();
		BukovRaidCoordinator raid = BukovRaidCoordinator.start(
				saves,
				91025L,
				"ordinary-contract",
				BukovHubController.FIRST_RAID_WEIGHT_LIMIT,
				Collections.singletonList(ExtractionState.basic()));

		try {
			raid.markBossContractCompleted();
			fail("non-Boss modes must not forge the boss objective");
		} catch (IllegalStateException expected) {
			assertTrue(expected.getMessage().contains("Boss Contract"));
		}
	}

	private static InMemoryBukovSaveService startBossContract(
			String raidId,
			long seed) throws IOException {
		InMemoryBukovSaveService saves =
				new InMemoryBukovSaveService();
		BukovHubController hub = new BukovHubController(saves);
		hub.selectRaidMode(BukovRaidMode.BOSS_CONTRACT);
		hub.confirmDeployment();
		BukovRaidCoordinator.start(
				saves,
				seed,
				raidId,
				BukovHubController.FIRST_RAID_WEIGHT_LIMIT,
				Collections.singletonList(ExtractionState.basic()));
		return saves;
	}
}
