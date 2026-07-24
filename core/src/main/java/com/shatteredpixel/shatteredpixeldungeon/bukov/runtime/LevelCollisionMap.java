package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;

public final class LevelCollisionMap implements CollisionMap {

	interface CellRefresh {
		void refresh(int cell);
	}

	private final Level level;
	private final CellRefresh cellRefresh;

	public LevelCollisionMap(Level level) {
		this(level, null);
	}

	LevelCollisionMap(Level level, CellRefresh cellRefresh) {
		if (level == null) {
			throw new IllegalArgumentException("level is required");
		}
		this.level = level;
		this.cellRefresh = cellRefresh;
	}

	@Override
	public int width() {
		return level.width();
	}

	@Override
	public int height() {
		return level.height();
	}

	@Override
	public boolean blocked(int x, int y) {
		if (x <= 0 || y <= 0 || x >= width() - 1 || y >= height() - 1) {
			return true;
		}
		int cell = x + y * width();
		int terrain = level.map[cell];
		// A normal door is a traversable realtime interaction, not a wall.
		// LOCKED_DOOR deliberately falls through to the solid flag below.
		if (terrain == Terrain.DOOR || terrain == Terrain.OPEN_DOOR) {
			return false;
		}
		return level.solid[cell] || (!level.passable[cell] && !level.avoid[cell]);
	}

	@Override
	public void approach(int x, int y) {
		if (x <= 0 || y <= 0 || x >= width() - 1 || y >= height() - 1) {
			return;
		}
		int cell = x + y * width();
		if (level.map[cell] != Terrain.DOOR) return;
		Level.set(cell, Terrain.OPEN_DOOR, level);
		if (cellRefresh != null) {
			cellRefresh.refresh(cell);
		}
	}
}
