package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.watabou.noosa.Game;

/**
 * Keeps all three short ambience loops phase-running and crossfades only their
 * gains. Startup allocates the audio handles; frame updates do not allocate.
 */
public final class BukovAtmospherePlayer {

	private final Sound[] tracks = new Sound[3];
	private final long[] ids = {-1L, -1L, -1L};
	private boolean started;

	public void start() {
		if (started) return;
		String[] assets = {
				Assets.Sounds.Bukov.AMBIENCE_CALM,
				Assets.Sounds.Bukov.AMBIENCE_TENSE,
				Assets.Sounds.Bukov.AMBIENCE_COMBAT
		};
		try {
			for (int index = 0; index < tracks.length; index++) {
				tracks[index] = Gdx.audio.newSound(
						Gdx.files.internal(assets[index]));
				ids[index] = tracks[index].loop(0f);
			}
			started = true;
		} catch (RuntimeException failure) {
			Game.reportException(failure);
			dispose();
		}
	}

	public void update(
			BukovAtmosphereController controller,
			float ambienceGain) {
		if (controller == null) {
			throw new IllegalArgumentException("controller is required");
		}
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
						ambienceGain)
				|| ambienceGain < 0f || ambienceGain > 1f) {
			throw new IllegalArgumentException("invalid ambienceGain");
		}
		if (!started) return;
		setVolume(
				0,
				ambienceGain
						* controller.gain(
								BukovAtmosphereController.State.CALM));
		setVolume(
				1,
				ambienceGain
						* controller.gain(
								BukovAtmosphereController.State.TENSE));
		setVolume(
				2,
				ambienceGain
						* controller.gain(
								BukovAtmosphereController.State.COMBAT));
	}

	public void dispose() {
		for (int index = 0; index < tracks.length; index++) {
			if (tracks[index] == null) continue;
			if (ids[index] >= 0L) tracks[index].stop(ids[index]);
			tracks[index].dispose();
			tracks[index] = null;
			ids[index] = -1L;
		}
		started = false;
	}

	private void setVolume(int index, float volume) {
		if (tracks[index] != null && ids[index] >= 0L) {
			tracks[index].setVolume(ids[index], volume);
		}
	}
}
