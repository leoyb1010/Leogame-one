package com.shatteredpixel.shatteredpixeldungeon.bukov.tutorial;

/**
 * Presentation boundary for the HUD. Reading it never advances simulation or
 * consumes the hint, so render cadence cannot change tutorial behavior.
 */
public interface BukovTutorialHintSource {

	void readTutorialHint(BukovTutorialHintState target);
}
