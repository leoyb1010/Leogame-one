package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** Profile-owned deployment selection. Items stay in the stash until raid start. */
public final class BukovLoadout implements Bundlable {

	public static final int MAX_DISTINCT_ITEMS = 12;

	private static final String SELECTED_UIDS = "selected_uids";

	private final Set<String> selectedUids = new LinkedHashSet<>();

	public BukovLoadout() {
		// Required by Bundle reflection.
	}

	public void select(String itemUid, BukovStash stash) {
		requireStash(stash);
		if (itemUid == null || itemUid.trim().isEmpty()) {
			throw new IllegalArgumentException("itemUid is required");
		}
		RaidItem item = stash.item(itemUid);
		if (item == null) {
			throw new IllegalArgumentException("Loadout item is not in stash: " + itemUid);
		}
		if (!deployable(item)) {
			throw new IllegalArgumentException(
					"Mission items cannot be added to a loadout");
		}
		if (!selectedUids.contains(itemUid)
				&& selectedUids.size() >= MAX_DISTINCT_ITEMS) {
			throw new IllegalStateException("Loadout item limit reached");
		}
		selectedUids.add(itemUid);
	}

	public boolean remove(String itemUid) {
		return selectedUids.remove(itemUid);
	}

	public void clear() {
		selectedUids.clear();
	}

	public boolean contains(String itemUid) {
		return selectedUids.contains(itemUid);
	}

	public int distinctItemCount() {
		return selectedUids.size();
	}

	public List<String> selectedUids() {
		return Collections.unmodifiableList(new ArrayList<>(selectedUids));
	}

	public List<RaidItem> items(BukovStash stash) {
		requireStash(stash);
		List<RaidItem> result = new ArrayList<>();
		for (String itemUid : selectedUids) {
			RaidItem item = stash.item(itemUid);
			if (item == null) {
				throw new IllegalStateException("Loadout item disappeared: " + itemUid);
			}
			result.add(item);
		}
		return Collections.unmodifiableList(result);
	}

	public float totalWeight(BukovStash stash) {
		float total = 0f;
		for (RaidItem item : items(stash)) {
			total += item.totalWeight();
		}
		return total;
	}

	public long totalValue(BukovStash stash) {
		long total = 0L;
		for (RaidItem item : items(stash)) {
			total = com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.addExact(total, item.totalValue());
		}
		return total;
	}

	public void pruneMissing(BukovStash stash) {
		requireStash(stash);
		Iterator<String> iterator = selectedUids.iterator();
		while (iterator.hasNext()) {
			RaidItem item = stash.item(iterator.next());
			if (item == null || !deployable(item)) {
				iterator.remove();
			}
		}
	}

	public static boolean deployable(RaidItem item) {
		return item != null && deployableDefinition(item.definitionId());
	}

	public static boolean deployableDefinition(String definitionId) {
		return definitionId != null
				&& !definitionId.startsWith("mission:")
				&& !FirstRaidMission.ARCHIVE_DEFINITION_ID.equals(
						definitionId);
	}

	BukovLoadout copy() {
		BukovLoadout result = new BukovLoadout();
		result.selectedUids.addAll(selectedUids);
		return result;
	}

	void replaceWith(BukovLoadout replacement) {
		selectedUids.clear();
		selectedUids.addAll(replacement.selectedUids);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(SELECTED_UIDS, selectedUids.toArray(new String[0]));
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		selectedUids.clear();
		for (String itemUid : bundle.getStringArray(SELECTED_UIDS)) {
			if (itemUid == null || itemUid.trim().isEmpty()) {
				throw new IllegalStateException("Invalid loadout item UID");
			}
			if (!selectedUids.add(itemUid)) {
				throw new IllegalStateException("Duplicate loadout item UID: " + itemUid);
			}
			if (selectedUids.size() > MAX_DISTINCT_ITEMS) {
				throw new IllegalStateException("Stored loadout item limit exceeded");
			}
		}
	}

	private static void requireStash(BukovStash stash) {
		if (stash == null) {
			throw new IllegalArgumentException("stash is required");
		}
	}
}
