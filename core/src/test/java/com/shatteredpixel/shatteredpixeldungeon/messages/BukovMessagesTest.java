package com.shatteredpixel.shatteredpixeldungeon.messages;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BukovMessagesTest {

	@Test
	public void headlessModelsCanReadAndFormatEnglishBundles() {
		assertEquals(
				"ESCAPE FROM BUKOV",
				BukovMessages.get("bukov.entry.brand.english_title"));
		assertEquals(
				"HP 63/100",
				BukovMessages.get(
						"bukov.raid.hud.health_format",
						63,
						100));
		assertEquals(
				"Contracts 2/5 · Areas 3/6",
				BukovMessages.get(
						"bukov.economy.hub.career_summary",
						2,
						5,
						3,
						6));
	}
}
