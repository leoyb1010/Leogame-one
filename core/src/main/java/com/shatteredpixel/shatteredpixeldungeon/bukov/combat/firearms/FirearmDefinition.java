package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms;

public final class FirearmDefinition {

	public String id;
	public String name;
	public FirearmClass weaponClass;
	public String caliber;
	public String defaultAmmo;
	public FireMode fireMode;
	public float damage;
	public float penetration;
	public float rpm;
	public int magazineSize;
	public float reloadSeconds;
	public float effectiveRangeTiles;
	public float baseSpreadDeg;
	public float movingSpreadDeg;
	public float recoilPerShot;
	public float recoilRecovery;
	public int pellets;
	public float noiseRadiusTiles;
	public float weightKg;
	public int value;

	public float secondsPerShot() {
		if (rpm <= 0f) {
			throw new IllegalStateException("rpm must be positive: " + id);
		}
		return 60f / rpm;
	}

	public void validate() {
		require(text(id), "missing id");
		require(text(name), "missing name: " + id);
		require(weaponClass != null, "missing weaponClass: " + id);
		require(text(caliber), "missing caliber: " + id);
		require(text(defaultAmmo), "missing defaultAmmo: " + id);
		require(fireMode != null, "missing fireMode: " + id);
		require(finitePositive(damage), "damage must be positive: " + id);
		require(finiteNonNegative(penetration), "penetration must not be negative: " + id);
		require(finite(rpm) && rpm >= 30f && rpm <= 1500f, "rpm out of range: " + id);
		require(magazineSize > 0 && magazineSize <= 200,
				"magazine out of range: " + id);
		require(finite(reloadSeconds) && reloadSeconds > 0f && reloadSeconds <= 15f,
				"reload out of range: " + id);
		require(finitePositive(effectiveRangeTiles),
				"effective range must be positive: " + id);
		require(finiteNonNegative(baseSpreadDeg)
						&& finiteNonNegative(movingSpreadDeg),
				"spread must not be negative: " + id);
		require(finiteNonNegative(recoilPerShot)
						&& finiteNonNegative(recoilRecovery),
				"recoil must not be negative: " + id);
		require(pellets >= 1 && pellets <= 20, "pellets out of range: " + id);
		require(finiteNonNegative(noiseRadiusTiles),
				"noise radius must not be negative: " + id);
		require(finitePositive(weightKg), "weight must be positive: " + id);
		require(value > 0, "value must be positive: " + id);
	}

	private static boolean finite(float value) {
		return com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
				.isFinite(value);
	}

	private static boolean text(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private static boolean finitePositive(float value) {
		return finite(value) && value > 0f;
	}

	private static boolean finiteNonNegative(float value) {
		return finite(value) && value >= 0f;
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new IllegalArgumentException(message);
		}
	}
}
