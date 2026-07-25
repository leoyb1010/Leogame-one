package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SoundConcurrencyBudgetTest {

	@Test
	public void oldestLowestEligibleVoiceIsReplacedDeterministically() {
		SoundConcurrencyBudget budget = new SoundConcurrencyBudget();
		long oldest = SoundConcurrencyBudget.NO_TOKEN;
		for (int index = 0;
				index < SoundConcurrencyBudget.MAX_ACTIVE_PER_BUS;
				index++) {
			SoundConcurrencyBudget.Admission admission = budget.admit(
					AudioChannel.SFX,
					SoundConcurrencyBudget.Priority.LOW,
					false,
					1f);
			assertTrue(admission.admitted());
			if (index == 0) oldest = admission.token();
		}

		SoundConcurrencyBudget.Admission replacement = budget.admit(
				AudioChannel.SFX,
				SoundConcurrencyBudget.Priority.LOW,
				false,
				1f);
		assertTrue(replacement.admitted());
		assertEquals(oldest, replacement.evictedToken());
		assertFalse(budget.active(oldest));
		assertEquals(
				SoundConcurrencyBudget.MAX_ACTIVE_PER_BUS,
				budget.activeCount(AudioChannel.SFX));
	}

	@Test
	public void protectedCriticalCuesSurviveLowPriorityPressure() {
		SoundConcurrencyBudget budget = new SoundConcurrencyBudget();
		for (int index = 0;
				index < SoundConcurrencyBudget.MAX_ACTIVE_PER_BUS;
				index++) {
			budget.admit(
					AudioChannel.SFX,
					SoundConcurrencyBudget.Priority.LOW,
					false,
					2f);
		}
		SoundConcurrencyBudget.Admission playerGunshot = budget.admit(
				AudioChannel.SFX,
				SoundConcurrencyBudget.defaultPriority(
						SoundCategory.PLAYER_GUNSHOT),
				SoundConcurrencyBudget.protectedByDefault(
						SoundCategory.PLAYER_GUNSHOT),
				2f);
		assertTrue(playerGunshot.admitted());

		for (int index = 0; index < 20; index++) {
			budget.admit(
					AudioChannel.SFX,
					SoundConcurrencyBudget.Priority.LOW,
					false,
					2f);
		}
		assertTrue(budget.active(playerGunshot.token()));
		assertEquals(
				SoundConcurrencyBudget.Priority.CRITICAL,
				SoundConcurrencyBudget.defaultPriority(
						SoundCategory.EXTRACTION_CUE));
		assertEquals(
				SoundConcurrencyBudget.Priority.CRITICAL,
				SoundConcurrencyBudget.defaultPriority(
						SoundCategory.PLAYER_GUNSHOT));
		assertEquals(
				SoundConcurrencyBudget.Priority.NORMAL,
				SoundConcurrencyBudget.defaultPriority(
						SoundCategory.ENEMY_GUNSHOT));
		assertEquals(
				SoundConcurrencyBudget.Priority.HIGH,
				SoundConcurrencyBudget.defaultPriority(
						SoundCategory.COMBAT_FEEDBACK));
		assertEquals(
				SoundConcurrencyBudget.Priority.LOW,
				SoundConcurrencyBudget.defaultPriority(
						SoundCategory.FOOTSTEP));
		assertTrue(SoundConcurrencyBudget.protectedByDefault(
				SoundCategory.EXTRACTION_CUE));
		assertTrue(SoundConcurrencyBudget.protectedByDefault(
				SoundCategory.UI));
	}

	@Test
	public void newestProtectedCriticalCueMayReplaceOldestProtectedPeer() {
		SoundConcurrencyBudget budget = new SoundConcurrencyBudget();
		long oldest = SoundConcurrencyBudget.NO_TOKEN;
		for (int index = 0;
				index < SoundConcurrencyBudget.MAX_ACTIVE_PER_BUS;
				index++) {
			long token = budget.admit(
					AudioChannel.SFX,
					SoundConcurrencyBudget.Priority.CRITICAL,
					true,
					1f).token();
			if (index == 0) oldest = token;
		}
		SoundConcurrencyBudget.Admission newest = budget.admit(
				AudioChannel.SFX,
				SoundConcurrencyBudget.Priority.CRITICAL,
				true,
				1f);
		assertTrue(newest.admitted());
		assertEquals(oldest, newest.evictedToken());
		assertFalse(budget.active(oldest));
	}

	@Test
	public void higherPriorityFullBusRejectsLowCueAndBusesAreIndependent() {
		SoundConcurrencyBudget budget = new SoundConcurrencyBudget();
		for (int index = 0;
				index < SoundConcurrencyBudget.MAX_ACTIVE_PER_BUS;
				index++) {
			budget.admit(
					AudioChannel.SFX,
					SoundConcurrencyBudget.Priority.HIGH,
					false,
					1f);
		}
		assertFalse(budget.admit(
				AudioChannel.SFX,
				SoundConcurrencyBudget.Priority.LOW,
				false,
				1f).admitted());
		assertTrue(budget.admit(
				AudioChannel.AMBIENCE,
				SoundConcurrencyBudget.Priority.LOW,
				false,
				1f).admitted());
		assertEquals(1, budget.activeCount(AudioChannel.AMBIENCE));
	}

	@Test
	public void explicitReleaseAndTimeoutRecoverCapacity() {
		SoundConcurrencyBudget budget = new SoundConcurrencyBudget();
		SoundConcurrencyBudget.Admission explicit = budget.admit(
				AudioChannel.SFX,
				SoundConcurrencyBudget.Priority.NORMAL,
				false,
				1f);
		assertTrue(budget.release(explicit.token()));
		assertFalse(budget.active(explicit.token()));

		SoundConcurrencyBudget.Admission timed = budget.admit(
				AudioChannel.MUSIC,
				SoundConcurrencyBudget.Priority.LOW,
				false,
				0.1f);
		budget.update(0.11f);
		assertFalse(budget.active(timed.token()));
		assertEquals(0, budget.activeCount(AudioChannel.MUSIC));
	}
}
