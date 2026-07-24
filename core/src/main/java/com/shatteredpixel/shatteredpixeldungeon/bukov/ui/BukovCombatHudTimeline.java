package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;

import java.util.HashMap;
import java.util.Map;

/**
 * Realtime-only presentation clock for the raid HUD.
 *
 * Simulation events explicitly wake the HUD. Directional damage indicators
 * are kept here instead of on actors so ongoing self damage (bleeding, pain)
 * can never accidentally manufacture a direction.
 */
public final class BukovCombatHudTimeline {

	public static final float ACTIVE_HOLD_SECONDS = 8f;
	public static final float FADE_SECONDS = 0.35f;
	public static final float IDLE_ALPHA = 0.30f;
	public static final float HIT_LIFETIME_SECONDS = 0.50f;
	public static final float SAME_SOURCE_DEDUP_SECONDS = 0.20f;
	public static final int MAX_HIT_DIRECTIONS = 3;

	private static final class Hit {
		int sourceId = -1;
		BukovRaidHudState.Direction direction;
		float strength;
		float remaining;

		void clear() {
			sourceId = -1;
			direction = null;
			strength = 0f;
			remaining = 0f;
		}
	}

	private final Hit[] hits = new Hit[MAX_HIT_DIRECTIONS];
	private final Map<Integer, Float> lastAcceptedBySource =
			new HashMap<>();
	private float elapsedSeconds;
	private float idleSeconds;

	public BukovCombatHudTimeline() {
		for (int index = 0; index < hits.length; index++) {
			hits[index] = new Hit();
		}
	}

	public void advance(float seconds) {
		if (!BukovNumbers.isFinite(seconds) || seconds <= 0f) return;
		elapsedSeconds += seconds;
		idleSeconds += seconds;
		for (Hit hit : hits) {
			if (hit.remaining <= 0f) continue;
			hit.remaining = Math.max(0f, hit.remaining - seconds);
			if (hit.remaining <= 0f) hit.clear();
		}
	}

	/** Firing and entering a new room both call this directly. */
	public void activity() {
		idleSeconds = 0f;
	}

	/**
	 * Records damage for HUD presentation.
	 *
	 * @param ongoing true for bleeding and other self-ticking damage; it wakes
	 *                the HUD but deliberately creates no direction arc.
	 * @return true only when a new direction arc was accepted.
	 */
	public boolean damage(
			int sourceId,
			BukovRaidHudState.Direction direction,
			float strength,
			boolean ongoing) {
		activity();
		if (ongoing || sourceId < 0 || direction == null) return false;

		Float lastAccepted = lastAcceptedBySource.get(sourceId);
		if (lastAccepted != null
				&& elapsedSeconds - lastAccepted
						< SAME_SOURCE_DEDUP_SECONDS) {
			return false;
		}

		Hit selected = null;
		for (Hit hit : hits) {
			if (hit.remaining <= 0f) {
				selected = hit;
				break;
			}
			if (selected == null || hit.remaining < selected.remaining) {
				selected = hit;
			}
		}
		selected.sourceId = sourceId;
		selected.direction = direction;
		selected.strength = clamp01(strength);
		selected.remaining = HIT_LIFETIME_SECONDS;
		lastAcceptedBySource.put(sourceId, elapsedSeconds);
		return true;
	}

	public float awarenessAlpha() {
		if (idleSeconds <= ACTIVE_HOLD_SECONDS) return 1f;
		float fade = Math.min(
				1f,
				(idleSeconds - ACTIVE_HOLD_SECONDS) / FADE_SECONDS);
		return 1f + (IDLE_ALPHA - 1f) * fade;
	}

	public int hitCount() {
		int result = 0;
		for (Hit hit : hits) {
			if (hit.remaining > 0f) result++;
		}
		return result;
	}

	public void copyTo(BukovRaidHudState target) {
		if (target == null) {
			throw new IllegalArgumentException("HUD state target is required");
		}
		target.combatAwareness(awarenessAlpha());
		for (Hit hit : hits) {
			if (hit.remaining > 0f) {
				target.hit(
						hit.direction,
						hit.strength,
						hit.remaining);
			}
		}
	}

	private static float clamp01(float value) {
		if (!BukovNumbers.isFinite(value)) return 0f;
		return Math.max(0f, Math.min(1f, value));
	}
}
