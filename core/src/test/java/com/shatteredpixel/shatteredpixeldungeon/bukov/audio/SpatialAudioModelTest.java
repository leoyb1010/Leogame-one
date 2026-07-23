package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.BukovExperienceSettings;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContract;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContractTestFixture;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpatialAudioModelTest {

	@Test
	public void authoredCurveIsFullAtOneMinusTwelveAtEightAndMinusSixPerWall() {
		ExperienceContract contract = ExperienceContractTestFixture.load();
		BukovExperienceSettings settings =
				BukovExperienceSettings.defaults(contract);
		SpatialAudioModel.Result result = new SpatialAudioModel.Result();

		SpatialAudioModel.resolve(
				contract, settings, AudioChannel.SFX,
				1f, 0f, false, result
		);
		assertEquals(0f, result.spatialDecibels(), 0.0001f);
		assertEquals(0.72f, result.outputGain(), 0.0001f);

		SpatialAudioModel.resolve(
				contract, settings, AudioChannel.SFX,
				8f, 0f, false, result
		);
		assertEquals(-12f, result.spatialDecibels(), 0.0001f);

		SpatialAudioModel.resolve(
				contract, settings, AudioChannel.SFX,
				8f, 1f, false, result
		);
		assertEquals(-18f, result.spatialDecibels(), 0.0001f);
		assertEquals(1000f, result.lowPassHz(), 0f);

		SpatialAudioModel.resolve(
				contract, settings, AudioChannel.SFX,
				8f, 2f, false, result
		);
		assertEquals(-24f, result.spatialDecibels(), 0.0001f);
		assertEquals(500f, result.lowPassHz(), 0f);
	}

	@Test
	public void lowPassAndAiPerceptionDoNotDependOnUserVolume() {
		ExperienceContract contract = ExperienceContractTestFixture.load();
		SpatialAudioModel.Result enabled = new SpatialAudioModel.Result();
		SpatialAudioModel.Result muted = new SpatialAudioModel.Result();

		SpatialAudioModel.resolve(
				contract,
				BukovExperienceSettings.defaults(contract),
				AudioChannel.SFX,
				15f, 0f, false, enabled
		);
		SpatialAudioModel.resolve(
				contract,
				BukovExperienceSettings.allPresentationOff(contract),
				AudioChannel.SFX,
				15f, 0f, false, muted
		);

		assertEquals(1000f, enabled.lowPassHz(), 0f);
		assertEquals(enabled.perceptionGain(), muted.perceptionGain(), 0f);
		assertTrue(enabled.perceivable());
		assertTrue(muted.perceivable());
		assertTrue(enabled.audible());
		assertFalse(muted.audible());
	}

	@Test
	public void minimumFloorIsAlsoTheSharedAiAndPlayerHearingCutoff() {
		ExperienceContract contract = ExperienceContractTestFixture.load();
		SpatialAudioModel.Result result = new SpatialAudioModel.Result();

		SpatialAudioModel.resolve(
				contract,
				1f,
				256f,
				5f,
				false,
				result);

		assertEquals(
				contract.minimumAudibleDecibels,
				result.spatialDecibels(),
				0f);
		assertFalse(result.perceivable());
		assertFalse(result.audible());
	}

	@Test
	public void localPlayerGunRemainsFullFrequencyWithoutSpatialAttenuation() {
		ExperienceContract contract = ExperienceContractTestFixture.load();
		SpatialAudioModel.Result result = new SpatialAudioModel.Result();

		SpatialAudioModel.resolve(
				contract,
				BukovExperienceSettings.defaults(contract),
				AudioChannel.SFX,
				100f, 10f, true, result
		);

		assertEquals(0f, result.spatialDecibels(), 0f);
		assertEquals(20_000f, result.lowPassHz(), 0f);
	}
}
