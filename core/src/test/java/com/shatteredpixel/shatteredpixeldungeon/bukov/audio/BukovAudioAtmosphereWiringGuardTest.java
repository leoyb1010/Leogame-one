package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovAudioAtmosphereWiringGuardTest {

	@Test
	public void dedicatedUiAndAtmosphereAssetsAreWiredWithoutGameplayRng()
			throws Exception {
		String assets = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/Assets.java");
		String scene = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java");
		String world = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java");
		String ui = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/BukovUiSoundPlayer.java");
		String atmosphere = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/BukovAtmosphereController.java");

		for (String cue : new String[]{
				"UI_FOCUS", "UI_CONFIRM", "UI_CANCEL", "UI_ERROR",
				"AMBIENCE_CALM", "AMBIENCE_TENSE", "AMBIENCE_COMBAT"}) {
			assertTrue(assets.contains(cue));
		}
		assertTrue(scene.contains("Music.INSTANCE.end()"));
		assertTrue(scene.contains("initializeBukovAudio()"));
		assertTrue(scene.contains("updateBukovAudio(Game.elapsed)"));
		assertTrue(scene.contains("disposeBukovAudio()"));
		assertTrue(scene.contains("playBukovUiCue("));
		assertTrue(world.contains("readAtmosphereSignal("));
		assertTrue(ui.contains("FOCUS_DEBOUNCE_SECONDS = 0.03f"));
		assertTrue(atmosphere.contains("CROSSFADE_SECONDS = 1.5f"));
		assertTrue(atmosphere.contains("COMBAT_RELEASE_SECONDS = 8f"));
		assertFalse(ui.contains("Random."));
		assertFalse(atmosphere.contains("Random."));
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
