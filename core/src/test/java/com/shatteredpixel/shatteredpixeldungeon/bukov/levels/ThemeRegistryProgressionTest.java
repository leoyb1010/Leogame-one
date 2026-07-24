package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ThemeRegistryProgressionTest {

	@Test
	public void seedSelectionNeverEscapesUnlockedThemeSet() {
		ThemeRegistry registry = new ThemeRegistry();
		registry.loadDefault();

		for (long seed = -100L; seed <= 100L; seed++) {
			String selected = registry.forSeed(
					seed,
					Arrays.asList(
							"fog_depot",
							"rust_workshop",
							"unknown_legacy_map")).id;
			assertTrue(
					selected,
					selected.equals("fog_depot")
							|| selected.equals("rust_workshop"));
		}
	}

	@Test
	public void emptyOrLegacyOnlyUnlocksFallBackToStartingMap() {
		ThemeRegistry registry = new ThemeRegistry();
		registry.loadDefault();

		assertEquals(
				"fog_depot",
				registry.forSeed(91L, Collections.<String>emptyList()).id);
		assertEquals(
				"fog_depot",
				registry.forSeed(
						91L,
						Collections.singletonList("legacy_first_level")).id);
	}
}
