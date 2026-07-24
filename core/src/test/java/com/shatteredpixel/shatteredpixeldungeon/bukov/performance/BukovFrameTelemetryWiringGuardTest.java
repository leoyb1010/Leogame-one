package com.shatteredpixel.shatteredpixeldungeon.bukov.performance;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovFrameTelemetryWiringGuardTest {

	@Test
	public void liveSceneSamplesRawRenderDeltaAndUsesDesktopLogRoute()
			throws Exception {
		String scene = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java");
		String deviceCompat = source(
				"../SPD-classes/src/main/java/com/watabou/utils/DeviceCompat.java");

		assertTrue(scene.contains("new BukovFrameTelemetry("));
		assertTrue(scene.contains(
				"bukovFrameTelemetry.recordFrame("));
		assertTrue(scene.contains("Gdx.graphics.getDeltaTime())"));
		assertFalse(scene.contains(
				"bukovFrameTelemetry.recordFrame(Game.elapsed)"));
		assertTrue(scene.contains("DeviceCompat.log("));
		assertTrue(scene.contains("BUKOV_FRAME_TELEMETRY_TAG,"));
		assertTrue(deviceCompat.contains("Gdx.app.log( tag, message )"));
	}

	@Test
	public void telemetryIsIsolatedFromDeterministicSimulation()
			throws Exception {
		String scene = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java");
		String fixedClock = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/FixedStepClock.java");
		String realtimeSystem = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/RealtimeRaidSystem.java");

		int liveBlock = scene.indexOf(
				"if (BukovMode.active() && bukovRealtime != null) {");
		int telemetryUpdate = scene.indexOf(
				"updateBukovFrameTelemetry();",
				liveBlock);
		int fixedSimulationUpdate = scene.indexOf(
				"bukovRealtime.update(Game.elapsed);",
				liveBlock);
		assertTrue(liveBlock >= 0);
		assertTrue(telemetryUpdate > liveBlock);
		assertTrue(fixedSimulationUpdate > telemetryUpdate);
		assertTrue(scene.contains("initializeBukovFrameTelemetry();"));
		assertFalse(fixedClock.contains("BukovFrameTelemetry"));
		assertFalse(realtimeSystem.contains("BukovFrameTelemetry"));
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
