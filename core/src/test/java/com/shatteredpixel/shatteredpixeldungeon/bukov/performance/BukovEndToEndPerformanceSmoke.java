package com.shatteredpixel.shatteredpixeldungeon.bukov.performance;

import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.GridLineOfSight;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.RealtimeEnemyBrain;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.HitscanResolver;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.RealtimeDamage;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.RealtimeProjectile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.armor.ArmorDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.armor.RealtimeArmorState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFeedbackPlan;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFeedbackRequest;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFeedbackResolver;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFeedbackType;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFxEvent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFxEventPool;
import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.CollisionMap;
import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.GridCollision;
import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.RealtimeBody;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.BukovExperienceSettings;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContract;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContractTestFixture;
import org.junit.Test;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertTrue;

/**
 * Sustained CPU end-to-end performance gate for the realtime combat pipeline.
 *
 * This intentionally does not instantiate GameScene, World, host actors, audio
 * devices, sprite batching, shaders, or a GPU. It measures the integrated pure
 * simulation path; a real rendered build still needs a separate GPU frame-time
 * capture.
 */
public class BukovEndToEndPerformanceSmoke {

	private static final int ENEMIES = 30;
	private static final int PROJECTILES = 200;
	private static final int MAP_SIZE = 64;
	private static final float FIXED_DT = 1f / 120f;
	private static final ArmorDefinition ENEMY_ARMOR =
			new ArmorDefinition(
					"performance_enemy",
					1,
					80f,
					6f,
					EnumSet.of(RealtimeDamage.HitZone.CORE),
					0f,
					0f);
	private static final CombatFeedbackRequest RIFLE_FEEDBACK =
			new CombatFeedbackRequest(
					CombatFeedbackType.RIFLE_SHOT, 0f, 1f
			);
	private static final CombatFeedbackRequest HIT_FEEDBACK =
			new CombatFeedbackRequest(
					CombatFeedbackType.PLAYER_HIT, 0f, 1f
			);
	private static final CombatFeedbackRequest KILL_FEEDBACK =
			new CombatFeedbackRequest(
					CombatFeedbackType.KILL, 0f, 1f
			);

	@Test
	public void integratedCombatPipelineMeetsCpuAndAllocationBudgets() {
		int simulatedSecondsAt60Hz = Integer.getInteger(
				"bukov.performance.e2e.seconds",
				60
		);
		int frames = Math.max(1, simulatedSecondsAt60Hz * 60);
		long[] samples = new long[frames];
		ExperienceContract contract =
				ExperienceContractTestFixture.load();
		Scenario scenario = new Scenario(
				contract,
				BukovExperienceSettings.defaults(contract)
		);

		for (int warmup = 0; warmup < 600; warmup++) {
			scenario.renderFrame();
		}
		scenario.clearCoverage();

		long allocatedBefore = currentThreadAllocatedBytes();
		long gcCountBefore = gcCount();
		long gcTimeBefore = gcTimeMs();
		for (int frame = 0; frame < frames; frame++) {
			long started = System.nanoTime();
			scenario.renderFrame();
			samples[frame] = System.nanoTime() - started;
		}
		long allocatedAfter = currentThreadAllocatedBytes();
		long gcCountAfter = gcCount();
		long gcTimeAfter = gcTimeMs();

		Arrays.sort(samples);
		double averageMs = average(samples) / 1_000_000d;
		double p95Ms = percentile(samples, 0.95d) / 1_000_000d;
		double p99Ms = percentile(samples, 0.99d) / 1_000_000d;
		long allocatedBytes = delta(allocatedBefore, allocatedAfter);
		double allocatedBytesPerFrame = allocatedBytes < 0L
				? -1d
				: (double)allocatedBytes / frames;
		long collected = delta(gcCountBefore, gcCountAfter);
		long collectionTimeMs = delta(gcTimeBefore, gcTimeAfter);

		System.out.println(String.format(
				Locale.ROOT,
				"{\"benchmark\":\"simulated-integrated-cpu\","
						+ "\"gpuRendered\":false,"
						+ "\"wallClockSoak\":false,"
						+ "\"productionGameplayPath\":false,"
						+ "\"simulatedSecondsAt60Hz\":%d,"
						+ "\"simulatedFrames\":%d,"
						+ "\"map\":\"%dx%d-host-scale-collision\","
						+ "\"enemies\":%d,\"projectiles\":%d,"
						+ "\"averageFrameMs\":%.4f,"
						+ "\"p95FrameMs\":%.4f,"
						+ "\"p99FrameMs\":%.4f,"
						+ "\"allocatedBytes\":%d,"
						+ "\"allocatedBytesPerFrame\":%.2f,"
						+ "\"gcCollections\":%d,"
						+ "\"gcCollectionTimeMs\":%d,"
						+ "\"losChecks\":%d,\"hitscanCasts\":%d,"
						+ "\"projectileSteps\":%d,"
						+ "\"damageQueued\":%d,"
						+ "\"feedbackResolved\":%d,"
						+ "\"fxDrained\":%d,"
						+ "\"collisionQueries\":%d}",
				simulatedSecondsAt60Hz,
				frames,
				MAP_SIZE,
				MAP_SIZE,
				ENEMIES,
				PROJECTILES,
				averageMs,
				p95Ms,
				p99Ms,
				allocatedBytes,
				allocatedBytesPerFrame,
				collected,
				collectionTimeMs,
				scenario.losChecks,
				scenario.hitscanCasts,
				scenario.projectileSteps,
				scenario.damageQueued,
				scenario.feedbackResolved,
				scenario.fxDrained,
				scenario.map.blockedQueries
		));

		scenario.assertCoverage();
		double maximumP95 = Double.parseDouble(System.getProperty(
				"bukov.performance.e2e.maxP95Ms",
				"4.5"
		));
		assertTrue(
				"P95 integrated CPU budget exceeded: " + p95Ms + "ms",
				p95Ms < maximumP95
		);
		if (allocatedBytesPerFrame >= 0d) {
			long maximumAllocated = Long.getLong(
					"bukov.performance.e2e.maxAllocatedBytesPerFrame",
					4096L
			);
			assertTrue(
					"allocation proxy exceeded: "
							+ allocatedBytesPerFrame
							+ " bytes/frame",
					allocatedBytesPerFrame <= maximumAllocated
			);
		}
	}

