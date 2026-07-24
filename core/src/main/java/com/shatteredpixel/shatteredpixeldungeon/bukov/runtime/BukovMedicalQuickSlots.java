package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.LootTransaction;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Stable four-slot medical policy shared by keyboard and controller input.
 *
 * <p>Each slot owns a distinct treatment family. UIDs are sorted so the same
 * physical stack is selected after checkpoint restore or replay.</p>
 */
public final class BukovMedicalQuickSlots {

	public static final int SLOT_COUNT = 4;

	private static final String[][] DEFINITIONS = {
			{"medical:tourniquet", "medical:bandage"},
			{"medical:first_aid", "medical:antiseptic"},
			{"medical:splint"},
			{"medical:stim", "medical:painkiller"}
	};

	public static List<String> candidateItemUids(
			LootTransaction loot,
			int slot) {
		if (loot == null) {
			throw new IllegalArgumentException("loot is required");
		}
		if (slot < 1 || slot > SLOT_COUNT) {
			throw new IllegalArgumentException(
					"medical slot must be in [1, 4]");
		}
		List<RaidItem> items = new ArrayList<>(loot.items());
		Collections.sort(items, new Comparator<RaidItem>() {
			@Override
			public int compare(RaidItem first, RaidItem second) {
				return first.itemUid().compareTo(second.itemUid());
			}
		});
		List<String> result = new ArrayList<>();
		for (String definition : DEFINITIONS[slot - 1]) {
			for (RaidItem item : items) {
				if (sameDefinition(definition, item.definitionId())) {
					result.add(item.itemUid());
				}
			}
		}
		return Collections.unmodifiableList(result);
	}

	static String primaryDefinition(int slot) {
		if (slot < 1 || slot > SLOT_COUNT) {
			throw new IllegalArgumentException(
					"medical slot must be in [1, 4]");
		}
		return DEFINITIONS[slot - 1][0];
	}

	private static boolean sameDefinition(
			String expected,
			String actual) {
		if (actual == null) return false;
		if (expected.equals(actual)) return true;
		return expected.startsWith("medical:")
				&& expected.substring("medical:".length()).equals(actual);
	}

	private BukovMedicalQuickSlots() {
	}
}
