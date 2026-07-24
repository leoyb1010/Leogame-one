package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFxEvent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFxEventPool;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCoordinator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.ExtractionState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidSession;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RealtimeRaidSystemTest {

	@Test
	public void fixedUpdateOrderIsStable() {
		RecordingWorld world = new RecordingWorld();
		RaidSession session = RaidSession.create(7L, "test-raid");
		RealtimeRaidSystem system = new RealtimeRaidSystem(world, session);

		system.update(1f / 120f);

		assertEquals("begin,input,player,actions,sound,perception,brains,mobs,projectiles,damage,status,loot,hud,end,render", world.calls.toString());
		assertEquals(1f / 120f, session.elapsedSeconds, 0.000001f);
	}

	@Test
	public void pauseDoesNotAccumulateOrSimulate() {
		RecordingWorld world = new RecordingWorld();
		world.paused = true;
		RaidSession session = RaidSession.create(9L, "paused-raid");
		RealtimeRaidSystem system = new RealtimeRaidSystem(world, session);

		system.update(1f);

		assertEquals("", world.calls.toString());
		assertEquals(0f, session.elapsedSeconds, 0f);
	}

	@Test
	public void terminalStateStopsRemainingFixedStepsInCurrentFrame() {
		RecordingWorld world = new RecordingWorld();
		world.pauseAfterDamage = true;
		RaidSession session = RaidSession.create(10L, "terminal-raid");
		RealtimeRaidSystem system = new RealtimeRaidSystem(world, session);

		system.update(1f / 60f);

		assertEquals(
				"begin,input,player,actions,sound,perception,brains,mobs,"
						+ "projectiles,damage,status,loot,hud,end,render",
				world.calls.toString());
		assertEquals(1f / 120f, session.elapsedSeconds, 0.000001f);
		system.update(0f);
		assertEquals(1f / 120f, session.elapsedSeconds, 0.000001f);
	}

	@Test
	public void coordinatorOwnsSessionTimeAndExtractionProgress() throws IOException {
		RecordingWorld world = new RecordingWorld();
		world.extractionInteraction = ExtractionState.Interaction.ACTIVE;
		BukovRaidCoordinator coordinator = BukovRaidCoordinator.start(
				new InMemoryBukovSaveService(),
				11L,
				"coordinated",
				20f,
				Collections.singletonList(ExtractionState.basic()));
		assertTrue(coordinator.beginExtraction("E01"));
		RealtimeRaidSystem system = new RealtimeRaidSystem(world, coordinator);

		system.update(1f / 120f);

		assertEquals(1f / 120f, coordinator.session().elapsedSeconds, 0.000001f);
		assertEquals(
				1f / 120f,
				coordinator.extraction("E01").progressSeconds(),
				0.000001f);
	}

	@Test
	public void settledSessionCannotBeSettledTwice() {
		RaidSession session = RaidSession.create(1L, "idempotency");
		session.markSettled();
		assertTrue(session.settled);
		try {
			session.markSettled();
		} catch (IllegalStateException expected) {
			return;
		}
		throw new AssertionError("second settlement must fail");
	}

	@Test
	public void renderOwnerCanDrainWorldCombatFxWithoutGameplayDependency() {
		RecordingWorld world = new RecordingWorld();
		RealtimeRaidSystem system = new RealtimeRaidSystem(
				world,
				RaidSession.create(2L, "fx-seam")
		);
		world.fx.impact(3, 9, true, 4f, 5f, 1f);
		int[] sequence = {-1};

		assertEquals(1, system.drainCombatFx(
				event -> sequence[0] = event.sequence()
		));
		assertEquals(9, sequence[0]);
	}

	private static final class RecordingWorld implements RealtimeRaidSystem.World {
		private final StringBuilder calls = new StringBuilder();
		private final CombatFxEventPool fx = new CombatFxEventPool(4);
		private boolean paused;
		private boolean pauseAfterDamage;
		private ExtractionState.Interaction extractionInteraction =
				ExtractionState.Interaction.NONE;

		private void add(String value) {
			if (calls.length() > 0) calls.append(',');
			calls.append(value);
		}

		@Override public boolean paused() { return paused; }
		@Override public void beginFixedStep() { add("begin"); }
		@Override public void pollInput() { add("input"); }
		@Override public void updatePlayer(float dt) { add("player"); }
		@Override public void emitPlayerActions(float dt) { add("actions"); }
		@Override public void updateSoundField(float dt) { add("sound"); }
		@Override public void updatePerception(float dt) { add("perception"); }
		@Override public void updateBrains(float dt) { add("brains"); }
		@Override public void updateMobs(float dt) { add("mobs"); }
		@Override public void updateProjectiles(float dt) { add("projectiles"); }
		@Override public void resolveDamageAndDeaths(float dt) {
			add("damage");
			if (pauseAfterDamage) paused = true;
		}
		@Override public void updateStatuses(float dt) { add("status"); }
		@Override public void updateLootAndExtraction(float dt) { add("loot"); }
		@Override public ExtractionState.Interaction extractionInteraction() {
			return extractionInteraction;
		}
		@Override public void updateCameraAndHud(float dt) { add("hud"); }
		@Override public void endFixedStep() { add("end"); }
		@Override public void renderInterpolate(float alpha) { add("render"); }
		@Override public int drainCombatFx(CombatFxEvent.Consumer consumer) {
			return fx.drain(consumer);
		}
		@Override public void disposeRealtimeObjects() { add("dispose"); }
	}
}
