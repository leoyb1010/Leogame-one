package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AmmoRegistryTest {

	@Test
	public void loadsDefinitionsAndChecksCompatibilityByCaliber() {
		AmmoRegistry registry = new AmmoRegistry();
		registry.loadJson(json(definition("standard", "STANDARD", "9x19")));

		assertEquals(1, registry.all().size());
		assertEquals(AmmoVariant.STANDARD, registry.require("standard").variant);
		assertTrue(registry.compatible("standard", "9x19"));
		assertFalse(registry.compatible("standard", "5.56x45"));
		assertFalse(registry.compatible("missing", "9x19"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsDuplicateDefinitionIds() {
		String duplicate = definition("duplicate", "STANDARD", "9x19");
		new AmmoRegistry().loadJson(json(duplicate + "," + duplicate));
	}

	@Test
	public void invalidReloadKeepsLastValidDefinitions() {
		AmmoRegistry registry = new AmmoRegistry();
		registry.loadJson(json(definition("stable", "STANDARD", "9x19")));

		try {
			registry.loadJson("{\"schemaVersion\":1,\"ammunition\":[]}");
		} catch (IllegalStateException expected) {
			// expected
		}

		assertEquals("stable", registry.require("stable").id);
	}

	private static String json(String definitions) {
		return "{\"schemaVersion\":1,\"ammunition\":[" + definitions + "]}";
	}

	private static String definition(String id, String variant, String caliber) {
		return "{"
				+ "\"id\":\"" + id + "\","
				+ "\"name\":\"Test\","
				+ "\"variant\":\"" + variant + "\","
				+ "\"caliber\":\"" + caliber + "\","
				+ "\"damageMultiplier\":1,"
				+ "\"penetrationMultiplier\":1,"
				+ "\"noiseMultiplier\":1,"
				+ "\"weightKg\":0.01,"
				+ "\"value\":10"
				+ "}";
	}
}
