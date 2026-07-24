package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.ExtractionState;

/**
 * Pure extraction-input policy shared by the realtime world and unit tests.
 */
public final class ExtractionIntentResolver {

	/**
	 * Matches the realtime medical system's severe-hit boundary. Smaller
	 * impacts pressure the extraction timer without deleting all progress.
	 */
	public static final float HEAVY_HIT_HEALTH_FRACTION = 0.18f;

	public static boolean wantsToStart(boolean insideZone,
									   boolean interactHeld,
									   boolean stationary,
									   boolean reloading,
									   int damageTaken) {
		return insideZone
				&& interactHeld
				&& stationary
				&& !reloading
				&& damageTaken <= 0;
	}

	public static ExtractionState.Interaction resolve(
			boolean extractionActive,
			boolean insideActiveZone,
			boolean interactHeld,
			boolean stationary,
			boolean reloading,
			int damageTaken,
			int maximumHealth) {
		if (!extractionActive) {
			return ExtractionState.Interaction.NONE;
		}
		if (damageTaken > 0) {
			return damageInteraction(damageTaken, maximumHealth);
		}
		if (reloading) {
			return ExtractionState.Interaction.RELOADED;
		}
		if (!insideActiveZone || !stationary || !interactHeld) {
			return ExtractionState.Interaction.MOVED;
		}
		return ExtractionState.Interaction.ACTIVE;
	}

	static ExtractionState.Interaction damageInteraction(
			int damageTaken,
			int maximumHealth) {
		if (damageTaken <= 0) {
			return ExtractionState.Interaction.NONE;
		}
		if (maximumHealth <= 0) {
			return ExtractionState.Interaction.HEAVY_HIT;
		}
		float fraction = damageTaken / (float)maximumHealth;
		return fraction >= HEAVY_HIT_HEALTH_FRACTION
				? ExtractionState.Interaction.HEAVY_HIT
				: ExtractionState.Interaction.LIGHT_HIT;
	}

	private ExtractionIntentResolver() {
	}
}
