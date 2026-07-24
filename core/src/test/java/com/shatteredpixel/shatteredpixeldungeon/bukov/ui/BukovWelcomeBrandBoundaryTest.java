package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Prevents the product entry path from regressing to inherited campaign
 * presentation while keeping required upstream attribution out of that path.
 */
public class BukovWelcomeBrandBoundaryTest {

	@Test
	public void welcomeUsesOnlyBukovPresentationAssets() throws Exception {
		String welcome = source(
				"scenes/WelcomeScene.java");

		assertTrue(welcome.contains(
				"TITLE_INDUSTRIAL_LANDSCAPE_V2"));
		assertTrue(welcome.contains(
				"TITLE_INDUSTRIAL_PORTRAIT_V2"));
		assertTrue(welcome.contains("BukovUiTokens.loadDefault()"));
		assertTrue(welcome.contains("BukovVisualContract"));
		assertTrue(welcome.contains("Assets.Sounds.Bukov.UI_CONFIRM"));
		assertTrue(welcome.contains("Music.INSTANCE.end()"));
		assertTrue(welcome.contains(
				"Messages.get(this, \"bukov_title\")"));
		assertTrue(welcome.contains(
				"Messages.get(this, \"bukov_english_title\")"));
		assertTrue(text("src/main/assets/messages/scenes/scenes_zh.properties")
				.contains("scenes.welcomescene.bukov_title=逃离布科夫"));
		assertTrue(text("src/main/assets/messages/scenes/scenes.properties")
				.contains("scenes.welcomescene.bukov_english_title=ESCAPE FROM BUKOV"));

		for (String inherited : new String[] {
				"TitleBackground",
				"Fireball",
				"Assets.Music.THEME_",
				"LeoIdentityConfig",
				"Chrome.",
				"StyledButton",
				"ChangesScene",
				"HeroSelectScene",
				"StartScene",
				"AboutScene",
				"Badges",
				"Rankings",
				"Journal",
				"selectedClass",
				"updateVersion(",
				"\"bukov_update\"",
				"\"bukov_future_save\""
		}) {
			assertFalse(inherited, welcome.contains(inherited));
		}
	}

	@Test
	public void titleCannotRestartHostPresentation() throws Exception {
		String title = source("scenes/TitleScene.java");

		assertTrue(title.contains(
				"TITLE_INDUSTRIAL_LANDSCAPE_V2"));
		assertTrue(title.contains(
				"TITLE_INDUSTRIAL_PORTRAIT_V2"));
		assertTrue(title.contains("Music.INSTANCE.end()"));
		assertTrue(title.contains("Assets.Sounds.Bukov.UI_CONFIRM"));
		assertFalse(title.contains("Assets.Sounds.CLICK"));
		assertFalse(title.contains("Assets.Music.THEME_"));
		assertFalse(title.contains("TitleBackground"));
		assertFalse(title.contains("Fireball"));
		assertFalse(title.contains("HeroSelectScene"));
		assertFalse(title.contains("ChangesScene"));
		assertFalse(title.contains("AboutScene"));
	}

	@Test
	public void attributionRemainsInLegalOnlySurfaces() throws Exception {
		String welcome = source("scenes/WelcomeScene.java");
		String title = source("scenes/TitleScene.java");
		String about = source("scenes/AboutScene.java");
		String notices = text(
				"src/main/assets/legal/THIRD_PARTY_NOTICES.txt");

		assertFalse(welcome.contains("\"Shattered Pixel Dungeon\""));
		assertFalse(title.contains("\"Shattered Pixel Dungeon\""));
		assertTrue(about.contains("\"Shattered Pixel Dungeon\""));
		assertTrue(about.contains("\"Pixel Dungeon\""));
		assertTrue(notices.contains("Shattered Pixel Dungeon"));
		assertTrue(notices.contains("Pixel Dungeon"));
		assertTrue(notices.contains(
				"Required\nupstream attribution remains"));
	}

	private static String source(String relative) throws Exception {
		return text(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/" + relative);
	}

	private static String text(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
