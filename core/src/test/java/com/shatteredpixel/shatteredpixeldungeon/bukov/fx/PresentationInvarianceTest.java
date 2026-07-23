package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.BukovExperienceSettings;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContract;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContractTestFixture;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class PresentationInvarianceTest {

	@Test
	public void allPresentationOnAndOffProduceIdenticalSimulationState() {
		ExperienceContract contract = ExperienceContractTestFixture.load();
		RunResult enabled = run(
				contract,
				BukovExperienceSettings.defaults(contract)
		);
		RunResult disabled = run(
				contract,
				BukovExperienceSettings.allPresentationOff(contract)
		);

		assertEquals(enabled.simulationHash, disabled.simulationHash);
		assertNotEquals(
				"test must exercise different presentation output",
				enabled.presentationHash,
				disabled.presentationHash
		);
	}

	private static RunResult run(ExperienceContract contract,
								 BukovExperienceSettings settings) {
		FakeSimulation simulation = new FakeSimulation();
		CombatFeedbackPlan plan = new CombatFeedbackPlan();
		long presentationHash = 17L;

		for (int tick = 0; tick < 200; tick++) {
			CombatFeedbackRequest committed = simulation.step(tick);
			plan.clear();
			if (committed != null) {
				CombatFeedbackResolver.add(
						committed, contract, settings, plan
				);
			}
			presentationHash = presentationHash * 31L
					+ Float.floatToIntBits(plan.visualIntensity());
			presentationHash = presentationHash * 31L
					+ Float.floatToIntBits(plan.shakeAmplitudePx());
			presentationHash = presentationHash * 31L
					+ Float.floatToIntBits(plan.vibrationAmplitude());
			presentationHash = presentationHash * 31L + plan.hitstopMs();
		}
		return new RunResult(simulation.hash(), presentationHash);
	}

	private static final class FakeSimulation {
		private int hitPoints = 100;
		private int ammo = 60;
		private int kills;
		private int extractionProgress;
		private int randomState = 0x5EED;

		CombatFeedbackRequest step(int tick) {
			randomState = randomState * 1103515245 + 12345;
			int roll = (randomState >>> 16) & 15;
			extractionProgress += tick % 9 == 0 ? 1 : 0;
			if (roll < 6 && ammo > 0) {
				ammo--;
				if (roll == 0) {
					kills++;
					return new CombatFeedbackRequest(
							CombatFeedbackType.KILL, 0f, 1f
					);
				}
				return new CombatFeedbackRequest(
						CombatFeedbackType.RIFLE_SHOT, 0f, 1f
				);
			}
			if (roll == 15) {
				hitPoints = Math.max(0, hitPoints - 3);
				return new CombatFeedbackRequest(
						CombatFeedbackType.PLAYER_HIT, 0f, 1f
				);
			}
			if (tick == 199) {
				return new CombatFeedbackRequest(
						CombatFeedbackType.EXTRACT_STAMP, 0f, 1f
				);
			}
			return null;
		}

		long hash() {
			long hash = 17L;
			hash = hash * 31L + hitPoints;
			hash = hash * 31L + ammo;
			hash = hash * 31L + kills;
			hash = hash * 31L + extractionProgress;
			hash = hash * 31L + randomState;
			return hash;
		}
	}

	private static final class RunResult {
		final long simulationHash;
		final long presentationHash;

		RunResult(long simulationHash, long presentationHash) {
			this.simulationHash = simulationHash;
			this.presentationHash = presentationHash;
		}
	}
}
