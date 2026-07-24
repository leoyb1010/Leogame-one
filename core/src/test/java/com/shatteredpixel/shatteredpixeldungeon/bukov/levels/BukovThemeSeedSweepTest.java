package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import org.junit.Test;

import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyArchetypeDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyArchetypeRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyRole;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyTier;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.FirstRaidEnemySpawnDirector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** Six themes share one validated generation and extraction contract. */
public class BukovThemeSeedSweepTest {

	private static final long[] SEEDS = {
			1L, 7L, 42L, 97L, 701L,
			991L, 1776L, 4099L, 8191L, 10007L,
			22039L, 44563L, 77899L, 991177L, 884422L,
			1357911L, 2468021L, 3141592L, 11235813L, 2147483647L
	};

	@Test
	public void everyThemePassesTwentyDeterministicSeeds() throws IOException {
		ThemeRegistry registry = themes();
		assertEquals(6, registry.all().size());

		for (ThemeDefinition theme : registry.all()) {
			for (long seed : SEEDS) {
				BukovRaidLayout first =
						BukovZonePlanner.generateFirstRaid(seed, theme);
				BukovRaidLayout second =
						BukovZonePlanner.generateFirstRaid(seed, theme);
				RaidMapValidator.Result result = RaidMapValidator.validate(first);
				assertTrue(
						theme.id + " seed " + seed + ": "
								+ result.failure + " " + result.reason,
						result.valid);
				assertEquals(theme.id, first.themeId);
				assertEquals(3, first.extractions.size());
				assertEquals(3, first.routes.size());
				assertEquals(first.marks.get(0).roomId(),
						second.marks.get(0).roomId());
				assertEquals(first.extractions.get(2).availableFromSeconds,
						second.extractions.get(2).availableFromSeconds, 0f);
			}
		}
	}

	@Test
	public void sixThemesHaveIndependentCompositionDefinitions()
			throws IOException {
		ThemeRegistry registry = themes();
		Set<String> names = new HashSet<>();
		Set<String> palettes = new HashSet<>();
		Set<String> compositionFingerprints = new HashSet<>();
		Set<String> visualGrammars = new HashSet<>();
		for (ThemeDefinition theme : registry.all()) {
			assertTrue(names.add(theme.name));
			assertTrue(palettes.add(
					theme.primaryColor + ":" + theme.secondaryColor));
			assertEquals(5, theme.roomWeights().size());
			assertEquals(5, theme.lootWeights().size());
			assertEquals(13, theme.enemyWeights().size());
			assertEquals(3, theme.coverCombination().size());
			compositionFingerprints.add(
					theme.riskMultiplier + ":"
							+ theme.roomWeights() + ":"
							+ theme.lootWeights() + ":"
							+ theme.enemyWeights() + ":"
							+ theme.coverCombination());
			visualGrammars.add(
					theme.floorPattern + ":"
							+ theme.wallDecoModulo + ":"
							+ theme.coverClusters);
		}
		assertEquals(6, compositionFingerprints.size());
		assertEquals("each theme needs a unique spatial visual grammar",
				6, visualGrammars.size());
		assertEquals(
				new HashSet<>(Arrays.asList(
						"雾港回收区",
						"锈蚀工场",
						"沉水通道",
						"荒草货场",
						"冷库环线",
						"封存实验层")),
				names);
		assertFalse(registry.require("fog_depot")
				.coverCombination().isEmpty());
		assertNotEquals(
				registry.require("fog_depot").riskMultiplier,
				registry.require("sealed_lab").riskMultiplier,
				0f);
	}

	@Test
	public void everyThemeCoversTheEntireLiveEnemyRoster()
			throws IOException {
		EnemyArchetypeRegistry enemies = new EnemyArchetypeRegistry();
		enemies.loadJson(new String(
				Files.readAllBytes(Paths.get(
						"src/main/assets/bukov/content/enemies.json")),
				StandardCharsets.UTF_8));
		Set<String> roster = new HashSet<>();
		for (EnemyArchetypeDefinition enemy : enemies.all()) {
			roster.add(enemy.id);
		}
		assertEquals(13, roster.size());
		for (ThemeDefinition theme : themes().all()) {
			assertEquals(theme.id, roster, theme.enemyWeights().keySet());
		}
	}

	@Test
	public void seededThemeSelectionIsStableAndCoversTheRegistry()
			throws IOException {
		ThemeRegistry registry = themes();
		Set<String> selected = new HashSet<>();
		long[] boundarySeeds = {
				Long.MIN_VALUE, Long.MIN_VALUE + 1L,
				-256L, -7L, -1L, 0L, 1L, 7L, 256L,
				Long.MAX_VALUE - 1L, Long.MAX_VALUE
		};
		for (long seed : boundarySeeds) {
			assertEquals(
					registry.forSeed(seed).id,
					registry.forSeed(seed).id);
		}
		for (long seed = -256L; seed < 256L; seed++) {
			String first = registry.forSeed(seed).id;
			assertEquals(first, registry.forSeed(seed).id);
			selected.add(first);
		}
		assertEquals(6, selected.size());
	}

