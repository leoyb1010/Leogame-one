package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BukovVisualContractTest {

	@Test
	public void fullHdRetinaUsesLogicalWidthWithoutDensityMultiplication() {
		// 1920x1080 at the common 3x UI camera is 640x360 logical pixels.
		float logicalWidth = 1920f / 3f;
		float content = BukovVisualContract.contentWidth(
				logicalWidth, true);
		assertEquals(BukovVisualContract.MAX_CONTENT_WIDTH, content, 0f);
		assertTrue(content < logicalWidth);
	}

	@Test
	public void iosSafeAreaAndTouchTargetsStayInsideViewport() {
		float screenHeight = 844f;
		float top = BukovVisualContract.safeTop(47f);
		float bottom = BukovVisualContract.safeBottom(
				screenHeight, 34f);
		assertTrue(top >= 47f);
		assertTrue(bottom <= screenHeight - 34f);
		assertTrue(bottom > top);
		assertTrue(BukovVisualContract.controlHeight(true) >= 22f);
		assertTrue(BukovVisualContract.controlHeight(true)
				> BukovVisualContract.controlHeight(false));
	}

	@Test
	public void narrowScreensRetainGuttersAndCenteredDesktopPanels() {
		assertEquals(304f,
				BukovVisualContract.contentWidth(320f, false), 0f);
		float width = BukovVisualContract.contentWidth(640f, true);
		assertEquals(110f,
				BukovVisualContract.centeredLeft(0f, 640f, width), 0f);
		assertTrue(BukovVisualContract.panelWidth(320f, false) <= 304f);
	}
}
