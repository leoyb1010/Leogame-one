package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable result returned by the settlement core. */
public final class RaidResult {

	private final String raidId;
	private final RaidOutcome outcome;
	private final boolean replayed;
	private final List<String> transferredUids;
	private final List<String> lostUids;
	private final List<SettlementItemSnapshot> transferredItems;
	private final List<SettlementItemSnapshot> lostItems;
	private final long transferredQuantity;
	private final long transferredValue;
	private final long lostQuantity;
	private final long lostValue;
	private final boolean debriefAvailable;
	private final float elapsedSeconds;
	private final int kills;
	private final boolean missionCompleted;

	RaidResult(
			String raidId,
			RaidOutcome outcome,
			boolean replayed,
			List<String> transferredUids,
			List<String> lostUids,
			long transferredQuantity,
			long transferredValue,
			long lostQuantity,
			long lostValue) {
		this(
				raidId,
				outcome,
				replayed,
				transferredUids,
				lostUids,
				Collections.emptyList(),
				Collections.emptyList(),
				transferredQuantity,
				transferredValue,
				lostQuantity,
				lostValue,
				false,
				0f,
				0,
				false);
	}

	RaidResult(
			String raidId,
			RaidOutcome outcome,
			boolean replayed,
			List<String> transferredUids,
			List<String> lostUids,
			List<SettlementItemSnapshot> transferredItems,
			List<SettlementItemSnapshot> lostItems,
			long transferredQuantity,
			long transferredValue,
			long lostQuantity,
			long lostValue) {
		this(
				raidId,
				outcome,
				replayed,
				transferredUids,
				lostUids,
				transferredItems,
				lostItems,
				transferredQuantity,
				transferredValue,
				lostQuantity,
				lostValue,
				false,
				0f,
				0,
				false);
	}

	RaidResult(
			String raidId,
			RaidOutcome outcome,
			boolean replayed,
			List<String> transferredUids,
			List<String> lostUids,
			List<SettlementItemSnapshot> transferredItems,
			List<SettlementItemSnapshot> lostItems,
			long transferredQuantity,
			long transferredValue,
			long lostQuantity,
			long lostValue,
			boolean debriefAvailable,
			float elapsedSeconds,
			int kills,
			boolean missionCompleted) {
		this.raidId = raidId;
		this.outcome = outcome;
		this.replayed = replayed;
		this.transferredUids = Collections.unmodifiableList(new ArrayList<>(transferredUids));
		this.lostUids = Collections.unmodifiableList(new ArrayList<>(lostUids));
		this.transferredItems = immutableSnapshots(transferredItems);
		this.lostItems = immutableSnapshots(lostItems);
		this.transferredQuantity = transferredQuantity;
		this.transferredValue = transferredValue;
		this.lostQuantity = lostQuantity;
		this.lostValue = lostValue;
		this.debriefAvailable = debriefAvailable;
		this.elapsedSeconds = elapsedSeconds;
		this.kills = kills;
		this.missionCompleted = missionCompleted;
	}

	public String raidId() {
		return raidId;
	}

	public RaidOutcome outcome() {
		return outcome;
	}

	public boolean replayed() {
		return replayed;
	}

	public List<String> transferredUids() {
		return transferredUids;
	}

	public List<String> lostUids() {
		return lostUids;
	}

	public List<SettlementItemSnapshot> transferredItems() {
		return immutableSnapshots(transferredItems);
	}

	public List<SettlementItemSnapshot> lostItems() {
		return immutableSnapshots(lostItems);
	}

	public long transferredQuantity() {
		return transferredQuantity;
	}

	public long transferredValue() {
		return transferredValue;
	}

	public long lostQuantity() {
		return lostQuantity;
	}

	public long lostValue() {
		return lostValue;
	}

	/** True for settlements written after durable debrief snapshots existed. */
	public boolean debriefAvailable() {
		return debriefAvailable;
	}

	public float elapsedSeconds() {
		return elapsedSeconds;
	}

	public int kills() {
		return kills;
	}

	public boolean missionCompleted() {
		return missionCompleted;
	}

	private static List<SettlementItemSnapshot> immutableSnapshots(
			List<SettlementItemSnapshot> values) {
		List<SettlementItemSnapshot> result = new ArrayList<>();
		if (values != null) {
			for (SettlementItemSnapshot value : values) {
				if (value == null) {
					throw new IllegalArgumentException(
							"settlement item snapshot is required");
				}
				result.add(value.copy());
			}
		}
		return Collections.unmodifiableList(result);
	}
}
