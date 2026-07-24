package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovIconLabelButtonTest {

	@Test
	public void recommendedHeightUsesSharedTouchContract() {
		assertEquals(
				BukovVisualContract.controlHeight(true),
				BukovIconLabelButton.recommendedHeight(true),
				0f);
		assertEquals(
				BukovVisualContract.controlHeight(false),
				BukovIconLabelButton.recommendedHeight(false),
				0f);
		assertTrue(
				BukovIconLabelButton.recommendedHeight(true)
						> BukovIconLabelButton.recommendedHeight(false));
	}

	@Test
	public void componentReusesAtlasAndOwnsAllRequiredStates()
			throws Exception {
		String source = source();

		assertTrue(source.contains("extends Button"));
		assertTrue(source.contains("BukovTouchIcon.Glyph"));
		assertTrue(source.contains("new BukovTouchIcon("));
		assertTrue(source.contains("BukovUiAssets.Surface.BUTTON,"));
		assertTrue(source.contains(
				"BukovUiAssets.Surface.BUTTON_PRESSED"));
		assertTrue(source.contains(
				"BukovUiAssets.Surface.BUTTON_FOCUSED"));
		assertTrue(source.contains(
				"BukovUiAssets.Surface.BUTTON_DISABLED"));
		assertTrue(source.contains("setFocused(boolean focused)"));
		assertTrue(source.contains("public void enable(boolean enabled)"));
		assertTrue(source.contains("public void iconOnly(boolean iconOnly)"));
		assertTrue(source.contains("super.createChildren();"));
		assertFalse(source.contains("Icons.get("));
		assertFalse(source.contains("new Texture("));
	}

	@Test
	public void layoutKeepsIconLeftAndShortLabelRight()
			throws Exception {
		String source = source();

		int iconLeft = source.indexOf(
				"float iconLeft = x + HORIZONTAL_PADDING;");
		int textLeft = source.indexOf(
				"iconLeft + iconSize + ICON_LABEL_GAP;");
		assertTrue(iconLeft >= 0);
		assertTrue(textLeft > iconLeft);
		assertTrue(source.contains("BukovVisualContract.FONT_CAPTION"));
	}

	@Test
	public void textOnlyCompatibilityAndAccessibleIconOnlyModeRemain()
			throws Exception {
		String source = source();

		assertTrue(source.contains(
				"public BukovIconLabelButton(String label, boolean touch)"));
		assertTrue(source.contains("public void text(String value)"));
		assertTrue(source.contains("public String text()"));
		assertTrue(source.contains(
				"return iconOnly && labelText.length() > 0"));
		assertTrue(source.contains(
				"Math.max(requestedHeight, recommendedHeight())"));
		assertTrue(source.contains(
				"public void contentRightInset(float inset)"));
		assertTrue(source.contains(
				"- trailingInset - textLeft"));
	}

	private static String source() throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/bukov/ui/"
								+ "BukovIconLabelButton.java")),
				StandardCharsets.UTF_8);
	}
}