	@Test
	public void nonNegativeIndexHandlesLongMinimumWithoutOverflow() {
		for (int size = 1; size <= 16; size++) {
			int minimum = ThemeRegistry.nonNegativeIndex(
					Long.MIN_VALUE, size);
			int negative = ThemeRegistry.nonNegativeIndex(-1L, size);
			int maximum = ThemeRegistry.nonNegativeIndex(
					Long.MAX_VALUE, size);
			assertTrue(minimum >= 0 && minimum < size);
			assertTrue(negative >= 0 && negative < size);
			assertTrue(maximum >= 0 && maximum < size);
		}
	}

	@Test
	public void sameSeedProducesSixLiveCompositionProfiles()
			throws IOException {
		ThemeRegistry registry = themes();
		Set<String> roomProfiles = new HashSet<>();
		Set<String> lootProfiles = new HashSet<>();
		for (ThemeDefinition theme : registry.all()) {
			BukovRaidLayout layout =
					BukovZonePlanner.generateFirstRaid(424242L, theme);
			StringBuilder rooms = new StringBuilder();
			for (BukovRaidLayout.Mark mark : layout.marks) {
				rooms.append(mark.zone).append('|');
			}
			StringBuilder loot = new StringBuilder();
			for (BukovRaidLayout.LootAnchor anchor : layout.lootAnchors) {
				loot.append(anchor.lootTableId).append('|');
			}
			roomProfiles.add(rooms.toString());
			lootProfiles.add(loot.toString());
			assertTrue(RaidMapValidator.validate(layout).valid);
		}
		assertEquals("roomWeights must affect semantic assignment",
				6, roomProfiles.size());
		assertTrue("lootWeights must affect live container tables",
				lootProfiles.size() >= 3);
	}

	@Test
	public void enemyAndRiskWeightsChangeRuntimePressure()
			throws IOException {
		ThemeRegistry registry = themes();
		ThemeDefinition fog = registry.require("fog_depot");
		ThemeDefinition sealed = registry.require("sealed_lab");
		assertEquals(12,
				fog.adjustedEnemyWeight("scavenger_gunner", 10));
		assertEquals(8,
				sealed.adjustedEnemyWeight("scavenger_gunner", 10));
		assertTrue(sealed.pressureAdjustedSeconds(12f)
				< fog.pressureAdjustedSeconds(12f));

		EnemyArchetypeDefinition gunner =
				enemy("scavenger_gunner", 10);
		EnemyArchetypeDefinition guard =
				enemy("iron_clasp_guard", 10);
		FirstRaidEnemySpawnDirector.Context context =
				new FirstRaidEnemySpawnDirector.Context(
						600f, false, 5, false, false, false);
		boolean differs = false;
		for (long key = 0L; key < 256L; key++) {
			EnemyArchetypeDefinition fogChoice =
					FirstRaidEnemySpawnDirector.select(
							Arrays.asList(gunner, guard),
							context,
							id -> 0,
							key,
							definition -> fog.adjustedEnemyWeight(
									definition.id,
									definition.spawnWeight));
			EnemyArchetypeDefinition sealedChoice =
					FirstRaidEnemySpawnDirector.select(
							Arrays.asList(gunner, guard),
							context,
							id -> 0,
							key,
							definition -> sealed.adjustedEnemyWeight(
									definition.id,
									definition.spawnWeight));
			if (!fogChoice.id.equals(sealedChoice.id)) {
				differs = true;
				break;
			}
		}
		assertTrue("enemyWeights must affect live weighted selection", differs);
	}

	private static EnemyArchetypeDefinition enemy(String id, int weight) {
		EnemyArchetypeDefinition definition =
				new EnemyArchetypeDefinition();
		definition.id = id;
		definition.name = id;
		definition.tier = EnemyTier.COMMON;
		definition.role = EnemyRole.RANGED_SKIRMISHER;
		definition.hostClassHint = "Rat";
		definition.abilities = new String[]{"TEST"};
		definition.health = 10;
		definition.movementSpeed = 1f;
		definition.perceptionRange = 5f;
		definition.engagementRange = 3f;
		definition.minimumDamage = 1;
		definition.maximumDamage = 2;
		definition.spawnWeight = weight;
		definition.minimumSpawnSeconds = 0f;
		definition.minimumDistanceFromSpawnRooms = 0;
		definition.maximumActive = 10;
		definition.firstRaidMinimumSeconds = 0f;
		definition.firstRaidMaximumActive = 10;
		return definition;
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
