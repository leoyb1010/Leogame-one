package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovHitscanSpatialIndexProductionWiringTest {

	@Test
	public void playerQueryTracksRosterMovementDeathAndDispose()
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
		assertTrue(playerQuery.contains(
				"targetSpatialIndex.candidates("));
		assertFalse(playerQuery.contains("return targetBodies;"));

		assertTrue(world.contains(
				"targetSpatialIndex.rebuild(targetBodies);"));
		assertTrue(world.contains(
				"targetSpatialIndex.update(enemy.body);"));
		assertTrue(world.contains(
				"targetSpatialIndex.remove(enemy.body);"));
		assertTrue(world.contains("targetSpatialIndex.clear();"));
	}
}
