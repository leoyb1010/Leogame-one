package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BukovUiScaleTest {

	@Test
	public void exposesExactAuthoredScaleLevels() {
		assertEquals(100, BukovUiScale.percent(0));
		assertEquals(125, BukovUiScale.percent(1));
		assertEquals(150, BukovUiScale.percent(2));
		assertEquals(1f, BukovUiScale.multiplier(-1), 0f);
		assertEquals(1.5f, BukovUiScale.multiplier(99), 0f);
	}

	@Test
	public void scalesDimensionsAndTypographyDeterministically() {
		assertEquals(100, BukovUiScale.pixels(100, 0));
		assertEquals(125, BukovUiScale.pixels(100, 1));
		assertEquals(150, BukovUiScale.pixels(100, 2));
		assertEquals(14, BukovUiScale.fontPixels(9, 2));
		assertEquals(17, BukovUiScale.fontPixels(11, 2));
	}

	@Test
	public void touchControlsNeverShrinkBelowMinimumTarget() {
		assertEquals(
				BukovUiScale.MINIMUM_TOUCH_TARGET,
				BukovUiScale.controlHeight(10f, true, 0),
				0f);
		assertEquals(27f,
				BukovUiScale.controlHeight(18f, true, 2),
				0f);
		assertEquals(15f,
				BukovUiScale.controlHeight(10f, false, 2),
				0f);
	}
}
