package com.shatteredpixel.shatteredpixeldungeon.messages;

import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

public class BukovLocalizationParityTest {

	private static final String[] BUNDLES = {
			"bukov_entry",
			"bukov_raid",
			"bukov_economy"
	};

	@Test
	public void englishAndChineseBundlesKeepKeysValuesAndPlaceholdersInSync() throws IOException {
		for (String bundle : BUNDLES) {
			Properties english = BukovLocalizationTestSupport.load(bundle, "");
			Properties chinese = BukovLocalizationTestSupport.load(bundle, "_zh");
			BukovLocalizationTestSupport.assertKeyValueAndPlaceholderParity(
					bundle, english, chinese);
		}
	}
}
