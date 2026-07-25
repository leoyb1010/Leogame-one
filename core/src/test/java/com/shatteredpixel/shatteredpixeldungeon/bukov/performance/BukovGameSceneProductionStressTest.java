package com.shatteredpixel.shatteredpixeldungeon.bukov.performance;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.HitscanResolver;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.RealtimeBodySpatialIndex;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFxEvent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFxEventPool;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.ExtractionState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidSession;
import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.CollisionMap;
import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.RealtimeBody;
import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.RealtimeRaidSystem;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Headless pressure acceptance for the exact realtime seam called by
 * GameScene. This exercises production simulation ordering, broad-phase
 * synchronization, hitscan and the bounded FX event pool. It intentionally
 * makes no GPU, sprite batching, shader or rendered-FPS claim.
 */
public class BukovGameSceneProductionStressTest {

	private static final int ENEMIES = 30;
	private static final int FX_CAPACITY = 128;
	private static final int DEFAULT_FRAMES = 600;
	private static final float RENDER_DELTA = 1f / 60f;

	@Test
	public void gameSceneUsesTheProductionFrameLoopInRenderOrder()
			throws Exception {
		Path scene = Paths.get(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/scenes/GameScene.java");
		if (!Files.isRegularFile(scene)) {
			scene = Paths.get("core").resolve(scene);
		}
		String source = new String(
				Files.readAllBytes(scene),
				StandardCharsets.UTF_8);
		int update = source.indexOf(
				"BukovGameSceneFrameLoop.update(");
		int drain = source.indexOf(
				"BukovGameSceneFrameLoop.drainCombatFx(");

		assertTrue("GameScene must use the production update seam",
				update >= 0);
		assertTrue("GameScene must drain FX after simulation",
				drain > update);
	}

	@Test
	public void thirtyEnemyGunfireKeepsIndexAndFxPoolBounded() {
		int frames = Math.max(1, Integer.getInteger(
				"bukov.gamescene.stress.frames",
				DEFAULT_FRAMES));
		StressWorld world = new StressWorld();
		RealtimeRaidSystem realtime = new RealtimeRaidSystem(
				world,
				RaidSession.create(
						0x42554B4F56535452L,
						"gamescene-production-stress"));
		IdentityHashMap<CombatFxEvent, Boolean> eventIdentities =
				new IdentityHashMap<>();
		EnumSet<CombatFxEvent.Type> observedTypes =
				EnumSet.noneOf(CombatFxEvent.Type.class);
		long drained = 0L;
		long started = System.nanoTime();

		for (int frame = 0; frame < frames; frame++) {
			BukovGameSceneFrameLoop.update(
					realtime, RENDER_DELTA);
			drained += BukovGameSceneFrameLoop.drainCombatFx(
					realtime,
					event -> {
						eventIdentities.put(event, Boolean.TRUE);
						observedTypes.add(event.type());
						assertTrue(event.intensity() > 0f);
						assertFalse(Float.isNaN(event.fromX()));
						assertFalse(Float.isNaN(event.toX()));
					});
			assertEquals(
					"GameScene must drain the cosmetic queue every frame",
					0,
					world.fx.size());
			assertEquals(
					"spatial index lost an active enemy",
					ENEMIES,
					world.indexedBodies());
		}
		long elapsedNanos = System.nanoTime() - started;

		assertEquals(frames * 2L, world.fixedSteps);
		assertEquals(world.hitscanCasts, world.hitscanHits);
		assertTrue(world.hitscanCasts >= frames * ENEMIES * 2L);
		assertEquals(
				world.emittedFx,
				drained + world.fx.dropped());
		assertTrue(
				"dense gunfire did not saturate the production event pool",
				world.fx.dropped() > 0L);
		assertTrue(
				"event objects escaped the fixed-capacity pool",
				eventIdentities.size() <= FX_CAPACITY);
		assertTrue(observedTypes.contains(
				CombatFxEvent.Type.MUZZLE_FLASH));
		assertTrue(observedTypes.contains(
				CombatFxEvent.Type.SHELL));
		assertTrue(observedTypes.contains(
				CombatFxEvent.Type.TRACER));
		assertTrue(observedTypes.contains(
				CombatFxEvent.Type.IMPACT));

		realtime.dispose();
		assertTrue(world.disposed);
		assertEquals(0, world.fx.size());
		assertEquals(0, world.indexedBodies());

		System.out.println(String.format(
				Locale.ROOT,
				"{\"gate\":\"bukov_gamescene_production_stress\","
						+ "\"headless\":true,\"gpuRendered\":false,"
						+ "\"renderedFpsClaim\":false,"
						+ "\"frames\":%d,\"fixedSteps\":%d,"
						+ "\"enemies\":%d,\"hitscanCasts\":%d,"
						+ "\"fxEmitted\":%d,\"fxDrained\":%d,"
						+ "\"fxDropped\":%d,\"eventObjectsObserved\":%d,"
						+ "\"spatialSynchronizations\":%d,"
						+ "\"concurrentModificationErrors\":0,"
						+ "\"elapsedCpuWallMsInformational\":%.3f}",
				frames,
				world.fixedSteps,
				ENEMIES,
				world.hitscanCasts,
				world.emittedFx,
				drained,
				world.fx.dropped(),
				eventIdentities.size(),
				world.spatialSynchronizations,
				elapsedNanos / 1_000_000d));
	}

	private static final class StressWorld
			implements RealtimeRaidSystem.World {

		private final CollisionMap map = new OpenArena();
		private final RealtimeBody player = body(32f, 32f);
		private final ArrayList<RealtimeBody> enemies =
				new ArrayList<>(ENEMIES);
		private final RealtimeBodySpatialIndex index =
				new RealtimeBodySpatialIndex(64, 64);
		private final HitscanResolver.Hit hit =
				new HitscanResolver.Hit();
		private final CombatFxEventPool fx =
				new CombatFxEventPool(FX_CAPACITY);
		private long fixedSteps;
		private long hitscanCasts;
		private long hitscanHits;
		private long emittedFx;
		private long spatialSynchronizations;
		private boolean disposed;

		private StressWorld() {
			for (int enemy = 0; enemy < ENEMIES; enemy++) {
				enemies.add(body(
						7f + enemy % 10 * 5f,
						8f + enemy / 10 * 20f));
			}
			index.rebuild(enemies);
		}

		@Override
		public boolean paused() {
			return false;
		}

		@Override
		public void beginFixedStep() {
			fixedSteps++;
			player.beginStep();
			for (RealtimeBody enemy : enemies) {
				enemy.beginStep();
			}
		}

		@Override
		public void pollInput() {
		}

		@Override
		public void updatePlayer(float dt) {
		}

		@Override
		public void emitPlayerActions(float dt) {
			for (int enemy = 0; enemy < enemies.size(); enemy++) {
				RealtimeBody target = enemies.get(enemy);
				float directionX = target.x - player.x;
				float directionY = target.y - player.y;
				HitscanResolver.cast(
						player.x,
						player.y,
						directionX,
						directionY,
						64f,
						map,
						index::candidates,
						player,
						hit);
				hitscanCasts++;
				if (hit.body != null) hitscanHits++;
				int sequence = (int)(
						fixedSteps * ENEMIES + enemy);
				fx.muzzle(
						enemy, sequence, false,
						player.x, player.y,
						directionX, directionY, 1f);
				fx.shell(
						enemy, sequence, false,
						player.x, player.y,
						-directionY, directionX, 0.8f);
				fx.tracer(
						enemy, sequence, false,
						player.x, player.y,
						hit.x, hit.y, 0.9f);
				fx.impact(
						enemy, sequence, false,
						hit.x, hit.y, 0.7f);
				emittedFx += 4L;
			}
		}

		@Override
		public void updateSoundField(float dt) {
		}

		@Override
		public void updatePerception(float dt) {
		}

		@Override
		public void updateBrains(float dt) {
		}

		@Override
		public void updateMobs(float dt) {
			for (int enemy = 0; enemy < enemies.size(); enemy++) {
				RealtimeBody body = enemies.get(enemy);
				float phase =
						(fixedSteps + enemy * 11L) * 0.03125f;
				body.x = 7f + enemy % 10 * 5f
						+ (float)Math.sin(phase) * 0.75f;
				body.y = 8f + enemy / 10 * 20f
						+ (float)Math.cos(phase) * 0.75f;
				index.update(body);
				spatialSynchronizations++;
			}

			// Exercise the same remove/rebuild lifecycle used when the host
			// roster changes, without allocating replacement bodies.
			int churn = (int)(fixedSteps % ENEMIES);
			RealtimeBody moved = enemies.remove(churn);
			moved.active = false;
			index.update(moved);
			moved.active = true;
			enemies.add(moved);
			index.rebuild(enemies);
			spatialSynchronizations += 2L;
		}

		@Override
		public void updateProjectiles(float dt) {
			// Starter firearms are hitscan in the production world.
		}

		@Override
		public void resolveDamageAndDeaths(float dt) {
		}

		@Override
		public void updateStatuses(float dt) {
		}

		@Override
		public void updateLootAndExtraction(float dt) {
		}

		@Override
		public ExtractionState.Interaction extractionInteraction() {
			return ExtractionState.Interaction.NONE;
		}

		@Override
		public void updateCameraAndHud(float dt) {
		}

		@Override
		public void endFixedStep() {
		}

		@Override
		public void renderInterpolate(float alpha) {
		}

		@Override
		public int drainCombatFx(CombatFxEvent.Consumer consumer) {
			return fx.drain(consumer);
		}

		@Override
		public void disposeRealtimeObjects() {
			fx.clear();
			index.clear();
			enemies.clear();
			disposed = true;
		}

		private int indexedBodies() {
			int count = 0;
			for (RealtimeBody ignored :
					index.candidates(0f, 0f, 64f, 64f)) {
				count++;
			}
			return count;
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
			return x < 0 || y < 0 || x >= width() || y >= height();
		}
	}
}
