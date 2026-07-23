package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

/**
 * Coordinate-free simulation summary for atmosphere selection.
 */
public final class BukovAtmosphereSignal {

	private boolean tense;
	private boolean combat;

	public void set(boolean tense, boolean combat) {
		this.tense = tense || combat;
		this.combat = combat;
	}

	public boolean tense() {
		return tense;
	}

	public boolean combat() {
		return combat;
	}
}
