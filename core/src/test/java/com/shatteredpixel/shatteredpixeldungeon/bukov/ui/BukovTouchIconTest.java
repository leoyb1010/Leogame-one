package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BukovTouchIconTest {

	@Test
	public void everyTouchRoleOwnsADistinctAtlasGlyph() {
		Set<Integer> atlasColumns = new HashSet<>();
		for (BukovTouchIcon.Glyph glyph : BukovTouchIcon.Glyph.values()) {
			int column = BukovTouchIcon.atlasColumn(glyph);
			assertTrue(column >= 8 && column <= 23);
			assertTrue(atlasColumns.add(column));
		}
		assertEquals(16, atlasColumns.size());
	}

	@Test
	public void tokenRgbBecomesOpaqueBeforeCreatingIconTextures() {
		int textureColor = BukovTouchIcon.withFullAlpha(0x123456);

		assertEquals(255, textureColor >>> 24);
		assertEquals(0x123456, textureColor & 0xFFFFFF);
	}

	@Test
	public void sticksAndEveryActionAreWiredToSemanticGlyphs() {
		assertEquals(
				BukovTouchIcon.Glyph.MOVEMENT,
				BukovTouchControls.iconFor(
						BukovTouchState.Stick.MOVEMENT));
		assertEquals(
				BukovTouchIcon.Glyph.AIM_FIRE,
				BukovTouchControls.iconFor(
						BukovTouchState.Stick.AIM_FIRE));

		Set<BukovTouchIcon.Glyph> actionGlyphs = new HashSet<>();
		for (BukovTouchState.Action action
				: BukovTouchState.Action.values()) {
			BukovTouchIcon.Glyph glyph =
					BukovTouchControls.iconFor(action);
			assertNotNull(glyph);
			assertTrue(actionGlyphs.add(glyph));
		}
		assertEquals(
				BukovTouchState.Action.values().length,
				actionGlyphs.size());
	}

	@Test
	public void touchButtonsKeepFullHitRectsAndNonColourStateCues()
			throws Exception {
		String controls = source("BukovTouchControls.java");
		String icon = source("BukovTouchIcon.java");

		assertTrue(controls.contains("pointerArea.width = width;"));
		assertTrue(controls.contains("pointerArea.height = height;"));
		assertTrue(controls.contains("pointerArea.width = hitWidth;"));
		assertTrue(controls.contains("pointerArea.height = hitHeight;"));
		assertTrue(controls.contains("listener.onActionPressed(action);"));
		assertTrue(controls.contains(
				"if (inputBlocked || disabled || pointerId != -1)"));
		assertTrue(controls.contains("liveActionAvailability("));
		assertTrue(controls.contains("icon.visualState("));
		assertTrue(controls.contains("setDisabled(blocked)"));
		assertTrue(controls.contains("ACTION_ICON_HEIGHT_RATIO = 0.66f"));
		assertTrue(controls.contains("ACTION_LABEL_HEIGHT_PX = 5f"));
		assertTrue(controls.contains(
				"BukovUiAssets.Surface.BUTTON_PRESSED"));
		assertTrue(controls.contains(
				"BukovUiAssets.Surface.BUTTON_DISABLED"));
		assertTrue(controls.contains(
				"BukovUiAssets.Surface.PANEL_RAISED"));
		assertTrue(controls.contains("labelDivider = new ColorBlock("));
		assertTrue(icon.contains("(pressed ? 1f : 0f)"));
		assertTrue(icon.contains("touchDisabledStrike("));
		assertTrue(icon.contains("TouchGlyph.valueOf(glyph.name())"));
		assertFalse(icon.contains("ColorBlock"));
		assertFalse(icon.contains("Icons.get("));
		assertFalse(icon.contains("new Image("));
	}

	@Test
	public void touchActionsUseRealShortWordsAndTwentyTwoPixelHitTargets() {
		assertEquals(
				"Use",
				BukovTouchControls.compactActionLabel(
						BukovTouchState.Action.INTERACT,
						"Interact"));
		assertEquals(
				"Reload",
				BukovTouchControls.compactActionLabel(
						BukovTouchState.Action.RELOAD,
						"Reload"));
		assertEquals(
				"Heal",
				BukovTouchControls.compactActionLabel(
						BukovTouchState.Action.MEDICAL,
						"Medical"));
		assertEquals(
				"交互",
				BukovTouchControls.compactActionLabel(
						BukovTouchState.Action.INTERACT,
						"交互"));
		assertFalse("Inte.".equals(
				BukovTouchControls.compactActionLabel(
						BukovTouchState.Action.INTERACT,
						"Interact")));
		assertEquals(5, BukovTouchControls.actionLabelFontPx(9));
		assertTrue(
				BukovTouchControls.actionIconSize(28f, 28f)
						> BukovTouchControls.actionLabelFontPx(9));
		assertEquals(22f, BukovTouchControls.actionHitSize(19f), 0f);
		assertEquals(28f, BukovTouchControls.actionHitSize(28f), 0f);
	}

	@Test
	public void perActionAvailabilityMatchesItsInputState() {
		assertFalse(BukovTouchControls.actionDisabled(false, true));
		assertTrue(BukovTouchControls.actionDisabled(false, false));
		assertTrue(BukovTouchControls.actionDisabled(true, true));
	}

	@Test
	public void desktopControllerPathRemainsSeparateFromTouchOverlay()
			throws Exception {
		String scene = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/scenes/GameScene.java")),
				StandardCharsets.UTF_8);
		int desktop = scene.indexOf("if (DeviceCompat.isDesktop()) {");
		int touch = scene.indexOf(
				"bukovTouchControls = new BukovTouchControls()",
				desktop);
		int alternate = scene.lastIndexOf("} else {", touch);

		assertTrue(desktop >= 0);
		assertTrue(touch > desktop);
		assertTrue(alternate > desktop);
		assertTrue(alternate < touch);
	}

	private static String source(String file) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/bukov/ui/" + file)),
				StandardCharsets.UTF_8);
	}
}
