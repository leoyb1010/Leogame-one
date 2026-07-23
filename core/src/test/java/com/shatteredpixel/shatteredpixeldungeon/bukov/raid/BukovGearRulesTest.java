package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.RealtimeDamage;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BukovGearRulesTest {

	@Test
	public void emptyLoadoutUsesStableBaseCapacity() {
		BukovGearRules.Snapshot gear =
				BukovGearRules.resolve(Collections.<RaidItem>emptyList());

		assertNull(gear.armor);
		assertNull(gear.backpack);
		assertEquals(40f, gear.weightCapacityKg, 0f);
		assertEquals(1f, gear.movementMultiplier, 0f);
		assertEquals(1f, gear.noiseMultiplier, 0f);
	}

	@Test
	public void firstArmorAndBackpackOccupySlotsAndAffectRaidRules() {
		RaidItem armor = item(
				"armor-a", "armor:patrol_vest", 3.6f, 3_100);
		RaidItem pack = item(
				"pack-a", "backpack:field_pack", 2f, 3_100);
		RaidItem sparePack = item(
				"pack-b", "backpack:scout_pack", 1.1f, 1_200);

		BukovGearRules.Snapshot gear = BukovGearRules.resolve(
				Arrays.asList(armor, pack, sparePack));

		assertEquals(armor.itemUid(), gear.armor.itemUid());
		assertEquals(pack.itemUid(), gear.backpack.itemUid());
		assertEquals(56f, gear.weightCapacityKg, 0f);
		assertEquals(0.90f, gear.movementMultiplier, 0.0001f);
		assertEquals(1.18f, gear.noiseMultiplier, 0.0001f);
	}

	@Test
	public void customBaseCapacityStillReceivesBackpackBonus() {
		BukovGearRules.Snapshot gear = BukovGearRules.resolve(
				Collections.singletonList(item(
						"pack", "backpack:scout_pack", 1.1f, 1_200)),
				20f);

		assertEquals(28f, gear.weightCapacityKg, 0f);
	}

	@Test(expected = IllegalArgumentException.class)
	public void stackedGearCannotOccupyEquipmentSlot() {
		BukovGearRules.resolve(Collections.singletonList(new RaidItem(
				"stacked-pack",
				"backpack:scout_pack",
				2,
				1.1f,
				1_200,
				false,
				false,
				1f)));
	}

	@Test
	public void ordinaryLootNeverAccidentallyOccupiesGearSlot() {
		assertNull(BukovGearRules.slotFor("duct_tape"));
		assertEquals(
				BukovGearRules.Slot.ARMOR,
				BukovGearRules.slotFor("armor:soft_vest"));
		assertEquals(
				BukovGearRules.Slot.BACKPACK,
				BukovGearRules.slotFor("backpack:scout_pack"));
		assertTrue(BukovGearRules.isKnownBackpack("field_pack"));
	}

	@Test
	public void equippedArmorReducesDamageAndPersistsDurabilityBySameUid() {
		RaidItem armor = item(
				"armor-live", "armor:patrol_vest", 3.6f, 3_100);
		LootTransaction ledger = new LootTransaction("raid-gear", 40f);
		assertEquals(
				LootTransaction.PickupResult.ADDED,
				ledger.pickup(armor));
		BukovEquippedGear gear =
				BukovEquippedGear.from(ledger.items());

		float healthDamage = gear.resolveIncomingBullet(
				30f,
				4f,
				RealtimeDamage.HitZone.CORE);
		gear.writeBack(ledger);
		RaidItem persisted = ledger.item(armor.itemUid());

		assertTrue(healthDamage < 30f);
		assertEquals(armor.itemUid(), persisted.itemUid());
		assertEquals(armor.definitionId(), persisted.definitionId());
		assertTrue(persisted.durability() < 1f);
		assertTrue(persisted.durability() >= 0f);
	}

	private static RaidItem item(
			String uid,
			String definitionId,
			float weight,
			int value) {
		return new RaidItem(
				uid,
				definitionId,
				1,
				weight,
				value,
				false,
				false,
				1f);
	}
}
