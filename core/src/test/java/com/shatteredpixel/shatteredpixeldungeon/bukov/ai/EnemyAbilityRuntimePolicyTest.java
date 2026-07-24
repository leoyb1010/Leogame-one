package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EnemyAbilityRuntimePolicyTest {

	@Test
	public void allThirteenArchetypesOnlyUseRegisteredRuntimeAbilities()
			throws Exception {
		String json = new String(
				Files.readAllBytes(Paths.get(
						"src/main/assets/bukov/content/enemies.json")),
				StandardCharsets.UTF_8);
		EnemyArchetypeRegistry registry = new EnemyArchetypeRegistry();
		registry.loadJson(json);
		Set<String> unique = new HashSet<>();

		for (EnemyArchetypeDefinition definition : registry.all()) {
			for (String ability : definition.abilities) {
				assertNotNull(
						definition.id + " has no runtime use for " + ability,
						EnemyAbilityRuntimePolicy.useFor(ability));
				unique.add(ability);
			}
		}

		assertEquals(13, registry.all().size());
		assertEquals(18, unique.size());
	}

	@Test
	public void weakTargetPressureOnlyAppliesAtLowHealth() {
		EnemyArchetypeDefinition pressure = definition(
				"PRESS_WEAK_TARGET");
		assertEquals(
				10,
				EnemyAbilityRuntimePolicy.damageAgainstTarget(
						pressure, 10, 60, 100));
		assertEquals(
				12,
				EnemyAbilityRuntimePolicy.damageAgainstTarget(
						pressure, 10, 35, 100));
		assertEquals(
				10,
				EnemyAbilityRuntimePolicy.damageAgainstTarget(
						definition("USE_COVER"), 10, 20, 100));
	}

	@Test
	public void investigatorHasGameplayHearingAdvantage() {
		assertTrue(
				EnemyAbilityRuntimePolicy.hearingMultiplier(
						definition("INVESTIGATE_SOUND")) > 1f);
		assertEquals(
				1f,
				EnemyAbilityRuntimePolicy.hearingMultiplier(
						definition("SHORT_DASH")),
				0f);
	}

	@Test
	public void cornerAndCoverTagsSelectExistingTacticalHandlers() {
		EnemyArchetypeDefinition corner = definition("CORNER_AMBUSH");
		corner.role = EnemyRole.RANGED_SKIRMISHER;
		assertEquals(
				RealtimeEnemyTactics.Profile.RUSHER,
				RealtimeEnemyTactics.profileFor(corner));

		EnemyArchetypeDefinition cover = definition("USE_COVER");
		cover.role = EnemyRole.RANGED_SKIRMISHER;
		assertEquals(
				RealtimeEnemyTactics.Profile.SUPPRESSOR,
				RealtimeEnemyTactics.profileFor(cover));
	}

	@Test(expected = IllegalArgumentException.class)
	public void definitionRejectsAbilityWithoutRuntimeHandler() {
		EnemyArchetypeDefinition unknown = validDefinition();
		unknown.abilities = new String[]{"UNWIRED_CONTENT_TAG"};
		unknown.validate();
	}

	@Test(expected = IllegalArgumentException.class)
	public void whiteLineCannotLoseARequiredPhaseAbility() {
		EnemyArchetypeDefinition boss = validDefinition();
		boss.tier = EnemyTier.BOSS;
		boss.role = EnemyRole.OPTIONAL_BOSS;
		boss.spawnWeight = 0;
		boss.bossArenaOnly = true;
		boss.optionalRouteOnly = true;
		boss.abilities = new String[]{
				"UMBRELLA_SHIELD",
				"FOG_LAMP_OVERLOAD"
		};
		boss.validate();
	}

	private static EnemyArchetypeDefinition definition(String ability) {
		EnemyArchetypeDefinition definition =
				new EnemyArchetypeDefinition();
		definition.abilities = new String[]{ability};
		return definition;
	}

	private static EnemyArchetypeDefinition validDefinition() {
		EnemyArchetypeDefinition definition =
				new EnemyArchetypeDefinition();
		definition.id = "test_enemy";
		definition.name = "Test";
		definition.tier = EnemyTier.COMMON;
		definition.role = EnemyRole.MELEE_RUSHER;
		definition.hostClassHint = "Rat";
		definition.abilities = new String[]{"SHORT_DASH"};
		definition.health = 10;
		definition.movementSpeed = 2f;
		definition.perceptionRange = 6f;
		definition.engagementRange = 1f;
		definition.minimumDamage = 1;
		definition.maximumDamage = 2;
		definition.spawnWeight = 1;
		definition.minimumSpawnSeconds = 0f;
		definition.minimumDistanceFromSpawnRooms = 1;
		definition.maximumActive = 1;
		definition.firstRaidMinimumSeconds = 0f;
		definition.firstRaidMaximumActive = 1;
		return definition;
	}
}
