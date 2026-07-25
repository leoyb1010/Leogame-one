package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.watabou.noosa.audio.Sample;

/** Production bridge from the Bukov voice budget to the host sound backend. */
final class BukovSamplePlaybackSink implements BukovSoundPlaybackSink {

	@Override
	public long play(
			String asset,
			float leftVolume,
			float rightVolume,
			float pitch) {
		return Sample.INSTANCE.play(asset, leftVolume, rightVolume, pitch);
	}

	@Override
	public void stop(String asset, long playbackId) {
		Sample.INSTANCE.stop(asset, playbackId);
	}
}
