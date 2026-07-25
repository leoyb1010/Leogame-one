package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovEnemySearchProductionWiringTest {

	@Test
	public void worldNavigatesAndFacesTheBrainsRememberedTarget()
			throws Exception {
		String world = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/bukov/runtime/"
								+ "BukovRealtimeWorld.java")),
				StandardCharsets.UTF_8);

		int brainsStart = world.indexOf("public void updateBrains(float dt)");
		int brainsEnd = world.indexOf(
				"public void updateMobs(float dt)", brainsStart);
		String updateBrains = world.substring(brainsStart, brainsEnd);
		assertTrue(updateBrains.contains(
				"enemy.brain.navigationTargetX()"));
		assertTrue(updateBrains.contains(
				"enemy.brain.navigationTargetY()"));
		assertTrue(updateBrains.contains(
				"enemy.brain.observeNavigation("));
		assertFalse(updateBrains.contains(
				"? heroBody.x : enemy.brain.lastSeenX()"));

		int renderStart = world.indexOf(
				"public void renderInterpolate(float alpha)");
		int renderEnd = world.indexOf(
				"public void disposeRealtimeObjects()", renderStart);
		String render = world.substring(renderStart, renderEnd);
		assertTrue(render.contains("enemy.brain.desiredX()"));
		assertFalse(render.contains(
				"enemy.mob.sprite.turnTo(enemy.mob.pos, hero.pos)"));
	}
}
