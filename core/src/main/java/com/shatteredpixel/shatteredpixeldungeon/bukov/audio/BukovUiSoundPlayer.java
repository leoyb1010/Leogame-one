package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;

/**
 * Dedicated Bukov UI sound bank. Focus movement is debounced at 30ms and no
 * cue uses gameplay RNG.
 */
public final class BukovUiSoundPlayer {

	public enum Cue {
		FOCUS,
		CONFIRM,
		CANCEL,
		ERROR
	}

	private static final float UI_MINUS_SIX_DB = 0.5011872f;
	private static final float FOCUS_DEBOUNCE_SECONDS = 0.03f;
	private float focusCooldown;

	public void update(float deltaSeconds) {
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
						deltaSeconds)
				|| deltaSeconds <= 0f) {
			return;
		}
		focusCooldown = Math.max(0f, focusCooldown - deltaSeconds);
	}

	public boolean play(Cue cue, float sfxGain) {
		if (cue == null) {
			throw new IllegalArgumentException("cue is required");
		}
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
						sfxGain)
				|| sfxGain < 0f || sfxGain > 1f) {
			throw new IllegalArgumentException("invalid sfxGain");
		}
		if (cue == Cue.FOCUS && focusCooldown > 0f) {
			return false;
		}
		if (cue == Cue.FOCUS) {
			focusCooldown = FOCUS_DEBOUNCE_SECONDS;
		}
		if (sfxGain <= 0f) return false;
		Sample.INSTANCE.play(asset(cue), sfxGain * UI_MINUS_SIX_DB, 1f);
		return true;
	}

	private static String asset(Cue cue) {
		switch (cue) {
			case FOCUS:
				return Assets.Sounds.Bukov.UI_FOCUS;
			case CONFIRM:
				return Assets.Sounds.Bukov.UI_CONFIRM;
			case CANCEL:
				return Assets.Sounds.Bukov.UI_CANCEL;
			case ERROR:
				return Assets.Sounds.Bukov.UI_ERROR;
			default:
				throw new IllegalStateException("unknown cue: " + cue);
		}
	}
}
