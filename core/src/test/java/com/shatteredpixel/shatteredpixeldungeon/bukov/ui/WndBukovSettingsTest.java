package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WndBukovSettingsTest {

	@Test
	public void labelsCoverAccessiblePresentationRanges() {
		assertEquals("关闭", WndBukovSettings.scaleLabel(0, 4));
		assertEquals("低", WndBukovSettings.scaleLabel(1, 4));
		assertEquals("标准", WndBukovSettings.scaleLabel(2, 4));
		assertEquals("强", WndBukovSettings.scaleLabel(4, 4));
		assertEquals("柔暗", WndBukovSettings.brightnessLabel(-1));
		assertEquals("标准", WndBukovSettings.brightnessLabel(0));
		assertEquals("明亮", WndBukovSettings.brightnessLabel(1));
		assertEquals("50%", WndBukovSettings.threeLevel(1));
		assertEquals("100%", WndBukovSettings.threeLevel(2));
		assertEquals("125%", WndBukovSettings.percentLevel(1));
		assertEquals("仅大伤害",
				WndBukovSettings.damageNumbersLabel(1));
		assertEquals("标准 30%",
				WndBukovSettings.aimAssistLabel(2));
		assertEquals("16% / 96%",
				WndBukovSettings.deadZoneLabel(16, 96));
		assertEquals(2, WndBukovSettings.nextDeadZoneProfile(16));
		assertEquals(2, WndBukovSettings.nextTriggerProfile(65));
	}

	@Test
	public void raidPauseNeverLeaksTheDungeonSettingsSurface()
			throws Exception {
		String source = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/shatteredpixeldungeon"
								+ "/bukov/ui/WndBukovPause.java")),
				StandardCharsets.UTF_8);
		assertTrue(source.contains("new WndBukovSettings(new Runnable()"));
		assertFalse(source.contains("new WndSettings()"));
		assertFalse(source.contains("windows.WndSettings"));
	}
}
