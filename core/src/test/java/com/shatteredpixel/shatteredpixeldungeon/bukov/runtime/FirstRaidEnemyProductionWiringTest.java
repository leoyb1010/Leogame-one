package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyArchetypeDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyRole;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyTier;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.RealtimeDamage;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.armor.ArmorCatalog;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.armor.RealtimeArmorState;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FirstRaidEnemyProductionWiringTest {

	@Test
	public void fourCommonRolesEliteAndBossHaveReadableIdentity() {
		assertEquals(
				BukovMessages.get(
						"bukov.raid.runtime.enemy_role_skirmisher"),
				label(EnemyRole.RANGED_SKIRMISHER));
		assertEquals(
				BukovMessages.get(
						"bukov.raid.runtime.enemy_role_rusher"),
				label(EnemyRole.MELEE_RUSHER));
		assertEquals(
				BukovMessages.get(
						"bukov.raid.runtime.enemy_role_armored"),
				label(EnemyRole.ARMORED_SUPPRESSOR));
		assertEquals(
				BukovMessages.get(
						"bukov.raid.runtime.enemy_role_scout"),
				label(EnemyRole.SCOUT_ALARM));
		assertEquals(
				BukovMessages.get(
						"bukov.raid.runtime.enemy_role_commander"),
				label(EnemyRole.ELITE_COMMANDER));
		assertEquals(
				BukovMessages.get(
						"bukov.raid.runtime.enemy_role_white_line"),
				label(EnemyRole.OPTIONAL_BOSS));
	}

	@Test
	public void armoredGuardConsumesPenetrationInsteadOfBeingCosmetic() {
		EnemyArchetypeDefinition armored =
				new EnemyArchetypeDefinition();
		armored.abilities = new String[]{"ARMORED_FRONT"};
		assertNotNull(BukovRealtimeWorld.createEnemyArmor(armored));
		armored.abilities = new String[]{"USE_COVER"};
		assertNull(BukovRealtimeWorld.createEnemyArmor(armored));

		float lowPen = resolveFreshEnemyArmor(100f, 8f);
		float mediumPen = resolveFreshEnemyArmor(100f, 18f);
		float highPen = resolveFreshEnemyArmor(100f, 30f);

		assertTrue(lowPen < mediumPen);
		assertTrue(mediumPen < highPen);
		assertTrue(highPen < 100f);
		assertEquals(
				100f,
				BukovRealtimeWorld.resolveEnemyArmor(
						null,
						100f,
						8f,
						RealtimeDamage.HitZone.CORE),
				0.0001f);

		RealtimeArmorState durable = RealtimeArmorState.fresh(
				ArmorCatalog.require("patrol_vest"));
		BukovRealtimeWorld.resolveEnemyArmor(
				durable,
				100f,
				8f,
				RealtimeDamage.HitZone.CORE);
		assertTrue(durable.durabilityFraction() < 1f);
	}

	@Test
	public void deathLootIsBoundedDeterministicAndEliteGuaranteed() {
		assertTrue(BukovRealtimeWorld.enemyDropsLoot(
				9L, 7, EnemyTier.ELITE));
		assertFalse(BukovRealtimeWorld.enemyDropsLoot(
				9L, 7, EnemyTier.BOSS));
		int commonDrops = 0;
		for (int stableId = 0; stableId < 1000; stableId++) {
			boolean first = BukovRealtimeWorld.enemyDropsLoot(
					991177L, stableId, EnemyTier.COMMON);
			boolean second = BukovRealtimeWorld.enemyDropsLoot(
					991177L, stableId, EnemyTier.COMMON);
			assertEquals(first, second);
			if (first) commonDrops++;
		}
		assertTrue(commonDrops >= 300);
		assertTrue(commonDrops <= 400);
	}

	@Test
	public void worldConsumesAlarmArmorMilestoneBossPulseAndDeathLoot()
			throws IOException {
		String source = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/bukov/runtime/"
								+ "BukovRealtimeWorld.java")),
				StandardCharsets.UTF_8);
		assertTrue(source.contains("selectFirstRaidMilestone("));
		assertTrue(source.contains("CALL_INVESTIGATORS"));
		assertTrue(source.contains("resolveEnemyArmor("));
		assertTrue(source.contains("updateWhiteLineOffense("));
		assertTrue(source.contains("releaseEnemyLoot("));
		assertTrue(source.contains("bypassWhiteLineForExtraction("));
		assertTrue(source.contains(
				"InitialEnemyRosterPolicy.shouldPopulate("));
		assertTrue(source.contains(
				"SpawnVisibility.VISIBLE_REQUIRED"));
		assertTrue(source.contains(
				"attemptVisibleInitialContactSpawn()"));
		assertTrue(source.contains(
				"raid.session().markInitialEnemySpawnCompleted();"));
		assertTrue(source.contains(
				"mob.markOnboardingContact();"));
		assertTrue(source.contains(
				"InitialContactCombatPolicy.openingWarningSeconds("));
		assertTrue(source.contains(
				"InitialContactCombatPolicy.maximumDamage("));
		assertEquals(
				source.indexOf("SpawnVisibility.ANY_SAFE"),
				source.lastIndexOf("SpawnVisibility.ANY_SAFE"));
	}

	private static String label(EnemyRole role) {
		EnemyArchetypeDefinition definition =
				new EnemyArchetypeDefinition();
		definition.role = role;
		return BukovRealtimeWorld.enemyRoleLabel(definition);
	}

	private static float resolveFreshEnemyArmor(
			float damage,
			float penetration) {
		return BukovRealtimeWorld.resolveEnemyArmor(
				RealtimeArmorState.fresh(
						ArmorCatalog.require("patrol_vest")),
				damage,
				penetration,
				RealtimeDamage.HitZone.CORE);
	}
}
