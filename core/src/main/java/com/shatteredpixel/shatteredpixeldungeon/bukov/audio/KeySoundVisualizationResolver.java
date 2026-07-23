package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.BukovExperienceSettings;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContract;

/**
 * Emits only eight-way direction and near/mid/far distance, never exact source
 * position or actor identity.
 */
public final class KeySoundVisualizationResolver {

	public static void resolve(SoundCategory category,
							   float deltaX,
							   float deltaY,
							   SpatialAudioModel.Result spatial,
							   ExperienceContract contract,
							   BukovExperienceSettings settings,
							   KeySoundVisualEvent out) {
		if (category == null
				|| spatial == null
				|| contract == null
				|| settings == null
				|| out == null) {
			throw new IllegalArgumentException("all arguments are required");
		}
		resolve(
				category,
				deltaX,
				deltaY,
				spatial,
				contract,
				settings.keySoundVisualization,
				out);
	}

	public static void resolve(SoundCategory category,
							   float deltaX,
							   float deltaY,
							   SpatialAudioModel.Result spatial,
							   ExperienceContract contract,
							   boolean visualizationEnabled,
							   KeySoundVisualEvent out) {
		if (category == null || spatial == null
				|| contract == null || out == null) {
			throw new IllegalArgumentException("all arguments are required");
		}
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(deltaX)
				|| !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
						deltaY)) {
			throw new IllegalArgumentException("sound delta must be finite");
		}
		if (!visualizationEnabled
				|| !category.visualizable()
				|| !spatial.perceivable()) {
			out.set(false, category, null, null, 0f);
			return;
		}
		float distance = (float)Math.sqrt(deltaX * deltaX + deltaY * deltaY);
		KeySoundVisualEvent.DistanceBand band =
				distance <= contract.visualizationNearDistance
						? KeySoundVisualEvent.DistanceBand.NEAR
						: distance <= contract.visualizationMidDistance
						? KeySoundVisualEvent.DistanceBand.MID
						: KeySoundVisualEvent.DistanceBand.FAR;
		out.set(
				true,
				category,
				direction(deltaX, deltaY),
				band,
				spatial.perceptionGain()
		);
	}

	private static KeySoundVisualEvent.Direction direction(float x, float y) {
		double angle = Math.atan2(y, x);
		int octant = com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.floorMod(
				(int)Math.round(angle / (Math.PI / 4d)),
				8
		);
		switch (octant) {
			case 0: return KeySoundVisualEvent.Direction.E;
			case 1: return KeySoundVisualEvent.Direction.SE;
			case 2: return KeySoundVisualEvent.Direction.S;
			case 3: return KeySoundVisualEvent.Direction.SW;
			case 4: return KeySoundVisualEvent.Direction.W;
			case 5: return KeySoundVisualEvent.Direction.NW;
			case 6: return KeySoundVisualEvent.Direction.N;
			default: return KeySoundVisualEvent.Direction.NE;
		}
	}

	private KeySoundVisualizationResolver() {
	}
}
