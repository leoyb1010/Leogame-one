package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovVendorCatalogProgressionTest {

	@Test
	public void newAndUnreconciledProfilesReceiveOnlyAUsableStarterPool() {
		BukovProfile profile = new BukovProfile();

		Set<String> offers = offerIds(
				BukovVendorCatalog.availableFor(profile));

		assertEquals(8, offers.size());
		assertTrue(offers.contains("firearm_needle_9"));
		assertTrue(offers.contains("firearm_sentinel_9"));
		assertTrue(offers.contains("firearm_shuttle_9"));
		assertTrue(offers.contains("ammo_9_standard_24"));
		assertTrue(offers.contains("ammo_9_training_30"));
		assertTrue(offers.contains("bandage_1"));
		assertTrue(offers.contains("soft_vest_1"));
		assertTrue(offers.contains("scout_pack_1"));
		assertFalse(offers.contains("firearm_frontier_762"));
		assertFalse(offers.contains("ceramic_rig_1"));
	}

	@Test
	public void campaignMapsUnlockMonotonicStockTiers() {
		BukovProfile profile = new BukovProfile();
		int[] expectedCounts = {8, 15, 21, 26, 30, 32};
		List<String> maps = BukovCareerProgression.allMapIds();

		for (int tier = 0; tier < maps.size(); tier++) {
			profile.unlockMap(maps.get(tier));
			assertEquals(
					"Unexpected vendor size at " + maps.get(tier),
					expectedCounts[tier],
					BukovVendorCatalog.availableFor(profile).size());
		}

		assertEquals(
				BukovVendorCatalog.all().size(),
				BukovVendorCatalog.availableFor(profile).size());
	}

	@Test
	public void retainedLateMapInLegacySaveAlsoUnlocksEarlierStock() {
		BukovProfile profile = new BukovProfile();
		profile.unlockMap("cold_storage");

		Set<String> offers = offerIds(
				BukovVendorCatalog.availableFor(profile));

		assertEquals(30, offers.size());
		assertTrue(offers.contains("firearm_needle_9"));
		assertTrue(offers.contains("firearm_longstreet_762"));
		assertTrue(offers.contains("ceramic_rig_1"));
		assertFalse(offers.contains("firearm_frontier_762"));
	}

	@Test
	public void everyOfferUsesARegisteredCampaignMapRequirement() {
		Set<String> maps = new HashSet<>(
				BukovCareerProgression.allMapIds());

		for (BukovVendorCatalog.Offer offer : BukovVendorCatalog.all()) {
			assertTrue(
					"Unknown vendor map requirement: "
							+ offer.offerId + " -> " + offer.requiredMapId,
					maps.contains(offer.requiredMapId));
		}
	}

	private static Set<String> offerIds(
			List<BukovVendorCatalog.Offer> offers) {
		Set<String> result = new HashSet<>();
		for (BukovVendorCatalog.Offer offer : offers) {
			assertTrue("Duplicate available offer", result.add(offer.offerId));
		}
		return result;
	}
}
