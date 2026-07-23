package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

/**
 * Grants a coherent first kit and prevents a permanent no-weapon soft lock.
 *
 * A recovery kit is issued only when the durable stash has no firearm. It uses
 * a settlement-indexed UID, so the physical item never aliases an earlier lost
 * kit and repeated hub opens cannot duplicate it.
 */
public final class BukovStarterProvisioning {

	public static final String WEAPON_UID = "provision:first:needle_9";
	public static final String AMMO_UID = "provision:first:ammo_9_standard";
	public static final String MEDICAL_UID = "provision:first:bandage";

	private BukovStarterProvisioning() {
	}

	public static boolean ensure(BukovProfile profile) {
		return ensure(profile, false);
	}

	/**
	 * Issues a kit only while the player is genuinely between raids.
	 *
	 * An active checkpoint owns the deployed firearm and therefore makes the
	 * stash look weaponless by design. Treating that state as a soft lock would
	 * mint a recovery kit every time the player returned to the hideout.
	 */
	public static boolean ensure(BukovProfile profile, boolean activeRaid) {
		if (profile == null) {
			throw new IllegalArgumentException("profile is required");
		}
		if (activeRaid) {
			return false;
		}
		if (hasFirearm(profile)) {
			return false;
		}
		boolean firstKit = profile.stash().distinctItemCount() == 0
				&& profile.settlements().isEmpty()
				&& profile.statistics().successfulRaids() == 0
				&& profile.statistics().deaths() == 0;
		String prefix = firstKit
				? "provision:first:"
				: "provision:recovery:"
						+ profile.settlements().size() + ":";
		String weaponUid = firstKit ? WEAPON_UID : prefix + "needle_9";
		String ammoUid = firstKit ? AMMO_UID : prefix + "ammo_9_standard";
		String medicalUid = firstKit ? MEDICAL_UID : prefix + "bandage";
		int ammunition = firstKit ? 36 : 24;
		int medical = firstKit ? 3 : 1;
		profile.stash().deposit(new RaidItem(
				weaponUid,
				"firearm:needle_9",
				1,
				0.90f,
				850,
				false,
				false,
				1f));
		profile.stash().deposit(new RaidItem(
				ammoUid,
				"ammo:ammo_9_standard",
				ammunition,
				0.012f,
				12,
				false,
				false,
				1f));
		profile.stash().deposit(new RaidItem(
				medicalUid,
				"bandage",
				medical,
				0.12f,
				180,
				false,
				false,
				1f));
		profile.loadout().select(weaponUid, profile.stash());
		profile.loadout().select(ammoUid, profile.stash());
		profile.loadout().select(medicalUid, profile.stash());
		return true;
	}

	private static boolean hasFirearm(BukovProfile profile) {
		for (RaidItem item : profile.stash().items()) {
			if (item.definitionId().startsWith("firearm:")) {
				return true;
			}
		}
		return false;
	}
}
