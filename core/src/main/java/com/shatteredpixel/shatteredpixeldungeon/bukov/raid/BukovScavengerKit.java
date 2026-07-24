package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import java.util.Collections;

/**
 * Raid-local recovery equipment issued only to a new scavenger action.
 *
 * The raid ID is part of every UID, so checkpoint resume reuses the same
 * physical kit. Settlement recognizes the reserved prefix and removes the
 * issued equipment from both success and death results.
 */
public final class BukovScavengerKit {

	static final String UID_PREFIX = "issued:scavenger:v1:";
	static final String FIREARM_DEFINITION = "firearm:needle_9";
	static final String AMMO_DEFINITION = "ammo:ammo_9_standard";
	static final String MEDICAL_DEFINITION = "bandage";
	static final String BACKPACK_DEFINITION = "backpack:scout_pack";
	static final int AMMO_QUANTITY = 36;
	static final int MEDICAL_QUANTITY = 2;

	private BukovScavengerKit() {
	}

	static void grant(LootTransaction carried, String raidId) {
		if (carried == null) {
			throw new IllegalArgumentException("carried loot is required");
		}
		if (raidId == null || raidId.trim().isEmpty()) {
			throw new IllegalArgumentException("raidId is required");
		}
		add(carried, item(
				raidId,
				"firearm",
				FIREARM_DEFINITION,
				1,
				0.90f));
		add(carried, item(
				raidId,
				"ammo",
				AMMO_DEFINITION,
				AMMO_QUANTITY,
				0.012f));
		add(carried, item(
				raidId,
				"medical",
				MEDICAL_DEFINITION,
				MEDICAL_QUANTITY,
				0.12f));
		add(carried, item(
				raidId,
				"backpack",
				BACKPACK_DEFINITION,
				1,
				1.10f));
	}

	static float weightCapacityKg() {
		RaidItem backpack = item(
				"capacity",
				"backpack",
				BACKPACK_DEFINITION,
				1,
				1.10f);
		return BukovGearRules.resolve(
				Collections.singletonList(backpack)).weightCapacityKg;
	}

	public static boolean issuedItem(RaidItem item) {
		return item != null && item.itemUid().startsWith(UID_PREFIX);
	}

	private static RaidItem item(
			String raidId,
			String slot,
			String definitionId,
			int quantity,
			float unitWeight) {
		return new RaidItem(
				UID_PREFIX + raidId + ":" + slot,
				definitionId,
				quantity,
				unitWeight,
				0,
				false,
				false,
				1f);
	}

	private static void add(LootTransaction carried, RaidItem item) {
		if (carried.pickup(item) != LootTransaction.PickupResult.ADDED) {
			throw new IllegalStateException(
					"Scavenger kit exceeds raid capacity: "
							+ item.itemUid());
		}
	}
}
