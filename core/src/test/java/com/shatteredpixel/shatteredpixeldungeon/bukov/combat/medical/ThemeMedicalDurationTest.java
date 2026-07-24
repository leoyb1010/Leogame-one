package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.medical;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.LootTransaction;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ThemeMedicalDurationTest {

	@Test
	public void environmentMultiplierSlowsTreatmentWithoutExtraConsumption() {
		LootTransaction ledger = new LootTransaction("theme-medical", 40f);
		ledger.pickup(new RaidItem(
				"uid-aid",
				"first_aid",
				2,
				0.1f,
				100,
				false,
				false,
				1f));
		RealtimeStatusState status =
				new RealtimeStatusState(100f, 30f);
		RealtimeMedicalSystem system =
				RealtimeMedicalSystem.fromLedger(ledger, status);
		assertEquals(
				RealtimeMedicalSystem.BeginResult.STARTED,
				system.beginUse("uid-aid"));

		assertEquals(
				RealtimeMedicalSystem.StepResult.IN_PROGRESS,
				system.fixedStep(
						2.5f,
						false,
						false,
						false,
						1.2f));
		assertEquals(2, system.quantity("uid-aid"));
		assertEquals(
				RealtimeMedicalSystem.StepResult.COMPLETED,
				system.fixedStep(
						0.5f,
						false,
						false,
						false,
						1.2f));
		assertEquals(1, system.quantity("uid-aid"));
	}
}
