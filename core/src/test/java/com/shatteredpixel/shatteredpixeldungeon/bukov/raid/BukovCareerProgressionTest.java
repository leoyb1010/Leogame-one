package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovCareerProgressionTest {

	@Test
	public void successfulMissionAndExtractionMilestonesUnlockWholeChain() {
		BukovProfile profile = new BukovProfile();

		assertTrue(BukovCareerProgression.reconcile(profile));
		assertEquals(1, profile.unlockedMaps().size());
		assertTrue(profile.unlockedMaps().contains("fog_depot"));
		assertEquals(
				BukovMessages.get(
						"bukov.economy.hub.contract_rust_workshop_title"),
				BukovCareerProgression.snapshot(profile).activeContract);

		settleSuccess(profile, "career-1", 500, true);
		assertTrue(profile.completedContracts().contains(
				FirstRaidMission.EVENT_ID));
		assertTrue(profile.unlockedMaps().contains("rust_workshop"));

		settleSuccess(profile, "career-2", 500, false);
		assertTrue(profile.completedContracts().contains(
				BukovCareerProgression.SAFE_RETURN));
		assertTrue(profile.unlockedMaps().contains("flooded_passage"));

		settleSuccess(profile, "career-3", 2500, false);
		assertTrue(profile.completedContracts().contains(
				BukovCareerProgression.FIELD_SUPPLIER));
		assertTrue(profile.unlockedMaps().contains("overgrown_yard"));

		settleSuccess(profile, "career-4", 5000, false);
		assertTrue(profile.completedContracts().contains(
				BukovCareerProgression.WHITE_LINE_HUNT));
		assertTrue(profile.unlockedMaps().contains("cold_storage"));

		settleSuccess(profile, "career-5", 7000, false);
		assertTrue(profile.completedContracts().contains(
				BukovCareerProgression.SEALED_LAB_CLEARANCE));
		assertTrue(profile.unlockedMaps().contains("sealed_lab"));

		BukovCareerProgression.Snapshot complete =
				BukovCareerProgression.snapshot(profile);
		assertEquals(5, complete.completedContracts);
		assertEquals(5, complete.totalContracts);
		assertEquals(6, complete.unlockedMaps);
		assertEquals(6, complete.totalMaps);
		assertEquals(
				BukovMessages.get(
						"bukov.economy.hub.contract_complete_title"),
				complete.activeContract);
	}

	@Test
	public void deathAndTrainingCannotAdvanceCareer() {
		BukovProfile deathProfile = new BukovProfile();
		new RaidSettlement().settle(
				deathProfile,
				loot("death", 50000),
				RaidOutcome.DEATH,
				90f,
				20,
				true,
				BukovRaidMode.EXPEDITION);
		assertFalse(deathProfile.completedContracts().contains(
				FirstRaidMission.EVENT_ID));
		assertEquals(1, deathProfile.unlockedMaps().size());

		BukovProfile trainingProfile = new BukovProfile();
		new RaidSettlement().settle(
				trainingProfile,
				loot("training", 50000),
				RaidOutcome.SUCCESS,
				90f,
				20,
				true,
				BukovRaidMode.TRAINING_GROUND);
		assertTrue(trainingProfile.completedContracts().isEmpty());
		assertTrue(trainingProfile.unlockedMaps().isEmpty());
		assertEquals(0, trainingProfile.statistics().successfulRaids());
	}

	@Test
	public void reconciliationIsIdempotentAndPreservesLegacyMapIds() {
		BukovProfile profile = new BukovProfile();
		profile.unlockMap("legacy_first_level");

		assertTrue(BukovCareerProgression.reconcile(profile));
		assertFalse(BukovCareerProgression.reconcile(profile));
		assertTrue(profile.unlockedMaps().contains("legacy_first_level"));
		assertEquals(
				1,
				BukovCareerProgression.availableMapIds(profile).size());
		assertEquals(
				"fog_depot",
				BukovCareerProgression.availableMapIds(profile).get(0));
	}

	private static void settleSuccess(
			BukovProfile profile,
			String raidId,
			int value,
			boolean missionCompleted) {
		new RaidSettlement().settle(
				profile,
				loot(raidId, value),
				RaidOutcome.SUCCESS,
				120f,
				3,
				missionCompleted,
				BukovRaidMode.EXPEDITION);
	}

	private static LootTransaction loot(String raidId, int value) {
		LootTransaction result = new LootTransaction(raidId, 100f);
		assertEquals(
				LootTransaction.PickupResult.ADDED,
				result.pickup(new RaidItem(
						raidId + "-loot",
						"valuable:test",
						1,
						0.1f,
						value,
						true,
						false,
						1f)));
		return result;
	}
}
