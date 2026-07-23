package com.shatteredpixel.shatteredpixeldungeon.bukov.levels.rooms;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;

/**
 * Bukov-only deployment room.
 *
 * It deliberately does not place the inherited adventurer guidebook or any
 * class tutorial pickup. The transition exists only as a durable host spawn
 * anchor; BukovLevel rejects legacy floor transitions at runtime.
 */
public final class BukovEntranceRoom extends StandardRoom {

	@Override
	public int minWidth() {
		return Math.max(super.minWidth(), 7);
	}

	@Override
	public int minHeight() {
		return Math.max(super.minHeight(), 7);
	}

	@Override
	public boolean isEntrance() {
		return true;
	}

	@Override
	public void paint(Level level) {
		Painter.fill(level, this, Terrain.WALL);
		Painter.fill(level, this, 1, Terrain.EMPTY);
		for (Room.Door door : connected.values()) {
			door.set(Room.Door.Type.REGULAR);
		}

		int spawn = level.pointToCell(center());
		Painter.set(level, spawn, Terrain.ENTRANCE);
		level.transitions.add(new LevelTransition(
				level, spawn, LevelTransition.Type.SURFACE));
	}
}
