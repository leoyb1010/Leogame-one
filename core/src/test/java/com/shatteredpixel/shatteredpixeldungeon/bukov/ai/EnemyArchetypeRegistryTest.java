package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EnemyArchetypeRegistryTest {

	@Test
	public void loadsNineCommonThreeEliteAndWhiteLineBoss()
			throws IOException {
		String json = new String(
				Files.readAllBytes(Paths.get(
						"src/main/assets/bukov/content/enemies.json"
				)),
				StandardCharsets.UTF_8
		);
		EnemyArchetypeRegistry registry = new EnemyArchetypeRegistry();
		registry.loadJson(json);

		int common = 0;
		int elite = 0;
		int boss = 0;
		int firstCommon = 0;
		int firstElite = 0;
		int firstBoss = 0;
		for (EnemyArchetypeDefinition definition : registry.all()) {
			if (definition.tier == EnemyTier.COMMON) common++;
			if (definition.tier == EnemyTier.ELITE) elite++;
			if (definition.tier == EnemyTier.BOSS) boss++;
			if (FirstRaidEnemySpawnDirector.firstRaidRoster(
					definition.id)) {
				if (definition.tier == EnemyTier.COMMON) firstCommon++;
				if (definition.tier == EnemyTier.ELITE) firstElite++;
				if (definition.tier == EnemyTier.BOSS) firstBoss++;
			}
		}
		assertEquals(9, common);
		assertEquals(3, elite);
		assertEquals(1, boss);
		assertEquals(4, firstCommon);
		assertEquals(1, firstElite);
		assertEquals(1, firstBoss);
		assertEquals(
				EnemyRole.SCOUT_ALARM,
				registry.require("sensor_doll").role
		);
		assertEquals(
				0,
				registry.require("boss_white_line").spawnWeight
		);
		assertTrue(
				registry.require("boss_white_line").optionalRouteOnly
		);
		assertEquals(
				3,
				registry.require("boss_white_line").abilities.length
		);
	}

	@Test(expected = IllegalStateException.class)
	public void rejectsIncompleteFirstRaidRoster() {
		new EnemyArchetypeRegistry().loadJson(
				"{\"schemaVersion\":1,\"enemies\":[]}"
		);
	}
}
