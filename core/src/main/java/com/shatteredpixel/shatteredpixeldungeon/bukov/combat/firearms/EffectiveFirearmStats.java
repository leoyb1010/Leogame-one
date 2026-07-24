package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms;

/** Final ballistic/runtime values after every installed attachment is applied. */
public final class EffectiveFirearmStats {

	public final float damage;
	public final float penetration;
	public final float rpm;
	public final int magazineSize;
	public final float reloadSeconds;
	public final float effectiveRangeTiles;
	public final float baseSpreadDeg;
	public final float movingSpreadDeg;
	public final float recoilPerShot;
	public final float noiseRadiusTiles;
	public final float weightKg;

	EffectiveFirearmStats(
			float damage,
			float penetration,
			float rpm,
			int magazineSize,
			float reloadSeconds,
			float effectiveRangeTiles,
			float baseSpreadDeg,
			float movingSpreadDeg,
			float recoilPerShot,
			float noiseRadiusTiles,
			float weightKg) {
		this.damage = damage;
		this.penetration = penetration;
		this.rpm = rpm;
		this.magazineSize = magazineSize;
		this.reloadSeconds = reloadSeconds;
		this.effectiveRangeTiles = effectiveRangeTiles;
		this.baseSpreadDeg = baseSpreadDeg;
		this.movingSpreadDeg = movingSpreadDeg;
		this.recoilPerShot = recoilPerShot;
		this.noiseRadiusTiles = noiseRadiusTiles;
		this.weightKg = weightKg;
	}
}
