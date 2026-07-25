package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.ThemeEnvironmentRules;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;

/**
 * Player-footstep material selected from real Bukov terrain and theme rules.
 */
public enum FootstepSurface {
	HARD(Assets.Sounds.Bukov.FOOTSTEP_HARD, 0.23f, 0.97f),
	WATER(Assets.Sounds.Bukov.FOOTSTEP_WATER, 0.27f, 0.95f),
	METAL(Assets.Sounds.Bukov.FOOTSTEP_METAL, 0.21f, 1.01f);

	private final String[] assets;
	private final float gain;
	private final float basePitch;

	FootstepSurface(String[] assets, float gain, float basePitch) {
		this.assets = assets;
		this.gain = gain;
		this.basePitch = basePitch;
	}

	public String asset(int sequence) {
		return assets[BukovNumbers.floorMod(sequence, assets.length)];
	}

	public float gain() {
		return gain;
	}

	public float pitch(int sequence) {
		return basePitch
				+ (BukovNumbers.floorMod(sequence, assets.length) == 0
						? 0f : 0.045f);
	}

	public static FootstepSurface resolve(
			int terrain,
			ThemeEnvironmentRules environmentRules) {
		if (terrain == Terrain.WATER) {
			return WATER;
		}
		if (terrain == Terrain.EMBERS
				|| terrain == Terrain.EMPTY_SP
				|| terrain == Terrain.OPEN_DOOR) {
			return METAL;
		}
		if (environmentRules != null
				&& environmentRules.activeOn(terrain)) {
			return forThemeSurface(environmentRules.surface);
		}
		return HARD;
	}

	static FootstepSurface forThemeSurface(
			ThemeEnvironmentRules.Surface surface) {
		if (surface == ThemeEnvironmentRules.Surface.WATER) {
			return WATER;
		}
		if (surface == ThemeEnvironmentRules.Surface.EMBERS
				|| surface == ThemeEnvironmentRules.Surface.EMPTY_SP) {
			return METAL;
		}
		return HARD;
	}
}
