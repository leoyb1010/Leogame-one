package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InitialContactCombatPolicyTest {

	@Test
	public void onboardingContactGetsThreeSecondWarningAndBoundedDamage() {
		boolean applies = InitialContactCombatPolicy.applies(true, true);

		assertTrue(applies);
		assertEquals(
				2.25f,
				InitialContactCombatPolicy.openingWarningSeconds(applies),
				0f);
		assertEquals(
				0.75f,
				InitialContactCombatPolicy.aimSeconds(0.45f, applies),
				0f);
		assertEquals(
				2,
				InitialContactCombatPolicy.minimumDamage(2, applies));
		assertEquals(
				3,
				InitialContactCombatPolicy.maximumDamage(2, 5, applies));

		float firstDamageSeconds =
				InitialContactCombatPolicy.OPENING_WARNING_SECONDS
						+ InitialContactCombatPolicy.AIM_SECONDS;
		int fastestLethalShots = (20
				+ InitialContactCombatPolicy.MAXIMUM_DAMAGE - 1)
				/ InitialContactCombatPolicy.MAXIMUM_DAMAGE;
		float fastestTtk = firstDamageSeconds
				+ (fastestLethalShots - 1)
						* InitialContactCombatPolicy.AIM_SECONDS;
		assertEquals(3f, firstDamageSeconds, 0f);
		assertEquals(7.5f, fastestTtk, 0f);
	}

	@Test
	public void normalReinforcementsKeepTheirAuthoredCombatProfile() {
		boolean applies = InitialContactCombatPolicy.applies(false, true);

		assertFalse(applies);
		assertEquals(
				0f,
				InitialContactCombatPolicy.openingWarningSeconds(applies),
				0f);
		assertEquals(
				0.45f,
				InitialContactCombatPolicy.aimSeconds(0.45f, applies),
				0f);
		assertEquals(
				5,
				InitialContactCombatPolicy.maximumDamage(2, 5, applies));
	}
}
