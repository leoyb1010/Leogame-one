package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.LootTransaction;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BukovBackpackViewModelTest {

	@Test
	public void presentsRaidCategoriesEconomyAndRuntimeWeaponState() {
		LootTransaction ledger = new LootTransaction("raid-01", 40f);
		pickup(ledger, item(
				"gun-a", "firearm:needle_9",
				1, 0.9f, 850, false, 0.78f));
		pickup(ledger, item(
				"gun-b", "firearm:shuttle_9",
				1, 2.2f, 2100, true, 0.51f));
		pickup(ledger, item(
				"ammo-a", "ammo:ammo_9_standard",
				18, 0.012f, 12, false, 1f));
		pickup(ledger, item(
				"med-a", "first_aid",
				2, 0.75f, 680, true, 1f));
		pickup(ledger, item(
				"mission-a", FirstRaidMission.ARCHIVE_DEFINITION_ID,
				1, 0f, 0, true, 1f));

		BukovBackpackViewModel model = BukovBackpackViewModel.from(
				ledger,
				firearms(),
				new BukovBackpackViewModel.EquippedFirearm(
						"gun-a",
						7,
						12));

		assertEquals(5, model.items.size());
		assertEquals("4.82/40.00kg", model.weightSummary());
		assertEquals(4526L, model.totalValue);

		BukovBackpackViewModel.ItemRow equipped = model.find("gun-a");
		assertNotNull(equipped);
		assertEquals("针蜂-9", equipped.name);
		assertEquals(BukovBackpackViewModel.Category.FIREARM, equipped.category);
		assertEquals(7, equipped.magazineAmmo);
		assertEquals(12, equipped.magazineCapacity);
		assertEquals("弹匣 7/12 · 耐久 78%", equipped.stateSummary());
		assertEquals(
				"单0.90kg · 总0.90kg · 值850",
				equipped.rowEconomySummary());
		assertTrue(equipped.equipped);
		assertFalse(equipped.canEquip);

		BukovBackpackViewModel.ItemRow second = model.find("gun-b");
		assertEquals("梭子-9", second.name);
		assertEquals(24, second.magazineCapacity);
		assertTrue(second.canEquip);

		BukovBackpackViewModel.ItemRow ammo = model.find("ammo-a");
		assertEquals("9毫米标准弹", ammo.name);
		assertEquals(BukovBackpackViewModel.Category.AMMUNITION, ammo.category);
		assertEquals("携带 18 发", ammo.stateSummary());

		BukovBackpackViewModel.ItemRow medical = model.find("med-a");
		assertEquals("急救包", medical.name);
		assertTrue(medical.canUse);

		BukovBackpackViewModel.ItemRow mission = model.find("mission-a");
		assertEquals("维修通道档案", mission.name);
		assertEquals(BukovBackpackViewModel.Category.MISSION, mission.category);
		assertFalse(mission.canDrop);
	}

	@Test
	public void emptyBackpackStillProducesValidTotals() {
		BukovBackpackViewModel model = BukovBackpackViewModel.from(
				new LootTransaction("raid-empty", 40f),
				firearms(),
				null);

		assertTrue(model.items.isEmpty());
		assertEquals("0.00/40.00kg", model.weightSummary());
		assertEquals(0L, model.totalValue);
	}

	private static RaidItem item(
			String uid,
			String definitionId,
			int quantity,
			float weight,
			int value,
			boolean found,
			float durability) {
		return new RaidItem(
				uid,
				definitionId,
				quantity,
				weight,
				value,
				found,
				false,
				durability);
	}

	private static void pickup(LootTransaction ledger, RaidItem item) {
		assertEquals(
				LootTransaction.PickupResult.ADDED,
				ledger.pickup(item));
	}

	private static FirearmRegistry firearms() {
		FirearmRegistry registry = new FirearmRegistry();
		registry.loadJson("{\"schemaVersion\":1,\"firearms\":["
				+ "{\"id\":\"needle_9\",\"name\":\"针蜂-9\","
				+ "\"caliber\":\"9x19\",\"defaultAmmo\":\"ammo_9_standard\","
				+ "\"fireMode\":\"SEMI\",\"damage\":21,\"penetration\":8,"
				+ "\"rpm\":360,\"magazineSize\":12,\"reloadSeconds\":1.45,"
				+ "\"effectiveRangeTiles\":8.5,\"baseSpreadDeg\":1.4,"
				+ "\"movingSpreadDeg\":2.1,\"recoilPerShot\":0.8,"
				+ "\"recoilRecovery\":6,\"pellets\":1,"
				+ "\"noiseRadiusTiles\":13,\"weightKg\":0.9,\"value\":850},"
				+ "{\"id\":\"shuttle_9\",\"name\":\"梭子-9\","
				+ "\"caliber\":\"9x19\",\"defaultAmmo\":\"ammo_9_standard\","
				+ "\"fireMode\":\"AUTO\",\"damage\":17,\"penetration\":7,"
				+ "\"rpm\":780,\"magazineSize\":24,\"reloadSeconds\":2.1,"
				+ "\"effectiveRangeTiles\":7,\"baseSpreadDeg\":2,"
				+ "\"movingSpreadDeg\":3.2,\"recoilPerShot\":0.56,"
				+ "\"recoilRecovery\":5.3,\"pellets\":1,"
				+ "\"noiseRadiusTiles\":15,\"weightKg\":2.2,\"value\":2100}"
				+ "]}");
		return registry;
	}
}
