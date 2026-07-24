package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.SparseArray;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LevelCollisionMapDoorTest {

	private static final int WIDTH = 9;
	private static final int HEIGHT = 7;
	private static final int CORRIDOR_Y = 3;
	private static final int GATE_X = 4;

	@Test
	public void ordinaryDoorOpensWhenRealtimeBodyApproaches() {
		TestLevel level = corridorLevel(Terrain.DOOR);
		int doorCell = cell(GATE_X, CORRIDOR_Y);
		int[] refreshedCell = {-1};
		LevelCollisionMap map = new LevelCollisionMap(
				level,
				cell -> refreshedCell[0] = cell);
		RealtimeBody body =
				new RealtimeBody(cell(3, CORRIDOR_Y), WIDTH, 0.2f);

		assertFalse("normal door must be pathfindable", map.blocked(
				GATE_X, CORRIDOR_Y));
		new GridCollision(map).move(body, 1.5f, 0f);

		assertEquals(Terrain.OPEN_DOOR, level.map[doorCell]);
		assertFalse(level.solid[doorCell]);
		assertEquals(doorCell, refreshedCell[0]);
		assertTrue("body must pass the opened door", body.x > 4.5f);
	}

	@Test
	public void freshGateBlocksAndCompletedRestoreImmediatelyUnblocks() {
		TestLevel fresh = corridorLevel(Terrain.EMPTY);
		int gateCell = cell(GATE_X, CORRIDOR_Y);
		assertTrue(MissionGateTerrain.apply(
				fresh, new int[]{gateCell}, false, null));
		LevelCollisionMap freshMap = new LevelCollisionMap(fresh);
		assertTrue(freshMap.blocked(GATE_X, CORRIDOR_Y));

		TestLevel restored = corridorLevel(Terrain.LOCKED_DOOR);
		LevelCollisionMap restoredMap = new LevelCollisionMap(restored);
		int[] refreshedCell = {-1};
		assertTrue(MissionGateTerrain.apply(
				restored,
				new int[]{gateCell},
				true,
				cell -> refreshedCell[0] = cell));

		assertEquals(Terrain.OPEN_DOOR, restored.map[gateCell]);
		assertTrue(restored.passable[gateCell]);
		assertFalse(restored.solid[gateCell]);
		assertEquals("visual refresh must target the opened gate",
				gateCell, refreshedCell[0]);
		assertFalse("existing map must observe restored event state",
				restoredMap.blocked(GATE_X, CORRIDOR_Y));
	}

	@Test
	public void samePlayerCollisionInstancePassesAfterMissionUnlock() {
		TestLevel level = corridorLevel(Terrain.LOCKED_DOOR);
		int gateCell = cell(GATE_X, CORRIDOR_Y);
		LevelCollisionMap map = new LevelCollisionMap(level);
		GridCollision collision = new GridCollision(map);
		RealtimeBody body =
				new RealtimeBody(cell(3, CORRIDOR_Y), WIDTH, 0.2f);

		collision.move(body, 1.5f, 0f);
		assertTrue("locked gate must stop the player", body.x < 4f);

		assertTrue(MissionGateTerrain.apply(
				level, new int[]{gateCell}, true, null));
		collision.move(body, 1.5f, 0f);

		assertTrue("unlocked gate must pass without rebuilding collision",
				body.x > 4.5f);
	}

	@Test
	public void enemyRepathsThroughGateAfterMissionUnlock() {
		TestLevel level = corridorLevel(Terrain.LOCKED_DOOR);
		int gateCell = cell(GATE_X, CORRIDOR_Y);
		LevelCollisionMap map = new LevelCollisionMap(level);
		RealtimeEnemyNavigator navigator =
				new RealtimeEnemyNavigator(0, WIDTH, HEIGHT);
		RealtimeEnemyNavigator.Intent intent =
				new RealtimeEnemyNavigator.Intent();
		RealtimeBody enemy =
				new RealtimeBody(cell(2, CORRIDOR_Y), WIDTH, 0.2f);

		navigator.step(
				0.1f,
				enemy.x,
				enemy.y,
				6.5f,
				CORRIDOR_Y + 0.5f,
				false,
				1f,
				0f,
				map,
				intent);
		assertTrue(intent.targetUnreachable());

		MissionGateTerrain.apply(
				level, new int[]{gateCell}, true, null);
		GridCollision collision = new GridCollision(map);
		for (int frame = 0; frame < 160 && enemy.x <= 5.5f; frame++) {
			navigator.step(
					0.05f,
					enemy.x,
					enemy.y,
					6.5f,
					CORRIDOR_Y + 0.5f,
					false,
					1f,
					0f,
					map,
					intent);
			collision.move(
					enemy,
					intent.desiredX() * 2f * 0.05f,
					intent.desiredY() * 2f * 0.05f);
			navigator.observePosition(0.05f, enemy.x, enemy.y);
		}

		assertFalse(intent.targetUnreachable());
		assertTrue("enemy must cross the same opened gate", enemy.x > 5.5f);
	}

	private static TestLevel corridorLevel(int gateTerrain) {
		TestLevel level = new TestLevel();
		Arrays.fill(level.map, Terrain.WALL);
		for (int x = 1; x < WIDTH - 1; x++) {
			level.map[cell(x, CORRIDOR_Y)] = Terrain.EMPTY;
		}
		level.map[cell(GATE_X, CORRIDOR_Y)] = gateTerrain;
		for (int cell = 0; cell < level.length(); cell++) {
			int flags = Terrain.flags[level.map[cell]];
			level.passable[cell] = (flags & Terrain.PASSABLE) != 0;
			level.solid[cell] = (flags & Terrain.SOLID) != 0;
			level.avoid[cell] = (flags & Terrain.AVOID) != 0;
		}
		return level;
	}

	private static int cell(int x, int y) {
		return x + y * WIDTH;
	}

	private static final class TestLevel extends Level {
		TestLevel() {
			setSize(WIDTH, HEIGHT);
			blobs = new HashMap<>();
			traps = new SparseArray<>();
		}

		@Override
		protected boolean build() {
			return true;
		}

		@Override
		protected void createMobs() {
		}

		@Override
		protected void createItems() {
		}
	}
}
