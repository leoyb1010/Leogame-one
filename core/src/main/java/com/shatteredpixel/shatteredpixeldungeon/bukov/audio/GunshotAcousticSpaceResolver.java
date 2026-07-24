package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.CollisionMap;

/**
 * Allocation-free approximation of the acoustic enclosure around a shot.
 */
public final class GunshotAcousticSpaceResolver {

	private static final int WALL_PROBE_TILES = 4;

	public static GunshotAcousticSpace resolve(
			CollisionMap collisionMap,
			float sourceX,
			float sourceY) {
		if (collisionMap == null) {
			throw new IllegalArgumentException("collisionMap is required");
		}
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
				.isFinite(sourceX)
				|| !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
						.isFinite(sourceY)) {
			throw new IllegalArgumentException("source position must be finite");
		}

		int x = (int)Math.floor(sourceX);
		int y = (int)Math.floor(sourceY);
		boolean north = wallWithin(collisionMap, x, y, 0, -1);
		boolean south = wallWithin(collisionMap, x, y, 0, 1);
		boolean west = wallWithin(collisionMap, x, y, -1, 0);
		boolean east = wallWithin(collisionMap, x, y, 1, 0);
		boolean verticalChannel = north && south;
		boolean horizontalChannel = west && east;
		int enclosedSides = (north ? 1 : 0)
				+ (south ? 1 : 0)
				+ (west ? 1 : 0)
				+ (east ? 1 : 0);

		if ((verticalChannel && horizontalChannel) || enclosedSides >= 3) {
			return GunshotAcousticSpace.INDOOR;
		}
		if (verticalChannel || horizontalChannel) {
			return GunshotAcousticSpace.CORRIDOR;
		}
		return GunshotAcousticSpace.OPEN;
	}

	private static boolean wallWithin(
			CollisionMap collisionMap,
			int x,
			int y,
			int stepX,
			int stepY) {
		for (int distance = 1; distance <= WALL_PROBE_TILES; distance++) {
			if (collisionMap.blocked(
					x + stepX * distance,
					y + stepY * distance)) {
				return true;
			}
		}
		return false;
	}

	private GunshotAcousticSpaceResolver() {
	}
}
