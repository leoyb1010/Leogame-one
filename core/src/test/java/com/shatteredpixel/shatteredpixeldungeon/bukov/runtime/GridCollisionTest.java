package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GridCollisionTest {

	private static final class TestMap implements CollisionMap {
		private final int width = 8;
		private final int height = 8;

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
			return x <= 0 || y <= 0 || x >= width - 1 || y >= height - 1 || (x == 4 && y >= 2);
		}
	}

	@Test
	public void preventsTunnelingThroughWall() {
		RealtimeBody body = new RealtimeBody(3 + 3 * 8, 8, 0.28f);
		GridCollision collision = new GridCollision(new TestMap());

		collision.move(body, 4f, 0f);

		assertTrue(body.x < 4f);
		assertEquals(0f, body.velocityX, 0f);
	}

	@Test
	public void slidesAlongWallOnFreeAxis() {
		RealtimeBody body = new RealtimeBody(3 + 3 * 8, 8, 0.28f);
		GridCollision collision = new GridCollision(new TestMap());
		float startY = body.y;

		collision.move(body, 1f, 1f);

		assertTrue(body.x < 4f);
		assertTrue(body.y > startY);
	}

	@Test
	public void inactiveBodyDoesNotMove() {
		RealtimeBody body = new RealtimeBody(2 + 2 * 8, 8, 0.28f);
		body.active = false;

		new GridCollision(new TestMap()).move(body, 1f, 1f);

		assertEquals(2.5f, body.x, 0f);
		assertEquals(2.5f, body.y, 0f);
	}
}
