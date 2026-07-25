package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovReloadInterruptionTest {

	@Test
	public void idleAndReloadButtonAloneDoNotCancelReload() {
		InputFrame input = new InputFrame();
		assertFalse(BukovRealtimeWorld.reloadInterruptRequested(input));

		input.reloadPressed = true;
		assertFalse(BukovRealtimeWorld.reloadInterruptRequested(input));
	}

	@Test
	public void normalPlayerActionsCancelReload() {
		InputFrame input = new InputFrame();
		input.movement.x = 1f;
		assertFalse(BukovRealtimeWorld.reloadInterruptRequested(input));

		input = new InputFrame();
		input.firePressed = true;
		assertTrue(BukovRealtimeWorld.reloadInterruptRequested(input));

		input = new InputFrame();
		input.interactHeld = true;
		assertTrue(BukovRealtimeWorld.reloadInterruptRequested(input));

		input = new InputFrame();
		input.medicalPressed = true;
		assertTrue(BukovRealtimeWorld.reloadInterruptRequested(input));

		input = new InputFrame();
		input.backpackPressed = true;
		assertTrue(BukovRealtimeWorld.reloadInterruptRequested(input));
	}
}
