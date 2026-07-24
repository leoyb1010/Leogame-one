package com.shatteredpixel.shatteredpixeldungeon.messages;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovEntryLocalizationWiringTest {

	private static final Path JAVA =
			Paths.get("src/main/java/com/shatteredpixel/shatteredpixeldungeon");

	@Test
	public void ownedEntrySurfacesUseTheBukovEntryBundle() throws IOException {
		assertUsesEntryBundle("scenes/TitleScene.java");
		assertUsesEntryBundle("scenes/WelcomeScene.java");
		assertUsesEntryBundle("scenes/BukovHubScene.java");
		assertUsesEntryBundle("scenes/BukovDeploymentScene.java");
		assertUsesEntryBundle("bukov/ui/WndBukovSettings.java");
		assertUsesEntryBundle(
				"bukov/ui/WndBukovFirstRunCalibration.java");
	}

	@Test
	public void languageSwitchPersistsBeforeSetupAndSafeSceneRefresh()
			throws IOException {
		String source = source("bukov/ui/WndBukovSettings.java");
		assertTrue(source.contains("LANGUAGE,"));
		assertTrue(source.contains("Languages.CHI_SMPL"));
		assertTrue(source.contains("Languages.ENGLISH"));
		assertTrue(source.contains("Game.platform.resetGenerators()"));

		int persist = source.indexOf("SPDSettings.language(language);");
		int setup = source.indexOf("Messages.setup(language);", persist);
		int refresh = source.indexOf(
				"ShatteredPixelDungeon.seamlessResetScene(", setup);
		assertTrue("language preference must be persisted", persist >= 0);
		assertTrue("message bundle setup must follow persistence",
				setup > persist);
		assertTrue("safe scene refresh must follow message setup",
				refresh > setup);
	}

	@Test
	public void languageSwitcherOnlyCyclesEnglishAndSimplifiedChinese()
			throws IOException {
		String source = source("bukov/ui/WndBukovSettings.java");
		int helper = source.indexOf(
				"static Languages nextLanguage(Languages current)");
		assertTrue(helper >= 0);
		String body = source.substring(
				helper,
				Math.min(source.length(), helper + 260));
		assertTrue(body.contains("Languages.ENGLISH"));
		assertTrue(body.contains("Languages.CHI_SMPL"));
		assertFalse(body.contains("Languages.CHI_TRAD"));
		assertFalse(body.contains("Languages.JAPANESE"));
	}

	@Test
	public void entryFlowCannotRouteToLegacyBrandOrCampaignSurfaces()
			throws IOException {
		String[] entrySurfaces = {
				"scenes/TitleScene.java",
				"scenes/WelcomeScene.java",
				"scenes/BukovHubScene.java",
				"scenes/BukovDeploymentScene.java",
				"bukov/ui/WndBukovSettings.java",
				"bukov/ui/WndBukovFirstRunCalibration.java"
		};
		String[] forbidden = {
				"HeroSelectScene",
				"StartScene",
				"ChangesScene",
				"AboutScene",
				"RankingsScene",
				"WndSupporter",
				"WndChallenges",
				"WndClass",
				"Messages.get(this, \"update\"",
				"Messages.get(this, \"changelog\"",
				"\"Shattered Pixel Dungeon\""
		};
		for (String relative : entrySurfaces) {
			String source = source(relative);
			for (String marker : forbidden) {
				assertFalse(relative + " exposes legacy marker " + marker,
						source.contains(marker));
			}
		}
	}

	@Test
	public void welcomeWarningsAndControllerHintStayInsideProductBundle()
			throws IOException {
		String welcome = source("scenes/WelcomeScene.java");
		assertTrue(welcome.contains("welcome.error_title"));
		assertTrue(welcome.contains("welcome.save_warning"));
		assertTrue(welcome.contains("welcome.controller_title"));
		assertTrue(welcome.contains("welcome.controller_body"));
		assertTrue(welcome.contains("welcome.controller_okay"));
		assertFalse(welcome.contains("Messages.get(WelcomeScene.class"));
		assertFalse(welcome.contains("Messages.get(WndError.class"));
	}

	private static void assertUsesEntryBundle(String relative)
			throws IOException {
		String source = source(relative);
		assertTrue(relative + " must use the Bukov entry bundle",
				source.contains(
						"BukovMessages.get(\"bukov.entry.\" + key, args)"));
	}

	private static String source(String relative) throws IOException {
		return new String(
				Files.readAllBytes(JAVA.resolve(relative)),
				StandardCharsets.UTF_8);
	}
}
