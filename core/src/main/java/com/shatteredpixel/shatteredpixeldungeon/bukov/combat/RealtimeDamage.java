package com.shatteredpixel.shatteredpixeldungeon.bukov.combat;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.armor.RealtimeArmorState;

public final class RealtimeDamage {

	public enum HitZone {
		CORE(1f),
		LIMB(0.72f),
		BOSS_WEAKPOINT(1.5f);

		public final float multiplier;

		HitZone(float multiplier) {
			this.multiplier = multiplier;
		}
	}

	public static float resolve(float weaponDamage,
								float ammoMultiplier,
								float distance,
								float effectiveRange,
								float penetration,
								HitZone zone,
								RealtimeArmorState armor) {
		if (weaponDamage < 0f || ammoMultiplier < 0f
				|| distance < 0f || penetration < 0f) {
			throw new IllegalArgumentException("damage inputs must not be negative");
		}
		if (zone == null) {
			throw new IllegalArgumentException("zone is required");
		}

		float rangeRatio = effectiveRange <= 0f
				? 1f
				: distance / effectiveRange;
		float rangeFactor = rangeRatio <= 1f
				? 1f
				: Math.max(0.45f, 1f - (rangeRatio - 1f) * 0.25f);
		float incoming = weaponDamage
				* ammoMultiplier
				* rangeFactor
				* zone.multiplier;

		if (armor == null) {
			return Math.max(1f, incoming);
		}
		return Math.max(
				1f,
				armor.resolveBullet(
						incoming,
						penetration,
						zone).healthDamage);
	}

	private RealtimeDamage() {
	}
}
