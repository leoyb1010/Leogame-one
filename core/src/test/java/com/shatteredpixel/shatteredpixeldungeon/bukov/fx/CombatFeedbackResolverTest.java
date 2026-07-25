package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.BukovExperienceSettings;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContract;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContractTestFixture;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CombatFeedbackResolverTest {

	@Test
	public void presentationMasterSwitchesDisableEveryFeedbackChannel() {
		ExperienceContract contract = ExperienceContractTestFixture.load();
		CombatFeedbackRequest request = new CombatFeedbackRequest(
				CombatFeedbackType.BOSS_PHASE_BREAK,
				0f,
				1f
		);
		CombatFeedbackPlan enabled = new CombatFeedbackPlan();
		CombatFeedbackPlan disabled = new CombatFeedbackPlan();

		CombatFeedbackResolver.add(
				request,
				contract,
				BukovExperienceSettings.defaults(contract),
				enabled
		);
		CombatFeedbackResolver.add(
				request,
				contract,
				BukovExperienceSettings.allPresentationOff(contract),
				disabled
		);

		assertTrue(enabled.visual());
		assertTrue(enabled.audio());
		assertEquals(1f, enabled.shakeAmplitudePx(), 0f);
		assertEquals(1f, enabled.vibrationAmplitude(), 0f);
		assertEquals(120, enabled.hitstopMs());
		assertFalse(disabled.visual());
		assertFalse(disabled.audio());
		assertEquals(0f, disabled.shakeAmplitudePx(), 0f);
		assertEquals(0f, disabled.vibrationAmplitude(), 0f);
		assertEquals(0, disabled.hitstopMs());
	}

	@Test
	public void simultaneousHitstopAndEnvelopesUseHighestValueWithoutStacking() {
		ExperienceContract contract = ExperienceContractTestFixture.load();
		CombatFeedbackPlan plan = new CombatFeedbackPlan();
		BukovExperienceSettings settings =
				BukovExperienceSettings.defaults(contract);

		CombatFeedbackResolver.add(
				new CombatFeedbackRequest(
						CombatFeedbackType.KILL, 0f, 1f
				),
				contract, settings, plan
		);
		CombatFeedbackResolver.add(
				new CombatFeedbackRequest(
						CombatFeedbackType.BOSS_PHASE_BREAK, 0f, 1f
				),
				contract, settings, plan
		);

		assertEquals(120, plan.hitstopMs());
		assertEquals(1f, plan.shakeAmplitudePx(), 0f);
		assertEquals(1f, plan.vibrationAmplitude(), 0f);
		assertTrue(plan.shakeAmplitudePx() <= contract.maximumShakePx);
	}

	@Test
	public void shakeVibrationAndHitstopCanBeDisabledIndependently() {
		ExperienceContract contract = ExperienceContractTestFixture.load();
		BukovExperienceSettings defaults =
				BukovExperienceSettings.defaults(contract);
		CombatFeedbackRequest request = new CombatFeedbackRequest(
				CombatFeedbackType.BOSS_PHASE_BREAK, 0f, 1f
		);
		CombatFeedbackPlan noShake = resolve(
				request, contract, controls(defaults, 0f, 1f, true)
		);
		CombatFeedbackPlan noVibration = resolve(
				request, contract, controls(defaults, 1f, 0f, true)
		);
		CombatFeedbackPlan noHitstop = resolve(
				request, contract, controls(defaults, 1f, 1f, false)
		);

		assertEquals(0f, noShake.shakeAmplitudePx(), 0f);
		assertEquals(1f, noShake.vibrationAmplitude(), 0f);
		assertEquals(120, noShake.hitstopMs());
		assertEquals(1f, noVibration.shakeAmplitudePx(), 0f);
		assertEquals(0f, noVibration.vibrationAmplitude(), 0f);
		assertEquals(120, noVibration.hitstopMs());
		assertEquals(1f, noHitstop.shakeAmplitudePx(), 0f);
		assertEquals(1f, noHitstop.vibrationAmplitude(), 0f);
		assertEquals(0, noHitstop.hitstopMs());
	}

	@Test
	public void explosionFeedbackFadesOutAtFifteenTiles() {
		ExperienceContract contract = ExperienceContractTestFixture.load();
		CombatFeedbackPlan explosion = new CombatFeedbackPlan();
		CombatFeedbackPlan bossOverload = new CombatFeedbackPlan();

		CombatFeedbackResolver.add(
				new CombatFeedbackRequest(
						CombatFeedbackType.EXPLOSION, 15f, 1f
				),
				contract,
				BukovExperienceSettings.defaults(contract),
				explosion
		);
		CombatFeedbackResolver.add(
				new CombatFeedbackRequest(
						CombatFeedbackType.BOSS_OVERLOAD, 15f, 1f
				),
				contract,
				BukovExperienceSettings.defaults(contract),
				bossOverload
		);

		for (CombatFeedbackPlan plan :
				new CombatFeedbackPlan[]{explosion, bossOverload}) {
			assertFalse(plan.visual());
			assertFalse(plan.audio());
			assertEquals(0f, plan.shakeAmplitudePx(), 0f);
			assertEquals(0f, plan.vibrationAmplitude(), 0f);
		}
	}

	@Test
	public void reducedMotionHalvesShakeWithoutChangingOtherChannels() {
		ExperienceContract contract = ExperienceContractTestFixture.load();
		BukovExperienceSettings defaults =
				BukovExperienceSettings.defaults(contract);
		BukovExperienceSettings reduced = new BukovExperienceSettings(
				defaults.masterVolume,
				defaults.musicVolume,
				defaults.sfxVolume,
				defaults.ambienceVolume,
				true,
				1f,
				1f,
				true,
				false,
				true,
				false
		);
		CombatFeedbackRequest request = new CombatFeedbackRequest(
				CombatFeedbackType.BOSS_SLAM, 0f, 1f
		);
		CombatFeedbackPlan normalPlan = new CombatFeedbackPlan();
		CombatFeedbackPlan reducedPlan = new CombatFeedbackPlan();

		CombatFeedbackResolver.add(
				request, contract, defaults, normalPlan
		);
		CombatFeedbackResolver.add(
				request, contract, reduced, reducedPlan
		);

		assertEquals(
				normalPlan.shakeAmplitudePx() * 0.5f,
				reducedPlan.shakeAmplitudePx(),
				0f
		);
		assertEquals(
				normalPlan.vibrationAmplitude(),
				reducedPlan.vibrationAmplitude(),
				0f
		);
	}

	@Test
	public void pooledPrimitiveAdapterMatchesObjectAdapter() {
		ExperienceContract contract = ExperienceContractTestFixture.load();
		BukovExperienceSettings settings =
				BukovExperienceSettings.defaults(contract);
		CombatFeedbackPlan objectPlan = new CombatFeedbackPlan();
		CombatFeedbackPlan pooledPlan = new CombatFeedbackPlan();

		CombatFeedbackResolver.add(
				new CombatFeedbackRequest(
						CombatFeedbackType.PLAYER_HIT, 0f, 0.8f),
				contract,
				settings,
				objectPlan);
		CombatFeedbackResolver.add(
				CombatFeedbackType.PLAYER_HIT,
				0f,
				0.8f,
				contract,
				settings.visualEffects,
				settings.sfxVolume * settings.masterVolume,
				settings.screenShakeScale,
				settings.controllerVibrationScale,
				settings.hitstopEnabled,
				settings.reduceMotion,
				settings.reduceFlashes,
				pooledPlan);

		assertEquals(
				objectPlan.visualIntensity(),
				pooledPlan.visualIntensity(),
				0f);
		assertEquals(
				objectPlan.shakeAmplitudePx(),
				pooledPlan.shakeAmplitudePx(),
				0f);
		assertEquals(
				objectPlan.vibrationAmplitude(),
				pooledPlan.vibrationAmplitude(),
				0f);
		assertEquals(objectPlan.hitstopMs(), pooledPlan.hitstopMs());
	}

	private static BukovExperienceSettings controls(
			BukovExperienceSettings defaults,
			float shake,
			float vibration,
			boolean hitstop) {
		return new BukovExperienceSettings(
				defaults.masterVolume,
				defaults.musicVolume,
				defaults.sfxVolume,
				defaults.ambienceVolume,
				true,
				shake,
				vibration,
				hitstop,
				false,
				false,
				false
		);
	}

	private static CombatFeedbackPlan resolve(
			CombatFeedbackRequest request,
			ExperienceContract contract,
			BukovExperienceSettings settings) {
		CombatFeedbackPlan plan = new CombatFeedbackPlan();
		CombatFeedbackResolver.add(request, contract, settings, plan);
		return plan;
	}
}
