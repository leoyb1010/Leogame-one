package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The carried-loot ledger for one raid.
 *
 * Pickup and drop are UID-preserving operations. The class does not know
 * about UI, heaps or Bag; adapters may call it before the existing collect
 * operation.
 */
public final class LootTransaction implements Bundlable {

	public enum PickupResult {
		ADDED,
		DUPLICATE_UID,
		OVERWEIGHT
	}

	private static final String RAID_ID = "raid_id";
	private static final String MAX_WEIGHT = "max_weight";
	private static final String ITEMS = "items";

	private String raidId;
	private float maxWeight;
	private final Map<String, RaidItem> itemsByUid = new LinkedHashMap<>();

	public LootTransaction() {
		// Required by Bundle reflection.
	}

	public LootTransaction(String raidId, float maxWeight) {
		if (raidId == null || raidId.trim().isEmpty()) {
			throw new IllegalArgumentException("raidId is required");
		}
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
				maxWeight) || maxWeight < 0f) {
			throw new IllegalArgumentException("maxWeight must be finite and non-negative");
		}
		this.raidId = raidId;
		this.maxWeight = maxWeight;
	}

	public String raidId() {
		return raidId;
	}

	public float maxWeight() {
		return maxWeight;
	}

	public PickupResult pickup(RaidItem item) {
		if (item == null) {
			throw new IllegalArgumentException("item is required");
		}
		if (itemsByUid.containsKey(item.itemUid())) {
			return PickupResult.DUPLICATE_UID;
		}
		if (totalWeight() + item.totalWeight() > maxWeight + 0.0001f) {
			return PickupResult.OVERWEIGHT;
		}
		itemsByUid.put(item.itemUid(), item.copy());
		return PickupResult.ADDED;
	}

	public RaidItem drop(String itemUid) {
		RaidItem removed = itemsByUid.remove(itemUid);
		return removed == null ? null : removed.copy();
	}

	/**
	 * Consumes exactly one matching item, choosing the lowest UID so checkpoint
	 * replay cannot consume a different key stack.
	 */
	public RaidItem consumeOneDefinition(String definitionId) {
		if (definitionId == null || definitionId.trim().isEmpty()) {
			throw new IllegalArgumentException("definitionId is required");
		}
		RaidItem selected = null;
		for (RaidItem item : itemsByUid.values()) {
			if (!definitionId.equals(item.definitionId())) continue;
			if (selected == null
					|| item.itemUid().compareTo(selected.itemUid()) < 0) {
				selected = item;
			}
		}
		if (selected == null) return null;
		RaidItem consumed = selected.withRuntimeState(
				1, selected.durability());
		if (selected.quantity() == 1) {
			itemsByUid.remove(selected.itemUid());
		} else {
			itemsByUid.put(
					selected.itemUid(),
					selected.withRuntimeState(
							selected.quantity() - 1,
							selected.durability()));
		}
		return consumed;
	}

	public boolean contains(String itemUid) {
		return itemsByUid.containsKey(itemUid);
	}

	public boolean containsDefinition(String definitionId) {
		return firstItemUidForDefinition(definitionId) != null;
	}

	public String firstItemUidForDefinition(String definitionId) {
		if (definitionId == null || definitionId.trim().isEmpty()) {
			throw new IllegalArgumentException("definitionId is required");
		}
		String selectedUid = null;
		for (RaidItem item : itemsByUid.values()) {
			if (definitionId.equals(item.definitionId())
					&& (selectedUid == null
							|| item.itemUid().compareTo(selectedUid) < 0)) {
				selectedUid = item.itemUid();
			}
		}
		return selectedUid;
	}

	public RaidItem item(String itemUid) {
		RaidItem item = itemsByUid.get(itemUid);
		return item == null ? null : item.copy();
	}

	public void replace(RaidItem item) {
		if (item == null) {
			throw new IllegalArgumentException("item is required");
		}
		RaidItem previous = itemsByUid.get(item.itemUid());
		if (previous == null) {
			throw new IllegalArgumentException("Unknown loot UID: " + item.itemUid());
		}
		float updatedWeight = totalWeight()
				- previous.totalWeight()
				+ item.totalWeight();
		if (updatedWeight > maxWeight + 0.0001f) {
			throw new IllegalArgumentException("Updated loot exceeds weight limit");
		}
		itemsByUid.put(item.itemUid(), item.copy());
	}

	public int distinctItemCount() {
		return itemsByUid.size();
	}

	public long totalQuantity() {
		long total = 0L;
		for (RaidItem item : itemsByUid.values()) {
			total += item.quantity();
		}
		return total;
	}

	public float totalWeight() {
		float total = 0f;
		for (RaidItem item : itemsByUid.values()) {
			total += item.totalWeight();
		}
		return total;
	}

	public long totalValue() {
		long total = 0L;
		for (RaidItem item : itemsByUid.values()) {
			total += item.totalValue();
		}
		return total;
	}

	public List<RaidItem> items() {
		List<RaidItem> result = new ArrayList<>();
		for (RaidItem item : itemsByUid.values()) {
			result.add(item.copy());
		}
		return Collections.unmodifiableList(result);
	}

	public String fingerprint() {
		List<RaidItem> sorted = new ArrayList<>(itemsByUid.values());
		Collections.sort(sorted, new Comparator<RaidItem>() {
			@Override
			public int compare(RaidItem first, RaidItem second) {
				return first.itemUid().compareTo(second.itemUid());
			}
		});
		StringBuilder out = new StringBuilder();
		for (RaidItem item : sorted) {
			String part = item.fingerprintPart();
			out.append(part.length()).append('@').append(part);
		}
		return out.toString();
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(RAID_ID, raidId);
		bundle.put(MAX_WEIGHT, maxWeight);
		bundle.put(ITEMS, itemsByUid.values());
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		String restoredRaidId = bundle.getString(RAID_ID);
		float restoredMaxWeight = bundle.getFloat(MAX_WEIGHT);
		LootTransaction restored = new LootTransaction(restoredRaidId, restoredMaxWeight);
		Collection<Bundlable> storedItems = bundle.getCollection(ITEMS);
		for (Bundlable stored : storedItems) {
			if (!(stored instanceof RaidItem)) {
				throw new IllegalStateException("Unexpected loot entry");
			}
			PickupResult result = restored.pickup((RaidItem) stored);
			if (result != PickupResult.ADDED) {
				throw new IllegalStateException(
						"Invalid persisted loot " + ((RaidItem) stored).itemUid() + ": " + result);
			}
		}
		raidId = restored.raidId;
		maxWeight = restored.maxWeight;
		itemsByUid.clear();
		itemsByUid.putAll(restored.itemsByUid);
	}
}
