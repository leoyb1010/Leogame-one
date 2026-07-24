package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.watabou.utils.Bundle;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovLongTermContractServiceTest {

	@Test
	public void settlementProgressRewardAndReplayAreIdempotent() {
		BukovProfile profile = new BukovProfile();
		RaidSettlement settlement = new RaidSettlement();
		LootTransaction last = null;
		for (int index = 0; index < 3; index++) {
			last = loot("contract-" + index, "loot-" + index, 5000);
			settlement.settle(
					profile, last, RaidOutcome.SUCCESS,
					60f, 9, false, BukovRaidMode.EXPEDITION);
		}

		assertTrue(profile.longTermContracts()
				.progress(BukovLongTermContractCatalog.SURVIVOR).ready());
		assertTrue(profile.longTermContracts()
				.progress(BukovLongTermContractCatalog.SUPPLIER).ready());
		assertTrue(profile.longTermContracts()
				.progress(BukovLongTermContractCatalog.HUNTER).ready());
		assertFalse(profile.longTermContracts()
				.progress(BukovLongTermContractCatalog.VETERAN).ready());

		long before = profile.currency();
		BukovLongTermContractService service =
				new BukovLongTermContractService();
		BukovLongTermContractService.ClaimResult first = service.claim(
				profile, BukovLongTermContractCatalog.SURVIVOR);
		BukovLongTermContractService.ClaimResult repeated = service.claim(
				profile, BukovLongTermContractCatalog.SURVIVOR);

		assertEquals(
				BukovLongTermContractService.ClaimStatus.CLAIMED,
				first.status);
		assertEquals(600L, first.currencyGranted);
		assertEquals(before + 600L, profile.currency());
		assertEquals(
				BukovLongTermContractService.ClaimStatus.ALREADY_CLAIMED,
				repeated.status);
		assertEquals(0L, repeated.currencyGranted);
		assertEquals(before + 600L, profile.currency());

		RaidResult replay = settlement.settle(
				profile, last, RaidOutcome.SUCCESS,
				60f, 9, false, BukovRaidMode.EXPEDITION);
		assertTrue(replay.replayed());
		assertEquals(3L, profile.longTermContracts()
				.progress(BukovLongTermContractCatalog.SURVIVOR)
				.progress());
	}

	@Test
	public void trainingDoesNotAdvanceAndProfileRoundTripPreservesClaim() {
		BukovProfile profile = new BukovProfile();
		new RaidSettlement().settle(
				profile,
				loot("training-contract", "training-loot", 99999),
				RaidOutcome.SUCCESS,
				30f, 99, true, BukovRaidMode.TRAINING_GROUND);
		assertEquals(0L, profile.longTermContracts()
				.progress(BukovLongTermContractCatalog.SUPPLIER)
				.progress());
		assertEquals(0L, profile.longTermContracts()
				.progress(BukovLongTermContractCatalog.HUNTER)
				.progress());

		Bundle stored = new Bundle();
		stored.put("profile", profile);
		BukovProfile restored = (BukovProfile) stored.get("profile");

		assertEquals(BukovProfile.CURRENT_VERSION, restored.profileVersion());
		assertEquals(4, restored.longTermContracts().allProgress().size());
	}

	@Test
	public void versionSixProfileMigratesWithSafeEmptyLongTermState() {
		BukovProfile source = new BukovProfile();
		source.setCurrency(77L);
		Bundle legacy = new Bundle();
		source.storeInBundle(legacy);
		legacy.put("profile_version", 6);
		legacy.remove("insurance");
		legacy.remove("long_term_contracts");
		legacy.remove("firearm_builds");

		BukovProfile restored = new BukovProfile();
		restored.restoreFromBundle(legacy);

		assertEquals(BukovProfile.CURRENT_VERSION, restored.profileVersion());
		assertEquals(77L, restored.currency());
		assertEquals(0, restored.insurance().pendingCount());
		assertEquals(0, restored.firearmBuilds().size());
		assertEquals(4, restored.longTermContracts().allProgress().size());
		assertEquals(0L, restored.longTermContracts()
				.progress(BukovLongTermContractCatalog.VETERAN)
				.progress());
	}

	private static LootTransaction loot(
			String raidId, String itemUid, int value) {
		LootTransaction result = new LootTransaction(raidId, 100f);
		result.pickup(new RaidItem(
				itemUid, "loot:" + itemUid,
				1, 1f, value, true, false, 1f));
		return result;
	}
}
