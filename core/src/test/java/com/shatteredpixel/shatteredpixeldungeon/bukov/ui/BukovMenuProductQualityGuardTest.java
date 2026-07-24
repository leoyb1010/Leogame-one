package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Guards the authored Bukov menu hierarchy from regressing to unframed text
 * lists or inherited dungeon chrome.
 */
public class BukovMenuProductQualityGuardTest {

	@Test
	public void coreWindowsUseOpaqueTokenSurfacesAndVisibleHierarchy()
			throws Exception {
		for (String file : new String[] {
				"WndBukovHub.java",
				"WndBukovPause.java",
				"WndBukovSettings.java",
				"WndBukovBackpack.java",
				"WndBukovVendor.java"
		}) {
			String source = source(file);
			assertTrue(file, source.contains("BukovUiTokens"));
			assertTrue(
					file,
					source.contains("\"ink.background\", 255"));
			assertTrue(file, source.contains("ColorBlock"));
			assertFalse(file, source.contains("RedButton"));
		}
	}

	@Test
	public void settingsPresentNamesAndValuesAsSeparateColumns()
			throws Exception {
		String source = source("WndBukovSettings.java");

		assertTrue(source.contains("RenderedTextBlock label"));
		assertTrue(source.contains("RenderedTextBlock value"));
		assertTrue(source.contains("ColorBlock valueSurface"));
		assertTrue(source.contains("entryMessage(\"settings.saved\")"));
		assertTrue(source.contains(
				"setCopy(entryMessage(\"settings.performance\")"));
	}

	@Test
	public void pauseAndBackpackExposePrimaryAndDestructiveStates()
			throws Exception {
		String pause = source("WndBukovPause.java");
		String backpack = source("WndBukovBackpack.java");

		assertTrue(pause.contains(
				"BukovMessages.get(\"bukov.raid.pause.resume_code\")"));
		assertTrue(pause.contains(
				"BukovMessages.get(\"bukov.raid.pause.leave_code\")"));
		assertTrue(pause.contains("\"accent.danger\""));
		assertTrue(pause.contains("BukovWindowLayout.safeWidth"));
		assertTrue(backpack.contains("detailSurface"));
		assertTrue(backpack.contains("stateSurface"));
		assertTrue(backpack.contains("action == Action.CLOSE"));
	}

	@Test
	public void hubAndVendorRowsHaveSelectionFillAndFocusRail()
			throws Exception {
		String hub = source("WndBukovHub.java");
		String vendor = source("WndBukovVendor.java");

		assertTrue(hub.contains("selectedSurface"));
		assertTrue(hub.contains("divider.size(width - 8, 1)"));
		assertTrue(vendor.contains("selection.size(width, height)"));
		assertTrue(vendor.contains("focusEdge.size(width, 2)"));
		assertTrue(vendor.contains("bukov.economy.vendor.balance"));
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
