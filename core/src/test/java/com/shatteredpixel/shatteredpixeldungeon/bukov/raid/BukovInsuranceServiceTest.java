package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.watabou.utils.Bundle;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovInsuranceServiceTest {

	@Test
	public void onlyLostInsuredDeploymentItemsReturnAfterAnotherSettlement() {
		BukovProfile profile = new BukovProfile();
		LootTransaction death = new LootTransaction("insured-death", 100f);
		death.pickup(item("insured", false, true, 900));
		death.pickup(item("uninsured", false, false, 700));
		death.pickup(item("found-insured", true, true, 500));

		new RaidSettlement().settle(
				profile, death, RaidOutcome.DEATH,
				20f, 1, false, BukovRaidMode.EXPEDITION);

		assertEquals(1, profile.insurance().pendingCount());
		assertEquals(3, profile.settlement("insured-death").lostItems().size());
		assertTrue(new BukovInsuranceService()
				.claimAvailable(profile).returnedItemUids.isEmpty());
		new RaidSettlement().settle(
				profile,
				new LootTransaction("training-does-not-delay", 100f),
				RaidOutcome.SUCCESS,
				10f, 10, false, BukovRaidMode.TRAINING_GROUND);
		assertTrue("training cannot mature an insurance return",
				new BukovInsuranceService()
						.claimAvailable(profile).returnedItemUids.isEmpty());

		new RaidSettlement().settle(
				profile,
				new LootTransaction("delay-raid", 100f),
				RaidOutcome.SUCCESS,
				10f, 0, false, BukovRaidMode.EXPEDITION);
		BukovInsuranceService.ClaimResult claimed =
				new BukovInsuranceService().claimAvailable(profile);

		assertEquals(1, claimed.returnedItemUids.size());
		assertEquals("insured", claimed.returnedItemUids.get(0));
		assertEquals(900L, claimed.returnedValue);
		assertTrue(profile.stash().contains("insured"));
		assertFalse(profile.stash().item("insured").insured());
		assertFalse(profile.stash().contains("uninsured"));
		assertFalse(profile.stash().contains("found-insured"));
		assertTrue(new BukovInsuranceService()
				.claimAvailable(profile).returnedItemUids.isEmpty());
	}

	@Test
	public void pendingAndClaimedReturnsSurviveProfileRoundTrip() {
		BukovProfile profile = new BukovProfile();
		LootTransaction death = new LootTransaction("persist-insurance", 100f);
		death.pickup(item("insured-persist", false, true, 300));
		new RaidSettlement().settle(
				profile, death, RaidOutcome.DEATH,
				1f, 0, false, BukovRaidMode.EXPEDITION);

		Bundle bundle = new Bundle();
		bundle.put("profile", profile);
		BukovProfile restored = (BukovProfile) bundle.get("profile");

		assertEquals(BukovProfile.CURRENT_VERSION, restored.profileVersion());
		assertEquals(1, restored.insurance().pendingCount());
		assertEquals(
				"persist-insurance",
				restored.insurance().returns().get(0).sourceRaidId());
	}

	private static RaidItem item(
			String uid,
			boolean foundInRaid,
			boolean insured,
			int value) {
		return new RaidItem(
				uid, "gear:" + uid, 1, 1f, value,
				foundInRaid, insured, 1f);
	}
}
