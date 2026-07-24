package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms;

/** Immutable authored modifier applied by {@link FirearmBuild}. */
public final class FirearmAttachmentDefinition {

	public final String id;
	public final String name;
	public final FirearmAttachmentSlot slot;
	public final float damageMultiplier;
	public final float rangeMultiplier;
	public final float baseSpreadMultiplier;
	public final float movingSpreadMultiplier;
	public final float recoilMultiplier;
	public final float reloadMultiplier;
	public final float magazineMultiplier;
	public final float noiseMultiplier;
	public final float addedWeightKg;

	FirearmAttachmentDefinition(
			String id,
			String name,
			FirearmAttachmentSlot slot,
			float damageMultiplier,
			float rangeMultiplier,
			float baseSpreadMultiplier,
			float movingSpreadMultiplier,
			float recoilMultiplier,
			float reloadMultiplier,
			float magazineMultiplier,
			float noiseMultiplier,
			float addedWeightKg) {
		this.id = requireText(id, "id");
		this.name = requireText(name, "name");
		if (slot == null) throw new IllegalArgumentException("slot is required");
		this.slot = slot;
		this.damageMultiplier = positive(damageMultiplier, "damageMultiplier");
		this.rangeMultiplier = positive(rangeMultiplier, "rangeMultiplier");
		this.baseSpreadMultiplier = positive(
				baseSpreadMultiplier, "baseSpreadMultiplier");
		this.movingSpreadMultiplier = positive(
				movingSpreadMultiplier, "movingSpreadMultiplier");
		this.recoilMultiplier = positive(recoilMultiplier, "recoilMultiplier");
		this.reloadMultiplier = positive(reloadMultiplier, "reloadMultiplier");
		this.magazineMultiplier = positive(
				magazineMultiplier, "magazineMultiplier");
		this.noiseMultiplier = positive(noiseMultiplier, "noiseMultiplier");
		if (!finite(addedWeightKg) || addedWeightKg < 0f) {
			throw new IllegalArgumentException(
					"addedWeightKg must be finite and non-negative");
		}
		this.addedWeightKg = addedWeightKg;
	}

	private static float positive(float value, String field) {
		if (!finite(value) || value <= 0f) {
			throw new IllegalArgumentException(field + " must be finite and positive");
		}
		return value;
	}

	private static boolean finite(float value) {
		return com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
				.isFinite(value);
	}

	private static String requireText(String value, String field) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(field + " is required");
		}
		return value;
	}
}
