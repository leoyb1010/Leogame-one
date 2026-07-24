package com.shatteredpixel.shatteredpixeldungeon.messages;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovEconomyLocalizationWiringTest {

	private static final Path UI_ROOT = Paths.get(
			"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui");
	private static final Pattern MESSAGE_KEY = Pattern.compile(
			"\"(bukov\\.economy\\.[^\"]+)\"");
	private static final Pattern CJK = Pattern.compile(
			"[\\u2E80-\\u9FFF\\uF900-\\uFAFF\\u3040-\\u30FF\\uAC00-\\uD7AF]");
	private static final Iterable<String> OWNED_SOURCES = Arrays.asList(
			"WndBukovHub.java",
			"WndBukovVendor.java",
			"WndBukovServices.java",
			"WndBukovSettlement.java",
			"WndBukovRaidModeSelection.java",
			"BukovHubViewModel.java",
			"BukovVendorViewModel.java",
			"BukovServicesViewModel.java",
			"BukovSettlementViewModel.java",
			"BukovRaidModeSelectionViewModel.java");

	@Test
	public void economyUiMessageReferencesExistInBothLocales() throws IOException {
		Properties english =
				BukovLocalizationTestSupport.load("bukov_economy", "");
		Properties chinese =
				BukovLocalizationTestSupport.load("bukov_economy", "_zh");
		Set<String> referencedKeys = new LinkedHashSet<>();

		for (String sourceName : OWNED_SOURCES) {
			String source = readSource(sourceName);
			assertTrue(
					sourceName + " must use the unambiguous Bukov message wrapper",
					source.contains("BukovMessages.get("));
			Matcher matcher = MESSAGE_KEY.matcher(source);
			while (matcher.find()) {
				String key = matcher.group(1);
				if (!key.endsWith("_") && !key.endsWith(".")) {
					referencedKeys.add(key);
				}
			}
		}

		assertFalse("Economy UI must reference localized copy",
				referencedKeys.isEmpty());
		for (String key : referencedKeys) {
			assertTrue("Missing English economy UI key: " + key,
					english.containsKey(key));
			assertTrue("Missing Chinese economy UI key: " + key,
					chinese.containsKey(key));
		}
	}

	@Test
	public void economyUiSourcesContainNoCjkPlayerCopy() throws IOException {
		for (String sourceName : OWNED_SOURCES) {
			assertFalse(
					sourceName + " contains hard-coded CJK text",
					CJK.matcher(readSource(sourceName)).find());
		}
	}

	private static String readSource(String sourceName) throws IOException {
		return new String(
				Files.readAllBytes(UI_ROOT.resolve(sourceName)),
				StandardCharsets.UTF_8);
	}
}
