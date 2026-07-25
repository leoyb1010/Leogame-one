package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.armor;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.RealtimeDamage;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;

/**
 * Mutable single-layer ballistic armor with RaidItem durability round-tripping.
 */
public final class RealtimeArmorState {

	public static final class HitResult {
		public final float healthDamage;
		public final float absorbedDamage;
		public final boolean armorCovered;
		public final boolean penetrated;
		public final float durabilityFraction;

		private HitResult(
				float healthDamage,
				float absorbedDamage,
				boolean armorCovered,
				boolean penetrated,
				float durabilityFraction) {
			this.healthDamage = healthDamage;
			this.absorbedDamage = absorbedDamage;
			this.armorCovered = armorCovered;
			this.penetrated = penetrated;
			this.durabilityFraction = durabilityFraction;
		}
	}

	private final RaidItem original;
	private final ArmorDefinition definition;
	private float durability;

	private RealtimeArmorState(
			RaidItem original,
			ArmorDefinition definition,
			float durabilityFraction) {
		this.original = original == null ? null : original.copy();
		this.definition = definition;
		this.durability =
				definition.maximumDurability
						* clamp(durabilityFraction, 0f, 1f);
	}

	public static RealtimeArmorState fromRaidItem(
			RaidItem item,
			ArmorDefinition definition) {
		if (item == null || definition == null) {
			throw new IllegalArgumentException("item and definition are required");
		}
		String stored = item.definitionId();
		String expected = definition.id;
		if (!stored.equals(expected)
				&& !stored.equals("armor:" + expected)) {
			throw new IllegalArgumentException(
					"armor definition does not match RaidItem");
		}
		if (item.quantity() != 1) {
			throw new IllegalArgumentException(
					"equipped armor must be one physical item");
		}
		return new RealtimeArmorState(
				item,
				definition,
				item.durability());
	}

	/**
	 * Creates full-condition runtime armor for actors that do not own a
	 * persistent RaidItem, such as an armored enemy.
	 */
	public static RealtimeArmorState fresh(
			ArmorDefinition definition) {
		if (definition == null) {
			throw new IllegalArgumentException("definition is required");
		}
		return new RealtimeArmorState(null, definition, 1f);
	}

	/**
	 * Resolves one bullet through coverage, penetration and durability.
	 */
	public HitResult resolveBullet(
			float incomingDamage,
			float penetration,
			RealtimeDamage.HitZone hitZone) {
		float damage = finiteNonNegative(incomingDamage, "incomingDamage");
		float penetrationValue =
				finiteNonNegative(penetration, "penetration");
		if (hitZone == null) {
			throw new IllegalArgumentException("hitZone is required");
		}
		if (damage == 0f
				|| durability <= 0f
				|| !definition.covers(hitZone)) {
			return new HitResult(
					damage,
					0f,
					false,
					false,
					durabilityFraction());
		}

		float condition = durabilityFraction();
		float effectiveResistance =
				definition.penetrationResistance
						* (0.55f + 0.45f * condition);
		float penetrationRatio =
				penetrationValue / Math.max(0.0001f, effectiveResistance);
		boolean penetrated = penetrationRatio >= 1f;
		float blockRatio = penetrated
				? clamp(0.24f - (penetrationRatio - 1f) * 0.10f, 0.06f, 0.24f)
				: clamp(0.72f - penetrationRatio * 0.34f, 0.38f, 0.72f);
		float absorbed = damage * blockRatio;
		float healthDamage = Math.max(0f, damage - absorbed);
		float durabilityLoss = damage
				* (penetrated ? 0.42f : 0.62f)
				+ penetrationValue * (penetrated ? 0.12f : 0.05f);
		durability = Math.max(0f, durability - durabilityLoss);
		return new HitResult(
				healthDamage,
				absorbed,
				true,
				penetrated,
				durabilityFraction());
	}

	public RaidItem toRaidItem() {
		if (original == null) {
			throw new IllegalStateException(
					"transient armor has no RaidItem");
		}
		return original.withRuntimeState(1, durabilityFraction());
	}

	public ArmorDefinition definition() {
		return definition;
	}

	public float durability() {
		return durability;
	}

	public float durabilityFraction() {
		return clamp(
				durability / definition.maximumDurability,
				0f,
				1f);
	}

	public boolean broken() {
		return durability <= 0f;
	}

	private static float finiteNonNegative(float value, String name) {
		if (!BukovNumbers.isFinite(value) || value < 0f) {
			throw new IllegalArgumentException(
					name + " must be finite and non-negative");
		}
		return value;
	}

	private static float clamp(float value, float minimum, float maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}
}
