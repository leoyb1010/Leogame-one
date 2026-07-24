package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BukovViewportTest {

	@Test
	public void cameraCrossesSeveralViewportsWithTheOperator() {
		float scroll = 0f;
		for (float focus = 80f; focus <= 880f; focus += 8f) {
			scroll = BukovViewport.resolveScroll(
					scroll, focus, 240f, 1_024f, 5f);
		}
		assertTrue(scroll > 600f);
	}

	@Test
	public void cameraClampsToMapAndPhysicalPixels() {
		assertEquals(0f, BukovViewport.resolveScroll(
				-400f, 5f, 240f, 1_024f, 4f), 0.0001f);
		assertEquals(784f, BukovViewport.resolveScroll(
				2_000f, 1_020f, 240f, 1_024f, 4f), 0.0001f);

		float aligned = BukovViewport.resolveScroll(
				123.123f, 240f, 240f, 1_024f, 5f);
		assertEquals(Math.round(aligned * 5f), aligned * 5f, 0.0001f);
	}

	@Test
	public void guardPreservesValidSmoothFollowPosition() {
		assertEquals(120f, BukovViewport.resolveScroll(
				120f, 240f, 240f, 1_024f, 4f), 0.0001f);
	}

	@Test
	public void genuinelyTinyMapStillCenters() {
		assertEquals(-80f, BukovViewport.resolveScroll(
				0f, 40f, 240f, 80f, 4f), 0.0001f);
	}
}
