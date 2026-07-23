package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.RealtimeDamage;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.armor.ArmorCatalog;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.armor.RealtimeArmorState;

import java.util.Collection;

/**
 * Runtime bridge for the deterministic armor/backpack equipment slots.
 */
public final class BukovEquippedGear {

	private final BukovGearRules.Snapshot rules;
	private final RealtimeArmorState armor;

	private BukovEquippedGear(
			BukovGearRules.Snapshot rules,
			RealtimeArmorState armor) {
		this.rules = rules;
		this.armor = armor;
	}

	public static BukovEquippedGear from(Collection<RaidItem> items) {
		BukovGearRules.Snapshot rules = BukovGearRules.resolve(items);
		RealtimeArmorState armor = rules.armor == null
				? null
				: RealtimeArmorState.fromRaidItem(
						rules.armor,
						ArmorCatalog.require(rules.armor.definitionId()));
		return new BukovEquippedGear(rules, armor);
	}

	public float resolveIncomingBullet(
			float incomingDamage,
			float penetration,
			RealtimeDamage.HitZone hitZone) {
		if (armor == null) {
			if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
					.isFinite(incomingDamage) || incomingDamage < 0f) {
				throw new IllegalArgumentException(
						"incomingDamage must be finite and non-negative");
			}
			return incomingDamage;
		}
		return armor.resolveBullet(
				incomingDamage,
				penetration,
				hitZone).healthDamage;
	}

	public float movementMultiplier() {
		return rules.movementMultiplier;
	}

	public float noiseMultiplier() {
		return rules.noiseMultiplier;
	}

	public float weightCapacityKg() {
		return rules.weightCapacityKg;
	}

	public RaidItem armorItem() {
		return armor == null ? null : armor.toRaidItem();
	}

	public RaidItem backpackItem() {
		return rules.backpack == null ? null : rules.backpack.copy();
	}

	public void writeBack(LootTransaction ledger) {
		if (ledger == null) {
			throw new IllegalArgumentException("ledger is required");
		}
		if (armor == null) {
			return;
		}
		RaidItem updated = armor.toRaidItem();
		if (ledger.contains(updated.itemUid())) {
			ledger.replace(updated);
		}
	}
}
