package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BukovRaidHudScaleTest {

	@Test
	public void settingsLevelsScaleCompactGeometryWithoutInventingFontSizes() {
		assertEquals(1f, BukovRaidHud.scaleMultiplier(0), 0f);
		assertEquals(1.25f, BukovRaidHud.scaleMultiplier(1), 0f);
		assertEquals(1.5f, BukovRaidHud.scaleMultiplier(2), 0f);
		assertEquals(1f, BukovRaidHud.scaleMultiplier(-4), 0f);
		assertEquals(1.5f, BukovRaidHud.scaleMultiplier(9), 0f);
		float compactNormal = BukovRaidHud.preferredHeight(127f, 0);
		assertTrue(
				BukovRaidHud.preferredHeight(127f, 1)
						> compactNormal);
		assertEquals(
				compactNormal * 1.5f,
				BukovRaidHud.preferredHeight(127f, 2),
				0.0001f);
		// Wide rows are fixed; growing only the background would consume the
		// compact iPhone landscape without enlarging any control.
		float wideNormal = BukovRaidHud.preferredHeight(320f, 0);
		assertEquals(
				wideNormal,
				BukovRaidHud.preferredHeight(320f, 2),
				0f);
		assertEquals(
				BukovMessages.get(
						"bukov.raid.hud.control_hint_desktop"),
				BukovRaidHud.controlHint(true));
		assertEquals(
				BukovMessages.get(
						"bukov.raid.hud.control_hint_touch"),
				BukovRaidHud.controlHint(false));
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
