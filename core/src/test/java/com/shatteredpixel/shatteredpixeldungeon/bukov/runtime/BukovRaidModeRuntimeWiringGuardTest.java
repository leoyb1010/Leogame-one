package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
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
		assertTrue(world.contains("raidMode.incomingDamage(damage)"));
		String convergence = between(
				world,
				"private void applyModeConvergence()",
				"static float themedSpawnInterval");
		assertFalse(convergence.contains("setExtractionCondition"));
		String pump = between(
				world,
				"private void activatePump()",
				"private boolean completeNearbyBossObjective()");
		assertTrue(pump.contains(
				"setExtractionCondition(CONDITIONAL_EXTRACTION_ID, true)"));
		assertTrue(pump.contains("Assets.Sounds.Bukov.GATE_UNLOCK"));
		assertTrue(pump.contains("emitPumpBroadcast("));
		assertTrue(pump.contains("scheduleInvestigators()"));
		assertTrue(coordinator.contains(
				"configureContainersForProfile(\n"
						+ "\t\t\t\t\t\tprofile,\n"
						+ "\t\t\t\t\t\traidMode,"));
		assertTrue(coordinator.contains(
				"configureContainersForProfile(\n"
						+ "\t\t\t\t\t\tprofile,\n"
						+ "\t\t\t\t\t\tsession().raidMode(),"));
		assertTrue(coordinator.contains(
				"raidMode.configureContainers(source, seed)"));
		assertTrue(settlement.contains("raidMode.settleExtractedItem(item)"));
		assertTrue(settlement.contains("\"|mode:\" + raidMode.name()"));
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}

	private static String between(
			String source,
			String start,
			String end) {
		int startIndex = source.indexOf(start);
		int endIndex = source.indexOf(end, startIndex);
		assertTrue(startIndex >= 0);
		assertTrue(endIndex > startIndex);
		return source.substring(startIndex, endIndex);
	}
}
