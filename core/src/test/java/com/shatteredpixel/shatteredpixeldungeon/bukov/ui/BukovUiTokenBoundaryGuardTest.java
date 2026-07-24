package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Keeps major Bukov interaction surfaces on the shared tactical palette. */
public class BukovUiTokenBoundaryGuardTest {

	private static final String[] SURFACES = {
			"BukovRaidHud.java",
			"WndBukovSettings.java",
			"WndBukovFirstRunCalibration.java",
			"WndBukovPause.java",
			"WndBukovBackpack.java",
			"BukovTouchControls.java"
	};

	private static final String[] PLAYER_SCENES = {
			"TitleScene.java",
			"WelcomeScene.java",
			"BukovHubScene.java",
			"BukovDeploymentScene.java"
	};

	private static final Path BUKOV_FX = Paths.get(
			"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
					+ "bukov/fx");
	private static final Path BUKOV_ENEMY_PRESENTATION = Paths.get(
			"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
					+ "sprites/bukov");
	private static final Path BUKOV_LEVEL = Paths.get(
			"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
					+ "bukov/levels/BukovLevel.java");

	@Test
	public void majorSurfacesUseTokensWithoutEmbeddedRgbColors()
			throws Exception {
		Pattern literalColor = Pattern.compile(
				"0x[0-9A-Fa-f]{6,8}");
		for (String file : SURFACES) {
			String source = source(file);
			assertTrue(file, source.contains("BukovUiTokens"));
			assertFalse(file, literalColor.matcher(source).find());
			if (file.startsWith("Wnd")) {
				assertTrue(
						file + " must use an opaque token window surface",
						source.contains("colorWithAlpha(")
								&& source.contains(
										"\"ink.background\", 255)"));
			}
		}
	}

	@Test
	public void translucentTouchColorsStillComeFromNamedTokens()
			throws Exception {
		String source = source("BukovTouchControls.java");
		String tokens = source("BukovUiTokens.java");

		assertTrue(source.contains("colorWithAlpha(\"panel.surface\""));
		assertTrue(source.contains("colorWithAlpha(\"text.secondary\""));
		assertTrue(tokens.contains("public int colorWithAlpha("));
	}

	@Test
	public void directionArcTexturesUseInjectableSolidToken()
			throws Exception {
		for (String file : new String[] {
				"BukovHitDirectionArc.java",
				"BukovSoundDirectionArc.java"
		}) {
			String source = source(file);
			assertTrue(
					file + " must accept the shared token contract",
					source.contains("BukovUiTokens tokens"));
			assertTrue(
					file + " must use the named solid texture token",
					source.contains(
							"tokens.colorWithAlpha("
									+ "\"combat.fx.solid\", 255)"));
			assertFalse(
					file + " must not embed an opaque white texture literal",
					source.contains("0xFFFFFFFF"));
		}
	}

	@Test
	public void playerUiClassesAvoidRgbLiteralsExceptTechnicalMasks()
			throws Exception {
		Pattern literalColor = Pattern.compile(
				"0x[0-9A-Fa-f]{6}(?:[0-9A-Fa-f]{2})?\\b");
		Path directory = Paths.get(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/bukov/ui");
		try (Stream<Path> paths = Files.walk(directory)) {
			for (Path path : (Iterable<Path>) paths
					.filter(value -> value.toString().endsWith(".java"))
					::iterator) {
				String[] lines = new String(
						Files.readAllBytes(path),
						StandardCharsets.UTF_8).split("\\R");
				for (int lineNumber = 0;
						lineNumber < lines.length;
						lineNumber++) {
					java.util.regex.Matcher matcher =
							literalColor.matcher(lines[lineNumber]);
					while (matcher.find()) {
						assertTrue(
								path.getFileName() + ":"
										+ (lineNumber + 1)
										+ " embeds RGB outside ui_tokens.json",
								isTechnicalMask(
										path.getFileName().toString(),
										lines[lineNumber],
										matcher.group()));
					}
				}
			}
		}
	}

	@Test
	public void playerReachableBukovScenesUseTheSameTokenPalette()
			throws Exception {
		Pattern literalColor = Pattern.compile("0x[0-9A-Fa-f]{6,8}");
		for (String file : PLAYER_SCENES) {
			String source = sceneSource(file);
			assertTrue(file, source.contains("BukovUiTokens"));
			assertFalse(file, literalColor.matcher(source).find());
		}
	}

	@Test
	public void combatFxEnemyPresentationAndLevelAvoidEmbeddedRgb()
			throws Exception {
		assertDirectoryHasNoRgbLiterals(BUKOV_FX);
		assertDirectoryHasNoRgbLiterals(BUKOV_ENEMY_PRESENTATION);
		assertFileHasNoRgbLiterals(BUKOV_LEVEL);

		assertTrue(
				sourceAt(BUKOV_FX.resolve("BukovTracerFx.java"))
						.contains("BukovUiTokens"));
		assertTrue(
				sourceAt(BUKOV_FX.resolve("BukovExplosionFx.java"))
						.contains("BukovUiTokens"));
		assertTrue(
				sourceAt(BUKOV_ENEMY_PRESENTATION.resolve(
						"BukovEnemySprite.java"))
						.contains("BukovUiTokens"));
		assertTrue(sourceAt(BUKOV_LEVEL).contains("BukovUiTokens"));
	}

	private static String source(String file) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/bukov/ui/"
								+ file)),
				StandardCharsets.UTF_8);
	}

	private static String sceneSource(String file) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/scenes/"
								+ file)),
				StandardCharsets.UTF_8);
	}

	private static void assertDirectoryHasNoRgbLiterals(Path directory)
			throws Exception {
		try (Stream<Path> paths = Files.walk(directory)) {
			for (Path path : (Iterable<Path>) paths
					.filter(value -> value.toString().endsWith(".java"))
					::iterator) {
				assertFileHasNoRgbLiterals(path);
			}
		}
	}

	private static void assertFileHasNoRgbLiterals(Path path)
			throws Exception {
		Pattern literalColor = Pattern.compile(
				"0x[0-9A-Fa-f]{6}(?:[0-9A-Fa-f]{2})?\\b");
		assertFalse(
				path.getFileName() + " embeds RGB outside ui_tokens.json",
				literalColor.matcher(sourceAt(path)).find());
	}

	private static String sourceAt(Path path) throws Exception {
		return new String(
				Files.readAllBytes(path),
				StandardCharsets.UTF_8);
	}

	private static boolean isTechnicalMask(
			String file,
			String line,
			String literal) {
		if ("BukovUiTokens.java".equals(file)) {
			return "0xFFFFFF".equals(literal)
					&& line.contains("&");
		}
		return false;
	}
}
