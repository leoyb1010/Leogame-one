package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

/**
 * Read-only, allocation-free bridge for the optional HUD sound arc.
 *
 * Rendering reads a snapshot; it cannot feed coordinates or decisions back
 * into simulation.
 */
public interface KeySoundVisualizationSource {

	void readKeySoundVisualEvent(KeySoundVisualEvent target);
}
