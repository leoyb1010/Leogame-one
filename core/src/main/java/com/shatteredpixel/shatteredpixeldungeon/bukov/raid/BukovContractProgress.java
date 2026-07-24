package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

/** Durable capped progress and one-time reward state for one contract. */
public final class BukovContractProgress implements Bundlable {

	private static final String CONTRACT_ID = "contract_id";
	private static final String PROGRESS = "progress";
	private static final String CLAIMED = "claimed";

	private String contractId;
	private long progress;
	private boolean claimed;

	public BukovContractProgress() {
	}

	BukovContractProgress(String contractId) {
		this.contractId = BukovLongTermContractCatalog.require(contractId).id;
	}

	public String contractId() {
		return contractId;
	}

	public long progress() {
		return progress;
	}

	public long target() {
		return definition().target;
	}

	public boolean ready() {
		return progress >= target();
	}

	public boolean claimed() {
		return claimed;
	}

	void add(long delta) {
		if (delta < 0L) throw new IllegalArgumentException("delta cannot be negative");
		long target = target();
		progress = Math.min(
				target,
				com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
						.addExact(progress, delta));
	}

	long claim() {
		if (claimed || !ready()) return 0L;
		claimed = true;
		return definition().rewardCurrency;
	}

	BukovContractProgress copy() {
		BukovContractProgress result = new BukovContractProgress(contractId);
		result.progress = progress;
		result.claimed = claimed;
		return result;
	}

	private BukovLongTermContractDefinition definition() {
		return BukovLongTermContractCatalog.require(contractId);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(CONTRACT_ID, contractId);
		bundle.put(PROGRESS, progress);
		bundle.put(CLAIMED, claimed);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		BukovLongTermContractDefinition definition =
				BukovLongTermContractCatalog.require(
						bundle.getString(CONTRACT_ID));
		long restoredProgress = bundle.getLong(PROGRESS);
		if (restoredProgress < 0L || restoredProgress > definition.target) {
			throw new IllegalStateException(
					"Invalid contract progress: " + definition.id);
		}
		contractId = definition.id;
		progress = restoredProgress;
		claimed = bundle.getBoolean(CLAIMED);
		if (claimed && progress < definition.target) {
			throw new IllegalStateException(
					"Unfinished contract cannot have claimed reward");
		}
	}
}
