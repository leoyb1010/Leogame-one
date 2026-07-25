package com.shatteredpixel.shatteredpixeldungeon.bukov.performance;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFxEvent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.RealtimeRaidSystem;

/**
 * Small production seam for the realtime work owned by GameScene.
 *
 * Rendering, audio and camera presentation stay in GameScene. This class keeps
 * the simulation and cosmetic queue handoff independently stress-testable
 * without claiming that a headless JVM rendered GPU frames.
 */
public final class BukovGameSceneFrameLoop {

	public static void update(
			RealtimeRaidSystem realtime, float renderDelta) {
		if (realtime == null) {
			throw new IllegalArgumentException(
					"realtime system is required");
		}
		if (!BukovNumbers.isFinite(renderDelta) || renderDelta < 0f) {
			throw new IllegalArgumentException(
					"render delta must be finite and non-negative");
		}
		realtime.update(renderDelta);
	}

	public static int drainCombatFx(
			RealtimeRaidSystem realtime,
			CombatFxEvent.Consumer consumer) {
		if (realtime == null) {
			throw new IllegalArgumentException(
					"realtime system is required");
		}
		if (consumer == null) {
			throw new IllegalArgumentException(
					"combat FX consumer is required");
		}
		return realtime.drainCombatFx(consumer);
	}

	private BukovGameSceneFrameLoop() {
	}
}