	private static final class Scenario
			implements RealtimeProjectile.Listener {

		final HostScaleArena map = new HostScaleArena();
		final GridCollision collision = new GridCollision(map);
		final RealtimeBody player = body(32.5f, 32.5f, 0.28f);
		final Enemy[] enemies = new Enemy[ENEMIES];
		final RealtimeBody[] enemyBodies = new RealtimeBody[ENEMIES];
		final ArrayList<RealtimeBody> enemyTargets =
				new ArrayList<>(ENEMIES);
		final ArrayList<RealtimeBody> playerTarget = new ArrayList<>(1);
		final RealtimeProjectile[] projectiles =
				new RealtimeProjectile[PROJECTILES];
		final DamageQueue damageQueue = new DamageQueue(1024);
		final CombatFxEventPool fxPool = new CombatFxEventPool(512);
		final CombatFeedbackPlan feedbackPlan =
				new CombatFeedbackPlan();
		final HitscanResolver.Hit hitscanHit =
				new HitscanResolver.Hit();
		final ExperienceContract contract;
		final BukovExperienceSettings settings;
		final HitscanResolver.TargetQuery enemyQuery =
				(minX, minY, maxX, maxY) -> enemyTargets;
		final HitscanResolver.TargetQuery playerQuery =
				(minX, minY, maxX, maxY) -> playerTarget;
		final CombatFxEvent.Consumer fxConsumer = this::consumeFx;
		long tick;
		long losChecks;
		long hitscanCasts;
		long projectileSteps;
		long damageQueued;
		long feedbackResolved;
		long fxDrained;
		long checksum;
		int playerHealth = 100_000;
		int fxSequence;

		Scenario(ExperienceContract contract,
				 BukovExperienceSettings settings) {
			this.contract = contract;
			this.settings = settings;
			playerTarget.add(player);
			for (int index = 0; index < ENEMIES; index++) {
				float x = 7f + (index % 10) * 5f;
				float y = 8f + (index / 10) * 16f;
				Enemy enemy = new Enemy(index, x, y);
				enemies[index] = enemy;
				enemyBodies[index] = enemy.body;
				enemyTargets.add(enemy.body);
			}
			for (int index = 0; index < PROJECTILES; index++) {
				projectiles[index] = new RealtimeProjectile();
				launch(projectiles[index], index);
			}
		}

		void renderFrame() {
			fixedStep();
			fixedStep();
		}

		void fixedStep() {
			tick++;
			beginBodies();
			movePlayer();
			updatePerceptionAndBrains();
			updateEnemies();
			emitPlayerHitscan();
			updateProjectiles();
			applyDamageQueue();
			fxDrained += fxPool.drain(fxConsumer);
			checksum = checksum * 31L
					+ Float.floatToIntBits(
							feedbackPlan.shakeAmplitudePx()
					)
					+ playerHealth;
		}

		void clearCoverage() {
			losChecks = 0L;
			hitscanCasts = 0L;
			projectileSteps = 0L;
			damageQueued = 0L;
			feedbackResolved = 0L;
			fxDrained = 0L;
			map.blockedQueries = 0L;
		}

		void assertCoverage() {
			assertTrue("LOS was not exercised", losChecks > 0L);
			assertTrue("hitscan was not exercised", hitscanCasts > 0L);
			assertTrue(
					"projectile updates were not exercised",
					projectileSteps > 0L
			);
			assertTrue(
					"damage queue was not exercised",
					damageQueued > 0L
			);
			assertTrue(
					"feedback resolver was not exercised",
					feedbackResolved > 0L
			);
			assertTrue("FX pool was not drained", fxDrained > 0L);
			assertTrue(
					"tile collision was not exercised",
					map.blockedQueries > 0L
			);
			assertTrue(
					"damage queue capacity was exceeded",
					damageQueue.highWaterMark < damageQueue.targets.length
			);
		}

		private void beginBodies() {
			player.beginStep();
			for (Enemy enemy : enemies) {
				enemy.body.beginStep();
			}
			feedbackPlan.clear();
		}

		private void movePlayer() {
			int phase = (int)(tick & 511L);
			float directionX = phase < 128
					? 1f
					: phase < 256 ? 0f : phase < 384 ? -1f : 0f;
			float directionY = phase < 128
					? 0f
					: phase < 256 ? 1f : phase < 384 ? 0f : -1f;
			collision.move(
					player,
					directionX * 1.25f * FIXED_DT,
					directionY * 1.25f * FIXED_DT
			);
		}

		private void updatePerceptionAndBrains() {
			for (Enemy enemy : enemies) {
				if (enemy.brain.perceptionDue(FIXED_DT)) {
					boolean visible = GridLineOfSight.visible(
							enemy.body.x,
							enemy.body.y,
							player.x,
							player.y,
							30f,
							map
					);
					losChecks++;
					enemy.brain.recordPlayer(
							visible,
							player.x,
							player.y
					);
				}
				enemy.brain.decide(
						FIXED_DT,
						enemy.body.x,
						enemy.body.y,
						player.x,
						player.y,
						14f
				);
			}
		}

		private void updateEnemies() {
			for (Enemy enemy : enemies) {
				collision.move(
						enemy.body,
						enemy.brain.desiredX() * 2.15f * FIXED_DT,
						enemy.brain.desiredY() * 2.15f * FIXED_DT
				);
				if (enemy.brain.consumeAttack(0.45f)) {
					enemyHitscan(enemy);
				}
			}
		}

		private void enemyHitscan(Enemy enemy) {
			float directionX = player.x - enemy.body.x;
			float directionY = player.y - enemy.body.y;
			HitscanResolver.cast(
					enemy.body.x,
					enemy.body.y,
					directionX,
					directionY,
					14f,
					map,
					playerQuery,
					enemy.body,
					hitscanHit
			);
			hitscanCasts++;
			emitShotFx(
					enemy.index,
					true,
					enemy.body.x,
					enemy.body.y,
					hitscanHit.x,
					hitscanHit.y,
					directionX,
					directionY
			);
			resolveFeedback(RIFLE_FEEDBACK);
			if (hitscanHit.body == player) {
				float damage = RealtimeDamage.resolve(
						7f,
						1f,
						hitscanHit.distance,
						8f,
						3f,
						RealtimeDamage.HitZone.CORE,
						null
				);
				queueDamage(-1, damage);
				resolveFeedback(HIT_FEEDBACK);
			}
		}

		private void emitPlayerHitscan() {
			if ((tick & 3L) != 0L) {
				return;
			}
			int targetIndex = (int)((tick >>> 2) % ENEMIES);
			Enemy target = enemies[targetIndex];
			float directionX = target.body.x - player.x;
			float directionY = target.body.y - player.y;
			HitscanResolver.cast(
					player.x,
					player.y,
					directionX,
					directionY,
					30f,
					map,
					enemyQuery,
					player,
					hitscanHit
			);
			hitscanCasts++;
			emitShotFx(
					10_000,
					false,
					player.x,
					player.y,
					hitscanHit.x,
					hitscanHit.y,
					directionX,
					directionY
			);
			resolveFeedback(RIFLE_FEEDBACK);
			if (hitscanHit.body != null) {
				int hitIndex = enemyIndex(hitscanHit.body);
				if (hitIndex >= 0) {
					Enemy hit = enemies[hitIndex];
					float damage = RealtimeDamage.resolve(
							18f,
							1f,
							hitscanHit.distance,
							18f,
							8f,
							RealtimeDamage.HitZone.CORE,
							hit.armor
					);
					queueDamage(hitIndex, damage);
				}
			}
		}

		private void updateProjectiles() {
			for (int index = 0; index < projectiles.length; index++) {
				RealtimeProjectile projectile = projectiles[index];
				if (!projectile.active) {
					launch(projectile, index);
				}
				projectile.update(FIXED_DT, map, this);
				projectileSteps++;
			}
		}

		private void applyDamageQueue() {
			for (int index = 0; index < damageQueue.size; index++) {
				int target = damageQueue.targets[index];
				int rounded = Math.max(
						1,
						Math.round(damageQueue.damage[index])
				);
				if (target < 0) {
					playerHealth -= rounded;
					if (playerHealth <= 0) {
						playerHealth = 100_000;
					}
				} else {
					Enemy enemy = enemies[target];
					enemy.health -= rounded;
					if (enemy.health <= 0) {
						enemy.health = 250;
						enemy.armor = RealtimeArmorState.fresh(
								ENEMY_ARMOR);
						resolveFeedback(KILL_FEEDBACK);
					}
				}
			}
			damageQueue.clear();
		}

		private void queueDamage(int target, float damage) {
			damageQueue.add(target, damage);
			damageQueued++;
		}

		private void resolveFeedback(CombatFeedbackRequest request) {
			CombatFeedbackResolver.add(
					request,
					contract,
					settings,
					feedbackPlan
			);
			feedbackResolved++;
		}

		private void emitShotFx(int source,
								boolean hostile,
								float fromX,
								float fromY,
								float toX,
								float toY,
								float directionX,
								float directionY) {
			float length = (float)Math.sqrt(
					directionX * directionX + directionY * directionY
			);
			float normalizedX = length <= 0.0001f
					? 0f
					: directionX / length;
			float normalizedY = length <= 0.0001f
					? 0f
					: directionY / length;
			int sequence = fxSequence++;
			fxPool.muzzle(
					source,
					sequence,
					hostile,
					fromX,
					fromY,
					normalizedX,
					normalizedY,
					1f
			);
			fxPool.tracer(
					source,
					sequence,
					hostile,
					fromX,
					fromY,
					toX,
					toY,
					0.8f
			);
			fxPool.impact(
					source,
					sequence,
					hostile,
					toX,
					toY,
					0.6f
			);
		}

		private void consumeFx(CombatFxEvent event) {
			checksum = checksum * 31L
					+ event.type().ordinal()
					+ event.sequence();
		}

		private int enemyIndex(RealtimeBody body) {
			for (int index = 0; index < enemyBodies.length; index++) {
				if (enemyBodies[index] == body) {
					return index;
				}
			}
			return -1;
		}

		private void launch(RealtimeProjectile projectile, int index) {
			float angle = (float)(index * Math.PI * 2d / PROJECTILES);
			float originX = 32f + (index % 5 - 2) * 0.08f;
			float originY = 32f + (index % 7 - 3) * 0.08f;
			projectile.launch(
					originX,
					originY,
					(float)Math.cos(angle) * 12f,
					(float)Math.sin(angle) * 12f,
					0.05f,
					3f
			);
		}

		@Override
		public boolean hitTarget(RealtimeProjectile projectile,
								 float fromX,
								 float fromY,
								 float toX,
								 float toY) {
			for (int index = 0; index < enemyBodies.length; index++) {
				RealtimeBody body = enemyBodies[index];
				if (segmentTouches(
						fromX,
						fromY,
						toX,
						toY,
						body.x,
						body.y,
						body.radius + projectile.radius
				)) {
					queueDamage(index, 3f);
					fxPool.impact(
							20_000,
							fxSequence++,
							false,
							toX,
							toY,
							0.4f
					);
					return true;
				}
			}
			return false;
		}

		@Override
		public void hitTerrain(
				RealtimeProjectile projectile,
				float x,
				float y) {
			fxPool.impact(
					20_000,
					fxSequence++,
					false,
					x,
					y,
					0.25f
			);
		}

		private static boolean segmentTouches(
				float fromX,
				float fromY,
				float toX,
				float toY,
				float centerX,
				float centerY,
				float radius) {
			float deltaX = toX - fromX;
			float deltaY = toY - fromY;
			float lengthSquared = deltaX * deltaX + deltaY * deltaY;
			float ratio = lengthSquared <= 0.000001f
					? 0f
					: ((centerX - fromX) * deltaX
							+ (centerY - fromY) * deltaY)
							/ lengthSquared;
			ratio = Math.max(0f, Math.min(1f, ratio));
			float closestX = fromX + deltaX * ratio;
			float closestY = fromY + deltaY * ratio;
			float distanceX = centerX - closestX;
			float distanceY = centerY - closestY;
			return distanceX * distanceX + distanceY * distanceY
					<= radius * radius;
		}
	}

