package com.shatteredpixel.shatteredpixeldungeon.bukov.combat;

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

	public static final class ArmorState {
		public float durability;
		public float resistance;
		public float absorbedLastHit;
	}

	public static float resolve(float weaponDamage,
								float ammoMultiplier,
								float distance,
								float effectiveRange,
								float penetration,
								HitZone zone,
								ArmorState armor) {
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

		if (armor == null || armor.durability <= 0f) {
			return Math.max(1f, incoming);
		}

		float penetrationRatio = penetration / Math.max(1f, armor.resistance);
		float blockRatio = clamp(
				0.72f - penetrationRatio * 0.52f,
				0.08f,
				0.72f
		);
		float blocked = incoming * blockRatio;
		armor.absorbedLastHit = blocked;
		armor.durability = Math.max(
				0f,
				armor.durability - incoming * 0.015f
		);
		return Math.max(1f, incoming - blocked);
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	private RealtimeDamage() {
	}
}
