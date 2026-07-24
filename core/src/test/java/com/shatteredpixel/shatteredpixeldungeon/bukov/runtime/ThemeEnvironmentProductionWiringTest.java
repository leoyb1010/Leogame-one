package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class ThemeEnvironmentProductionWiringTest {

	@Test
	public void existingFixedStepWorldConsumesEveryEnvironmentRule()
			throws IOException {
		String world = source(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/bukov/runtime/"
						+ "BukovRealtimeWorld.java");
		assertTrue(world.contains(
				"movementMultiplier(heroTerrain())"));
		assertTrue(world.contains(
				"movementNoiseRadius(heroTerrain())"));
		assertTrue(world.contains(
				"reloadDurationMultiplier(heroTerrain())"));
		assertTrue(world.contains(
				"medicalDurationMultiplier(heroTerrain())"));
		assertTrue(world.contains(
				".enemyHearingMultiplier("));
		assertTrue(world.contains(
				".enemySightMultiplier("));

		String theme = source(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/bukov/levels/"
						+ "ThemeDefinition.java");
		assertTrue(theme.contains(
				"environmentRules.reinforcementIntervalMultiplier"));
	}

	@Test
	public void restoreResolvesRulesFromSavedThemeWithoutNewSaveState()
			throws IOException {
		String level = source(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/bukov/levels/"
						+ "BukovLevel.java");
		assertTrue(level.contains(
				"ThemeDefinition theme = themeForId(raidLayout.themeId)"));
		assertTrue(level.contains(
				"themeId == null || themeId.isEmpty() ? \"fog_depot\""));
	}

	private static String source(String path) throws IOException {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
