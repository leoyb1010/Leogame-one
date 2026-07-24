package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BukovRaidHudScaleTest {

	@Test
	public void settingsLevelsChangeHudGeometryByDisplayedPercentage() {
		assertEquals(1f, BukovRaidHud.scaleMultiplier(0), 0f);
		assertEquals(1.25f, BukovRaidHud.scaleMultiplier(1), 0f);
		assertEquals(1.5f, BukovRaidHud.scaleMultiplier(2), 0f);
		assertEquals(1f, BukovRaidHud.scaleMultiplier(-4), 0f);
		assertEquals(1.5f, BukovRaidHud.scaleMultiplier(9), 0f);
		float normal = BukovRaidHud.preferredHeight(320f, 0);
		assertTrue(BukovRaidHud.preferredHeight(320f, 1) > normal);
		assertEquals(normal * 1.5f,
				BukovRaidHud.preferredHeight(320f, 2),
				0.0001f);
		assertEquals(7, BukovRaidHud.textSize(7, 0));
		assertEquals(8, BukovRaidHud.textSize(7, 1));
		assertEquals(9, BukovRaidHud.textSize(7, 2));
		assertTrue(BukovRaidHud.controlHint(true).contains("TAB"));
		assertTrue(BukovRaidHud.controlHint(true).contains("暂停"));
		assertTrue(BukovRaidHud.controlHint(false).contains("背包"));
		assertTrue(BukovRaidHud.controlHint(false).contains("暂停"));
	}

	@Test
	public void awarenessBadgesStayCompactAtTheHudEdge() {
		assertEquals(84f,
				BukovRaidHud.awarenessBadgeWidth(360f, 1f),
				0f);
		assertEquals(96f,
				BukovRaidHud.awarenessBadgeWidth(360f, 1.5f),
				0f);
		assertEquals(42f,
				BukovRaidHud.awarenessBadgeWidth(100f, 1f),
				0f);
	}
}
