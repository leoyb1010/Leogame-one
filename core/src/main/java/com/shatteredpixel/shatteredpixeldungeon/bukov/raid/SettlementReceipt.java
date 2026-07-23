package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Persisted proof that a raid has already been settled. */
public final class SettlementReceipt implements Bundlable {

	private static final String RAID_ID = "raid_id";
	private static final String OUTCOME = "outcome";
	private static final String LOOT_FINGERPRINT = "loot_fingerprint";
	private static final String TRANSFERRED_UIDS = "transferred_uids";
	private static final String LOST_UIDS = "lost_uids";
	private static final String TRANSFERRED_ITEMS = "transferred_items";
	private static final String LOST_ITEMS = "lost_items";
	private static final String TRANSFERRED_QUANTITY = "transferred_quantity";
	private static final String TRANSFERRED_VALUE = "transferred_value";
	private static final String LOST_QUANTITY = "lost_quantity";
	private static final String LOST_VALUE = "lost_value";
	private static final String DEBRIEF_AVAILABLE = "debrief_available";
	private static final String ELAPSED_SECONDS = "elapsed_seconds";
	private static final String KILLS = "kills";
	private static final String MISSION_COMPLETED = "mission_completed";

	private String raidId;
	private RaidOutcome outcome;
	private String lootFingerprint;
	private final List<String> transferredUids = new ArrayList<>();
	private final List<String> lostUids = new ArrayList<>();
	private final List<SettlementItemSnapshot> transferredItems = new ArrayList<>();
	private final List<SettlementItemSnapshot> lostItems = new ArrayList<>();
	private long transferredQuantity;
	private long transferredValue;
	private long lostQuantity;
	private long lostValue;
	private boolean debriefAvailable;
	private float elapsedSeconds;
	private int kills;
	private boolean missionCompleted;

	public SettlementReceipt() {
		// Required by Bundle reflection.
	}

	static SettlementReceipt create(
			String raidId,
			RaidOutcome outcome,
			String lootFingerprint,
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
		SettlementReceipt result = new SettlementReceipt();
		result.raidId = requireText(raidId, "raidId");
		result.outcome = requireOutcome(outcome);
		result.lootFingerprint = requireNonNull(lootFingerprint, "lootFingerprint");
		requireNonNull(transferredUids, "transferredUids");
		requireNonNull(lostUids, "lostUids");
		requireNonNull(transferredItems, "transferredItems");
		requireNonNull(lostItems, "lostItems");
		result.transferredUids.addAll(transferredUids);
		result.lostUids.addAll(lostUids);
		copySnapshots(transferredItems, result.transferredItems);
		copySnapshots(lostItems, result.lostItems);
		result.transferredQuantity = requireNonNegative(transferredQuantity, "transferredQuantity");
		result.transferredValue = requireNonNegative(transferredValue, "transferredValue");
		result.lostQuantity = requireNonNegative(lostQuantity, "lostQuantity");
		result.lostValue = requireNonNegative(lostValue, "lostValue");
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
		result.debriefAvailable = debriefAvailable;
		result.elapsedSeconds = debriefAvailable ? elapsedSeconds : 0f;
		result.kills = debriefAvailable ? kills : 0;
		result.missionCompleted =
				debriefAvailable && missionCompleted;
		return result;
	}

	public String raidId() {
		return raidId;
	}

	public RaidOutcome outcome() {
		return outcome;
	}

	public String lootFingerprint() {
		return lootFingerprint;
	}

	public List<String> transferredUids() {
		return Collections.unmodifiableList(new ArrayList<>(transferredUids));
	}

