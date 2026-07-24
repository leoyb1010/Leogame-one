package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovRaidModeSelectionWiringGuardTest {

	@Test
	public void hubOpensExplicitSelectorAndRestoresExactFocus()
			throws Exception {
		String hub = source("WndBukovHub.java");

		assertTrue(hub.contains("new WndBukovRaidModeSelection("));
		assertTrue(hub.contains("focus.focus(inventoryItems.size())"));
		assertTrue(hub.contains("int restoredFocus = focus.index()"));
		assertTrue(hub.contains("restoredFocus,"));
		assertTrue(hub.contains(
				"\"bukov.economy.hub.mode_select\""));
		assertFalse(hub.contains("ModeCycleButton"));
		assertFalse(hub.contains("controller.cycleRaidMode()"));
	}

	@Test
	public void selectorAppliesModeButNeverDeploysPlayer()
			throws Exception {
		String selector = source("WndBukovRaidModeSelection.java");

		assertEquals(1, occurrences(
				selector,
				"controller.selectRaidMode("));
		assertFalse(selector.contains("controller.confirmDeployment("));
		assertFalse(selector.contains("deploy.call()"));
		assertTrue(selector.contains("focus.draftMode()"));
		assertTrue(selector.contains("focus.applyEnabled()"));
		assertTrue(selector.contains("viewModel.locked"));
		assertTrue(selector.contains(
				"\"bukov.economy.mode.badge_current_locked\""));
	}

	@Test
	public void selectorConsumesNavigationOnceAndGuardsReentry()
			throws Exception {
		String selector = source("WndBukovRaidModeSelection.java");

		assertTrue(selector.contains(
				"private final BukovRaidModeFocusModel focus"));
		assertTrue(selector.contains("BukovFocusRepeater"));
		assertTrue(selector.contains(
				"if (!event.pressed || closing || committing)"));
		assertTrue(selector.contains(
				"if (committing || closing)"));
		assertTrue(selector.contains("BukovNavigation.back(event)"));
		assertTrue(selector.contains("BukovNavigation.confirm(event)"));
		assertTrue(selector.contains("return true;"));
		assertEquals(1, occurrences(selector, "close.call();"));
	}

	private static int occurrences(String source, String value) {
		int count = 0;
		int offset = 0;
		while ((offset = source.indexOf(value, offset)) >= 0) {
			count++;
			offset += value.length();
		}
		return count;
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
