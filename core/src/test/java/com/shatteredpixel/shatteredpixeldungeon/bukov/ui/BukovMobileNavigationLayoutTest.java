package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BukovMobileNavigationLayoutTest {

	@Test
	public void allEightDirectionsStayInsideSafePlayableBounds() {
		float viewportWidth = 240f;
		float viewportHeight = 135f;
		float safeLeft = 6f;
		float playableTop = 53f;
		float safeRight = 8f;
		float safeBottom = 5f;

		for (BukovRaidHudState.Direction direction
				: BukovRaidHudState.Direction.values()) {
			BukovMobileNavigationLayout.Rect cue =
					BukovMobileNavigationLayout.calculate(
							viewportWidth,
							viewportHeight,
							safeLeft,
							playableTop,
							safeRight,
							safeBottom,
							direction,
							new BukovMobileNavigationLayout.Rect());
			assertEquals(
					BukovMobileNavigationLayout.WIDTH,
					cue.width,
					0f);
			assertEquals(
					BukovMobileNavigationLayout.HEIGHT,
					cue.height,
					0f);
			assertTrue(cue.x >= safeLeft);
			assertTrue(cue.y >= playableTop);
			assertTrue(cue.right() <= viewportWidth - safeRight);
			assertTrue(cue.bottom() <= viewportHeight - safeBottom);
		}
	}

	@Test
	public void portraitLayoutAlsoRespectsNotchAndHomeIndicatorInsets() {
		float viewportWidth = 135f;
		float viewportHeight = 240f;
		float safeLeft = 3f;
		float safeTop = 14f;
		float safeRight = 3f;
		float safeBottom = 9f;

		for (BukovRaidHudState.Direction direction
				: BukovRaidHudState.Direction.values()) {
			BukovMobileNavigationLayout.Rect cue =
					BukovMobileNavigationLayout.calculate(
							viewportWidth,
							viewportHeight,
							safeLeft,
							safeTop,
							safeRight,
							safeBottom,
							direction,
							new BukovMobileNavigationLayout.Rect());
			assertTrue(cue.x >= safeLeft);
			assertTrue(cue.y >= safeTop);
			assertTrue(cue.right() <= viewportWidth - safeRight);
			assertTrue(cue.bottom() <= viewportHeight - safeBottom);
		}
	}

	@Test
	public void directionControlsWhichScreenEdgeOwnsTheCue() {
		Map<BukovRaidHudState.Direction,
				BukovMobileNavigationLayout.Rect> cues =
				new EnumMap<>(BukovRaidHudState.Direction.class);
		for (BukovRaidHudState.Direction direction
				: BukovRaidHudState.Direction.values()) {
			cues.put(
					direction,
					BukovMobileNavigationLayout.calculate(
							240f,
							135f,
							6f,
							53f,
							8f,
							5f,
							direction,
							new BukovMobileNavigationLayout.Rect()));
		}

		assertEquals(cues.get(BukovRaidHudState.Direction.N).y,
				cues.get(BukovRaidHudState.Direction.NE).y, 0f);
		assertEquals(cues.get(BukovRaidHudState.Direction.E).x,
				cues.get(BukovRaidHudState.Direction.SE).x, 0f);
		assertEquals(cues.get(BukovRaidHudState.Direction.S).y,
				cues.get(BukovRaidHudState.Direction.SW).y, 0f);
		assertEquals(cues.get(BukovRaidHudState.Direction.W).x,
				cues.get(BukovRaidHudState.Direction.NW).x, 0f);
		assertTrue(cues.get(BukovRaidHudState.Direction.N).x
				< cues.get(BukovRaidHudState.Direction.NE).x);
		assertTrue(cues.get(BukovRaidHudState.Direction.W).y
				< cues.get(BukovRaidHudState.Direction.SW).y);
	}

	@Test
	public void layoutReusesCallerOwnedRect() {
		BukovMobileNavigationLayout.Rect result =
				new BukovMobileNavigationLayout.Rect();
		assertSame(
				result,
				BukovMobileNavigationLayout.calculate(
						240f,
						135f,
						6f,
						53f,
						8f,
						5f,
						BukovRaidHudState.Direction.SE,
						result));
	}

	@Test
	public void targetAtInnerViewportMarginCountsAsOnScreen() {
		assertTrue(BukovMobileNavigationLayout.targetInsideWorldViewport(
				100f,
				100f,
				-5.75f,
				0f,
				16f,
				0f,
				0f,
				200f,
				200f,
				8f));
		assertFalse(BukovMobileNavigationLayout.targetInsideWorldViewport(
				100f,
				100f,
				-5.751f,
				0f,
				16f,
				0f,
				0f,
				200f,
				200f,
				8f));
	}

	@Test
	public void targetInsideViewportRetractsCueAndOutsideKeepsIt() {
		assertTrue(BukovMobileNavigationLayout.targetInsideWorldViewport(
				160f,
				90f,
				3f,
				-2f,
				16f,
				0f,
				0f,
				320f,
				180f,
				8f));
		assertFalse(BukovMobileNavigationLayout.targetInsideWorldViewport(
				160f,
				90f,
				12f,
				0f,
				16f,
				0f,
				0f,
				320f,
				180f,
				8f));
		assertFalse(BukovMobileNavigationLayout.targetInsideWorldViewport(
				160f,
				90f,
				0f,
				-6f,
				16f,
				0f,
				0f,
				320f,
				180f,
				8f));
	}

	@Test
	public void invalidWorldGeometryFailsClosed() {
		assertFalse(BukovMobileNavigationLayout.targetInsideWorldViewport(
				0f,
				0f,
				1f,
				1f,
				16f,
				0f,
				0f,
				Float.NaN,
				180f,
				8f));
	}
}
