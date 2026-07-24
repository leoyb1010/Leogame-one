package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.BukovExperienceSettings;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContractRegistry;

/**
 * One entry point for Bukov UI cues across scenes and windows.
 *
 * <p>The raid scene may supply its already-mixed SFX gain. Hideout scenes and
 * windows use the same experience-contract defaults and Bukov volume sliders.
 * The shared player keeps focus movement debounced across window boundaries.</p>
 */
public final class BukovUiSoundRouter {

	private static final BukovUiSoundPlayer PLAYER =
			new BukovUiSoundPlayer();

	private static BukovExperienceSettings defaults;

	private BukovUiSoundRouter() {
	}

	public static void update(float deltaSeconds) {
		PLAYER.update(deltaSeconds);
	}

	public static boolean play(BukovUiSoundPlayer.Cue cue) {
		return play(cue, configuredSfxGain());
	}

	public static boolean play(
			BukovUiSoundPlayer.Cue cue,
			float mixedSfxGain) {
		if (!SPDSettings.soundFx()) {
			return false;
		}
		return PLAYER.play(cue, mixedSfxGain);
	}

	private static float configuredSfxGain() {
		BukovExperienceSettings experience = defaults();
		return experience.masterVolume
				* SPDSettings.bukovVolumeGain(
						SPDSettings.bukovMasterVolume())
				* experience.sfxVolume
				* SPDSettings.bukovVolumeGain(
						SPDSettings.bukovSfxVolume());
	}

	private static BukovExperienceSettings defaults() {
		if (defaults == null) {
			defaults = BukovExperienceSettings.defaults(
					new ExperienceContractRegistry().loadDefault());
		}
		return defaults;
	}
}
