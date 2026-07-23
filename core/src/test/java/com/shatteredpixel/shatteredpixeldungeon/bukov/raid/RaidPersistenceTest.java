package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.watabou.utils.Bundle;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RaidPersistenceTest {

	@Test
	public void profileRoundTripPreservesStashAndSettlementIdempotency() {
		BukovProfile profile = new BukovProfile();
		profile.setCurrency(250L);
		profile.unlockMap("first_level");
		profile.completeContract("first_extract");

		LootTransaction loot = new LootTransaction("persisted-raid", 100f);
		assertEquals(
				LootTransaction.PickupResult.ADDED,
				loot.pickup(item("persisted-item", 4)));
		new RaidSettlement().settle(profile, loot, RaidOutcome.SUCCESS);

		Bundle bundle = new Bundle();
		bundle.put("profile", profile);
		BukovProfile restored = (BukovProfile) bundle.get("profile");

		assertNotNull(restored);
		assertEquals(BukovProfile.CURRENT_VERSION, restored.profileVersion());
		assertEquals(250L, restored.currency());
		assertTrue(restored.unlockedMaps().contains("first_level"));
		assertTrue(restored.completedContracts().contains("first_extract"));
		assertEquals(1, restored.stash().distinctItemCount());
		assertEquals(4L, restored.stash().totalQuantity());
		assertEquals(1, restored.statistics().successfulRaids());
		SettlementReceipt receipt = restored.settlements().get(0);
		assertEquals(1, receipt.transferredItems().size());
		assertEquals(
				"def-persisted-item",
				receipt.transferredItems().get(0).definitionId());
		assertEquals(4, receipt.transferredItems().get(0).quantity());
		assertEquals(40L, receipt.transferredItems().get(0).totalValue());

		RaidResult replay = new RaidSettlement().settle(restored, loot, RaidOutcome.SUCCESS);
		assertTrue(replay.replayed());
		assertEquals(1, restored.stash().distinctItemCount());
		assertEquals(4L, restored.stash().totalQuantity());
	}

	@Test
	public void lootRoundTripPreservesWeightValueAndFingerprint() {
		LootTransaction loot = new LootTransaction("loot-save", 12f);
		assertEquals(LootTransaction.PickupResult.ADDED, loot.pickup(item("a", 2)));
		assertEquals(LootTransaction.PickupResult.ADDED, loot.pickup(item("b", 3)));
		String fingerprint = loot.fingerprint();

		Bundle bundle = new Bundle();
		bundle.put("loot", loot);
		LootTransaction restored = (LootTransaction) bundle.get("loot");

		assertNotNull(restored);
		assertEquals(5L, restored.totalQuantity());
		assertEquals(2.5f, restored.totalWeight(), 0.0001f);
		assertEquals(50L, restored.totalValue());
		assertEquals(fingerprint, restored.fingerprint());
	}

	@Test
	public void profileRoundTripPreservesLastDeploymentTemplate() {
		BukovProfile profile = new BukovProfile();
		RaidItem weapon = item("template-weapon", 1);
		profile.stash().deposit(weapon);
		profile.loadout().select(weapon.itemUid(), profile.stash());
		profile.rememberCurrentLoadout();

		Bundle bundle = new Bundle();
		bundle.put("profile", profile);
		BukovProfile restored = (BukovProfile) bundle.get("profile");

		assertEquals(1, restored.lastLoadoutDefinitions().size());
		assertEquals(
				"def-template-weapon",
				restored.lastLoadoutDefinitions().get(0));
	}

	@Test
	public void extractionRoundTripPreservesConditionAndProgress() {
		ExtractionState extraction = ExtractionState.conditional();
		extraction.setConditionMet(true);
		extraction.update(20f, 3f, ExtractionState.Interaction.ACTIVE);

		Bundle bundle = new Bundle();
		bundle.put("extraction", extraction);
		ExtractionState restored = (ExtractionState) bundle.get("extraction");

		assertNotNull(restored);
		assertEquals("E02", restored.extractionId());
		assertTrue(restored.conditionMet());
		assertEquals(3f, restored.progressSeconds(), 0.0001f);
		assertFalse(restored.completed());
	}

	private static RaidItem item(String uid, int quantity) {
		return new RaidItem(uid, "def-" + uid, quantity, 0.5f, 10, true, false, 1f);
	}
}
