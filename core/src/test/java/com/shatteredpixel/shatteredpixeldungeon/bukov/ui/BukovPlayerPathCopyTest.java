package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Keeps the Bukov player path from regressing to the legacy dungeon/expedition
 * vocabulary while the classic secondary mode remains available.
 */
public class BukovPlayerPathCopyTest {

	private static final String[] SCENE_KEYS = {
			"scenes.gamescene.bukov_enter",
			"scenes.gamescene.bukov_resume",
			"scenes.gamescene.bukov_tutorial_mobile",
			"scenes.gamescene.bukov_tutorial_desktop",
			"scenes.gamescene.bukov_tutorial_controller",
			"scenes.titlescene.start_bukov",
			"scenes.titlescene.continue_bukov",
			"scenes.titlescene.rankings",
			"scenes.titlescene.journal"
	};

	@Test
	public void englishAndChineseMainPathCopyIsPresentAndLegacyFree()
			throws Exception {
		assertBundle(
				"src/main/assets/messages/scenes/scenes.properties",
				SCENE_KEYS,
				"dungeon", "expedition", "hero class", "leo's");
		assertBundle(
				"src/main/assets/messages/scenes/scenes_zh.properties",
				SCENE_KEYS,
				"地牢", "远征", "职业", "leo的");
	}

	@Test
	public void bukovDoesNotRebrandHostHeroClassesAsOperators()
			throws Exception {
		for (String path : new String[] {
				"src/main/assets/messages/scenes/scenes.properties",
				"src/main/assets/messages/scenes/scenes_zh.properties"
		}) {
			Map<String, String> values = readProperties(new File(path));
			assertFalse(values.containsKey(
					"scenes.heroselectscene.bukov_title"));
			assertFalse(values.containsKey(
					"scenes.heroselectscene.bukov_start"));
			for (String hostClass : new String[] {
				"warrior", "mage", "rogue",
				"huntress", "duelist", "cleric"
			}) {
				assertFalse(values.containsKey(
						"scenes.heroselectscene.bukov_operator_"
								+ hostClass));
				assertFalse(values.containsKey(
						"scenes.heroselectscene.bukov_operator_"
								+ hostClass + "_desc"));
			}
		}
	}

	@Test
	public void bukovBranchesOwnFirstLogAndHud() throws Exception {
		String gameScene = read(new File(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java"));

		assertTrue(gameScene.contains("Messages.get(this,")
				&& gameScene.contains("\"bukov_enter\""));
		assertTrue(gameScene.contains("new BukovRaidHud()"));
		assertTrue(gameScene.contains("new BukovPauseButton("));
		assertTrue(gameScene.contains("scene != null && !BukovMode.active()"));
	}

	@Test
	public void welcomeNeverRoutesIntoInheritedPlayerFacingUpdates()
			throws Exception {
		String welcome = read(new File(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/WelcomeScene.java"));

		assertFalse(welcome.contains("ChangesScene.class"));
		assertTrue(welcome.contains("entryMessage(\"welcome.intro\")"));
		assertTrue(welcome.contains(
				"entryMessage(\"welcome.save_warning\")"));
		assertTrue(welcome.contains(
				"BukovMessages.get(\"bukov.entry.\" + key, args)"));
		assertFalse(welcome.contains("\"bukov_update\""));
		assertFalse(welcome.contains("\"bukov_future_save\""));
		assertFalse(welcome.contains("Messages.get(this, \"update_intro\")"));
		assertFalse(welcome.contains("Messages.get(this, \"what_msg\")"));
		assertFalse(welcome.contains("Messages.get(this, \"save_warning\")"));
		assertFalse(welcome.contains("updateVersion("));
	}

	@Test
	public void mobileHudKeepsRealtimeControlHint() throws Exception {
		String hud = read(new File(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/BukovHudFormat.java"));
		assertTrue(hud.contains(
				"BukovMessages.get(\"bukov.raid.hud.touch_objective\")"));
		assertFalse(hud.contains(
				"左拖移动 · 右拖射击 · 左上互动 · 右上装填"));
		assertFalse(hud.contains("点击一个位置以进行移动"));
	}

	private static void assertBundle(
			String path,
			String[] requiredKeys,
			String... bannedTerms) throws Exception {
		Map<String, String> values = readProperties(new File(path));
		for (String key : requiredKeys) {
			assertTrue(path + " missing " + key,
					values.containsKey(key) && !values.get(key).trim().isEmpty());
			String value = values.get(key).toLowerCase(Locale.ENGLISH);
			for (String term : bannedTerms) {
				assertFalse(
						path + " " + key + " contains legacy term " + term,
						value.contains(term.toLowerCase(Locale.ENGLISH)));
			}
		}
	}

	private static Map<String, String> readProperties(File file) throws IOException {
		Map<String, String> values = new LinkedHashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(file), "UTF-8"))) {
			String line;
			while ((line = reader.readLine()) != null) {
				int equals = line.indexOf('=');
				if (equals <= 0 || line.startsWith("#")) continue;
				values.put(line.substring(0, equals), line.substring(equals + 1));
			}
		}
		return values;
	}

	private static String read(File file) throws IOException {
		StringBuilder result = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(file), "UTF-8"))) {
			String line;
			while ((line = reader.readLine()) != null) {
				result.append(line).append('\n');
			}
		}
		return result.toString();
	}
}
