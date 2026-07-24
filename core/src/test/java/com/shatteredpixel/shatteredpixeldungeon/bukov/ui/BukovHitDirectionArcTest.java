package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BukovHitDirectionArcTest {

	@Test
	public void mapsEightDirectionsClockwiseFromViewportTop() {
		assertEquals(0f, angle(BukovRaidHudState.Direction.N), 0f);
		assertEquals(45f, angle(BukovRaidHudState.Direction.NE), 0f);
		assertEquals(90f, angle(BukovRaidHudState.Direction.E), 0f);
		assertEquals(135f, angle(BukovRaidHudState.Direction.SE), 0f);
		assertEquals(180f, angle(BukovRaidHudState.Direction.S), 0f);
		assertEquals(225f, angle(BukovRaidHudState.Direction.SW), 0f);
		assertEquals(270f, angle(BukovRaidHudState.Direction.W), 0f);
		assertEquals(315f, angle(BukovRaidHudState.Direction.NW), 0f);
	}

	private static float angle(BukovRaidHudState.Direction direction) {
		return BukovHitDirectionArc.angleFor(direction);
	}
}
