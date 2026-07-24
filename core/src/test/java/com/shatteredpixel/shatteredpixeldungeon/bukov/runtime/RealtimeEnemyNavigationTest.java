package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RealtimeEnemyNavigationTest {

	@Test
	public void cachedPathPursuesAcrossRoomsInsteadOfPushingIntoWall() {
		TestMap map = new TestMap(12, 9);
		for (int y = 1; y < 7; y++) {
			map.block(5, y);
		}
		RealtimeEnemyNavigator navigator =
				new RealtimeEnemyNavigator(0, map.width(), map.height());
		RealtimeEnemyNavigator.Intent intent =
				new RealtimeEnemyNavigator.Intent();
		RealtimeBody body =
				new RealtimeBody(2 + 2 * map.width(), map.width(), 0.25f);
		GridCollision collision = new GridCollision(map);

		for (int step = 0; step < 300; step++) {
			navigator.step(
					0.05f,
					body.x,
					body.y,
					9.5f,
					2.5f,
					false,
					1f,
					0f,
					map,
					intent);
			collision.move(
					body,
					intent.desiredX() * 2.5f * 0.05f,
					intent.desiredY() * 2.5f * 0.05f);
			navigator.observePosition(0.05f, body.x, body.y);
			if (body.x > 8.5f && body.y < 4.5f) {
				break;
			}
		}

		assertTrue("enemy should cross the separating wall", body.x > 8.5f);
		assertTrue("enemy should return toward target room", body.y < 4.5f);
		assertTrue(navigator.totalSearches() <= 4);
	}

	@Test
	public void unreachableTargetFallsBackWithinStrictNodeBudget() {
		TestMap map = new TestMap(16, 10);
		for (int y = 1; y < map.height() - 1; y++) {
			map.block(8, y);
		}
		RealtimeEnemyNavigator navigator =
				new RealtimeEnemyNavigator(0, map.width(), map.height(), 24);
		RealtimeEnemyNavigator.Intent intent =
				new RealtimeEnemyNavigator.Intent();

		navigator.step(
				0.1f,
				2.5f,
				4.5f,
				13.5f,
				4.5f,
				false,
				1f,
				0f,
				map,
				intent);

		assertTrue(intent.targetUnreachable());
		assertTrue(intent.followingPath());
		assertTrue(navigator.lastExpandedNodes() <= 24);
	}

	@Test
	public void targetTileChangeDeterministicallyRebuildsCache() {
		TestMap map = new TestMap(10, 10);
		RealtimeEnemyNavigator first =
				new RealtimeEnemyNavigator(0, map.width(), map.height());
		RealtimeEnemyNavigator second =
				new RealtimeEnemyNavigator(0, map.width(), map.height());
		RealtimeEnemyNavigator.Intent firstIntent =
				new RealtimeEnemyNavigator.Intent();
		RealtimeEnemyNavigator.Intent secondIntent =
				new RealtimeEnemyNavigator.Intent();

		first.step(0.1f, 2.5f, 2.5f, 7.5f, 2.5f,
				false, 1f, 0f, map, firstIntent);
		second.step(0.1f, 2.5f, 2.5f, 7.5f, 2.5f,
				false, 1f, 0f, map, secondIntent);
		assertEquals(firstIntent.desiredX(), secondIntent.desiredX(), 0f);
		assertEquals(firstIntent.desiredY(), secondIntent.desiredY(), 0f);

		first.step(0.01f, 2.5f, 2.5f, 2.5f, 7.5f,
				false, 0f, 1f, map, firstIntent);
		assertEquals(2, first.totalSearches());
		assertTrue(firstIntent.desiredY() > 0.9f);
	}

	@Test
	public void thirtyEnemiesStayInsideDeterministicSearchBudget() {
		TestMap map = new TestMap(28, 20);
		RealtimeEnemyNavigator[] navigators =
				new RealtimeEnemyNavigator[30];
		RealtimeEnemyNavigator.Intent[] intents =
				new RealtimeEnemyNavigator.Intent[30];
		float[] x = new float[30];
		float[] y = new float[30];
		for (int i = 0; i < navigators.length; i++) {
			navigators[i] = new RealtimeEnemyNavigator(
					i,
					map.width(),
					map.height(),
					512);
			intents[i] = new RealtimeEnemyNavigator.Intent();
			x[i] = 1.5f + i % 10;
			y[i] = 1.5f + i / 10;
		}

		for (int frame = 0; frame < 180; frame++) {
			float targetX = frame < 90 ? 24.5f : 23.5f;
			float targetY = frame < 90 ? 16.5f : 15.5f;
			for (int i = 0; i < navigators.length; i++) {
				float deltaX = targetX - x[i];
				float deltaY = targetY - y[i];
				float inverseLength = 1f / (float)Math.sqrt(
						deltaX * deltaX + deltaY * deltaY);
				navigators[i].step(
						1f / 60f,
						x[i],
						y[i],
						targetX,
						targetY,
						false,
						deltaX * inverseLength,
						deltaY * inverseLength,
						map,
						intents[i]);
				x[i] += intents[i].desiredX() * 2f / 60f;
				y[i] += intents[i].desiredY() * 2f / 60f;
				navigators[i].observePosition(1f / 60f, x[i], y[i]);
			}
		}

		int totalSearches = 0;
		int totalExpanded = 0;
		for (RealtimeEnemyNavigator navigator : navigators) {
			totalSearches += navigator.totalSearches();
			totalExpanded += navigator.totalExpandedNodes();
		}
		assertTrue("only initial and changed-target searches are expected",
				totalSearches <= 70);
		assertTrue(totalExpanded <= totalSearches * 512);
	}

	@Test
	public void directLineOfSightAvoidsUnnecessarySearch() {
		TestMap map = new TestMap(10, 10);
		RealtimeEnemyNavigator navigator =
				new RealtimeEnemyNavigator(0, map.width(), map.height());
		RealtimeEnemyNavigator.Intent intent =
				new RealtimeEnemyNavigator.Intent();

		navigator.step(
				0.1f,
				2.5f,
				2.5f,
				7.5f,
				2.5f,
				true,
				1f,
				0f,
				map,
				intent);

		assertEquals(1f, intent.desiredX(), 0f);
		assertFalse(intent.followingPath());
		assertEquals(0, navigator.totalSearches());
	}

	@Test
	public void collisionStallInvalidatesCachedPathAfterGracePeriod() {
		TestMap map = new TestMap(10, 10);
		RealtimeEnemyNavigator navigator =
				new RealtimeEnemyNavigator(0, map.width(), map.height());
		RealtimeEnemyNavigator.Intent intent =
				new RealtimeEnemyNavigator.Intent();

		for (int frame = 0; frame < 6; frame++) {
			navigator.step(
					0.08f,
					2.5f,
					2.5f,
					7.5f,
					2.5f,
					false,
					1f,
					0f,
					map,
					intent);
			// Simulates collision/body pressure holding the enemy in place.
			navigator.observePosition(0.08f, 2.5f, 2.5f);
		}

		assertEquals(2, navigator.totalSearches());
		assertTrue(intent.followingPath());
	}

	private static final class TestMap implements CollisionMap {
		private final int width;
		private final int height;
		private final boolean[] blocked;

		private TestMap(int width, int height) {
			this.width = width;
			this.height = height;
			blocked = new boolean[width * height];
		}

		private void block(int x, int y) {
			blocked[x + y * width] = true;
		}

		@Override
		public int width() {
			return width;
		}

		@Override
		public int height() {
			return height;
		}

		@Override
		public boolean blocked(int x, int y) {
			return x <= 0 || y <= 0
					|| x >= width - 1 || y >= height - 1
					|| blocked[x + y * width];
		}
	}
}
