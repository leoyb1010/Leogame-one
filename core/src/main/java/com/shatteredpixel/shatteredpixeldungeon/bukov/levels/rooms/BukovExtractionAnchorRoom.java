package com.shatteredpixel.shatteredpixeldungeon.bukov.levels.rooms;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;

/**
 * Host-compatible far-side anchor for the procedural room graph.
 *
 * Extraction is resolved by BukovRealtimeWorld, never by this transition.
 */
public final class BukovExtractionAnchorRoom extends StandardRoom {

	@Override
	public int minWidth() {
		return Math.max(super.minWidth(), 7);
	}

	@Override
	public int minHeight() {
		return Math.max(super.minHeight(), 7);
	}

	@Override
	public boolean isExit() {
		return true;
	}

	@Override
	public void paint(Level level) {
		Painter.fill(level, this, Terrain.WALL);
		Painter.fill(level, this, 1, Terrain.EMPTY);
		for (Room.Door door : connected.values()) {
			door.set(Room.Door.Type.REGULAR);
		}

		int anchor = level.pointToCell(center());
		Painter.set(level, anchor, Terrain.EXIT);
		level.transitions.add(new LevelTransition(
				level, anchor, LevelTransition.Type.REGULAR_EXIT));
	}
}
