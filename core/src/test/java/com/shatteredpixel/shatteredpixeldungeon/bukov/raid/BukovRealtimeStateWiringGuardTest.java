package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class BukovRealtimeStateWiringGuardTest {

	@Test
	public void worldRestoresAndPublishesAllFixedStepState()
			throws Exception {
		String world = read(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/runtime/BukovRealtimeWorld.java");

		assertTrue(world.contains("raid.realtimeStatus()"));
		assertTrue(world.contains(
				"medicalSystem.restoreSnapshot(raid.medicalRuntime())"));
		assertTrue(world.contains("raid.enemyRuntime(mob.id())"));
		assertTrue(world.contains("brain.restoreSnapshot("));
		assertTrue(world.contains("rangedCombat.restoreSnapshot("));
		assertTrue(world.contains("publishRealtimeState();"));
		assertTrue(world.contains("enemy.brain.snapshot()"));
		assertTrue(world.contains("enemy.rangedCombat.snapshot()"));
		assertTrue(world.contains("heroHealthAtStep - hero.HP"));
	}

	@Test
	public void scenePublishesRuntimeBeforeHostAndCheckpointWrites()
			throws Exception {
		String scene = read(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "scenes/GameScene.java");
		int method = scene.indexOf(
				"private void persistBukovHostAndCheckpoint()");
		int writeBack = scene.indexOf(
				"writeBackBukovRuntimeLoadout();", method);
		int host = scene.indexOf("Dungeon.saveAll();", method);
		int checkpoint = scene.indexOf(
				"bukovRaid.saveCheckpoint();", method);

		assertTrue(method >= 0);
		assertTrue(writeBack > method);
		assertTrue(host > writeBack);
		assertTrue(checkpoint > host);
	}

	private static String read(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
