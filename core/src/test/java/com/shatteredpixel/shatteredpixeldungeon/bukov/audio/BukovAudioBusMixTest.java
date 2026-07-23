package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BukovAudioBusMixTest {

	@Test
	public void allFourChannelsRespectMasterAndCombatOnlyDucksMusic() {
		BukovAudioBusMix mix = new BukovAudioBusMix();
		mix.set(0.8f, 0.7f, 0.9f, 0.7f);

		assertEquals(0.8f, mix.gain(AudioChannel.MASTER, 0f), 0f);
		assertEquals(0.56f, mix.gain(AudioChannel.MUSIC, 0f), 0.0001f);
		assertEquals(0.72f, mix.gain(AudioChannel.SFX, 0f), 0.0001f);
		assertEquals(0.56f, mix.gain(AudioChannel.AMBIENCE, 0f), 0.0001f);
		assertTrue(
				mix.gain(AudioChannel.MUSIC, 1f)
						< mix.gain(AudioChannel.MUSIC, 0f));
		assertEquals(
				mix.gain(AudioChannel.SFX, 0f),
				mix.gain(AudioChannel.SFX, 1f),
				0f);
	}
}
