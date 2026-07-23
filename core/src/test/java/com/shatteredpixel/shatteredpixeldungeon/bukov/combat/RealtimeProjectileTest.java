package com.shatteredpixel.shatteredpixeldungeon.bukov.combat;

import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.CollisionMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RealtimeProjectileTest {

	@Test
	public void substepsPreventFastProjectileFromCrossingWall() {
		RealtimeProjectile projectile = new RealtimeProjectile();
		RecordingListener listener = new RecordingListener();
		projectile.launch(1.5f, 2.5f, 10f, 0f, 0.1f, 2f);

		projectile.update(0.4f, mapWithVerticalWall(4), listener);

		assertFalse(projectile.active);
		assertEquals(1, listener.terrainHits);
		assertTrue(listener.lastTerrainX >= 4f);
	}

	@Test
	public void listenerCanConsumeProjectileOnTargetSweep() {
		RealtimeProjectile projectile = new RealtimeProjectile();
		RecordingListener listener = new RecordingListener();
		listener.targetAtX = 2.5f;
		projectile.launch(1.5f, 2.5f, 5f, 0f, 0.1f, 2f);

		projectile.update(0.4f, mapWithVerticalWall(8), listener);

		assertFalse(projectile.active);
		assertEquals(1, listener.targetHits);
		assertEquals(0, listener.terrainHits);
	}

	@Test
	public void expiresAfterLifetime() {
		RealtimeProjectile projectile = new RealtimeProjectile();
		RecordingListener listener = new RecordingListener();
		projectile.launch(1.5f, 2.5f, 1f, 0f, 0.1f, 0.1f);

		projectile.update(0.1f, mapWithVerticalWall(8), listener);

		assertFalse(projectile.active);
		assertEquals(0, listener.targetHits);
		assertEquals(0, listener.terrainHits);
	}

	@Test
	public void inactiveProjectileDoesNotInvokeListener() {
		RealtimeProjectile projectile = new RealtimeProjectile();
		RecordingListener listener = new RecordingListener();

		projectile.update(1f, mapWithVerticalWall(4), listener);

		assertEquals(0, listener.targetHits);
		assertEquals(0, listener.terrainHits);
	}

	private static CollisionMap mapWithVerticalWall(int wallX) {
		return new CollisionMap() {
			@Override
			public int width() {
				return 10;
			}

			@Override
			public int height() {
				return 8;
			}

			@Override
			public boolean blocked(int x, int y) {
				return x <= 0 || y <= 0 || x >= 9 || y >= 7 || x == wallX;
			}
		};
	}

	private static final class RecordingListener
			implements RealtimeProjectile.Listener {

		int targetHits;
		int terrainHits;
		float lastTerrainX;
		float targetAtX = Float.POSITIVE_INFINITY;

		@Override
		public boolean hitTarget(
				RealtimeProjectile projectile,
				float fromX,
				float fromY,
				float toX,
				float toY) {
			if (fromX < targetAtX && toX >= targetAtX) {
				targetHits++;
				return true;
			}
			return false;
		}

		@Override
		public void hitTerrain(
				RealtimeProjectile projectile,
				float x,
				float y) {
			terrainHits++;
			lastTerrainX = x;
		}
	}
}
