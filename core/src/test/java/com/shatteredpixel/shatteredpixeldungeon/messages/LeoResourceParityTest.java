package com.shatteredpixel.shatteredpixeldungeon.messages;

import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LeoResourceParityTest {
	private static final String[] RESOURCE_GROUPS = {
			"actors", "items", "journal", "levels", "misc",
			"plants", "scenes", "ui", "windows"
	};

	@Test
	public void globalChineseResourceKeysMatchEnglish() throws IOException {
		for (String group : RESOURCE_GROUPS) {
			assertPrefixParity("messages/" + group + "/" + group + ".properties",
					"messages/" + group + "/" + group + "_zh.properties", "");
		}
	}

	@Test
	public void leoEnglishAndChineseKeysStayInSync() throws IOException {
		assertPrefixParity("messages/misc/misc.properties", "messages/misc/misc_zh.properties", "leoidentityconfig.");
		assertPrefixParity("messages/scenes/scenes.properties", "messages/scenes/scenes_zh.properties", "scenes.aboutscene.");
		assertPrefixParity("messages/ui/ui.properties", "messages/ui/ui_zh.properties", "ui.changelist.leochanges.");
		assertPrefixParity("messages/windows/windows.properties", "messages/windows/windows_zh.properties", "windows.wndleowelcome.");
	}

	private static void assertPrefixParity(String englishPath, String chinesePath, String prefix) throws IOException {
		Properties english = load(englishPath);
		Properties chinese = load(chinesePath);
		long englishCount = english.stringPropertyNames().stream().filter(key -> key.startsWith(prefix)).count();
		long chineseCount = chinese.stringPropertyNames().stream().filter(key -> key.startsWith(prefix)).count();
		assertTrue("No English resources found for " + prefix, englishCount > 0);
		assertEquals("Resource count differs for " + prefix, englishCount, chineseCount);
		for (String key : english.stringPropertyNames()) {
			if (key.startsWith(prefix)) assertTrue("Missing Chinese resource: " + key, chinese.containsKey(key));
		}
	}

	private static Properties load(String relativePath) throws IOException {
		Path path = Paths.get("src/main/assets").resolve(relativePath);
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			properties.load(reader);
		}
		return properties;
	}
}
