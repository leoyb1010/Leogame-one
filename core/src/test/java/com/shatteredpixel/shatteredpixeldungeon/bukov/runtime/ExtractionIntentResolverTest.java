package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.ExtractionState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExtractionIntentResolverTest {

	@Test
	public void heldInteractionAndStationaryPositionAreBothRequiredToStart() {
		assertTrue(ExtractionIntentResolver.wantsToStart(
				true, true, true, false, 0
		));
		assertFalse(ExtractionIntentResolver.wantsToStart(
				true, false, true, false, 0
		));
		assertFalse(ExtractionIntentResolver.wantsToStart(
				true, true, false, false, 0
		));
		assertFalse(ExtractionIntentResolver.wantsToStart(
				false, true, true, false, 0
		));
	}

	@Test
	public void movementReloadAndHeavyDamageInterruptActiveCountdown() {
		assertEquals(
				ExtractionState.Interaction.MOVED,
				ExtractionIntentResolver.resolve(
						true, true, true, false, false, 0, 100
				)
		);
		assertEquals(
				ExtractionState.Interaction.RELOADED,
				ExtractionIntentResolver.resolve(
						true, true, true, true, true, 0, 100
				)
		);
		assertEquals(
				ExtractionState.Interaction.HEAVY_HIT,
				ExtractionIntentResolver.resolve(
						true, true, true, true, false, 18, 100
				)
		);
	}

	@Test
	public void activeCountdownRequiresHeldInteraction() {
		assertEquals(
				ExtractionState.Interaction.ACTIVE,
				ExtractionIntentResolver.resolve(
						true, true, true, true, false, 0, 100
				)
		);
		assertEquals(
				ExtractionState.Interaction.MOVED,
				ExtractionIntentResolver.resolve(
						true, true, false, true, false, 0, 100
				)
		);
		assertEquals(
				ExtractionState.Interaction.MOVED,
				ExtractionIntentResolver.resolve(
						true, false, true, true, false, 0, 100
				)
		);
	}

	@Test
	public void lightDamageRegressesButDoesNotCancelCountdown() {
		assertEquals(
				ExtractionState.Interaction.LIGHT_HIT,
				ExtractionIntentResolver.resolve(
						true, true, true, true, false, 17, 100
				)
		);
		assertEquals(
				ExtractionState.Interaction.LIGHT_HIT,
				ExtractionIntentResolver.damageInteraction(1, 100)
		);
		assertEquals(
				ExtractionState.Interaction.HEAVY_HIT,
				ExtractionIntentResolver.damageInteraction(1, 0)
		);
		assertEquals(
				ExtractionState.Interaction.NONE,
				ExtractionIntentResolver.damageInteraction(0, 100)
		);
	}

	@Test
	public void adjacentDiscoveryNeverStartsOrContinuesRemoteExtraction() {
		assertFalse(ExtractionIntentResolver.wantsToStart(
				false, true, true, false, 0));
		assertEquals(
				ExtractionState.Interaction.MOVED,
				ExtractionIntentResolver.resolve(
						true, false, true, true, false, 0, 100));
	}
}