	public List<String> lostUids() {
		return Collections.unmodifiableList(new ArrayList<>(lostUids));
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

	boolean matches(
			RaidOutcome requestedOutcome,
			String requestedFingerprint,
			boolean requestedDebrief,
			float requestedElapsedSeconds,
			int requestedKills,
			boolean requestedMissionCompleted) {
		boolean fingerprintMatches =
				lootFingerprint.equals(requestedFingerprint)
				|| !lootFingerprint.contains("|mode:")
				&& requestedFingerprint.equals(
						lootFingerprint + "|mode:"
								+ BukovRaidMode.EXPEDITION.name());
		if (outcome != requestedOutcome || !fingerprintMatches) {
			return false;
		}
		// Legacy callers and receipts have no debrief payload. They remain
		// replay-safe because the economic fingerprint is still authoritative.
		if (!debriefAvailable || !requestedDebrief) {
			return true;
		}
		return Float.floatToIntBits(elapsedSeconds)
						== Float.floatToIntBits(requestedElapsedSeconds)
				&& kills == requestedKills
				&& missionCompleted == requestedMissionCompleted;
	}

	RaidResult result(boolean replayed) {
		return new RaidResult(
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
				debriefAvailable,
				elapsedSeconds,
				kills,
				missionCompleted);
	}

	SettlementReceipt copy() {
		return create(
				raidId,
				outcome,
				lootFingerprint,
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
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(RAID_ID, raidId);
		bundle.put(OUTCOME, outcome);
		bundle.put(LOOT_FINGERPRINT, lootFingerprint);
		bundle.put(TRANSFERRED_UIDS, transferredUids.toArray(new String[0]));
		bundle.put(LOST_UIDS, lostUids.toArray(new String[0]));
		bundle.put(TRANSFERRED_ITEMS, transferredItems);
		bundle.put(LOST_ITEMS, lostItems);
		bundle.put(TRANSFERRED_QUANTITY, transferredQuantity);
		bundle.put(TRANSFERRED_VALUE, transferredValue);
		bundle.put(LOST_QUANTITY, lostQuantity);
		bundle.put(LOST_VALUE, lostValue);
		bundle.put(DEBRIEF_AVAILABLE, debriefAvailable);
		bundle.put(ELAPSED_SECONDS, elapsedSeconds);
		bundle.put(KILLS, kills);
		bundle.put(MISSION_COMPLETED, missionCompleted);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		SettlementReceipt restored = create(
				bundle.getString(RAID_ID),
				bundle.getEnum(OUTCOME, RaidOutcome.class),
				bundle.getString(LOOT_FINGERPRINT),
				arrayList(bundle.getStringArray(TRANSFERRED_UIDS)),
				arrayList(bundle.getStringArray(LOST_UIDS)),
				snapshotList(bundle, TRANSFERRED_ITEMS),
				snapshotList(bundle, LOST_ITEMS),
				bundle.getLong(TRANSFERRED_QUANTITY),
				bundle.getLong(TRANSFERRED_VALUE),
				bundle.getLong(LOST_QUANTITY),
				bundle.getLong(LOST_VALUE),
				bundle.getBoolean(DEBRIEF_AVAILABLE),
				bundle.getFloat(ELAPSED_SECONDS),
				bundle.getInt(KILLS),
				bundle.getBoolean(MISSION_COMPLETED));
		raidId = restored.raidId;
		outcome = restored.outcome;
		lootFingerprint = restored.lootFingerprint;
		transferredUids.clear();
		transferredUids.addAll(restored.transferredUids);
		lostUids.clear();
		lostUids.addAll(restored.lostUids);
		transferredItems.clear();
		copySnapshots(restored.transferredItems, transferredItems);
		lostItems.clear();
		copySnapshots(restored.lostItems, lostItems);
		transferredQuantity = restored.transferredQuantity;
		transferredValue = restored.transferredValue;
		lostQuantity = restored.lostQuantity;
		lostValue = restored.lostValue;
		debriefAvailable = restored.debriefAvailable;
		elapsedSeconds = restored.elapsedSeconds;
		kills = restored.kills;
		missionCompleted = restored.missionCompleted;
	}

	private static String requireText(String value, String name) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(name + " is required");
		}
		return value;
	}

	private static <T> T requireNonNull(T value, String name) {
		if (value == null) {
			throw new IllegalArgumentException(name + " is required");
		}
		return value;
	}

	private static RaidOutcome requireOutcome(RaidOutcome value) {
		if (value == null) {
			throw new IllegalArgumentException("outcome is required");
		}
		return value;
	}

	private static long requireNonNegative(long value, String name) {
		if (value < 0L) {
			throw new IllegalArgumentException(name + " must be non-negative");
		}
		return value;
	}

	private static List<String> arrayList(String[] values) {
		return values == null
				? Collections.emptyList()
				: Arrays.asList(values);
	}

	private static List<SettlementItemSnapshot> snapshotList(
			Bundle bundle,
			String key) {
		if (!bundle.contains(key)) {
			return Collections.emptyList();
		}
		List<SettlementItemSnapshot> result = new ArrayList<>();
		for (Bundlable stored : bundle.getCollection(key)) {
			if (!(stored instanceof SettlementItemSnapshot)) {
				throw new IllegalStateException(
						"Unexpected settlement item snapshot");
			}
			result.add(((SettlementItemSnapshot) stored).copy());
		}
		return result;
	}

	private static List<SettlementItemSnapshot> immutableSnapshots(
			List<SettlementItemSnapshot> values) {
		List<SettlementItemSnapshot> result = new ArrayList<>();
		copySnapshots(values, result);
		return Collections.unmodifiableList(result);
	}

	private static void copySnapshots(
			List<SettlementItemSnapshot> source,
			List<SettlementItemSnapshot> destination) {
		for (SettlementItemSnapshot snapshot : source) {
			if (snapshot == null) {
				throw new IllegalArgumentException(
						"settlement item snapshot is required");
			}
			destination.add(snapshot.copy());
		}
	}
}
