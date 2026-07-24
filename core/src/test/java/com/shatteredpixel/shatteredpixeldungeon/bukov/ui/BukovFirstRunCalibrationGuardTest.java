package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Guards the one-shot migration boundary and non-blocking calibration UI. */
public class BukovFirstRunCalibrationGuardTest {

	@Test
	public void onlyBrandNewWelcomeFlowSchedulesCalibration()
			throws Exception {
		String welcome = source("scenes/WelcomeScene.java");

		assertTrue(welcome.contains("if (previousVersion > 0)"));
		assertTrue(welcome.contains(
				"} else {\n"
						+ "\t\t\t\t\t\tSPDSettings"
						+ ".scheduleBukovFirstRunCalibration();"));
		assertFalse(welcome.contains(
				"updateVersion(previousVersion);\n"
						+ "\t\t\t\t\t\tSPDSettings"
						+ ".scheduleBukovFirstRunCalibration();"));
	}

	@Test
	public void titleConsumesPromptBeforeShowingIt() throws Exception {
		String title = source("scenes/TitleScene.java");
		String settings = source("SPDSettings.java");

		assertTrue(title.contains(
				"SPDSettings.consumeBukovFirstRunCalibration()"));
		assertTrue(title.contains("new WndBukovFirstRunCalibration()"));
		assertTrue(settings.contains(
				"if (!contains(KEY_BUKOV_FIRST_RUN_CALIBRATION))"));
		assertTrue(settings.contains(
				"put(KEY_BUKOV_FIRST_RUN_CALIBRATION,"
						+ " BUKOV_CALIBRATION_CONSUMED);"));
		assertTrue(settings.indexOf("BUKOV_CALIBRATION_CONSUMED);")
				< settings.indexOf("return true;",
						settings.indexOf(
								"consumeBukovFirstRunCalibration")));
	}

	@Test
	public void threeChoicesPersistImmediatelyAndWindowNeverTrapsInput()
			throws Exception {
		String window = source(
				"bukov/ui/WndBukovFirstRunCalibration.java");

		assertTrue(window.contains("SPDSettings.bukovUiScale("));
		assertTrue(window.contains(
				"SPDSettings.bukovControllerVibration("));
		assertTrue(window.contains("SPDSettings.bukovAimAssist("));
		assertTrue(window.contains(
				"public void onBackPressed() {\n\t\thide();"));
		assertTrue(window.contains("BukovNavigation.back(event)"));
		assertTrue(window.contains("case DONE:\n\t\t\t\t\thide();"));
		assertFalse(window.contains("WndHardNotification"));
	}

	private static String source(String relative) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/"
								+ relative)),
				StandardCharsets.UTF_8);
	}
}
