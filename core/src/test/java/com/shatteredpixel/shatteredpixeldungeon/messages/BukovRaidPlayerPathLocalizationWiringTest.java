package com.shatteredpixel.shatteredpixeldungeon.messages;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovRaidPlayerPathLocalizationWiringTest {

	private static final Pattern MESSAGE_KEY = Pattern.compile(
			"\"(bukov\\.raid\\.[^\"]+)\"");
	private static final Pattern CJK = Pattern.compile(
			"[\\u2E80-\\u9FFF\\uF900-\\uFAFF\\u3040-\\u30FF\\uAC00-\\uD7AF]");
	private static final Iterable<Path> PLAYER_PATH_SOURCES = Arrays.asList(
			path("bukov/runtime/BukovRealtimeWorld.java"),
			path("bukov/mission/FirstRaidMission.java"),
			path("bukov/tutorial/BukovTutorialEvent.java"),
			path("bukov/raid/BukovRaidCoordinator.java"),
			path("bukov/ui/BukovBackpackViewModel.java"));

	@Test
	public void playerPathUsesPairedRaidBundleKeysWithoutHardCodedCjk()
			throws IOException {
		Properties english = BukovLocalizationTestSupport.load("bukov_raid", "");
		Properties chinese = BukovLocalizationTestSupport.load("bukov_raid", "_zh");

		for (Path path : PLAYER_PATH_SOURCES) {
			String source = new String(
					Files.readAllBytes(path),
					StandardCharsets.UTF_8);
			assertTrue(
					path + " must use the unambiguous Bukov message wrapper",
					source.contains("BukovMessages.get("));
			assertFalse(
					path + " contains hard-coded CJK player copy",
					CJK.matcher(source).find());

			Matcher matcher = MESSAGE_KEY.matcher(source);
			boolean foundKey = false;
			while (matcher.find()) {
				foundKey = true;
				String key = matcher.group(1);
				assertTrue("Missing English player-path key: " + key,
						english.containsKey(key));
				assertTrue("Missing Chinese player-path key: " + key,
						chinese.containsKey(key));
			}
			assertTrue(path + " must reference localized raid copy", foundKey);
		}
	}

	private static Path path(String relative) {
		return Paths.get(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon")
				.resolve(relative);
	}
}
