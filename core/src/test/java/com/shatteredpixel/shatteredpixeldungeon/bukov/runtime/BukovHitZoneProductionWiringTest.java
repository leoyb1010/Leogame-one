package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovHitZoneProductionWiringTest {

	@Test
	public void playerDamageConsumesHitscanZoneAndBossVulnerability()
			throws Exception {
		String world = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/bukov/runtime/"
								+ "BukovRealtimeWorld.java")),
				StandardCharsets.UTF_8);

		int queryStart = world.indexOf(
				"private final HitscanResolver.TargetQuery targetQuery");
		int queryEnd = world.indexOf(
				"private final HitscanResolver.TargetQuery "
						+ "enemyShotTargetQuery",
				queryStart);
		String playerQuery = world.substring(queryStart, queryEnd);
		assertTrue(playerQuery.contains("HitZoneGeometry.resolve("));
		assertTrue(playerQuery.contains(
				"boss && enemy.bossState.vulnerable()"));

		int fireStart = world.indexOf(
				"public void fire(Firearm firearm, "
						+ "FirearmDefinition definition)");
		int fireEnd = world.indexOf(
				"public FireControl.AmmoSelection requestAmmo(",
				fireStart);
		String playerFire = world.substring(fireStart, fireEnd);
		assertTrue(playerFire.contains("shotHit.zone"));
		assertFalse(playerFire.contains(
				"RealtimeDamage.HitZone.CORE"));
	}
}
