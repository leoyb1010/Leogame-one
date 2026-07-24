package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.medical;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.LootTransaction;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidOutcome;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidResult;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidSettlement;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;
import com.watabou.utils.Bundle;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RealtimeMedicalSystemTest {

	@Test
	public void catalogDefinesAllSevenAuthoredMedicalItems() {
		Set<String> ids = new HashSet<>();
		for (MedicalDefinition definition : MedicalCatalog.all()) {
			ids.add(definition.id);
			assertTrue(definition.useSeconds > 0f);
			assertTrue(definition.cooldownSeconds >= 0f);
		}
		assertEquals(7, ids.size());
		assertTrue(ids.contains("bandage"));
		assertTrue(ids.contains("painkiller"));
		assertTrue(ids.contains("first_aid"));
		assertTrue(ids.contains("tourniquet"));
		assertTrue(ids.contains("antiseptic"));
		assertTrue(ids.contains("splint"));
		assertTrue(ids.contains("stim"));
		assertEquals(2.5f,
				MedicalCatalog.require("first_aid").useSeconds, 0.0001f);
		assertEquals(4f,
				MedicalCatalog.require("medical:splint").useSeconds, 0.0001f);
	}

	@Test
	public void interruptionsNeverConsumeThePhysicalStack() {
		assertInterruptedWithoutConsumption(
				false, true, false,
				RealtimeMedicalSystem.StepResult.INTERRUPTED_DAMAGE);
		assertInterruptedWithoutConsumption(
				false, false, true,
				RealtimeMedicalSystem.StepResult.INTERRUPTED_SHOT);
		assertInterruptedWithoutConsumption(
				true, false, false,
				RealtimeMedicalSystem.StepResult.INTERRUPTED_MOVE);
	}

	@Test
	public void mobilePainkillerCompletesButDamageStillInterruptsIt() {
		LootTransaction ledger = medicalLedger(
				"raid-pain",
				"uid-pain",
				"painkiller",
				2);
		RealtimeStatusState status = new RealtimeStatusState(100f, 100f);
		status.addPain(0.8f);
		RealtimeMedicalSystem system =
				RealtimeMedicalSystem.fromLedger(ledger, status);

		assertEquals(
				RealtimeMedicalSystem.BeginResult.STARTED,
				system.beginUse("uid-pain"));
		assertEquals(
				RealtimeMedicalSystem.StepResult.IN_PROGRESS,
				system.fixedStep(0.6f, true, false, false));
		assertEquals(
				RealtimeMedicalSystem.StepResult.COMPLETED,
				system.fixedStep(0.6f, true, false, false));
		assertEquals(1, system.quantity("uid-pain"));
		assertTrue(status.painSuppressed());

		system.fixedStep(0.75f, false, false, false);
		status.addPain(0.1f);
		assertEquals(
				RealtimeMedicalSystem.BeginResult.NO_EFFECT,
				system.beginUse("uid-pain"));
	}

	@Test
	public void healingIsCappedAndEachCompletionConsumesExactlyOne() {
		LootTransaction ledger = medicalLedger(
				"raid-heal",
				"uid-aid",
				"first_aid",
				2);
		RealtimeStatusState status = new RealtimeStatusState(100f, 30f);
		RealtimeMedicalSystem system =
				RealtimeMedicalSystem.fromLedger(ledger, status);

		complete(system, "uid-aid", 2.5f);
		assertEquals(72f, status.health(), 0.0001f);
		assertEquals(1, system.quantity("uid-aid"));
		system.fixedStep(1f, false, false, false);

		complete(system, "uid-aid", 2.5f);
		assertEquals(100f, status.health(), 0.0001f);
		assertEquals(0, system.quantity("uid-aid"));
		system.fixedStep(1f, false, false, false);

		assertEquals(
				RealtimeMedicalSystem.BeginResult.EMPTY,
				system.beginUse("uid-aid"));
		assertEquals(100f, status.health(), 0.0001f);
	}

	@Test
	public void mobileAvailabilityIsReadOnlyAndRequiresApplicableMedicine() {
		LootTransaction ledger = medicalLedger(
				"raid-availability",
				"uid-aid",
				"first_aid",
				1);
		RealtimeStatusState status = new RealtimeStatusState(100f, 100f);
		RealtimeMedicalSystem system =
				RealtimeMedicalSystem.fromLedger(ledger, status);

		assertFalse(system.canBeginAny());
		status.applyDamage(20f);
		assertTrue(system.canBeginAny());
		assertEquals(1, system.quantity("uid-aid"));
		assertFalse(system.isUsing());

		assertEquals(
				RealtimeMedicalSystem.BeginResult.STARTED,
				system.beginUse("uid-aid"));
		assertFalse(system.canBeginAny());
	}

	@Test
	public void bleedingUsesWallClockDeltaAndTreatmentCannotResurrect() {
		RealtimeStatusState status = new RealtimeStatusState(10f, 10f);
		status.addBleeding(2f);
		status.fixedStep(2.5f);
		assertEquals(5f, status.health(), 0.0001f);
		status.fixedStep(3f);
		assertTrue(status.isDead());
		status.fixedStep(30f);
		assertEquals(0f, status.health(), 0.0001f);

		LootTransaction ledger = medicalLedger(
				"raid-dead",
				"uid-stim",
				"stim",
				1);
		RealtimeMedicalSystem system =
				RealtimeMedicalSystem.fromLedger(ledger, status);
		assertEquals(
				RealtimeMedicalSystem.BeginResult.DEAD,
				system.beginUse("uid-stim"));
		assertEquals(1, system.quantity("uid-stim"));
	}

	@Test
	public void medicalEffectsTreatBleedingFractureConcussionAndPain() {
		RealtimeStatusState status = new RealtimeStatusState(100f, 70f);
		status.addBleeding(1.2f);
		status.setFractured(true);
		status.addConcussion(25f);
		status.addPain(0.7f);
		assertTrue(status.movementMultiplier() < 1f);
		assertTrue(status.aimMultiplier() < 1f);

		LootTransaction ledger = new LootTransaction("raid-status", 40f);
		add(ledger, "tourniquet-uid", "tourniquet", 1);
		add(ledger, "splint-uid", "splint", 1);
		add(ledger, "stim-uid", "stim", 1);
		RealtimeMedicalSystem system =
				RealtimeMedicalSystem.fromLedger(ledger, status);

		complete(system, "tourniquet-uid", 2f);
		assertEquals(0f, status.bleedingPerSecond(), 0.0001f);
		system.fixedStep(0.75f, false, false, false);
		complete(system, "splint-uid", 4f);
		assertFalse(status.fractured());
		system.fixedStep(1f, false, false, false);
		complete(system, "stim-uid", 1f);
		assertTrue(status.painSuppressed());
		assertTrue(status.stimulantRemaining() > 0f);
		assertTrue(status.concussionRemaining() < 25f);
	}

	@Test
	public void settlementWriteBackIsIdempotentForDeathAndExtraction() {
		LootTransaction extractedLedger =
				consumedAndFinishedLedger("raid-extract");
		BukovProfile extractedProfile = new BukovProfile();
		RaidResult extracted = new RaidSettlement().settle(
				extractedProfile,
				extractedLedger,
				RaidOutcome.SUCCESS);
		assertEquals(2L, extracted.transferredQuantity());
		assertEquals(
				2,
				extractedProfile.stash().item("uid-bandage").quantity());

		LootTransaction deathLedger =
				consumedAndFinishedLedger("raid-death");
		BukovProfile deadProfile = new BukovProfile();
		RaidResult death = new RaidSettlement().settle(
				deadProfile,
				deathLedger,
				RaidOutcome.DEATH);
		assertEquals(2L, death.lostQuantity());
		assertEquals(0, deadProfile.stash().distinctItemCount());
	}

	@Test
	public void unfinishedTreatmentIsNotConsumedAtSettlement() {
		LootTransaction ledger = medicalLedger(
				"raid-unfinished",
				"uid-bandage",
				"bandage",
				3);
		RealtimeStatusState status = new RealtimeStatusState(100f, 50f);
		RealtimeMedicalSystem system =
				RealtimeMedicalSystem.fromLedger(ledger, status);
		system.beginUse("uid-bandage");
		system.fixedStep(0.5f, false, false, false);

		system.finishRaid(ledger);
		assertEquals(3, ledger.item("uid-bandage").quantity());
		assertTrue(system.closed());
		assertEquals(
				RealtimeMedicalSystem.BeginResult.CLOSED,
				system.beginUse("uid-bandage"));
		assertEquals(
				RealtimeMedicalSystem.StepResult.CLOSED,
				system.fixedStep(1f, false, false, false));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNonFiniteFixedStep() {
		RealtimeStatusState status = new RealtimeStatusState(100f, 80f);
		LootTransaction ledger = medicalLedger(
				"raid-nan",
				"uid-bandage",
				"bandage",
				1);
		RealtimeMedicalSystem.fromLedger(ledger, status).fixedStep(
				Float.NaN,
				false,
				false,
				false);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsInfiniteBleeding() {
		new RealtimeStatusState(100f, 80f).addBleeding(
				Float.POSITIVE_INFINITY);
	}

	@Test
	public void statusAndActiveTreatmentResumeWithoutQuantityOrTimeDrift() {
		LootTransaction ledger = medicalLedger(
				"raid-medical-resume",
				"uid-aid",
				"first_aid",
				2);
		RealtimeStatusState status = new RealtimeStatusState(100f, 40f);
		status.addBleeding(0.7f);
		status.setFractured(true);
		status.addPain(0.2f);
		status.addConcussion(9f);
		status.suppressPain(12f);
		status.applyStimulant(8f);
		RealtimeMedicalSystem original =
				RealtimeMedicalSystem.fromLedger(ledger, status);
		assertEquals(RealtimeMedicalSystem.BeginResult.STARTED,
				original.beginUse("uid-aid"));
		assertEquals(RealtimeMedicalSystem.StepResult.IN_PROGRESS,
				original.fixedStep(0.75f, false, false, false));
		original.writeBack(ledger);

		Bundle bundle = new Bundle();
		bundle.put("status", status);
		bundle.put("medical", original.snapshot());
		RealtimeStatusState restoredStatus =
				(RealtimeStatusState)bundle.get("status");
		RealtimeMedicalSystem.Snapshot restoredSnapshot =
				(RealtimeMedicalSystem.Snapshot)bundle.get("medical");
		RealtimeMedicalSystem restored =
				RealtimeMedicalSystem.fromLedger(ledger, restoredStatus);
		restored.restoreSnapshot(restoredSnapshot);

		assertEquals(status.health(), restoredStatus.health(), 0f);
		assertEquals(status.bleedingPerSecond(),
				restoredStatus.bleedingPerSecond(), 0f);
		assertEquals(status.fractured(), restoredStatus.fractured());
		assertEquals(status.painSeverity(),
				restoredStatus.painSeverity(), 0f);
		assertEquals(status.concussionRemaining(),
				restoredStatus.concussionRemaining(), 0f);
		assertEquals(status.painSuppressionRemaining(),
				restoredStatus.painSuppressionRemaining(), 0f);
		assertEquals(status.stimulantRemaining(),
				restoredStatus.stimulantRemaining(), 0f);
		assertEquals("uid-aid", restored.activeItemUid());
		assertEquals(original.useProgress(), restored.useProgress(), 0f);
		assertEquals(2, restored.quantity("uid-aid"));

		assertEquals(RealtimeMedicalSystem.StepResult.COMPLETED,
				restored.fixedStep(1.75f, false, false, false));
		restored.writeBack(ledger);
		assertEquals(1, ledger.item("uid-aid").quantity());

		RealtimeMedicalSystem afterSecondResume =
				RealtimeMedicalSystem.fromLedger(ledger, restoredStatus);
		afterSecondResume.restoreSnapshot(restored.snapshot());
		assertEquals(1, afterSecondResume.quantity("uid-aid"));
		assertEquals(restored.cooldownRemaining(),
				afterSecondResume.cooldownRemaining(), 0f);
	}

	private static void assertInterruptedWithoutConsumption(
			boolean moved,
			boolean damaged,
			boolean fired,
			RealtimeMedicalSystem.StepResult expected) {
		LootTransaction ledger = medicalLedger(
				"raid-interrupt-" + expected,
				"uid-bandage",
				"bandage",
				2);
		RealtimeStatusState status = new RealtimeStatusState(100f, 50f);
		RealtimeMedicalSystem system =
				RealtimeMedicalSystem.fromLedger(ledger, status);
		assertEquals(
				RealtimeMedicalSystem.BeginResult.STARTED,
				system.beginUse("uid-bandage"));
		assertEquals(
				expected,
				system.fixedStep(0.25f, moved, damaged, fired));
		assertEquals(2, system.quantity("uid-bandage"));
		system.writeBack(ledger);
		assertEquals(2, ledger.item("uid-bandage").quantity());
	}

	private static LootTransaction consumedAndFinishedLedger(String raidId) {
		LootTransaction ledger = medicalLedger(
				raidId,
				"uid-bandage",
				"bandage",
				3);
		RealtimeStatusState status = new RealtimeStatusState(100f, 50f);
		RealtimeMedicalSystem system =
				RealtimeMedicalSystem.fromLedger(ledger, status);
		complete(system, "uid-bandage", 1.5f);
		system.finishRaid(ledger);
		String fingerprint = ledger.fingerprint();
		system.finishRaid(ledger);

		assertEquals(fingerprint, ledger.fingerprint());
		assertEquals(2, ledger.item("uid-bandage").quantity());
		assertEquals(2L, ledger.totalQuantity());
		return ledger;
	}

	private static void complete(
			RealtimeMedicalSystem system,
			String uid,
			float duration) {
		assertEquals(
				RealtimeMedicalSystem.BeginResult.STARTED,
				system.beginUse(uid));
		assertEquals(
				RealtimeMedicalSystem.StepResult.COMPLETED,
				system.fixedStep(duration, false, false, false));
	}

	private static LootTransaction medicalLedger(
			String raidId,
			String uid,
			String definitionId,
			int quantity) {
		LootTransaction ledger = new LootTransaction(raidId, 40f);
		add(ledger, uid, definitionId, quantity);
		return ledger;
	}

	private static void add(
			LootTransaction ledger,
			String uid,
			String definitionId,
			int quantity) {
		assertEquals(
				LootTransaction.PickupResult.ADDED,
				ledger.pickup(new RaidItem(
						uid,
						definitionId,
						quantity,
						0.1f,
						100,
						false,
						false,
						1f)));
	}
}
