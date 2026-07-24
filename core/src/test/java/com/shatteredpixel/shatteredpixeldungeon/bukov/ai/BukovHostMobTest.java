package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovAlleyScoutSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovArmoredSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovBreachVeteranSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovCaptainSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovDepotShotgunnerSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovDroneSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovFogStalkerSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovGunnerSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovIronClaspMarksmanSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovLineRiflemanSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovScavengerSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovSignalOperatorSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovWhiteLineSprite;
import com.watabou.utils.Bundle;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BukovHostMobTest {

	@Test
	public void definitionControlsHostStatsAndBukovSprite() {
		EnemyArchetypeDefinition definition = new EnemyArchetypeDefinition();
		definition.id = "test_guard";
		definition.name = "测试卫兵";
		definition.tier = EnemyTier.COMMON;
		definition.role = EnemyRole.ARMORED_SUPPRESSOR;
		definition.hostClassHint = "Guard";
		definition.weaponDefinitionId = "ward_556";
		definition.health = 46;
		definition.movementSpeed = 1.8f;
		definition.perceptionRange = 9f;
		definition.engagementRange = 8f;
		definition.minimumDamage = 4;
		definition.maximumDamage = 8;
		definition.spawnWeight = 20;
		definition.minimumSpawnSeconds = 240f;
		definition.minimumDistanceFromSpawnRooms = 4;
		definition.maximumActive = 3;
		definition.firstRaidMinimumSeconds = 300f;
		definition.firstRaidMaximumActive = 2;
		definition.abilities = new String[]{"USE_COVER"};

		BukovHostMob mob = new BukovHostMob().configure(definition);

		assertEquals("test_guard", mob.definitionId());
		assertEquals("测试卫兵", mob.name());
		assertEquals(46, mob.HT);
		assertEquals(46, mob.HP);
		assertSame(BukovArmoredSprite.class, mob.spriteClass);
	}

	@Test
	public void onboardingContactMarkerSurvivesHostSave() {
		EnemyArchetypeDefinition definition = definition(
				"scavenger_gunner",
				"GnollTrickster");
		BukovHostMob mob = new BukovHostMob().configure(definition);
		assertFalse(mob.onboardingContact());
		mob.markOnboardingContact();

		Bundle bundle = new Bundle();
		mob.storeInBundle(bundle);
		BukovHostMob restored = new BukovHostMob();
		restored.restoreFromBundle(bundle);

		assertTrue(restored.onboardingContact());
		assertEquals(mob.definitionId(), restored.definitionId());
	}

	@Test
	public void everyFirstRaidArchetypeHasDedicatedBukovArt() {
		assertSprite("scavenger_gunner", "GnollTrickster", BukovGunnerSprite.class);
		assertSprite("melee_rusher", "Rat", BukovScavengerSprite.class);
		assertSprite("iron_clasp_guard", "Guard", BukovArmoredSprite.class);
		assertSprite("sensor_doll", "DM100", BukovDroneSprite.class);
		assertSprite("iron_clasp_captain", "Brute", BukovCaptainSprite.class);
		assertSprite("boss_white_line", "Goo", BukovWhiteLineSprite.class);
		assertSprite("alley_scout", "Rat", BukovAlleyScoutSprite.class);
		assertSprite("depot_shotgunner", "Rat", BukovDepotShotgunnerSprite.class);
		assertSprite("line_rifleman", "Rat", BukovLineRiflemanSprite.class);
		assertSprite("fog_stalker", "Rat", BukovFogStalkerSprite.class);
		assertSprite("signal_operator", "Rat", BukovSignalOperatorSprite.class);
		assertSprite("iron_clasp_marksman", "Rat", BukovIronClaspMarksmanSprite.class);
		assertSprite("breach_veteran", "Rat", BukovBreachVeteranSprite.class);
	}

	private static void assertSprite(
			String id,
			String hostHint,
			Class<?> expectedSprite) {
		assertSame(
				expectedSprite,
				new BukovHostMob()
						.configure(definition(id, hostHint))
						.spriteClass);
	}

	private static EnemyArchetypeDefinition definition(
			String id,
			String hostHint) {
		EnemyArchetypeDefinition definition = new EnemyArchetypeDefinition();
		definition.id = id;
		definition.name = id;
		boolean boss = id.equals("boss_white_line");
		definition.tier = boss ? EnemyTier.BOSS
				: id.equals("iron_clasp_captain") ? EnemyTier.ELITE : EnemyTier.COMMON;
		definition.role = boss ? EnemyRole.OPTIONAL_BOSS : EnemyRole.MELEE_RUSHER;
		definition.hostClassHint = hostHint;
		definition.health = 1;
		definition.movementSpeed = 1f;
		definition.perceptionRange = 1f;
		definition.engagementRange = 1f;
		definition.minimumDamage = 0;
		definition.maximumDamage = 1;
		definition.spawnWeight = boss ? 0 : 1;
		definition.minimumDistanceFromSpawnRooms = 1;
		definition.maximumActive = 1;
		definition.firstRaidMaximumActive = 1;
		definition.abilities = new String[0];
		definition.optionalRouteOnly = boss;
		definition.bossArenaOnly = boss;
		return definition;
	}
}
