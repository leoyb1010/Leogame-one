package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.Reader;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WndBukovSettingsTest {

	@Test
	public void labelsCoverAccessiblePresentationRanges() throws Exception {
		Properties english = entryMessages("");
		Properties chinese = entryMessages("_zh");
		assertEquals("OFF",
				english.getProperty("bukov.entry.settings.off"));
		assertEquals("关闭",
				chinese.getProperty("bukov.entry.settings.off"));
		assertEquals("LOW",
				english.getProperty("bukov.entry.settings.low"));
		assertEquals("低",
				chinese.getProperty("bukov.entry.settings.low"));
		assertEquals("STANDARD",
				english.getProperty("bukov.entry.settings.standard"));
		assertEquals("标准",
				chinese.getProperty("bukov.entry.settings.standard"));
		assertEquals("HIGH",
				english.getProperty("bukov.entry.settings.strong"));
		assertEquals("强",
				chinese.getProperty("bukov.entry.settings.strong"));
		assertEquals("SOFT DARK",
				english.getProperty("bukov.entry.settings.dim"));
		assertEquals("柔暗",
				chinese.getProperty("bukov.entry.settings.dim"));
		assertEquals("BRIGHT",
				english.getProperty("bukov.entry.settings.bright"));
		assertEquals("明亮",
				chinese.getProperty("bukov.entry.settings.bright"));
		assertEquals("LARGE HITS",
				english.getProperty(
						"bukov.entry.settings.large_damage"));
		assertEquals("仅大伤害",
				chinese.getProperty(
						"bukov.entry.settings.large_damage"));
		assertEquals("STANDARD %1$d%%",
				english.getProperty(
						"bukov.entry.settings.standard_percent"));
		assertEquals("标准 %1$d%%",
				chinese.getProperty(
						"bukov.entry.settings.standard_percent"));

		assertEquals("50%", WndBukovSettings.threeLevel(1));
		assertEquals("100%", WndBukovSettings.threeLevel(2));
		assertEquals("125%", WndBukovSettings.percentLevel(1));
		assertEquals("16% / 96%",
				WndBukovSettings.deadZoneLabel(16, 96));
		assertEquals(2, WndBukovSettings.nextDeadZoneProfile(16));
		assertEquals(2, WndBukovSettings.nextTriggerProfile(65));

		String source = settingsSource();
		assertTrue(source.contains("settings.off"));
		assertTrue(source.contains("settings.low"));
		assertTrue(source.contains("settings.standard"));
		assertTrue(source.contains("settings.strong"));
		assertTrue(source.contains("settings.dim"));
		assertTrue(source.contains("settings.bright"));
		assertTrue(source.contains("settings.large_damage"));
		assertTrue(source.contains("settings.standard_percent"));
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

	@Test
	public void legalEntryUsesPackagedNoticesWithoutLegacyPromotion()
			throws Exception {
		String source = settingsSource();
		String chineseNotice = entryMessages("_zh").getProperty(
				"bukov.entry.settings.legal_notice");
		String englishNotice = entryMessages("").getProperty(
				"bukov.entry.settings.legal_notice");

		assertTrue(source.contains(
				"entryMessage(\"settings.legal_notice\")"));
		assertTrue(chineseNotice.contains("legal/LICENSE.txt"));
		assertTrue(chineseNotice.contains(
				"legal/THIRD_PARTY_NOTICES.txt"));
		assertTrue(chineseNotice.contains("不附带任何担保"));
		assertTrue(chineseNotice.contains("对应源码说明"));
		assertTrue(chineseNotice.contains("上游署名"));
		assertTrue(chineseNotice.contains("第三方声明"));
		assertTrue(englishNotice.contains("without warranty"));
		assertTrue(englishNotice.contains(
				"corresponding-source information"));
		assertTrue(englishNotice.contains("upstream attribution"));
		assertTrue(englishNotice.contains("third-party notices"));
		assertFalse(chineseNotice.contains("ShatteredPixel.com"));
		assertFalse(chineseNotice.contains("patreon.com"));
		assertFalse(englishNotice.contains("ShatteredPixel.com"));
		assertFalse(englishNotice.contains("patreon.com"));

		String notices = new String(
				Files.readAllBytes(Paths.get(
						"src/main/assets/legal/THIRD_PARTY_NOTICES.txt")),
				StandardCharsets.UTF_8);
		assertTrue(notices.contains("Shattered Pixel Dungeon"));
		assertTrue(notices.contains("Pixel Dungeon"));
		assertTrue(notices.contains(
				"Required\nupstream attribution remains"));
		assertFalse(notices.contains("in-game\ncredits"));
	}

	@Test
	public void bindingEntriesOpenExistingEditorAndPreserveSettingsWindow()
			throws Exception {
		String source = settingsSource();
		Properties english = entryMessages("");
		Properties chinese = entryMessages("_zh");

		assertEquals("KEYBOARD BINDINGS", english.getProperty(
				"bukov.entry.settings.keyboard_bindings"));
		assertEquals("CONTROLLER BINDINGS", english.getProperty(
				"bukov.entry.settings.controller_bindings"));
		assertEquals("CONFIGURE", english.getProperty(
				"bukov.entry.settings.configure"));
		assertEquals("键盘绑定", chinese.getProperty(
				"bukov.entry.settings.keyboard_bindings"));
		assertEquals("手柄绑定", chinese.getProperty(
				"bukov.entry.settings.controller_bindings"));
		assertEquals("配置", chinese.getProperty(
				"bukov.entry.settings.configure"));

		assertTrue(source.contains(
				"windows.WndKeyBindings;"));
		assertTrue(source.contains("KEYBOARD_BINDINGS,"));
		assertTrue(source.contains("CONTROLLER_BINDINGS,"));
		assertTrue(source.indexOf("KEYBOARD_BINDINGS,")
				< source.indexOf("LEGAL,"));
		assertTrue(source.contains("openBindings(false);"));
		assertTrue(source.contains("openBindings(true);"));
		assertTrue(source.contains(
				"setting == Setting.KEYBOARD_BINDINGS"));
		assertTrue(source.contains(
				"setting == Setting.CONTROLLER_BINDINGS"));
		assertTrue(source.contains(
				"return BukovTouchIcon.Glyph.SETTINGS;"));

		int helperStart = source.indexOf(
				"private void openBindings(boolean controller)");
		int helperEnd = source.indexOf(
				"\n\tprivate enum Setting", helperStart);
		assertTrue(helperStart >= 0);
		assertTrue(helperEnd > helperStart);
		String helper = source.substring(helperStart, helperEnd);
		assertTrue(helper.contains("new WndKeyBindings(controller)"));
		assertTrue(helper.contains("addToFront("));
		assertFalse(helper.contains("hide();"));
	}

	private static String settingsSource() throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/shatteredpixeldungeon"
								+ "/bukov/ui/WndBukovSettings.java")),
				StandardCharsets.UTF_8);
	}

	private static Properties entryMessages(String suffix)
			throws Exception {
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(
				Paths.get("src/main/assets/messages/bukov_entry/"
						+ "bukov_entry" + suffix + ".properties"),
				StandardCharsets.UTF_8)) {
			properties.load(reader);
		}
		return properties;
	}
}
