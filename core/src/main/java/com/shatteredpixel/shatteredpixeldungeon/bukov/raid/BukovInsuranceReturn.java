package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

/** One delayed, single-use insurance return for a lost deployment item. */
public final class BukovInsuranceReturn implements Bundlable {

	private static final String SOURCE_RAID_ID = "source_raid_id";
	private static final String ITEM = "item";
	private static final String AVAILABLE_AFTER = "available_after";
	private static final String CLAIMED = "claimed";

	private String sourceRaidId;
	private RaidItem item;
	private int availableAfterSettlementCount;
	private boolean claimed;

	public BukovInsuranceReturn() {
	}

	BukovInsuranceReturn(
			String sourceRaidId,
			RaidItem item,
			int availableAfterSettlementCount) {
		if (sourceRaidId == null || sourceRaidId.trim().isEmpty()) {
			throw new IllegalArgumentException("sourceRaidId is required");
		}
		if (item == null || !item.insured() || item.foundInRaid()) {
			throw new IllegalArgumentException(
					"only insured deployment items can be scheduled");
		}
		if (availableAfterSettlementCount <= 0) {
			throw new IllegalArgumentException(
					"availableAfterSettlementCount must be positive");
		}
		this.sourceRaidId = sourceRaidId;
		this.item = item.copy();
		this.availableAfterSettlementCount = availableAfterSettlementCount;
	}

	public String sourceRaidId() {
		return sourceRaidId;
	}

	public RaidItem item() {
		return item.copy();
	}

	public int availableAfterSettlementCount() {
		return availableAfterSettlementCount;
	}

	public boolean claimed() {
		return claimed;
	}

	public boolean availableAt(int settlementCount) {
		return !claimed && settlementCount >= availableAfterSettlementCount;
	}

	RaidItem claim(int settlementCount) {
		if (!availableAt(settlementCount)) return null;
		claimed = true;
		return item.withFoundInRaid(false).withInsured(false);
	}

	BukovInsuranceReturn copy() {
		BukovInsuranceReturn result = new BukovInsuranceReturn(
				sourceRaidId, item, availableAfterSettlementCount);
		result.claimed = claimed;
		return result;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(SOURCE_RAID_ID, sourceRaidId);
		bundle.put(ITEM, item);
		bundle.put(AVAILABLE_AFTER, availableAfterSettlementCount);
		bundle.put(CLAIMED, claimed);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		Bundlable storedItem = bundle.get(ITEM);
		if (!(storedItem instanceof RaidItem)) {
			throw new IllegalStateException("Insurance return item is missing");
		}
		BukovInsuranceReturn restored = new BukovInsuranceReturn(
				bundle.getString(SOURCE_RAID_ID),
				(RaidItem) storedItem,
				bundle.getInt(AVAILABLE_AFTER));
		restored.claimed = bundle.getBoolean(CLAIMED);
		sourceRaidId = restored.sourceRaidId;
		item = restored.item;
		availableAfterSettlementCount =
				restored.availableAfterSettlementCount;
		claimed = restored.claimed;
	}
}
