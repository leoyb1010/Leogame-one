package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Guards realtime inventory and input lifecycle boundaries in GameScene. */
public class BukovSceneLifecycleGuardTest {

	@Test
	public void returnToHubWritesRuntimeInventoryBeforeBothSaves()
			throws Exception {
		String returnMethod = between(
				scene(),
				"private void saveBukovAndReturnToHub()",
				"private static String currentBukovRaidId()");
		assertTrue(returnMethod.contains(
				"persistBukovHostAndCheckpoint();"));

		String method = between(
				scene(),
				"private void persistBukovHostAndCheckpoint()",
				"private void writeBackBukovRuntimeLoadout()");
		int writeBack = method.indexOf("writeBackBukovRuntimeLoadout();");
		int hostSave = method.indexOf("Dungeon.saveAll();");
		int raidSave = method.indexOf("bukovRaid.saveCheckpoint();");
		assertTrue(writeBack >= 0);
		assertTrue(hostSave > writeBack);
		assertTrue(raidSave > hostSave);
	}

	@Test
	public void backpackControllerSurvivesSceneFieldTeardown()
			throws Exception {
		String method = between(
				scene(),
				"private void openBukovBackpack()",
				"public void addCustomTile");
		assertTrue(method.contains(
				"final BukovRealtimeWorld backpackWorld = bukovWorld;"));
		assertTrue(method.contains("backpackWorld.backpackSnapshot()"));
		assertTrue(method.contains("backpackWorld.setBackpackOpen(open)"));
		assertTrue(method.contains("private void closeBukovBackpack()"));
	}

	@Test
	public void pauseAndDestroyClearHeldRealtimeInput() throws Exception {
		String source = scene();
		String pause = between(
				source,
				"public synchronized void onPause()",
				"private static Thread actorThread");
		String destroy = between(
				source,
				"public void destroy()",
				"public static void endActorThread()");
		assertTrue(pause.contains("resetBukovInputState();"));
		assertTrue(pause.contains(
				"catch (IOException | RuntimeException e)"));
		assertTrue(destroy.contains("closeBukovBackpack();"));
		assertTrue(destroy.contains("resetBukovInputState();"));

		String input = read(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/RealtimeInput.java");
		assertTrue(input.contains("public void resetTransientState()"));
		assertTrue(input.contains("controllerFireHeld = false;"));
		assertTrue(input.contains("controllerInteractHeld = false;"));
		assertTrue(input.contains("frame.clearEdges();"));
	}

	@Test
	public void realtimeCriticalChangesUseTheSameHostAndCheckpointCommit()
			throws Exception {
		String source = scene();
		assertTrue(source.contains(
				"new BukovRaidPersistence.Commit()"));
		assertTrue(source.contains(
				"persistBukovHostAndCheckpoint();"));

		String world = read(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/runtime/BukovRealtimeWorld.java");
		assertTrue(world.contains(
				"persistence.criticalStateChanged()"));
		assertTrue(world.contains(
				"persistence.update(dt)"));
	}

	private static String scene() throws Exception {
		return read(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java");
	}

	private static String between(String source, String start, String end) {
		int from = source.indexOf(start);
		int to = source.indexOf(end, from);
		if (from < 0 || to < 0) {
			throw new AssertionError("Source boundary not found");
		}
		return source.substring(from, to);
	}

	private static String read(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
