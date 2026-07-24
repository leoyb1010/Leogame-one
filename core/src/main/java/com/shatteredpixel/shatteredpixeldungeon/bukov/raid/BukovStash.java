package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The profile-owned warehouse. Item UIDs are unique across the stash. */
public final class BukovStash implements Bundlable {

	private static final String ITEMS = "items";

	private final Map<String, RaidItem> itemsByUid = new LinkedHashMap<>();

	public BukovStash() {
		// Required by Bundle reflection.
	}

	public void deposit(RaidItem item) {
		if (item == null) {
			throw new IllegalArgumentException("item is required");
		}
		if (itemsByUid.containsKey(item.itemUid())) {
			throw new IllegalStateException("Duplicate stash UID: " + item.itemUid());
		}
		itemsByUid.put(item.itemUid(), item.copy());
	}

	public RaidItem withdraw(String itemUid) {
		RaidItem removed = itemsByUid.remove(itemUid);
		return removed == null ? null : removed.copy();
	}

	/** Replaces runtime flags on the same physical item without changing UID order. */
	public void replace(RaidItem item) {
		if (item == null) throw new IllegalArgumentException("item is required");
		if (!itemsByUid.containsKey(item.itemUid())) {
			throw new IllegalArgumentException(
					"Stash item does not exist: " + item.itemUid());
		}
		itemsByUid.put(item.itemUid(), item.copy());
	}

	public boolean contains(String itemUid) {
		return itemsByUid.containsKey(itemUid);
	}

	public RaidItem item(String itemUid) {
		RaidItem item = itemsByUid.get(itemUid);
		return item == null ? null : item.copy();
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

	BukovStash copy() {
		BukovStash result = new BukovStash();
		for (RaidItem item : itemsByUid.values()) {
			result.deposit(item);
		}
		return result;
	}

	void replaceWith(BukovStash replacement) {
		itemsByUid.clear();
		for (RaidItem item : replacement.itemsByUid.values()) {
			itemsByUid.put(item.itemUid(), item.copy());
		}
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(ITEMS, itemsByUid.values());
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		itemsByUid.clear();
		Collection<Bundlable> storedItems = bundle.getCollection(ITEMS);
		for (Bundlable stored : storedItems) {
			if (!(stored instanceof RaidItem)) {
				throw new IllegalStateException("Unexpected stash entry");
			}
			deposit((RaidItem) stored);
		}
	}
}
