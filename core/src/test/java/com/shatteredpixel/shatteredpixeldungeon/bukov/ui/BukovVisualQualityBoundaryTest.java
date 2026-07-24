package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Static boundary checks for the visual failures that are hard to exercise in
 * headless tests: filtered fog, forgotten exploration and sub-pixel drift.
 */
public class BukovVisualQualityBoundaryTest {

	@Test
	public void bukovFogIsCrispReadableAndHasItsOwnTexture() throws Exception {
		String fog = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/tiles/FogOfWar.java");
		String scene = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java");

		assertTrue(fog.contains(
				"texture.filter(SmartTexture.NEAREST, SmartTexture.NEAREST)"));
		assertTrue(fog.contains("(bukovPalette ? \":bukov\" : \":classic\")"));
		assertTrue(fog.contains("TextureCache.remove(cacheKey)"));
		assertFalse(fog.contains("TextureCache.remove(FogOfWar.class)"));
		assertTrue(scene.contains("BukovMode.active())"));
		assertTrue(scene.contains("rememberBukovVisibility();"));
		assertTrue(scene.contains(
				"Dungeon.level.visited[cell] = true;"));
		assertTrue(
				"visited ground must remain materially brighter than unseen space",
				fog.contains("0x40131D20"));
		assertTrue(
				"unseen space must retain a distinct tactical shroud",
				fog.contains("0xDE0B1012"));
	}

	@Test
	public void realtimeWorldAndSpritesStayPixelAlignedWhileFollowingHero()
			throws Exception {
		String scene = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/PixelScene.java");
		String sprite = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/CharSprite.java");
		String world = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java");

		assertTrue(scene.contains(
				"float sx = align( this, scroll.x + shakeX )"));
		assertTrue(scene.contains(
				"float sy = align( this, scroll.y + shakeY )"));
		assertTrue(sprite.contains("PixelScene.align( Camera.main, centerX"));
		assertTrue(sprite.contains("PixelScene.align( Camera.main, (centerY"));
		assertTrue(world.contains("updateRealtimeCamera(Game.elapsed)"));
		assertTrue(world.contains("cameraFollow.update("));
		assertTrue(world.contains("camera.scroll.set("));
		assertTrue(sceneSource("GameScene.java").contains(
				"BukovViewport.resolveScroll("));
	}

	private static String sceneSource(String file) throws Exception {
		return source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/"
						+ file);
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
