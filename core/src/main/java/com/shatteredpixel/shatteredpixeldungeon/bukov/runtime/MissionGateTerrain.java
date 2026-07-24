package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;

/**
 * Keeps the mission gate's terrain flags synchronized with its durable event.
 */
final class MissionGateTerrain {

	interface CellRefresh {
		void refresh(int cell);
	}

	private MissionGateTerrain() {
	}

	static boolean apply(
			Level level,
			int[] gateCells,
			boolean unlocked,
			CellRefresh cellRefresh) {
		if (level == null) {
			throw new IllegalArgumentException("level is required");
		}
		if (gateCells == null || gateCells.length == 0) return false;
		int desired = unlocked ? Terrain.OPEN_DOOR : Terrain.LOCKED_DOOR;
		boolean changed = false;
		for (int cell : gateCells) {
			if (cell < 0 || cell >= level.length()
					|| level.map[cell] == desired) {
				continue;
			}
			Level.set(cell, desired, level);
			if (cellRefresh != null) {
				cellRefresh.refresh(cell);
			}
			changed = true;
		}
		return changed;
	}
}
