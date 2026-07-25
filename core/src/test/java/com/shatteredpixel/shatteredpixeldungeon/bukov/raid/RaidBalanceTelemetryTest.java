package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.watabou.utils.Bundle;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RaidBalanceTelemetryTest {

	@Test
	public void settlementPersistsLocalFactsAndReplaysIdempotently() {
		RaidSession session = RaidSession.create(
				41L,
				"telemetry-raid",
				BukovRaidMode.EXPEDITION,
				2);
		session.identifyBalanceContext(
				"fog_depot",
				"balanced_mid");
		session.recordContainerSearch();
		session.recordContainerSearch();
		for (int i = 0; i < 4; i++) session.recordFirefight();
		session.recordDamageTaken(17);
		session.recordKill();
		session.recordKill();
		session.advance(14f * 60f);
		RaidBalanceTelemetry telemetry =
				session.settledBalanceTelemetry(
						RaidOutcome.SUCCESS,
						ExtractionState.Type.CONDITIONAL);

		BukovProfile profile = new BukovProfile();
		LootTransaction loot =
				new LootTransaction("telemetry-raid", 20f);
		loot.pickup(new RaidItem(
				"valuable",
				"loot:valuable",
				2,
				0.5f,
				75,
				true,
				false,
				1f));
		RaidSettlement settlement = new RaidSettlement();
		RaidResult first = settlement.settle(
				profile,
				loot,
				RaidOutcome.SUCCESS,
				session.elapsedSeconds,
				session.killCount(),
				false,
				session.raidMode(),
				telemetry);

		Bundle profileBundle = new Bundle();
		profileBundle.put("profile", profile);
		BukovProfile restoredProfile =
				(BukovProfile) profileBundle.get("profile");
		RaidBalanceTelemetry stored =
				restoredProfile.settlement("telemetry-raid")
						.balanceTelemetry();
		assertFalse(first.replayed());
		assertTrue(stored.available());
		assertEquals(41L, stored.seed());
		assertEquals(BukovRaidMode.EXPEDITION, stored.mode());
		assertEquals("fog_depot", stored.themeId());
		assertEquals("balanced_mid", stored.routeId());
		assertEquals(2, stored.containerSearches());
		assertEquals(4, stored.firefights());
		assertEquals(2, stored.kills());
		assertEquals(17, stored.damageTaken());
		assertEquals(150L, stored.extractedValue());
		assertEquals(
				RaidBalanceTelemetry.End.CONDITIONAL_EXTRACTION,
				stored.end());

		RaidResult replay = settlement.settle(
				restoredProfile,
				loot,
				RaidOutcome.SUCCESS,
				session.elapsedSeconds,
				session.killCount(),
				false,
				session.raidMode(),
				telemetry);
		assertTrue(replay.replayed());
		assertEquals(1, restoredProfile.settlements().size());

		session.recordFirefight();
		RaidBalanceTelemetry changed =
				session.settledBalanceTelemetry(
						RaidOutcome.SUCCESS,
						ExtractionState.Type.CONDITIONAL);
		try {
			settlement.settle(
					restoredProfile,
					loot,
					RaidOutcome.SUCCESS,
					session.elapsedSeconds,
					session.killCount(),
					false,
					session.raidMode(),
					changed);
			fail("changed telemetry must be rejected");
		} catch (IllegalStateException expected) {
			assertTrue(expected.getMessage().contains(
					"payload changed"));
		}
	}

	@Test
	public void firefightLatchCountsOnlyNewEncountersAcrossCheckpoint() {
		RaidSession session = RaidSession.create(
				52L,
				"firefight-latch",
				BukovRaidMode.EXPEDITION,
				1);
		session.recordBalanceRoom("spawn");
		session.recordBalanceRoom("shared");
		session.recordBalanceRoom("shared");
		session.updateBalanceFirefightState(true, true);
		session.updateBalanceFirefightState(true, true);

		Bundle checkpoint = new Bundle();
		checkpoint.put("session", session);
		RaidSession restored =
				(RaidSession)checkpoint.get("session");
		assertEquals(
				java.util.Arrays.asList("spawn", "shared"),
				restored.balanceVisitedRooms());
		restored.updateBalanceFirefightState(true, true);
		assertEquals(1, restored.balanceTelemetry().firefights());

		// SEARCH/INVESTIGATE holds the current encounter open but never starts
		// a gunfight by itself.
		restored.updateBalanceFirefightState(false, true);
		restored.updateBalanceFirefightState(true, true);
		assertEquals(1, restored.balanceTelemetry().firefights());

		restored.updateBalanceFirefightState(false, false);
		restored.updateBalanceFirefightState(true, true);
		assertEquals(2, restored.balanceTelemetry().firefights());
	}

	@Test
	public void investigationCannotPreArmTheFirefightLatch() {
		RaidSession session = RaidSession.create(
				53L,
				"sound-investigation",
				BukovRaidMode.EXPEDITION,
				1);
		session.updateBalanceFirefightState(false, false);
		assertEquals(0, session.balanceTelemetry().firefights());

		session.updateBalanceFirefightState(true, false);
		assertEquals(1, session.balanceTelemetry().firefights());
	}

	@Test
	public void oldCheckpointAndReceiptUseUnavailableDefaults() {
		RaidSession current = RaidSession.create(
				9L,
				"legacy-session");
		Bundle sessionBundle = new Bundle();
		current.storeInBundle(sessionBundle);
		sessionBundle.remove("balance_telemetry");
		RaidSession restoredSession = new RaidSession();
		restoredSession.restoreFromBundle(sessionBundle);

		assertFalse(restoredSession.balanceTelemetry().available());
		// New instrumentation is a no-op for an incomplete legacy run.
		restoredSession.recordContainerSearch();
		restoredSession.recordFirefight();
		restoredSession.recordDamageTaken(5);

		BukovProfile profile = new BukovProfile();
		LootTransaction loot =
				new LootTransaction("legacy-receipt", 20f);
		new RaidSettlement().settle(
				profile,
				loot,
				RaidOutcome.DEATH);
		SettlementReceipt currentReceipt =
				profile.settlement("legacy-receipt");
		Bundle receiptBundle = new Bundle();
		currentReceipt.storeInBundle(receiptBundle);
		receiptBundle.remove("balance_telemetry");
		SettlementReceipt restoredReceipt =
				new SettlementReceipt();
		restoredReceipt.restoreFromBundle(receiptBundle);

		assertFalse(restoredReceipt.balanceTelemetry().available());
		assertEquals(
				RaidBalanceTelemetry.End.UNKNOWN,
				restoredReceipt.balanceTelemetry().end());
	}
}
