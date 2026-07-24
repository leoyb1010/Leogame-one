package com.shatteredpixel.shatteredpixeldungeon.bukov.settings;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

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
		assertTrue(window.contains("setCopy(\"主音量\","));
		assertTrue(window.contains("setCopy(\"音乐音量\","));
		assertTrue(window.contains("setCopy(\"音效音量\","));
		assertTrue(window.contains("setCopy(\"环境声音量\","));
		assertTrue(window.contains("setCopy(\"性能档\","));
		assertTrue(window.contains("\"高画质\""));
		assertTrue(window.contains("\"平衡\""));
		assertTrue(window.contains("\"高帧\""));
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
}
