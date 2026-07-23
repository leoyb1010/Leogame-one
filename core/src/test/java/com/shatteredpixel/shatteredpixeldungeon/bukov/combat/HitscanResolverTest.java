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
}
