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
		assertTrue(player.contains(
				"BukovConcurrentSoundPlayer.production("));
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

	@Test
	public void realtimeWorldRoutesAllPlaybackThroughSixVoiceBudget()
			throws Exception {
		String world = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java");

		assertTrue(world.contains(
				"BukovConcurrentSoundPlayer worldSounds"));
		assertTrue(world.contains(
				"BukovConcurrentSoundPlayer.production("));
			assertTrue(world.contains("worldSounds.update(dt)"));
			assertTrue(world.contains("worldSounds.stopAll()"));
			assertTrue(world.contains(
					"input.cancelTouches();\n"
							+ "\t\t\tpreserveExtractionCompleteCue();\n"
							+ "\t\t\tworldSounds.stopAll();"));
			assertTrue(world.contains(
					"worldSounds.detach(extractionCompleteSoundToken)"));
		assertTrue(world.contains("worldSounds.begin("));
		assertTrue(world.contains("worldSounds.playLayer("));
		assertTrue(world.contains("worldSounds.play("));
		assertTrue(world.contains("SoundCategory.PLAYER_GUNSHOT"));
		assertTrue(world.contains("SoundCategory.ENEMY_GUNSHOT"));
		assertTrue(world.contains("SoundCategory.EXTRACTION_CUE"));
		assertTrue(world.contains("SoundCategory.FOOTSTEP"));
		assertTrue(world.contains(
				"enemyDefinition.audioProfile.gunshotFamily"));
		assertTrue(world.contains(
				"GunshotAcousticSpaceResolver.resolve("));
		assertTrue(world.contains(
				"gunshotAudio.bodyLeft() * gainScale * gain"));
		assertTrue(world.contains("if (gain <= 0f) return;"));
		assertTrue(world.contains("emitPlayerSound("));
		assertTrue(world.contains("aiSoundSpatial.perceivable()"));
		assertTrue(world.contains(
				"KeySoundVisualizationResolver.resolve("));
		assertFalse(world.contains("Assets.Sounds.Bukov.GUNSHOT_ENEMY"));
		assertFalse(world.contains("Sample.INSTANCE.play("));
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
