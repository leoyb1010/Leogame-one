package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Static guard for the complete hideout vendor interaction surface. */
public class BukovVendorUiBoundaryGuardTest {

	@Test
	public void hubExposesCashAndDedicatedVendorEntry() throws Exception {
		String hub = source("WndBukovHub.java");

		assertTrue(hub.contains("bukov.economy.hub.cash"));
		assertTrue(hub.contains("bukov.economy.hub.vendor"));
		assertTrue(hub.contains("new WndBukovVendor("));
		assertTrue(hub.contains("ACTION_VENDOR"));
		assertTrue(hub.contains("int restoredFocus = focus.index()"));
		assertTrue(hub.contains("restoredFocus"));
	}

	@Test
	public void vendorHasAtomicBuySellRefreshAndFailureFeedback()
			throws Exception {
		String vendor = source("WndBukovVendor.java");

		assertTrue(vendor.contains("controller.buy("));
		assertTrue(vendor.contains("controller.sell("));
		assertTrue(vendor.contains("pendingTransactionId"));
		assertTrue(vendor.contains("nextTransactionId("));
		assertTrue(vendor.contains("new WndBukovVendor("));
		assertTrue(vendor.contains("new WndMessage("));
		assertTrue(vendor.contains("BukovNavigation.previous(event)"));
		assertTrue(vendor.contains("BukovNavigation.confirm(event)"));
		assertTrue(vendor.contains("ControllerHandler.leftStickPosition"));
		assertFalse(vendor.contains("RedButton"));
		assertFalse(vendor.contains("Chrome"));
	}

	@Test
	public void vendorViewportNeverExposesAPartialTextRow() {
		assertEquals(
				95,
				WndBukovVendor.completeRowViewportHeight(
						174,
						40,
						34));
		assertEquals(
				133,
				WndBukovVendor.completeRowViewportHeight(
						218,
						40,
						34));
	}

	private static String source(String file) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/bukov/ui/"
								+ file)),
				StandardCharsets.UTF_8);
	}
}
