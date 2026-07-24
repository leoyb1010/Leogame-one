package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Atomic claim operation for all insurance returns currently due. */
public final class BukovInsuranceService {

	public static final class ClaimResult {
		public final List<String> returnedItemUids;
		public final long returnedValue;

		private ClaimResult(List<String> returnedItemUids, long returnedValue) {
			this.returnedItemUids = Collections.unmodifiableList(
					new ArrayList<>(returnedItemUids));
			this.returnedValue = returnedValue;
		}
	}

	public ClaimResult claimAvailable(BukovProfile profile) {
		if (profile == null) {
			throw new IllegalArgumentException("profile is required");
		}
		BukovProfile working = profile.copy();
		int formalSettlementCount =
				working.statistics().successfulRaids()
						+ working.statistics().deaths();
		List<RaidItem> due = working.insurance()
				.claimAvailable(formalSettlementCount);
		List<String> uids = new ArrayList<>();
		long value = 0L;
		for (RaidItem item : due) {
			working.stash().deposit(item);
			uids.add(item.itemUid());
			value = com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
					.addExact(value, item.totalValue());
		}
		profile.replaceWith(working);
		return new ClaimResult(uids, value);
	}
}