	private static final class Enemy {
		final int index;
		final RealtimeBody body;
		final RealtimeEnemyBrain brain;
		RealtimeArmorState armor;
		int health = 250;

		Enemy(int index, float x, float y) {
			this.index = index;
			body = body(x, y, 0.28f);
			brain = new RealtimeEnemyBrain(index);
			armor = RealtimeArmorState.fresh(ENEMY_ARMOR);
		}
	}

	private static final class DamageQueue {
		final int[] targets;
		final float[] damage;
		int size;
		int highWaterMark;

		DamageQueue(int capacity) {
			targets = new int[capacity];
			damage = new float[capacity];
		}

		void add(int target, float amount) {
			if (size >= targets.length) {
				throw new IllegalStateException("damage queue overflow");
			}
			targets[size] = target;
			damage[size] = amount;
			size++;
			highWaterMark = Math.max(highWaterMark, size);
		}

		void clear() {
			size = 0;
		}
	}

	private static final class HostScaleArena implements CollisionMap {
		long blockedQueries;

		@Override
		public int width() {
			return MAP_SIZE;
		}

		@Override
		public int height() {
			return MAP_SIZE;
		}

		@Override
		public boolean blocked(int x, int y) {
			blockedQueries++;
			if (x <= 0 || y <= 0 || x >= MAP_SIZE - 1
					|| y >= MAP_SIZE - 1) {
				return true;
			}
			boolean firstVertical =
					x == 21 && (y < 29 || y > 34);
			boolean secondVertical =
					x == 43 && (y < 13 || y > 18);
			boolean firstHorizontal =
					y == 20 && x >= 8 && x <= 32
							&& (x < 17 || x > 19);
			boolean secondHorizontal =
					y == 45 && x >= 30 && x <= 56
							&& (x < 46 || x > 48);
			return firstVertical
					|| secondVertical
					|| firstHorizontal
					|| secondHorizontal;
		}
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

