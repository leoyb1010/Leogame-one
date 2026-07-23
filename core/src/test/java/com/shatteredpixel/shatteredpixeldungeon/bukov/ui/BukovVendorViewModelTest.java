package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovEconomyService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovVendorCatalog;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovVendorViewModelTest {

	@Test
	public void exposesCatalogCashAndAffordability() {
		BukovProfile profile = new BukovProfile();
		profile.setCurrency(1_000L);

		BukovVendorViewModel model = BukovVendorViewModel.from(
				profile,
				BukovVendorCatalog.all(),
				false);

		assertEquals(1_000L, model.currency);
		assertEquals(BukovVendorCatalog.all().size(), model.offers.size());
		assertTrue(model.offers.get(0).affordable);
		assertFalse(model.offers.get(model.offers.size() - 1).affordable);
		assertFalse(model.tradingLocked);
	}

	@Test
	public void sellViewListsOnlyUnequippedWarehouseItems() {
		BukovProfile profile = new BukovProfile();
		RaidItem equipped = item("equipped", "firearm:needle_9", 850);
		RaidItem loot = item("loot", "encrypted_drive", 2_400);
		RaidItem provision = item(
				"provision:free",
				"ammo:ammo_9_standard",
				120);
		profile.stash().deposit(equipped);
		profile.stash().deposit(loot);
		profile.stash().deposit(provision);
		profile.loadout().select(equipped.itemUid(), profile.stash());

		BukovVendorViewModel model = BukovVendorViewModel.from(
				profile,
				BukovVendorCatalog.all(),
				false);

		assertEquals(2, model.stash.size());
		assertEquals("loot", model.stash.get(0).itemUid);
		assertTrue(model.stash.get(0).sellable);
		assertEquals(
				BukovEconomyService.appraisal(loot),
				model.stash.get(0).price);
		assertEquals("provision:free", model.stash.get(1).itemUid);
		assertFalse(model.stash.get(1).sellable);
		assertEquals(0L, model.stash.get(1).price);
	}

	private static RaidItem item(
			String uid,
			String definition,
			int unitValue) {
		return new RaidItem(
				uid,
				definition,
				1,
				0.2f,
				unitValue,
				false,
				false,
				1f);
	}
}
