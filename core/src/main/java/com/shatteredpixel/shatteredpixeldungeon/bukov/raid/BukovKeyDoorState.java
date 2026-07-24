package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Raid-local, checkpoint-safe key consumption and door unlock ledger. */
public final class BukovKeyDoorState implements Bundlable {

	public enum UnlockResult {
		UNLOCKED,
		ALREADY_UNLOCKED,
		KEY_MISSING
	}

	private static final String UNLOCKED_DOORS = "unlocked_doors";
	private static final String CONSUMED_KEY_DEFINITIONS =
			"consumed_key_definitions";

	private final Map<String, String> unlockedDoors = new LinkedHashMap<>();

	public BukovKeyDoorState() {
	}

	public UnlockResult unlock(
			String doorId,
			String requiredKeyDefinitionId,
			LootTransaction carriedLoot) {
		String validDoor = requireId(doorId, "doorId");
		String validKey = requireId(
				requiredKeyDefinitionId, "requiredKeyDefinitionId");
		if (carriedLoot == null) {
			throw new IllegalArgumentException("carriedLoot is required");
		}
		if (unlockedDoors.containsKey(validDoor)) {
			return UnlockResult.ALREADY_UNLOCKED;
		}
		RaidItem consumed = carriedLoot.consumeOneDefinition(validKey);
		if (consumed == null) return UnlockResult.KEY_MISSING;
		unlockedDoors.put(validDoor, validKey);
		return UnlockResult.UNLOCKED;
	}

	public boolean unlocked(String doorId) {
		return unlockedDoors.containsKey(requireId(doorId, "doorId"));
	}

	public String consumedKeyDefinition(String doorId) {
		return unlockedDoors.get(requireId(doorId, "doorId"));
	}

	public Set<String> unlockedDoorIds() {
		return Collections.unmodifiableSet(
				new LinkedHashSet<>(unlockedDoors.keySet()));
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		String[] doors = new String[unlockedDoors.size()];
		String[] keys = new String[unlockedDoors.size()];
		int index = 0;
		for (Map.Entry<String, String> entry : unlockedDoors.entrySet()) {
			doors[index] = entry.getKey();
			keys[index] = entry.getValue();
			index++;
		}
		bundle.put(UNLOCKED_DOORS, doors);
		bundle.put(CONSUMED_KEY_DEFINITIONS, keys);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		String[] doors = bundle.getStringArray(UNLOCKED_DOORS);
		String[] keys = bundle.getStringArray(CONSUMED_KEY_DEFINITIONS);
		if (doors.length != keys.length) {
			throw new IllegalStateException("Stored key door ledger is incomplete");
		}
		unlockedDoors.clear();
		for (int index = 0; index < doors.length; index++) {
			String door = requireId(doors[index], "doorId");
			String key = requireId(keys[index], "requiredKeyDefinitionId");
			if (unlockedDoors.put(door, key) != null) {
				throw new IllegalStateException(
						"Duplicate unlocked key door: " + door);
			}
		}
	}

	private static String requireId(String value, String field) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(field + " is required");
		}
		return value;
	}
}
