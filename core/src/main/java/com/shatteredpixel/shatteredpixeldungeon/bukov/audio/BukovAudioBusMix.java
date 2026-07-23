package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

/**
 * Allocation-free four-channel mix. The host sliders may feed these values,
 * while Bukov keeps channel priority and combat music ducking explicit.
 */
public final class BukovAudioBusMix {

	private static final float COMBAT_MUSIC_DUCK_GAIN =
			(float)Math.pow(10d, -3d / 20d);

	private float master;
	private float music;
	private float sfx;
	private float ambience;

	public void set(
			float master,
			float music,
			float sfx,
			float ambience) {
		requireUnit(master, "master");
		requireUnit(music, "music");
		requireUnit(sfx, "sfx");
		requireUnit(ambience, "ambience");
		this.master = master;
		this.music = music;
		this.sfx = sfx;
		this.ambience = ambience;
	}

	public float gain(AudioChannel channel, float combatBlend) {
		if (channel == null) {
			throw new IllegalArgumentException("channel is required");
		}
		requireUnit(combatBlend, "combatBlend");
		switch (channel) {
			case MASTER:
				return master;
			case MUSIC:
				return master * music * (
						1f - combatBlend
								* (1f - COMBAT_MUSIC_DUCK_GAIN));
			case SFX:
				return master * sfx;
			case AMBIENCE:
				return master * ambience;
			default:
				throw new IllegalStateException("unknown channel: " + channel);
		}
	}

	private static void requireUnit(float value, String label) {
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
						value)
				|| value < 0f || value > 1f) {
			throw new IllegalArgumentException(
					label + " must be in unit range");
		}
	}
}
