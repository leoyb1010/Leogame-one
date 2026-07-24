package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InitialEnemyRosterPolicyTest {

	@Test
	public void freshRaidPopulatesUntilConfiguredTarget() {
		assertTrue(InitialEnemyRosterPolicy.shouldPopulate(
				false, 0, 0, 3));
		assertTrue(InitialEnemyRosterPolicy.shouldPopulate(
				false, 2, 0, 3));
		assertFalse(InitialEnemyRosterPolicy.shouldPopulate(
				false, 3, 0, 3));
		assertTrue(InitialEnemyRosterPolicy.completed(3, 3));
	}

	@Test
	public void legacyCompletedEmptyUntouchedRosterIsRecovered() {
		assertTrue(InitialEnemyRosterPolicy.shouldPopulate(
				true, 0, 0, 3));
	}

	@Test
	public void legitimateKillsNeverTriggerInitialRosterRespawn() {
		assertFalse(InitialEnemyRosterPolicy.shouldPopulate(
				true, 0, 1, 3));
		assertFalse(InitialEnemyRosterPolicy.shouldPopulate(
				true, 1, 0, 3));
	}

	@Test
	public void trainingAndFirstRaidStartWithVisibleContactOnlyWhenEmpty() {
		assertTrue(InitialEnemyRosterPolicy.needsVisibleContact(
				BukovRaidMode.TRAINING_GROUND, 8, 0));
		assertTrue(InitialEnemyRosterPolicy.needsVisibleContact(
				BukovRaidMode.EXPEDITION, 1, 0));
		assertFalse(InitialEnemyRosterPolicy.needsVisibleContact(
				BukovRaidMode.EXPEDITION, 2, 0));
		assertFalse(InitialEnemyRosterPolicy.needsVisibleContact(
				BukovRaidMode.EXPEDITION, 1, 1));
	}
}
