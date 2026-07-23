package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.BukovExperienceSettings;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContract;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContractTestFixture;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KeySoundVisualizationResolverTest {

	@Test
	public void enabledFootstepProducesOnlyDirectionAndDistanceBand() {
		ExperienceContract contract = ExperienceContractTestFixture.load();
		BukovExperienceSettings settings =
				ExperienceContractTestFixture.visualizationEnabled(contract);
		SpatialAudioModel.Result spatial = new SpatialAudioModel.Result();
		SpatialAudioModel.resolve(
				contract, settings, AudioChannel.SFX,
				6f, 0f, false, spatial
		);
		KeySoundVisualEvent event = new KeySoundVisualEvent();

		KeySoundVisualizationResolver.resolve(
				SoundCategory.FOOTSTEP,
				-4f,
				-4f,
				spatial,
				contract,
				settings,
				event
		);

		assertTrue(event.visible());
		assertEquals(KeySoundVisualEvent.Direction.NW, event.direction());
		assertEquals(KeySoundVisualEvent.DistanceBand.MID, event.distanceBand());
	}

	@Test
	public void eventLifetimeAndCopyExposeStateWithoutSourceCoordinates() {
		ExperienceContract contract = ExperienceContractTestFixture.load();
		SpatialAudioModel.Result spatial = new SpatialAudioModel.Result();
		SpatialAudioModel.resolve(
				contract, 1f, 6f, 0f, false, spatial);
		KeySoundVisualEvent event = new KeySoundVisualEvent();
		KeySoundVisualEvent copy = new KeySoundVisualEvent();

		KeySoundVisualizationResolver.resolve(
				SoundCategory.ENEMY_GUNSHOT,
				4f, 4f, spatial, contract, true, event);
		event.activate(17, 0.9f);
		event.advance(0.4f);
		event.copyTo(copy);

		assertTrue(copy.visible());
		assertEquals(17, copy.sequence());
		assertEquals(0.5f, copy.remainingSeconds(), 0.0001f);
		assertEquals(KeySoundVisualEvent.Direction.SE, copy.direction());

		event.advance(0.5f);
		assertFalse(event.visible());
	}

	@Test
	public void settingOffAndUiSoundsNeverProduceSoundRingEvents() {
		ExperienceContract contract = ExperienceContractTestFixture.load();
		BukovExperienceSettings defaults =
				BukovExperienceSettings.defaults(contract);
		SpatialAudioModel.Result spatial = new SpatialAudioModel.Result();
		SpatialAudioModel.resolve(
				contract, defaults, AudioChannel.SFX,
				2f, 0f, false, spatial
		);
		KeySoundVisualEvent event = new KeySoundVisualEvent();

		KeySoundVisualizationResolver.resolve(
				SoundCategory.FOOTSTEP,
				1f, 0f, spatial, contract, defaults, event
		);
		assertFalse(event.visible());

		KeySoundVisualizationResolver.resolve(
				SoundCategory.UI,
				1f, 0f, spatial, contract,
				ExperienceContractTestFixture.visualizationEnabled(contract),
				event
		);
		assertFalse(event.visible());
	}
}
