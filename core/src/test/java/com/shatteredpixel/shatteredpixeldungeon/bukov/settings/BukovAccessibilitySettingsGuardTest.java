package com.shatteredpixel.shatteredpixeldungeon.bukov.settings;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class BukovAccessibilitySettingsGuardTest {

	@Test
	public void planSettingsWriteThroughCrossPlatformPreferenceStore()
			throws Exception {
		String settings = read(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "SPDSettings.java");
		String window = read(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/ui/WndBukovSettings.java");

		String[] persistedMethods = {
				"bukovUiScale",
				"bukovReduceMotion",
				"bukovReduceFlashes",
				"bukovColorblindAssist",
				"bukovSoundVisualization",
				"bukovControllerVibration",
				"bukovDamageNumbers",
				"bukovAimAssist",
				"bukovLeftInnerDeadZone",
				"bukovLeftOuterDeadZone",
				"bukovRightInnerDeadZone",
				"bukovRightOuterDeadZone",
				"bukovAimCurve",
				"bukovTriggerThresholds"
		};
		for (String method : persistedMethods) {
			assertTrue(method + " must be persisted",
					settings.contains("void " + method + "("));
			assertTrue(method + " must be reachable from settings UI",
					window.contains("SPDSettings." + method + "("));
		}
	}

	@Test
	public void realtimeInputConsumesDeadZonesCurveTriggerAndAssist()
			throws Exception {
		String input = read(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/runtime/RealtimeInput.java");
		assertTrue(input.contains("bukovLeftInnerDeadZone"));
		assertTrue(input.contains("bukovLeftOuterDeadZone"));
		assertTrue(input.contains("bukovRightInnerDeadZone"));
		assertTrue(input.contains("bukovRightOuterDeadZone"));
		assertTrue(input.contains("bukovAimCurve"));
		assertTrue(input.contains("bukovAimAssist"));
		assertTrue(input.contains("configureTriggerThresholds"));
	}

	private static String read(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
