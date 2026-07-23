package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;

/**
 * Deterministically selects a walkable extraction marker inside a room mark.
 */
public final class ExtractionCellSelector {

	public static int select(int width,
							 int height,
							 int[] map,
							 BukovRaidLayout.Mark mark,
							 int... forbiddenCells) {
		if (width <= 0 || height <= 0 || map == null
				|| map.length != width * height || mark == null) {
			throw new IllegalArgumentException(
					"valid dimensions, map, and mark are required"
			);
		}

		int centerX = (mark.left + mark.right) / 2;
		int centerY = (mark.top + mark.bottom) / 2;
		int bestCell = -1;
		int bestDistance = Integer.MAX_VALUE;
		int minimumX = Math.max(1, mark.left + 1);
		int maximumX = Math.min(width - 2, mark.right - 1);
		int minimumY = Math.max(1, mark.top + 1);
		int maximumY = Math.min(height - 2, mark.bottom - 1);

		for (int y = minimumY; y <= maximumY; y++) {
			for (int x = minimumX; x <= maximumX; x++) {
				int cell = x + y * width;
				if (contains(forbiddenCells, cell) || !isPassable(map[cell])) {
					continue;
				}
				int deltaX = x - centerX;
				int deltaY = y - centerY;
				int distance = deltaX * deltaX + deltaY * deltaY;
				if (distance < bestDistance
						|| (distance == bestDistance && cell < bestCell)) {
					bestCell = cell;
					bestDistance = distance;
				}
			}
		}
		return bestCell;
	}

	private static boolean contains(int[] cells, int target) {
		if (cells == null) return false;
		for (int cell : cells) {
			if (cell == target) return true;
		}
		return false;
	}

	private static boolean isPassable(int terrain) {
		return terrain >= 0
				&& terrain < Terrain.flags.length
				&& (Terrain.flags[terrain] & Terrain.PASSABLE) != 0;
	}

	private ExtractionCellSelector() {
	}
}
