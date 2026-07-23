package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.SoundCategory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovRaidHudStateTest {

	@Test
	public void clampsRealtimeValuesAtPresentationBoundary() {
		BukovRaidHudState state = new BukovRaidHudState();
		state.beginFrame("  取回维修档案  ", Float.NaN);
		state.weapon(
				"针蜂-9",
				false,
				99,
				24,
				-7,
				0.7f,
				1.4f);
		state.status(
				Float.POSITIVE_INFINITY,
				true,
				2f,
				-4f,
				Float.NaN);
		state.interaction(
				BukovRaidHudState.Interaction.SEARCH,
				" 搜索容器 ",
				1.5f,
				-1f);
		state.extraction(
				-2,
				" E01 ",
				true,
				true,
				-0.5f,
				5f);

		assertEquals("取回维修档案", state.objective());
		assertEquals(0f, state.raidElapsedSeconds(), 0f);
		assertEquals(24, state.magazine());
		assertEquals(0, state.reserve());
		assertTrue(state.reloading());
		assertEquals(0.5f, state.reloadProgress(), 0.0001f);
		assertEquals(0f, state.bleedingPerSecond(), 0f);
		assertTrue(state.fractured());
		assertEquals(1f, state.painSeverity(), 0f);
		assertEquals(1f, state.interactionProgress(), 0f);
		assertEquals(0f, state.interactionSeconds(), 0f);
		assertEquals(0, state.availableExtractions());
		assertEquals("E01", state.extractionId());
		assertEquals(0f, state.extractionProgress(), 0f);
	}

	@Test
	public void beginFrameClearsStaleTransientPresentation() {
		BukovRaidHudState state = new BukovRaidHudState();
		state.beginFrame("任务 A", 10f);
		state.weapon("城防-556", true, 12, 24, 48, 1f, 2f);
		state.status(0.5f, true, 0.4f, 3f, 5f);
		state.interaction(
				BukovRaidHudState.Interaction.EXTRACT,
				"撤离中",
				0.5f,
				5f);
		state.extraction(2, "E01", true, true, 0.5f, 5f);
		state.presentationSettings(true, 2);
		state.sound(
				SoundCategory.ENEMY_GUNSHOT,
				BukovRaidHudState.Direction.NW,
				BukovRaidHudState.Distance.FAR,
				0.8f,
				0.9f);
		state.hit(BukovRaidHudState.Direction.E, 0.7f, 0.8f);
		state.boss(
				"白线", 2, 3, "诱饵搜索",
				67, 100, false, "识别真身", true);

		state.beginFrame("任务 B", 11f);

		assertEquals("任务 B", state.objective());
		assertEquals(0, state.magazineCapacity());
		assertFalse(state.reloading());
		assertFalse(state.fractured());
		assertEquals(BukovRaidHudState.Interaction.NONE, state.interaction());
		assertFalse(state.extractionActive());
		assertEquals(0, state.availableExtractions());
		assertFalse(state.soundVisible());
		assertFalse(state.hitVisible());
		assertFalse(state.bossActive());
		assertFalse(state.colorblindAssist());
	}

	@Test
	public void combatAwarenessClampsAndCarriesTextRedundancy() {
		BukovRaidHudState state = new BukovRaidHudState();
		state.beginFrame("击败白线", 30f);
		state.presentationSettings(true, 9);
		state.sound(
				SoundCategory.ENEMY_GUNSHOT,
				BukovRaidHudState.Direction.NW,
				BukovRaidHudState.Distance.FAR,
				2f,
				0.9f);
		state.hit(BukovRaidHudState.Direction.E, 2f, 0.85f);
		state.boss(
				"白线", 5, 3, "雾灯过载",
				999, 180, true, "关闭雾灯", true);

		assertEquals(2, state.damageNumbersMode());
		assertEquals(1f, state.soundStrength(), 0f);
		assertEquals(1f, state.hitStrength(), 0f);
		assertEquals(3, state.bossPhase());
		assertEquals(180, state.bossHealth());
		assertEquals(1f, state.bossHealthFraction(), 0f);
		assertTrue(BukovCombatHudFormat.sound(state).contains("↖ 西北"));
		assertTrue(BukovCombatHudFormat.sound(state).contains("○ 远"));
		assertTrue(BukovCombatHudFormat.hit(state).contains("→ 东"));
		assertTrue(BukovCombatHudFormat.bossObjective(state)
				.contains("◇ 弱点开放"));
		assertTrue(BukovCombatHudFormat.bossObjective(state)
				.contains("⚠ 建议撤离"));
	}
}
