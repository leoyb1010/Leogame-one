package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BukovRaidModeMapContractTest {

	@Test
	public void fourContractsAndTrainingOwnDistinctPhysicalMapEnvelopes() {
		assertRange(BukovRaidMode.TRAINING_GROUND, 18, 16, 20);
		assertRange(BukovRaidMode.QUICK_SWEEP, 22, 18, 24);
		assertRange(BukovRaidMode.SCAVENGER, 26, 22, 29);
		assertRange(BukovRaidMode.EXPEDITION, 31, 26, 34);
		assertRange(BukovRaidMode.BOSS_CONTRACT, 34, 28, 37);

		assertTrue(BukovRaidMode.QUICK_SWEEP.routeDetourAllowance
				< BukovRaidMode.SCAVENGER.routeDetourAllowance);
		assertTrue(BukovRaidMode.SCAVENGER.routeDetourAllowance
				< BukovRaidMode.EXPEDITION.routeDetourAllowance);
		assertTrue(BukovRaidMode.EXPEDITION.routeDetourAllowance
				< BukovRaidMode.BOSS_CONTRACT.routeDetourAllowance);
		assertTrue(BukovRaidMode.BOSS_CONTRACT.bossEnabled);
	}

	@Test
	public void deploymentCarriesDurableRaidModeIntoNewLevelButRestoreBinds()
			throws Exception {
		String deployment = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/BukovDeploymentScene.java");
		String level = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/levels/BukovLevel.java");

		assertTrue(deployment.contains(
				"checkpoint.session().raidMode()"));
		assertTrue(deployment.contains(
				"deploymentProfile.selectedRaidMode()"));
		assertTrue(deployment.contains(
				"BukovMode.prepareRaidMode(deploymentMode)"));
		assertTrue(deployment.contains(
				"BukovMode.prepareSelectedMap(deploymentProfile.selectedMap())"));
		assertTrue(level.contains(
				"private final BukovRaidMode raidMode = BukovMode.raidMode()"));
		assertTrue(level.contains(
				"themeForId(BukovMode.selectedRaidTheme())"));
		assertTrue(level.contains(
				"BukovRoomGraphAdapter.adapt(\n"
						+ "\t\t\t\tthis, Dungeon.seedCurDepth(), theme.id, raidMode)"));
		assertTrue(level.contains(
				"BukovRoomGraphAdapter.bind(this, raidLayout, raidMode)"));
	}

	private static void assertRange(
			BukovRaidMode mode, int budget, int minimum, int maximum) {
		assertEquals(budget, mode.standardRoomBudget);
		assertEquals(minimum, mode.minimumContentRooms);
		assertEquals(maximum, mode.maximumContentRooms);
		assertTrue(mode.acceptsContentRoomCount(minimum));
		assertTrue(mode.acceptsContentRoomCount(maximum));
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
