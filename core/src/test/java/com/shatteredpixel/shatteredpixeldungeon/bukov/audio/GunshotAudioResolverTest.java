package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContract;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContractTestFixture;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GunshotAudioResolverTest {

	@Test
	public void threeVariantsStayInsideAuthoredFourPercentPitchRange() {
		assertEquals(0, GunshotAudioResolver.variationIndex(0));
		assertEquals(1, GunshotAudioResolver.variationIndex(1));
		assertEquals(2, GunshotAudioResolver.variationIndex(2));
		assertEquals(2, GunshotAudioResolver.variationIndex(-1));
		assertEquals(0.96f, GunshotAudioResolver.variationPitch(0), 0f);
		assertEquals(1f, GunshotAudioResolver.variationPitch(1), 0f);
		assertEquals(1.04f, GunshotAudioResolver.variationPitch(2), 0f);
		assertEquals(0.96f, GunshotAudioResolver.variationPitch(3), 0f);
	}

	@Test
	public void localShotIsFullFrequencyCenteredAndThreeLayered() {
		ExperienceContract contract = ExperienceContractTestFixture.load();
		SpatialAudioModel.Result spatial = new SpatialAudioModel.Result();
		SpatialAudioModel.resolve(
				contract, 1f, 100f, 9f, true, spatial);
		GunshotAudioPlan plan = new GunshotAudioPlan();

		GunshotAudioResolver.resolve(
				true, 2, 0f, 0f, spatial, plan);

		assertTrue(plan.audible());
		assertEquals(20_000f, plan.lowPassHz(), 0f);
		assertEquals(plan.bodyLeft(), plan.bodyRight(), 0f);
		assertEquals(1.04f, plan.bodyPitch(), 0f);
		assertTrue(plan.mechanicalLeft() > 0f);
		assertTrue(plan.bodyLeft() > plan.mechanicalLeft());
		assertTrue(plan.tailLeft() > 0f);
	}

	@Test
	public void remoteShotUsesSameAttenuationAndStereoDirection() {
		ExperienceContract contract = ExperienceContractTestFixture.load();
		SpatialAudioModel.Result spatial = new SpatialAudioModel.Result();
		SpatialAudioModel.resolve(
				contract, 1f, 8f, 1f, false, spatial);
		GunshotAudioPlan plan = new GunshotAudioPlan();

		GunshotAudioResolver.resolve(
				false, 1, 8f, 0f, spatial, plan);

		assertEquals(-18f, spatial.spatialDecibels(), 0.0001f);
		assertEquals(1000f, plan.lowPassHz(), 0f);
		assertTrue(plan.bodyRight() > plan.bodyLeft());
		assertTrue(plan.bodyRight() < 0.2f);
		assertTrue(
				"occlusion must suppress mechanical highs more than the body",
				plan.mechanicalRight() / plan.bodyRight() < 0.04f);
	}
}
