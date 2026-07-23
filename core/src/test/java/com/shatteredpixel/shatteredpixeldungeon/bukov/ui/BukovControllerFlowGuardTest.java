package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class BukovControllerFlowGuardTest {

	@Test
	public void everyPlayerFacingWindowOwnsFocusAndNavigation()
			throws Exception {
		assertFocus("WndBukovBackpack.java");
		assertFocus("WndBukovPause.java");
		assertFocus("WndBukovSettings.java");
		assertFocus("WndBukovSettlement.java");
	}

	@Test
	public void titleAndRealtimeBindingsCoverControllerCriticalPath()
			throws Exception {
		String title = read(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/scenes/TitleScene.java");
		String input = read(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/bukov/runtime/"
						+ "RealtimeInput.java");

		assertTrue(title.contains("SPDAction.TAG_ATTACK"));
		assertTrue(title.contains("public GameAction keyAction()"));
		assertTrue(input.contains("Input.Keys.BUTTON_R2"));
		assertTrue(input.contains("Input.Keys.BUTTON_X"));
		assertTrue(input.contains("Input.Keys.BUTTON_A"));
		assertTrue(input.contains("Input.Keys.BUTTON_Y"));
		assertTrue(input.contains("Input.Keys.TAB"));
		assertTrue(input.contains("ControllerHandler.DPAD_KEY_OFFSET"));
	}

	private static void assertFocus(String file) throws Exception {
		String source = read(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/bukov/ui/" + file);
		assertTrue(file + " must own focus",
				source.contains("BukovFocusModel"));
		assertTrue(file + " must accept shared controller navigation",
				source.contains("BukovNavigation"));
	}

	private static String read(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
