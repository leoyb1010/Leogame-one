package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovContainerDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Guards the production boundary between controls training and Q01. */
public class BukovTrainingGroundMissionIsolationTest {

	@Test
	public void trainingModeDoesNotInjectFirstRaidMissionContainers() {
		List<BukovContainerDefinition> configured =
				BukovRaidMode.TRAINING_GROUND.configureContainers(
						Arrays.asList(
								container("L01", "low", 2, 2f),
								container("L02", "medical", 2, 2.4f),
								container(
										"L03",
										FirstRaidMission
												.HIGH_VALUE_LOOT_TABLE_ID,
										3,
										3f),
								container(
										"Q01",
										FirstRaidMission
												.ARCHIVE_LOOT_TABLE_ID,
										1,
										1.4f)),
						881177L);

		assertEquals(2, configured.size());
		for (BukovContainerDefinition definition : configured) {
			assertFalse(
					FirstRaidMission.ARCHIVE_LOOT_TABLE_ID.equals(
							definition.lootTableId));
			assertFalse(
					FirstRaidMission.HIGH_VALUE_LOOT_TABLE_ID.equals(
							definition.lootTableId));
			assertFalse(definition.locked);
		}
	}

	@Test
	public void trainingObjectiveTeachesControlsAndExtractionWithoutQ01() {
		String objective = BukovRealtimeWorld.trainingObjective();

		assertTrue(objective.contains(
				BukovMessages.get("bukov.raid.touch.movement")));
		assertTrue(objective.contains(
				BukovMessages.get("bukov.raid.touch.aim_fire")));
		assertTrue(objective.contains(
				BukovMessages.get("bukov.raid.touch.reload")));
		assertTrue(objective.contains(
				BukovMessages.get("bukov.raid.hud.interaction_extract")));
		assertFalse(objective.contains(
				BukovMessages.get(
						"bukov.raid.mission.objective_recover_archive")));
	}

	@Test
	public void sceneDoesNotCreateTrainingMissionContainers() throws Exception {
		String scene = source(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/scenes/GameScene.java");
		String containers = between(
				scene,
				"private List<BukovContainerDefinition> "
						+ "bukovContainerDefinitions()",
				"private void consumeBukovCombatFx");

		assertTrue(containers.contains(
				"boolean missionEnabled = !level.raidMode().trainingGround()"));
		assertTrue(containers.contains(
				"FirstRaidMission.HIGH_VALUE_LOOT_TABLE_ID.equals("));
		int trainingReturn = containers.indexOf(
				"if (!missionEnabled) {\n\t\t\t\t\treturn result;");
		int archiveCreation = containers.indexOf(
				"FirstRaidMission.ARCHIVE_CONTAINER_ID");
		assertTrue(trainingReturn >= 0);
		assertTrue(archiveCreation > trainingReturn);
	}

	@Test
	public void worldUsesOneMissionPolicyForGateObjectiveAndNavigation()
			throws Exception {
		String world = source(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/bukov/runtime/"
						+ "BukovRealtimeWorld.java");

		assertTrue(world.contains("private final boolean missionEnabled;"));
		assertTrue(world.contains(
				"missionEnabled = raid != null "
						+ "&& raid.firstRaidMissionActive();"));
		assertEquals(1, occurrences(world, "firstRaidMissionActive()"));
		assertTrue(world.contains(
				"missionGateUnlocked = !missionEnabled"));
		assertTrue(world.contains(
				"if (missionEnabled && !missionGateUnlocked)"));
		assertTrue(world.contains(
				"if (!missionEnabled\n"
						+ "\t\t\t\t|| missionGateUnlocked"));
		assertTrue(world.contains(
				"if (raidMode.trainingGround()) {\n"
						+ "\t\t\treturn trainingObjective();"));
	}

	private static String between(String source, String start, String end) {
		int from = source.indexOf(start);
		int to = source.indexOf(end, from);
		if (from < 0 || to < 0) {
			throw new AssertionError("Source boundary not found");
		}
		return source.substring(from, to);
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}

	private static int occurrences(String source, String value) {
		int count = 0;
		int at = 0;
		while ((at = source.indexOf(value, at)) >= 0) {
			count++;
			at += value.length();
		}
		return count;
	}

	private static BukovContainerDefinition container(
			String id,
			String table,
			int rolls,
			float seconds) {
		return new BukovContainerDefinition(
				id,
				id.hashCode() & 0x7FFF,
				table,
				rolls,
				seconds,
				false);
	}
}
