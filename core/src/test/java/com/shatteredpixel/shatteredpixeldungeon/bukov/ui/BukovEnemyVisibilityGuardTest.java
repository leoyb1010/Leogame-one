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
		assertTrue(BukovEnemySprite.CONTACT_SCALE >= 1.15f);
		assertTrue(BukovEnemySprite.CONTACT_LIGHTNESS >= 0.60f);
		assertEquals(0xFF, BukovEnemySprite.CONTACT_COLOR >>> 24);

		String sprite = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "sprites/bukov/BukovEnemySprite.java");
		String scene = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "scenes/GameScene.java");

		assertTrue(sprite.contains(
				"texture.filter(SmartTexture.NEAREST, SmartTexture.NEAREST)"));
		assertTrue(sprite.contains("scale.set(CONTACT_SCALE, CONTACT_SCALE)"));
		assertTrue(sprite.contains("lightness(CONTACT_LIGHTNESS)"));
		assertTrue(sprite.contains("new ColorBlock(12f, 1f, CONTACT_COLOR)"));
		assertFalse(sprite.contains("alpha(0."));

		// Contacts are still hidden outside FOV. The marker is drawn by the
		// same sprite and therefore cannot leak enemy position through fog.
		assertTrue(scene.contains(
				"sprite.visible = Dungeon.level.heroFOV[mob.pos]"));
		assertTrue(scene.contains(
				"mob.sprite.visible = Dungeon.level.heroFOV[mob.pos]"));
	}

	private static String source(String relativePath) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(relativePath)),
				StandardCharsets.UTF_8);
	}
}
