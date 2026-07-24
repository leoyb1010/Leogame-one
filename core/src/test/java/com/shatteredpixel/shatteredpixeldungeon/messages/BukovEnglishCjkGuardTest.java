package com.shatteredpixel.shatteredpixeldungeon.messages;

import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class BukovEnglishCjkGuardTest {

	private static final String[] BUNDLES = {
			"bukov_entry",
			"bukov_raid",
			"bukov_economy"
	};

	@Test
	public void englishBundlesContainNoCjkOutsideExactBilingualAllowlist() throws IOException {
		Map<String, String> entryAllowlist = new HashMap<>();
		entryAllowlist.put("bukov.entry.reserved.bilingual_logo",
				"ESCAPE FROM BUKOV / 逃离布科夫");

		for (String bundle : BUNDLES) {
			Properties english = BukovLocalizationTestSupport.load(bundle, "");
			Map<String, String> allowlist = bundle.equals("bukov_entry")
					? entryAllowlist
					: Collections.emptyMap();
			BukovLocalizationTestSupport.assertEnglishContainsNoCjk(
					bundle, english, allowlist);
		}
	}

	@Test
	public void chineseBundlesContainCjkText() throws IOException {
		for (String bundle : BUNDLES) {
			BukovLocalizationTestSupport.assertContainsCjk(
					bundle, BukovLocalizationTestSupport.load(bundle, "_zh"));
		}
	}
}
