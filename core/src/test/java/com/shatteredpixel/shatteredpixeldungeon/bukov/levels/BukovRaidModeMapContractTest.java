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
	public void temporaryExtractionFitsEveryModeSessionEnvelope() {
		assertTemporaryWindow(BukovRaidMode.EXPEDITION, 480f, 840f);
		assertTemporaryWindow(BukovRaidMode.QUICK_SWEEP, 240f, 480f);
		assertTemporaryWindow(BukovRaidMode.SCAVENGER, 360f, 720f);
		assertTemporaryWindow(BukovRaidMode.BOSS_CONTRACT, 360f, 780f);
		assertTemporaryWindow(BukovRaidMode.TRAINING_GROUND, 0f, 0f);

		for (BukovRaidMode mode : BukovRaidMode.values()) {
			for (long seed : new long[]{0L, 1L, 42L, -1L, Long.MAX_VALUE}) {
				float start = mode.temporaryExtractionStartSeconds(seed);
				assertTrue(start >= mode.temporaryExtractionEarliestSeconds());
				assertTrue(start <= mode.temporaryExtractionLatestSeconds());
				if (mode.hasTimeLimit()) {
					assertTrue(start + 120f <= mode.targetMaximumSeconds());
				}
			}
		}
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
				"raidMode.mapSeed(Dungeon.seedCurDepth())"));
		assertTrue(level.contains(
				"BukovRoomGraphAdapter.adapt("));
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

	private static void assertTemporaryWindow(
			BukovRaidMode mode, float earliest, float latest) {
		assertEquals(earliest, mode.temporaryExtractionEarliestSeconds(), 0f);
		assertEquals(latest, mode.temporaryExtractionLatestSeconds(), 0f);
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
