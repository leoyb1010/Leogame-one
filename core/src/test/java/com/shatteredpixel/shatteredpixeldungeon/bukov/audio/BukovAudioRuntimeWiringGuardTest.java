package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovAudioRuntimeWiringGuardTest {

	@Test
	public void worldSharesSpatialContractAndDoesNotSpendGameplayRngOnAudio()
			throws Exception {
		String world = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java");

		assertTrue(world.contains("KeySoundVisualizationSource"));
		assertTrue(world.contains("readKeySoundVisualEvent("));
		assertTrue(world.contains("SpatialAudioModel.resolve("));
		assertTrue(world.contains("aiSoundSpatial.perceivable()"));
		assertTrue(world.contains("GunshotAudioResolver.resolve("));
		assertTrue(world.contains("playGunshotLayers("));
		assertEquals(
				"Dry-fire asset must only be used for an empty chamber",
				1,
				occurrences(world, "Assets.Sounds.Bukov.DRY_FIRE"));
		assertTrue(world.contains(
				"definition.audioProfile.gunshotFamily.mechanicalAsset(sequence)"));
		assertTrue(world.contains(
				"definition.audioProfile.gunshotFamily.bodyAsset(sequence)"));
		assertTrue(world.contains("acousticSpace.tailAsset(sequence)"));
		assertTrue(world.contains("GunshotAcousticSpaceResolver.resolve("));
		assertFalse(world.contains("Assets.Sounds.Bukov.GUNSHOT_PLAYER"));
		assertFalse(world.contains("Assets.Sounds.Bukov.GUNSHOT_ENEMY"));
		assertTrue(world.contains("void reloadAudioCues("));
		assertTrue(world.contains("ReloadAudioCue.values()"));
		assertTrue(world.contains("cue.asset()"));
		assertEquals(
				"Only the enemy compatibility path may use the legacy reload cue",
				1,
				occurrences(world, "Assets.Sounds.Bukov.RELOAD_START"));
		assertFalse(world.contains("Assets.Sounds.Bukov.RELOAD_FINISH"));
		assertTrue(world.contains("KEY_SOUND_LIFETIME_SECONDS = 0.9f"));
		assertTrue(world.contains("SPDSettings.bukovMasterVolume()"));
		assertTrue(world.contains("SPDSettings.bukovSfxVolume()"));
		assertTrue(world.contains("private float realtimeSfxGain()"));
		assertEquals(
				"Runtime SFX must only reach the bounded audio wrapper",
				0,
				occurrences(world, "Sample.INSTANCE.play("));
		assertTrue(world.contains("BukovConcurrentSoundPlayer worldSounds"));
		assertTrue(world.contains("worldSounds.update(dt)"));
		assertTrue(world.contains("worldSounds.stopAll()"));

		// The sole global RNG call is the committed ballistic spread. Audio
		// variation is derived from its own sequence.
		assertEquals(1, occurrences(world, "Random.Float("));
		assertTrue(world.contains("Random.Float(-spread, spread)"));

		// Critical interaction cues remain on the original Bukov asset bank.
		assertTrue(world.contains("Assets.Sounds.Bukov.SEARCH_COMPLETE"));
		assertTrue(world.contains("Assets.Sounds.Bukov.GATE_UNLOCK"));
		assertTrue(world.contains("Assets.Sounds.Bukov.EXTRACTION_START"));
		assertTrue(world.contains("Assets.Sounds.Bukov.EXTRACTION_COMPLETE"));
		assertTrue(world.contains("Assets.Sounds.Bukov.LOOT_PICKUP"));

		String presentation = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/fx/BukovCombatPresentation.java");
		assertTrue(presentation.contains(
				"CombatFeedbackResolver.add("));
		assertTrue(!presentation.contains(
				"new CombatFeedbackRequest("));
		assertTrue(!presentation.contains(
				"new BukovExperienceSettings("));
	}

	private static int occurrences(String source, String token) {
		int count = 0;
		int offset = 0;
		while ((offset = source.indexOf(token, offset)) >= 0) {
			count++;
			offset += token.length();
		}
		return count;
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
