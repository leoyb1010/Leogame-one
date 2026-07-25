package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class BukovMaintenanceCacheProductionWiringTest {

	@Test
	public void worldConsumesKeyThroughRaidLedgerAndShowsExplicitPrompts()
			throws IOException {
		String world = source(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/bukov/runtime/"
						+ "BukovRealtimeWorld.java");
		assertTrue(world.contains(
				"raid.session().keyDoors().unlock("));
		assertTrue(world.contains(
				"BukovFirstRaidLootTables.MAINTENANCE_KEY_DEFINITION_ID"));
		assertTrue(world.contains("raid.loot())"));
		assertTrue(world.contains(
				"\"bukov.raid.runtime.maintenance_key_required\""));
		assertTrue(world.contains(
				"\"bukov.raid.runtime.unlock_with_maintenance_key\""));
		assertTrue(world.contains(
				"BukovFirstRaidLootTables.maintenanceKeyDrops("));
	}

	@Test
	public void sideCacheIsAdditionalLockedContentOutsideMissionTopology()
			throws IOException {
		String definitions = source(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/bukov/raid/"
						+ "BukovRaidWorldDefinitions.java");
		assertTrue(definitions.contains(
				".semanticCell(\"scrap_compactor\")"));
		assertTrue(definitions.contains(
				".MAINTENANCE_CACHE_CONTAINER_ID"));
		assertTrue(definitions.contains(
				"BukovFirstRaidLootTables.MAINTENANCE_CACHE"));

		String level = source(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/bukov/levels/"
						+ "BukovLevel.java");
		assertTrue(level.contains(
				"forbidden[index++] = gate.archiveCell"));
		assertTrue(level.contains(
				"for (int gateCell : gate.gateCells)"));
	}

	private static String source(String path) throws IOException {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
