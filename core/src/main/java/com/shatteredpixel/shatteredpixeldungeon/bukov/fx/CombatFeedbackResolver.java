package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.AudioChannel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.BukovExperienceSettings;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContract;

/**
 * Converts committed combat outcomes to presentation envelopes without access
 * to mutable gameplay state, RNG, actors, damage, or input.
 */
public final class CombatFeedbackResolver {

	public static void add(CombatFeedbackRequest request,
						   ExperienceContract contract,
						   BukovExperienceSettings settings,
						   CombatFeedbackPlan out) {
		if (request == null || contract == null || settings == null || out == null) {
			throw new IllegalArgumentException("all arguments are required");
		}
		add(
				request.type,
				request.distanceTiles,
				request.intensityScale,
				contract,
				settings.visualEffects,
				settings.channelGain(AudioChannel.SFX),
				settings.screenShakeScale,
				settings.controllerVibrationScale,
				settings.hitstopEnabled,
				settings.reduceMotion,
				settings.reduceFlashes,
				out);
	}

	/**
	 * Allocation-free presentation adapter for pooled combat events.
	 */
	public static void add(CombatFeedbackType type,
						   float distanceTiles,
						   float intensityScale,
						   ExperienceContract contract,
						   boolean visualEffects,
						   float sfxGain,
						   float screenShakeScale,
						   float controllerVibrationScale,
						   boolean hitstopEnabled,
						   boolean reduceMotion,
						   boolean reduceFlashes,
						   CombatFeedbackPlan out) {
		if (type == null || contract == null || out == null) {
			throw new IllegalArgumentException("type, contract, and out are required");
		}
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
						distanceTiles)
				|| distanceTiles < 0f
				|| !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
						intensityScale)
				|| intensityScale < 0f
				|| !unit(sfxGain)
				|| !unit(screenShakeScale)
				|| !unit(controllerVibrationScale)) {
			throw new IllegalArgumentException("invalid feedback controls");
		}
		ExperienceContract.FeedbackProfile profile =
				contract.profile(type);
		float distanceScale = distanceScale(type, distanceTiles);
		float intensity = intensityScale * distanceScale;
		float motionScale = reduceMotion ? 0.5f : 1f;
		float flashScale = reduceFlashes ? 0.5f : 1f;
		float shake = Math.min(
				contract.maximumShakePx,
				profile.shakeAmplitudePx
						* screenShakeScale
						* motionScale
						* intensity
		);
		float vibration = Math.min(
				1f,
				profile.vibrationAmplitude
						* controllerVibrationScale
						* intensity
		);
		out.merge(
				visualEffects && intensity > 0f,
				sfxGain > 0f && intensity > 0f,
				visualEffects
						? profile.visualIntensity * intensity * flashScale
						: 0f,
				shake,
				shake > 0f ? profile.shakeDurationMs : 0,
				vibration,
				vibration > 0f ? profile.vibrationDurationMs : 0,
				hitstopEnabled ? profile.hitstopMs : 0
		);
	}

	private static float distanceScale(
			CombatFeedbackType type,
			float distanceTiles) {
		if (type != CombatFeedbackType.EXPLOSION) {
			return 1f;
		}
		return Math.max(0f, 1f - distanceTiles / 15f);
	}

	private static boolean unit(float value) {
		return com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
						value)
				&& value >= 0f
				&& value <= 1f;
	}

	private CombatFeedbackResolver() {
	}
}
