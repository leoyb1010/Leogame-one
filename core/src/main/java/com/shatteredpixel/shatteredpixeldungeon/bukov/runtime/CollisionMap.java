package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

public interface CollisionMap {
	int width();
	int height();
	boolean blocked(int x, int y);

	/**
	 * Called by body collision when an actor is close enough to touch a tile.
	 * Pathfinding remains read-only through {@link #blocked(int, int)}.
	 */
	default void approach(int x, int y) {
		// Most collision maps have no interactive terrain.
	}
}
