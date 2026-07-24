package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Durable pending/claimed insurance queue, keyed by source raid and item UID. */
public final class BukovInsuranceLedger implements Bundlable {

	private static final String RETURNS = "returns";
	private final Map<String, BukovInsuranceReturn> returns =
			new LinkedHashMap<>();

	public BukovInsuranceLedger() {
	}

	void schedule(
			String raidId,
			RaidItem item,
			int availableAfterSettlementCount) {
		if (item == null || !item.insured() || item.foundInRaid()) return;
		String key = key(raidId, item.itemUid());
		if (returns.containsKey(key)) return;
		returns.put(key, new BukovInsuranceReturn(
				raidId, item, availableAfterSettlementCount));
	}

	List<RaidItem> claimAvailable(int settlementCount) {
		List<RaidItem> result = new ArrayList<>();
		for (BukovInsuranceReturn pending : returns.values()) {
			RaidItem claimed = pending.claim(settlementCount);
			if (claimed != null) result.add(claimed);
		}
		return result;
	}

	public List<BukovInsuranceReturn> returns() {
		List<BukovInsuranceReturn> result = new ArrayList<>();
		for (BukovInsuranceReturn pending : returns.values()) {
			result.add(pending.copy());
		}
		return Collections.unmodifiableList(result);
	}

	public int pendingCount() {
		int count = 0;
		for (BukovInsuranceReturn pending : returns.values()) {
			if (!pending.claimed()) count++;
		}
		return count;
	}

	BukovInsuranceLedger copy() {
		BukovInsuranceLedger result = new BukovInsuranceLedger();
		for (Map.Entry<String, BukovInsuranceReturn> entry : returns.entrySet()) {
			result.returns.put(entry.getKey(), entry.getValue().copy());
		}
		return result;
	}

	void replaceWith(BukovInsuranceLedger replacement) {
		returns.clear();
		for (Map.Entry<String, BukovInsuranceReturn> entry
				: replacement.returns.entrySet()) {
			returns.put(entry.getKey(), entry.getValue().copy());
		}
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(RETURNS, returns.values());
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		returns.clear();
		Collection<Bundlable> stored = bundle.getCollection(RETURNS);
		for (Bundlable entry : stored) {
			if (!(entry instanceof BukovInsuranceReturn)) {
				throw new IllegalStateException(
						"Unexpected insurance return entry");
			}
			BukovInsuranceReturn pending = (BukovInsuranceReturn) entry;
			String key = key(
					pending.sourceRaidId(), pending.item().itemUid());
			if (returns.put(key, pending.copy()) != null) {
				throw new IllegalStateException(
						"Duplicate insurance return: " + key);
			}
		}
	}

	private static String key(String raidId, String itemUid) {
		if (raidId == null || raidId.trim().isEmpty()
				|| itemUid == null || itemUid.trim().isEmpty()) {
			throw new IllegalArgumentException(
					"raidId and itemUid are required");
		}
		return raidId.length() + "#" + raidId + itemUid;
	}
}
