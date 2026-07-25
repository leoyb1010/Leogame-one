package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class BukovResponsiveUiMatrixTest {

	private static final String LONGEST_ZH =
			"主线：搜索维修间全部可交互容器，回收通道档案并在倒计时结束前抵达紧急撤离点";
	private static final String LONGEST_EN =
			"Primary objective: search every maintenance container, recover the access archive, and reach emergency extraction before the raid timer expires";

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> viewports() {
		return Arrays.asList(new Object[][] {
				{"iPhone minimum portrait", true,
						135f, 225f, 4f, 6f, 4f, 10f},
				{"iPhone tall portrait", true,
						135f, 291f, 4f, 14f, 4f, 10f},
				{"iPhone minimum landscape", true,
						240f, 135f, 6f, 3f, 6f, 5f},
				{"iPhone compact landscape", true,
						240f, 160f, 6f, 3f, 6f, 5f},
				{"iPad portrait", true,
						341f, 454f, 8f, 8f, 8f, 10f},
				{"iPad landscape", true,
						454f, 341f, 8f, 8f, 8f, 10f},
				{"macOS minimum", false,
						360f, 200f, 0f, 0f, 0f, 0f},
				{"macOS compact window", false,
						640f, 360f, 0f, 0f, 0f, 0f},
				{"macOS standard window", false,
						960f, 540f, 0f, 0f, 0f, 0f}
		});
	}

	private final boolean touchEnabled;
	private final float width;
	private final float height;
	private final float safeLeft;
	private final float safeTop;
	private final float safeRight;
	private final float safeBottom;

	public BukovResponsiveUiMatrixTest(
			String ignoredName,
			boolean touchEnabled,
			float width,
			float height,
			float safeLeft,
			float safeTop,
			float safeRight,
			float safeBottom) {
		this.touchEnabled = touchEnabled;
		this.width = width;
		this.height = height;
		this.safeLeft = safeLeft;
		this.safeTop = safeTop;
		this.safeRight = safeRight;
		this.safeBottom = safeBottom;
	}

	@Test
	public void responsiveMatrixKeepsEveryReservedSurfaceSeparated() {
		String[] localeWorstCases = {LONGEST_ZH, LONGEST_EN};
		for (int scaleLevel = 0; scaleLevel <= 2; scaleLevel++) {
			float hudWidth = touchEnabled
					? Math.max(
							1f,
							width - safeLeft - safeRight - 8f)
					: BukovRaidHudLayout.desktopHudWidth(
							width,
							safeLeft,
							safeRight);
			float hudTop = safeTop + 4f;
			if (!touchEnabled) {
				float hudLeft =
						safeLeft + BukovRaidHudLayout.HUD_SIDE_INSET;
				float pauseLeft = BukovRaidHudLayout.desktopPauseX(
						width, safeRight);
				assertTrue(hudLeft + hudWidth <= pauseLeft);
				assertTrue(
						pauseLeft
								+ BukovRaidHudLayout.DESKTOP_PAUSE_WIDTH
						<= width - safeRight
								- BukovRaidHudLayout
										.DESKTOP_PAUSE_RIGHT_MARGIN);
			}
			BukovRaidHudLayout hud = BukovRaidHudLayout.calculate(
					hudWidth,
					height,
					scaleLevel);
			float hudBottom = hudTop + hud.height;
			assertTrue(hudTop >= safeTop);
			assertTrue(hudBottom <= height - safeBottom);
			assertHudBandsSeparated(hud);

			for (String copy : localeWorstCases) {
				assertCopyBounded(copy, hud, scaleLevel);
			}
			if (!touchEnabled) continue;

			for (boolean tutorialVisible : new boolean[] {false, true}) {
				for (boolean interactionVisible
						: new boolean[] {false, true}) {
					for (BukovRaidHudState.Direction direction
							: BukovRaidHudState.Direction.values()) {
						BukovResponsiveUiLayout layout =
								BukovResponsiveUiLayout.calculateMobile(
										width,
										height,
										safeLeft,
										safeTop,
										safeRight,
										safeBottom,
										hudBottom,
										scaleLevel,
										interactionVisible,
										direction,
										tutorialVisible);
						assertTouchControls(layout.touch, hudBottom);
						assertOverlay(layout.interaction, layout.touch);
						assertOverlay(layout.navigation, layout.touch);
						assertOverlay(layout.tutorial, layout.touch);
						assertFalse(layout.interaction.overlaps(
								layout.navigation));
						assertFalse(layout.interaction.overlaps(
								layout.tutorial));
						assertFalse(layout.navigation.overlaps(
								layout.tutorial));
						if (!layout.navigation.visible()) {
							assertFalse(
									"tutorial must yield when navigation "
											+ "cannot fit",
									layout.tutorial.visible());
						}
					}
				}
			}
		}
	}

	@Test
	public void stableProductionInputsReuseTheCachedLayout() {
		if (!touchEnabled) return;
		BukovResponsiveUiLayout.Cache cache =
				new BukovResponsiveUiLayout.Cache();
		float hudWidth = width - safeLeft - safeRight - 8f;
		float hudBottom = safeTop + 4f
				+ BukovRaidHudLayout.preferredHeight(
						hudWidth,
						height,
						2);
		BukovResponsiveUiLayout first = cache.layout(
				width,
				height,
				safeLeft,
				safeTop,
				safeRight,
				safeBottom,
				hudBottom,
				2,
				true,
				BukovRaidHudState.Direction.NE,
				true);
		for (int frame = 0; frame < 240; frame++) {
			assertSame(
					first,
					cache.layout(
							width,
							height,
							safeLeft,
							safeTop,
							safeRight,
							safeBottom,
							hudBottom,
							2,
							true,
							BukovRaidHudState.Direction.NE,
							true));
		}
		assertTrue(cache.recomputations() == 1);

		BukovResponsiveUiLayout changedDirection = cache.layout(
				width,
				height,
				safeLeft,
				safeTop,
				safeRight,
				safeBottom,
				hudBottom,
				2,
				true,
				BukovRaidHudState.Direction.SW,
				true);
		assertFalse(first == changedDirection);
		assertTrue(cache.recomputations() == 2);

		BukovResponsiveUiLayout changedVisibility = cache.layout(
				width,
				height,
				safeLeft,
				safeTop,
				safeRight,
				safeBottom,
				hudBottom,
				2,
				false,
				BukovRaidHudState.Direction.SW,
				true);
		assertFalse(changedDirection == changedVisibility);
		assertTrue(cache.recomputations() == 3);
	}

	private static void assertHudBandsSeparated(BukovRaidHudLayout hud) {
		assertFalse(hud.vitals.overlaps(hud.firepower));
		if (hud.compact) {
			assertFalse(hud.condition.overlaps(hud.clock));
			assertFalse(hud.clock.overlaps(hud.extraction));
			assertFalse(hud.extraction.overlaps(hud.objective));
			assertTrue(hud.objective.bottom() <= hud.height);
		}
	}

	private static void assertCopyBounded(
			String copy,
			BukovRaidHudLayout hud,
			int scaleLevel) {
		float objectiveWidth =
				Math.max(1f, hud.objective.width - 12f);
		float captionWidth =
				Math.max(1f, hud.extraction.width);
		String objective = hud.compact
				? BukovRaidHudLayout.compactObjective(
						copy,
						objectiveWidth,
						scaleLevel)
				: BukovRaidHudLayout.compactBodyLine(
						copy,
						objectiveWidth,
						scaleLevel);
		String line = BukovRaidHudLayout.compactLine(
				copy,
				captionWidth,
				scaleLevel);
		BukovRaidHudLayout.TextFootprint objectiveFootprint =
				BukovRaidHudLayout.objectiveFootprint(
						objective,
						objectiveWidth,
						scaleLevel);
		BukovRaidHudLayout.TextFootprint captionFootprint =
				BukovRaidHudLayout.captionFootprint(
						line,
						captionWidth,
						scaleLevel);
		assertNotNull(objective);
		assertNotNull(line);
		assertFalse(objective.contains("\n"));
		assertFalse(line.contains("\n"));
		assertTrue(objectiveFootprint.fits(
				objectiveWidth,
				hud.objective.height,
				hud.compact ? 2 : 1));
		assertTrue(captionFootprint.fits(
				captionWidth,
				hud.extraction.height,
				1));
	}

	private static void assertTouchControls(
			BukovTouchLayout touch,
			float hudBottom) {
		BukovTouchLayout.Rect[] controls = controls(touch);
		for (BukovTouchLayout.Rect control : controls) {
			assertTrue(touch.safeBounds.contains(control));
			assertTrue(control.y >= hudBottom + 2f);
		}
		for (int first = 0; first < controls.length; first++) {
			for (int second = first + 1;
					second < controls.length;
					second++) {
				assertFalse(
						"touch controls overlap: "
								+ first + " and " + second,
						controls[first].overlaps(controls[second]));
			}
		}
	}

	private static void assertOverlay(
			BukovResponsiveUiLayout.Overlay overlay,
			BukovTouchLayout touch) {
		assertNotNull(overlay.presentation);
		if (!overlay.visible()) {
			assertTrue(overlay.width == 0f);
			assertTrue(overlay.height == 0f);
			return;
		}
		assertTrue(overlay.width > 0f);
		assertTrue(overlay.height > 0f);
		assertTrue(overlay.x >= touch.safeBounds.x);
		assertTrue(overlay.y >= touch.safeBounds.y);
		assertTrue(overlay.right() <= touch.safeBounds.right());
		assertTrue(overlay.bottom() <= touch.safeBounds.bottom());
		for (BukovTouchLayout.Rect control : controls(touch)) {
			assertFalse(overlaps(overlay, control));
		}
	}

	private static BukovTouchLayout.Rect[] controls(
			BukovTouchLayout touch) {
		return new BukovTouchLayout.Rect[] {
				touch.movement,
				touch.aimFire,
				touch.interact,
				touch.reload,
				touch.medical,
				touch.drop,
				touch.backpack,
				touch.pause
		};
	}

	private static boolean overlaps(
			BukovResponsiveUiLayout.Overlay first,
			BukovTouchLayout.Rect second) {
		return first.x < second.right()
				&& first.right() > second.x
				&& first.y < second.bottom()
				&& first.bottom() > second.y;
	}
}
