package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Transactional, idempotent raid settlement.
 *
 * The profile is changed only after a complete working copy has been
 * validated. A repeated request with the same raid ID, outcome and loot
 * fingerprint returns the stored result without granting loot twice.
 */
public final class RaidSettlement {

	public RaidResult settle(
			BukovProfile profile,
			LootTransaction carriedLoot,
			RaidOutcome outcome) {
		return settle(
				profile,
				carriedLoot,
				outcome,
				false,
				0f,
				0,
				false);
	}

	/**
	 * Commits the economic result and its durable debrief in one receipt.
	 * Retrying the same settlement is safe; changed stats are rejected.
	 */
	public RaidResult settle(
			BukovProfile profile,
			LootTransaction carriedLoot,
			RaidOutcome outcome,
			float elapsedSeconds,
			int kills,
			boolean missionCompleted) {
		return settle(
				profile,
				carriedLoot,
				outcome,
				elapsedSeconds,
				kills,
				missionCompleted,
				BukovRaidMode.EXPEDITION);
	}

	public RaidResult settle(
			BukovProfile profile,
			LootTransaction carriedLoot,
			RaidOutcome outcome,
			float elapsedSeconds,
			int kills,
			boolean missionCompleted,
			BukovRaidMode raidMode) {
		if (raidMode == null) {
			throw new IllegalArgumentException("raidMode is required");
		}
		return settle(
				profile,
				carriedLoot,
				outcome,
				true,
				elapsedSeconds,
				kills,
				missionCompleted,
				raidMode);
	}

	private RaidResult settle(
			BukovProfile profile,
			LootTransaction carriedLoot,
			RaidOutcome outcome,
			boolean debriefAvailable,
			float elapsedSeconds,
			int kills,
			boolean missionCompleted) {
		return settle(
				profile,
				carriedLoot,
				outcome,
				debriefAvailable,
				elapsedSeconds,
				kills,
				missionCompleted,
				BukovRaidMode.EXPEDITION);
	}

