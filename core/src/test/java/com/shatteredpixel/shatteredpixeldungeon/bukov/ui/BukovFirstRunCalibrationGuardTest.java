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

		assertTrue(welcome.contains(
				"previousVersion != 0 && !SPDSettings.intro()"));
		assertTrue(welcome.contains(
				"previousVersion == 0);"));
		assertTrue(welcome.contains(
				"if (brandNewProfile) {\n"
						+ "\t\t\tSPDSettings"
						+ ".scheduleBukovFirstRunCalibration();"));
		assertFalse(welcome.contains(
				"updateVersion("));
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

	@Test
	public void allCalibrationCopyUsesTheEntryBundle()
			throws Exception {
		String window = source(
				"bukov/ui/WndBukovFirstRunCalibration.java");
		assertTrue(window.contains(
				"BukovMessages.get(\"bukov.entry.\" + key, args)"));
		assertTrue(window.contains("calibration.eyebrow"));
		assertTrue(window.contains("calibration.title"));
		assertTrue(window.contains("calibration.hint"));
		assertTrue(window.contains("calibration.ui_scale"));
		assertTrue(window.contains("calibration.vibration"));
		assertTrue(window.contains("calibration.aim_assist"));
		assertTrue(window.contains("calibration.done"));
		assertTrue(window.contains("calibration.enter"));

		assertFalse(window.contains("\"首次体验校准\""));
		assertFalse(window.contains("\"点击切换 · 即时保存 · 设置中可再改\""));
		assertFalse(window.contains("\"震动强度\""));
		assertFalse(window.contains("\"完成校准，继续\""));
	}

	@Test
	public void everyCalibrationActionHasASemanticIcon()
			throws Exception {
		String window = source(
				"bukov/ui/WndBukovFirstRunCalibration.java");

		assertTrue(window.contains("actionIcon = new BukovTouchIcon("));
		assertTrue(window.contains(
				"return BukovTouchIcon.Glyph.MODE;"));
		assertTrue(window.contains(
				"return BukovTouchIcon.Glyph.RECOMMEND;"));
		assertTrue(window.contains(
				"return BukovTouchIcon.Glyph.AIM_FIRE;"));
		assertTrue(window.contains(
				"return BukovTouchIcon.Glyph.DEPLOY;"));
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
