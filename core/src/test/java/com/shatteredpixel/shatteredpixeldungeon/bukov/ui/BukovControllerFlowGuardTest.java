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
		String catalog = read(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/bukov/runtime/"
						+ "BukovInputBindings.java");

		assertTrue(title.contains("SPDAction.TAG_ATTACK"));
		assertTrue(title.contains("public GameAction keyAction()"));
		assertTrue(input.contains("BukovInputBindings.isFire(action)"));
		assertTrue(input.contains("BukovInputBindings.isReload("));
		assertTrue(input.contains("BukovInputBindings.isInteract("));
		assertTrue(input.contains("BukovInputBindings.isBackpack("));
		assertTrue(input.contains("BukovInputBindings.medicalSlot("));
		assertTrue(catalog.contains(
				"CONTROLLER_RELOAD =\n"
						+ "\t\t\tSPDAction.QUICKSLOT_SELECTOR"));
		assertTrue(catalog.contains(
				"CONTROLLER_BACKPACK =\n"
						+ "\t\t\tSPDAction.INVENTORY_SELECTOR"));
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
