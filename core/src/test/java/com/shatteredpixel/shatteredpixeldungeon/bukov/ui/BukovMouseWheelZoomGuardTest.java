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

		assertGuardBefore(
				text,
				"protected void onPointerDown",
				"camera.edgeScroll.set(-1);");
		assertGuardBefore(
				text,
				"protected void onPointerUp",
				"camera.edgeScroll.set(1);");
		assertGuardBefore(
				text,
				"protected void onDrag",
				"if (pinching)");
		assertTrue(text.contains(
				"if (BukovMode.active()) {\n"
						+ "\t\t\tcamera.edgeScroll.set(0);"));
	}

	private static void assertGuardBefore(
			String source, String method, String legacyMutation) {
		int start = source.indexOf(method);
		int guard = source.indexOf("if (BukovMode.active())", start);
		int mutation = source.indexOf(legacyMutation, start);
		assertTrue(method + " must guard the inherited camera mutation",
				start >= 0 && guard > start && mutation > guard);
	}
}
