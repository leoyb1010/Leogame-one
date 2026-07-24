package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Keeps the full-screen hideout on the explicit five-mode selector. The old
 * two-card surface silently cycled formal modes and made three modes look
 * hidden or random.
 */
public class BukovHubRaidModeEntryGuardTest {

	@Test
	public void deploymentPanelOpensExplicitFiveModeSelector() throws Exception {
		String source = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/scenes/"
								+ "BukovHubScene.java")),
				StandardCharsets.UTF_8);
		String panel = between(
				source,
				"private void buildDeploymentPanel(",
				"private void buildActiveRaidPanel(");

		assertTrue(panel.contains("controller.selectedRaidMode()"));
		assertTrue(panel.contains("点击查看全部5种模式"));
		assertTrue(panel.contains("openRaidModeSelection();"));
		assertFalse(panel.contains("controller.cycleFormalRaidMode()"));
		assertFalse(panel.contains("controller.selectTrainingGround()"));

		String selector = between(
				source,
				"private void openRaidModeSelection()",
				"private void openVendor()");
		assertTrue(selector.contains("new WndBukovRaidModeSelection("));
		assertTrue(selector.contains("reload();"));
	}

	private static String between(String source, String start, String end) {
		int from = source.indexOf(start);
		int to = source.indexOf(end, from);
		if (from < 0 || to < 0) {
			throw new AssertionError("Source boundary not found");
		}
		return source.substring(from, to);
	}
}
