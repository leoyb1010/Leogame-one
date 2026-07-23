package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Thirty-six settlement combinations: success/death x three carried item
 * counts x three stack quantities x two pre-existing stash sizes.
 */
@RunWith(Parameterized.class)
public class RaidSettlementParameterizedTest {

	@Parameterized.Parameters(name = "{index}: {0}, items={1}, qty={2}, stash={3}")
	public static Collection<Object[]> parameters() {
		List<Object[]> result = new ArrayList<>();
		for (RaidOutcome outcome : RaidOutcome.values()) {
			for (int itemCount : new int[]{0, 1, 3}) {
				for (int quantity : new int[]{1, 2, 5}) {
					for (int existingStashCount : new int[]{0, 2}) {
						result.add(new Object[]{
								outcome,
								itemCount,
								quantity,
								existingStashCount
						});
					}
				}
			}
		}
		return result;
	}

	private final RaidOutcome outcome;
	private final int itemCount;
	private final int quantity;
	private final int existingStashCount;

	public RaidSettlementParameterizedTest(
			RaidOutcome outcome,
			int itemCount,
			int quantity,
			int existingStashCount) {
		this.outcome = outcome;
		this.itemCount = itemCount;
		this.quantity = quantity;
		this.existingStashCount = existingStashCount;
	}

	@Test
	public void settlementIsConservativeAndIdempotent() {
		String raidId = "raid-" + outcome + '-' + itemCount + '-' + quantity + '-' + existingStashCount;
		BukovProfile profile = new BukovProfile();
		for (int index = 0; index < existingStashCount; index++) {
			profile.stash().deposit(item("existing-" + index, 1, 10));
		}

		LootTransaction loot = new LootTransaction(raidId, 1_000f);
		for (int index = 0; index < itemCount; index++) {
			assertEquals(
					LootTransaction.PickupResult.ADDED,
					loot.pickup(item("loot-" + index, quantity, 100 + index)));
		}

		RaidSettlement settlement = new RaidSettlement();
		RaidResult first = settlement.settle(profile, loot, outcome);

		assertFalse(first.replayed());
		assertEquals(raidId, first.raidId());
		assertEquals(outcome, first.outcome());
		assertTrue(profile.isSettled(raidId));

		long expectedQuantity = (long) itemCount * quantity;
		long expectedValue = 0L;
		for (int index = 0; index < itemCount; index++) {
			expectedValue += (long) (100 + index) * quantity;
		}
		if (outcome == RaidOutcome.SUCCESS) {
			assertEquals(existingStashCount + itemCount, profile.stash().distinctItemCount());
			assertEquals(expectedQuantity, first.transferredQuantity());
			assertEquals(expectedValue, first.transferredValue());
			assertEquals(0L, first.lostQuantity());
			assertEquals(0L, first.lostValue());
			assertEquals(1, profile.statistics().successfulRaids());
			assertEquals(0, profile.statistics().deaths());
		} else {
			assertEquals(existingStashCount, profile.stash().distinctItemCount());
			assertEquals(0L, first.transferredQuantity());
			assertEquals(0L, first.transferredValue());
			assertEquals(expectedQuantity, first.lostQuantity());
			assertEquals(expectedValue, first.lostValue());
			assertEquals(0, profile.statistics().successfulRaids());
			assertEquals(1, profile.statistics().deaths());
		}

		int stashCountAfterFirst = profile.stash().distinctItemCount();
		long stashQuantityAfterFirst = profile.stash().totalQuantity();
		long stashValueAfterFirst = profile.stash().totalValue();
		RaidResult replay = settlement.settle(profile, loot, outcome);

		assertTrue(replay.replayed());
		assertEquals(first.transferredUids(), replay.transferredUids());
		assertEquals(first.transferredQuantity(), replay.transferredQuantity());
		assertEquals(first.transferredValue(), replay.transferredValue());
		assertEquals(first.lostQuantity(), replay.lostQuantity());
		assertEquals(first.lostValue(), replay.lostValue());
		assertEquals(stashCountAfterFirst, profile.stash().distinctItemCount());
		assertEquals(stashQuantityAfterFirst, profile.stash().totalQuantity());
		assertEquals(stashValueAfterFirst, profile.stash().totalValue());
		assertEquals(1, profile.settlements().size());

		Set<String> uniqueUids = new HashSet<>();
		for (RaidItem stashItem : profile.stash().items()) {
			assertTrue(stashItem.quantity() > 0);
			assertTrue(uniqueUids.add(stashItem.itemUid()));
		}
	}

	private static RaidItem item(String uid, int quantity, int value) {
		return new RaidItem(
				uid,
				"definition-" + uid,
				quantity,
				0.25f,
				value,
				true,
				false,
				1f);
	}
}
