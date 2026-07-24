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
		String uiRouter = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/BukovUiSoundRouter.java");
		String hub = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/BukovHubScene.java");
		String title = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/TitleScene.java");
		String welcome = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/WelcomeScene.java");
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
		assertTrue(scene.contains("BukovUiSoundRouter.play("));
		assertTrue(scene.contains("BukovUiSoundRouter.update(deltaSeconds)"));
		assertTrue(world.contains("readAtmosphereSignal("));
		assertTrue(ui.contains("FOCUS_DEBOUNCE_SECONDS = 0.03f"));
		assertTrue(uiRouter.contains("SPDSettings.bukovMasterVolume()"));
		assertTrue(uiRouter.contains("SPDSettings.bukovSfxVolume()"));
		assertTrue(uiRouter.contains("SPDSettings.soundFx()"));
		assertTrue(hub.contains("BukovUiSoundRouter.update(Game.elapsed)"));
		assertTrue(hub.contains("BukovUiSoundPlayer.Cue.CONFIRM"));
		assertTrue(hub.contains("BukovUiSoundPlayer.Cue.CANCEL"));
		assertTrue(hub.contains("BukovUiSoundPlayer.Cue.ERROR"));
		assertFalse(hub.contains("Assets.Sounds.CLICK"));
		assertFalse(title.contains("Assets.Sounds.Bukov.UI_CONFIRM"));
		assertFalse(welcome.contains("Assets.Sounds.Bukov.UI_CONFIRM"));
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
