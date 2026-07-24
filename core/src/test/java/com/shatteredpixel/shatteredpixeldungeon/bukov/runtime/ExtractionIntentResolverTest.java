package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.ExtractionState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExtractionIntentResolverTest {

	@Test
	public void interactOrStandingStillStartsInsideZone() {
		assertTrue(ExtractionIntentResolver.wantsToStart(
				true, true, false, false, 0
		));
		assertTrue(ExtractionIntentResolver.wantsToStart(
				true, false, true, false, 0
		));
		assertFalse(ExtractionIntentResolver.wantsToStart(
				false, true, true, false, 0
		));
	}

	@Test
	public void movementReloadAndDamageInterruptActiveCountdown() {
		assertEquals(
				ExtractionState.Interaction.MOVED,
				ExtractionIntentResolver.resolve(
						true, true, true, false, false, 0
				)
		);
		assertEquals(
				ExtractionState.Interaction.RELOADED,
				ExtractionIntentResolver.resolve(
						true, true, true, true, true, 0
				)
		);
		assertEquals(
				ExtractionState.Interaction.HEAVY_HIT,
				ExtractionIntentResolver.resolve(
						true, true, true, true, false, 1
				)
		);
	}

	@Test
	public void stationaryActiveZoneAdvancesWithoutHeldKey() {
		assertEquals(
				ExtractionState.Interaction.ACTIVE,
				ExtractionIntentResolver.resolve(
						true, true, false, true, false, 0
				)
		);
		assertEquals(
				ExtractionState.Interaction.MOVED,
				ExtractionIntentResolver.resolve(
						true, false, false, true, false, 0
				)
		);
	}

	@Test
	public void adjacentDiscoveryNeverStartsOrContinuesRemoteExtraction() {
		assertFalse(ExtractionIntentResolver.wantsToStart(
				false, true, true, false, 0));
		assertEquals(
				ExtractionState.Interaction.MOVED,
				ExtractionIntentResolver.resolve(
						true, false, true, true, false, 0));
	}
}
