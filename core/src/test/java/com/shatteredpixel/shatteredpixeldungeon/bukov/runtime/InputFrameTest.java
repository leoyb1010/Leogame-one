package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InputFrameTest {

	@Test
	public void clearEdgesKeepsHeldStateButConsumesOneShotActions() {
		InputFrame frame = new InputFrame();
		frame.fireHeld = true;
		frame.interactHeld = true;
		frame.firePressed = true;
		frame.reloadPressed = true;
		frame.interactPressed = true;
		frame.dropPressed = true;

		frame.clearEdges();

		assertTrue(frame.fireHeld);
		assertTrue(frame.interactHeld);
		assertFalse(frame.firePressed);
		assertFalse(frame.reloadPressed);
		assertFalse(frame.interactPressed);
		assertFalse(frame.dropPressed);
	}
}
