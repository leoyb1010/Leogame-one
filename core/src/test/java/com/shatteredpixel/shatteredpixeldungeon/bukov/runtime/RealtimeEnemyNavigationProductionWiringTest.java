package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class RealtimeEnemyNavigationProductionWiringTest {

	@Test
	public void worldUsesNavigationSeparationAndStuckFeedback()
			throws IOException {
		String source = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/bukov/runtime/"
								+ "BukovRealtimeWorld.java")),
				StandardCharsets.UTF_8);

		assertTrue(source.contains("enemy.navigator.step("));
		assertTrue(source.contains("enemy.avoidance.avoid("));
		assertTrue(source.contains("enemy.navigator.observePosition("));
		assertTrue(source.contains(
				"enemy.navigationIntent.desiredX()"));
	}
}
