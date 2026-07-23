package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

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
	public void scavengerRecoversAnyAccidentalOwnedGearButNotFoundLoot() {
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

		assertTrue(profile.stash().contains("owned"));
		assertFalse(profile.stash().contains("found"));
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
