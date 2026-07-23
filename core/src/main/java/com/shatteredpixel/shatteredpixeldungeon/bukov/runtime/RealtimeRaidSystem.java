package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFxEvent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatPresentationEvent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCoordinator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.ExtractionState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidSession;

/**
 * The single authoritative ordering for all realtime raid simulation.
 */
public final class RealtimeRaidSystem {

	public interface World {
		boolean paused();
		void beginFixedStep();
		void pollInput();
		void updatePlayer(float dt);
		void emitPlayerActions(float dt);
		void updateSoundField(float dt);
		void updatePerception(float dt);
		void updateBrains(float dt);
		void updateMobs(float dt);
		void updateProjectiles(float dt);
		void resolveDamageAndDeaths(float dt);
		void updateStatuses(float dt);
		void updateLootAndExtraction(float dt);
		default ExtractionState.Interaction extractionInteraction() {
			return ExtractionState.Interaction.NONE;
		}
		void updateCameraAndHud(float dt);
		void endFixedStep();
		void renderInterpolate(float alpha);
		default int drainCombatFx(CombatFxEvent.Consumer consumer) {
			return 0;
		}
		default int drainCombatPresentation(
				CombatPresentationEvent.Consumer consumer) {
			return 0;
		}
		void disposeRealtimeObjects();
	}

	private final FixedStepClock clock = new FixedStepClock(120f, 0.10f, 8);
	private final World world;
	private final RaidSession session;
	private final BukovRaidCoordinator coordinator;

	public RealtimeRaidSystem(World world, RaidSession session) {
		if (world == null || session == null) {
			throw new IllegalArgumentException("world and session are required");
		}
		this.world = world;
		this.session = session;
		coordinator = null;
	}

	public RealtimeRaidSystem(
			World world,
			BukovRaidCoordinator coordinator) {
		if (world == null || coordinator == null) {
			throw new IllegalArgumentException("world and coordinator are required");
		}
		this.world = world;
		this.coordinator = coordinator;
		session = coordinator.session();
	}

	public void update(float renderDelta) {
		if (world.paused()) {
			clock.reset();
			return;
		}
		clock.advance(renderDelta, this::fixedUpdate);
		world.renderInterpolate(clock.alpha());
	}

	private void fixedUpdate(float dt) {
		world.beginFixedStep();
		world.pollInput();
		world.updatePlayer(dt);
		world.emitPlayerActions(dt);
		world.updateSoundField(dt);
		world.updatePerception(dt);
		world.updateBrains(dt);
		world.updateMobs(dt);
		world.updateProjectiles(dt);
		world.resolveDamageAndDeaths(dt);
		world.updateStatuses(dt);
		world.updateLootAndExtraction(dt);
		world.updateCameraAndHud(dt);
		if (coordinator == null) {
			session.advance(dt);
		} else {
			coordinator.tick(dt, world.extractionInteraction());
		}
		world.endFixedStep();
	}

	public void dispose() {
		world.disposeRealtimeObjects();
		clock.reset();
	}

	public int drainCombatFx(CombatFxEvent.Consumer consumer) {
		if (consumer == null) {
			throw new IllegalArgumentException("consumer is required");
		}
		return world.drainCombatFx(consumer);
	}

	public int drainCombatPresentation(
			CombatPresentationEvent.Consumer consumer) {
		if (consumer == null) {
			throw new IllegalArgumentException("consumer is required");
		}
		return world.drainCombatPresentation(consumer);
	}
}
