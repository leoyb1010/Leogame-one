package com.shatteredpixel.shatteredpixeldungeon.ios;

import com.badlogic.gdx.audio.Music;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SilentIOSAudioTest {

	@Test
	public void musicAcceptsNormalGameOperationsWithoutNativeAudio() {
		Music music = new SilentIOSAudio().newMusic(null);
		music.setLooping(true);
		music.setVolume(0.4f);
		music.play();

		assertFalse(music.isPlaying());
		assertEquals(0.4f, music.getVolume(), 0.0001f);
	}
}
