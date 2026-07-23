package com.shatteredpixel.shatteredpixeldungeon.bukov.levels.painters;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.RegularPainter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;

import java.util.ArrayList;

/**
 * First-raid painter that keeps the host room graph but removes the City
 * painter's fantasy decoration language.
 */
public final class BukovPainter extends RegularPainter {

	@Override
	protected void decorate(Level level, ArrayList<Room> rooms) {
		int[] map = level.map;
		int width = level.width();
		long seed = com.shatteredpixel.shatteredpixeldungeon.Dungeon.seed;
		for (int cell = 0; cell < map.length; cell++) {
			switch (map[cell]) {
				case Terrain.GRASS:
				case Terrain.HIGH_GRASS:
				case Terrain.FURROWED_GRASS:
				case Terrain.EMPTY_WELL:
					map[cell] = Terrain.EMPTY_DECO;
					break;
				case Terrain.WELL:
					map[cell] = Terrain.CUSTOM_DECO_EMPTY;
					break;
				case Terrain.BOOKSHELF:
					map[cell] = Terrain.WALL_DECO;
					break;
				case Terrain.ALCHEMY:
					map[cell] = Terrain.REGION_DECO;
					break;
				case Terrain.PEDESTAL:
				case Terrain.SECRET_TRAP:
				case Terrain.TRAP:
				case Terrain.INACTIVE_TRAP:
					map[cell] = Terrain.EMPTY_DECO;
					break;
				case Terrain.SECRET_DOOR:
					map[cell] = Terrain.WALL_DECO;
					break;
				case Terrain.CRYSTAL_DOOR:
					map[cell] = Terrain.WALL;
					break;
				case Terrain.BARRICADE:
				case Terrain.MINE_CRYSTAL:
				case Terrain.MINE_BOULDER:
					map[cell] = Terrain.STATUE;
					break;
				case Terrain.EMPTY:
					if (variation(seed, cell) % 19 == 0) {
						map[cell] = Terrain.EMPTY_DECO;
					}
					break;
				case Terrain.WALL:
					if (cell + width < map.length
							&& isWalkableFloor(map[cell + width])
							&& variation(seed ^ 0x6A09E667F3BCC909L, cell)
									% 13 == 0) {
						map[cell] = Terrain.WALL_DECO;
					}
					break;
				default:
					break;
			}
		}
	}

	private static boolean isWalkableFloor(int terrain) {
		return terrain >= 0
				&& terrain < Terrain.flags.length
				&& (Terrain.flags[terrain] & Terrain.PASSABLE) != 0;
	}

	private static long variation(long seed, int cell) {
		long value = seed ^ (cell * 0x9E3779B97F4A7C15L);
		value ^= value >>> 30;
		value *= 0xBF58476D1CE4E5B9L;
		value ^= value >>> 27;
		value *= 0x94D049BB133111EBL;
		return (value ^ value >>> 31) & Long.MAX_VALUE;
	}
}
