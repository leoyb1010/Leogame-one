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
			assertBandBelow(
					layout.vitalSecondary,
					layout.vitalPrimary);
			assertBandBelow(
					layout.firepowerSecondary,
					layout.firepowerPrimary);
			assertTrue(layout.condition.width > layout.vitals.width);
			assertEquals(0f, layout.medicalHint.width, 0f);
			assertFalse(layout.condition.overlaps(layout.clock));
			assertFalse(layout.condition.overlaps(layout.medicalHint));
			assertFalse(layout.medicalHint.overlaps(layout.clock));
			assertBandBelow(layout.clock, layout.condition);
			assertBandBelow(layout.clock, layout.medicalHint);
			assertBandBelow(layout.extraction, layout.condition);
			assertBandBelow(layout.extraction, layout.medicalHint);
			assertBandBelow(layout.extraction, layout.clock);
			assertBandBelow(layout.objective, layout.extraction);
			assertTrue(layout.objective.bottom() < layout.height);
		}
	}

	@Test
	public void realIphonePointViewportUsesPortraitBandsNotWideColumns() {
		for (int scaleLevel = 0; scaleLevel <= 2; scaleLevel++) {
			BukovRaidHudLayout portrait =
					BukovRaidHudLayout.calculate(
							402f,
							874f,
							scaleLevel);
			assertTrue(portrait.compact);
			assertFalse(portrait.vitals.overlaps(portrait.firepower));
			assertBandBelow(portrait.condition, portrait.vitals);
			assertBandBelow(portrait.clock, portrait.condition);
			assertBandBelow(portrait.extraction, portrait.clock);
			assertBandBelow(portrait.objective, portrait.extraction);
			assertTrue(portrait.objective.bottom() < portrait.height);
		}

		assertFalse(BukovRaidHudLayout.calculate(
				874f, 402f, 0).compact);
		assertFalse(BukovRaidHudLayout.calculate(
				960f, 600f, 0).compact);
		assertEquals(
				46f,
				BukovRaidHudLayout.preferredHeight(
						874f, 402f, 2),
				0f);
	}

	@Test
	public void realIphoneChineseAndEnglishPriorityCopyStaysSingleLine() {
		BukovRaidHudLayout portrait =
				BukovRaidHudLayout.calculate(402f, 874f, 0);
		String chineseHealth =
				BukovRaidHudLayout.compactPrimaryLine(
						"HP 100/100 +999",
						portrait.vitalPrimary.width - 10f,
						0);
		String chineseStatus =
				BukovRaidHudLayout.compactLine(
						"流血 2.5/秒 · 骨折 · 震荡 12.5秒 · 疼痛",
						portrait.condition.width,
						0);
		String englishWeapon =
				BukovRaidHudLayout.compactLine(
						"Needle-9 · Single",
						portrait.firepowerSecondary.width,
						0);
		String englishExtraction =
				BukovRaidHudLayout.compactLine(
						"Extraction points available: 2",
						portrait.extraction.width,
						0);

		assertFalse(chineseHealth.contains("\n"));
		assertFalse(chineseStatus.contains("\n"));
		assertFalse(englishWeapon.contains("\n"));
		assertFalse(englishExtraction.contains("\n"));
		assertTrue(chineseHealth.codePointCount(
				0, chineseHealth.length()) <= 24);
		assertTrue(chineseStatus.codePointCount(
				0, chineseStatus.length()) <= 32);
		assertTrue(englishWeapon.codePointCount(
				0, englishWeapon.length()) <= 32);
		assertTrue(englishExtraction.codePointCount(
				0, englishExtraction.length()) <= 32);
	}

	@Test
	public void iphonePortraitTutorialOwnsASeparateRowFromHudAndControls() {
		float viewportWidth = 135f;
		float viewportHeight = 291f;
		float safeTop = 6f;
		float hudTop = safeTop + 4f;
		for (int scaleLevel = 0; scaleLevel <= 2; scaleLevel++) {
			BukovRaidHudLayout hud =
					BukovRaidHudLayout.calculate(127f, scaleLevel);
			float hudBottom = hudTop + hud.height;
			BukovRaidHudLayout.Rect feedback =
					BukovRaidHudLayout.mobileFeedback(
							viewportWidth,
							viewportHeight,
							4f,
							hudBottom);
			BukovRaidHudLayout.Rect tutorial =
					BukovRaidHudLayout.portraitTutorialHint(
							viewportWidth,
							viewportHeight,
							hudBottom,
							scaleLevel);
			BukovTouchLayout touch = BukovTouchLayout.calculate(
					viewportWidth,
					viewportHeight,
					4f,
					safeTop,
					4f,
					10f,
					hudBottom + 2f);

			assertTrue(tutorial.y >= hudBottom);
			assertTrue(tutorial.bottom() <= viewportHeight);
			assertFalse(tutorial.overlaps(feedback));
			assertFalse(overlaps(tutorial, touch.backpack));
			assertFalse(overlaps(tutorial, touch.pause));
			assertFalse(overlaps(tutorial, touch.movement));
			assertFalse(overlaps(tutorial, touch.aimFire));
			assertFalse(overlaps(tutorial, touch.interact));
			assertFalse(overlaps(tutorial, touch.reload));
			assertFalse(overlaps(tutorial, touch.medical));
			assertFalse(overlaps(tutorial, touch.drop));
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
	public void wideLandscapeDoesNotGrowAnEmptyHudBackground() {
		assertEquals(
				46f,
				BukovRaidHudLayout.preferredHeight(232f, 0),
				0f);
		assertEquals(
				46f,
				BukovRaidHudLayout.preferredHeight(232f, 2),
				0f);
	}

	@Test
	public void v2ReloadRingStaysTwentyFourSquareInsideCompactFirepower() {
		for (int scaleLevel = 0; scaleLevel <= 2; scaleLevel++) {
			BukovRaidHudLayout layout =
					BukovRaidHudLayout.calculate(127f, scaleLevel);
			BukovRaidHudLayout.Rect ring =
					BukovRaidHudLayout.compactReloadRing(
							127f, scaleLevel);

			assertEquals(24f, ring.width, 0f);
			assertEquals(24f, ring.height, 0f);
			assertTrue(ring.x >= layout.firepower.x);
			assertTrue(ring.y >= layout.firepower.y);
			assertTrue(ring.right() <= layout.firepower.right());
			assertTrue(ring.bottom() <= layout.firepower.bottom());
			assertFalse(ring.overlaps(layout.condition));
			assertFalse(ring.overlaps(layout.medicalHint));
		}
	}

	@Test
	public void mobileInteractionRailAvoidsNavigationAndSticks() {
		assertMobileFeedbackClear(
				135f, 225f, 4f, 6f, 4f, 10f, 128.5f);
		assertMobileFeedbackClear(
				240f, 135f, 6f, 3f, 6f, 5f, 53f);
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
		String primary = BukovRaidHudLayout.compactPrimaryLine(
				"HP 100/100 +999",
				47f,
				0);

		assertTrue(chinese.endsWith("…"));
		assertTrue(chinese.codePointCount(0, chinese.length()) <= 32);
		assertTrue(english.endsWith("…"));
		assertTrue(english.codePointCount(0, english.length()) <= 28);
		assertTrue(emoji.endsWith("…"));
		assertFalse(emoji.contains("\uFFFD"));
		assertTrue(primary.endsWith("…"));
		assertFalse(primary.contains("\n"));
		assertTrue(primary.codePointCount(0, primary.length()) <= 8);
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

	private static void assertMobileFeedbackClear(
			float viewportWidth,
			float viewportHeight,
			float safeLeft,
			float safeTop,
			float safeRight,
			float safeBottom,
			float hudBottom) {
		BukovRaidHudLayout.Rect feedback =
				BukovRaidHudLayout.mobileFeedback(
						viewportWidth,
						viewportHeight,
						safeLeft + 4f,
						hudBottom);
		BukovTouchLayout touch = BukovTouchLayout.calculate(
				viewportWidth,
				viewportHeight,
				safeLeft,
				safeTop,
				safeRight,
				safeBottom,
				hudBottom + 2f);

		assertTrue(feedback.x >= safeLeft);
		assertTrue(feedback.y >= 0f);
		assertTrue(feedback.right() <= viewportWidth);
		assertTrue(feedback.bottom() <= viewportHeight);
		assertFalse(overlaps(feedback, touch.movement));
		assertFalse(overlaps(feedback, touch.aimFire));
		assertFalse(overlaps(feedback, touch.interact));
		assertFalse(overlaps(feedback, touch.drop));
		assertFalse(overlaps(feedback, touch.reload));
		assertFalse(overlaps(feedback, touch.medical));
		assertFalse(overlaps(feedback, touch.backpack));
		assertFalse(overlaps(feedback, touch.pause));
	}

	private static boolean overlaps(
			BukovRaidHudLayout.Rect first,
			BukovTouchLayout.Rect second) {
		return first.x < second.right()
				&& first.right() > second.x
				&& first.y < second.bottom()
				&& first.bottom() > second.y;
	}
}
