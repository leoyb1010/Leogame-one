package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovFinishedVisualWiringTest {

	@Test
	public void titleHubAndDeploymentShareTokensAndSafeLayoutContract()
			throws Exception {
		String title = read("scenes/TitleScene.java");
		String hub = read("scenes/BukovHubScene.java");
		String deployment = read("scenes/BukovDeploymentScene.java");
		for (String source : new String[]{title, hub, deployment}) {
			assertTrue(source.contains("BukovUiTokens"));
			assertTrue(source.contains("BukovVisualContract"));
			assertTrue(source.contains("getCommonInsets()"));
			assertFalse(source.contains("RenderedTextBlock.zoom"));
			assertFalse(source.contains(".zoom("));
		}
		assertTrue(title.contains("controlHeight(touch)"));
		assertTrue(hub.contains("controlHeight("));
		assertTrue(deployment.contains(
				"entryMessage(\"deployment.eyebrow\")"));
	}

	@Test
	public void hudUsesSafeFontSizesAndExposesBackpackEntry()
			throws Exception {
		String hud = readBukovUi("BukovRaidHud.java");
		assertTrue(hud.contains("tokens.typographyPx(typography)"));
		assertTrue(hud.contains(
				"\"bukov.raid.hud.control_hint_desktop\""));
		assertTrue(hud.contains(
				"\"bukov.raid.hud.control_hint_touch\""));
		assertFalse(hud.contains(".zoom("));
	}

	private static String read(String scene) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/" + scene)),
				StandardCharsets.UTF_8);
	}

	private static String readBukovUi(String file) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/bukov/ui/" + file)),
				StandardCharsets.UTF_8);
	}
}
