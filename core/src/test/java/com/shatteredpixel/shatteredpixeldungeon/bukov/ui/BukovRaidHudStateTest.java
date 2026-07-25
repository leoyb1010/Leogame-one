package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.SoundCategory;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
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
		state.mobility(2f, true, -1f);
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
		assertEquals(1f, state.staminaFraction(), 0f);
		assertTrue(state.sprinting());
		assertEquals(0f, state.carriedLoadFraction(), 0f);
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
		state.mobility(0.25f, true, 0.8f);
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
		state.aim(1f, -1f, true);
		state.navigation(
				BukovRaidHudState.Cue.MISSION,
				-4f,
				-4f,
				6f,
				"维修档案",
				true);
		state.threat(3f, 0f, 3f, "拾荒枪手", true);

		state.beginFrame("任务 B", 11f);

		assertEquals("任务 B", state.objective());
		assertEquals(0, state.magazineCapacity());
		assertFalse(state.reloading());
		assertFalse(state.fractured());
		assertEquals(1f, state.staminaFraction(), 0f);
		assertFalse(state.sprinting());
		assertEquals(0f, state.carriedLoadFraction(), 0f);
		assertEquals(BukovRaidHudState.Interaction.NONE, state.interaction());
		assertFalse(state.extractionActive());
		assertEquals(0, state.availableExtractions());
		assertFalse(state.soundVisible());
		assertFalse(state.hitVisible());
		assertFalse(state.bossActive());
		assertFalse(state.colorblindAssist());
		assertFalse(state.aimVisible());
		assertFalse(state.navigationVisible());
		assertEquals(0f, state.navigationDeltaX(), 0f);
		assertEquals(0f, state.navigationDeltaY(), 0f);
		assertFalse(state.threatVisible());
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
		assertTrue(BukovCombatHudFormat.sound(state).contains(
				"↖ " + BukovMessages.get(
						"bukov.raid.combat.direction_northwest")));
		assertTrue(BukovCombatHudFormat.sound(state).contains(
				". " + BukovMessages.get(
						"bukov.raid.combat.distance_far")));
		assertTrue(BukovCombatHudFormat.hit(state).contains(
				"→ " + BukovMessages.get(
						"bukov.raid.combat.direction_east")));
		assertTrue(BukovCombatHudFormat.bossObjective(state)
				.contains(BukovMessages.get(
						"bukov.raid.combat.boss_weak_open")));
		assertTrue(BukovCombatHudFormat.bossObjective(state)
				.contains(BukovMessages.get(
						"bukov.raid.combat.boss_retreat_warning")));
	}

	@Test
	public void combatAwarenessCarriesThreeIndependentHitDirections() {
		BukovRaidHudState state = new BukovRaidHudState();
		state.beginFrame("突围", 30f);
		state.combatAwareness(0.1f);
		state.hit(BukovRaidHudState.Direction.N, 0.3f, 0.5f);
		state.hit(BukovRaidHudState.Direction.E, 0.6f, 0.4f);
		state.hit(BukovRaidHudState.Direction.S, 0.9f, 0.3f);
		state.hit(BukovRaidHudState.Direction.W, 1f, 0.2f);

		assertEquals(
				BukovCombatHudTimeline.IDLE_ALPHA,
				state.combatAwarenessAlpha(),
				0f);
		assertEquals(3, state.hitCount());
		assertEquals(
				BukovRaidHudState.Direction.N,
				state.hitDirection(0));
		assertEquals(
				BukovRaidHudState.Direction.S,
				state.hitDirection(2));
		assertTrue(BukovCombatHudFormat.hit(state).contains(
				"↑ " + BukovMessages.get(
						"bukov.raid.combat.direction_north")));
		assertTrue(BukovCombatHudFormat.hit(state).contains(
				"→ " + BukovMessages.get(
						"bukov.raid.combat.direction_east")));
		assertTrue(BukovCombatHudFormat.hit(state).contains(
				"↓ " + BukovMessages.get(
						"bukov.raid.combat.direction_south")));
		assertFalse(BukovCombatHudFormat.hit(state).contains(
				"← " + BukovMessages.get(
						"bukov.raid.combat.direction_west")));
	}

	@Test
	public void aimNavigationAndThreatCarryReadableDirection() {
		BukovRaidHudState state = new BukovRaidHudState();
		state.beginFrame("找到维修档案", 5f);
		state.aim(5f, -5f, true);
		state.navigation(
				BukovRaidHudState.Cue.MISSION,
				-8f,
				-8f,
				11.4f,
				"维修档案",
				false);
		state.threat(3f, 0f, 3f, "拾荒枪手", true);

		assertTrue(state.aimVisible());
		assertEquals(0.7071f, state.aimX(), 0.001f);
		assertEquals(-0.7071f, state.aimY(), 0.001f);
		assertTrue(state.firing());
		assertEquals(
				BukovRaidHudState.Direction.NW,
				state.navigationDirection());
		assertEquals(
				BukovRaidHudState.Distance.FAR,
				state.navigationDistance());
		assertEquals(-8f, state.navigationDeltaX(), 0f);
		assertEquals(-8f, state.navigationDeltaY(), 0f);
		assertEquals(
				BukovMessages.get(
						"bukov.raid.combat.navigation_format",
						"*",
						"↖",
						"维修档案",
						BukovMessages.get(
								"bukov.raid.combat.distance_far"),
						BukovMessages.get(
								"bukov.raid.combat."
										+ "navigation_unavailable")),
				BukovCombatHudFormat.navigation(state));
		assertEquals(
				BukovMessages.get(
						"bukov.raid.combat.threat_format",
						"! ",
						"→",
						"拾荒枪手",
						BukovMessages.get(
								"bukov.raid.combat.distance_mid")),
				BukovCombatHudFormat.threat(state));
	}

	@Test
	public void invalidAimAndCueVectorsStayHidden() {
		BukovRaidHudState state = new BukovRaidHudState();
		state.beginFrame("任务", 0f);
		state.aim(Float.NaN, 1f, false);
		state.navigation(
				BukovRaidHudState.Cue.PICKUP,
				0f,
				0f,
				1f,
				"物资",
				true);
		state.threat(Float.POSITIVE_INFINITY, 1f, 1f, null, true);

		assertFalse(state.aimVisible());
		assertFalse(state.navigationVisible());
		assertFalse(state.threatVisible());
	}
}
