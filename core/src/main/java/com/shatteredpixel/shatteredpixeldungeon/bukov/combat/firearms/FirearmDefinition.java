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
	/** Human-readable authored family used by audio/FX QA and weapon UI. */
	public String feedbackProfile = "SIDEARM";
	/** Per-weapon mix controls. The same authored sample no longer means the same sound. */
	public float soundPitch = 1f;
	public float soundGain = 1f;
	/** Per-weapon presentation controls consumed by the live firing path. */
	public float muzzleIntensity = 1f;
	public float tracerIntensity = 1f;
	public float impactIntensity = 1f;
	public float feedbackIntensity = 1f;

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
		require(text(feedbackProfile), "missing feedbackProfile: " + id);
		require(finite(soundPitch) && soundPitch >= 0.72f && soundPitch <= 1.28f,
				"soundPitch out of range: " + id);
		require(finite(soundGain) && soundGain >= 0.35f && soundGain <= 1.5f,
				"soundGain out of range: " + id);
		require(unitPresentation(muzzleIntensity),
				"muzzleIntensity out of range: " + id);
		require(unitPresentation(tracerIntensity),
				"tracerIntensity out of range: " + id);
		require(unitPresentation(impactIntensity),
				"impactIntensity out of range: " + id);
		require(unitPresentation(feedbackIntensity),
				"feedbackIntensity out of range: " + id);
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

	private static boolean unitPresentation(float value) {
		return finite(value) && value >= 0.35f && value <= 1.5f;
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new IllegalArgumentException(message);
		}
	}
}
