package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidOutcome;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidResult;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.SettlementItemSnapshot;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable renderer-independent data for the dedicated raid result window. */
public final class BukovSettlementViewModel {

	public static final class ItemRow {
		public final String itemUid;
		public final String name;
		public final int quantity;
		public final long value;
		public final boolean legacy;

		private ItemRow(SettlementItemSnapshot snapshot) {
			itemUid = snapshot.itemUid();
			name = displayName(snapshot.definitionId());
			quantity = snapshot.quantity();
			value = snapshot.totalValue();
			legacy = false;
		}

		private ItemRow(String legacyUid) {
			itemUid = legacyUid;
			name = BukovMessages.get("bukov.economy.settlement.legacy_item");
			quantity = 0;
			value = 0L;
			legacy = true;
		}

		public String summary() {
			return legacy
					? BukovMessages.get(
							"bukov.economy.settlement.legacy_summary", name)
					: BukovMessages.get(
							"bukov.economy.settlement.item_summary",
							name,
							quantity,
							value);
		}
	}

	public final RaidOutcome outcome;
	public final String headline;
	public final String manifestTitle;
	public final String duration;
	public final int kills;
	public final long quantity;
	public final long value;
	public final List<ItemRow> items;
	public final boolean legacyDetails;
	public final boolean missionCompleted;

	private BukovSettlementViewModel(
			RaidOutcome outcome,
			String headline,
			String manifestTitle,
			String duration,
			int kills,
			long quantity,
			long value,
			List<ItemRow> items,
			boolean legacyDetails,
			boolean missionCompleted) {
		this.outcome = outcome;
		this.headline = headline;
		this.manifestTitle = manifestTitle;
		this.duration = duration;
		this.kills = kills;
		this.quantity = quantity;
		this.value = value;
		this.items = Collections.unmodifiableList(items);
		this.legacyDetails = legacyDetails;
		this.missionCompleted = missionCompleted;
	}

	/**
	 * @param elapsedSeconds snapshot this before calling settleSuccess/death
	 * @param kills real-time world kill count, or zero when no combat occurred
	 */
	public static BukovSettlementViewModel from(
			RaidResult result,
			float elapsedSeconds,
			int kills) {
		if (result == null) {
			throw new IllegalArgumentException("result is required");
		}
		if (!BukovNumbers.isFinite(elapsedSeconds) || elapsedSeconds < 0f) {
			throw new IllegalArgumentException(
					"elapsedSeconds must be finite and non-negative");
		}
		if (kills < 0) {
			throw new IllegalArgumentException("kills must be non-negative");
		}

		boolean success = result.outcome() == RaidOutcome.SUCCESS;
		float durableElapsed = result.debriefAvailable()
				? result.elapsedSeconds()
				: elapsedSeconds;
		int durableKills = result.debriefAvailable()
				? result.kills()
				: kills;
		List<SettlementItemSnapshot> snapshots = success
				? result.transferredItems()
				: result.lostItems();
		List<String> legacyUids = success
				? result.transferredUids()
				: result.lostUids();
		List<ItemRow> rows = new ArrayList<>();
		for (SettlementItemSnapshot snapshot : snapshots) {
			rows.add(new ItemRow(snapshot));
		}
		boolean legacy = rows.isEmpty() && !legacyUids.isEmpty();
		if (legacy) {
			for (String uid : legacyUids) {
				rows.add(new ItemRow(uid));
			}
		}
		return new BukovSettlementViewModel(
				result.outcome(),
				BukovMessages.get(success
						? "bukov.economy.settlement.headline_success"
						: "bukov.economy.settlement.headline_failed"),
				BukovMessages.get(success
						? "bukov.economy.settlement.manifest_success"
						: "bukov.economy.settlement.manifest_failed"),
				BukovHudFormat.clock(durableElapsed),
				durableKills,
				success
						? result.transferredQuantity()
						: result.lostQuantity(),
				success ? result.transferredValue() : result.lostValue(),
				rows,
				legacy,
				result.debriefAvailable() && result.missionCompleted());
	}

	public String totals() {
		return totals(value);
	}

	public String totals(long displayedValue) {
		if (displayedValue < 0L || displayedValue > value) {
			throw new IllegalArgumentException(
					"displayed value must be inside the settlement total");
		}
		return BukovMessages.get(
				outcome == RaidOutcome.SUCCESS
						? "bukov.economy.settlement.totals_success"
						: "bukov.economy.settlement.totals_failed",
				quantity,
				displayedValue);
	}

	public String stats() {
		return BukovMessages.get(
				"bukov.economy.settlement.stats", duration, kills);
	}

	public String mission() {
		if (!missionCompleted) {
			return BukovMessages.get(
					"bukov.economy.settlement.mission_incomplete");
		}
		return BukovMessages.get(outcome == RaidOutcome.SUCCESS
				? "bukov.economy.settlement.mission_returned"
				: "bukov.economy.settlement.mission_lost");
	}

	public String earnings() {
		return BukovMessages.get(
				outcome == RaidOutcome.SUCCESS
						? "bukov.economy.settlement.earnings_success"
						: "bukov.economy.settlement.earnings_failed",
				value);
	}

	public String emptyManifest() {
		return BukovMessages.get(outcome == RaidOutcome.SUCCESS
				? "bukov.economy.settlement.empty_success"
				: "bukov.economy.settlement.empty_failed");
	}

	private static String displayName(String definitionId) {
		return BukovHubViewModel.displayName(definitionId);
	}
}
