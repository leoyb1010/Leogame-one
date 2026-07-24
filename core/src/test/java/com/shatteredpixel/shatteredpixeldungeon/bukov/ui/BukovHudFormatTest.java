package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BukovHudFormatTest {

	@Test
	public void formatsRaidClockWithoutLocaleDependencies() {
		assertEquals("00:00", BukovHudFormat.clock(-1f));
		assertEquals("09:05", BukovHudFormat.clock(545.9f));
		assertEquals("01:02:03", BukovHudFormat.clock(3723f));
	}

	@Test
	public void formatsLiveVitalsAndAmmo() {
		assertEquals("HP 72/100 +12", BukovHudFormat.health(72, 100, 12));
		assertEquals("护甲 2-8", BukovHudFormat.armor(2, 8));
		assertEquals("护甲 --", BukovHudFormat.armor(null, null));
		assertEquals("弹药 17 / 90", BukovHudFormat.ammo(17, 90));
		assertEquals("弹药 -- / --", BukovHudFormat.ammo(null, null));
		assertEquals("17 | 90", BukovHudFormat.tacticalAmmo(
				"针蜂-9", 17, 24, 90));
		assertEquals("-- | --", BukovHudFormat.tacticalAmmo(
				null, 17, 24, 90));
		assertEquals("针蜂-9 · 单发",
				BukovHudFormat.weapon("针蜂-9", false));
		assertEquals("城防-556 · 自动",
				BukovHudFormat.weapon("城防-556", true));
	}

	@Test
	public void fallsBackToRequiredFirstRaidObjective() {
		assertEquals(BukovHudFormat.DEFAULT_OBJECTIVE, BukovHudFormat.objective("  "));
		assertEquals("开启泵站", BukovHudFormat.objective("  开启泵站  "));
	}

	@Test
	public void formatsInjuriesReloadAndInteractionWithoutFrameUnits() {
		assertEquals("状态稳定",
				BukovHudFormat.status(0f, false, 0f, 0f, 0f));
		assertEquals(
				"流血 0.4/秒 · 骨折 · 震荡 2.5秒 · 疼痛",
				BukovHudFormat.status(0.4f, true, 0.2f, 2.5f, 0f));
		assertEquals("换弹 35%", BukovHudFormat.reload(true, 0.35f));
		assertEquals(
				"按住互动 · 搜索容器 1.2秒",
				BukovHudFormat.interaction(
						BukovRaidHudState.Interaction.SEARCH,
						"搜索容器",
						0f,
						1.2f));
		assertEquals(
				"按住 E · 搜索容器 1.2秒",
				BukovHudFormat.interaction(
						BukovRaidHudState.Interaction.SEARCH,
						"搜索容器",
						0f,
						1.2f,
						true));
		assertEquals(
				"按 E · 拾取物资",
				BukovHudFormat.interaction(
						BukovRaidHudState.Interaction.PICKUP,
						"拾取物资",
						0f,
						0f,
						true));
		assertEquals(
				"按 E · 使用维修钥匙解锁",
				BukovHudFormat.interaction(
						BukovRaidHudState.Interaction.UNLOCK,
						"使用维修钥匙解锁",
						0f,
						0f,
						true));
		assertEquals(
				"不可交互 · 需要维修钥匙",
				BukovHudFormat.interaction(
						BukovRaidHudState.Interaction.LOCKED,
						"需要维修钥匙",
						0f,
						0f,
						true));
		assertEquals(
				"搜索容器 50%",
				BukovHudFormat.interaction(
						BukovRaidHudState.Interaction.SEARCH,
						"搜索容器",
						0.5f,
						1.2f));
	}

	@Test
	public void formatsExtractionAvailabilityAndActiveCountdown() {
		assertEquals(
				"撤离点 2 可用",
				BukovHudFormat.extraction(2, null, false, false, 0f, 0f));
		assertEquals(
				"撤离 E02 · 未开放",
				BukovHudFormat.extraction(1, "E02", false, false, 0f, 8f));
		assertEquals(
				"撤离 E01 · 3.0秒",
				BukovHudFormat.extraction(1, "E01", true, true, 0.4f, 5f));
	}
}
