package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.tutorial.BukovTutorialEvent;
import com.watabou.utils.Bundle;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovRaidModeTest {

	@Test
	public void modeMatrixHasDistinctPlayableRules() {
		Set<String> summaries = new HashSet<>();
		for (BukovRaidMode mode : BukovRaidMode.values()) {
			assertTrue(mode.targetMinutesMinimum > 0f);
			assertTrue(mode.targetMinutesMaximum
					>= mode.targetMinutesMinimum);
			assertTrue(mode.spawnIntervalSeconds > 0f);
			assertTrue(mode.maximumActiveEnemies > 0);
			assertTrue(mode.initialEnemyCount > 0);
			assertTrue(summaries.add(mode.summary));
		}
		assertTrue(BukovRaidMode.EXPEDITION.bossEnabled);
		assertFalse(BukovRaidMode.QUICK_SWEEP.bossEnabled);
		assertFalse(BukovRaidMode.SCAVENGER.usesPlayerLoadout());
		assertFalse(BukovRaidMode.TRAINING_GROUND.usesPlayerLoadout());
		assertFalse(BukovRaidMode.TRAINING_GROUND
				.countsTowardEconomyStatistics());
		assertEquals(3f,
				BukovRaidMode.TRAINING_GROUND.targetMinutesMinimum, 0f);
		assertEquals(5f,
				BukovRaidMode.TRAINING_GROUND.targetMinutesMaximum, 0f);
		assertFalse(BukovRaidMode.TRAINING_GROUND.hasTimeLimit());
		assertFalse(BukovRaidMode.TRAINING_GROUND
				.convergenceStarted(Float.MAX_VALUE));
		assertFalse(BukovRaidMode.TRAINING_GROUND
				.overtime(Float.MAX_VALUE));
		assertEquals(1f, BukovRaidMode.TRAINING_GROUND
				.pressureMultiplier(Float.MAX_VALUE), 0f);
		assertEquals(
				BukovRaidMode.TRAINING_GROUND.maximumActiveEnemies,
				BukovRaidMode.TRAINING_GROUND
						.maximumActiveEnemiesAt(Float.MAX_VALUE));
		assertEquals(2,
				BukovRaidMode.TRAINING_GROUND.initialEnemyCount);
		assertEquals(BukovRaidMode.TRAINING_GROUND,
				BukovRaidMode.BOSS_CONTRACT.next());
		assertEquals(BukovRaidMode.EXPEDITION,
				BukovRaidMode.TRAINING_GROUND.next());
		assertTrue(BukovRaidMode.BOSS_CONTRACT.bossEarliestSeconds
				< BukovRaidMode.EXPEDITION.bossEarliestSeconds);
	}

	@Test
	public void trainingMapIsFixedAndFormalRaidSeedsRemainDistinct() {
		assertEquals(
				BukovRaidMode.TRAINING_GROUND.mapSeed(1L),
				BukovRaidMode.TRAINING_GROUND.mapSeed(999_999L));
		for (BukovRaidMode mode : BukovRaidMode.values()) {
			if (mode.trainingGround()) continue;
			assertEquals(123_456L, mode.mapSeed(123_456L));
		}
	}

	@Test
	public void profileRoundTripPreservesModeProgressAndTutorialLedger() {
		BukovProfile profile = new BukovProfile();
		profile.selectRaidMode(BukovRaidMode.BOSS_CONTRACT);
		assertEquals(1, profile.beginRaid());
		assertTrue(profile.markTutorialSeen(
				BukovTutorialEvent.FIREARM_PICKUP));

		Bundle bundle = new Bundle();
		bundle.put("profile", profile);
		BukovProfile restored = (BukovProfile)bundle.get("profile");

		assertEquals(BukovRaidMode.BOSS_CONTRACT,
				restored.selectedRaidMode());
		assertEquals(1, restored.raidsStarted());
		assertTrue(restored.tutorialSeen(
				BukovTutorialEvent.FIREARM_PICKUP));
		assertFalse(restored.tutorialSeen(
				BukovTutorialEvent.BLEEDING));
	}

	@Test
	public void sessionRoundTripPinsModeAndTutorialWindow() {
		RaidSession session = RaidSession.create(
				91L,
				"boss-contract-1",
				BukovRaidMode.BOSS_CONTRACT,
				2);
		Bundle bundle = new Bundle();
		bundle.put("session", session);
		RaidSession restored = (RaidSession)bundle.get("session");

		assertEquals(BukovRaidMode.BOSS_CONTRACT, restored.raidMode());
		assertEquals(2, restored.raidOrdinal());
		assertFalse(restored.firstRaidProtectionActive());
	}

	@Test
	public void firstRaidProtectionExpiresAtNinetySeconds() {
		RaidSession first = RaidSession.create(
				7L, "first", BukovRaidMode.EXPEDITION, 1);
		assertTrue(first.firstRaidProtectionActive());
		first.advance(90f);
		assertFalse(first.firstRaidProtectionActive());

		RaidSession third = RaidSession.create(
				8L, "third", BukovRaidMode.EXPEDITION, 3);
		assertFalse(third.firstRaidProtectionActive());
	}
}
