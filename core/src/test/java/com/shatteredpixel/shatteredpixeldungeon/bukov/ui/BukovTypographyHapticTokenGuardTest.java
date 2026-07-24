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

/** Prevents typography and haptic presentation data from splitting again. */
public class BukovTypographyHapticTokenGuardTest {

	private static final Path UI = Paths.get(
			"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui");
	private static final Path SCENES = Paths.get(
			"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes");
	private static final String[] PLAYER_SCENES = {
			"TitleScene.java",
			"WelcomeScene.java",
			"BukovHubScene.java",
			"BukovDeploymentScene.java"
	};

	@Test
	public void playerVisibleBukovUiHasNoLiteralRenderedFontSizes()
			throws Exception {
		try (Stream<Path> paths = Files.walk(UI)) {
			for (Path path : (Iterable<Path>)paths
					.filter(value -> value.toString().endsWith(".java"))
					::iterator) {
				assertNoLiteralRenderedFontSize(path);
			}
		}
		for (String scene : PLAYER_SCENES) {
			assertNoLiteralRenderedFontSize(SCENES.resolve(scene));
		}
	}

	@Test
	public void hapticEnvelopeHasOneAuthoredSource() throws Exception {
		String experience = source(Paths.get(
				"src/main/assets/bukov/content/experience_contract.json"));
		String tokens = source(Paths.get(
				"src/main/assets/bukov/content/ui_tokens.json"));
		String registry = source(Paths.get(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/settings/ExperienceContractRegistry.java"));

		assertFalse(experience.contains("\"shakeAmplitudePx\""));
		assertFalse(experience.contains("\"vibrationAmplitude\""));
		assertFalse(experience.contains("\"maximumShakePx\""));
		assertTrue(tokens.contains("\"typographyPx\""));
		assertTrue(tokens.contains("\"hapticMaximumShakePx\""));
		assertTrue(tokens.contains("\"haptics\""));
		assertTrue(registry.contains("uiTokens.haptic(type.name())"));
	}

	@Test
	public void scaledLayoutsDoNotDoubleScaleDenseDeviceGlyphs()
			throws Exception {
		String tokens = source(UI.resolve("BukovUiTokens.java"));
		int helper = tokens.indexOf(
				"public int scaledTypographyPx(String token)");
		int nextMethod = tokens.indexOf(
				"public float maximumShakePx()", helper);
		String body = tokens.substring(helper, nextMethod);

		assertTrue(body.contains("return typographyPx(token);"));
		assertFalse(body.contains("SPDSettings.bukovUiScale()"));
		assertFalse(body.contains("BukovUiScale.fontPixels("));
	}

	private static void assertNoLiteralRenderedFontSize(Path path)
			throws Exception {
		String source = source(path);
		Pattern singleArgument = Pattern.compile(
				"renderTextBlock\\s*\\(\\s*\\d+\\s*\\)");
		Pattern textAndSize = Pattern.compile(
				"renderTextBlock\\s*\\([^;]{0,240}?,\\s*\\d+\\s*\\)",
				Pattern.DOTALL);
		assertFalse(path.getFileName() + " uses a literal font size",
				singleArgument.matcher(source).find());
		assertFalse(path.getFileName() + " uses a literal font size",
				textAndSize.matcher(source).find());
		if (source.contains("private RenderedTextBlock text(")
				|| source.contains("private RenderedTextBlock label(")) {
			assertTrue(
					path.getFileName()
							+ " helper bypasses typography tokens",
					source.contains("tokens.typographyPx(typography)")
							|| source.contains(
									"tokens.scaledTypographyPx(typography)"));
		}
	}

	private static String source(Path path) throws Exception {
		return new String(
				Files.readAllBytes(path),
				StandardCharsets.UTF_8);
	}
}
