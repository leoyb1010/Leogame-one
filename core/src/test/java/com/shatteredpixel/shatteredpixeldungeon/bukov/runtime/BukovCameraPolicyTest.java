package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BukovCameraPolicyTest {

	@Test
	public void wideMacWindowKeepsRaidAtActionScale() {
		float zoom = BukovCameraPolicy.resolveWorldZoom(
				1_572f, 16f, true, 1f, 8f);
		float visibleTiles = 1_572f / zoom / 16f;

		assertEquals(4f, zoom, 0f);
		assertTrue(visibleTiles >= 22f);
		assertTrue(visibleTiles <= 26f);
	}

	@Test
	public void portraitIphoneKeepsAboutFourteenTilesAcross() {
		float zoom = BukovCameraPolicy.resolveWorldZoom(
				1_179f, 16f, false, 1f, 16f);
		float visibleTiles = 1_179f / zoom / 16f;

		assertEquals(5f, zoom, 0f);
		assertTrue(visibleTiles >= 13f);
		assertTrue(visibleTiles <= 16f);
	}

	@Test
	public void landscapeIphoneUsesCloserCombatScaleThanDesktop() {
		float mobileZoom = BukovCameraPolicy.resolveWorldZoom(
				874f, 16f, true, false, 1f, 8f);
		float desktopZoom = BukovCameraPolicy.resolveWorldZoom(
				874f, 16f, true, true, 1f, 8f);
		float mobileVisibleTiles = 874f / mobileZoom / 16f;

		assertEquals(3f, mobileZoom, 0f);
		assertEquals(2f, desktopZoom, 0f);
		assertTrue(mobileVisibleTiles >= 18f);
		assertTrue(mobileVisibleTiles <= 21f);
	}

	@Test
	public void zoomIsIntegerAndRespectsEngineBounds() {
		assertEquals(4f, BukovCameraPolicy.resolveWorldZoom(
				2_560f, 16f, true, 1f, 4f), 0f);
		assertEquals(2f, BukovCameraPolicy.resolveWorldZoom(
				720f, 16f, true, 2f, 4f), 0f);
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidViewportIsRejected() {
		BukovCameraPolicy.resolveWorldZoom(
				0f, 16f, true, 1f, 4f);
	}
}