	private static long percentile(long[] sorted, double percentile) {
		int index = (int)Math.ceil(sorted.length * percentile) - 1;
		return sorted[Math.max(0, Math.min(sorted.length - 1, index))];
	}

	private static double average(long[] values) {
		double total = 0d;
		for (long value : values) {
			total += value;
		}
		return total / values.length;
	}

	@SuppressWarnings("deprecation")
	private static long currentThreadAllocatedBytes() {
		java.lang.management.ThreadMXBean bean =
				ManagementFactory.getThreadMXBean();
		if (!(bean instanceof com.sun.management.ThreadMXBean)) {
			return -1L;
		}
		com.sun.management.ThreadMXBean allocationBean =
				(com.sun.management.ThreadMXBean)bean;
		try {
			if (!allocationBean.isThreadAllocatedMemorySupported()) {
				return -1L;
			}
			if (!allocationBean.isThreadAllocatedMemoryEnabled()) {
				allocationBean.setThreadAllocatedMemoryEnabled(true);
			}
			return allocationBean.getThreadAllocatedBytes(
					Thread.currentThread().getId()
			);
		} catch (RuntimeException ignored) {
			return -1L;
		}
	}

	private static long gcCount() {
		long count = 0L;
		boolean supported = false;
		List<GarbageCollectorMXBean> beans =
				ManagementFactory.getGarbageCollectorMXBeans();
		for (GarbageCollectorMXBean bean : beans) {
			long value = bean.getCollectionCount();
			if (value >= 0L) {
				count += value;
				supported = true;
			}
		}
		return supported ? count : -1L;
	}

	private static long gcTimeMs() {
		long time = 0L;
		boolean supported = false;
		List<GarbageCollectorMXBean> beans =
				ManagementFactory.getGarbageCollectorMXBeans();
		for (GarbageCollectorMXBean bean : beans) {
			long value = bean.getCollectionTime();
			if (value >= 0L) {
				time += value;
				supported = true;
			}
		}
		return supported ? time : -1L;
	}

	private static long delta(long before, long after) {
		return before < 0L || after < 0L
				? -1L
				: Math.max(0L, after - before);
	}
}
