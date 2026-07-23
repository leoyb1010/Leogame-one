package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class FirstRaidEnemySpawnDirectorTest {

	private static final FirstRaidEnemySpawnDirector.ActiveCounts NONE =
			definitionId -> 0;

	@Test
	public void firstTwoMinutesAllowOnlyAuthoredLowThreatGunner() {
		EnemyArchetypeDefinition gunner = definition(
				"gunner",
				EnemyTier.COMMON,
				38,
				0f,
				0f,
				false,
				false
		);
		EnemyArchetypeDefinition rusher = definition(
				"rusher",
				EnemyTier.COMMON,
				30,
				120f,
				120f,
				false,
				false
		);
		FirstRaidEnemySpawnDirector.Context context = context(
				60f,
				true,
				false,
				false,
				false
		);

		assertTrue(FirstRaidEnemySpawnDirector.eligible(
				gunner, context, NONE
		));
		assertFalse(FirstRaidEnemySpawnDirector.eligible(
				rusher, context, NONE
		));
		assertSame(gunner, FirstRaidEnemySpawnDirector.select(
				Arrays.asList(gunner, rusher),
				context,
				NONE,
				123L
		));
	}

	@Test
	public void playerFovAndMandatoryRouteProtectSpawn() {
		EnemyArchetypeDefinition elite = definition(
				"elite",
				EnemyTier.ELITE,
				5,
				360f,
				480f,
				true,
				false
		);

		assertFalse(FirstRaidEnemySpawnDirector.eligible(
				elite,
				context(600f, true, true, false, false),
				NONE
		));
		assertFalse(FirstRaidEnemySpawnDirector.eligible(
				elite,
				context(600f, true, false, true, false),
				NONE
		));
		assertTrue(FirstRaidEnemySpawnDirector.eligible(
				elite,
				context(600f, true, false, false, false),
				NONE
		));
	}

	@Test
	public void firstRaidActiveCapPreventsPressureSpike() {
		EnemyArchetypeDefinition enemy = definition(
				"limited",
				EnemyTier.COMMON,
				20,
				0f,
				0f,
				false,
				false
		);
		enemy.maximumActive = 4;
		enemy.firstRaidMaximumActive = 1;

		assertFalse(FirstRaidEnemySpawnDirector.eligible(
				enemy,
				context(300f, true, false, false, false),
				id -> 1
		));
		assertTrue(FirstRaidEnemySpawnDirector.eligible(
				enemy,
				context(300f, false, false, false, false),
				id -> 1
		));
	}

	@Test
	public void bossIsExplicitOptionalArenaContentNotRandomSpawn() {
		EnemyArchetypeDefinition boss = definition(
				"boss",
				EnemyTier.BOSS,
				0,
				360f,
				480f,
				true,
				true
		);

		assertTrue(FirstRaidEnemySpawnDirector.eligible(
				boss,
				context(600f, true, false, false, true),
				NONE
		));
		assertNull(FirstRaidEnemySpawnDirector.select(
				Collections.singletonList(boss),
				context(600f, true, false, false, true),
				NONE,
				9L
		));
	}

	@Test
	public void equalInputProducesEqualWeightedSelection() {
		EnemyArchetypeDefinition first = definition(
				"first", EnemyTier.COMMON, 1, 0f, 0f, false, false
		);
		EnemyArchetypeDefinition second = definition(
				"second", EnemyTier.COMMON, 3, 0f, 0f, false, false
		);
		FirstRaidEnemySpawnDirector.Context context =
				context(300f, false, false, false, false);

		assertEquals(
				FirstRaidEnemySpawnDirector.select(
						Arrays.asList(first, second), context, NONE, 42L
				).id,
				FirstRaidEnemySpawnDirector.select(
						Arrays.asList(first, second), context, NONE, 42L
				).id
		);
	}

	private static FirstRaidEnemySpawnDirector.Context context(
			float seconds,
			boolean firstRaid,
			boolean inFov,
			boolean mandatoryRoute,
			boolean bossArena) {
		return new FirstRaidEnemySpawnDirector.Context(
				seconds,
				firstRaid,
				5,
				inFov,
				mandatoryRoute,
				bossArena
		);
	}

	private static EnemyArchetypeDefinition definition(
			String id,
			EnemyTier tier,
			int weight,
			float minimumSeconds,
			float firstRaidMinimumSeconds,
			boolean optional,
			boolean bossArena) {
		EnemyArchetypeDefinition definition =
				new EnemyArchetypeDefinition();
		definition.id = id;
		definition.name = id;
		definition.tier = tier;
		definition.role = tier == EnemyTier.BOSS
				? EnemyRole.OPTIONAL_BOSS
				: tier == EnemyTier.ELITE
				? EnemyRole.ELITE_COMMANDER
				: EnemyRole.RANGED_SKIRMISHER;
		definition.hostClassHint = "Rat";
		definition.abilities = new String[]{"TEST"};
		definition.health = 10;
		definition.movementSpeed = 1f;
		definition.perceptionRange = 5f;
		definition.engagementRange = 3f;
		definition.minimumDamage = 1;
		definition.maximumDamage = 2;
		definition.spawnWeight = weight;
		definition.minimumSpawnSeconds = minimumSeconds;
		definition.minimumDistanceFromSpawnRooms = 2;
		definition.maximumActive = 4;
		definition.firstRaidMinimumSeconds = firstRaidMinimumSeconds;
		definition.firstRaidMaximumActive = 2;
		definition.optionalRouteOnly = optional;
		definition.bossArenaOnly = bossArena;
		return definition;
	}
}
