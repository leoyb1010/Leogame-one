package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms;

public final class AmmoDefinition {

	public String id;
	public String name;
	public AmmoVariant variant;
	public String caliber;
	public float damageMultiplier;
	public float penetrationMultiplier;
	public float noiseMultiplier;
	public float weightKg;
	public int value;

	public float applyDamage(float baseDamage) {
		return baseDamage * damageMultiplier;
	}

	public float applyPenetration(float basePenetration) {
		return basePenetration * penetrationMultiplier;
	}

	public float applyNoise(float baseNoise) {
		return baseNoise * noiseMultiplier;
	}

	public void validate() {
		require(text(id), "missing id");
		require(text(name), "missing name: " + id);
		require(variant != null, "missing variant: " + id);
		require(text(caliber), "missing caliber: " + id);
		require(finitePositive(damageMultiplier), "invalid damage multiplier: " + id);
		require(finitePositive(penetrationMultiplier),
				"invalid penetration multiplier: " + id);
		require(finitePositive(noiseMultiplier), "invalid noise multiplier: " + id);
		require(com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(weightKg) && weightKg > 0f,
				"invalid weight: " + id);
		require(value > 0, "invalid value: " + id);
	}

	private static boolean finitePositive(float value) {
		return com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(value) && value > 0f;
	}

	private static boolean text(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new IllegalArgumentException(message);
		}
	}
}
