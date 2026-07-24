package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveService;

import java.io.IOException;

/**
 * Atomic offline buy/sell boundary.
 *
 * Every operation stages changes in a freshly loaded profile and publishes
 * exactly one profile save after all validation succeeds. Active raids lock
 * trading so deployed UIDs can never be sold from the warehouse.
 */
public final class BukovEconomyService {

	public static final class Receipt {
		public final String transactionId;
		public final String itemUid;
		public final long currencyDelta;
		public final long balanceAfter;
		public final boolean alreadyCommitted;

		private Receipt(
				String transactionId,
				String itemUid,
				long currencyDelta,
				long balanceAfter,
				boolean alreadyCommitted) {
			this.transactionId = transactionId;
			this.itemUid = itemUid;
			this.currencyDelta = currencyDelta;
			this.balanceAfter = balanceAfter;
			this.alreadyCommitted = alreadyCommitted;
		}
	}

	private static final double SELL_RATIO = 0.55d;

	private final BukovSaveService saves;

	public BukovEconomyService(BukovSaveService saves) {
		if (saves == null) {
			throw new IllegalArgumentException("saves are required");
		}
		this.saves = saves;
	}

	public Receipt buy(String transactionId, String offerId)
			throws IOException {
		requireBetweenRaids();
		String cleanTransactionId = requireTransactionId(transactionId);
		String itemUid = "vendor:" + cleanTransactionId;
		BukovProfile profile = saves.loadProfile();
		BukovEconomyReceipt committed =
				profile.economyReceipt(cleanTransactionId);
		if (committed != null) {
			return committedReceipt(
					committed,
					BukovEconomyReceipt.Operation.BUY,
					offerId);
		}
		BukovVendorCatalog.Offer offer =
				BukovVendorCatalog.require(offerId);
		RaidItem existing = profile.stash().item(itemUid);
		if (existing != null) {
			if (!offer.definitionId.equals(existing.definitionId())
					|| offer.quantity != existing.quantity()) {
				throw new IllegalStateException(
						"Transaction UID already belongs to another item");
			}
			BukovProfile staged = profile.copy();
			BukovEconomyReceipt migrated = new BukovEconomyReceipt(
					cleanTransactionId,
					BukovEconomyReceipt.Operation.BUY,
					offerId,
					itemUid,
					0L,
					profile.currency());
			staged.recordEconomyReceipt(migrated);
			saves.saveProfile(staged);
			return receipt(migrated, true);
		}
		if (profile.currency() < offer.purchasePrice) {
			throw new IllegalStateException("Insufficient currency");
		}

		BukovProfile staged = profile.copy();
		staged.setCurrency(profile.currency() - offer.purchasePrice);
		staged.stash().deposit(offer.createItem(itemUid));
		BukovEconomyReceipt committedReceipt = new BukovEconomyReceipt(
				cleanTransactionId,
				BukovEconomyReceipt.Operation.BUY,
				offerId,
				itemUid,
				-offer.purchasePrice,
				staged.currency());
		staged.recordEconomyReceipt(committedReceipt);
		saves.saveProfile(staged);
		return receipt(committedReceipt, false);
	}

	public Receipt sell(String transactionId, String itemUid)
			throws IOException {
		requireBetweenRaids();
		String cleanTransactionId = requireTransactionId(transactionId);
		if (itemUid == null || itemUid.trim().isEmpty()) {
			throw new IllegalArgumentException("itemUid is required");
		}
		BukovProfile profile = saves.loadProfile();
		BukovEconomyReceipt committed =
				profile.economyReceipt(cleanTransactionId);
		if (committed != null) {
			return committedReceipt(
					committed,
					BukovEconomyReceipt.Operation.SELL,
					itemUid);
		}
		RaidItem item = profile.stash().item(itemUid);
		if (item == null) {
			throw new IllegalArgumentException(
					"Item is not in stash: " + itemUid);
		}
		if (profile.loadout().contains(itemUid)) {
			throw new IllegalStateException(
					"Remove item from loadout before selling");
		}
		if (!sellable(item)) {
			throw new IllegalStateException("Item cannot be sold");
		}
		long proceeds = appraisal(item);
		if (proceeds <= 0L) {
			throw new IllegalStateException("Item has no vendor value");
		}

		BukovProfile staged = profile.copy();
		RaidItem removed = staged.stash().withdraw(itemUid);
		if (removed == null || !removed.equals(item)) {
			throw new IllegalStateException(
					"Stash changed during sale staging");
		}
		// A build belongs to this physical UID, not to the firearm
		// definition. Selling the gun permanently removes that UID, so retain
		// neither an unreachable build nor attachments that could leak onto a
		// future item reusing the same identifier.
		staged.firearmBuilds().remove(itemUid);
		staged.setCurrency(BukovNumbers.addExact(
				staged.currency(),
				proceeds));
		BukovEconomyReceipt committedReceipt = new BukovEconomyReceipt(
				cleanTransactionId,
				BukovEconomyReceipt.Operation.SELL,
				itemUid,
				itemUid,
				proceeds,
				staged.currency());
		staged.recordEconomyReceipt(committedReceipt);
		saves.saveProfile(staged);
		return receipt(committedReceipt, false);
	}

	private static Receipt committedReceipt(
			BukovEconomyReceipt committed,
			BukovEconomyReceipt.Operation operation,
			String subjectId) {
		if (committed.operation() != operation
				|| !committed.subjectId().equals(subjectId)) {
			throw new IllegalStateException(
					"Transaction ID already belongs to another command");
		}
		return receipt(committed, true);
	}

	private static Receipt receipt(
			BukovEconomyReceipt committed,
			boolean alreadyCommitted) {
		return new Receipt(
				committed.transactionId(),
				committed.itemUid(),
				committed.currencyDelta(),
				committed.balanceAfter(),
				alreadyCommitted);
	}

	public static long appraisal(RaidItem item) {
		if (item == null) {
			throw new IllegalArgumentException("item is required");
		}
		if (!sellable(item)) {
			return 0L;
		}
		double condition = 0.35d + 0.65d * item.durability();
		double raw = item.totalValue() * SELL_RATIO * condition;
		if (Double.isNaN(raw) || Double.isInfinite(raw)
				|| raw > Long.MAX_VALUE) {
			throw new ArithmeticException("appraisal overflow");
		}
		return Math.max(1L, Math.round(raw));
	}

	public static boolean sellable(RaidItem item) {
		return item != null
				&& item.totalValue() > 0L
				&& !item.itemUid().startsWith("provision:")
				&& !item.definitionId().startsWith("mission:")
				&& BukovLoadout.deployable(item);
	}

	private void requireBetweenRaids() throws IOException {
		if (saves.loadRaidCheckpoint() != null) {
			throw new IllegalStateException(
					"Trading is locked during an active raid");
		}
	}

	private static String requireTransactionId(String transactionId) {
		if (transactionId == null
				|| !transactionId.matches("[A-Za-z0-9._-]{1,96}")) {
			throw new IllegalArgumentException(
					"transactionId must be 1-96 safe characters");
		}
		return transactionId;
	}
}
