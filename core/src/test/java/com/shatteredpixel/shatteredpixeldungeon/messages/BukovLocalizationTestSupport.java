package com.shatteredpixel.shatteredpixeldungeon.messages;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

final class BukovLocalizationTestSupport {

	private static final Pattern CJK = Pattern.compile(
			"[\\u2E80-\\u9FFF\\uF900-\\uFAFF\\u3040-\\u30FF\\uAC00-\\uD7AF]");
	private static final Pattern PRINTF_PLACEHOLDER = Pattern.compile(
			"%(?:(\\d+)\\$)?[-#+ 0,(<]*\\d*(?:\\.\\d+)?([a-zA-Z])");
	private static final Pattern MESSAGE_FORMAT_PLACEHOLDER = Pattern.compile(
			"\\{(\\d+)(?:,[^}]*)?}");

	private BukovLocalizationTestSupport() {
	}

	static Properties load(String bundle, String localeSuffix) throws IOException {
		Path path = Paths.get("src/main/assets/messages")
				.resolve(bundle)
				.resolve(bundle + localeSuffix + ".properties");
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			properties.load(reader);
		}
		return properties;
	}

	static void assertKeyValueAndPlaceholderParity(
			String bundle, Properties english, Properties chinese) {
		assertFalse(bundle + " English bundle must not be empty", english.isEmpty());
		assertEquals(bundle + " English/Chinese keys differ",
				english.stringPropertyNames(), chinese.stringPropertyNames());

		for (String key : english.stringPropertyNames()) {
			String englishValue = english.getProperty(key);
			String chineseValue = chinese.getProperty(key);
			assertFalse(bundle + " English value is blank: " + key, englishValue.trim().isEmpty());
			assertFalse(bundle + " Chinese value is blank: " + key, chineseValue.trim().isEmpty());
			assertEquals(bundle + " placeholder signature differs: " + key,
					placeholderSignature(englishValue), placeholderSignature(chineseValue));
		}
	}

	static void assertEnglishContainsNoCjk(
			String bundle, Properties english, Map<String, String> exactAllowlist) {
		for (Map.Entry<String, String> allowed : exactAllowlist.entrySet()) {
			assertEquals(bundle + " allowlisted bilingual value changed: " + allowed.getKey(),
					allowed.getValue(), english.getProperty(allowed.getKey()));
		}

		for (String key : english.stringPropertyNames()) {
			String value = english.getProperty(key);
			Matcher matcher = CJK.matcher(value);
			if (!matcher.find()) continue;

			assertTrue(bundle + " unexpected CJK in English value: " + key,
					exactAllowlist.containsKey(key));
			assertEquals(bundle + " allowlisted bilingual value changed: " + key,
					exactAllowlist.get(key), value);
		}
	}

	static void assertContainsCjk(String bundle, Properties properties) {
		boolean found = false;
		for (String key : properties.stringPropertyNames()) {
			String value = properties.getProperty(key);
			if (CJK.matcher(value).find()) {
				found = true;
				break;
			}
		}
		assertTrue(bundle + " Chinese bundle contains no CJK text", found);
	}

	private static List<String> placeholderSignature(String value) {
		List<String> signature = new ArrayList<>();
		Matcher printf = PRINTF_PLACEHOLDER.matcher(value);
		int implicitIndex = 1;
		while (printf.find()) {
			String explicitIndex = printf.group(1);
			int index = explicitIndex == null ? implicitIndex++ : Integer.parseInt(explicitIndex);
			signature.add("printf:" + index + ":"
					+ printf.group(2).toLowerCase(Locale.ROOT));
		}

		Matcher messageFormat = MESSAGE_FORMAT_PLACEHOLDER.matcher(value);
		while (messageFormat.find()) {
			signature.add("message-format:" + messageFormat.group(1));
		}
		Collections.sort(signature);
		return signature;
	}
}
