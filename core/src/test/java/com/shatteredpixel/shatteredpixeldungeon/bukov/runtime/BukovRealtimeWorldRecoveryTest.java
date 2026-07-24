package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.HitscanResolver;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFxEvent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFxEventPool;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.SparseArray;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovRealtimeWorldRecoveryTest {

	private static final int WIDTH = 8;
	private static final int HEIGHT = 7;

	@Test
	public void worldInitializationRepairsAfterGateTerrainAndBeforeFirearms()
			throws Exception {
		String source = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java")),
				StandardCharsets.UTF_8);
		int gateTerrain = source.indexOf("applyMissionGateTerrain();");
		int recovery = source.indexOf(
				"recoverHeroCheckpoint(hero, heroBody, collisionMap)");
		int firearms = source.indexOf("firearmRegistry.loadDefault();");

		assertTrue(gateTerrain >= 0);
		assertTrue(recovery > gateTerrain);
		assertTrue(firearms > recovery);
	}

	@Test
	public void blockedCheckpointMovesToNearestOpenCellAndSynchronizesState() {
		TestLevel level = blockedLevel();
		open(level, 3, 2);
		refreshFlags(level);
		LevelCollisionMap collision = new LevelCollisionMap(level);
		RealtimeBody body = body(3.5f, 3.5f);
		body.previousX = 2.5f;
		body.previousY = 3.5f;
		body.velocityX = 4f;
		body.velocityY = -2f;
		Hero hero = new Hero();
		hero.pos = cell(3, 3);

		assertTrue(BukovRealtimeWorld.recoverHeroCheckpoint(
				hero,
				body,
				collision));

		assertEquals(cell(3, 2), hero.pos);
		assertEquals(3.5f, body.x, 0f);
		assertEquals(2.5f, body.y, 0f);
		assertEquals(body.x, body.previousX, 0f);
		assertEquals(body.y, body.previousY, 0f);
		assertEquals(0f, body.velocityX, 0f);
		assertEquals(0f, body.velocityY, 0f);
		assertFalse(collision.blocked(
				(int)Math.floor(body.x),
				(int)Math.floor(body.y)));
	}

	@Test
	public void mismatchedCheckpointUsesRealtimeCellAndSynchronizesHeroCell() {
		TestLevel level = blockedLevel();
		open(level, 1, 1);
		open(level, 4, 3);
		refreshFlags(level);
		RealtimeBody body = body(4.2f, 3.7f);
		Hero hero = new Hero();
		hero.pos = cell(1, 1);

		assertTrue(BukovRealtimeWorld.recoverHeroCheckpoint(
				hero,
				body,
				new LevelCollisionMap(level)));

		assertEquals(cell(4, 3), hero.pos);
		assertEquals(4.5f, body.x, 0f);
		assertEquals(3.5f, body.y, 0f);
		assertEquals(body.x, body.previousX, 0f);
		assertEquals(body.y, body.previousY, 0f);
	}

	@Test
	public void validMatchingCheckpointKeepsContinuousSubtilePosition() {
		TestLevel level = blockedLevel();
		open(level, 4, 3);
		refreshFlags(level);
		RealtimeBody body = body(4.2f, 3.7f);
		Hero hero = new Hero();
		hero.pos = cell(4, 3);

		assertFalse(BukovRealtimeWorld.recoverHeroCheckpoint(
				hero,
				body,
				new LevelCollisionMap(level)));

		assertEquals(cell(4, 3), hero.pos);
		assertEquals(4.2f, body.x, 0f);
		assertEquals(3.7f, body.y, 0f);
	}

	@Test
	public void recoveredBodyProducesNonZeroTracerThroughProductionWorldPath() {
		TestLevel level = blockedLevel();
		for (int x = 1; x < WIDTH - 1; x++) {
			open(level, x, 2);
		}
		refreshFlags(level);
		LevelCollisionMap collision = new LevelCollisionMap(level);
		RealtimeBody body = body(3.5f, 3.5f);
		Hero hero = new Hero();
		hero.pos = cell(3, 3);
		assertTrue(BukovRealtimeWorld.recoverHeroCheckpoint(
				hero,
				body,
				collision));
		assertEquals(cell(3, 2), hero.pos);

		HitscanResolver.Hit hit = new HitscanResolver.Hit();
		CombatFxEventPool fx = new CombatFxEventPool(4);
		BukovRealtimeWorld.resolvePlayerShot(
				7,
				11,
				body.x,
				body.y,
				1f,
				0f,
				12f,
				0.8f,
				collision,
				(minX, minY, maxX, maxY) -> Collections.emptyList(),
				body,
				hit,
				fx);

		assertTrue(hit.distance >= 1f);
		assertTrue(hit.x > body.x);
		assertEquals(1, fx.drain(event -> assertNonZeroTracer(event, body)));
	}

	private static void assertNonZeroTracer(
			CombatFxEvent event,
			RealtimeBody body) {
		assertEquals(CombatFxEvent.Type.TRACER, event.type());
		assertEquals(body.x, event.fromX(), 0f);
		assertEquals(body.y, event.fromY(), 0f);
		float deltaX = event.toX() - event.fromX();
		float deltaY = event.toY() - event.fromY();
		assertTrue(deltaX * deltaX + deltaY * deltaY >= 1f);
	}

	private static TestLevel blockedLevel() {
		TestLevel level = new TestLevel();
		Arrays.fill(level.map, Terrain.WALL);
		return level;
	}

	private static void open(TestLevel level, int x, int y) {
		level.map[cell(x, y)] = Terrain.EMPTY;
	}

	private static void refreshFlags(TestLevel level) {
		for (int cell = 0; cell < level.length(); cell++) {
			int flags = Terrain.flags[level.map[cell]];
			level.passable[cell] = (flags & Terrain.PASSABLE) != 0;
			level.solid[cell] = (flags & Terrain.SOLID) != 0;
			level.avoid[cell] = (flags & Terrain.AVOID) != 0;
		}
	}

	private static RealtimeBody body(float x, float y) {
		RealtimeBody body = new RealtimeBody();
		body.x = x;
		body.y = y;
		body.previousX = x;
		body.previousY = y;
		body.radius = 0.28f;
		return body;
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
