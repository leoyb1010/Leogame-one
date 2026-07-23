package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WndBukovBackpackLayoutTest {

	@Test
	public void portraitLayoutFitsIPhoneViewport() {
		assertEquals(
				109,
				WndBukovBackpack.inventoryViewportHeight(217, false));
		assertEquals(
				127,
				WndBukovBackpack.windowWidthFor(135, false));
		assertEquals(
				217,
				WndBukovBackpack.windowHeightFor(225, false));
		assertTrue(WndBukovBackpack.fitsViewport(135, 225, false));
	}

	@Test
	public void landscapeLayoutFitsCompactViewport() {
		assertEquals(
				44,
				WndBukovBackpack.inventoryViewportHeight(152, true));
		assertEquals(
				226,
				WndBukovBackpack.windowWidthFor(240, true));
		assertEquals(
				152,
				WndBukovBackpack.windowHeightFor(160, true));
		assertTrue(WndBukovBackpack.fitsViewport(240, 160, true));
	}
}
