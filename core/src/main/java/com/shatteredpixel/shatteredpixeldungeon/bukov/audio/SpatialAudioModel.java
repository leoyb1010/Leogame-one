package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.BukovExperienceSettings;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContract;

/**
 * Shared distance/wall attenuation contract for player audio and AI hearing.
 */
public final class SpatialAudioModel {

	public static final class Result {
		private float spatialDecibels;
		private float spatialGain;
		private float outputGain;
		private float lowPassHz;
		private boolean perceivable;
		private boolean audible;

		public float spatialDecibels() {
			return spatialDecibels;
		}

		public float perceptionGain() {
			return spatialGain;
		}

		public float outputGain() {
			return outputGain;
		}

		public float lowPassHz() {
			return lowPassHz;
		}

		/**
		 * Simulation-facing hearing result. User volume never changes this.
		 */
		public boolean perceivable() {
			return perceivable;
		}

		public boolean audible() {
			return audible;
		}
	}

	public static void resolve(ExperienceContract contract,
							   BukovExperienceSettings settings,
							   AudioChannel channel,
							   float distanceTiles,
							   float wallOcclusion,
							   boolean localPlayerSound,
							   Result out) {
		if (contract == null || settings == null || channel == null || out == null) {
			throw new IllegalArgumentException(
					"contract, settings, channel, and out are required"
			);
		}
		if (distanceTiles < 0f || !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(distanceTiles)) {
			throw new IllegalArgumentException("invalid distanceTiles");
		}
		if (wallOcclusion < 0f || !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(wallOcclusion)) {
			throw new IllegalArgumentException("invalid wallOcclusion");
		}
		resolve(
				contract,
				settings.channelGain(channel),
				distanceTiles,
				wallOcclusion,
				localPlayerSound,
				out);
	}

	/**
	 * Allocation-free overload for runtime playback and AI perception. The
	 * spatial result is shared; only outputGain receives the user-facing mix.
	 */
	public static void resolve(ExperienceContract contract,
							   float channelGain,
							   float distanceTiles,
							   float wallOcclusion,
							   boolean localPlayerSound,
							   Result out) {
		if (contract == null || out == null) {
			throw new IllegalArgumentException("contract and out are required");
		}
		if (channelGain < 0f || channelGain > 1f
				|| !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
						channelGain)) {
			throw new IllegalArgumentException("invalid channelGain");
		}
		if (distanceTiles < 0f || !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(distanceTiles)) {
			throw new IllegalArgumentException("invalid distanceTiles");
		}
		if (wallOcclusion < 0f || !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(wallOcclusion)) {
			throw new IllegalArgumentException("invalid wallOcclusion");
		}

		float distanceDb = 0f;
		float wallDb = 0f;
		boolean filtered = false;
		if (!localPlayerSound) {
			if (distanceTiles > contract.fullVolumeDistance) {
				distanceDb = contract.referenceDecibels
						* (float)(
								Math.log(
										distanceTiles
												/ contract.fullVolumeDistance
								)
										/ Math.log(
												contract.referenceDistance
														/ contract.fullVolumeDistance
										)
						);
			}
			wallDb = wallOcclusion * contract.wallDecibels;
			filtered = distanceTiles >= contract.lowPassDistance
					|| wallOcclusion > 0f;
		}
		out.spatialDecibels = Math.max(
				contract.minimumAudibleDecibels,
				distanceDb + wallDb
		);
		out.spatialGain = decibelsToGain(out.spatialDecibels);
		out.outputGain = out.spatialGain * channelGain;
		out.lowPassHz = filtered
				? Math.max(
						500f,
						contract.lowPassHz / Math.max(1f, wallOcclusion)
				)
				: 20_000f;
		out.perceivable =
				out.spatialDecibels > contract.minimumAudibleDecibels;
		out.audible = out.perceivable && out.outputGain > 0f;
	}

	static float decibelsToGain(float decibels) {
		return (float)Math.pow(10d, decibels / 20d);
	}

	private SpatialAudioModel() {
	}
}
