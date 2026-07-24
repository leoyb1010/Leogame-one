package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class BukovMouseWheelZoomGuardTest {

	@Test
	public void realtimeRaidConsumesNoLegacyMouseWheelZoom() throws Exception {
		Path source = Paths.get(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/CellSelector.java");
		String text = new String(
				Files.readAllBytes(source),
				StandardCharsets.UTF_8);

		int scroll = text.indexOf("protected void onScroll");
		int guard = text.indexOf("if (BukovMode.active())", scroll);
		int zoom = text.indexOf("float diff = event.amount", scroll);
		assertTrue(scroll >= 0 && guard > scroll && zoom > guard);

		Path sceneSource = Paths.get(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java");
		String scene = new String(
				Files.readAllBytes(sceneSource),
				StandardCharsets.UTF_8);
		assertTrue(scene.contains(
				"Camera.main.zoom(BukovMode.active()"
						+ "\n\t\t\t\t? BukovCameraPolicy.resolveWorldZoom("));
		assertTrue(scene.contains("Camera.main.screenWidth(),"));
		assertTrue(scene.contains(
				"Camera.main.screenWidth()"
						+ "\n\t\t\t\t\t\t\t\t>= Camera.main.screenHeight(),"));
		assertTrue(scene.contains("DungeonTilemap.SIZE,"));
	}

	@Test
	public void realtimeRaidDisablesLegacyPinchDragAndZoomBindings()
			throws Exception {
		Path source = Paths.get(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/CellSelector.java");
		String text = new String(
				Files.readAllBytes(source),
				StandardCharsets.UTF_8);

		assertTrue(text.contains(
				"active = !BukovMode.active();"));
		assertTrue(text.contains(
				"if (!BukovMode.active() && action == SPDAction.ZOOM_IN)"));
		assertTrue(text.contains(
				"} else if (!BukovMode.active() && action == SPDAction.ZOOM_OUT)"));
	}
}
