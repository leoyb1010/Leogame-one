package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyArchetypeDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyRole;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyTier;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FirstRaidEnemyProductionWiringTest {

	@Test
	public void fourCommonRolesEliteAndBossHaveReadableIdentity() {
		assertEquals("游击射手", label(EnemyRole.RANGED_SKIRMISHER));
		assertEquals("突击近战", label(EnemyRole.MELEE_RUSHER));
		assertEquals("正面装甲", label(EnemyRole.ARMORED_SUPPRESSOR));
		assertEquals("侦测报警", label(EnemyRole.SCOUT_ALARM));
		assertEquals("精英指挥", label(EnemyRole.ELITE_COMMANDER));
		assertEquals("可选Boss · 白线", label(EnemyRole.OPTIONAL_BOSS));
	}

	@Test
	public void armoredGuardConsumesPenetrationInsteadOfBeingCosmetic() {
		EnemyArchetypeDefinition armored =
				new EnemyArchetypeDefinition();
		armored.abilities = new String[]{"ARMORED_FRONT"};
		assertEquals(
				65,
				BukovRealtimeWorld.resolveEnemyArmor(
						armored, 100, 8f));
		assertEquals(
				80,
				BukovRealtimeWorld.resolveEnemyArmor(
						armored, 100, 18f));
		assertEquals(
				90,
				BukovRealtimeWorld.resolveEnemyArmor(
						armored, 100, 30f));

		armored.abilities = new String[]{"USE_COVER"};
		assertEquals(
				100,
				BukovRealtimeWorld.resolveEnemyArmor(
						armored, 100, 8f));
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
	}

	private static String label(EnemyRole role) {
		EnemyArchetypeDefinition definition =
				new EnemyArchetypeDefinition();
		definition.role = role;
		return BukovRealtimeWorld.enemyRoleLabel(definition);
	}
}
