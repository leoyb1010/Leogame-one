package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WndBukovBackpackLayoutTest {

	@Test
	public void portraitLayoutFitsIPhoneViewport() {
		assertEquals(
				101,
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
				36,
				WndBukovBackpack.inventoryViewportHeight(152, true));
		assertEquals(
				226,
				WndBukovBackpack.windowWidthFor(240, true));
		assertEquals(
				152,
				WndBukovBackpack.windowHeightFor(160, true));
		assertTrue(WndBukovBackpack.fitsViewport(240, 160, true));
	}

	@Test
	public void wrappedPortraitHeaderAndLongWeaponDetailNeverOverlapActions() {
		WndBukovBackpack.LayoutMetrics layout =
				WndBukovBackpack.layoutFor(
						127,
						217,
						false,
						35,
						20,
						64);

		assertEquals(59, layout.headerHeight);
		assertEquals(72, layout.listHeight);
		assertEquals(32, layout.detailHeight);
		assertEquals(86, layout.footerHeight);
		assertEquals(
				217,
				layout.headerHeight
						+ layout.listHeight
						+ layout.footerHeight);
	}

	@Test
	public void landscapeKeepsInventoryAndCloseActionVisibleWithLargeCopy() {
		WndBukovBackpack.LayoutMetrics layout =
				WndBukovBackpack.layoutFor(
						226,
						152,
						true,
						18,
						10,
						54);

		assertEquals(33, layout.headerHeight);
		assertEquals(40, layout.listHeight);
		assertEquals(25, layout.detailHeight);
		assertEquals(79, layout.footerHeight);
		assertEquals(
				152,
				layout.headerHeight
						+ layout.listHeight
						+ layout.footerHeight);
	}

	@Test
	public void localizedHeaderOnlySharesARowWhenMeasuredCopyFits() {
		assertFalse(WndBukovBackpack.headerFitsInline(
				127, 40f, 80f));
		assertTrue(WndBukovBackpack.headerFitsInline(
				226, 40f, 80f));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsInvalidMeasuredLayoutInput() {
		WndBukovBackpack.layoutFor(
				127,
				217,
				false,
				35,
				-1,
				64);
	}
}
