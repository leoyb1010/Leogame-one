package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

/** Atomic, one-time reward claim for a ready long-term contract. */
public final class BukovLongTermContractService {

	public enum ClaimStatus {
		CLAIMED,
		NOT_READY,
		ALREADY_CLAIMED
	}

	public static final class ClaimResult {
		public final ClaimStatus status;
		public final String contractId;
		public final long currencyGranted;

		private ClaimResult(
				ClaimStatus status,
				String contractId,
				long currencyGranted) {
			this.status = status;
			this.contractId = contractId;
			this.currencyGranted = currencyGranted;
		}
	}

	public ClaimResult claim(BukovProfile profile, String contractId) {
		if (profile == null) {
			throw new IllegalArgumentException("profile is required");
		}
		BukovContractProgress before =
				profile.longTermContracts().progress(contractId);
		if (before.claimed()) {
			return new ClaimResult(
					ClaimStatus.ALREADY_CLAIMED, contractId, 0L);
		}
		if (!before.ready()) {
			return new ClaimResult(ClaimStatus.NOT_READY, contractId, 0L);
		}
		BukovProfile working = profile.copy();
		long reward = working.longTermContracts().claim(contractId);
		working.setCurrency(
				com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
						.addExact(working.currency(), reward));
		working.completeContract(contractId);
		profile.replaceWith(working);
		return new ClaimResult(ClaimStatus.CLAIMED, contractId, reward);
	}
}