	private RaidResult settle(
			BukovProfile profile,
			LootTransaction carriedLoot,
			RaidOutcome outcome,
			boolean debriefAvailable,
			float elapsedSeconds,
			int kills,
			boolean missionCompleted,
			BukovRaidMode raidMode) {
		if (profile == null) {
			throw new IllegalArgumentException("profile is required");
		}
		if (carriedLoot == null) {
			throw new IllegalArgumentException("carriedLoot is required");
		}
		if (outcome == null) {
			throw new IllegalArgumentException("outcome is required");
		}
		if (debriefAvailable
				&& (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
						.isFinite(elapsedSeconds)
						|| elapsedSeconds < 0f)) {
			throw new IllegalArgumentException(
					"elapsedSeconds must be finite and non-negative");
		}
		if (kills < 0) {
			throw new IllegalArgumentException("kills must be non-negative");
		}

		String fingerprint = carriedLoot.fingerprint()
				+ "|mode:" + raidMode.name();
		SettlementReceipt existing = profile.settlement(carriedLoot.raidId());
		if (existing != null) {
			if (!existing.matches(
					outcome,
					fingerprint,
					debriefAvailable,
					elapsedSeconds,
					kills,
					missionCompleted)) {
				throw new IllegalStateException(
						"Settlement payload changed for raid: " + carriedLoot.raidId());
			}
			return existing.result(true);
		}

		if (!raidMode.countsTowardEconomyStatistics()) {
			return settleTrainingAttempt(
					profile,
					carriedLoot,
					outcome,
					fingerprint,
					debriefAvailable,
					elapsedSeconds,
					kills,
					missionCompleted);
		}

		BukovProfile working = profile.copy();
		List<String> transferredUids = new ArrayList<>();
		List<String> lostUids = new ArrayList<>();
		List<SettlementItemSnapshot> transferredItems = new ArrayList<>();
		List<SettlementItemSnapshot> lostItems = new ArrayList<>();
		long transferredQuantity = 0L;
		long transferredValue = 0L;
		long lostQuantity = 0L;
		long lostValue = 0L;

		if (outcome == RaidOutcome.SUCCESS) {
			for (RaidItem item : carriedLoot.items()) {
				RaidItem settledItem = raidMode.settleExtractedItem(item);
				// "found in raid" describes the just-finished action. Mission
				// evidence stays as an archived collectible; BukovLoadout keeps
				// it out of every later deployment.
				working.stash().deposit(
						settledItem.withFoundInRaid(false));
				transferredUids.add(settledItem.itemUid());
				transferredItems.add(
						SettlementItemSnapshot.from(settledItem));
				transferredQuantity += settledItem.quantity();
				transferredValue += settledItem.totalValue();
			}
			Collections.sort(transferredUids);
		} else {
			String protectedUid = protectedDeploymentUid(
					carriedLoot,
					raidMode);
			for (RaidItem item : carriedLoot.items()) {
				boolean protectedFromLoss =
						item.itemUid().equals(protectedUid)
								|| raidMode == BukovRaidMode.SCAVENGER
								&& !item.foundInRaid();
				if (protectedFromLoss) {
					working.stash().deposit(
							item.withFoundInRaid(false));
					transferredUids.add(item.itemUid());
					transferredItems.add(
							SettlementItemSnapshot.from(item));
					transferredQuantity += item.quantity();
					transferredValue += item.totalValue();
					continue;
				}
				lostUids.add(item.itemUid());
				lostItems.add(SettlementItemSnapshot.from(item));
				lostQuantity += item.quantity();
				lostValue += item.totalValue();
			}
			Collections.sort(lostUids);
			Collections.sort(transferredUids);
		}

		SettlementReceipt receipt = SettlementReceipt.create(
				carriedLoot.raidId(),
				outcome,
				fingerprint,
				transferredUids,
				lostUids,
				transferredItems,
				lostItems,
				transferredQuantity,
				transferredValue,
				lostQuantity,
				lostValue,
				debriefAvailable,
				elapsedSeconds,
				kills,
				missionCompleted);
		working.recordSettlement(receipt);
		working.statistics().record(
				outcome,
				outcome == RaidOutcome.SUCCESS
						? transferredValue : lostValue);
		if (debriefAvailable
				&& outcome == RaidOutcome.SUCCESS
				&& missionCompleted) {
			working.completeContract(
					com.shatteredpixel.shatteredpixeldungeon.bukov.mission
							.FirstRaidMission.EVENT_ID);
		}
		BukovCareerProgression.reconcile(working);

		// Single in-memory commit point. File persistence can atomically write
		// this resulting profile through a later BukovSaveService adapter.
		profile.replaceWith(working);
		return receipt.result(false);
	}

	/**
	 * Practice gear and pickups exist only inside the training checkpoint.
	 * A zero-value receipt is still persisted so interrupted checkpoint
	 * deletion remains replay-safe, but stash, currency, contracts and the
	 * four-mode statistics ledger stay byte-for-byte unchanged.
	 */
	private static RaidResult settleTrainingAttempt(
			BukovProfile profile,
			LootTransaction carriedLoot,
			RaidOutcome outcome,
			String fingerprint,
			boolean debriefAvailable,
			float elapsedSeconds,
			int kills,
			boolean missionCompleted) {
		BukovProfile working = profile.copy();
		SettlementReceipt receipt = SettlementReceipt.create(
				carriedLoot.raidId(),
				outcome,
				fingerprint,
				Collections.<String>emptyList(),
				Collections.<String>emptyList(),
				Collections.<SettlementItemSnapshot>emptyList(),
				Collections.<SettlementItemSnapshot>emptyList(),
				0L,
				0L,
				0L,
				0L,
				debriefAvailable,
				elapsedSeconds,
				kills,
				missionCompleted);
		working.recordSettlement(receipt);
		profile.replaceWith(working);
		return receipt.result(false);
	}

	private static String protectedDeploymentUid(
			LootTransaction loot,
			BukovRaidMode mode) {
		if (!mode.protectsHighestValueDeploymentOnDeath()) {
			return "";
		}
		RaidItem protectedItem = null;
		for (RaidItem item : loot.items()) {
			if (item.foundInRaid()) {
				continue;
			}
			if (protectedItem == null
					|| item.totalValue() > protectedItem.totalValue()
					|| item.totalValue() == protectedItem.totalValue()
					&& item.itemUid().compareTo(
							protectedItem.itemUid()) < 0) {
				protectedItem = item;
			}
		}
		return protectedItem == null ? "" : protectedItem.itemUid();
	}
}
