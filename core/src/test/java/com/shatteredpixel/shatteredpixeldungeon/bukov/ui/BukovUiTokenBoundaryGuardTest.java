package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Keeps major Bukov interaction surfaces on the shared tactical palette. */
public class BukovUiTokenBoundaryGuardTest {

	private static final String[] SURFACES = {
			"WndBukovSettings.java",
			"WndBukovPause.java",
			"WndBukovBackpack.java",
			"BukovTouchControls.java"
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

	private static String source(String file) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/bukov/ui/"
								+ file)),
				StandardCharsets.UTF_8);
	}
}
