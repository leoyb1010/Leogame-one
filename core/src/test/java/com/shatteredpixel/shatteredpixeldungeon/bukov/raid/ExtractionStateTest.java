package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExtractionStateTest {

	@Test
	public void basicExtractionIsAlwaysAvailableAndCompletesInFiveSeconds() {
		ExtractionState extraction = ExtractionState.basic();
		assertTrue(extraction.availableAt(0f));
		extraction.update(10f, 4.9f, ExtractionState.Interaction.ACTIVE);
		assertFalse(extraction.completed());
		extraction.update(14.9f, 0.1f, ExtractionState.Interaction.ACTIVE);
		assertTrue(extraction.completed());
		assertEquals(1f, extraction.progressFraction(), 0.0001f);
	}

	@Test
	public void conditionalExtractionNeedsPowerAndEightSeconds() {
		ExtractionState extraction = ExtractionState.conditional();
		assertFalse(extraction.availableAt(100f));
		extraction.update(100f, 4f, ExtractionState.Interaction.ACTIVE);
		assertEquals(0f, extraction.progressSeconds(), 0f);

		extraction.setConditionMet(true);
		assertTrue(extraction.availableAt(100f));
		extraction.update(100f, 8f, ExtractionState.Interaction.ACTIVE);
		assertTrue(extraction.completed());
	}

	@Test
	public void temporaryExtractionHasTwoMinuteWindow() {
		float opensAt = 10f * 60f;
		ExtractionState extraction = ExtractionState.temporary(opensAt);
		assertFalse(extraction.availableAt(opensAt - 0.01f));
		assertTrue(extraction.availableAt(opensAt));
		assertTrue(extraction.availableAt(opensAt + 119.99f));
		assertFalse(extraction.availableAt(opensAt + 120f));
	}

	@Test
	public void movementReloadAndHeavyHitResetProgress() {
		for (ExtractionState.Interaction interruption : new ExtractionState.Interaction[]{
				ExtractionState.Interaction.MOVED,
				ExtractionState.Interaction.RELOADED,
				ExtractionState.Interaction.HEAVY_HIT
		}) {
			ExtractionState extraction = ExtractionState.basic();
			extraction.update(0f, 3f, ExtractionState.Interaction.ACTIVE);
			extraction.update(3f, 0f, interruption);
			assertEquals(interruption.name(), 0f, extraction.progressSeconds(), 0f);
		}
	}

	@Test
	public void lightHitRegressesAtTwentyFivePercentPerSecond() {
		ExtractionState extraction = ExtractionState.conditional();
		extraction.setConditionMet(true);
		extraction.update(0f, 6f, ExtractionState.Interaction.ACTIVE);
		extraction.update(6f, 1f, ExtractionState.Interaction.LIGHT_HIT);
		assertEquals(4f, extraction.progressSeconds(), 0.0001f);
		extraction.update(7f, 3f, ExtractionState.Interaction.LIGHT_HIT);
		assertEquals(0f, extraction.progressSeconds(), 0f);
	}

	@Test
	public void expiredTemporaryExtractionClearsPartialProgress() {
		float opensAt = 8f * 60f;
		ExtractionState extraction = ExtractionState.temporary(opensAt);
		extraction.update(opensAt, 2f, ExtractionState.Interaction.ACTIVE);
		assertEquals(2f, extraction.progressSeconds(), 0f);
		extraction.update(opensAt + 120f, 0f, ExtractionState.Interaction.NONE);
		assertEquals(0f, extraction.progressSeconds(), 0f);
	}
}
