package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BukovFocusRepeaterTest {

	@Test
	public void heldStickStepsImmediatelyThenWaitsThreeHundredMilliseconds() {
		BukovFocusRepeater repeat = new BukovFocusRepeater();

		assertEquals(1, repeat.update(0f, 0.8f, 0f));
		assertEquals(0, repeat.update(0f, 0.8f, 0.20f));
		assertEquals(1, repeat.update(0f, 0.8f, 0.10f));
		assertEquals(0, repeat.update(0f, 0.8f, 0.05f));
		assertEquals(1, repeat.update(0f, 0.8f, 0.07f));
	}

	@Test
	public void releaseAndDirectionChangeResetRepeatState() {
		BukovFocusRepeater repeat = new BukovFocusRepeater();

		assertEquals(-1, repeat.update(-0.8f, 0f, 0f));
		assertEquals(0, repeat.update(0f, 0f, 0.1f));
		assertEquals(1, repeat.update(0.8f, 0f, 0f));
		assertEquals(-1, repeat.update(-0.8f, 0f, 0f));
	}

	@Test
	public void driftInsideThresholdDoesNotMoveFocus() {
		BukovFocusRepeater repeat = new BukovFocusRepeater();
		assertEquals(0, repeat.update(0.3f, -0.4f, 1f));
	}
}
