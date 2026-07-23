package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.armor;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.RealtimeDamage;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Static ballistic-armor tuning. Runtime durability belongs to ArmorState.
 */
public final class ArmorDefinition {

	public final String id;
	public final int armorClass;
	public final float maximumDurability;
	public final float penetrationResistance;
	public final float movementPenalty;
	public final float noisePenalty;
	private final EnumSet<RealtimeDamage.HitZone> coverage;

	public ArmorDefinition(
			String id,
			int armorClass,
			float maximumDurability,
			float penetrationResistance,
			Set<RealtimeDamage.HitZone> coverage,
			float movementPenalty,
			float noisePenalty) {
		if (id == null || id.trim().isEmpty()) {
			throw new IllegalArgumentException("id is required");
		}
		if (armorClass < 1 || armorClass > 4) {
			throw new IllegalArgumentException("armorClass must be between 1 and 4");
		}
		this.id = id;
		this.armorClass = armorClass;
		this.maximumDurability =
				finitePositive(maximumDurability, "maximumDurability");
		this.penetrationResistance =
				finitePositive(penetrationResistance, "penetrationResistance");
		this.movementPenalty =
				finiteRange(movementPenalty, "movementPenalty");
		this.noisePenalty = finiteRange(noisePenalty, "noisePenalty");
		if (coverage == null || coverage.isEmpty()) {
			throw new IllegalArgumentException("coverage is required");
		}
		this.coverage = EnumSet.copyOf(coverage);
	}

	public boolean covers(RealtimeDamage.HitZone zone) {
		return zone != null && coverage.contains(zone);
	}

	public Set<RealtimeDamage.HitZone> coverage() {
		return Collections.unmodifiableSet(
				EnumSet.copyOf(coverage));
	}

	private static float finitePositive(float value, String name) {
		if (!BukovNumbers.isFinite(value) || value <= 0f) {
			throw new IllegalArgumentException(name + " must be finite and positive");
		}
		return value;
	}

	private static float finiteRange(float value, String name) {
		if (!BukovNumbers.isFinite(value) || value < 0f || value > 1f) {
			throw new IllegalArgumentException(
					name + " must be finite and between zero and one");
		}
		return value;
	}
}
