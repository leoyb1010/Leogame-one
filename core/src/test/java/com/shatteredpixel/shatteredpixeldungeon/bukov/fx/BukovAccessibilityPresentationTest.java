package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BukovAccessibilityPresentationTest {

	@Test
	public void reducedFlashesHalveFinalRenderedAlpha() {
		assertEquals(
				0.8f,
				BukovAccessibilityPresentation.flashAlpha(0.8f, false),
				0.0001f);
		assertEquals(
				0.4f,
				BukovAccessibilityPresentation.flashAlpha(0.8f, true),
				0.0001f);
		assertEquals(
				0.5f,
				BukovAccessibilityPresentation.flashAlpha(3f, true),
				0.0001f);
	}

	@Test
	public void combatViewsAndHudConsumeTheSharedFinalRenderScale()
			throws Exception {
		String pool = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/fx/BukovCombatFxViewPool.java");
		String muzzle = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/fx/BukovMuzzleFx.java");
		String explosion = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/fx/BukovExplosionFx.java");
		String hud = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/ui/BukovRaidHud.java");

		assertTrue(pool.contains("SPDSettings.bukovReduceFlashes()"));
		assertTrue(pool.contains(
				"BukovAccessibilityPresentation.flashScale("));
		assertTrue(muzzle.contains("* flashScale"));
		assertTrue(explosion.contains("* flashScale"));
		assertTrue(hud.contains(
				"BukovAccessibilityPresentation.flashAlpha("));
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
