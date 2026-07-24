package com.shatteredpixel.shatteredpixeldungeon.bukov.settings;

import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFeedbackType;

import java.util.EnumMap;
import java.util.Map;

public final class ExperienceContract {

	public static final class FeedbackProfile {
		public float shakeAmplitudePx;
		public int shakeDurationMs;
		public float vibrationAmplitude;
		public int vibrationDurationMs;
		public String frequency;
		public int hitstopMs;
		public float visualIntensity;

		void validate(CombatFeedbackType type, float maximumShakePx) {
			require(com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(shakeAmplitudePx)
							&& shakeAmplitudePx >= 0f
							&& shakeAmplitudePx <= maximumShakePx,
					"invalid shake amplitude: " + type);
			require(shakeDurationMs >= 0 && shakeDurationMs <= 1000,
					"invalid shake duration: " + type);
			require(com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(vibrationAmplitude)
							&& vibrationAmplitude >= 0f
							&& vibrationAmplitude <= 1f,
					"invalid vibration amplitude: " + type);
			require(vibrationDurationMs >= 0 && vibrationDurationMs <= 1000,
					"invalid vibration duration: " + type);
			require("low".equals(frequency)
							|| "medium".equals(frequency)
							|| "high".equals(frequency),
					"invalid haptic frequency: " + type);
			require(hitstopMs >= 0 && hitstopMs <= 120,
					"invalid hitstop: " + type);
			require(com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(visualIntensity)
							&& visualIntensity >= 0f
							&& visualIntensity <= 2f,
					"invalid visual intensity: " + type);
		}
	}

	public float defaultMasterVolume;
	public float defaultMusicVolume;
	public float defaultSfxVolume;
	public float defaultAmbienceVolume;
	public float fullVolumeDistance;
	public float referenceDistance;
	public float referenceDecibels;
	public float wallDecibels;
	public float lowPassDistance;
	public float lowPassHz;
	public float minimumAudibleDecibels;
	public float visualizationNearDistance;
	public float visualizationMidDistance;
	public float maximumShakePx;
	private final Map<CombatFeedbackType, FeedbackProfile> profiles =
			new EnumMap<>(CombatFeedbackType.class);

	public FeedbackProfile profile(CombatFeedbackType type) {
		FeedbackProfile profile = profiles.get(type);
		if (profile == null) {
			throw new IllegalArgumentException(
					"Missing feedback profile: " + type
			);
		}
		return profile;
	}

	void put(CombatFeedbackType type, FeedbackProfile profile) {
		if (profiles.put(type, profile) != null) {
			throw new IllegalArgumentException(
					"Duplicate feedback profile: " + type
			);
		}
	}

	void validate() {
		requireUnit(defaultMasterVolume, "defaultMasterVolume");
		requireUnit(defaultMusicVolume, "defaultMusicVolume");
		requireUnit(defaultSfxVolume, "defaultSfxVolume");
		requireUnit(defaultAmbienceVolume, "defaultAmbienceVolume");
		require(com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(fullVolumeDistance)
						&& fullVolumeDistance > 0f,
				"fullVolumeDistance must be positive");
		require(com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(referenceDistance)
						&& referenceDistance > fullVolumeDistance,
				"referenceDistance must exceed fullVolumeDistance");
		require(com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(referenceDecibels)
						&& referenceDecibels < 0f,
				"referenceDecibels must be negative");
		require(com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(wallDecibels) && wallDecibels < 0f,
				"wallDecibels must be negative");
		require(com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(lowPassDistance)
						&& lowPassDistance >= referenceDistance,
				"lowPassDistance must not precede referenceDistance");
		require(com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(lowPassHz) && lowPassHz > 0f,
				"lowPassHz must be positive");
		require(com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(minimumAudibleDecibels)
						&& minimumAudibleDecibels < referenceDecibels,
				"minimumAudibleDecibels must be below reference");
		require(com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(visualizationNearDistance)
						&& com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(visualizationMidDistance)
						&& visualizationNearDistance > 0f
						&& visualizationMidDistance > visualizationNearDistance,
				"invalid visualization distance bands");
		require(com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(maximumShakePx)
						&& maximumShakePx > 0f
						&& maximumShakePx <= 8f,
				"maximumShakePx must be within Gate 5 ceiling");
		for (CombatFeedbackType type : CombatFeedbackType.values()) {
			profile(type).validate(type, maximumShakePx);
		}
	}

	private static void requireUnit(float value, String label) {
		require(com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(value) && value >= 0f && value <= 1f,
				label + " must be unit range");
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new IllegalArgumentException(message);
		}
	}
}
