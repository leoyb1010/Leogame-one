package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Keeps hideout loadout risk connected to the actual realtime raid. */
public class BukovRuntimeLoadoutWiringGuardTest {

	@Test
	public void sceneInstallsAndWritesBackDeployedLoadout() throws Exception {
		String scene = read(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java");
		assertTrue(scene.contains("installBukovRuntimeLoadout()"));
		assertTrue(scene.contains(".materialize(bukovRaid)"));
		assertTrue(scene.contains("bukovRuntimeLoadout.installOn(Dungeon.hero)"));
		assertTrue(scene.contains("writeBackBukovRuntimeLoadout()"));
		assertTrue(scene.contains("bukovRuntimeLoadout.writeBack(bukovRaid.loot())"));
	}

	@Test
	public void realtimeWorldNeverGrantsAnUntrackedFallbackGun() throws Exception {
		String world = read(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java");
		assertFalse(world.contains("ensureStarterLoadout"));
		assertFalse(world.contains("\"starter-\" + Dungeon.seed"));
		assertTrue(world.contains("resolveEquippedFirearm();"));
	}

	private static String read(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
