package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;

/**
 * Data-only environmental tradeoffs consumed by the existing realtime loop.
 *
 * <p>Rules may tune movement, perception, treatment, reload and reinforcement
 * cadence, but never mutate terrain or apply direct damage. This keeps mission
 * items, locked gates and every extraction route under the existing validated
 * topology contract.</p>
 */
public final class ThemeEnvironmentRules {

	public enum Surface {
		WATER(Terrain.WATER),
		EMBERS(Terrain.EMBERS),
		EMPTY_SP(Terrain.EMPTY_SP),
		EMPTY_DECO(Terrain.EMPTY_DECO),
		CUSTOM_DECO_EMPTY(Terrain.CUSTOM_DECO_EMPTY);

		private final int terrain;

		Surface(int terrain) {
			this.terrain = terrain;
		}

		public boolean matches(int terrain) {
			return this.terrain == terrain;
		}
	}

	public final String id;
	public final Surface surface;
	public final float surfaceMovementMultiplier;
	public final float surfaceEnemySightMultiplier;
	public final float surfaceEnemyHearingMultiplier;
	public final float surfaceReloadMultiplier;
	public final float surfaceMedicalMultiplier;
	public final float movementNoiseRadius;
	public final float reinforcementIntervalMultiplier;

	ThemeEnvironmentRules(
			String id,
			Surface surface,
			float surfaceMovementMultiplier,
			float surfaceEnemySightMultiplier,
			float surfaceEnemyHearingMultiplier,
			float surfaceReloadMultiplier,
			float surfaceMedicalMultiplier,
			float movementNoiseRadius,
			float reinforcementIntervalMultiplier) {
		this.id = id;
		this.surface = surface;
		this.surfaceMovementMultiplier = surfaceMovementMultiplier;
		this.surfaceEnemySightMultiplier = surfaceEnemySightMultiplier;
		this.surfaceEnemyHearingMultiplier = surfaceEnemyHearingMultiplier;
		this.surfaceReloadMultiplier = surfaceReloadMultiplier;
		this.surfaceMedicalMultiplier = surfaceMedicalMultiplier;
		this.movementNoiseRadius = movementNoiseRadius;
		this.reinforcementIntervalMultiplier =
				reinforcementIntervalMultiplier;
		validate();
	}

	public boolean activeOn(int terrain) {
		return surface.matches(terrain);
	}

	public float movementMultiplier(int terrain) {
		return activeOn(terrain) ? surfaceMovementMultiplier : 1f;
	}

	public float enemySightMultiplier(int targetTerrain) {
		return activeOn(targetTerrain)
				? surfaceEnemySightMultiplier : 1f;
	}

	public float enemyHearingMultiplier(int soundSourceTerrain) {
		return activeOn(soundSourceTerrain)
				? surfaceEnemyHearingMultiplier : 1f;
	}

	public float reloadDurationMultiplier(int terrain) {
		return activeOn(terrain) ? surfaceReloadMultiplier : 1f;
	}

	public float medicalDurationMultiplier(int terrain) {
		return activeOn(terrain) ? surfaceMedicalMultiplier : 1f;
	}

	public float movementNoiseRadius(int terrain) {
		return activeOn(terrain) ? movementNoiseRadius : 0f;
	}

	public String fingerprint() {
		return id + ":" + surface + ":"
				+ surfaceMovementMultiplier + ":"
				+ surfaceEnemySightMultiplier + ":"
				+ surfaceEnemyHearingMultiplier + ":"
				+ surfaceReloadMultiplier + ":"
				+ surfaceMedicalMultiplier + ":"
				+ movementNoiseRadius + ":"
				+ reinforcementIntervalMultiplier;
	}

	private void validate() {
		require(text(id), "environment rule id is required");
		require(surface != null, "environment surface is required: " + id);
		require(range(surfaceMovementMultiplier, 0.65f, 1.05f),
				"movement multiplier out of range: " + id);
		require(range(surfaceEnemySightMultiplier, 0.60f, 1.35f),
				"sight multiplier out of range: " + id);
		require(range(surfaceEnemyHearingMultiplier, 0.75f, 1.50f),
				"hearing multiplier out of range: " + id);
		require(range(surfaceReloadMultiplier, 1f, 1.35f),
				"reload multiplier out of range: " + id);
		require(range(surfaceMedicalMultiplier, 1f, 1.35f),
				"medical multiplier out of range: " + id);
		require(range(movementNoiseRadius, 0f, 7f),
				"movement noise radius out of range: " + id);
		require(range(reinforcementIntervalMultiplier, 0.80f, 1.10f),
				"reinforcement multiplier out of range: " + id);
	}

	private static boolean range(float value, float minimum, float maximum) {
		return BukovNumbers.isFinite(value)
				&& value >= minimum && value <= maximum;
	}

	private static boolean text(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private static void require(boolean condition, String message) {
		if (!condition) throw new IllegalArgumentException(message);
	}
}
