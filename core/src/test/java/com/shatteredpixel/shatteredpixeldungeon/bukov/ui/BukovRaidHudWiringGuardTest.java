package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Prevents the realtime HUD from regressing into simulated or frame-based data. */
public class BukovRaidHudWiringGuardTest {

	@Test
	public void worldCopiesLiveWeaponStatusInteractionAndExtractionState()
			throws Exception {
		String world = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java");
		String hud = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/BukovRaidHud.java");

		assertTrue(world.contains("implements RealtimeRaidSystem.World, FireControl.Sink,"));
		assertTrue(world.contains("RaidObjectiveSource, BukovRaidHudSource"));
		assertTrue(world.contains("void readRaidHudState(BukovRaidHudState target)"));
		assertTrue(world.contains("fireControl.reloadRemaining()"));
		assertTrue(world.contains("medicalStatus.bleedingPerSecond()"));
		assertTrue(world.contains("active.progressFraction()"));
		assertTrue(world.contains("extractionHere.availableAt(elapsed)"));
		assertTrue(world.contains("selectVisibleLootHeap("));
		assertTrue(world.contains("target.sound(keySoundVisual)"));
		assertTrue(world.contains("target.hit("));
		assertTrue(world.contains("readBossHudState(target)"));
		assertTrue(world.contains("SPDSettings.bukovDamageNumbers()"));
		assertTrue(world.contains("shouldShowDamageNumber("));

		assertTrue(hud.contains("final BukovRaidHudState live"));
		assertTrue(hud.contains("hudSource.readRaidHudState(live)"));
		assertTrue(hud.contains("live.reloadProgress()"));
		assertTrue(hud.contains("live.interactionProgress()"));
		assertTrue(hud.contains("live.extractionProgress()"));
		assertTrue(hud.contains("healthFlashRemaining = 0.07f"));
		assertTrue(hud.contains("Math.ceil(Math.max(1, lastMaxHp) / 10f)"));
		assertTrue(hud.contains("BukovCombatHudFormat.sound(live)"));
		assertTrue(hud.contains("BukovCombatHudFormat.hit(live)"));
		assertTrue(hud.contains("BukovCombatHudFormat.bossTitle(live)"));
		assertTrue(hud.contains("live.bossHealthFraction()"));
		assertFalse(hud.contains("Actor."));
		assertFalse(hud.contains("Random."));
	}

	@Test
	public void hudBlinkAndProgressAreTimeBasedNotFrameBased()
			throws Exception {
		String hud = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/BukovRaidHud.java");
		assertTrue(hud.contains("float elapsed = Math.max(0f, Game.elapsed)"));
		assertTrue(hud.contains("uiSeconds += elapsed"));
		assertTrue(hud.contains("Math.floor(uiSeconds * 2f)"));
		assertFalse(hud.contains("frameCount"));
		assertFalse(hud.contains("Actor.now"));
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
