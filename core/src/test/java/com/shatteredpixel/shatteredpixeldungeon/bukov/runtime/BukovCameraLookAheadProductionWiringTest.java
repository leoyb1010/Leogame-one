package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovCameraLookAheadProductionWiringTest {

	@Test
	public void activeAimDeviceSelectsTheAuthoredPresentationDistance()
			throws Exception {
		String frame = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/InputFrame.java");
		String input = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/RealtimeInput.java");

		assertTrue(frame.contains("public float cameraLookAheadTiles;"));
		assertTrue(input.contains(
				"POINTER_CAMERA_LOOK_AHEAD_TILES = 3.5f;"));
		assertTrue(input.contains(
				"DIRECT_CAMERA_LOOK_AHEAD_TILES = 2.5f;"));
		assertTrue(input.contains(
				"frame.cameraLookAheadTiles = POINTER_CAMERA_LOOK_AHEAD_TILES;"));
		assertTrue(input.contains(
				"frame.cameraLookAheadTiles = DIRECT_CAMERA_LOOK_AHEAD_TILES;"));
	}

	@Test
	public void worldConvertsTilesOnlyInsidePresentationCameraPath()
			throws Exception {
		String world = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java");
		String cameraMethod = between(
				world,
				"private void updateRealtimeCamera(float renderDelta)",
				"public float presentationCameraFocusX()");

		assertTrue(cameraMethod.contains(
				"inputFrame.cameraLookAheadTiles * DungeonTilemap.SIZE"));
		assertTrue(cameraMethod.contains(
				"inputFrame.aim.x * lookAheadPixels"));
		assertTrue(cameraMethod.contains(
				"inputFrame.aim.y * lookAheadPixels"));
		assertTrue(cameraMethod.contains("cameraFollow.update("));
		assertTrue(cameraMethod.contains("camera.scroll.set("));
		assertFalse(cameraMethod.contains("heroBody.x ="));
		assertFalse(cameraMethod.contains("heroBody.y ="));
		assertFalse(cameraMethod.contains("hero.pos ="));
	}

	@Test
	public void viewportClampsAndPixelAlignsTheLookAheadFocus()
			throws Exception {
		String scene = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java");
		String viewport = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovViewport.java");

		assertTrue(scene.contains(
				"bukovWorld.presentationCameraFocusX()"));
		assertTrue(scene.contains(
				"bukovWorld.presentationCameraFocusY()"));
		assertTrue(scene.contains("BukovViewport.resolveScroll("));
		assertTrue(viewport.contains(
				"return Math.round(value * zoom) / zoom;"));
	}

	private static String between(
			String value,
			String start,
			String end) {
		int startIndex = value.indexOf(start);
		int endIndex = value.indexOf(end, startIndex);
		if (startIndex < 0 || endIndex < 0) {
			throw new AssertionError("Could not locate production camera method");
		}
		return value.substring(startIndex, endIndex);
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
