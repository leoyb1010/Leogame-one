package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovTouchLayoutTest {

	@Test
	public void landscapeLayoutKeepsEveryControlInsideSafeArea() {
		BukovTouchLayout layout = BukovTouchLayout.calculate(
				426f,
				240f,
				14f,
				6f,
				18f,
				8f
		);

		assertTrue(layout.landscape);
		assertContained(layout);
		assertCoreControlsDoNotOverlap(layout);
		assertTrue(layout.movement.centerX() < layout.safeBounds.centerX());
		assertTrue(layout.aimFire.centerX() > layout.safeBounds.centerX());
	}

	@Test
	public void portraitLayoutKeepsControlsReachableAndSeparated() {
		BukovTouchLayout layout = BukovTouchLayout.calculate(
				135f,
				225f,
				0f,
				6f,
				0f,
				10f,
				42f
		);

		assertFalse(layout.landscape);
		assertContained(layout);
		assertCoreControlsDoNotOverlap(layout);
		assertTrue(layout.backpack.y >= 42f);
		assertTrue(layout.pause.y >= 42f);
		assertTrue(layout.movement.bottom() <= layout.safeBounds.bottom());
		assertTrue(layout.aimFire.bottom() <= layout.safeBounds.bottom());
	}

	@Test
	public void compactLandscapePutsNavigationBelowHudWithoutCollisions() {
		BukovTouchLayout layout = BukovTouchLayout.calculate(
				240f,
				160f,
				6f,
				3f,
				6f,
				5f,
				36f
		);

		assertTrue(layout.landscape);
		assertContained(layout);
		assertCoreControlsDoNotOverlap(layout);
		assertTrue(layout.backpack.y >= 36f);
		assertTrue(layout.pause.y >= 36f);
	}

	@Test
	public void compactLogicalViewportStillPreservesTwoIndependentSticks() {
		BukovTouchLayout layout = BukovTouchLayout.calculate(
				240f,
				135f,
				0f,
				0f,
				0f,
				3f
		);

		assertContained(layout);
		assertFalse(layout.movement.overlaps(layout.aimFire));
		assertTrue(layout.movement.width >= 36f);
		assertTrue(layout.aimFire.width >= 36f);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsMissingTouchSurface() {
		BukovTouchLayout.calculate(0f, 200f, 0f, 0f, 0f, 0f);
	}

	private static void assertContained(BukovTouchLayout layout) {
		assertTrue(layout.safeBounds.contains(layout.movement));
		assertTrue(layout.safeBounds.contains(layout.aimFire));
		assertTrue(layout.safeBounds.contains(layout.interact));
		assertTrue(layout.safeBounds.contains(layout.reload));
		assertTrue(layout.safeBounds.contains(layout.medical));
		assertTrue(layout.safeBounds.contains(layout.drop));
		assertTrue(layout.safeBounds.contains(layout.backpack));
		assertTrue(layout.safeBounds.contains(layout.pause));
	}

	private static void assertCoreControlsDoNotOverlap(BukovTouchLayout layout) {
		assertFalse(layout.movement.overlaps(layout.aimFire));
		assertFalse(layout.reload.overlaps(layout.medical));
		assertFalse(layout.interact.overlaps(layout.drop));
		assertFalse(layout.interact.overlaps(layout.aimFire));
		assertFalse(layout.drop.overlaps(layout.aimFire));
		assertFalse(layout.backpack.overlaps(layout.pause));
		assertFalse(layout.backpack.overlaps(layout.movement));
		assertFalse(layout.backpack.overlaps(layout.aimFire));
		assertFalse(layout.backpack.overlaps(layout.interact));
		assertFalse(layout.backpack.overlaps(layout.drop));
		assertFalse(layout.pause.overlaps(layout.movement));
		assertFalse(layout.pause.overlaps(layout.aimFire));
		assertFalse(layout.pause.overlaps(layout.interact));
		assertFalse(layout.pause.overlaps(layout.drop));
	}
}
