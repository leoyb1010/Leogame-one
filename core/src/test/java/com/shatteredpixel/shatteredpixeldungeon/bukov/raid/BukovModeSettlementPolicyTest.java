package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BukovModeSettlementPolicyTest {

	@Test
	public void quickSweepProtectsHighestValueDeploymentOnly() {
		BukovProfile profile = new BukovProfile();
		LootTransaction loot = new LootTransaction("quick-death", 40f);
		loot.pickup(item("weapon", 900, false));
		loot.pickup(item("ammo", 120, false));
		loot.pickup(item("found", 1600, true));

		RaidResult result = new RaidSettlement().settle(
				profile,
				loot,
				RaidOutcome.DEATH,
				80f,
				1,
				false,
				BukovRaidMode.QUICK_SWEEP);

		assertTrue(profile.stash().contains("weapon"));
		assertFalse(profile.stash().contains("ammo"));
		assertFalse(profile.stash().contains("found"));
		assertEquals(1, result.transferredUids().size());
		assertEquals(2, result.lostUids().size());
		assertEquals(1720L, result.lostValue());
		assertEquals(1720L, profile.statistics().lostValue());
	}

	@Test
	public void expeditionAndBossContractKeepFullLoss() {
		for (BukovRaidMode mode : new BukovRaidMode[] {
				BukovRaidMode.EXPEDITION,
				BukovRaidMode.BOSS_CONTRACT}) {
			BukovProfile profile = new BukovProfile();
			LootTransaction loot = new LootTransaction(
					"full-loss-" + mode.name(), 40f);
			loot.pickup(item("weapon-" + mode.name(), 900, false));

			RaidResult result = new RaidSettlement().settle(
					profile, loot, RaidOutcome.DEATH,
					20f, 0, false, mode);

			assertEquals(0, profile.stash().distinctItemCount());
			assertEquals(1, result.lostUids().size());
		}
	}

	@Test
	public void scavengerDiscardsAccidentalOwnedGearAndLosesFoundLootOnDeath() {
		BukovProfile profile = new BukovProfile();
		LootTransaction loot = new LootTransaction("cloth-death", 40f);
		loot.pickup(item("owned", 300, false));
		loot.pickup(item("found", 600, true));

		RaidResult result = new RaidSettlement().settle(
				profile,
				loot,
				RaidOutcome.DEATH,
				30f,
				0,
				false,
				BukovRaidMode.SCAVENGER);

		assertFalse(profile.stash().contains("owned"));
		assertFalse(profile.stash().contains("found"));
		assertEquals(0, result.transferredUids().size());
		assertEquals(1, result.lostUids().size());
		assertEquals(600L, result.lostValue());
	}

	@Test
	public void settledRaidCannotBeReplayedUnderAnotherMode() {
		BukovProfile profile = new BukovProfile();
		LootTransaction loot = new LootTransaction("mode-locked", 40f);
		loot.pickup(item("found", 100, true));
		new RaidSettlement().settle(
				profile, loot, RaidOutcome.SUCCESS,
				30f, 0, false, BukovRaidMode.QUICK_SWEEP);
		try {
			new RaidSettlement().settle(
					profile, loot, RaidOutcome.SUCCESS,
					30f, 0, false, BukovRaidMode.BOSS_CONTRACT);
			fail("mode change must alter settlement fingerprint");
		} catch (IllegalStateException expected) {
			assertTrue(expected.getMessage().contains("payload changed"));
		}
	}

	@Test
	public void trainingSettlementNeverMutatesStashContractsOrStatistics() {
		for (RaidOutcome outcome : RaidOutcome.values()) {
			BukovProfile profile = new BukovProfile();
			profile.setCurrency(7_500L);
			profile.stash().deposit(item(
					"safe-" + outcome.name(), 900, false));
			LootTransaction loot = new LootTransaction(
					"training-" + outcome.name(), 40f);
			loot.pickup(item(
					"practice-" + outcome.name(), 1_600, true));

			RaidResult result = new RaidSettlement().settle(
					profile,
					loot,
					outcome,
					180f,
					4,
					true,
					BukovRaidMode.TRAINING_GROUND);

			assertEquals(1, profile.stash().distinctItemCount());
			assertTrue(profile.stash().contains(
					"safe-" + outcome.name()));
			assertEquals(7_500L, profile.currency());
			assertEquals(0, profile.statistics().successfulRaids());
			assertEquals(0, profile.statistics().deaths());
			assertEquals(0L, profile.statistics().extractedValue());
			assertEquals(0L, profile.statistics().lostValue());
			assertFalse(profile.completedContracts().contains(
					FirstRaidMission.EVENT_ID));
			assertEquals(0L, result.transferredQuantity());
			assertEquals(0L, result.lostQuantity());

			RaidResult replay = new RaidSettlement().settle(
					profile,
					loot,
					outcome,
					180f,
					4,
					true,
					BukovRaidMode.TRAINING_GROUND);
			assertTrue(replay.replayed());
			assertEquals(1, profile.stash().distinctItemCount());
		}
	}

	private static RaidItem item(
			String uid, int value, boolean foundInRaid) {
		return new RaidItem(
				uid,
				"def:" + uid,
				1,
				1f,
				value,
				foundInRaid,
				false,
				1f);
	}
}
