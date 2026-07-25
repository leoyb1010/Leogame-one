package com.shatteredpixel.shatteredpixeldungeon.bukov.combat;

import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.CollisionMap;
import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.RealtimeBody;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class HitscanResolverTest {

	@Test
	public void returnsNearestActiveTargetBeforeTerrain() {
		RealtimeBody near = body(4.5f, 2.5f, 0.25f);
		RealtimeBody far = body(5.5f, 2.5f, 0.25f);
		HitscanResolver.Hit hit = new HitscanResolver.Hit();

		HitscanResolver.cast(
				1.5f, 2.5f,
				1f, 0f,
				10f,
				mapWithVerticalWall(8),
				query(Arrays.asList(far, near)),
				null,
				hit
		);

		assertSame(near, hit.body);
		assertEquals(2.75f, hit.distance, 0.0001f);
		assertEquals(4.25f, hit.x, 0.0001f);
		assertEquals(2.5f, hit.y, 0.0001f);
		assertEquals(RealtimeDamage.HitZone.CORE, hit.zone);
	}

	@Test
	public void selectedTargetProvidesItsActualHitZone() {
		RealtimeBody target = body(4.5f, 2.5f, 0.25f);
		HitscanResolver.Hit hit = new HitscanResolver.Hit();
		HitscanResolver.TargetQuery query =
				new HitscanResolver.TargetQuery() {
					@Override
					public Iterable<RealtimeBody> candidates(
							float minX,
							float minY,
							float maxX,
							float maxY) {
						return Collections.singletonList(target);
					}

					@Override
					public RealtimeDamage.HitZone hitZone(
							RealtimeBody body,
							float originX,
							float originY,
							float directionX,
							float directionY) {
						assertSame(target, body);
						return RealtimeDamage.HitZone.LIMB;
					}
				};

		HitscanResolver.cast(
				1.5f, 2.5f,
				1f, 0f,
				10f,
				mapWithVerticalWall(8),
				query,
				null,
				hit);

		assertSame(target, hit.body);
		assertEquals(RealtimeDamage.HitZone.LIMB, hit.zone);
	}

	@Test
	public void terrainStopsTargetsBehindIt() {
		RealtimeBody behindWall = body(7.5f, 2.5f, 0.25f);
		HitscanResolver.Hit hit = new HitscanResolver.Hit();

		HitscanResolver.cast(
				1.5f, 2.5f,
				1f, 0f,
				10f,
				mapWithVerticalWall(6),
				query(Collections.singletonList(behindWall)),
				null,
				hit
		);

		assertNull(hit.body);
		assertNull(hit.zone);
		assertEquals(4.5f, hit.distance, 0.0001f);
		assertEquals(6f, hit.x, 0.0001f);
	}

	@Test
	public void ignoresSpecifiedAndInactiveBodies() {
		RealtimeBody ignored = body(3.5f, 2.5f, 0.25f);
		RealtimeBody inactive = body(4.5f, 2.5f, 0.25f);
		inactive.active = false;
		HitscanResolver.Hit hit = new HitscanResolver.Hit();

		HitscanResolver.cast(
				1.5f, 2.5f,
				1f, 0f,
				4f,
				mapWithVerticalWall(9),
				query(Arrays.asList(ignored, inactive)),
				ignored,
				hit
		);

		assertNull(hit.body);
		assertEquals(4f, hit.distance, 0.0001f);
	}

	@Test
	public void zeroDirectionReturnsZeroDistance() {
		HitscanResolver.Hit hit = new HitscanResolver.Hit();
		HitscanResolver.cast(
				2f, 2f,
				0f, 0f,
				10f,
				mapWithVerticalWall(8),
				query(Collections.emptyList()),
				null,
				hit
		);

		assertNull(hit.body);
		assertEquals(0f, hit.distance, 0.0001f);
	}

	@Test
	public void rayStartingInsideTargetHitsImmediately() {
		assertEquals(
				0f,
				HitscanResolver.rayCircle(
						2f, 2f,
						1f, 0f,
						2f, 2f,
						0.5f
				),
				0.0001f
		);
	}

	@Test
	public void diagonalCornerUsesSupercoverInAllFourDirections() {
		float[][] directions = {
				{1f, 1f},
				{-1f, 1f},
				{1f, -1f},
				{-1f, -1f}
		};
		float cornerDistance = (float)Math.sqrt(0.5f);

		for (float[] direction : directions) {
			int stepX = direction[0] < 0f ? -1 : 1;
			int stepY = direction[1] < 0f ? -1 : 1;
			int[][] horizontalSide = {{3 + stepX, 3}};
			int[][] verticalSide = {{3, 3 + stepY}};
			int[][] bothSides = {
					{3 + stepX, 3},
					{3, 3 + stepY}
			};

			assertCornerOpen(direction);
			assertCornerBlocked(direction, horizontalSide, cornerDistance);
			assertCornerBlocked(direction, verticalSide, cornerDistance);
			assertCornerBlocked(direction, bothSides, cornerDistance);
		}
	}

	private static void assertCornerOpen(float[] direction) {
		HitscanResolver.Hit hit = new HitscanResolver.Hit();
		HitscanResolver.cast(
				3.5f,
				3.5f,
				direction[0],
				direction[1],
				1.5f,
				mapWithWalls(new int[0][0]),
				query(Collections.emptyList()),
				null,
				hit);

		assertNull(hit.body);
		assertEquals(1.5f, hit.distance, 0.0001f);
	}

	private static void assertCornerBlocked(
			float[] direction,
			int[][] walls,
			float expectedDistance) {
		HitscanResolver.Hit hit = new HitscanResolver.Hit();
		HitscanResolver.cast(
				3.5f,
				3.5f,
				direction[0],
				direction[1],
				1.5f,
				mapWithWalls(walls),
				query(Collections.emptyList()),
				null,
				hit);

		assertNull(hit.body);
		assertEquals(expectedDistance, hit.distance, 0.0001f);
	}

	private static RealtimeBody body(float x, float y, float radius) {
		RealtimeBody body = new RealtimeBody();
		body.x = x;
		body.y = y;
		body.previousX = x;
		body.previousY = y;
		body.radius = radius;
		return body;
	}

	private static HitscanResolver.TargetQuery query(List<RealtimeBody> bodies) {
		return (minX, minY, maxX, maxY) -> bodies;
	}

	private static CollisionMap mapWithVerticalWall(int wallX) {
		return new CollisionMap() {
			@Override
			public int width() {
				return 12;
			}

			@Override
			public int height() {
				return 8;
			}

			@Override
			public boolean blocked(int x, int y) {
				return x <= 0 || y <= 0 || x >= 11 || y >= 7 || x == wallX;
			}
		};
	}

	private static CollisionMap mapWithWalls(int[][] walls) {
		return new CollisionMap() {
			@Override
			public int width() {
				return 8;
			}

			@Override
			public int height() {
				return 8;
			}

			@Override
			public boolean blocked(int x, int y) {
				if (x <= 0 || y <= 0 || x >= 7 || y >= 7) {
					return true;
				}
				for (int[] wall : walls) {
					if (wall[0] == x && wall[1] == y) {
						return true;
					}
				}
				return false;
			}
		};
	}
}
