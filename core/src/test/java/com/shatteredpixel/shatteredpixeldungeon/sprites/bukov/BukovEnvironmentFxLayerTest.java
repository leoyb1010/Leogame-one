package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovRaidLayout;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.BukovPerformancePolicy;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovUiTokens;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BukovEnvironmentFxLayerTest {

	@Test
	public void performanceBudgetsAreBoundedAndMonotonic() {
		BukovEnvironmentFxLayer.Budget quality =
				BukovEnvironmentFxLayer.budgetFor(
						BukovPerformancePolicy.HIGH_QUALITY);
		BukovEnvironmentFxLayer.Budget balanced =
				BukovEnvironmentFxLayer.budgetFor(
						BukovPerformancePolicy.BALANCED);
		BukovEnvironmentFxLayer.Budget highFrameRate =
				BukovEnvironmentFxLayer.budgetFor(
						BukovPerformancePolicy.HIGH_FRAME_RATE);

		assertEquals(
				BukovEnvironmentFxLayer.MAX_DRAWABLES,
				quality.maximumDrawables());
		assertTrue(quality.maximumDrawables()
				> balanced.maximumDrawables());
		assertTrue(balanced.maximumDrawables()
				> highFrameRate.maximumDrawables());
		assertEquals(15, highFrameRate.maximumDrawables());
		assertSame(
				quality,
				BukovEnvironmentFxLayer.budgetFor(
						BukovPerformancePolicy.HIGH_QUALITY));
		assertSame(
				balanced,
				BukovEnvironmentFxLayer.budgetFor(
						BukovPerformancePolicy.BALANCED));
		assertSame(
				highFrameRate,
				BukovEnvironmentFxLayer.budgetFor(
						BukovPerformancePolicy.HIGH_FRAME_RATE));
		assertTrue(quality.fogPatches
				<= BukovEnvironmentFxLayer.MAX_FOG_PATCHES);
		assertTrue(quality.dangerGlows
				<= BukovEnvironmentFxLayer.MAX_DANGER_GLOWS);
		assertTrue(quality.extractionGlows
				<= BukovEnvironmentFxLayer.MAX_EXTRACTION_GLOWS);
		assertTrue(quality.waterGlints
				<= BukovEnvironmentFxLayer.MAX_WATER_GLINTS);
		assertTrue(quality.ripples
				<= BukovEnvironmentFxLayer.MAX_RIPPLES);
	}

	@Test
	public void atmosphereColorsAlwaysCarryOpaqueTextureAlpha() {
		assertEquals(
				0xFF123456,
				BukovUiTokens.withAlpha(0x123456, 255));
		assertEquals(
				0xFFABCDEF,
				BukovUiTokens.withAlpha(0x01ABCDEF, 255));
		assertEquals(
				0xFF345678,
				BukovUiTokens.withAlpha(0x00345678, 255));
	}

	@Test
	public void reducedMotionFreezesPulseAndSlowsNoSimulation() {
		assertEquals(
				BukovEnvironmentFxLayer.pulseAt(0f, 0f, true),
				BukovEnvironmentFxLayer.pulseAt(97f, 2.4f, true),
				0.0001f);
		assertTrue(Math.abs(
				BukovEnvironmentFxLayer.pulseAt(0f, 0f, false)
						- BukovEnvironmentFxLayer.pulseAt(
								1f, 0f, false)) > 0.01f);
		assertTrue(BukovEnvironmentFxLayer.rippleInterval(
				BukovPerformancePolicy.HIGH_QUALITY)
				< BukovEnvironmentFxLayer.rippleInterval(
						BukovPerformancePolicy.HIGH_FRAME_RATE));
	}

	@Test
	public void waterSamplingIsDeterministicCappedAndWaterOnly() {
		int[] map = {
				Terrain.WALL,
				Terrain.WATER,
				Terrain.WATER,
				Terrain.EMPTY,
				Terrain.WATER,
				Terrain.EMPTY,
				Terrain.WATER,
				Terrain.WATER,
				Terrain.WATER,
				Terrain.EMPTY
		};

		int[] first = BukovEnvironmentFxLayer.selectWaterCells(map, 3);
		int[] second = BukovEnvironmentFxLayer.selectWaterCells(map, 3);

		assertArrayEquals(first, second);
		assertEquals(3, first.length);
		for (int cell : first) {
			assertEquals(Terrain.WATER, map[cell]);
		}
		assertEquals(
				0,
				BukovEnvironmentFxLayer.selectWaterCells(map, 0).length);
	}

	@Test
	public void dangerClassificationDoesNotTintOrdinaryRoutes() {
		assertTrue(BukovEnvironmentFxLayer.dangerZone(
				BukovRaidLayout.Zone.HAZARD));
		assertTrue(BukovEnvironmentFxLayer.dangerZone(
				BukovRaidLayout.Zone.HIGH_VALUE));
		assertTrue(BukovEnvironmentFxLayer.dangerZone(
				BukovRaidLayout.Zone.BOSS));
		assertFalse(BukovEnvironmentFxLayer.dangerZone(
				BukovRaidLayout.Zone.SPAWN));
		assertFalse(BukovEnvironmentFxLayer.dangerZone(
				BukovRaidLayout.Zone.TRANSIT));
	}

	@Test
	public void gameSceneMountsAtmosphereBetweenTerrainAndLandmarks()
			throws Exception {
		String source = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/scenes/"
								+ "GameScene.java")),
				StandardCharsets.UTF_8);
		int floor = source.indexOf("levelVisuals = Dungeon.level.addVisuals()");
		int atmosphere = source.indexOf(
				"add(new BukovEnvironmentFxLayer(bukovLevel));");
		int landmarks = source.indexOf(
				"add(new BukovFirstRaidLandmarks(bukovLevel));");
		int actors = source.indexOf("mobs = new Group();");

		assertTrue(floor >= 0);
		assertTrue(atmosphere > floor);
		assertTrue(landmarks > atmosphere);
		assertTrue(actors > landmarks);
	}
}
