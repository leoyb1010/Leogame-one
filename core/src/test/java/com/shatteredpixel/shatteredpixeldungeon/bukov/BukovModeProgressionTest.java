package com.shatteredpixel.shatteredpixeldungeon.bukov;

import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BukovModeProgressionTest {

	@After
	public void resetMode() {
		BukovMode.leave();
	}

	@Test
	public void deploymentUsesExplicitUnlockedSelectedRegion() {
		BukovMode.prepareUnlockedMaps(Arrays.asList(
				"legacy_first_level",
				"fog_depot",
				"rust_workshop"));
		BukovMode.prepareSelectedMap("rust_workshop");

		assertEquals(
				Arrays.asList("fog_depot", "rust_workshop"),
				BukovMode.unlockedRaidThemes());
		assertEquals("rust_workshop", BukovMode.selectedRaidTheme());
	}

	@Test
	public void invalidLegacySelectionFallsBackToStartingRegion() {
		BukovMode.prepareUnlockedMaps(
				Collections.singletonList("legacy_first_level"));
		BukovMode.prepareSelectedMap("legacy_first_level");

		assertEquals(
				Collections.singletonList("fog_depot"),
				BukovMode.unlockedRaidThemes());
		assertEquals("fog_depot", BukovMode.selectedRaidTheme());
	}
}
