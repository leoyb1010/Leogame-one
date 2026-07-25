package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovSoundConcurrencyProductionWiringTest {

	@Test
	public void sharedUiEntryUsesBudgetWithoutBypassingMuteOrGain()
			throws Exception {
		String player = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/BukovUiSoundPlayer.java");
		String router = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/BukovUiSoundRouter.java");
		String scene = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java");
		String sample = source(
				"../SPD-classes/src/main/java/com/watabou/noosa/audio/Sample.java");

		assertTrue(player.contains("BukovConcurrentSoundPlayer sounds"));
		assertTrue(player.contains("sounds.update(deltaSeconds)"));
		assertTrue(player.contains("AudioChannel.SFX"));
		assertTrue(player.contains("cue != Cue.FOCUS"));
		assertFalse(player.contains("Sample.INSTANCE.play("));
		assertTrue(router.contains("SPDSettings.soundFx()"));
		assertTrue(router.contains("return PLAYER.play(cue, mixedSfxGain)"));
		assertTrue(scene.contains("BukovUiSoundRouter.update(deltaSeconds)"));
		assertTrue(scene.contains("BukovUiSoundRouter.play("));
		assertTrue(sample.contains(
				"public synchronized void stop( Object id, long playbackId )"));
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
