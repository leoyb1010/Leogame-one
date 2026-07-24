package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovSprintStateTest {

	@Test
	public void heavyLoadReducesSpeedBonusAndDrainsFaster() {
		BukovSprintState light = new BukovSprintState();
		BukovSprintState heavy = new BukovSprintState();

		assertTrue(light.speedMultiplier(true, true, 0f)
				> heavy.speedMultiplier(true, true, 1f));
		light.fixedStep(1f, true, true, 0f);
		heavy.fixedStep(1f, true, true, 1f);

		assertTrue(light.sprinting());
		assertTrue(heavy.sprinting());
		assertTrue(light.staminaFraction() > heavy.staminaFraction());
	}

	@Test
	public void blockedMovementDoesNotDrainAndRecoveryWaitsForDelay() {
		BukovSprintState sprint = new BukovSprintState();
		sprint.fixedStep(1f, true, false, 0.5f);
		assertEquals(1f, sprint.staminaFraction(), 0f);
		assertFalse(sprint.sprinting());

		sprint.fixedStep(1f, true, true, 0f);
		float drained = sprint.staminaFraction();
		sprint.fixedStep(0.4f, false, false, 0f);
		assertEquals(drained, sprint.staminaFraction(), 0f);
		sprint.fixedStep(0.5f, false, false, 0f);
		assertTrue(sprint.staminaFraction() > drained);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsInvalidLoadFraction() {
		new BukovSprintState().speedMultiplier(true, true, Float.NaN);
	}
}
