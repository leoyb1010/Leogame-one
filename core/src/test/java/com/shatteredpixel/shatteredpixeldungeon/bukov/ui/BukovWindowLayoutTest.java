package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BukovWindowLayoutTest {

	@Test
	public void portraitWindowsFitMinimumViewportAndSafeArea() {
		int hubHeight =
				WndBukovHub.windowHeightFor(225, 4f, 8f, false);
		int vendorHeight =
				WndBukovVendor.windowHeightFor(225, 4f, 8f, false);
		int settingsHeight =
				WndBukovSettings.windowHeightFor(225, 4f, 8f, false);

		assertEquals(205, hubHeight);
		assertEquals(205, vendorHeight);
		assertEquals(205, settingsHeight);
		assertTrue(WndBukovHub.fitsViewport(
				225, 4f, 8f, false));
		assertTrue(WndBukovHub.inventoryViewportHeight(
				hubHeight, false) > 0);
		int compactWidth =
				BukovWindowLayout.fit(135, 0f, 0f, 150);
		assertEquals(127, compactWidth);
		assertTrue(BukovWindowLayout.fits(
				135, 0f, 0f, compactWidth));
	}

	@Test
	public void fittingNeverExceedsDesiredSizeOnLargeViewport() {
		assertEquals(
				226,
				WndBukovHub.windowHeightFor(
						300, 0f, 0f, false));
		assertEquals(
				174,
				WndBukovVendor.windowHeightFor(
						220, 0f, 0f, true));
		assertEquals(
				166,
				WndBukovSettings.windowHeightFor(
						220, 0f, 0f, true));
	}
}
