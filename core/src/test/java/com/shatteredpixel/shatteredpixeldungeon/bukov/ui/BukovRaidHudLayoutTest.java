package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovRaidHudLayoutTest {

	@Test
	public void portraitBandsNeverOverlapAtAnyUiScale() {
		for (int scaleLevel = 0; scaleLevel <= 2; scaleLevel++) {
			BukovRaidHudLayout layout =
					BukovRaidHudLayout.calculate(127f, scaleLevel);

			assertTrue(layout.compact);
			assertFalse(layout.vitals.overlaps(layout.firepower));
			assertFalse(layout.condition.overlaps(layout.clock));
			assertBandBelow(layout.extraction, layout.condition);
			assertBandBelow(layout.extraction, layout.clock);
			assertBandBelow(layout.objective, layout.extraction);
			assertTrue(layout.objective.bottom() < layout.height);
		}
	}

	@Test
	public void largeUiScaleReservesItsRealHeightFromTouchNavigation() {
		float safeTop = 6f;
		float hudTop = safeTop + 4f;
		BukovRaidHudLayout hud =
				BukovRaidHudLayout.calculate(127f, 2);
		float reservedBottom = hudTop + hud.height + 2f;
		BukovTouchLayout touch = BukovTouchLayout.calculate(
				135f,
				225f,
				4f,
				safeTop,
				4f,
				10f,
				reservedBottom);

		assertTrue(touch.backpack.y >= reservedBottom);
		assertTrue(touch.pause.y >= reservedBottom);
		assertFalse(touch.backpack.overlaps(touch.movement));
		assertFalse(touch.pause.overlaps(touch.aimFire));
	}

	@Test
	public void compactCopyHasBoundedUnicodeSafePriorityLines() {
		String chinese = BukovRaidHudLayout.compactObjective(
				"主线：前往背腰索维修间，找到通道档案，然后抵达北部泵站并启动紧急供电",
				103f,
				0);
		String english = BukovRaidHudLayout.compactObjective(
				"Primary objective: recover the maintenance archive and reach extraction",
				103f,
				2);
		String emoji = BukovRaidHudLayout.compactLine(
				"状态稳定🙂但需要继续检查装备",
				48f,
				2);

		assertTrue(chinese.endsWith("…"));
		assertTrue(chinese.codePointCount(0, chinese.length()) <= 32);
		assertTrue(english.endsWith("…"));
		assertTrue(english.codePointCount(0, english.length()) <= 28);
		assertTrue(emoji.endsWith("…"));
		assertFalse(emoji.contains("\uFFFD"));
		assertEquals("状态 稳定", BukovRaidHudLayout.compactLine(
				"  状态   稳定  ", 100f, 0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsMissingHudWidth() {
		BukovRaidHudLayout.calculate(0f, 0);
	}

	private static void assertBandBelow(
			BukovRaidHudLayout.Rect lower,
			BukovRaidHudLayout.Rect upper) {
		assertTrue(lower.y >= upper.bottom());
	}
}
