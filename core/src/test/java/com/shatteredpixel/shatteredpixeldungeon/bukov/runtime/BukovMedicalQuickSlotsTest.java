package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.LootTransaction;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BukovMedicalQuickSlotsTest {

	@Test
	public void fourSlotsResolveDistinctTreatmentFamiliesAndConcreteUids() {
		LootTransaction loot = new LootTransaction("raid-slots", 50f);
		add(loot, "z-bandage", "bandage");
		add(loot, "a-tourniquet", "medical:tourniquet");
		add(loot, "aid", "first_aid");
		add(loot, "splint", "medical:splint");
		add(loot, "stim", "stim");

		assertEquals(
				java.util.Arrays.asList("a-tourniquet", "z-bandage"),
				BukovMedicalQuickSlots.candidateItemUids(loot, 1));
		assertEquals(
				java.util.Collections.singletonList("aid"),
				BukovMedicalQuickSlots.candidateItemUids(loot, 2));
		assertEquals(
				java.util.Collections.singletonList("splint"),
				BukovMedicalQuickSlots.candidateItemUids(loot, 3));
		assertEquals(
				java.util.Collections.singletonList("stim"),
				BukovMedicalQuickSlots.candidateItemUids(loot, 4));
	}

	@Test
	public void missingFamilyReturnsEmptyInsteadOfBorrowingAnotherSlot() {
		LootTransaction loot = new LootTransaction("raid-empty-slot", 10f);
		add(loot, "aid", "first_aid");
		List<String> trauma =
				BukovMedicalQuickSlots.candidateItemUids(loot, 3);
		assertTrue(trauma.isEmpty());
	}

	private static void add(
			LootTransaction loot, String uid, String definition) {
		assertEquals(
				LootTransaction.PickupResult.ADDED,
				loot.pickup(new RaidItem(
						uid,
						definition,
						1,
						0.2f,
						10,
						false,
						false,
						1f)));
	}
}
