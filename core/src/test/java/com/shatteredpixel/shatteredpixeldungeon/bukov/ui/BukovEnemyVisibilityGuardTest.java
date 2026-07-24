package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovEnemySprite;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Regression guard for the realtime-camera failure where enemy AI and HUD
 * contacts existed, but the original dark 16x18 sheets looked invisible.
 */
public class BukovEnemyVisibilityGuardTest {

	private static final String[] ENEMY_SHEETS = {
			"scavenger.png",
			"gunner.png",
			"armored.png",
			"captain.png",
			"drone.png",
			"white_line.png",
			"alley_scout.png",
			"depot_shotgunner.png",
			"line_rifleman.png",
			"fog_stalker.png",
			"signal_operator.png",
			"iron_clasp_marksman.png",
			"breach_veteran.png"
	};

	@Test
	public void allThirteenEnemySheetsHaveAnOpaqueIdleSilhouette()
			throws Exception {
		assertEquals(13, ENEMY_SHEETS.length);
		for (String sheet : ENEMY_SHEETS) {
			Path path = Paths.get("src/main/assets/sprites/bukov", sheet);
			assertTrue(sheet, Files.isRegularFile(path));
			BufferedImage image = ImageIO.read(path.toFile());
			assertNotNull(sheet, image);
			assertEquals(sheet, 256, image.getWidth());
			assertEquals(sheet, 18, image.getHeight());

			int visiblePixels = 0;
			int maximumAlpha = 0;
			for (int y = 0; y < 18; y++) {
				for (int x = 0; x < 16; x++) {
					int alpha = image.getRGB(x, y) >>> 24;
					if (alpha > 0) visiblePixels++;
					maximumAlpha = Math.max(maximumAlpha, alpha);
				}
			}
			assertTrue(sheet + " idle frame is empty", visiblePixels >= 60);
			assertEquals(sheet + " has no opaque contact pixels",
					255, maximumAlpha);
		}
	}

	@Test
	public void sharedPresentationIsLargeBrightOpaqueAndFogBound()
			throws Exception {
		assertTrue(BukovEnemySprite.CONTACT_SCALE >= 1.25f);
		assertTrue(BukovEnemySprite.CONTACT_LIGHTNESS >= 0.70f);
		assertTrue(BukovEnemySprite.CONTACT_OUTLINE_OFFSET > 0f);
		assertEquals(0xFF, BukovEnemySprite.CONTACT_COLOR >>> 24);

		String sprite = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "sprites/bukov/BukovEnemySprite.java");
		String scene = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "scenes/GameScene.java");
		String world = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/runtime/BukovRealtimeWorld.java");
		String hud = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/ui/BukovRaidHud.java");

		assertTrue(sprite.contains(
				"texture.filter(SmartTexture.NEAREST, SmartTexture.NEAREST)"));
		assertTrue(sprite.contains("scale.set(CONTACT_SCALE, CONTACT_SCALE)"));
		assertTrue(sprite.contains("lightness(CONTACT_LIGHTNESS)"));
		assertTrue(sprite.contains(
				"drawContactOutline(-CONTACT_OUTLINE_OFFSET, 0f)"));
		assertTrue(sprite.contains(
				"drawContactOutline(0f, CONTACT_OUTLINE_OFFSET)"));
		assertTrue(sprite.contains(
				"tint(CONTACT_COLOR & 0xFFFFFF, 1f)"));
		assertTrue(sprite.contains("renderShadow = false"));
		assertTrue(sprite.contains(
				"renderShadow = originalRenderShadow"));
		assertTrue(sprite.contains("new ColorBlock(12f, 1f, CONTACT_COLOR)"));
		assertFalse(sprite.contains("alpha(0."));

		// Contacts are still hidden outside FOV. The marker is drawn by the
		// same sprite and therefore cannot leak enemy position through fog.
		assertTrue(scene.contains(
				"sprite.visible = Dungeon.level.heroFOV[mob.pos]"));
		assertTrue(scene.contains(
				"mob.sprite.visible = Dungeon.level.heroFOV[mob.pos]"));
		assertEquals(
				"every realtime FOV refresh must use the shared visibility path",
				4,
				occurrences(world, "refreshHeroVisibility();"));
		assertTrue(between(
				world,
				"collisionMap = new LevelCollisionMap(",
				"collision = new GridCollision(")
				.contains("refreshHeroVisibility();"));
		assertTrue(between(
				world,
				"if (recoverHeroCheckpoint(",
				"lastHudRoom = currentHudRoom();")
				.contains("refreshHeroVisibility();"));
		assertTrue(between(
				world,
				"if (hero.pos != previousCell) {",
				"public void emitPlayerActions(")
				.contains("refreshHeroVisibility();"));
		assertTrue(between(
				world,
				"private void applyMissionGateTerrain() {",
				"private boolean movementWasBlocked(")
				.contains("refreshHeroVisibility();"));
		String refresh = between(
				world,
				"private void refreshHeroVisibility() {",
				"private int terrainAt(");
		assertTrue(refresh.contains("Dungeon.level.updateFieldOfView("));
		assertTrue(refresh.contains("GameScene.updateFog();"));
		assertTrue(refresh.contains("GameScene.afterObserve();"));

		// Awareness text belongs at the HUD edge. A radial threat slab can
		// otherwise sit directly over the enemy whose direction it describes.
		assertTrue(hud.contains(
				"AWARENESS_SIDE_MARGIN + awarenessWidth * 0.5f"));
		assertTrue(hud.contains(
				"viewportWidth - AWARENESS_SIDE_MARGIN"));
		assertFalse(hud.contains("float threatRadius"));
		assertFalse(hud.contains("float navigationRadius"));
	}

	private static String source(String relativePath) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(relativePath)),
				StandardCharsets.UTF_8);
	}

	private static int occurrences(String source, String token) {
		int count = 0;
		int offset = 0;
		while ((offset = source.indexOf(token, offset)) >= 0) {
			count++;
			offset += token.length();
		}
		return count;
	}

	private static String between(
			String source, String startToken, String endToken) {
		int start = source.indexOf(startToken);
		int end = source.indexOf(endToken, start + startToken.length());
		assertTrue("missing start token " + startToken, start >= 0);
		assertTrue("missing end token " + endToken, end > start);
		return source.substring(start, end);
	}
}
