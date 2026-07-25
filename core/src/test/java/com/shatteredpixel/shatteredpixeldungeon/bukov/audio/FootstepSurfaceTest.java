package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.ThemeEnvironmentRules;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class FootstepSurfaceTest {

	@Test
	public void routesWaterMetalAndDefaultHardTerrain() {
		assertEquals(
				FootstepSurface.WATER,
				FootstepSurface.resolve(Terrain.WATER, null));
		assertEquals(
				FootstepSurface.METAL,
				FootstepSurface.resolve(Terrain.EMBERS, null));
		assertEquals(
				FootstepSurface.METAL,
				FootstepSurface.resolve(Terrain.EMPTY_SP, null));
		assertEquals(
				FootstepSurface.METAL,
				FootstepSurface.resolve(Terrain.OPEN_DOOR, null));
		assertEquals(
				FootstepSurface.HARD,
				FootstepSurface.resolve(Terrain.EMPTY, null));
		assertEquals(
				FootstepSurface.HARD,
				FootstepSurface.resolve(Terrain.CUSTOM_DECO_EMPTY, null));
	}

	@Test
	public void allAuthoredThemeSurfacesMapToOneOfThreeFamilies() {
		assertEquals(
				FootstepSurface.WATER,
				FootstepSurface.forThemeSurface(
						ThemeEnvironmentRules.Surface.WATER));
		assertEquals(
				FootstepSurface.METAL,
				FootstepSurface.forThemeSurface(
						ThemeEnvironmentRules.Surface.EMBERS));
		assertEquals(
				FootstepSurface.METAL,
				FootstepSurface.forThemeSurface(
						ThemeEnvironmentRules.Surface.EMPTY_SP));
		assertEquals(
				FootstepSurface.HARD,
				FootstepSurface.forThemeSurface(
						ThemeEnvironmentRules.Surface.EMPTY_DECO));
		assertEquals(
				FootstepSurface.HARD,
				FootstepSurface.forThemeSurface(
						ThemeEnvironmentRules.Surface.CUSTOM_DECO_EMPTY));
	}

	@Test
	public void everySurfaceHasTwoDeterministicVariants() {
		for (FootstepSurface surface : FootstepSurface.values()) {
			assertNotEquals(surface.asset(0), surface.asset(1));
			assertEquals(surface.asset(0), surface.asset(2));
			assertTrue(surface.gain() > 0f);
			assertTrue(surface.pitch(0) > 0f);
		}
	}
}
