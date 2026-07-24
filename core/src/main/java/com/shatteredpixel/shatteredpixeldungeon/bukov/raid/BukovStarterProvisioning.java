package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

/**
 * Grants a coherent first kit and prevents permanent combat-loadout soft locks.
 *
 * A recovery kit is issued when the durable stash has no supported firearm, or
 * when a retained firearm has no compatible ammunition. It uses a
 * settlement-indexed UID, so the physical item never aliases an earlier lost
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
		RaidItem retainedFirearm = preferredSupportedFirearm(profile);
		if (retainedFirearm != null) {
			String ammunitionOfferId =
					recoveryAmmunitionOffer(
							retainedFirearm.definitionId());
			BukovVendorCatalog.Offer ammunition =
					BukovVendorCatalog.require(ammunitionOfferId);
			RaidItem compatibleAmmunition =
					findDefinition(
							profile,
							ammunition.definitionId);
			boolean changed = false;
			if (compatibleAmmunition == null) {
				String ammunitionUid = uniqueUid(
						profile,
						"provision:recovery:"
						+ profile.settlements().size()
						+ ":"
						+ ammunition.offerId);
				profile.stash().deposit(
						ammunition.createItem(ammunitionUid));
				compatibleAmmunition =
						profile.stash().item(ammunitionUid);
				changed = true;
			}
			if (!profile.loadout().contains(
					retainedFirearm.itemUid())) {
				profile.loadout().select(
						retainedFirearm.itemUid(),
						profile.stash());
				changed = true;
			}
			if (!profile.loadout().contains(
					compatibleAmmunition.itemUid())) {
				profile.loadout().select(
						compatibleAmmunition.itemUid(),
						profile.stash());
				changed = true;
			}
			return changed;
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
		RaidItem weapon = findDefinition(profile, "firearm:needle_9");
		RaidItem ammo = findDefinition(profile, "ammo:ammo_9_standard");
		RaidItem bandage = findDefinition(profile, "bandage");
		boolean changed = false;
		if (weapon == null) {
			weaponUid = uniqueUid(profile, weaponUid);
			profile.stash().deposit(new RaidItem(
					weaponUid,
					"firearm:needle_9",
					1,
					0.90f,
					850,
					false,
					false,
					1f));
			weapon = profile.stash().item(weaponUid);
			changed = true;
		}
		if (ammo == null) {
			ammoUid = uniqueUid(profile, ammoUid);
			profile.stash().deposit(new RaidItem(
					ammoUid,
					"ammo:ammo_9_standard",
					ammunition,
					0.012f,
					12,
					false,
					false,
					1f));
			ammo = profile.stash().item(ammoUid);
			changed = true;
		}
		if (bandage == null) {
			medicalUid = uniqueUid(profile, medicalUid);
			profile.stash().deposit(new RaidItem(
					medicalUid,
					"bandage",
					medical,
					0.12f,
					180,
					false,
					false,
					1f));
			bandage = profile.stash().item(medicalUid);
			changed = true;
		}
		if (!profile.loadout().contains(weapon.itemUid())) {
			profile.loadout().select(weapon.itemUid(), profile.stash());
			changed = true;
		}
		if (!profile.loadout().contains(ammo.itemUid())) {
			profile.loadout().select(ammo.itemUid(), profile.stash());
			changed = true;
		}
		if (!profile.loadout().contains(bandage.itemUid())) {
			profile.loadout().select(bandage.itemUid(), profile.stash());
			changed = true;
		}
		return changed;
	}

	private static RaidItem preferredSupportedFirearm(
			BukovProfile profile) {
		for (String itemUid : profile.loadout().selectedUids()) {
			RaidItem item = profile.stash().item(itemUid);
			if (supportedFirearm(item)) return item;
		}
		for (RaidItem item : profile.stash().items()) {
			if (supportedFirearm(item)) return item;
		}
		return null;
	}

	private static boolean supportedFirearm(RaidItem item) {
		return item != null
				&& recoveryAmmunitionOffer(
						item.definitionId()) != null;
	}

	private static RaidItem findDefinition(
			BukovProfile profile, String definitionId) {
		for (RaidItem item : profile.stash().items()) {
			if (definitionId.equals(item.definitionId())) {
				return item;
			}
		}
		return null;
	}

	private static String uniqueUid(
			BukovProfile profile,
			String preferredUid) {
		if (!profile.stash().contains(preferredUid)) {
			return preferredUid;
		}
		int suffix = 2;
		while (profile.stash().contains(
				preferredUid + ":" + suffix)) {
			suffix++;
		}
		return preferredUid + ":" + suffix;
	}

	private static String recoveryAmmunitionOffer(
			String firearmDefinitionId) {
		if (firearmDefinitionId == null
				|| !firearmDefinitionId.startsWith("firearm:")) {
			return null;
		}
		int separator = firearmDefinitionId.lastIndexOf('_');
		if (separator < 0
				|| separator == firearmDefinitionId.length() - 1) {
			return null;
		}
		String caliber = firearmDefinitionId.substring(separator + 1);
		if ("9".equals(caliber)) {
			return "ammo_9_standard_24";
		}
		if ("556".equals(caliber)) {
			return "ammo_556_standard_24";
		}
		if ("762".equals(caliber)) {
			return "ammo_762_standard_20";
		}
		if ("12".equals(caliber) || "12g".equals(caliber)) {
			return "ammo_12g_buckshot_12";
		}
		return null;
	}
}
