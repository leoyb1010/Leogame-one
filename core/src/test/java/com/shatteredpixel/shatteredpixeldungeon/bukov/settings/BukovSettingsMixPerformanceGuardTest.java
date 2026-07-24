package com.shatteredpixel.shatteredpixeldungeon.bukov.settings;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.Reader;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovSettingsMixPerformanceGuardTest {

	@Test
	public void settingsPersistFourChannelsAndDefaultToHighFrameRate()
			throws Exception {
		String settings = source(
				"com/shatteredpixel/shatteredpixeldungeon/SPDSettings.java");
		String window = source(
				"com/shatteredpixel/shatteredpixeldungeon/bukov/ui/"
						+ "WndBukovSettings.java");

		assertTrue(settings.contains("KEY_BUKOV_MASTER_VOLUME"));
		assertTrue(settings.contains("KEY_BUKOV_MUSIC_VOLUME"));
		assertTrue(settings.contains("KEY_BUKOV_SFX_VOLUME"));
		assertTrue(settings.contains("KEY_BUKOV_AMBIENCE_VOLUME"));
		assertTrue(settings.contains("KEY_BUKOV_PERFORMANCE_PROFILE"));
		assertTrue(settings.contains(
				"getInt(KEY_BUKOV_PERFORMANCE_PROFILE, 2, 0, 2)"));
		assertTrue(window.contains("settings.master"));
		assertTrue(window.contains("settings.music"));
		assertTrue(window.contains("settings.sfx"));
		assertTrue(window.contains("settings.ambience"));
		assertTrue(window.contains("settings.performance"));
		assertTrue(window.contains("settings.quality"));
		assertTrue(window.contains("settings.balanced"));
		assertTrue(window.contains("settings.framerate"));

		Properties english = entryMessages("");
		Properties chinese = entryMessages("_zh");
		assertEquals(english, chinese,
				"bukov.entry.settings.master", "MASTER VOLUME", "主音量");
		assertEquals(english, chinese,
				"bukov.entry.settings.music", "MUSIC VOLUME", "音乐音量");
		assertEquals(english, chinese,
				"bukov.entry.settings.sfx", "SFX VOLUME", "音效音量");
		assertEquals(english, chinese,
				"bukov.entry.settings.ambience",
				"AMBIENCE VOLUME", "环境声音量");
		assertEquals(english, chinese,
				"bukov.entry.settings.performance",
				"PERFORMANCE", "性能档");
		assertEquals(english, chinese,
				"bukov.entry.settings.quality",
				"HIGH QUALITY", "高画质");
		assertEquals(english, chinese,
				"bukov.entry.settings.balanced",
				"BALANCED", "平衡");
		assertEquals(english, chinese,
				"bukov.entry.settings.framerate",
				"HIGH FRAME RATE", "高帧");
	}

	@Test
	public void gameSceneReadsMixAndOnlyShedsPresentationFx()
			throws Exception {
		String scene = source(
				"com/shatteredpixel/shatteredpixeldungeon/scenes/"
						+ "GameScene.java");

		assertTrue(scene.contains("SPDSettings.bukovMasterVolume()"));
		assertTrue(scene.contains("SPDSettings.bukovMusicVolume()"));
		assertTrue(scene.contains("SPDSettings.bukovSfxVolume()"));
		assertTrue(scene.contains("SPDSettings.bukovAmbienceVolume()"));
		assertTrue(scene.contains(
				"BukovPerformancePolicy.renderCombatFx("));
		assertFalse(scene.contains(
				"bukovPerformanceProfile()) * BukovRealtimeWorld"));
	}

	private static String source(String file) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get("src/main/java/" + file)),
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

	private static void assertEquals(
			Properties english,
			Properties chinese,
			String key,
			String expectedEnglish,
			String expectedChinese) {
		org.junit.Assert.assertEquals(
				expectedEnglish, english.getProperty(key));
		org.junit.Assert.assertEquals(
				expectedChinese, chinese.getProperty(key));
	}
}
