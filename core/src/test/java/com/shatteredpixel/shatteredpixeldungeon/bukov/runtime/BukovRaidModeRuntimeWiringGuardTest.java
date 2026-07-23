package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class BukovRaidModeRuntimeWiringGuardTest {

	@Test
	public void runtimeAndSettlementConsumeModePolicies() throws Exception {
		String world = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java");
		String coordinator = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/raid/BukovRaidCoordinator.java");
		String settlement = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/raid/RaidSettlement.java");
		assertTrue(world.contains("raidMode.spawnIntervalAt(elapsedSeconds)"));
		assertTrue(world.contains("raidMode.maximumActiveEnemiesAt(elapsed)"));
		assertTrue(world.contains("raidMode.convergenceStarted(elapsed)"));
		assertTrue(world.contains("raidMode.overtime(elapsed)"));
		assertTrue(coordinator.contains("raidMode.configureContainers("));
		assertTrue(coordinator.contains(
				"session().raidMode().configureContainers("));
		assertTrue(settlement.contains("raidMode.settleExtractedItem(item)"));
		assertTrue(settlement.contains("\"|mode:\" + raidMode.name()"));
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
