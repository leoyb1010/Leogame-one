package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;

import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ThemeEnvironmentRulesTest {

	private static final long[] SEEDS = {
			1L, 7L, 42L, 97L, 701L,
			991L, 1776L, 4099L, 8191L, 10007L,
			22039L, 44563L, 77899L, 991177L, 884422L,
			1357911L, 2468021L, 3141592L, 11235813L, 2147483647L
	};

	@Test
	public void allSixThemesExposeUniqueBoundedRouteTradeoffs()
			throws IOException {
		ThemeRegistry registry = themes();
		Set<String> fingerprints = new HashSet<>();
		for (ThemeDefinition theme : registry.all()) {
			ThemeEnvironmentRules rules = theme.environmentRules;
			assertNotNull(rules);
			assertTrue(fingerprints.add(rules.fingerprint()));
			assertTrue(rules.surfaceMovementMultiplier >= 0.65f);
			assertTrue(rules.surfaceMovementMultiplier <= 1.05f);
			assertTrue(rules.surfaceReloadMultiplier >= 1f);
			assertTrue(rules.surfaceMedicalMultiplier >= 1f);
		}
		assertEquals(6, fingerprints.size());
	}

	@Test
	public void authoredRulesMatchEachThemesGameplayPromise()
			throws IOException {
		ThemeRegistry registry = themes();
		ThemeEnvironmentRules fog =
				registry.require("fog_depot").environmentRules;
		assertEquals(0.68f,
				fog.enemySightMultiplier(Terrain.WATER), 0f);
		assertEquals(1.35f,
				fog.enemyHearingMultiplier(Terrain.WATER), 0f);

		ThemeEnvironmentRules rust =
				registry.require("rust_workshop").environmentRules;
		assertEquals(5.5f,
				rust.movementNoiseRadius(Terrain.EMBERS), 0f);

		ThemeEnvironmentRules flood =
				registry.require("flooded_passage").environmentRules;
		assertEquals(0.72f,
				flood.movementMultiplier(Terrain.WATER), 0f);
		assertEquals(1f,
				flood.movementMultiplier(Terrain.EMPTY), 0f);

		ThemeEnvironmentRules yard =
				registry.require("overgrown_yard").environmentRules;
		assertEquals(1.25f,
				yard.enemySightMultiplier(
						Terrain.CUSTOM_DECO_EMPTY),
				0f);

		ThemeEnvironmentRules cold =
				registry.require("cold_storage").environmentRules;
		assertEquals(1.2f,
				cold.reloadDurationMultiplier(Terrain.EMPTY_SP), 0f);
		assertEquals(1.18f,
				cold.medicalDurationMultiplier(Terrain.EMPTY_SP), 0f);

		ThemeEnvironmentRules lab =
				registry.require("sealed_lab").environmentRules;
		assertEquals(4f,
				lab.movementNoiseRadius(Terrain.EMBERS), 0f);
		assertEquals(0.85f,
				lab.reinforcementIntervalMultiplier, 0f);
	}

	@Test
	public void rulesHaveNoHealthOrDamageMutationSurface() {
		for (Field field : ThemeEnvironmentRules.class.getDeclaredFields()) {
			String name = field.getName().toLowerCase();
			assertFalse(name.contains("damage"));
			assertFalse(name.contains("health"));
		}
	}

	@Test
	public void twentySeedsRetainMissionAndExtractionReachability()
			throws IOException {
		for (ThemeDefinition theme : themes().all()) {
			for (long seed : SEEDS) {
				BukovRaidLayout layout =
						BukovZonePlanner.generateFirstRaid(seed, theme);
				RaidMapValidator.Result result =
						RaidMapValidator.validate(layout);
				assertTrue(
						theme.id + " seed " + seed + ": "
								+ result.failure + " " + result.reason,
						result.valid);
				assertNotNull(layout.missionGate());
				assertEquals(3, layout.extractions.size());
				assertNotNull(layout.extraction("E01"));
			}
		}
	}

	private static ThemeRegistry themes() throws IOException {
		ThemeRegistry registry = new ThemeRegistry();
		registry.loadJson(new String(
				Files.readAllBytes(Paths.get(
						"src/main/assets/bukov/content/themes.json")),
				StandardCharsets.UTF_8));
		return registry;
	}
}
