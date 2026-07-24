package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.Firearm;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovFirstRaidLootTables;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

/**
 * One-shot compatibility repair for active raids saved before deployment
 * always produced a usable runtime firearm.
 *
 * The emergency kit is raid-local and zero-value. Settlement discards it on
 * both extraction and death, so migration cannot mint stash value or bypass
 * normal loss rules.
 */
public final class BukovActiveRaidRecovery {

	static final String MIGRATION_EVENT_ID =
			"migration:active_raid_emergency_loadout_v1";
	private static final String UID_PREFIX =
			"provision:active-raid-rescue:v1:";
	static final String WEAPON_DEFINITION = "firearm:needle_9";
	static final String AMMO_DEFINITION = "ammo:ammo_9_standard";
	static final int AMMUNITION = 36;

	private BukovActiveRaidRecovery() {
	}

	static void markCurrentCheckpoint(BukovRaidCheckpoint checkpoint) {
		checkpoint.completeEvent(MIGRATION_EVENT_ID);
	}

	/**
	 * Evaluates exactly one legacy checkpoint. The caller persists the marker
	 * and any injected kit in the same checkpoint write.
	 */
	static boolean migrateLegacyCheckpoint(BukovRaidCheckpoint checkpoint) {
		if (checkpoint.eventCompleted(MIGRATION_EVENT_ID)) {
			return false;
		}
		boolean granted = false;
		if (!hasUsableFirearm(checkpoint)) {
			granted = add(
					checkpoint.loot(),
					emergencyWeapon(checkpoint.session().raidId));
			if (!granted) {
				throw new IllegalStateException(
						"Unable to grant legacy active-raid emergency firearm");
			}
			if (!add(
					checkpoint.loot(),
					emergencyAmmo(checkpoint.session().raidId))) {
				checkpoint.loot().drop(
						weaponUid(checkpoint.session().raidId));
				throw new IllegalStateException(
						"Unable to grant legacy active-raid emergency ammunition");
			}
		}
		checkpoint.completeEvent(MIGRATION_EVENT_ID);
		return granted;
	}

	public static boolean disposableEmergencyItem(RaidItem item) {
		return item != null && item.itemUid().startsWith(UID_PREFIX);
	}

	private static boolean hasUsableFirearm(BukovRaidCheckpoint checkpoint) {
		for (RaidItem item : checkpoint.loot().items()) {
			if (!isFirearmDefinition(item.definitionId())) continue;
			if (!item.foundInRaid()) {
				// Deployment items are materialized from the ledger when their
				// host instance predates runtime firearm persistence.
				return true;
			}
			Item host = checkpoint.hostItem(item.itemUid());
			if (host instanceof Firearm) {
				return true;
			}
		}
		return false;
	}

	private static boolean isFirearmDefinition(String definitionId) {
		if (definitionId.startsWith("firearm:")) {
			return BukovFirstRaidLootTables.createByEconomicDefinitionId(
					definitionId) != null;
		}
		return BukovFirstRaidLootTables.createByEconomicDefinitionId(
				"firearm:" + definitionId) != null;
	}

	private static RaidItem emergencyWeapon(String raidId) {
		return new RaidItem(
				weaponUid(raidId),
				WEAPON_DEFINITION,
				1,
				0f,
				0,
				true,
				false,
				1f);
	}

	private static RaidItem emergencyAmmo(String raidId) {
		return new RaidItem(
				ammoUid(raidId),
				AMMO_DEFINITION,
				AMMUNITION,
				0f,
				0,
				true,
				false,
				1f);
	}

	private static String weaponUid(String raidId) {
		return UID_PREFIX + raidId + ":weapon";
	}

	private static String ammoUid(String raidId) {
		return UID_PREFIX + raidId + ":ammo";
	}

	private static boolean add(
			LootTransaction loot,
			RaidItem item) {
		return loot.pickup(item) == LootTransaction.PickupResult.ADDED;
	}
}
