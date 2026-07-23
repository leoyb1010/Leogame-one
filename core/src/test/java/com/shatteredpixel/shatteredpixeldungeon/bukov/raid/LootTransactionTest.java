package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LootTransactionTest {

	@Test
	public void pickupRejectsOverweightWithoutMutation() {
		LootTransaction loot = new LootTransaction("weight", 2f);
		assertEquals(
				LootTransaction.PickupResult.ADDED,
				loot.pickup(item("small", 1, 1f)));
		assertEquals(
				LootTransaction.PickupResult.OVERWEIGHT,
				loot.pickup(item("heavy", 2, 1f)));
		assertEquals(1, loot.distinctItemCount());
		assertEquals(1f, loot.totalWeight(), 0.0001f);
	}

	@Test
	public void duplicateUidCannotEnterCarriedLootTwice() {
		LootTransaction loot = new LootTransaction("uid", 20f);
		assertEquals(
				LootTransaction.PickupResult.ADDED,
				loot.pickup(item("same", 1, 1f)));
		assertEquals(
				LootTransaction.PickupResult.DUPLICATE_UID,
				loot.pickup(item("same", 5, 1f)));
		assertEquals(1L, loot.totalQuantity());
	}

	@Test
	public void dropAndPickupPreserveUidAndQuantity() {
		LootTransaction loot = new LootTransaction("drop", 20f);
		RaidItem original = item("persistent", 3, 0.5f);
		assertEquals(LootTransaction.PickupResult.ADDED, loot.pickup(original));

		RaidItem dropped = loot.drop("persistent");
		assertNotNull(dropped);
		assertEquals(original.itemUid(), dropped.itemUid());
		assertEquals(original.quantity(), dropped.quantity());
		assertFalse(loot.contains("persistent"));

		assertEquals(LootTransaction.PickupResult.ADDED, loot.pickup(dropped));
		assertTrue(loot.contains("persistent"));
		assertEquals(3L, loot.totalQuantity());
	}

	@Test
	public void invalidItemQuantitiesAndWeightsFailFast() {
		try {
			new RaidItem("uid", "def", 0, 1f, 1, true, false, 1f);
			fail("zero quantity must fail");
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage().contains("quantity"));
		}
		try {
			new RaidItem("uid", "def", 1, -1f, 1, true, false, 1f);
			fail("negative weight must fail");
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage().contains("unitWeight"));
		}
	}

	private static RaidItem item(String uid, int quantity, float unitWeight) {
		return new RaidItem(
				uid,
				"def-" + uid,
				quantity,
				unitWeight,
				10,
				true,
				false,
				0.75f);
	}
}
