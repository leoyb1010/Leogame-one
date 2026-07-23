package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

/**
 * Read-only bridge from the realtime simulation to the raid HUD.
 *
 * The HUD owns and reuses the target object. Implementations must only copy
 * presentation state into it; they must never advance simulation or consume
 * input while a render frame is being drawn.
 */
public interface BukovRaidHudSource {

	void readRaidHudState(BukovRaidHudState target);
}
