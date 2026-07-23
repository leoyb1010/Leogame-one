package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BukovAtmosphereControllerTest {

	@Test
	public void calmTenseCombatCrossfadeAndEightSecondReleaseAreDeterministic() {
		BukovAtmosphereController controller =
				new BukovAtmosphereController();
		BukovAtmosphereSignal signal = new BukovAtmosphereSignal();

		signal.set(true, false);
		controller.update(1.5f, signal);
		assertEquals(
				BukovAtmosphereController.State.TENSE,
				controller.target());
		assertEquals(
				1f,
				controller.gain(BukovAtmosphereController.State.TENSE),
				0f);

		signal.set(false, true);
		controller.update(1.5f, signal);
		assertEquals(
				BukovAtmosphereController.State.COMBAT,
				controller.target());
		assertEquals(1f, controller.combatBlend(), 0f);

		signal.set(false, false);
		controller.update(7.9f, signal);
		assertEquals(
				BukovAtmosphereController.State.COMBAT,
				controller.target());
		controller.update(0.2f, signal);
		assertEquals(
				BukovAtmosphereController.State.CALM,
				controller.target());

		float total = 0f;
		for (BukovAtmosphereController.State state
				: BukovAtmosphereController.State.values()) {
			total += controller.gain(state);
		}
		assertEquals(1f, total, 0.0001f);
		assertTrue(controller.combatBlend() < 1f);
	}
}
