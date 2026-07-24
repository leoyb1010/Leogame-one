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
	public void playerUiClassesExceptRenderersAndTokenParserAvoidRgbLiterals()
			throws Exception {
		Pattern literalColor = Pattern.compile("0x[0-9A-Fa-f]{6,8}");
		Path directory = Paths.get(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/bukov/ui");
		try (Stream<Path> paths = Files.walk(directory)) {
			for (Path path : (Iterable<Path>) paths
					.filter(value -> value.toString().endsWith(".java"))
					.filter(value -> !value.getFileName().toString()
							.equals("BukovRaidHud.java"))
					// Combat direction arc is an FX renderer owned by the
					// presentation pipeline, not a player UI surface.
					.filter(value -> !value.getFileName().toString()
							.equals("BukovHitDirectionArc.java"))
					.filter(value -> !value.getFileName().toString()
							.equals("BukovUiTokens.java"))::iterator) {
				String source = new String(
						Files.readAllBytes(path),
						StandardCharsets.UTF_8);
				assertFalse(
						path.getFileName().toString(),
						literalColor.matcher(source).find());
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
}
