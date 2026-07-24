package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmClass;

/**
 * Authored firearm audio contract. Reload points are normalized fractions so
 * attachments may change reload duration without desynchronizing mechanics.
 */
public final class FirearmAudioProfile {

	public final GunshotSoundFamily gunshotFamily;
	public final float magazineOutFraction;
	public final float magazineInFraction;
	public final float chargeFraction;

	public FirearmAudioProfile(
			GunshotSoundFamily gunshotFamily,
			float magazineOutFraction,
			float magazineInFraction,
			float chargeFraction) {
		this.gunshotFamily = gunshotFamily;
		this.magazineOutFraction = magazineOutFraction;
		this.magazineInFraction = magazineInFraction;
		this.chargeFraction = chargeFraction;
		validate();
	}

	public static FirearmAudioProfile defaultFor(
			FirearmClass weaponClass) {
		if (weaponClass == FirearmClass.SUBMACHINE_GUN) {
			return new FirearmAudioProfile(
					GunshotSoundFamily.SMG, 0.12f, 0.60f, 0.88f);
		}
		if (weaponClass == FirearmClass.CARBINE) {
			return new FirearmAudioProfile(
					GunshotSoundFamily.CARBINE, 0.12f, 0.60f, 0.88f);
		}
		if (weaponClass == FirearmClass.ASSAULT_RIFLE
				|| weaponClass == FirearmClass.MARKSMAN_RIFLE) {
			return new FirearmAudioProfile(
					GunshotSoundFamily.RIFLE, 0.14f, 0.64f, 0.90f);
		}
		if (weaponClass == FirearmClass.SHOTGUN) {
			return new FirearmAudioProfile(
					GunshotSoundFamily.SHOTGUN, 0.08f, 0.55f, 0.92f);
		}
		if (weaponClass == FirearmClass.HEAVY_WEAPON) {
			return new FirearmAudioProfile(
					GunshotSoundFamily.HEAVY, 0.15f, 0.70f, 0.93f);
		}
		return new FirearmAudioProfile(
				GunshotSoundFamily.PISTOL, 0.10f, 0.55f, 0.86f);
	}

	public float fraction(ReloadAudioCue cue) {
		if (cue == ReloadAudioCue.MAG_OUT) return magazineOutFraction;
		if (cue == ReloadAudioCue.MAG_IN) return magazineInFraction;
		if (cue == ReloadAudioCue.CHARGE) return chargeFraction;
		throw new IllegalArgumentException("cue is required");
	}

	public void validate() {
		if (gunshotFamily == null) {
			throw new IllegalArgumentException(
					"gunshot sound family is required");
		}
		if (!fraction(magazineOutFraction)
				|| !fraction(magazineInFraction)
				|| !fraction(chargeFraction)
				|| magazineOutFraction >= magazineInFraction
				|| magazineInFraction >= chargeFraction) {
			throw new IllegalArgumentException(
					"reload audio fractions must be ordered inside (0, 1)");
		}
	}

	private static boolean fraction(float value) {
		return com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
				.isFinite(value) && value > 0f && value < 1f;
	}
}
