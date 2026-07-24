package com.shatteredpixel.shatteredpixeldungeon.bukov.performance;

import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyRangedCombatController;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyRangedCombatIntent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.RealtimeProjectile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.CollisionMap;
import org.junit.Test;

import java.util.Arrays;
import java.util.Locale;

import static org.junit.Assert.assertTrue;

/**
 * Explicit performance task, excluded from the default *Test naming pattern.
 */
public class BukovPerformanceSmoke {

	private static final int ENEMIES = 30;
	private static final int PROJECTILES = 200;
	private static final float FIXED_DT = 1f / 120f;

	@Test
	public void thirtyEnemiesAndTwoHundredProjectilesMeetFixedStepBudget() {
		int simulatedSecondsAt60Hz = Integer.getInteger(
				"bukov.performance.seconds", 60);
		int frames = Math.max(1, simulatedSecondsAt60Hz * 60);
		long[] samples = new long[frames];

		EnemyRangedCombatController.Config config =
				new EnemyRangedCombatController.Config(
						24, 600f, 1.4f, 14f, 0.2f, 4, 8);
		EnemyRangedCombatController[] controllers =
				new EnemyRangedCombatController[ENEMIES];
		EnemyRangedCombatIntent[] intents =
				new EnemyRangedCombatIntent[ENEMIES];
		for (int index = 0; index < ENEMIES; index++) {
			controllers[index] = new EnemyRangedCombatController(
					config, 24, 1_000_000, 0x42554B4F + index);
			intents[index] = new EnemyRangedCombatIntent();
		}

		RealtimeProjectile[] projectiles = new RealtimeProjectile[PROJECTILES];
		for (int index = 0; index < PROJECTILES; index++) {
			projectiles[index] = new RealtimeProjectile();
			launch(projectiles[index], index);
		}
		CollisionMap map = new OpenArena();
		RealtimeProjectile.Listener listener = new NoHitListener();

		for (int warmup = 0; warmup < 300; warmup++) {
			step(controllers, intents, projectiles, map, listener);
			step(controllers, intents, projectiles, map, listener);
		}
		for (int frame = 0; frame < frames; frame++) {
			long started = System.nanoTime();
			step(controllers, intents, projectiles, map, listener);
			step(controllers, intents, projectiles, map, listener);
			samples[frame] = System.nanoTime() - started;
		}

		Arrays.sort(samples);
		double averageMs = average(samples) / 1_000_000d;
		double p95Ms = percentile(samples, 0.95d) / 1_000_000d;
		double p99Ms = percentile(samples, 0.99d) / 1_000_000d;
		System.out.println(String.format(
				Locale.ROOT,
				"{\"benchmark\":\"simulated-inner-loop-cpu\","
						+ "\"wallClockSoak\":false,"
						+ "\"productionGameplayPath\":false,"
						+ "\"simulatedSecondsAt60Hz\":%d,"
						+ "\"simulatedFrames\":%d,"
						+ "\"enemies\":%d,\"projectiles\":%d,"
						+ "\"averageFrameMs\":%.4f,\"p95FrameMs\":%.4f,"
						+ "\"p99FrameMs\":%.4f}",
				simulatedSecondsAt60Hz,
				frames,
				ENEMIES,
				PROJECTILES,
				averageMs,
				p95Ms,
				p99Ms));

		// One rendered 60 Hz frame advances two fixed ticks. The approved
		// simulation budget is P95 < 4.5 ms for that complete pair.
		assertTrue("P95 fixed simulation budget exceeded: " + p95Ms + "ms",
				p95Ms < 4.5d);
	}

	private static void step(
			EnemyRangedCombatController[] controllers,
			EnemyRangedCombatIntent[] intents,
			RealtimeProjectile[] projectiles,
			CollisionMap map,
			RealtimeProjectile.Listener listener) {
		for (int index = 0; index < controllers.length; index++) {
			controllers[index].step(
					FIXED_DT,
					true,
					5f + (index % 3),
					(index % 2 == 0) ? 1f : -1f,
					intents[index]);
		}
		for (int index = 0; index < projectiles.length; index++) {
			RealtimeProjectile projectile = projectiles[index];
			if (!projectile.active) {
				launch(projectile, index);
			}
			projectile.update(FIXED_DT, map, listener);
		}
	}

	private static void launch(RealtimeProjectile projectile, int index) {
		float angle = (float)(index * Math.PI * 2d / PROJECTILES);
		projectile.launch(
				32f,
				32f,
				(float)Math.cos(angle) * 7f,
				(float)Math.sin(angle) * 7f,
				0.05f,
				3f);
	}

	private static long percentile(long[] sorted, double percentile) {
		int index = (int)Math.ceil(sorted.length * percentile) - 1;
		return sorted[Math.max(0, Math.min(sorted.length - 1, index))];
	}

	private static double average(long[] values) {
		double total = 0d;
		for (long value : values) total += value;
		return total / values.length;
	}

	private static final class OpenArena implements CollisionMap {
		@Override
		public int width() {
			return 64;
		}

		@Override
		public int height() {
			return 64;
		}

		@Override
		public boolean blocked(int x, int y) {
			return x <= 0 || y <= 0 || x >= 63 || y >= 63;
		}
	}

	private static final class NoHitListener
			implements RealtimeProjectile.Listener {
		@Override
		public boolean hitTarget(
				RealtimeProjectile projectile,
				float fromX,
				float fromY,
				float toX,
				float toY) {
			return false;
		}

		@Override
		public void hitTerrain(RealtimeProjectile projectile, float x, float y) {
			// The projectile is relaunched on the next fixed step.
		}
	}
}
