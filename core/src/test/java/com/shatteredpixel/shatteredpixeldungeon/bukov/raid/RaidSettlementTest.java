package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RaidSettlementTest {

	@Test
	public void changedPayloadForSettledRaidIsRejected() {
		BukovProfile profile = new BukovProfile();
		LootTransaction original = loot("same-raid", item("uid-1", 1, 20));
		new RaidSettlement().settle(profile, original, RaidOutcome.SUCCESS);

		LootTransaction changed = loot("same-raid", item("uid-1", 2, 20));
		try {
			new RaidSettlement().settle(profile, changed, RaidOutcome.SUCCESS);
			fail("changed settlement payload must fail");
		} catch (IllegalStateException expected) {
			assertTrue(expected.getMessage().contains("payload changed"));
		}
		assertEquals(1, profile.stash().distinctItemCount());
		assertEquals(1L, profile.stash().totalQuantity());
	}

	@Test
	public void changedOutcomeForSettledRaidIsRejected() {
		BukovProfile profile = new BukovProfile();
		LootTransaction loot = loot("same-raid", item("uid-1", 1, 20));
		new RaidSettlement().settle(profile, loot, RaidOutcome.DEATH);

		try {
			new RaidSettlement().settle(profile, loot, RaidOutcome.SUCCESS);
			fail("changed settlement outcome must fail");
		} catch (IllegalStateException expected) {
			assertTrue(expected.getMessage().contains("payload changed"));
		}
		assertFalse(profile.stash().contains("uid-1"));
	}

	@Test
	public void failedTransferLeavesProfileUntouched() {
		BukovProfile profile = new BukovProfile();
		profile.setCurrency(77L);
		profile.stash().deposit(item("duplicate", 1, 10));
		LootTransaction loot = loot("atomic-raid", item("duplicate", 1, 100));

		try {
			new RaidSettlement().settle(profile, loot, RaidOutcome.SUCCESS);
			fail("duplicate stash UID must fail");
		} catch (IllegalStateException expected) {
			assertTrue(expected.getMessage().contains("Duplicate stash UID"));
		}
		assertEquals(77L, profile.currency());
		assertEquals(1, profile.stash().distinctItemCount());
		assertEquals(10L, profile.stash().totalValue());
		assertFalse(profile.isSettled("atomic-raid"));
		assertEquals(0, profile.statistics().successfulRaids());
	}

	private static LootTransaction loot(String raidId, RaidItem... items) {
		LootTransaction result = new LootTransaction(raidId, 100f);
		for (RaidItem item : items) {
			assertEquals(LootTransaction.PickupResult.ADDED, result.pickup(item));
		}
		return result;
	}

	private static RaidItem item(String uid, int quantity, int value) {
		return new RaidItem(uid, "def-" + uid, quantity, 1f, value, true, false, 1f);
	}
}
