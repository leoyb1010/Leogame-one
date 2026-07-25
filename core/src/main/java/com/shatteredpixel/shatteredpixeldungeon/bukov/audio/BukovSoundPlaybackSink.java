package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

/** Backend seam that keeps concurrency policy testable without an audio device. */
public interface BukovSoundPlaybackSink {

	long play(
			String asset,
			float leftVolume,
			float rightVolume,
			float pitch);

	void stop(String asset, long playbackId);
}
