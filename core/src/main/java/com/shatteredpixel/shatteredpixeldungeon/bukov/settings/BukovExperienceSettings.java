package com.shatteredpixel.shatteredpixeldungeon.bukov.settings;

import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.AudioChannel;

/**
 * Immutable user-facing presentation settings. Simulation code never receives
 * this object.
 */
public final class BukovExperienceSettings {

	public final float masterVolume;
	public final float musicVolume;
	public final float sfxVolume;
	public final float ambienceVolume;
	public final boolean visualEffects;
	public final float screenShakeScale;
	public final float controllerVibrationScale;
	public final boolean hitstopEnabled;
	public final boolean keySoundVisualization;
	public final boolean reduceMotion;
	public final boolean reduceFlashes;

	public BukovExperienceSettings(float masterVolume,
								   float musicVolume,
								   float sfxVolume,
								   float ambienceVolume,
								   boolean visualEffects,
								   float screenShakeScale,
								   float controllerVibrationScale,
								   boolean hitstopEnabled,
								   boolean keySoundVisualization,
								   boolean reduceMotion,
								   boolean reduceFlashes) {
		requireUnit(masterVolume, "masterVolume");
		requireUnit(musicVolume, "musicVolume");
		requireUnit(sfxVolume, "sfxVolume");
		requireUnit(ambienceVolume, "ambienceVolume");
		requireUnit(screenShakeScale, "screenShakeScale");
		requireUnit(controllerVibrationScale, "controllerVibrationScale");
		this.masterVolume = masterVolume;
		this.musicVolume = musicVolume;
		this.sfxVolume = sfxVolume;
		this.ambienceVolume = ambienceVolume;
		this.visualEffects = visualEffects;
		this.screenShakeScale = screenShakeScale;
		this.controllerVibrationScale = controllerVibrationScale;
		this.hitstopEnabled = hitstopEnabled;
		this.keySoundVisualization = keySoundVisualization;
		this.reduceMotion = reduceMotion;
		this.reduceFlashes = reduceFlashes;
	}

	public static BukovExperienceSettings defaults(
			ExperienceContract contract) {
		if (contract == null) {
			throw new IllegalArgumentException("contract is required");
		}
		return new BukovExperienceSettings(
				contract.defaultMasterVolume,
				contract.defaultMusicVolume,
				contract.defaultSfxVolume,
				contract.defaultAmbienceVolume,
				true,
				1f,
				1f,
				true,
				false,
				false,
				false
		);
	}

	public static BukovExperienceSettings allPresentationOff(
			ExperienceContract contract) {
		if (contract == null) {
			throw new IllegalArgumentException("contract is required");
		}
		return new BukovExperienceSettings(
				0f, 0f, 0f, 0f,
				false, 0f, 0f,
				false, false, true, true
		);
	}

	public float channelGain(AudioChannel channel) {
		if (channel == null) {
			throw new IllegalArgumentException("channel is required");
		}
		switch (channel) {
			case MASTER:
				return masterVolume;
			case MUSIC:
				return masterVolume * musicVolume;
			case SFX:
				return masterVolume * sfxVolume;
			case AMBIENCE:
				return masterVolume * ambienceVolume;
			default:
				throw new IllegalStateException("Unknown channel: " + channel);
		}
	}

	private static void requireUnit(float value, String label) {
		if (value < 0f || value > 1f
				|| !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(value)) {
			throw new IllegalArgumentException(
					label + " must be between zero and one"
			);
		}
	}
}
