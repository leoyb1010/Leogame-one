package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class BukovFirearmConditionProductionWiringTest {

	@Test
	public void liveWorldAgesCoolsAndConsumesConditionSpread() throws Exception {
		String source = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/bukov/runtime/"
								+ "BukovRealtimeWorld.java")),
				StandardCharsets.UTF_8);

		assertTrue(source.contains("equippedFirearm.cool(dt);"));
		assertTrue(source.contains("firearm.recordShot(definition);"));
		assertTrue(source.contains(
				"spread += firearm.conditionSpreadPenaltyDeg();"));
	}
}
