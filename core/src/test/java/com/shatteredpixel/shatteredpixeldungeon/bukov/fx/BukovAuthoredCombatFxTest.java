package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovAuthoredCombatFxTest {

	@Test
	public void bloodMistHasShortReadableRiseAndFade() {
		assertEquals(0f, BukovBloodMistFx.mistAlphaAt(-1f), 0f);
		assertTrue(BukovBloodMistFx.mistAlphaAt(0.1f) > 0f);
		assertTrue(BukovBloodMistFx.mistAlphaAt(0.3f)
				> BukovBloodMistFx.mistAlphaAt(0.9f));
		assertEquals(0f, BukovBloodMistFx.mistAlphaAt(1f), 0f);
	}

	@Test
	public void bulletMarkPersistsThenFadesWithinItsBoundedLifetime() {
		assertEquals(1f, BukovBulletMarkFx.alphaAt(0f), 0f);
		assertEquals(1f, BukovBulletMarkFx.alphaAt(
				BukovBulletMarkFx.FADE_START_SECONDS), 0f);
		assertTrue(BukovBulletMarkFx.alphaAt(10f)
				> BukovBulletMarkFx.alphaAt(11f));
		assertEquals(0f, BukovBulletMarkFx.alphaAt(
				BukovBulletMarkFx.DURATION_SECONDS), 0f);
	}

	@Test
	public void explosionSeparatesFastFlashFromLaterSmoke() {
		assertEquals(1f, BukovExplosionFx.flashAlphaAt(0f), 0f);
		assertEquals(0f, BukovExplosionFx.smokeAlphaAt(0f), 0f);
		assertTrue(BukovExplosionFx.flashAlphaAt(0.1f)
				> BukovExplosionFx.flashAlphaAt(0.7f));
		assertTrue(BukovExplosionFx.smokeAlphaAt(0.3f) > 0f);
		assertEquals(0f, BukovExplosionFx.flashAlphaAt(1f), 0f);
		assertEquals(0f, BukovExplosionFx.smokeAlphaAt(1f), 0f);
	}

	@Test
	public void presentationHotPathAllocatesNoViewsOrCollections()
			throws Exception {
		String pool = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/fx/BukovCombatFxViewPool.java");
		String present = pool.substring(
				pool.indexOf("public void present("),
				pool.indexOf("public int activeCount()"));
		assertFalse(present.contains("new "));
		assertFalse(present.contains("List<"));
		assertFalse(present.contains("Map<"));
	}

	@Test
	public void productionRoutesBloodMarksAndExplosionAfterSimulation()
			throws Exception {
		String world = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java");
		assertTrue(world.contains("combatFx.bloodMist("));
		assertTrue(world.contains("combatFx.bulletMark("));
		assertTrue(world.contains("combatFx.explosion("));
		assertTrue(world.indexOf("combatFx.bloodMist(")
				> world.indexOf("resolvePlayerShot("));
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
