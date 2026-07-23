package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Guards the Retina rendering path from being defeated by an extra world-camera
 * magnification. PixelScene and the iOS backend already render at an integer
 * zoom into a physical-pixel backbuffer; forcing maxZoom here only makes source
 * pixels twice as large and reduces the visible world area by four.
 */
public class BukovVisualQualityGuardTest {

	@Test
	public void bukovRaidDoesNotForceMaximumWorldZoom() throws Exception {
		String source = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java");

		assertFalse(
				"Bukov raids must use the normal integer world zoom; maxZoom makes Retina output look low-resolution",
				source.contains("Camera.main.zoom(maxZoom)"));
	}

	@Test
	public void iosRetinaBackbufferAndPixelSamplingStayEnabled() throws Exception {
		String launcher = source(
				"../ios/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ios/IOSLauncher.java");
		String texture = source(
				"../SPD-classes/src/main/java/com/watabou/gltextures/SmartTexture.java");

		assertTrue(
				"iOS must render into the physical-pixel Retina backbuffer",
				launcher.contains("config.hdpiMode = HdpiMode.Pixels"));
		assertTrue(
				"Pixel-art atlases must default to nearest-neighbour sampling",
				texture.contains("this( bitmap, NEAREST, CLAMP, false )"));
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
