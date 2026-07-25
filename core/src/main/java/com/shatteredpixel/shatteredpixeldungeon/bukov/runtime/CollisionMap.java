package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

public interface CollisionMap {
	int width();
	int height();

	/**
	 * Movement and pathfinding obstruction. Interactive ordinary doors may be
	 * reachable here so body collision can call {@link #approach(int, int)}
	 * and open them without making pathfinding treat the route as sealed.
	 */
	boolean blocked(int x, int y);

	/**
	 * Obstruction for line traces such as visibility, hitscan, projectiles and
	 * acoustic occlusion. Most maps share their movement and trace topology;
	 * host-level adapters can override this for interactive closed doors.
	 */
	default boolean blocksLine(int x, int y) {
		return blocked(x, y);
	}

	/**
	 * Called by body collision when an actor is close enough to touch a tile.
	 * Pathfinding remains read-only through {@link #blocked(int, int)}.
	 */
	default void approach(int x, int y) {
		// Most collision maps have no interactive terrain.
	}
}
