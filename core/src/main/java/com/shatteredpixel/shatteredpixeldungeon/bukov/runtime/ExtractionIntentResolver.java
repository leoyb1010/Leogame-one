package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.ExtractionState;

/**
 * Pure extraction-input policy shared by the realtime world and unit tests.
 */
public final class ExtractionIntentResolver {

	public static boolean wantsToStart(boolean insideZone,
									   boolean interactHeld,
									   boolean stationary,
									   boolean reloading,
									   int damageTaken) {
		return insideZone
				&& (interactHeld || stationary)
				&& !reloading
				&& damageTaken <= 0;
	}

	public static ExtractionState.Interaction resolve(
			boolean extractionActive,
			boolean insideActiveZone,
			boolean interactHeld,
			boolean stationary,
			boolean reloading,
			int damageTaken) {
		if (!extractionActive) {
			return ExtractionState.Interaction.NONE;
		}
		if (damageTaken > 0) {
			return ExtractionState.Interaction.HEAVY_HIT;
		}
		if (reloading) {
			return ExtractionState.Interaction.RELOADED;
		}
		if (!insideActiveZone || !stationary) {
			return ExtractionState.Interaction.MOVED;
		}
		return interactHeld || stationary
				? ExtractionState.Interaction.ACTIVE
				: ExtractionState.Interaction.NONE;
	}

	private ExtractionIntentResolver() {
	}
}
