package com.shatteredpixel.shatteredpixeldungeon.desktop;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DesktopSaveVendorResolutionTest {

	@Test
	public void localizedProductTitleUsesBukovVendor() {
		assertEquals(
				"leoyuan",
				DesktopLauncher.saveVendor("逃离布科夫"));
	}

	@Test
	public void missingProductTitleUsesBukovVendor() {
		assertEquals("leoyuan", DesktopLauncher.saveVendor(null));
		assertEquals("leoyuan", DesktopLauncher.saveVendor("  "));
	}

	@Test
	public void legacyPackageTitleKeepsItsExistingVendor() {
		assertEquals(
				"shatteredpixel",
				DesktopLauncher.saveVendor(
						"com.shatteredpixel.shatteredpixeldungeon"));
	}
}
