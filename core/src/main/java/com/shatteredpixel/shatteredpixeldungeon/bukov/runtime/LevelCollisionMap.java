package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;

public final class LevelCollisionMap implements CollisionMap {

	private final Level level;

	public LevelCollisionMap(Level level) {
		if (level == null) {
			throw new IllegalArgumentException("level is required");
		}
		this.level = level;
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
		return level.solid[cell] || (!level.passable[cell] && !level.avoid[cell]);
	}
}
